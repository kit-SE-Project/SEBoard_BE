package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecoveryService {
    private final JdbcTemplate db;private final WorkerService workers;private final AuditService audit;private final Clock clock;
    public RecoveryService(JdbcTemplate db,WorkerService workers,AuditService audit,Clock clock) {this.db=db;this.workers=workers;this.audit=audit;this.clock=clock;}
    void fresh(Instant at) {
        if(at.isBefore(clock.instant().minusSeconds(120))||at.isAfter(clock.instant().plusSeconds(5)))throw ApiProblem.invalid("최신 관측 시각이 필요합니다.");
    }
    void unknown(long rental) {
        db.update("UPDATE vm_instance SET state='UNKNOWN',access_enabled=false WHERE rental_id=?",rental);
        db.update("UPDATE gpu_lease SET state='UNKNOWN',changed_at=? WHERE owner_rental_id=?",Rows.ts(clock.instant()),rental);
    }
    @Transactional public Object failure(long id,Requests.Failure input,String requestId) {
        var c=workers.locked(id);Object prior=workers.completed(c,input,input.commandId(),input.operationId());if(prior!=null)return prior;
        workers.lease(c,input.workerId());fresh(input.observedAt());var r=c.rental();long rental=Rows.number(r,"id");
        if(input.desiredRevision()!=Rows.number(r,"desired_revision"))throw ApiProblem.conflict("STALE_REVISION","현재 계약 의도와 다릅니다.");
        boolean known=input.outcomeKnown()&&input.accessBlocked();String reason=known?input.reasonCode():"OPERATION_OUTCOME_UNKNOWN";
        db.update("UPDATE job SET status='FAILED',step='WORKER_REPORTED_FAILURE',worker_id=NULL,locked_until=NULL,operation_id=?,reason_code=? WHERE id=?",input.operationId(),reason,id);
        db.update("UPDATE rental SET version=version+1,reason_code=CASE WHEN reason_code='CREDENTIAL_APPLY_UNKNOWN' THEN reason_code ELSE ? END WHERE id=?",reason,rental);unknown(rental);
        audit.event(rental,id,null,requestId,r.get("status").toString(),r.get("status").toString(),reason,
            "reportedReasonCode="+input.reasonCode()+", outcomeKnown="+input.outcomeKnown()+", accessBlocked="+input.accessBlocked());
        db.update("UPDATE rental_event SET operation_id=? WHERE id=(SELECT max(id) FROM rental_event WHERE rental_id=?)",input.operationId(),rental);
        var result=Map.of("jobId",id,"status","FAILED","reasonCode",reason,"resourcesReleased",false);workers.saveResult(id,input,result);return result;
    }
    @Transactional public Object reconcile(long id,Actor actor,Requests.Reconcile input,String requestId) {
        actor.requireAdmin();var c=workers.locked(id);workers.command(c,input.commandId());fresh(input.observedAt());var r=c.rental();var j=c.job();long rental=Rows.number(r,"id");
        if(Rows.number(r,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다.");
        if(!"RUNNING".equals(j.get("status")))throw ApiProblem.conflict("JOB_NOT_RUNNING","RUNNING 작업만 수렴할 수 있습니다.");
        boolean stale=Rows.number(j,"desired_revision")!=Rows.number(r,"desired_revision");
        if(!stale&&Rows.instant(j,"locked_until").isAfter(clock.instant()))throw ApiProblem.conflict("LEASE_STILL_VALID","유효한 현재 작업을 강제로 회수할 수 없습니다.");
        boolean terminal=input.terminalConfirmed()&&input.accessBlocked();
        String result=terminal?(stale?"SUPERSEDED":"FAILED"):"RUNNING";
        db.update("UPDATE job SET status=?,step='RECOVERY_REQUIRED',worker_id=?,locked_until=?,operation_id=?,reason_code='ADMIN_RECONCILIATION' WHERE id=?",result,terminal?null:j.get("worker_id"),terminal?null:j.get("locked_until"),input.operationId(),id);
        db.update("UPDATE rental SET version=version+1,reason_code=CASE WHEN reason_code='CREDENTIAL_APPLY_UNKNOWN' THEN reason_code ELSE 'ADMIN_RECONCILIATION' END WHERE id=?",rental);unknown(rental);
        audit.event(rental,id,actor.memberId(),requestId,r.get("status").toString(),r.get("status").toString(),"ADMIN_RECONCILIATION",input.reason());
        db.update("UPDATE rental_event SET operation_id=? WHERE id=(SELECT max(id) FROM rental_event WHERE rental_id=?)",input.operationId(),rental);
        return Map.of("jobId",id,"status",result,"resourcesReleased",false,"version",Rows.number(r,"version")+1);
    }
}
