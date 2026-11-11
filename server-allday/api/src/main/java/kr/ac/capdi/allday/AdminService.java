package kr.ac.capdi.allday;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final JdbcTemplate db;private final RentalService rentals;private final AuditService audit;private final Clock clock;private final CredentialService credentials;private final JsonCodec json;
    public AdminService(JdbcTemplate db,RentalService rentals,AuditService audit,Clock clock,CredentialService credentials,JsonCodec json) {this.db=db;this.rentals=rentals;this.audit=audit;this.clock=clock;this.credentials=credentials;this.json=json;}
    public Object categories(Actor actor) {
        actor.requireAdmin();
        return db.queryForList("SELECT id,name,description,version FROM category WHERE deleted_at IS NULL ORDER BY id")
            .stream().map(Rows::api).toList();
    }

    @Transactional public Object category(Actor actor,Requests.CategoryInput request) {
        actor.requireAdmin();
        try {
            return Rows.api(db.queryForMap("INSERT INTO category(name,description) VALUES (?,?) RETURNING id,name,description,version",
                request.name().strip(),categoryDescription(request.description())));
        } catch(org.springframework.dao.DuplicateKeyException e) {
            throw ApiProblem.conflict("CATEGORY_NAME_EXISTS","이미 사용 중인 카테고리 이름입니다.");
        }
    }

    @Transactional public Object editCategory(Actor actor,long id,Requests.CategoryEdit request) {
        actor.requireAdmin();
        lockCategory(id,request.expectedVersion());
        try {
            return Rows.api(db.queryForMap("UPDATE category SET name=?,description=?,version=version+1 WHERE id=? RETURNING id,name,description,version",
                request.name().strip(),categoryDescription(request.description()),id));
        } catch(org.springframework.dao.DuplicateKeyException e) {
            throw ApiProblem.conflict("CATEGORY_NAME_EXISTS","이미 사용 중인 카테고리 이름입니다.");
        }
    }

    @Transactional public void deleteCategory(Actor actor,long id,Requests.CategoryDelete request) {
        actor.requireAdmin();
        lockCategory(id,request.expectedVersion());
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM server_node WHERE category_id=? AND deleted_at IS NULL)",Boolean.class,id))
            throw ApiProblem.conflict("CATEGORY_IN_USE","연결된 서버를 다른 카테고리로 옮긴 뒤 삭제하세요.");
        db.update("UPDATE category SET deleted_at=?,version=version+1 WHERE id=?",Rows.ts(clock.instant()),id);
    }

    private void lockCategory(long id,long expectedVersion) {
        var row=Rows.one(db,"SELECT version FROM category WHERE id=? AND deleted_at IS NULL FOR UPDATE",id);
        if(Rows.number(row,"version")!=expectedVersion)
            throw ApiProblem.conflict("VERSION_CONFLICT","카테고리가 변경되었습니다. 다시 조회하세요.");
    }

    private void requireActiveCategory(long id) {
        if(db.queryForList("SELECT id FROM category WHERE id=? AND deleted_at IS NULL FOR SHARE",id).isEmpty())
            throw new ApiProblem(422,"INVALID_CATEGORY","유효한 카테고리를 선택하세요.");
    }

    private String categoryDescription(String description) {
        return description==null||description.isBlank()?null:description.strip();
    }

    @Transactional public Object server(Actor actor,Requests.ServerInput request) {
        actor.requireAdmin();
        requireActiveCategory(request.categoryId());
        var row=db.queryForMap("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,gpu_enabled) VALUES (?,?,?,?,?,?,?) RETURNING id,status,version,gpu_enabled",
            request.categoryId(),request.displayName(),request.nodeName(),request.cpuModel(),request.memoryGb(),
            request.gpuModel(),!Boolean.FALSE.equals(request.gpuEnabled()));
        db.update("INSERT INTO gpu_lease(server_id) VALUES (?)",row.get("id"));return Rows.api(row);
    }
    @Transactional public Object status(Actor actor,long id,Requests.ServerStatus request) {
        actor.requireAdmin();var row=rentals.activeServer(id,true);
        if(Rows.number(row,"version")!=request.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","서버 상태가 변경되었습니다.");
        db.update("UPDATE server_node SET status=?,version=version+1,status_reason=?,changed_by=?,changed_at=? WHERE id=?",
            request.status(),request.reason(),actor.memberId(),Rows.ts(clock.instant()),id);
        return Map.of("id",id,"status",request.status(),"version",Rows.number(row,"version")+1);
    }
    boolean inUse(long id) {
        return db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental WHERE server_id=? AND status NOT IN ('RETURNED','CANCELLED'))",Boolean.class,id);
    }
    void event(long id,Actor actor,String requestId,String action,String reason,Object before,Object after) {
        db.update("INSERT INTO server_metadata_event(server_id,actor_id,request_id,action,reason,before_data,after_data,created_at) VALUES (?,?,?,?,?,?::jsonb,?::jsonb,?)",id,actor.memberId(),requestId,action,reason,json.write(before),json.write(after),Rows.ts(clock.instant()));
    }
    @Transactional public Object edit(Actor actor,long id,Requests.ServerEdit input,String requestId) {
        actor.requireAdmin();var row=rentals.activeServer(id,true);
        if(Rows.number(row,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","서버 정보가 변경되었습니다. 다시 조회하세요.");
        requireActiveCategory(input.categoryId());
        boolean specificationChanged=Rows.number(row,"category_id")!=input.categoryId()||!row.get("cpu_model").equals(input.cpuModel().trim())||Rows.number(row,"memory_gb")!=input.memoryGb()||!row.get("gpu_model").equals(input.gpuModel().trim());
        if(specificationChanged&&inUse(id))throw ApiProblem.conflict("SERVER_IN_USE","진행 대여 중에는 표시명만 수정할 수 있습니다.");
        var before=rentals.serverView(row);
        db.update("UPDATE server_node SET category_id=?,display_name=?,cpu_model=?,memory_gb=?,gpu_model=?,version=version+1,status_reason=?,changed_by=?,changed_at=? WHERE id=?",input.categoryId(),input.displayName().trim(),input.cpuModel().trim(),input.memoryGb(),input.gpuModel().trim(),input.reason().trim(),actor.memberId(),Rows.ts(clock.instant()),id);
        var after=rentals.serverView(rentals.server(id,false));event(id,actor,requestId,"METADATA_UPDATED",input.reason().trim(),before,after);return after;
    }
    @Transactional public void deactivate(Actor actor,long id,Requests.ServerDelete input,String requestId) {
        actor.requireAdmin();var row=rentals.server(id,true);
        if(row.get("deleted_at")!=null)return;
        if(Rows.number(row,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","서버 정보가 변경되었습니다. 다시 조회하세요.");
        if(inUse(id))throw ApiProblem.conflict("SERVER_IN_USE","종료되지 않은 대여가 있는 서버는 삭제할 수 없습니다.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM job WHERE server_id=? AND status='RUNNING') OR EXISTS(SELECT 1 FROM vmid_allocation WHERE server_id=? AND kind='RENTAL' AND released_at IS NULL) OR EXISTS(SELECT 1 FROM ip_lease WHERE server_id=? AND released_at IS NULL) OR EXISTS(SELECT 1 FROM gpu_lease WHERE server_id=? AND (owner_rental_id IS NOT NULL OR state<>'FREE')) OR EXISTS(SELECT 1 FROM vm_instance WHERE server_id=? AND state<>'DELETED')",Boolean.class,id,id,id,id,id))throw ApiProblem.conflict("SERVER_RESOURCES_IN_USE","실행 작업·자원 소유·불명 결과를 확인한 뒤 삭제하세요.");
        var before=rentals.serverView(row);
        db.update("UPDATE server_node SET deleted_at=?,deleted_by=?,deleted_reason=?,status='MAINTENANCE',version=version+1,status_reason=?,changed_by=?,changed_at=? WHERE id=?",Rows.ts(clock.instant()),actor.memberId(),input.reason().trim(),input.reason().trim(),actor.memberId(),Rows.ts(clock.instant()),id);
        var after=new LinkedHashMap<String,Object>(rentals.serverView(rentals.server(id,false)));after.put("deletedAt",clock.instant());event(id,actor,requestId,"DEACTIVATED",input.reason().trim(),before,after);
    }
    @Transactional public Object retry(Actor actor,long jobId,Requests.Change request,String requestId) {
        actor.requireAdmin();if(request.reason()==null||request.reason().isBlank())throw ApiProblem.invalid("재시도 사유를 입력하세요.");
        var initial=Rows.one(db,"SELECT rental_id FROM job WHERE id=?",jobId);
        var rental=rentals.lockOwned(Rows.number(initial,"rental_id"),actor,true);
        var job=Rows.one(db,"SELECT * FROM job WHERE id=? FOR UPDATE",jobId);
        if(Rows.number(rental,"version")!=request.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다.");
        if(!"FAILED".equals(job.get("status"))||Rows.number(job,"desired_revision")!=Rows.number(rental,"desired_revision"))
            throw ApiProblem.conflict("JOB_NOT_RETRYABLE","현재 의도에 해당하는 실패 작업만 재시도할 수 있습니다.");
        if(Set.of("RETURNED","CANCELLED").contains(rental.get("status")))throw ApiProblem.conflict("INVALID_STATE","종료된 계약 작업은 재시도할 수 없습니다.");
        boolean needsCredential=Set.of("PREPARE","RESET_CREDENTIAL").contains(job.get("type"))&&
            db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='PENDING'",Integer.class,rental.get("id"))==0;
        if(needsCredential&&("RESET_CREDENTIAL".equals(job.get("type"))||db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='ACTIVE'",Integer.class,rental.get("id"))==0))credentials.createPending(Rows.number(rental,"id"));
        db.update("UPDATE job SET status='PENDING',step='QUEUED',worker_id=NULL,locked_until=NULL,retry_count=retry_count+1,next_run_at=?,reason_code=NULL,result_payload_hash=NULL,result_response=NULL,credential_step_hash=NULL WHERE id=?",Rows.ts(clock.instant()),jobId);
        audit.event(Rows.number(rental,"id"),jobId,actor.memberId(),requestId,rental.get("status").toString(),rental.get("status").toString(),"ADMIN_JOB_RETRY",request.reason());
        return Map.of("jobId",jobId,"status","PENDING","attempt",Rows.number(job,"retry_count")+1);
    }
    public Object events(Actor actor,int limit) {
        actor.requireAdmin();if(limit<1||limit>200)throw ApiProblem.invalid("limit은 1–200이어야 합니다.");
        return db.queryForList("SELECT * FROM rental_event ORDER BY id DESC LIMIT ?",limit).stream().map(Rows::api).toList();
    }
    @Transactional public Object prepareNow(Actor actor,long id,Requests.PrepareNow input,String requestId) {
        return rentals.prepareNow(id,actor,input,requestId);
    }
    @Transactional public Object startNow(Actor actor,long id,Requests.PrepareNow input,String requestId) {
        return rentals.startNow(id,actor,input,requestId);
    }
}
