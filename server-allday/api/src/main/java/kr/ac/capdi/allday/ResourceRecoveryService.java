package kr.ac.capdi.allday;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceRecoveryService {
    private final JdbcTemplate db;
    private final RentalService rentals;
    private final WorkerService workers;
    private final AuditService audit;
    private final JsonCodec json;
    private final Clock clock;

    public ResourceRecoveryService(JdbcTemplate db,RentalService rentals,WorkerService workers,
        AuditService audit,JsonCodec json,Clock clock) {
        this.db=db;this.rentals=rentals;this.workers=workers;this.audit=audit;this.json=json;this.clock=clock;
    }

    @Transactional(readOnly=true)
    public Object inspect(long id,Actor actor) {
        actor.requireAdmin();var rental=rentals.owned(id,actor,false,true);
        long server=Rows.number(rental,"server_id");
        var result=new LinkedHashMap<String,Object>();
        result.put("rentalId",id);result.put("serverId",server);result.put("status",rental.get("status"));
        result.put("version",rental.get("version"));result.put("desiredRevision",rental.get("desired_revision"));
        result.put("gpuEnabled",rentals.server(server,false).get("gpu_enabled"));
        result.put("gpuLease",Rows.api(Rows.one(db,"SELECT * FROM gpu_lease WHERE server_id=?",server)));
        result.put("allocations",db.queryForList("SELECT id,server_id,rental_id,vmid,kind FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",id).stream().map(Rows::api).toList());
        result.put("ipLeases",db.queryForList("SELECT id,server_id,rental_id,host(address) AS address FROM ip_lease WHERE rental_id=? AND released_at IS NULL",id).stream().map(Rows::api).toList());
        result.put("jobs",db.queryForList("SELECT * FROM job WHERE rental_id=? AND type='PREPARE' AND desired_revision=? ORDER BY id",id,rental.get("desired_revision")).stream().map(workers::jobView).toList());
        return result;
    }

    @Transactional
    public Object reconcile(long id,Actor actor,Requests.ResourceLockReconcile input,String requestId) {
        actor.requireAdmin();var rental=rentals.lockOwned(id,actor,true);
        long server=Rows.number(rental,"server_id");
        if(server!=input.serverId())throw ApiProblem.conflict("RESOURCE_OWNER_MISMATCH","예약 장비와 복구 대상 장비가 다릅니다.");
        if(Rows.number(rental,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다. 다시 조회하세요.");
        if(!"PROVISIONING".equals(rental.get("status"))||!clock.instant().isBefore(Rows.instant(rental,"contract_end")))
            throw ApiProblem.conflict("RESOURCE_RECOVERY_NOT_ALLOWED","이용 기간 안의 준비 중 예약만 이 경로로 복구할 수 있습니다.");
        if(!Boolean.FALSE.equals(rentals.activeServer(server,false).get("gpu_enabled")))
            throw ApiProblem.conflict("GPU_RECOVERY_REQUIRES_PHYSICAL_VERIFICATION","GPU 사용 장비의 물리 상태는 이 복구 경로로 변경할 수 없습니다.");

        var job=Rows.one(db,"SELECT * FROM job WHERE id=? FOR UPDATE",input.jobId());
        if(Rows.number(job,"rental_id")!=id||Rows.number(job,"server_id")!=server||!"PREPARE".equals(job.get("type")))
            throw ApiProblem.conflict("RESOURCE_OWNER_MISMATCH","현재 예약의 준비 작업을 지정하세요.");
        workers.command(new WorkerService.Context(job,rental),input.commandId());
        if(Rows.number(job,"desired_revision")!=Rows.number(rental,"desired_revision"))
            throw ApiProblem.conflict("STALE_REVISION","현재 예약 의도의 준비 작업을 지정하세요.");
        if(!"FAILED".equals(job.get("status")))
            throw ApiProblem.conflict("JOB_NOT_RECONCILED","준비 작업을 먼저 공식 reconcile 경로로 FAILED 수렴하세요.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM job WHERE server_id=? AND status='RUNNING')",Boolean.class,server))
            throw ApiProblem.conflict("SERVER_JOB_RUNNING","만료된 lease를 포함해 RUNNING 작업을 먼저 수렴하세요.");

        var evidence=input.evidence();var now=clock.instant();
        if(evidence.observedAt().isBefore(now.minusSeconds(120))||evidence.observedAt().isAfter(now.plusSeconds(5)))
            throw ApiProblem.conflict("STALE_RECOVERY_EVIDENCE","실제 장비를 다시 확인한 120초 이내의 복구 증거가 필요합니다.");
        if(!Boolean.TRUE.equals(evidence.ownershipConfirmed())||!Boolean.TRUE.equals(evidence.accessBlocked())||
            evidence.activeTaskCount()!=0||evidence.unknownExecutionCount()!=0)
            throw ApiProblem.conflict("RECOVERY_EVIDENCE_INCOMPLETE","소유권 일치·진행 작업 0·UNKNOWN 실행 0·학생 접근 차단 확인이 필요합니다.");

        var gpu=Rows.one(db,"SELECT * FROM gpu_lease WHERE server_id=? FOR UPDATE",server);
        if(gpu.get("owner_rental_id")==null||Rows.number(gpu,"owner_rental_id")!=id||
            gpu.get("owner_vmid")==null||Rows.number(gpu,"owner_vmid")!=input.vmid())
            throw ApiProblem.conflict("RESOURCE_OWNER_MISMATCH","기존 자원 잠금의 예약·VM 소유권이 다릅니다.");
        if(Rows.number(gpu,"generation")!=input.gpuGeneration())
            throw ApiProblem.conflict("RESOURCE_GENERATION_CHANGED","자원 잠금 세대가 변경되었습니다. 다시 조회하세요.");
        if(!"UNKNOWN".equals(gpu.get("state")))
            throw ApiProblem.conflict("RESOURCE_LOCK_NOT_UNKNOWN","현재 소유자의 UNKNOWN 잠금만 복구할 수 있습니다.");

        var allocations=db.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL FOR UPDATE",id);
        var ips=db.queryForList("SELECT *,host(address) AS ip_address FROM ip_lease WHERE rental_id=? AND released_at IS NULL FOR UPDATE",id);
        var manifests=db.queryForList("SELECT * FROM resource_manifest WHERE rental_id=? FOR UPDATE",id);
        if(allocations.size()!=1||ips.size()!=1||manifests.size()!=1)
            throw ApiProblem.conflict("RESOURCE_OWNERSHIP_INCOMPLETE","기존 VMID·IP 배정과 소유 자원 목록이 모두 있어야 합니다.");
        var allocation=allocations.getFirst();var ip=ips.getFirst();var manifest=manifests.getFirst();
        if(Rows.number(allocation,"id")!=input.allocationId()||Rows.number(allocation,"server_id")!=server||
            Rows.number(allocation,"vmid")!=input.vmid()||!"RENTAL".equals(allocation.get("kind"))||
            Rows.number(ip,"id")!=input.ipLeaseId()||Rows.number(ip,"server_id")!=server||!input.ip().equals(ip.get("ip_address"))||
            Rows.number(manifest,"server_id")!=server||Rows.number(manifest,"vmid")!=input.vmid())
            throw ApiProblem.conflict("RESOURCE_OWNER_MISMATCH","복구 요청과 기존 VMID·IP·manifest 소유권이 일치하지 않습니다.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM vmid_allocation WHERE server_id=? AND kind='RENTAL' AND rental_id<>? AND released_at IS NULL) OR EXISTS(SELECT 1 FROM ip_lease WHERE server_id=? AND rental_id<>? AND released_at IS NULL) OR EXISTS(SELECT 1 FROM vm_instance WHERE server_id=? AND rental_id<>? AND state<>'DELETED')",Boolean.class,server,id,server,id,server,id))
            throw ApiProblem.conflict("SERVER_RESOURCES_IN_USE","다른 예약의 자원 소유가 남아 있습니다.");
        var vms=db.queryForList("SELECT * FROM vm_instance WHERE rental_id=? FOR UPDATE",id);
        if(!vms.isEmpty()) {
            var vm=vms.getFirst();
            if(Rows.number(vm,"server_id")!=server||Rows.number(vm,"vmid")!=input.vmid()||Rows.number(vm,"vmid_allocation_id")!=input.allocationId())
                throw ApiProblem.conflict("RESOURCE_OWNER_MISMATCH","기존 VM 소유 정보가 복구 대상과 다릅니다.");
            if(Boolean.TRUE.equals(vm.get("access_enabled")))
                throw ApiProblem.conflict("ACCESS_NOT_BLOCKED","백엔드의 학생 접근도 먼저 차단되어야 합니다.");
        }

        // This restores the same owner's logical reservation, never physical GPU availability.
        // Allocation, IP, manifest, VM, job result, owner and generation remain unchanged.
        db.update("UPDATE gpu_lease SET state='HELD',changed_at=? WHERE server_id=?",Rows.ts(now),server);
        db.update("UPDATE rental SET version=version+1,reason_code='OWNED_RESOURCE_LOCK_RECONCILED' WHERE id=?",id);
        var after=new LinkedHashMap<String,Object>(Rows.api(gpu));after.put("state","HELD");after.put("changedAt",now);
        var note=new LinkedHashMap<String,Object>();note.put("reason",input.reason());note.put("evidenceSource","ADMIN_ATTESTATION_OF_WINDOWS_RECOVERY");
        note.put("evidence",evidence);note.put("commandId",input.commandId());note.put("allocationId",input.allocationId());note.put("ipLeaseId",input.ipLeaseId());
        note.put("ip",input.ip());note.put("before",Rows.api(gpu));note.put("after",after);
        audit.event(id,input.jobId(),actor.memberId(),requestId,rental.get("status").toString(),rental.get("status").toString(),"OWNED_RESOURCE_LOCK_RECONCILED",json.write(note));
        var result=new LinkedHashMap<String,Object>();
        result.put("rentalId",id);result.put("serverId",server);result.put("status",rental.get("status"));result.put("version",Rows.number(rental,"version")+1);
        result.put("desiredRevision",rental.get("desired_revision"));result.put("jobId",input.jobId());result.put("jobStatus",job.get("status"));
        result.put("gpuState","HELD");result.put("gpuGeneration",gpu.get("generation"));result.put("ownerRentalId",id);result.put("vmid",input.vmid());result.put("ip",input.ip());
        result.put("resourcesReleased",false);result.put("vmMutationsPerformed",false);result.put("retryRequired",true);
        return result;
    }
}
