package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RentalService {
    private final JdbcTemplate db;private final ReservationCompiler compiler;private final AuditService audit;
    private final CredentialService credentials;private final JsonCodec json;private final Clock clock;
    private final long minimumSwitch,prepareLead,maximumAge;
    private final Set<Long> reservationOpenServers;
    private final ImmediateReservationPolicy immediate;
    private final String sshHost;private final int sshPort;private final RealRentalPolicy realRentals;private final RealTestMode mode;
    public RentalService(JdbcTemplate db,ReservationCompiler compiler,AuditService audit,CredentialService credentials,
        JsonCodec json,Clock clock,@Value("${allday.minimum-switch-seconds}") long minimumSwitch,
        @Value("${allday.prepare-lead-seconds:3600}") long prepareLead,
        @Value("${allday.observation-max-age-seconds}") long maximumAge,
        @Value("${allday.public-ssh-host}") String sshHost,@Value("${allday.public-ssh-port}") int sshPort,RealRentalPolicy realRentals,RealTestMode mode,
        @Value("${allday.reservation-open-server-ids:}") String reservationOpenServers,ImmediateReservationPolicy immediate) {
        this.db=db;this.compiler=compiler;this.audit=audit;this.credentials=credentials;this.json=json;this.clock=clock;
        if(minimumSwitch<0||prepareLead<0||maximumAge<1) throw new IllegalStateException("시간 정책 설정을 확인하세요.");
        this.minimumSwitch=minimumSwitch;this.prepareLead=prepareLead;this.maximumAge=maximumAge;this.sshHost=sshHost;this.sshPort=sshPort;this.realRentals=realRentals;this.mode=mode;
        var open=new HashSet<Long>();
        if(!reservationOpenServers.isBlank())for(String item:reservationOpenServers.split(",",-1)) {
            try {long id=Long.parseLong(item.trim());if(id<=0)throw new NumberFormatException();open.add(id);}
            catch(NumberFormatException e){throw new IllegalArgumentException("예약 접수 허용 장비 ID는 양의 정수여야 합니다.");}
        }
        this.reservationOpenServers=Set.copyOf(open);
        this.immediate=immediate;
    }
    Map<String,Object> server(long id,boolean lock) {
        return Rows.one(db,"SELECT * FROM server_node WHERE id=?"+(lock?" FOR UPDATE":""),id);
    }
    Map<String,Object> activeServer(long id,boolean lock) {
        var node=server(id,lock);if(node.get("deleted_at")!=null||(mode.enabled&&"DEMO".equals(node.get("inventory_source"))))throw ApiProblem.notFound();return node;
    }
    Map<String,Object> serverView(Map<String,Object> row) {
        var selected=new LinkedHashMap<String,Object>();
        for(String key:List.of("id","category_id","display_name","node_name","cpu_model","memory_gb","gpu_model","gpu_enabled","status","observed_status","observed_at","inventory_source","version"))selected.put(key,row.get(key));
        var out=Rows.api(selected);out.put("reservationAllowed",bookable(row));return out;
    }
    Map<String,Object> rentalView(Map<String,Object> row) {
        var out=Rows.api(row);if(out.get("serverSnapshot")!=null)out.put("serverSnapshot",json.read(out.get("serverSnapshot").toString()));return out;
    }
    boolean bookable(Map<String,Object> server) {
        if(server.get("deleted_at")==null&&reservationOpenServers.contains(Rows.number(server,"id"))&&Set.of("UNKNOWN","AVAILABLE").contains(server.get("status")))return true;
        return executionReady(server);
    }
    boolean executionReady(Map<String,Object> server) {
        if(server.get("deleted_at")!=null||!"AVAILABLE".equals(server.get("status")))return false;
        if("DEMO".equals(server.get("inventory_source")))return true;
        var observed=Rows.instant(server,"observed_at");
        return "AVAILABLE".equals(server.get("observed_status"))&&observed!=null&&!observed.isBefore(clock.instant().minusSeconds(maximumAge))&&!observed.isAfter(clock.instant().plusSeconds(5));
    }
    public List<Map<String,Object>> categories() {
        if(mode.enabled)return db.queryForList("SELECT c.id,c.name,c.description,c.version FROM category c WHERE c.deleted_at IS NULL AND EXISTS(SELECT 1 FROM server_node s WHERE s.category_id=c.id AND s.deleted_at IS NULL AND s.inventory_source<>'DEMO') ORDER BY c.id").stream().map(Rows::api).toList();
        return db.queryForList("SELECT id,name,description,version FROM category WHERE deleted_at IS NULL ORDER BY id").stream().map(Rows::api).toList();
    }
    public List<Map<String,Object>> servers(Long categoryId) {
        String select="SELECT id,category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,gpu_enabled,status,observed_status,observed_at,inventory_source,version FROM server_node";
        String visibility=mode.enabled?" AND inventory_source<>'DEMO'":"";
        var rows=categoryId==null?db.queryForList(select+" WHERE deleted_at IS NULL"+visibility+" ORDER BY id"):db.queryForList(select+" WHERE deleted_at IS NULL"+visibility+" AND category_id=? ORDER BY id",categoryId);
        return rows.stream().map(row->{var api=Rows.api(row);api.put("reservationAllowed",bookable(row));return api;}).toList();
    }
    public Map<String,Object> availability(long serverId,Instant from,Instant to) {
        if(from==null||to==null||!from.isBefore(to)||Duration.between(from,to).compareTo(Duration.ofDays(63))>0)
            throw ApiProblem.invalid("조회 구간은 시작보다 종료가 늦고 63일 이하여야 합니다.");
        var s=activeServer(serverId,false);var serverNow=clock.instant();
        var slots=db.queryForList("SELECT start_at,end_at,blocked_start,blocked_end,state FROM rental_slot WHERE server_id=? AND state<>'CANCELLED' AND blocked_start<=? AND blocked_end>=? ORDER BY start_at",serverId,Rows.ts(to.plusSeconds(minimumSwitch)),Rows.ts(from.minusSeconds(minimumSwitch)));
        return Map.of("serverId",serverId,"from",from,"to",to,"reservationAllowed",bookable(s),"inventorySource",s.get("inventory_source"),
            "blockedSlots",slots.stream().map(Rows::api).toList(),"minimumSwitchSeconds",minimumSwitch,"prepareLeadSeconds",prepareLead,"serverNow",serverNow,"reservationHorizonAt",serverNow.atZone(ReservationCompiler.ZONE).plusMonths(2).toInstant());
    }
    List<Map<String,Object>> conflicts(long serverId,List<Requests.Slot> slots) {
        return conflicts(serverId,slots,0);
    }
    List<Map<String,Object>> conflicts(long serverId,List<Requests.Slot> slots,long excludedRentalId) {
        Instant first=slots.getFirst().startAt().minusSeconds(minimumSwitch);
        Instant last=slots.getLast().endAt().plusSeconds(minimumSwitch);
        var existing=db.queryForList("SELECT blocked_start,blocked_end FROM rental_slot WHERE server_id=? AND rental_id<>? AND state<>'CANCELLED' AND blocked_start<=? AND blocked_end>=? ORDER BY blocked_start",
            serverId,excludedRentalId,Rows.ts(last),Rows.ts(first));
        var result=new ArrayList<Map<String,Object>>();
        for(var candidate:slots) for(var row:existing) {
            Instant start=Rows.instant(row,"blocked_start"),end=Rows.instant(row,"blocked_end");
            boolean overlap=candidate.startAt().isBefore(end)&&candidate.endAt().isAfter(start);
            boolean shortBefore=!candidate.endAt().isAfter(start)&&Duration.between(candidate.endAt(),start).getSeconds()<minimumSwitch;
            boolean shortAfter=!candidate.startAt().isBefore(end)&&Duration.between(end,candidate.startAt()).getSeconds()<minimumSwitch;
            if(overlap||shortBefore||shortAfter) result.add(Map.of("code",overlap?"SLOT_CONFLICT":"INSUFFICIENT_SWITCH_TIME",
                "candidate",candidate,"blockedStart",start,"blockedEnd",end));
        }
        return result;
    }
    public Map<String,Object> preview(Actor actor,Requests.Reservation request) {
        var slots=immediate.slots(actor,request.serverId(),compiler.compile(request));
        var s=activeServer(request.serverId(),false);
        realRentals.require(actor,s);
        var conflicts=new ArrayList<>(conflicts(request.serverId(),slots));
        if(hasOpenRental(actor.memberId())) conflicts.add(0,Map.of("code","MEMBER_RENTAL_LIMIT"));
        return Map.of("serverId",request.serverId(),"slots",slots,"canReserve",bookable(s)&&conflicts.isEmpty(),"conflicts",conflicts,
            "serverAvailable",bookable(s),"horizonPolicy","CALENDAR_TWO_MONTHS","minimumSwitchSeconds",minimumSwitch);
    }
    private boolean hasOpenRental(long memberId) {
        return db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental WHERE member_id=? AND status NOT IN ('RETURNED','CANCELLED'))",
            Boolean.class,memberId);
    }

    @Transactional
    public IdempotencyService.Result create(Actor actor,Requests.Reservation request,String requestId) {
        actor.requireRentalEligibility();
        var s=activeServer(request.serverId(),true);var slots=immediate.slots(actor,request.serverId(),compiler.compile(request));
        realRentals.require(actor,s);
        if(hasOpenRental(actor.memberId())) throw ApiProblem.conflict("MEMBER_RENTAL_LIMIT",ApiErrors.MEMBER_RENTAL_LIMIT_MESSAGE);
        if(!bookable(s))throw new ApiProblem(503,"SERVER_UNAVAILABLE","서버 상태가 예약 가능하지 않거나 최신 관측을 확인할 수 없습니다.");
        var conflicts=conflicts(request.serverId(),slots);
        if(!conflicts.isEmpty())throw ApiProblem.conflict(conflicts.getFirst().get("code").toString(),"다른 예약과 겹치거나 VM 전환 준비 시간이 부족합니다.");
        long id=db.queryForObject("INSERT INTO rental(member_id,server_id,contract_start,contract_end,purpose) VALUES (?,?,?,?,?) RETURNING id",Long.class,
            actor.memberId(),request.serverId(),Rows.ts(slots.getFirst().startAt()),Rows.ts(slots.getLast().endAt()),request.purpose().trim());
        for(var slot:slots)db.update("INSERT INTO rental_slot(rental_id,server_id,start_at,end_at,blocked_start,blocked_end) VALUES (?,?,?,?,?,?)",
            id,request.serverId(),Rows.ts(slot.startAt()),Rows.ts(slot.endAt()),Rows.ts(slot.startAt()),Rows.ts(slot.endAt()));
        credentials.createPending(id);
        long jobId=createJob(id,request.serverId(),0,"PREPARE",slots.getFirst().startAt().minusSeconds(prepareLead));
        audit.event(id,jobId,actor.memberId(),requestId,null,"RESERVED","RESERVATION_CREATED");
        notify(actor.memberId(),"rental:"+id+":reserved",Map.of("rentalId",id,"status","RESERVED"));
        return new IdempotencyService.Result(201,Map.of("id",id,"status","RESERVED","version",0,"desiredRevision",0,"jobId",jobId,"slots",slots));
    }
    @Transactional public Object startNow(long id,Actor actor,Requests.PrepareNow input,String requestId) {
        actor.requireAdmin();var rental=lockOwned(id,actor,true);
        long serverId=Rows.number(rental,"server_id"),memberId=Rows.number(rental,"member_id");
        if(serverId!=input.serverId()||memberId!=input.memberId())throw ApiProblem.conflict("RESERVATION_TARGET_MISMATCH","예약 소유자와 장비를 다시 확인하세요.");
        if(Rows.number(rental,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다. 다시 조회하세요.");
        if(!"READY".equals(rental.get("status")))throw ApiProblem.conflict("PREPARATION_NOT_READY","준비 완료된 예약만 이용 시작을 앞당길 수 있습니다.");
        if(!executionReady(activeServer(serverId,false)))throw new ApiProblem(503,"SERVER_UNAVAILABLE","실제 장비의 최신 정상 관측이 필요합니다.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM job WHERE server_id=? AND status='RUNNING')",Boolean.class,serverId))throw ApiProblem.conflict("SERVER_JOB_RUNNING","진행 중인 장비 작업이 있습니다.");
        var now=clock.instant();var slots=db.queryForList("SELECT * FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' ORDER BY start_at FOR UPDATE",id);
        if(slots.isEmpty()||!now.isBefore(Rows.instant(slots.getFirst(),"start_at")))throw ApiProblem.conflict("START_WINDOW_CLOSED","첫 이용 구간이 시작되기 전이어야 합니다.");
        var first=slots.getFirst();var blocked=Rows.instant(first,"blocked_start");var effective=blocked.isBefore(now)?blocked:now;
        var candidates=new ArrayList<Requests.Slot>();
        for(var slot:slots)candidates.add(new Requests.Slot(Rows.instant(slot,"blocked_start"),Rows.instant(slot,"blocked_end")));
        candidates.set(0,new Requests.Slot(effective,Rows.instant(first,"blocked_end")));
        var conflicts=conflicts(serverId,candidates,id);
        if(!conflicts.isEmpty())throw ApiProblem.conflict(conflicts.getFirst().get("code").toString(),"즉시 이용 구간이 다른 예약과 겹치거나 전환 시간이 부족합니다.");
        var jobs=db.queryForList("SELECT * FROM job WHERE rental_id=? AND slot_id=? AND desired_revision=? AND type='START' FOR UPDATE",id,first.get("id"),rental.get("desired_revision"));
        if(jobs.size()!=1||!"PENDING".equals(jobs.getFirst().get("status")))throw ApiProblem.conflict("START_NOT_PENDING","현재 첫 구간의 시작 대기 작업 한 건이 필요합니다.");
        if(!db.queryForObject("SELECT EXISTS(SELECT 1 FROM vm_instance WHERE rental_id=? AND state IN ('RUNNING','STOPPED')) AND EXISTS(SELECT 1 FROM vm_credential WHERE rental_id=? AND status='ACTIVE')",Boolean.class,id,id))throw ApiProblem.conflict("PREPARATION_NOT_READY","실제 VM과 적용 완료된 암호가 필요합니다.");
        var job=jobs.getFirst();
        db.update("UPDATE rental_slot SET start_at=?,blocked_start=? WHERE id=?",Rows.ts(now),Rows.ts(effective),first.get("id"));
        db.update("UPDATE rental SET contract_start=?,version=version+1 WHERE id=?",Rows.ts(now),id);
        db.update("UPDATE job SET next_run_at=? WHERE id=?",Rows.ts(now),job.get("id"));
        audit.event(id,Rows.number(job,"id"),actor.memberId(),requestId,"READY","READY","ADMIN_START_NOW",input.reason());
        return Map.of("id",id,"memberId",memberId,"serverId",serverId,"status","READY","version",Rows.number(rental,"version")+1,"jobId",Rows.number(job,"id"),"jobStatus","PENDING","startAt",now);
    }
    long createJob(long rentalId,long serverId,long revision,String type,Instant nextRun) {
        return db.queryForObject("INSERT INTO job(server_id,rental_id,intent_key,type,desired_revision,priority,next_run_at) VALUES (?,?,?,?,?,?,?) RETURNING id",Long.class,
            serverId,rentalId,"rental:"+rentalId+":"+type.toLowerCase(Locale.ROOT)+":r"+revision,type,revision,type.equals("RETURN")?100:type.equals("RESET_CREDENTIAL")?60:40,Rows.ts(nextRun));
    }
    @Transactional public Object prepareNow(long id,Actor actor,Requests.PrepareNow input,String requestId) {
        actor.requireAdmin();var rental=lockOwned(id,actor,true);
        long serverId=Rows.number(rental,"server_id"),memberId=Rows.number(rental,"member_id");
        if(serverId!=input.serverId()||memberId!=input.memberId())throw ApiProblem.conflict("RESERVATION_TARGET_MISMATCH","예약 소유자와 장비를 다시 확인하세요.");
        if(Rows.number(rental,"version")!=input.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다. 다시 조회하세요.");
        var node=activeServer(serverId,false);
        if(!bookable(node))throw new ApiProblem(503,"SERVER_UNAVAILABLE","예약 접수가 허용된 장비만 준비를 앞당길 수 있습니다.");
        if(!"RESERVED".equals(rental.get("status"))||!noResources(id))throw ApiProblem.conflict("PREPARATION_ALREADY_STARTED","아직 실행되지 않은 예약만 준비를 앞당길 수 있습니다.");
        var now=clock.instant();
        var slots=db.queryForList("SELECT * FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' ORDER BY start_at FOR UPDATE",id);
        if(slots.isEmpty()||!now.isBefore(Rows.instant(slots.getFirst(),"start_at")))throw ApiProblem.conflict("PREPARATION_WINDOW_CLOSED","이용 시작 전 예약만 준비를 앞당길 수 있습니다.");
        var jobs=db.queryForList("SELECT * FROM job WHERE rental_id=? AND server_id=? AND desired_revision=? AND type='PREPARE' FOR UPDATE",id,serverId,rental.get("desired_revision"));
        if(jobs.size()!=1||!"PENDING".equals(jobs.getFirst().get("status")))throw ApiProblem.conflict("PREPARE_NOT_PENDING","현재 예약의 준비 대기 작업 한 건이 필요합니다.");
        var job=jobs.getFirst();
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM job WHERE server_id=? AND rental_id<>? AND status='RUNNING') OR EXISTS(SELECT 1 FROM vmid_allocation WHERE server_id=? AND kind='RENTAL' AND rental_id<>? AND released_at IS NULL) OR EXISTS(SELECT 1 FROM ip_lease WHERE server_id=? AND rental_id<>? AND released_at IS NULL) OR EXISTS(SELECT 1 FROM vm_instance WHERE server_id=? AND rental_id<>? AND state<>'DELETED') OR EXISTS(SELECT 1 FROM gpu_lease WHERE server_id=? AND (state='UNKNOWN' OR (owner_rental_id IS NOT NULL AND owner_rental_id<>?)))",Boolean.class,serverId,id,serverId,id,serverId,id,serverId,id,serverId,id))
            throw ApiProblem.conflict("SERVER_RESOURCES_IN_USE","다른 예약의 실행 작업과 자원 소유를 먼저 확인하세요.");
        var first=slots.getFirst();var blockedStart=Rows.instant(first,"blocked_start");
        var effectiveStart=blockedStart.isBefore(now)?blockedStart:now;
        var candidates=new ArrayList<Requests.Slot>();
        for(var slot:slots)candidates.add(new Requests.Slot(Rows.instant(slot,"blocked_start"),Rows.instant(slot,"blocked_end")));
        candidates.set(0,new Requests.Slot(effectiveStart,Rows.instant(first,"blocked_end")));
        var conflicts=conflicts(serverId,candidates,id);
        if(!conflicts.isEmpty())throw ApiProblem.conflict(conflicts.getFirst().get("code").toString(),"즉시 준비 구간이 다른 예약과 겹치거나 VM 전환 준비 시간이 부족합니다.");
        boolean changed=Rows.instant(job,"next_run_at").isAfter(now)||blockedStart.isAfter(now);
        if(changed) {
            // Reserve the earlier preparation interval without changing actual usage times.
            db.update("UPDATE rental_slot SET blocked_start=? WHERE id=?",Rows.ts(effectiveStart),first.get("id"));
            db.update("UPDATE job SET next_run_at=LEAST(next_run_at,?) WHERE id=?",Rows.ts(now),job.get("id"));
            db.update("UPDATE rental SET version=version+1 WHERE id=?",id);
            audit.event(id,Rows.number(job,"id"),actor.memberId(),requestId,"RESERVED","RESERVED","ADMIN_PREPARE_NOW",input.reason());
        }
        return Map.of("id",id,"memberId",memberId,"serverId",serverId,"status","RESERVED","version",Rows.number(rental,"version")+(changed?1:0),"jobId",Rows.number(job,"id"),"jobStatus","PENDING","nextRunAt",Rows.instant(job,"next_run_at").isBefore(now)?Rows.instant(job,"next_run_at"):now,"startAt",Rows.instant(first,"start_at"),"preparationRequested",true);
    }
    void notify(long member,String key,Object payload) {
        db.update("INSERT INTO notification_outbox(member_id,event_key,channel,payload,next_run_at,delivered_at) VALUES (?,?,'IN_APP',?::jsonb,?,?) ON CONFLICT (event_key) DO NOTHING",
            member,key,json.write(payload),Rows.ts(clock.instant()),Rows.ts(clock.instant()));
    }
    Map<String,Object> owned(long id,Actor actor,boolean lock,boolean adminAllowed) {
        var row=Rows.one(db,"SELECT * FROM rental WHERE id=?"+(lock?" FOR UPDATE":""),id);
        if(Rows.number(row,"member_id")!=actor.memberId()&&!(adminAllowed&&actor.admin()))throw new ApiProblem(403,"FORBIDDEN","본인 계약만 이용할 수 있습니다.");
        return row;
    }
    Map<String,Object> lockOwned(long id,Actor actor,boolean adminAllowed) {
        var row=owned(id,actor,false,adminAllowed);server(Rows.number(row,"server_id"),true);return owned(id,actor,true,adminAllowed);
    }
    public List<Map<String,Object>> list(Actor actor,boolean all,int limit,int offset) {
        if(limit<1||limit>100||offset<0)throw ApiProblem.invalid("limit은 1–100, offset은 0 이상이어야 합니다.");
        if(all)actor.requireAdmin();
        String select="SELECT r.*,r.server_snapshot->>'displayName' AS server_name FROM rental r JOIN server_node s ON s.id=r.server_id";
        var rows=all?db.queryForList(select+" ORDER BY r.created_at DESC,r.id DESC LIMIT ? OFFSET ?",limit,offset):
            db.queryForList(select+" WHERE r.member_id=? ORDER BY r.created_at DESC,r.id DESC LIMIT ? OFFSET ?",actor.memberId(),limit,offset);
        return rows.stream().map(this::rentalView).toList();
    }
    public Map<String,Object> detail(long id,Actor actor) {
        var row=owned(id,actor,false,true);var result=rentalView(row);
        result.put("slots",db.queryForList("SELECT id,start_at,end_at,blocked_start,blocked_end,state FROM rental_slot WHERE rental_id=? ORDER BY start_at",id).stream().map(Rows::api).toList());
        result.put("jobs",db.queryForList("SELECT id,type,status,step,desired_revision,retry_count,next_run_at,reason_code FROM job WHERE rental_id=? ORDER BY id DESC LIMIT 100",id).stream().map(Rows::api).toList());
        var vm=db.queryForList("SELECT vmid,ip,state,observed_at,host_key_fingerprint FROM vm_instance WHERE rental_id=?",id);
        result.put("vm",vm.isEmpty()?null:Rows.api(vm.getFirst()));return result;
    }
    boolean noResources(long id) {
        return Boolean.TRUE.equals(db.queryForObject("SELECT NOT EXISTS(SELECT 1 FROM vm_instance WHERE rental_id=?) AND NOT EXISTS(SELECT 1 FROM resource_manifest WHERE rental_id=?) AND NOT EXISTS(SELECT 1 FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL) AND NOT EXISTS(SELECT 1 FROM ip_lease WHERE rental_id=? AND released_at IS NULL) AND NOT EXISTS(SELECT 1 FROM gpu_lease WHERE owner_rental_id=?) AND NOT EXISTS(SELECT 1 FROM job WHERE rental_id=? AND (status NOT IN ('PENDING','CANCELLED','SUPERSEDED') OR operation_id IS NOT NULL))",Boolean.class,id,id,id,id,id,id));
    }
    @Transactional
    public IdempotencyService.Result finish(long id,Actor actor,Requests.Change request,String requestId,boolean cancel,boolean forced) {
        if(forced) {actor.requireAdmin();if(request.reason()==null||request.reason().isBlank())throw ApiProblem.invalid("강제 반납 사유가 필요합니다.");}
        var row=lockOwned(id,actor,forced);String before=row.get("status").toString();
        if(Set.of("RETURNED","CANCELLED").contains(before))return new IdempotencyService.Result(200,Map.of("id",id,"status",before,"version",row.get("version")));
        if(before.equals("RETURNING"))return new IdempotencyService.Result(202,Map.of("id",id,"status","RETURNING","version",row.get("version")));
        if(Rows.number(row,"version")!=request.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다. 다시 조회하세요.");
        long revision=Rows.number(row,"desired_revision")+1;long version=Rows.number(row,"version")+1;
        boolean immediate=cancel&&before.equals("RESERVED")&&noResources(id);
        String after=immediate?"CANCELLED":"RETURNING";String reason=forced?"ADMIN_FORCE_RETURN":cancel?"USER_CANCEL":"USER_RETURN";
        db.update("UPDATE rental SET status=?,retained_reason=NULL,desired_revision=?,version=?,reason_code=? WHERE id=?",after,revision,version,reason,id);
        db.update("UPDATE job SET status='SUPERSEDED',step='INVALIDATED' WHERE rental_id=? AND status='PENDING'",id);
        Long jobId=null;
        if(immediate) {
            db.update("UPDATE rental_slot SET state='CANCELLED' WHERE rental_id=?",id);
            db.update("UPDATE vm_credential SET status='RETIRED' WHERE rental_id=?",id);
        }else jobId=createJob(id,Rows.number(row,"server_id"),revision,"RETURN",clock.instant());
        audit.event(id,jobId,actor.memberId(),requestId,before,after,reason,request.reason());
        notify(Rows.number(row,"member_id"),"rental:"+id+":r"+revision,Map.of("rentalId",id,"status",after));
        var response=new LinkedHashMap<String,Object>();response.put("id",id);response.put("status",after);response.put("version",version);response.put("desiredRevision",revision);response.put("jobId",jobId);
        return new IdempotencyService.Result(immediate?200:202,response);
    }
    @Transactional
    public IdempotencyService.Result reset(long id,Actor actor,Requests.Change request,String requestId) {
        var row=lockOwned(id,actor,false);String status=row.get("status").toString();
        if(!Set.of("READY","ACTIVE","RETAINED").contains(status))throw ApiProblem.conflict("INVALID_STATE","VM 준비 이후 계약에서만 암호 재설정을 요청할 수 있습니다.");
        if(Rows.number(row,"version")!=request.expectedVersion())throw ApiProblem.conflict("VERSION_CONFLICT","계약 상태가 변경되었습니다.");
        if(db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='PENDING'",Integer.class,id)>0)
            throw ApiProblem.conflict("RESET_IN_PROGRESS","암호 적용 작업이 진행 중입니다.");
        long revision=Rows.number(row,"desired_revision")+1,version=Rows.number(row,"version")+1;
        int credentialVersion=credentials.createPending(id);
        db.update("UPDATE rental SET desired_revision=?,version=? WHERE id=?",revision,version,id);
        db.update("UPDATE job SET status='SUPERSEDED',step='INVALIDATED' WHERE rental_id=? AND status='PENDING'",id);
        long jobId=createJob(id,Rows.number(row,"server_id"),revision,"RESET_CREDENTIAL",clock.instant());
        audit.event(id,jobId,actor.memberId(),requestId,status,status,"CREDENTIAL_RESET_REQUESTED");
        return new IdempotencyService.Result(202,Map.of("id",id,"status",status,"version",version,"jobId",jobId,"credentialVersion",credentialVersion));
    }
    @Transactional
    public Map<String,Object> credential(long id,Actor actor,String requestId) {
        var row=owned(id,actor,false,false);
        if(Set.of("RETURNING","RETURNED","CANCELLED").contains(row.get("status")))throw new ApiProblem(403,"CREDENTIAL_BLOCKED","반납·취소 계약의 암호는 조회할 수 없습니다.");
        if("CREDENTIAL_APPLY_UNKNOWN".equals(row.get("reason_code")))throw ApiProblem.conflict("CREDENTIAL_STATE_UNKNOWN","실제 VM의 암호 적용 상태를 확인할 때까지 조회를 보류합니다.");
        var active=db.queryForObject("SELECT count(*) FROM vm_credential WHERE rental_id=? AND status='ACTIVE'",Integer.class,id);
        if(active==0)throw ApiProblem.conflict("CREDENTIAL_NOT_READY","VM에서 적용 완료가 확인된 암호가 없습니다.");
        var result=credentials.active(id);audit.event(id,null,actor.memberId(),requestId,row.get("status").toString(),row.get("status").toString(),"CREDENTIAL_REVEALED");return result;
    }
    public Map<String,Object> connection(long id,Actor actor) {
        var row=owned(id,actor,false,false);var vm=db.queryForList("SELECT * FROM vm_instance WHERE rental_id=?",id);
        if("CREDENTIAL_APPLY_UNKNOWN".equals(row.get("reason_code")))return Map.of("available",false,"reason","CREDENTIAL_STATE_UNKNOWN");
        boolean inSlot=Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,id,Rows.ts(clock.instant()),Rows.ts(clock.instant())));
        if(!"ACTIVE".equals(row.get("status"))||!inSlot||vm.isEmpty())return Map.of("available",false,"reason","OUTSIDE_ACTIVE_SLOT_OR_VM_NOT_READY");
        var v=vm.getFirst();Instant observed=Rows.instant(v,"observed_at");
        if(!"RUNNING".equals(v.get("state"))||!Boolean.TRUE.equals(v.get("access_enabled"))||observed==null||observed.isBefore(clock.instant().minusSeconds(maximumAge))||observed.isAfter(clock.instant().plusSeconds(5)))
            return Map.of("available",false,"reason","VM_OBSERVATION_UNKNOWN_OR_ACCESS_BLOCKED");
        var node=server(Rows.number(row,"server_id"),false);
        String host=node.get("public_ssh_host")==null?sshHost:node.get("public_ssh_host").toString();
        int port=node.get("public_ssh_port")==null?sshPort:((Number)node.get("public_ssh_port")).intValue();
        if(host.isBlank()||!host.matches("[A-Za-z0-9.-]+")||port<1||port>65535||v.get("host_key_fingerprint")==null)
            return Map.of("available",false,"reason","CONNECTION_ROUTE_NOT_CONFIGURED");
        return Map.of("available",true,"host",host,"port",port,"username","student","hostKeyFingerprint",v.get("host_key_fingerprint"),
            "sshCommand","ssh -p "+port+" student@"+host,
            "jupyterTunnelCommand","ssh -N -L 8888:127.0.0.1:8888 -p "+port+" student@"+host,
            "jupyterUrl","http://127.0.0.1:8888");
    }
    public List<Map<String,Object>> events(long id,Actor actor) {
        owned(id,actor,false,true);
        return db.queryForList("SELECT id,job_id,operation_id,upid,actor_id,source,request_id,before_status,after_status,step,reason_code,created_at FROM rental_event WHERE rental_id=? ORDER BY id DESC LIMIT 200",id).stream().map(Rows::api).toList();
    }
    public List<Map<String,Object>> verifications(long id,Actor actor) {
        owned(id,actor,false,true);var results=db.queryForList("SELECT * FROM return_verification WHERE rental_id=? ORDER BY id DESC LIMIT 50",id);
        return results.stream().map(row->{var api=Rows.api(row);api.put("items",db.queryForList("SELECT kind,passed,evidence,checked_at FROM verification_item WHERE verification_id=? ORDER BY kind",row.get("id")).stream().map(item->{var out=Rows.api(item);out.put("evidence",json.read(item.get("evidence").toString()));return out;}).toList());return api;}).toList();
    }
}
