package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fenced periodic reports refresh observation, never create jobs or release resources. */
@Service
public class HealthService {
    private final JdbcTemplate db;private final RentalService rentals;private final Clock clock;private final AuditService audit;private final AccessWindowService accessWindows;
    public HealthService(JdbcTemplate db,RentalService rentals,Clock clock,AuditService audit,AccessWindowService accessWindows) {this.db=db;this.rentals=rentals;this.clock=clock;this.audit=audit;this.accessWindows=accessWindows;}
    public Object targets(){
        return db.queryForList("SELECT r.id AS rental_id,r.server_id,r.desired_revision,r.status,v.vmid,g.generation AS gpu_generation,s.inventory_source,(r.status='ACTIVE' AND r.reason_code IS DISTINCT FROM 'CREDENTIAL_APPLY_UNKNOWN' AND g.state='HELD' AND g.owner_rental_id=r.id AND g.owner_vmid=v.vmid AND s.status='AVAILABLE' AND s.deleted_at IS NULL AND EXISTS(SELECT 1 FROM vm_credential c WHERE c.rental_id=r.id AND c.status='ACTIVE')) AS access_allowed FROM rental r JOIN vm_instance v ON v.rental_id=r.id JOIN gpu_lease g ON g.server_id=r.server_id JOIN server_node s ON s.id=r.server_id WHERE r.status IN ('READY','ACTIVE','RETAINED') ORDER BY r.id LIMIT 1000").stream().map(row->{
            var out=Rows.api(row);var window=accessWindows.forRental(Rows.number(row,"rental_id"));out.put("accessWindow",window);
            out.put("accessAllowed",Boolean.TRUE.equals(row.get("access_allowed"))&&((Number)window.get("maxLeaseSeconds")).longValue()>=5);return out;
        }).toList();
    }
    @Transactional public Object report(long id,Requests.VmHealth input,String requestId) {
        var initial=Rows.one(db,"SELECT server_id FROM rental WHERE id=?",id);rentals.server(Rows.number(initial,"server_id"),true);
        var r=Rows.one(db,"SELECT * FROM rental WHERE id=? FOR UPDATE",id);
        if(Set.of("RETURNING","RETURNED","CANCELLED").contains(r.get("status")))throw ApiProblem.conflict("INVALID_STATE","반납·취소 계약을 건강 보고로 되돌릴 수 없습니다.");
        if(input.desiredRevision()!=Rows.number(r,"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 계약 의도와 다릅니다.");
        var vm=Rows.one(db,"SELECT * FROM vm_instance WHERE rental_id=? AND vmid=? FOR UPDATE",id,input.vmid());
        var gpu=Rows.one(db,"SELECT * FROM gpu_lease WHERE server_id=? FOR UPDATE",r.get("server_id"));
        if(input.gpuGeneration()!=Rows.number(gpu,"generation"))throw ApiProblem.conflict("STALE_GENERATION","GPU 세대가 다릅니다.");
        if("RUNNING".equals(input.vmState())&&(!"HELD".equals(gpu.get("state"))||gpu.get("owner_rental_id")==null||Rows.number(gpu,"owner_rental_id")!=id||Rows.number(gpu,"owner_vmid")!=input.vmid()))throw ApiProblem.conflict("GPU_NOT_OWNED","GPU 실행 소유자가 아닙니다.");
        var at=input.observedAt();var previous=Rows.instant(vm,"observed_at");
        if(at.isBefore(clock.instant().minusSeconds(120))||at.isAfter(clock.instant().plusSeconds(5)))throw ApiProblem.invalid("최신 관측 시각이 필요합니다.");
        if(previous!=null&&!at.isAfter(previous))throw ApiProblem.conflict("STALE_OBSERVATION","기존 VM 관측보다 새 결과가 필요합니다.");
        boolean inSlot=db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,id,Rows.ts(clock.instant()),Rows.ts(clock.instant()));
        boolean credential=db.queryForObject("SELECT EXISTS(SELECT 1 FROM vm_credential WHERE rental_id=? AND status='ACTIVE')",Boolean.class,id);
        boolean uncertain="CREDENTIAL_APPLY_UNKNOWN".equals(r.get("reason_code"));
        if(input.accessEnabled()&&(!"ACTIVE".equals(r.get("status"))||!inSlot||!input.healthPassed()||!"RUNNING".equals(input.vmState())||!credential||uncertain||input.hostKeyFingerprint()==null||input.hostKeyFingerprint().isBlank()))throw ApiProblem.invalid("이용 슬롯·준비·암호 확인 전에는 접근을 열 수 없습니다.");
        if(vm.get("host_key_fingerprint")!=null&&input.hostKeyFingerprint()!=null&&!input.hostKeyFingerprint().equals(vm.get("host_key_fingerprint")))throw ApiProblem.conflict("HOST_KEY_CHANGED","기존 VM 호스트키와 다릅니다.");
        db.update("UPDATE vm_instance SET state=?,access_enabled=?,observed_at=?,host_key_fingerprint=COALESCE(?,host_key_fingerprint) WHERE rental_id=?",input.vmState(),input.accessEnabled(),Rows.ts(at),input.hostKeyFingerprint(),id);
        String old=(String)r.get("reason_code"),reason=old;
        if(!uncertain) {
            if(!input.healthPassed()||"UNKNOWN".equals(input.vmState()))reason="VM_HEALTH_UNKNOWN_OR_FAILED";
            else if("VM_HEALTH_UNKNOWN_OR_FAILED".equals(old))reason=null;
        }
        if(!Objects.equals(old,reason)) {
            db.update("UPDATE rental SET reason_code=?,version=version+1 WHERE id=?",reason,id);
            audit.event(id,null,null,requestId,r.get("status").toString(),r.get("status").toString(),reason==null?"VM_HEALTH_RECOVERED":reason);
        }
        return Map.of("rentalId",id,"observedAt",at,"vmState",input.vmState(),"accessEnabled",input.accessEnabled(),"resourcesReleased",false);
    }
}
