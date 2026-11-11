package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerService {
    private final JdbcTemplate db;private final RentalService rentals;private final CredentialService credentials;
    private final AuditService audit;private final JsonCodec json;private final Clock clock;
    public WorkerService(JdbcTemplate db,RentalService rentals,CredentialService credentials,AuditService audit,JsonCodec json,Clock clock) {
        this.db=db;this.rentals=rentals;this.credentials=credentials;this.audit=audit;this.json=json;this.clock=clock;
    }
    record Context(Map<String,Object> job,Map<String,Object> rental) {}
    Context locked(long id) {
        var initial=Rows.one(db,"SELECT * FROM job WHERE id=?",id);
        rentals.server(Rows.number(initial,"server_id"),true);
        var rental=Rows.one(db,"SELECT * FROM rental WHERE id=? FOR UPDATE",initial.get("rental_id"));
        var job=Rows.one(db,"SELECT * FROM job WHERE id=? FOR UPDATE",id);return new Context(job,rental);
    }
    void lease(Context c,String workerId) {
        var job=c.job();
        if(!"RUNNING".equals(job.get("status"))||!workerId.equals(job.get("worker_id"))||!Rows.instant(job,"locked_until").isAfter(clock.instant()))
            throw ApiProblem.conflict("WORKER_LEASE_INVALID","작업 소유권이 없거나 만료되었습니다.");
        if(Rows.number(job,"desired_revision")!=Rows.number(c.rental(),"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","이전 의도의 작업 결과는 적용할 수 없습니다.");
    }
    String hash(Object input) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.write(input).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    Object completed(Context c,Object input,String commandId,String operationId) {
        if(!jobView(c.job()).get("commandId").equals(commandId))throw ApiProblem.conflict("STALE_COMMAND","현재 작업 시도와 다른 명령입니다.");
        if(Rows.number(c.job(),"desired_revision")!=Rows.number(c.rental(),"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 계약 의도와 다릅니다.");
        if(c.job().get("result_payload_hash")!=null&&operationId.equals(c.job().get("operation_id"))) {
            if(!hash(input).equals(c.job().get("result_payload_hash")))throw ApiProblem.conflict("RESULT_CHANGED","동일 작업 결과를 다른 내용으로 덮을 수 없습니다.");
            return json.read(c.job().get("result_response").toString());
        }
        return null;
    }
    void saveResult(long jobId,Object input,Object response) {
        db.update("UPDATE job SET result_payload_hash=?,result_response=?::jsonb WHERE id=?",hash(input),json.write(response),jobId);
    }
    @Transactional public Object claim(long id,String workerId) {
        var c=locked(id);var j=c.job();
        if(Rows.number(j,"desired_revision")!=Rows.number(c.rental(),"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 계약 의도와 일치하지 않습니다.");
        if("RUNNING".equals(j.get("status"))) {lease(c,workerId);return jobView(j);}
        if(!"PENDING".equals(j.get("status")))throw ApiProblem.conflict("JOB_NOT_PENDING","대기 중인 작업만 가져올 수 있습니다.");
        if(Rows.instant(j,"next_run_at").isAfter(clock.instant()))throw ApiProblem.conflict("JOB_NOT_DUE","아직 실행 시각이 아닙니다.");
        if(db.queryForObject("SELECT count(*) FROM job WHERE server_id=? AND status='RUNNING'",Integer.class,j.get("server_id"))>0)
            throw ApiProblem.conflict("SERVER_JOB_RUNNING","이 서버의 다른 작업을 먼저 수렴·복구해야 합니다.");
        if(Set.of("RETURNED","CANCELLED").contains(c.rental().get("status")))throw ApiProblem.conflict("INVALID_STATE","종료된 계약의 작업입니다.");
        if(!"RETURN".equals(j.get("type"))&&!clock.instant().isBefore(Rows.instant(c.rental(),"contract_end")))throw ApiProblem.conflict("CONTRACT_EXPIRED","만료된 계약은 반납 흐름이 필요합니다.");
        if(!"RETURN".equals(j.get("type"))&&"RETURNING".equals(c.rental().get("status")))throw ApiProblem.conflict("INVALID_STATE","반납 작업만 실행할 수 있습니다.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM job other JOIN rental r ON r.id=other.rental_id WHERE other.server_id=? AND other.id<>? AND other.status='PENDING' AND other.desired_revision=r.desired_revision AND other.next_run_at<=? AND other.priority>?)",Boolean.class,j.get("server_id"),id,Rows.ts(clock.instant()),j.get("priority")))
            throw ApiProblem.conflict("PRIOR_JOB_REQUIRED","반납·정지 등 우선 작업을 먼저 처리하세요.");
        if("START".equals(j.get("type"))) {
            boolean slot=db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE id=? AND start_at<=? AND end_at>? AND state<>'CANCELLED')",Boolean.class,j.get("slot_id"),Rows.ts(clock.instant()),Rows.ts(clock.instant()));
            if(!slot)throw ApiProblem.conflict("OUTSIDE_SLOT","현재 이용 슬롯의 시작 작업만 실행할 수 있습니다.");
            boolean prepared=db.queryForObject("SELECT EXISTS(SELECT 1 FROM vm_instance WHERE rental_id=? AND state NOT IN ('DELETED','FAILED')) AND EXISTS(SELECT 1 FROM vm_credential WHERE rental_id=? AND status='ACTIVE')",Boolean.class,j.get("rental_id"),j.get("rental_id"));
            if(!prepared)throw ApiProblem.conflict("PREPARATION_NOT_READY","VM과 암호 준비를 먼저 완료하세요.");
        }
        db.update("UPDATE job SET status='RUNNING',step='DISPATCHING',worker_id=?,locked_until=? WHERE id=?",workerId,Rows.ts(clock.instant().plusSeconds(120)),id);
        if("PREPARE".equals(j.get("type"))&&"RESERVED".equals(c.rental().get("status"))) {
            db.update("UPDATE rental SET status='PROVISIONING',version=version+1 WHERE id=?",j.get("rental_id"));
            audit.event(Rows.number(c.rental(),"id"),id,null,UUID.randomUUID().toString(),"RESERVED","PROVISIONING","WORKER_CLAIMED_PREPARE");
        }
        return jobView(Rows.one(db,"SELECT * FROM job WHERE id=?",id));
    }
    Map<String,Object> jobView(Map<String,Object> j) {
        var result=new LinkedHashMap<String,Object>();
        for(var key:List.of("id","server_id","rental_id","slot_id","type","step","status","desired_revision","retry_count","next_run_at","locked_until"))result.put(key,j.get(key));
        result.put("command_id","job:"+j.get("id")+":attempt:"+j.get("retry_count")+":r"+j.get("desired_revision"));return Rows.api(result);
    }
    public Object due() {
        return db.queryForList("SELECT j.* FROM job j JOIN rental r ON r.id=j.rental_id WHERE j.status='PENDING' AND j.next_run_at<=? AND j.desired_revision=r.desired_revision ORDER BY j.priority DESC,j.next_run_at,j.id LIMIT 100",Rows.ts(clock.instant())).stream().map(this::jobView).toList();
    }
    void command(Context c,String commandId) {
        if(!jobView(c.job()).get("commandId").equals(commandId))throw ApiProblem.conflict("STALE_COMMAND","현재 작업 시도와 다릅니다.");
    }
    @Transactional public Object heartbeat(long id,String workerId,String commandId) {
        var c=locked(id);command(c,commandId);lease(c,workerId);db.update("UPDATE job SET locked_until=? WHERE id=?",Rows.ts(clock.instant().plusSeconds(120)),id);return Map.of("jobId",id,"renewed",true);
    }
    @Transactional public Object defer(long id,Requests.DispatchDeferred input,String requestId) {
        var c=locked(id);command(c,input.commandId());var j=c.job();var r=c.rental();var now=clock.instant();
        if(!Boolean.TRUE.equals(input.dispatchNotStarted()))throw ApiProblem.invalid("Agent 전송 전임을 확인한 작업만 지연할 수 있습니다.");
        if(input.observedAt().isBefore(now.minusSeconds(120))||input.observedAt().isAfter(now.plusSeconds(5)))throw ApiProblem.invalid("최신 관측 시각이 필요합니다.");
        if(("PENDING".equals(j.get("status"))&&"WAITING_FOR_PREREQUISITE".equals(j.get("step")))||
            ("SUPERSEDED".equals(j.get("status"))&&"DISPATCH_NOT_STARTED".equals(j.get("step"))))return jobView(j);
        // The authenticated worker attests that its durable operation store has no
        // operation for this exact command. Never use this path after Agent submit.
        if(!"RUNNING".equals(j.get("status"))||!input.workerId().equals(j.get("worker_id"))||!"DISPATCHING".equals(j.get("step")))
            throw ApiProblem.conflict("WORKER_LEASE_INVALID","현재 worker가 아직 전송하지 않은 실행 잠금만 정리할 수 있습니다.");
        boolean stale=Rows.number(j,"desired_revision")!=Rows.number(r,"desired_revision")||Set.of("RETURNED","CANCELLED").contains(r.get("status"))||
            ("RETURNING".equals(r.get("status"))&&!"RETURN".equals(j.get("type")));
        String status=stale?"SUPERSEDED":"PENDING",step=stale?"DISPATCH_NOT_STARTED":"WAITING_FOR_PREREQUISITE";
        db.update("UPDATE job SET status=?,step=?,worker_id=NULL,locked_until=NULL,next_run_at=?,reason_code=? WHERE id=?",status,step,Rows.ts(now.plusSeconds(10)),input.reasonCode(),id);
        audit.event(Rows.number(r,"id"),id,null,requestId,r.get("status").toString(),r.get("status").toString(),"DISPATCH_DEFERRED",input.reasonCode());
        return jobView(Rows.one(db,"SELECT * FROM job WHERE id=?",id));
    }
    @Transactional public Object pendingCredential(long id,String workerId,String commandId) {
        var c=locked(id);command(c,commandId);lease(c,workerId);
        if(!Set.of("PREPARE","RESET_CREDENTIAL").contains(c.job().get("type")))throw new ApiProblem(403,"FORBIDDEN","이 작업은 비밀값을 조회할 수 없습니다.");
        long rentalId=Rows.number(c.rental(),"id");
        if(db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='PENDING'",Integer.class,rentalId)>0)return credentials.pending(rentalId);
        if(c.job().get("type").equals("PREPARE"))return credentials.active(rentalId);
        throw ApiProblem.conflict("CREDENTIAL_NOT_PENDING","재설정할 암호가 준비되지 않았습니다.");
    }
    @Transactional public Object applied(long id,Requests.CredentialApplied input,String requestId) {
        var c=locked(id);Object prior=completed(c,input,input.commandId(),input.operationId());if(prior!=null)return prior;
        lease(c,input.workerId());var j=c.job();var r=c.rental();
        if(!Set.of("PREPARE","RESET_CREDENTIAL").contains(j.get("type")))throw ApiProblem.conflict("WRONG_JOB_TYPE","암호 적용 작업이 아닙니다.");
        if(input.desiredRevision()!=Rows.number(r,"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 의도와 다릅니다.");
        boolean success=Boolean.TRUE.equals(input.passed());
        var response=new LinkedHashMap<String,Object>();response.put("jobId",id);response.put("credentialVersion",input.credentialVersion());response.put("applied",input.passed());
        if(hash(input).equals(j.get("credential_step_hash")))return response;
        var rows=db.queryForList("SELECT * FROM vm_credential WHERE rental_id=? AND credential_version=? AND status IN ('PENDING','ACTIVE') FOR UPDATE",r.get("id"),input.credentialVersion());
        if(rows.isEmpty())throw ApiProblem.conflict("CREDENTIAL_NOT_PENDING","해당 암호 버전이 적용 대상이 아닙니다.");
        var pending=rows.getFirst();
        if("ACTIVE".equals(pending.get("status"))&&!success)throw ApiProblem.conflict("CREDENTIAL_ALREADY_ACTIVE","이미 적용된 암호를 실패로 되돌릴 수 없습니다.");
        if(success) {
            db.update("UPDATE vm_credential SET status='RETIRED' WHERE rental_id=? AND status='ACTIVE'",r.get("id"));
            db.update("UPDATE vm_credential SET status='ACTIVE' WHERE id=?",pending.get("id"));
        }else db.update("UPDATE vm_credential SET status='FAILED' WHERE id=?",pending.get("id"));
        // A successful PREPARE password step is not evidence that the VM is READY.
        boolean terminal=j.get("type").equals("RESET_CREDENTIAL")||!success;
        String reason=success?null:input.passed()==null?"CREDENTIAL_APPLY_UNKNOWN":"CREDENTIAL_APPLY_FAILED";
        db.update("UPDATE job SET status=?,step=?,operation_id=?,upid=?,worker_id=?,locked_until=?,reason_code=? WHERE id=?",
            terminal?(success?"SUCCEEDED":"FAILED"):"RUNNING",success?"CREDENTIAL_APPLIED":reason,input.operationId(),input.upid(),
            terminal?null:input.workerId(),terminal?null:Rows.ts(clock.instant().plusSeconds(120)),reason,id);
        if(input.passed()==null)db.update("UPDATE rental SET reason_code='CREDENTIAL_APPLY_UNKNOWN',version=version+1 WHERE id=?",r.get("id"));
        if(success)db.update("UPDATE rental SET reason_code=CASE WHEN reason_code='CREDENTIAL_APPLY_UNKNOWN' THEN NULL ELSE reason_code END,version=version+1 WHERE id=?",r.get("id"));
        audit.event(Rows.number(r,"id"),id,null,requestId,r.get("status").toString(),r.get("status").toString(),success?"CREDENTIAL_APPLIED":reason);
        db.update("UPDATE rental_event SET operation_id=?,upid=? WHERE id=(SELECT max(id) FROM rental_event WHERE rental_id=?)",input.operationId(),input.upid(),r.get("id"));
        db.update("UPDATE job SET credential_step_hash=? WHERE id=?",hash(input),id);
        if(terminal)saveResult(id,input,response);return response;
    }
    @Transactional public Object verify(long id,Requests.Verification input,String requestId) {
        var c=locked(id);Object prior=completed(c,input,input.commandId(),input.operationId());if(prior!=null)return prior;
        lease(c,input.workerId());var j=c.job();var r=c.rental();long rentalId=Rows.number(r,"id");
        if(!"RETURN".equals(j.get("type"))||!"RETURNING".equals(r.get("status")))throw ApiProblem.conflict("WRONG_JOB_TYPE","반납 중인 계약의 반납 작업만 검증할 수 있습니다.");
        if(input.desiredRevision()!=Rows.number(r,"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 계약 의도와 다릅니다.");
        if(input.observedAt().isBefore(clock.instant().minusSeconds(120))||input.observedAt().isAfter(clock.instant().plusSeconds(5)))throw ApiProblem.invalid("검증 시각이 최신이 아니거나 미래입니다.");
        Set<String> kinds=new HashSet<>();for(var item:input.items())kinds.add(item.kind());
        if(kinds.size()!=5)throw ApiProblem.invalid("서로 다른 필수 검증 항목 5개가 필요합니다.");
        if(json.write(input.items()).length()>65536)throw ApiProblem.invalid("검증 증거 크기를 초과했습니다.");
        boolean allPass=input.items().stream().allMatch(i->Boolean.TRUE.equals(i.passed()));
        boolean hasFail=input.items().stream().anyMatch(i->Boolean.FALSE.equals(i.passed()));
        String result=allPass?"PASS":hasFail?"FAIL":"UNKNOWN";
        if(allPass)validateManifest(rentalId,input.items());
        long verificationId=db.queryForObject("INSERT INTO return_verification(rental_id,job_id,result,verified_at,attempt) VALUES (?,?,?,?,?) RETURNING id",Long.class,rentalId,id,result,Rows.ts(input.observedAt()),j.get("retry_count"));
        for(var item:input.items())db.update("INSERT INTO verification_item(verification_id,kind,passed,evidence,checked_at) VALUES (?,?,?,?::jsonb,?)",verificationId,item.kind(),item.passed(),json.write(item.evidence()),Rows.ts(input.observedAt()));
        db.update("UPDATE job SET status=?,step=?,worker_id=NULL,locked_until=NULL,operation_id=?,upid=?,reason_code=? WHERE id=?",
            allPass?"SUCCEEDED":"FAILED",allPass?"VERIFIED":"VERIFICATION_BLOCKED",input.operationId(),input.upid(),allPass?null:"VERIFICATION_"+result,id);
        if(allPass) {
            db.update("UPDATE rental SET status='RETURNED',retained_reason=NULL,version=version+1,reason_code='RETURN_VERIFIED' WHERE id=?",rentalId);
            db.update("UPDATE rental_slot SET state='CANCELLED' WHERE rental_id=? AND end_at>?",rentalId,Rows.ts(clock.instant()));
            db.update("UPDATE vm_instance SET state='DELETED',access_enabled=false,observed_at=? WHERE rental_id=?",Rows.ts(input.observedAt()),rentalId);
            db.update("UPDATE vm_credential SET status='RETIRED' WHERE rental_id=?",rentalId);
            db.update("UPDATE ip_lease SET released_at=? WHERE rental_id=? AND released_at IS NULL",Rows.ts(clock.instant()),rentalId);
            db.update("UPDATE vmid_allocation SET released_at=? WHERE rental_id=? AND released_at IS NULL",Rows.ts(clock.instant()),rentalId);
            db.update("UPDATE gpu_lease SET owner_rental_id=NULL,owner_vmid=NULL,state='FREE',generation=generation+1,changed_at=? WHERE owner_rental_id=?",Rows.ts(clock.instant()),rentalId);
            rentals.notify(Rows.number(r,"member_id"),"rental:"+rentalId+":returned",Map.of("rentalId",rentalId,"status","RETURNED"));
        }
        audit.event(rentalId,id,null,requestId,"RETURNING",allPass?"RETURNED":"RETURNING","RETURN_VERIFICATION_"+result);
        db.update("UPDATE rental_event SET operation_id=?,upid=? WHERE id=(SELECT max(id) FROM rental_event WHERE rental_id=?)",input.operationId(),input.upid(),rentalId);
        var response=Map.of("verificationId",verificationId,"result",result,"rentalStatus",allPass?"RETURNED":"RETURNING");saveResult(id,input,response);return response;
    }
    @Transactional public Object observe(long id,Requests.Observation input,String requestId) {
        var c=locked(id);Object prior=completed(c,input,input.commandId(),input.operationId());if(prior!=null)return prior;
        lease(c,input.workerId());var r=c.rental();var j=c.job();long rentalId=Rows.number(r,"id");
        if(!Set.of("PREPARE","START","STOP","SWITCH").contains(j.get("type"))||Set.of("RETURNING","RETURNED","CANCELLED").contains(r.get("status")))
            throw ApiProblem.conflict("INVALID_STATE","반납·취소 계약에 준비 결과를 적용할 수 없습니다.");
        if(input.desiredRevision()!=Rows.number(r,"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 의도와 다릅니다.");
        if(input.observedAt().isBefore(clock.instant().minusSeconds(120))||input.observedAt().isAfter(clock.instant().plusSeconds(5)))throw ApiProblem.invalid("관측 시각이 올바르지 않습니다.");
        var allocation=Rows.one(db,"SELECT * FROM vmid_allocation WHERE rental_id=? AND server_id=? AND vmid=? AND released_at IS NULL",rentalId,r.get("server_id"),input.vmid());
        boolean ipValid;
        try{ipValid=Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM ip_lease WHERE rental_id=? AND server_id=? AND address=?::inet AND released_at IS NULL)",Boolean.class,rentalId,r.get("server_id"),input.ip()));}
        catch(org.springframework.dao.DataAccessException e){throw ApiProblem.invalid("IP 주소를 확인하세요.");}
        if(!ipValid)throw ApiProblem.conflict("IP_NOT_OWNED","계약에 미리 할당된 IP만 기록할 수 있습니다.");
        var gpu=Rows.one(db,"SELECT * FROM gpu_lease WHERE server_id=? FOR UPDATE",r.get("server_id"));
        if(Rows.number(gpu,"generation")!=input.gpuGeneration())throw ApiProblem.conflict("STALE_GENERATION","GPU 소유 세대가 다릅니다.");
        if(input.vmState().equals("RUNNING")&&(!"HELD".equals(gpu.get("state"))||gpu.get("owner_rental_id")==null||Rows.number(gpu,"owner_rental_id")!=rentalId||Rows.number(gpu,"owner_vmid")!=input.vmid()))
            throw ApiProblem.conflict("GPU_NOT_OWNED","현재 GPU 소유 계약과 VM이 다릅니다.");
        var manifests=db.queryForList("SELECT * FROM resource_manifest WHERE rental_id=? AND server_id=? AND vmid=?",rentalId,r.get("server_id"),input.vmid());
        if(manifests.isEmpty())throw ApiProblem.conflict("MANIFEST_REQUIRED","소유 자원 목록이 필요합니다.");
        var existing=db.queryForList("SELECT vmid FROM vm_instance WHERE rental_id=?",rentalId);
        if(!existing.isEmpty()&&Rows.number(existing.getFirst(),"vmid")!=input.vmid())throw ApiProblem.conflict("CONTRACT_VM_CHANGED","계약 중 다른 VM으로 바꿀 수 없습니다.");
        boolean inSlot=Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,rentalId,Rows.ts(clock.instant()),Rows.ts(clock.instant())));
        boolean activeCredential=db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='ACTIVE'",Integer.class,rentalId)>0;
        if(input.accessEnabled()&&(!inSlot||!input.vmState().equals("RUNNING")||!input.healthPassed()||!activeCredential||input.hostKeyFingerprint()==null||input.hostKeyFingerprint().isBlank()))
            throw ApiProblem.invalid("시간·건강·암호·호스트키 확인 전에는 접근을 열 수 없습니다.");
        boolean healthy=input.healthPassed()&&!input.vmState().equals("UNKNOWN")&&activeCredential;
        boolean stopping="STOP".equals(j.get("type"));
        if(stopping&&healthy) {
            if(inSlot||!"STOPPED".equals(input.vmState())||input.accessEnabled()||!Boolean.TRUE.equals(input.gpuDetached()))
                throw ApiProblem.invalid("정지 성공에는 슬롯 밖·VM STOPPED·접근 차단·실제 GPU 분리 확인이 필요합니다.");
            if(gpu.get("owner_rental_id")==null||Rows.number(gpu,"owner_rental_id")!=rentalId)
                throw ApiProblem.conflict("GPU_NOT_OWNED","정지 대상의 GPU 소유권이 다릅니다.");
        }
        if(!stopping&&healthy&&!"RUNNING".equals(input.vmState()))throw ApiProblem.invalid("준비·시작 성공은 RUNNING 건강 검사 결과여야 합니다.");
        if(input.accessEnabled()&&"CREDENTIAL_APPLY_UNKNOWN".equals(r.get("reason_code")))throw ApiProblem.conflict("CREDENTIAL_STATE_UNKNOWN","암호 적용 상태를 먼저 확인하세요.");
        String after=r.get("status").toString();String retained=(String)r.get("retained_reason");
        if(healthy) {
            retained=null;
            if(inSlot)after=input.vmState().equals("RUNNING")&&input.accessEnabled()?"ACTIVE":"READY";
            else if(clock.instant().isAfter(Rows.instant(r,"contract_start"))&&clock.instant().isBefore(Rows.instant(r,"contract_end"))) {after="RETAINED";retained="BETWEEN_SLOTS";}
            else if(clock.instant().isBefore(Rows.instant(r,"contract_start")))after="READY";
            else throw ApiProblem.conflict("CONTRACT_EXPIRED","만료된 계약은 반납 흐름으로 수렴해야 합니다.");
        }
        db.update("INSERT INTO vm_instance(rental_id,server_id,vmid,vmid_allocation_id,ip,state,observed_at,host_key_fingerprint,access_enabled) VALUES (?,?,?,?,?::inet,?,?,?,?) ON CONFLICT (rental_id) DO UPDATE SET ip=excluded.ip,state=excluded.state,observed_at=excluded.observed_at,host_key_fingerprint=excluded.host_key_fingerprint,access_enabled=excluded.access_enabled",
            rentalId,r.get("server_id"),input.vmid(),allocation.get("id"),input.ip(),input.vmState(),Rows.ts(input.observedAt()),input.hostKeyFingerprint(),input.accessEnabled());
        String reason="CREDENTIAL_APPLY_UNKNOWN".equals(r.get("reason_code"))?"CREDENTIAL_APPLY_UNKNOWN":healthy?null:"VM_HEALTH_UNKNOWN_OR_FAILED";
        db.update("UPDATE rental SET status=?,retained_reason=?,version=version+1,reason_code=? WHERE id=?",after,retained,reason,rentalId);
        if(stopping&&healthy)db.update("UPDATE gpu_lease SET owner_rental_id=NULL,owner_vmid=NULL,state='FREE',generation=generation+1,changed_at=? WHERE server_id=?",Rows.ts(clock.instant()),r.get("server_id"));
        if(healthy) {
            db.update("UPDATE rental_slot SET state='COMPLETED' WHERE rental_id=? AND state<>'CANCELLED' AND end_at<=?",rentalId,Rows.ts(clock.instant()));
            if(after.equals("ACTIVE"))db.update("UPDATE rental_slot SET state='ACTIVE' WHERE rental_id=? AND start_at<=? AND end_at>? AND state<>'CANCELLED'",rentalId,Rows.ts(clock.instant()),Rows.ts(clock.instant()));
            if("PREPARE".equals(j.get("type"))&&"ACTIVE".equals(after)) {
                // Actual PREPARE observation already proved this slot is running and accessible.
                // Retain the START intent as superseded instead of dispatching a second start.
                db.update("UPDATE job target SET status='SUPERSEDED',step='PREPARE_ALREADY_ACTIVE',reason_code='START_SATISFIED_BY_PREPARE' FROM rental_slot slot WHERE target.rental_id=? AND target.desired_revision=? AND target.type='START' AND target.status='PENDING' AND target.slot_id=slot.id AND slot.state='ACTIVE' AND slot.start_at<=? AND slot.end_at>?",rentalId,r.get("desired_revision"),Rows.ts(clock.instant()),Rows.ts(clock.instant()));
            }
        }
        db.update("UPDATE job SET status=?,step=?,worker_id=NULL,locked_until=NULL,operation_id=?,upid=?,reason_code=? WHERE id=?",healthy?"SUCCEEDED":"FAILED",healthy?"OBSERVED":"HEALTH_FAILED",input.operationId(),input.upid(),healthy?null:"VM_HEALTH_UNKNOWN_OR_FAILED",id);
        audit.event(rentalId,id,null,requestId,r.get("status").toString(),after,healthy?"VM_OBSERVED":"VM_HEALTH_FAILED");
        db.update("UPDATE rental_event SET operation_id=?,upid=? WHERE id=(SELECT max(id) FROM rental_event WHERE rental_id=?)",input.operationId(),input.upid(),rentalId);
        var response=Map.of("jobId",id,"rentalId",rentalId,"status",after,"healthPassed",healthy);saveResult(id,input,response);return response;
    }
    void validateManifest(long rentalId,List<Requests.VerificationItem> items) {
        var manifests=db.queryForList("SELECT * FROM resource_manifest WHERE rental_id=?",rentalId);
        boolean knownResources=db.queryForObject("SELECT EXISTS(SELECT 1 FROM vm_instance WHERE rental_id=?) OR EXISTS(SELECT 1 FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL)",Boolean.class,rentalId,rentalId);
        if(knownResources&&manifests.isEmpty())throw ApiProblem.conflict("MANIFEST_REQUIRED","소유 자원 목록을 먼저 수렴해야 합니다.");
        Map<String,Object> manifest=manifests.isEmpty()?Map.of():manifests.getFirst();
        for(var item:items) {
            if(item.kind().equals("VM_ABSENT")) {
                Object expected=manifest.get("vmid"),actual=item.evidence().get("vmid");
                if(expected!=null&&(!(actual instanceof Number)||((Number)actual).intValue()!=((Number)expected).intValue()))throw ApiProblem.invalid("검증 VMID가 소유 자원과 일치하지 않습니다.");
            }
            String field=switch(item.kind()){case "VOLUMES_ABSENT"->"volume_ids";case "SNIPPETS_ABSENT"->"snippet_ids";case "INGRESS_ABSENT"->"ingress_rule_ids";default->null;};
            if(field!=null) {
                Object expected=manifest.containsKey(field)?json.read(manifest.get(field).toString()):List.of();
                Object checked=item.evidence().get("checkedIds");
                if(!(checked instanceof List<?> actual)||!(expected instanceof List<?> wanted)||!new HashSet<>(actual).equals(new HashSet<>(wanted)))
                    throw ApiProblem.invalid("검증한 자원 목록이 manifest와 일치하지 않습니다.");
            }
        }
    }
}
