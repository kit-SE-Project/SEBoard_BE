package kr.ac.capdi.allday;

import java.time.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"allday.scheduler-enabled=false","allday.cors-origin=http://localhost:3000,http://127.0.0.1:3001","allday.real-rental-member-ids=1001,1002"})
@Import(ApiIntegrationTest.FixedClock.class)
@EnabledIfEnvironmentVariable(named="DB_URL",matches=".*[/]capdi_test")
class ApiIntegrationTest {
    static final Instant NOW=Instant.parse("2026-10-06T03:00:00Z");
    static class MutableClock extends Clock {
        private volatile Instant at=NOW;
        void set(Instant value){at=value;}
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return at;}
    }
    @TestConfiguration static class FixedClock {@Bean @Primary Clock testClock(){return new MutableClock();}}
    @Autowired Clock clock;
    void time(Instant at){((MutableClock)clock).set(at);}
    Instant now(){return clock.instant();}
    @Autowired TestRestTemplate http;
    @Autowired DevSeed seed;
    @Autowired JsonCodec json;
    JdbcTemplate owner;
    long server,otherServer;
    String student=System.getenv("DEV_STUDENT_ONE_TOKEN"),other=System.getenv("DEV_STUDENT_TWO_TOKEN"),admin=System.getenv("DEV_ADMIN_TOKEN"),worker=System.getenv("WORKER_TOKEN");

    @BeforeAll static void clearContractCapture() throws IOException {
        Files.createDirectories(Path.of(".local"));Files.deleteIfExists(Path.of(".local/contract-responses.jsonl"));
    }
    void capture(String method,String path,int status,Object body) {
        if(path.contains("credential"))return;
        synchronized(ApiIntegrationTest.class) {
            try {Files.writeString(Path.of(".local/contract-responses.jsonl"),json.write(new LinkedHashMap<String,Object>(){{put("method",method);put("path",path);put("status",status);put("body",body);}})+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
            catch(IOException e){throw new UncheckedIOException(e);}
        }
    }
    @BeforeEach void resetDatabase() {
        time(NOW);
        var ds=new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_MIGRATION_USER"),System.getenv("DB_MIGRATION_PASSWORD"));owner=new JdbcTemplate(ds);
        assertThat(owner.queryForObject("SELECT current_database()",String.class)).isEqualTo("capdi_test");
        owner.execute("TRUNCATE category,external_subject_binding RESTART IDENTITY CASCADE");seed.run(null);
        server=owner.queryForObject("SELECT id FROM server_node WHERE node_name='demo-node-1'",Long.class);
        otherServer=owner.queryForObject("SELECT id FROM server_node WHERE node_name='demo-node-2'",Long.class);
        owner.execute("REVOKE INSERT,UPDATE,DELETE ON flyway_schema_history FROM capdi_app,capdi_worker");
    }
    ResponseEntity<Map> request(HttpMethod method,String path,String token,Object body,String key) {
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);if(token!=null)headers.setBearerAuth(token);if(key!=null)headers.set("Idempotency-Key",key);
        var result=http.exchange(path,method,new HttpEntity<>(body,headers),Map.class);
        capture(method.name(),path,result.getStatusCode().value(),result.getBody());return result;
    }
    ResponseEntity<Map> post(String path,String token,Object body,String key){return request(HttpMethod.POST,path,token,body,key);}
    ResponseEntity<Map> get(String path,String token){return request(HttpMethod.GET,path,token,null,null);}
    Map<String,Object> reservation(long s) {return Map.of("serverId",s,"purpose","통합 시험","slots",List.of(Map.of("startAt","2026-10-08T00:00:00Z","endAt","2026-10-08T06:00:00Z")));}
    long reserve() {var response=post("/api/v1/rentals",student,reservation(server),UUID.randomUUID().toString());assertThat(response.getStatusCode().value()).isEqualTo(201);return ((Number)response.getBody().get("id")).longValue();}
    long version(long id){return owner.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,id);}
    Map<String,Object> change(long id){return Map.of("expectedVersion",version(id),"reason","시험 사유");}
    long job(long id,String type){return owner.queryForObject("SELECT id FROM job WHERE rental_id=? AND type=? ORDER BY id DESC LIMIT 1",Long.class,id,type);}
    Map claim(long j) {
        owner.update("UPDATE job SET next_run_at=? WHERE id=?",Rows.ts(NOW.minusSeconds(1)),j);
        var result=post("/api/internal/jobs/"+j+"/claim",worker,Map.of("workerId","test-worker"),null);assertThat(result.getStatusCode().value()).isEqualTo(200);return result.getBody();
    }
    void resources(long rental) {
        long lease=owner.queryForObject("INSERT INTO vmid_allocation(vmid,server_id,rental_id,kind,allocated_at) VALUES (2000,?,?,'RENTAL',?) RETURNING id",Long.class,server,rental,Rows.ts(NOW.minusSeconds(60)));
        owner.update("INSERT INTO vm_instance(rental_id,server_id,vmid,vmid_allocation_id,ip,state) VALUES (?,?,2000,?,'192.168.101.10','RUNNING')",rental,server,lease);
        owner.update("INSERT INTO resource_manifest(rental_id,server_id,vmid,volume_ids,snippet_ids,ingress_rule_ids) VALUES (?,?,2000,'[\"volume-1\"]','[\"snippet-1\"]','[\"ingress-1\"]')",rental,server);
        owner.update("INSERT INTO ip_lease(server_id,rental_id,address,allocated_at) VALUES (?,?,'192.168.101.10',?)",server,rental,Rows.ts(NOW.minusSeconds(60)));
        owner.update("UPDATE gpu_lease SET owner_rental_id=?,owner_vmid=2000,state='HELD',generation=7 WHERE server_id=?",rental,server);
    }
    Map<String,Object> verification(long rental,Map claim,Boolean passed) {
        var items=new ArrayList<Map<String,Object>>();
        for(String kind:List.of("ACCESS_BLOCKED","VM_ABSENT","VOLUMES_ABSENT","SNIPPETS_ABSENT","INGRESS_ABSENT")) {
            var item=new LinkedHashMap<String,Object>();item.put("kind",kind);item.put("passed",kind.equals("SNIPPETS_ABSENT")?passed:Boolean.TRUE);
            Object evidence=switch(kind){case "VM_ABSENT"->Map.of("vmid",2000);case "VOLUMES_ABSENT"->Map.of("checkedIds",List.of("volume-1"));case "SNIPPETS_ABSENT"->Map.of("checkedIds",List.of("snippet-1"));case "INGRESS_ABSENT"->Map.of("checkedIds",List.of("ingress-1"));default->Map.of("checked",true);};
            item.put("evidence",evidence);items.add(item);
        }
        return Map.of("workerId","test-worker","desiredRevision",owner.queryForObject("SELECT desired_revision FROM rental WHERE id=?",Long.class,rental),
            "commandId",claim.get("commandId"),"operationId","test-operation-"+claim.get("commandId"),"observedAt",NOW.toString(),"items",items);
    }
    @Test void managementInventoryAndHealthTargetsRequireWorkerIdentity() {
        for(String path:List.of("/api/internal/inventory","/api/internal/health-targets")) {
            assertThat(get(path,null).getStatusCode().value()).isEqualTo(401);
            assertThat(get(path,student).getStatusCode().value()).isEqualTo(401);
            var h=new HttpHeaders();h.setBearerAuth(worker);
            var response=http.exchange(path,HttpMethod.GET,new HttpEntity<>(null,h),List.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            capture("GET",path,200,response.getBody());
            assertThat(response.getBody().toString()).doesNotContain("password","encryptedSecret","workerToken");
        }
    }
    @Test void managementContextIncludesAuthoritativeSlotAndInventorySource() {
        long rental=reserve();Map c=claim(job(rental,"PREPARE"));
        var response=get("/api/internal/jobs/"+c.get("id")+"/context?workerId=test-worker&commandId="+c.get("commandId"),worker);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("inSlot")).isEqualTo(false);
        assertThat((Map)response.getBody().get("accessWindow")).containsEntry("maxLeaseSeconds",0);
        assertThat((Map)response.getBody().get("server")).containsKey("inventorySource");
    }
    @Test void onlyApprovedMembersCanPreviewOrCreateRealEquipmentReservations() {
        configure(server,1000);
        long before=owner.queryForObject("SELECT count(*) FROM rental",Long.class);
        for(String path:List.of("/api/v1/rental-previews","/api/v1/rentals")) {
            var result=post(path,admin,reservation(server),UUID.randomUUID().toString());
            assertThat(result.getStatusCode().value()).isEqualTo(403);
            assertThat(result.getBody().get("code")).isEqualTo("REAL_RENTAL_NOT_APPROVED");
        }
        assertThat(owner.queryForObject("SELECT count(*) FROM rental",Long.class)).isEqualTo(before);
        assertThat(post("/api/v1/rental-previews",student,reservation(server),null).getStatusCode().value()).isEqualTo(200);
    }
    @Test void healthLeaseDeadlineTracksSlotEndCancellationAndGpuOwnership() {
        long id=reserve();resources(id);
        owner.update("UPDATE rental SET status='ACTIVE' WHERE id=?",id);
        owner.update("UPDATE vm_credential SET status='ACTIVE' WHERE rental_id=?",id);
        time(Instant.parse("2026-10-08T05:59:43Z"));
        var headers=new HttpHeaders();headers.setBearerAuth(worker);
        var response=http.exchange("/api/internal/health-targets",HttpMethod.GET,new HttpEntity<>(headers),List.class);
        Map target=(Map)response.getBody().getFirst();Map window=(Map)target.get("accessWindow");
        assertThat(target.get("accessAllowed")).isEqualTo(true);
        assertThat(window.get("maxLeaseSeconds")).isEqualTo(17);
        assertThat(window.get("validUntil")).isEqualTo("2026-10-08T06:00:00Z");
        owner.update("UPDATE gpu_lease SET state='FREE',owner_rental_id=NULL,owner_vmid=NULL WHERE server_id=?",server);
        response=http.exchange("/api/internal/health-targets",HttpMethod.GET,new HttpEntity<>(headers),List.class);
        assertThat(((Map)response.getBody().getFirst()).get("accessAllowed")).isEqualTo(false);
        owner.update("UPDATE rental_slot SET state='CANCELLED' WHERE rental_id=?",id);
        response=http.exchange("/api/internal/health-targets",HttpMethod.GET,new HttpEntity<>(headers),List.class);
        assertThat((Map)((Map)response.getBody().getFirst()).get("accessWindow")).containsEntry("maxLeaseSeconds",0);
    }
    @Test void corsAcceptsNgrokBrowserWarningHeaderBeforeAuthentication() {
        var h=new HttpHeaders();h.setOrigin("http://localhost:3000");h.set("Access-Control-Request-Method","POST");h.set("Access-Control-Request-Headers","authorization,content-type,ngrok-skip-browser-warning");
        var response=http.exchange("/formLogin",HttpMethod.OPTIONS,new HttpEntity<>(null,h),String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getAccessControlAllowHeaders()).contains("ngrok-skip-browser-warning");
    }
    @Test void savesContractSlotsJobAndEncryptedCredentialAtomically() {
        long id=reserve();assertThat(owner.queryForObject("SELECT count(*) FROM rental_slot WHERE rental_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=?",Integer.class,id)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT encrypted_secret FROM vm_credential WHERE rental_id=?",String.class,id)).doesNotContain("password");
        assertThat(get("/api/v1/rentals/"+id,student).getBody().toString()).doesNotContain("encryptedSecret","password");
    }
    @Test void twentyConcurrentConflictingRequestsCommitExactlyOneContract() throws Exception {
        try(var executor=Executors.newFixedThreadPool(20)) {
            List<Callable<Integer>> tasks=new ArrayList<>();for(int i=0;i<20;i++)tasks.add(()->post("/api/v1/rentals",student,reservation(server),UUID.randomUUID().toString()).getStatusCode().value());
            var statuses=new ArrayList<Integer>();for(var f:executor.invokeAll(tasks))statuses.add(f.get());
            assertThat(statuses.stream().filter(s->s==201).count()).isEqualTo(1);assertThat(statuses.stream().filter(s->s==409).count()).isEqualTo(19);
            assertThat(owner.queryForObject("SELECT count(*) FROM rental",Integer.class)).isEqualTo(1);
        }
    }
    @Test void concurrentSameKeyReturnsOriginalResultAndDoesNotDuplicate() throws Exception {
        try(var executor=Executors.newFixedThreadPool(10)) {
            List<Callable<Long>> tasks=new ArrayList<>();for(int i=0;i<10;i++)tasks.add(()->((Number)post("/api/v1/rentals",student,reservation(server),"same-key").getBody().get("id")).longValue());
            var ids=new HashSet<Long>();for(var f:executor.invokeAll(tasks))ids.add(f.get());assertThat(ids).hasSize(1);
            assertThat(owner.queryForObject("SELECT count(*) FROM job",Integer.class)).isEqualTo(1);
        }
    }
    @Test void rejectsSameKeyWithDifferentPayload() {
        assertThat(post("/api/v1/rentals",student,reservation(server),"key").getStatusCode().value()).isEqualTo(201);
        assertThat(post("/api/v1/rentals",student,reservation(otherServer),"key").getStatusCode().value()).isEqualTo(422);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental",Integer.class)).isEqualTo(1);
    }
    @Test void previewDoesNotSaveAndFinalRequestRechecksCompetition() {
        assertThat(post("/api/v1/rental-previews",student,reservation(server),null).getBody().get("canReserve")).isEqualTo(true);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental",Integer.class)).isZero();reserve();
        assertThat(post("/api/v1/rentals",other,reservation(server),"other").getStatusCode().value()).isEqualTo(409);
    }
    @Test void differentServersCanBeReservedAtSameTime() {
        reserve();assertThat(post("/api/v1/rentals",other,reservation(otherServer),"other").getStatusCode().value()).isEqualTo(201);
    }
    @Test void protectsOwnershipAdminRoutesAndWorkerTokenBoundary() {
        long id=reserve();assertThat(get("/api/v1/rentals/"+id,other).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/v1/admin/rentals",student).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/v1/rentals/"+id,null).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/internal/jobs",student).getStatusCode().value()).isEqualTo(401);
        var spoof=new HashMap<>(reservation(server));spoof.put("memberId",1002);
        assertThat(post("/api/v1/rentals",student,spoof,"spoof").getStatusCode().value()).isEqualTo(422);
    }
    @Test void cancelsUnstartedReservationWithoutClaimingVmDeletion() {
        long id=reserve();var result=post("/api/v1/rentals/"+id+"/cancel",student,change(id),"cancel");assertThat(result.getBody().get("status")).isEqualTo("CANCELLED");
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_slot WHERE state='CANCELLED'",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM return_verification",Integer.class)).isZero();
    }
    @Test void rejectsStaleVersionWithoutChangingContract() {
        long id=reserve();var result=post("/api/v1/rentals/"+id+"/return",student,Map.of("expectedVersion",99),"stale");assertThat(result.getStatusCode().value()).isEqualTo(409);
        assertThat(owner.queryForObject("SELECT status FROM rental WHERE id=?",String.class,id)).isEqualTo("RESERVED");
    }
    @Test void unknownVerificationDoesNotReleaseAnyResources() {
        long id=reserve();resources(id);post("/api/v1/rentals/"+id+"/return",student,change(id),"return");long j=job(id,"RETURN");var c=claim(j);
        var result=post("/api/internal/jobs/"+j+"/return-verification",worker,verification(id,c,null),null);
        assertThat(result.getStatusCode().value()).isEqualTo(200);assertThat(result.getBody().get("result")).isEqualTo("UNKNOWN");
        assertThat(owner.queryForObject("SELECT status FROM rental WHERE id=?",String.class,id)).isEqualTo("RETURNING");
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE released_at IS NULL",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM vmid_allocation WHERE released_at IS NULL",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM verification_item WHERE passed IS NULL",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,server)).isEqualTo("HELD");
    }
    @Test void allPassVerificationReturnsAndReleasesResourcesWithIdempotentCompletion() {
        long id=reserve();resources(id);post("/api/v1/rentals/"+id+"/return",student,change(id),"return");long j=job(id,"RETURN");var c=claim(j);var input=verification(id,c,true);
        var first=post("/api/internal/jobs/"+j+"/return-verification",worker,input,null);assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat(first.getBody().get("rentalStatus")).isEqualTo("RETURNED");
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,input,null).getBody()).isEqualTo(first.getBody());
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE released_at IS NULL",Integer.class)).isZero();
        assertThat(owner.queryForObject("SELECT count(*) FROM vmid_allocation WHERE released_at IS NULL",Integer.class)).isZero();
        assertThat(owner.queryForObject("SELECT generation FROM gpu_lease WHERE server_id=?",Long.class,server)).isEqualTo(8);
    }
    @Test void staleRevisionCannotApplyReturnResult() {
        long id=reserve();resources(id);post("/api/v1/rentals/"+id+"/return",student,change(id),"return");long j=job(id,"RETURN");var c=claim(j);var input=verification(id,c,true);
        owner.update("UPDATE rental SET desired_revision=desired_revision+1 WHERE id=?",id);
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,input,null).getStatusCode().value()).isEqualTo(409);
        assertThat(owner.queryForObject("SELECT count(*) FROM return_verification",Integer.class)).isZero();
    }
    @Test void validatesEvidenceAgainstManifestBeforeFreeingResources() {
        long id=reserve();resources(id);post("/api/v1/rentals/"+id+"/return",student,change(id),"return");long j=job(id,"RETURN");var c=claim(j);
        var input=verification(id,c,true);List<Map<String,Object>> items=(List<Map<String,Object>>)input.get("items");items.get(2).put("evidence",Map.of("checkedIds",List.of("not-owned")));
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,input,null).getStatusCode().value()).isEqualTo(422);
        assertThat(owner.queryForObject("SELECT count(*) FROM return_verification",Integer.class)).isZero();
    }
    @Test void databasePreventsReleaseAndReturnedWithoutVerification() {
        long id=reserve();resources(id);
        assertThatThrownBy(()->owner.update("UPDATE ip_lease SET released_at=now() WHERE rental_id=?",id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->owner.update("UPDATE rental SET status='RETURNED' WHERE id=?",id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void vmidUniquenessIncludesTemplatesAcrossNodes() {
        long id=reserve();owner.update("INSERT INTO vmid_allocation(vmid,server_id,kind) VALUES (2000,?,'TEMPLATE')",otherServer);
        assertThatThrownBy(()->owner.update("INSERT INTO vmid_allocation(vmid,server_id,rental_id,kind) VALUES (2000,?,?,'RENTAL')",server,id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void unknownServerObservationIsNotTreatedAsAvailable() {
        owner.update("UPDATE server_node SET inventory_source='AGENT',observed_at=NULL,status='AVAILABLE' WHERE id=?",server);
        assertThat(post("/api/v1/rentals",student,reservation(server),"unknown").getStatusCode().value()).isEqualTo(503);
    }
    @Test void adminForceReturnStoresActorAndReason() {
        long id=reserve();var result=post("/api/v1/admin/rentals/"+id+"/force-return",admin,Map.of("expectedVersion",version(id),"reason","관리자 시험 사유"),"force");assertThat(result.getStatusCode().value()).isEqualTo(202);
        assertThat(owner.queryForObject("SELECT reason_text FROM rental_event WHERE rental_id=? AND reason_code='ADMIN_FORCE_RETURN'",String.class,id)).isEqualTo("관리자 시험 사유");
        assertThat(owner.queryForObject("SELECT actor_id FROM rental_event WHERE rental_id=? AND reason_code='ADMIN_FORCE_RETURN'",Long.class,id)).isEqualTo(9001);
    }
    @Test void connectionAndCredentialAreBlockedBeforeRealPreparation() {
        long id=reserve();assertThat(get("/api/v1/rentals/"+id+"/connection",student).getBody().get("available")).isEqualTo(false);
        assertThat(get("/api/v1/rentals/"+id+"/credential",student).getStatusCode().value()).isEqualTo(409);
    }
    @Test void appliedPasswordBecomesReadableAndFailedResetKeepsPreviousPassword() {
        long id=reserve();resources(id);long prepare=job(id,"PREPARE");var claim=claim(prepare);
        var pending=get("/api/internal/jobs/"+prepare+"/pending-credential?workerId=test-worker&commandId="+claim.get("commandId"),worker).getBody();
        var applied=Map.of("workerId","test-worker","commandId",claim.get("commandId"),"credentialVersion",1,"desiredRevision",0,"operationId","apply-first","passed",true);
        assertThat(post("/api/internal/jobs/"+prepare+"/credential-applied",worker,applied,null).getStatusCode().value()).isEqualTo(200);
        var observed=new LinkedHashMap<String,Object>();observed.put("workerId","test-worker");observed.put("commandId",claim.get("commandId"));observed.put("desiredRevision",0);
        observed.put("vmid",2000);observed.put("ip","192.168.101.10");observed.put("gpuGeneration",7);observed.put("observedAt",NOW.toString());observed.put("vmState","RUNNING");
        observed.put("healthPassed",true);observed.put("accessEnabled",false);observed.put("hostKeyFingerprint","SHA256:simulation-only");observed.put("operationId","observe-first");
        assertThat(post("/api/internal/jobs/"+prepare+"/observation",worker,observed,null).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/rentals/"+id+"/credential",student).getBody().get("password")).isEqualTo(pending.get("password"));
        var reset=post("/api/v1/rentals/"+id+"/credential-resets",student,change(id),"reset");assertThat(reset.getStatusCode().value()).isEqualTo(202);
        long resetJob=((Number)reset.getBody().get("jobId")).longValue();var resetClaim=claim(resetJob);
        var failed=Map.of("workerId","test-worker","commandId",resetClaim.get("commandId"),"credentialVersion",2,"desiredRevision",1,"operationId","apply-reset-failed","passed",false);
        assertThat(post("/api/internal/jobs/"+resetJob+"/credential-applied",worker,failed,null).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/rentals/"+id+"/credential",student).getBody().get("password")).isEqualTo(pending.get("password"));
        assertThat(owner.queryForObject("SELECT string_agg(response_body::text,' ') FROM idempotency_record",String.class)).doesNotContain(pending.get("password").toString());
    }
    @Test void administrativeRetryUsesNewAttemptAndOldCommandCannotComplete() {
        long id=reserve();resources(id);post("/api/v1/rentals/"+id+"/return",student,change(id),"return");long j=job(id,"RETURN");var firstClaim=claim(j);var oldInput=verification(id,firstClaim,false);
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,oldInput,null).getStatusCode().value()).isEqualTo(200);
        assertThat(post("/api/v1/admin/jobs/"+j+"/retry",admin,change(id),"retry").getStatusCode().value()).isEqualTo(202);
        var secondClaim=claim(j);assertThat(secondClaim.get("commandId")).isNotEqualTo(firstClaim.get("commandId"));
        assertThat(post("/api/internal/jobs/"+j+"/heartbeat",worker,Map.of("workerId","test-worker","commandId",firstClaim.get("commandId")),null).getStatusCode().value()).isEqualTo(409);
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,oldInput,null).getStatusCode().value()).isEqualTo(409);
        assertThat(post("/api/internal/jobs/"+j+"/return-verification",worker,verification(id,secondClaim,true),null).getBody().get("result")).isEqualTo("PASS");
    }
    @Test void failureDuringSecondSlotInsertRollsBackWholeRequest() {
        owner.execute("CREATE OR REPLACE FUNCTION reject_test_slot() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.start_at='2026-10-09 00:00Z'::timestamptz THEN RAISE EXCEPTION 'injected storage failure' USING ERRCODE='23514'; END IF; RETURN NEW; END $$");
        owner.execute("CREATE TRIGGER test_storage_failure BEFORE INSERT ON rental_slot FOR EACH ROW EXECUTE FUNCTION reject_test_slot()");
        try {
            var input=new HashMap<>(reservation(server));input.put("slots",List.of(Map.of("startAt","2026-10-08T00:00:00Z","endAt","2026-10-08T06:00:00Z"),Map.of("startAt","2026-10-09T00:00:00Z","endAt","2026-10-09T06:00:00Z")));
            assertThat(post("/api/v1/rentals",student,input,"atomic-failure").getStatusCode().value()).isEqualTo(500);
            for(String table:List.of("rental","rental_slot","job","idempotency_record"))assertThat(owner.queryForObject("SELECT count(*) FROM "+table,Integer.class)).isZero();
        }finally {owner.execute("DROP TRIGGER test_storage_failure ON rental_slot");owner.execute("DROP FUNCTION reject_test_slot()");}
    }
    @Test void anotherMemberStillNeedsSwitchPreparation() {
        reserve();var input=new HashMap<>(reservation(server));input.put("slots",List.of(Map.of("startAt","2026-10-08T06:30:00Z","endAt","2026-10-08T07:00:00Z")));
        assertThat(post("/api/v1/rentals",other,input,"switch").getBody().get("code")).isEqualTo("INSUFFICIENT_SWITCH_TIME");
    }
    @Test void oneOpenRentalIsEnforcedAcrossServersAndReleasesOnCancellation() throws Exception {
        var later=Map.of("serverId",otherServer,"purpose","다른 서버","slots",
            List.of(Map.of("startAt","2026-10-09T00:00:00Z","endAt","2026-10-09T06:00:00Z")));
        try(var executor=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);
            var first=executor.submit(()->{gate.await();return post("/api/v1/rentals",student,reservation(server),"first");});
            var second=executor.submit(()->{gate.await();return post("/api/v1/rentals",student,later,"second");});
            gate.countDown();
            assertThat(List.of(first.get().getStatusCode().value(),second.get().getStatusCode().value())).containsExactlyInAnyOrder(201,409);
            var denied=first.get().getStatusCode().value()==409?first.get():second.get();
            assertThat(denied.getBody().get("code")).isEqualTo("MEMBER_RENTAL_LIMIT");
        }
        long id=owner.queryForObject("SELECT id FROM rental",Long.class);
        assertThat(post("/api/v1/rental-previews",student,later,null).getBody().get("canReserve")).isEqualTo(false);
        assertThatThrownBy(()->owner.update("""
            INSERT INTO rental(member_id,server_id,contract_start,contract_end,purpose)
            SELECT member_id,server_id,contract_start,contract_end,'second direct booking' FROM rental WHERE id=?
            """,id)).rootCause().hasMessageContaining("Member already has an open rental");
        assertThat(post("/api/v1/rentals/"+id+"/cancel",student,change(id),"cancel-to-book").getStatusCode().value()).isLessThan(300);
        assertThat(post("/api/v1/rentals",student,later,"after-cancel").getStatusCode().value()).isEqualTo(201);
    }

    @Test void categoryCrudRequiresAdminAndHonorsVersionsAndReferences() {
        String route="/api/v1/admin/categories";
        var create=Map.of("name","신규 분류","description","설명");
        assertThat(post(route,student,create,"student-create").getStatusCode().value()).isEqualTo(403);
        var created=post(route,admin,create,"new-category-test");
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        long id=((Number)created.getBody().get("id")).longValue();
        String item=route+"/"+id;
        assertThat(post(route,admin,create,"duplicate-category").getBody().get("code")).isEqualTo("CATEGORY_NAME_EXISTS");
        var change=Map.of("name","변경 분류","description"," ","expectedVersion",0);
        var updated=request(HttpMethod.PATCH,item,admin,change,"edit-category-test");
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(updated.getBody()).containsEntry("name","변경 분류").containsEntry("version",1).containsEntry("description",null);
        assertThat(request(HttpMethod.PATCH,item,admin,change,"edit-category-test").getBody()).isEqualTo(updated.getBody());
        assertThat(request(HttpMethod.DELETE,item,admin,Map.of("expectedVersion",0),"stale-delete").getBody().get("code")).isEqualTo("VERSION_CONFLICT");
        assertThat(request(HttpMethod.DELETE,item,student,Map.of("expectedVersion",1),"student-delete").getStatusCode().value()).isEqualTo(403);
        long activeCategory=owner.queryForObject("SELECT category_id FROM server_node WHERE id=?",Long.class,server);
        assertThat(request(HttpMethod.DELETE,route+"/"+activeCategory,admin,Map.of("expectedVersion",0),"in-use-category").getBody().get("code")).isEqualTo("CATEGORY_IN_USE");
        assertThat(request(HttpMethod.DELETE,item,admin,Map.of("expectedVersion",1),"delete-category-test").getStatusCode().value()).isEqualTo(204);
        assertThat(request(HttpMethod.DELETE,item,admin,Map.of("expectedVersion",1),"delete-category-test").getStatusCode().value()).isEqualTo(204);
        assertThat(request(HttpMethod.PATCH,item,admin,change,"deleted-edit").getStatusCode().value()).isEqualTo(404);
        assertThat(owner.queryForObject("SELECT deleted_at IS NOT NULL FROM category WHERE id=?",Boolean.class,id)).isTrue();
    }
    @Test void unknownPasswordApplicationBlocksOldPasswordDisclosure() {
        long id=reserve();resources(id);
        owner.update("UPDATE vm_credential SET status='ACTIVE' WHERE rental_id=?",id);owner.update("UPDATE rental SET status='READY' WHERE id=?",id);
        var reset=post("/api/v1/rentals/"+id+"/credential-resets",student,change(id),"unknown-reset");long j=((Number)reset.getBody().get("jobId")).longValue();var c=claim(j);
        var input=new LinkedHashMap<String,Object>();input.put("workerId","test-worker");input.put("commandId",c.get("commandId"));input.put("credentialVersion",2);input.put("desiredRevision",1);input.put("operationId","uncertain-apply");input.put("passed",null);
        assertThat(post("/api/internal/jobs/"+j+"/credential-applied",worker,input,null).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/rentals/"+id+"/credential",student).getStatusCode().value()).isEqualTo(409);
        assertThat(get("/api/v1/rentals/"+id+"/connection",student).getBody().get("available")).isEqualTo(false);
    }
    @Test void morningToAfternoonUsesOneHourButNightTransitionUsesActualGap() {
        reserve();var afternoon=new HashMap<>(reservation(server));afternoon.put("slots",List.of(Map.of("startAt","2026-10-08T07:00:00Z","endAt","2026-10-08T14:00:00Z")));
        assertThat(post("/api/v1/rentals",other,afternoon,"afternoon").getStatusCode().value()).isEqualTo(201);
        var nextMorning=new HashMap<>(reservation(server));nextMorning.put("slots",List.of(Map.of("startAt","2026-10-08T23:00:00Z","endAt","2026-10-09T06:00:00Z")));
        assertThat(post("/api/v1/rentals",admin,nextMorning,"next-morning").getStatusCode().value()).isEqualTo(201);
    }
    @Test void runtimeRoleCannotAlterSchemaOrMigrationHistory() {
        var runtime=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_USER"),System.getenv("DB_PASSWORD")));
        assertThatThrownBy(()->runtime.execute("ALTER TABLE rental ADD COLUMN illicit text")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->runtime.update("UPDATE flyway_schema_history SET description='illicit'")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    Map<String,Object> twoSlots() {
        var input=new HashMap<String,Object>(reservation(server));
        input.put("slots",List.of(Map.of("startAt","2026-10-08T00:00:00Z","endAt","2026-10-08T06:00:00Z"),Map.of("startAt","2026-10-10T00:00:00Z","endAt","2026-10-10T06:00:00Z")));return input;
    }
    void configure(long node,int template) {
        long v=owner.queryForObject("SELECT version FROM server_node WHERE id=?",Long.class,node);
        String subnet=node==server?"192.168.101.":"192.168.102.";
        var body=new LinkedHashMap<String,Object>();body.put("expectedVersion",v);body.put("vmSubnet",subnet+"0/24");body.put("ipPoolStart",subnet+"10");body.put("ipPoolEnd",subnet+"11");
        body.put("vmidStart",2000);body.put("vmidEnd",2002);body.put("gpuMappingRef","test-gpu");body.put("publicSshHost","192.0.2.10");body.put("publicSshPort",2201);body.put("reason","모의 인벤토리 설정");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+node+"/configuration",admin,body,null).getStatusCode().value()).isEqualTo(200);
        var t=Map.of("templateVmid",template,"expectedVersion",v+1,"imageHash","sha256:simulation-only","driverVersion","simulation","reason","모의 템플릿 승인");
        assertThat(post("/api/v1/admin/servers/"+node+"/templates",admin,t,"template-"+node).getStatusCode().value()).isEqualTo(201);
        refresh(node);
    }
    @Test void noGpuConfigurationAllocatesAndKeepsOwnershipFenceWithoutMapping() {
        configure(server,1000);
        owner.update("UPDATE server_node SET gpu_enabled=false,gpu_mapping_ref=NULL WHERE id=?",server);
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);var a=allocation(j,c);
        assertThat((Map)a.get("allocation")).containsKey("vmid");
        assertThat((Map)a.get("gpu")).containsEntry("state","HELD");
        var context=request(HttpMethod.GET,"/api/internal/jobs/"+j+"/context?workerId=test-worker&commandId="+c.get("commandId"),worker,null,null);
        assertThat((Map)context.getBody().get("server")).containsEntry("gpuEnabled",false).containsEntry("gpuMappingRef",null);
    }
    @Test void inventoryAcceptsExplicitNoGpuAndRejectsStringNullOrMissingEnabledMapping() {
        var body=new LinkedHashMap<String,Object>();body.put("expectedVersion",0);body.put("vmSubnet","10.77.0.0/24");body.put("ipPoolStart","10.77.0.10");body.put("ipPoolEnd","10.77.0.20");body.put("vmidStart",2000);body.put("vmidEnd",2002);body.put("publicSshHost","192.0.2.10");body.put("publicSshPort",2201);body.put("reason","test no GPU");
        body.put("expectedVersion",owner.queryForObject("SELECT version FROM server_node WHERE id=?",Long.class,server));
        body.put("gpuEnabled",true);body.put("gpuMappingRef",null);
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server+"/configuration",admin,body,null).getStatusCode().value()).isEqualTo(422);
        body.put("gpuEnabled",false);body.put("gpuMappingRef","null");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server+"/configuration",admin,body,null).getStatusCode().value()).isEqualTo(422);
        body.put("gpuMappingRef",null);
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server+"/configuration",admin,body,null).getStatusCode().value()).isEqualTo(200);
        assertThat(owner.queryForObject("SELECT gpu_enabled FROM server_node WHERE id=?",Boolean.class,server)).isFalse();
    }
    void refresh(long node) {
        assertThat(post("/api/internal/servers/"+node+"/observation",worker,Map.of("observedAt",now().toString(),"status","AVAILABLE"),null).getStatusCode().value()).isEqualTo(200);
    }
    Map<String,Object> allocation(long j,Map c) {
        var response=post("/api/internal/jobs/"+j+"/resources",worker,Map.of("workerId","test-worker","commandId",c.get("commandId")),null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);return response.getBody();
    }
    Map<String,Object> observation(Map c,Map resources,String state,boolean access,Boolean detached) {
        var a=(Map)resources.get("allocation");var ip=(Map)resources.get("ip");var gpu=(Map)resources.get("gpu");
        var out=new LinkedHashMap<String,Object>();out.put("workerId","test-worker");out.put("commandId",c.get("commandId"));out.put("desiredRevision",c.get("desiredRevision"));
        out.put("vmid",a.get("vmid"));out.put("ip",ip.get("address"));out.put("gpuGeneration",gpu.get("generation"));out.put("observedAt",now().toString());out.put("vmState",state);
        out.put("healthPassed",true);out.put("accessEnabled",access);out.put("hostKeyFingerprint","SHA256:simulation-only");out.put("operationId","observation-"+c.get("commandId"));out.put("gpuDetached",detached);return out;
    }
    Map<String,Object> prepare(long rental) {
        long j=job(rental,"PREPARE");var c=claim(j);var a=allocation(j,c);
        assertThat(post("/api/internal/jobs/"+j+"/credential-applied",worker,Map.of("workerId","test-worker","commandId",c.get("commandId"),"credentialVersion",1,"desiredRevision",c.get("desiredRevision"),"operationId","apply-"+j,"passed",true),null).getStatusCode().value()).isEqualTo(200);
        assertThat(post("/api/internal/jobs/"+j+"/observation",worker,observation(c,a,"RUNNING",false,null),null).getStatusCode().value()).isEqualTo(200);return a;
    }
    long observePrepareInsideSlot(long rental,boolean access) {
        refresh(server);
        long j=job(rental,"PREPARE");var c=claim(j);var a=allocation(j,c);
        assertThat(post("/api/internal/jobs/"+j+"/credential-applied",worker,Map.of("workerId","test-worker","commandId",c.get("commandId"),"credentialVersion",1,"desiredRevision",c.get("desiredRevision"),"operationId","apply-"+j,"passed",true),null).getStatusCode().value()).isEqualTo(200);
        var response=post("/api/internal/jobs/"+j+"/observation",worker,observation(c,a,"RUNNING",access,null),null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);assertThat(response.getBody().get("status")).isEqualTo(access?"ACTIVE":"READY");return j;
    }
    @Test void activePrepareSupersedesOnlyCurrentPendingStartWithoutClosingWorkingAccess() {
        configure(server,1000);long rental=((Number)post("/api/v1/rentals",student,twoSlots(),"two-slots-active-prepare").getBody().get("id")).longValue();
        post("/api/internal/lifecycle/tick",worker,null,null);time(Instant.parse("2026-10-08T00:00:00Z"));
        long firstStart=owner.queryForObject("SELECT id FROM job WHERE rental_id=? AND type='START' ORDER BY next_run_at LIMIT 1",Long.class,rental);
        observePrepareInsideSlot(rental,true);
        assertThat(owner.queryForObject("SELECT status FROM job WHERE id=?",String.class,firstStart)).isEqualTo("SUPERSEDED");
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='START' AND status='PENDING'",Integer.class,rental)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='STOP' AND status='PENDING'",Integer.class,rental)).isEqualTo(1);
        assertThat(post("/api/internal/jobs/"+firstStart+"/claim",worker,Map.of("workerId","test-worker"),null).getBody().get("code")).isEqualTo("JOB_NOT_PENDING");
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(true);
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,server)).isEqualTo("HELD");
    }
    @Test void schedulerAfterActivePrepareCreatesStableSupersededStartInsteadOfRedispatch() {
        configure(server,1000);long rental=reserve();time(Instant.parse("2026-10-08T00:00:00Z"));observePrepareInsideSlot(rental,true);
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("createdJobs")).isEqualTo(1);
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("createdJobs")).isEqualTo(0);
        assertThat(owner.queryForObject("SELECT status FROM job WHERE rental_id=? AND type='START'",String.class,rental)).isEqualTo("SUPERSEDED");
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='START'",Integer.class,rental)).isEqualTo(1);
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(true);
    }
    @Test void prepareWithoutAccessDoesNotSuppressNeededStart() {
        configure(server,1000);long rental=reserve();time(Instant.parse("2026-10-08T00:00:00Z"));
        post("/api/internal/lifecycle/tick",worker,null,null);observePrepareInsideSlot(rental,false);
        assertThat(owner.queryForObject("SELECT status FROM job WHERE rental_id=? AND type='START'",String.class,rental)).isEqualTo("PENDING");
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(false);
    }
    @Test void lifecycleCreatesDurableSlotIntentsOnlyOnce() {
        long id=((Number)post("/api/v1/rentals",student,twoSlots(),"slots").getBody().get("id")).longValue();
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("createdJobs")).isEqualTo(3);
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("createdJobs")).isEqualTo(0);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=?",Integer.class,id)).isEqualTo(4);
        assertThat(owner.queryForObject("SELECT count(*) FROM vm_instance",Integer.class)).isZero();
    }
    @Test void mergedContinuousBandDoesNotCreateIntermediateStop() {
        var input=Map.of("serverId",server,"purpose","연속 계약","rule",Map.of("from","2026-10-08","to","2026-10-08","weekdays",List.of("THURSDAY"),"bands",List.of("MORNING","AFTERNOON")));
        assertThat(post("/api/v1/rentals",student,input,"merged").getStatusCode().value()).isEqualTo(201);post("/api/internal/lifecycle/tick",worker,null,null);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_slot",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE type='STOP'",Integer.class)).isZero();
    }
    @Test void expirationWithoutResourcesCancelsOnceWithoutClaimingDeletion() {
        long id=reserve();time(NOW.plus(Duration.ofDays(4)));
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("expiredRentals")).isEqualTo(1);
        assertThat(post("/api/internal/lifecycle/tick",worker,null,null).getBody().get("expiredRentals")).isEqualTo(0);
        assertThat(owner.queryForObject("SELECT status FROM rental WHERE id=?",String.class,id)).isEqualTo("CANCELLED");
        assertThat(owner.queryForObject("SELECT count(*) FROM return_verification",Integer.class)).isZero();
    }
    @Test void expirationWithResourcesQueuesVerifiedReturnAndRetainsLeases() {
        long id=reserve();resources(id);time(NOW.plus(Duration.ofDays(4)));post("/api/internal/lifecycle/tick",worker,null,null);
        assertThat(owner.queryForObject("SELECT status FROM rental WHERE id=?",String.class,id)).isEqualTo("RETURNING");
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE type='RETURN' AND desired_revision=1",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE released_at IS NULL",Integer.class)).isEqualTo(1);
    }
    @Test void unconfiguredInventoryCannotAllocateAndDoesNotInventResources() {
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);
        assertThat(post("/api/internal/jobs/"+j+"/resources",worker,Map.of("workerId","test-worker","commandId",c.get("commandId")),null).getBody().get("code")).isEqualTo("INVENTORY_NOT_CONFIGURED");
        assertThat(owner.queryForObject("SELECT count(*) FROM vmid_allocation",Integer.class)).isZero();
    }
    @Test void configuredAllocationReplaysWithoutDuplicatingAndManifestOnlyAppends() {
        configure(server,1000);long id=reserve(),j=job(id,"PREPARE");var c=claim(j);var a=allocation(j,c);
        assertThat(allocation(j,c)).isEqualTo(a);
        var input=Map.of("workerId","test-worker","commandId",c.get("commandId"),"volumeIds",List.of("owned-disk"),"snippetIds",List.of("owned-snippet"),"ingressRuleIds",List.of("owned-rule"));
        assertThat(post("/api/internal/jobs/"+j+"/manifest",worker,input,null).getStatusCode().value()).isEqualTo(200);
        var empty=Map.of("workerId","test-worker","commandId",c.get("commandId"),"volumeIds",List.of(),"snippetIds",List.of(),"ingressRuleIds",List.of());
        assertThat(post("/api/internal/jobs/"+j+"/manifest",worker,empty,null).getBody().get("volumeIds")).isEqualTo(List.of("owned-disk"));
        var context=get("/api/internal/jobs/"+j+"/context?workerId=test-worker&commandId="+c.get("commandId"),worker);
        assertThat(context.getStatusCode().value()).isEqualTo(200);assertThat(context.getBody().toString()).doesNotContain("encryptedSecret","password","secretRef");
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease",Integer.class)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT generation FROM gpu_lease WHERE server_id=?",Long.class,server)).isEqualTo(1);
    }
    @Test void allocatorIncludesTemplateIdsAndRollsBackOnGlobalPoolExhaustion() {
        configure(server,2000);configure(otherServer,2002);long id=reserve();var a=allocation(job(id,"PREPARE"),claim(job(id,"PREPARE")));
        assertThat(((Map)a.get("allocation")).get("vmid")).isEqualTo(2001);
        long second=((Number)post("/api/v1/rentals",other,reservation(otherServer),"second").getBody().get("id")).longValue(),j=job(second,"PREPARE");var c=claim(j);
        assertThat(post("/api/internal/jobs/"+j+"/resources",worker,Map.of("workerId","test-worker","commandId",c.get("commandId")),null).getBody().get("code")).isEqualTo("VMID_POOL_EXHAUSTED");
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE rental_id=?",Integer.class,second)).isZero();
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,otherServer)).isEqualTo("FREE");
    }
    @Test void configurationRejectsInvalidSubnetAndTemplateIsIdempotent() {
        configure(server,1000);
        var input=Map.of("templateVmid",1000,"expectedVersion",1,"imageHash","sha256:simulation-only","driverVersion","simulation","reason","모의 템플릿 승인");
        assertThat(post("/api/v1/admin/servers/"+server+"/templates",admin,input,"template-"+server).getStatusCode().value()).isEqualTo(201);
        assertThat(owner.queryForObject("SELECT count(*) FROM template_version",Integer.class)).isEqualTo(1);
        var invalid=new LinkedHashMap<String,Object>();invalid.put("expectedVersion",2);invalid.put("vmSubnet","192.168.101.0/24");invalid.put("ipPoolStart","192.168.101.0");invalid.put("ipPoolEnd","192.168.101.11");invalid.put("vmidStart",2000);invalid.put("vmidEnd",2002);invalid.put("gpuMappingRef","test");invalid.put("publicSshHost","192.0.2.10");invalid.put("publicSshPort",2201);invalid.put("reason","잘못된 네트워크 주소");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server+"/configuration",admin,invalid,null).getStatusCode().value()).isEqualTo(422);
        assertThat(owner.queryForObject("SELECT version FROM server_node WHERE id=?",Long.class,server)).isEqualTo(2);
    }
    @Test void freshServerObservationCannotOverrideAdministrativeMaintenance() {
        configure(server,1000);owner.update("UPDATE server_node SET status='MAINTENANCE' WHERE id=?",server);time(NOW.plusSeconds(1));refresh(server);
        assertThat(post("/api/v1/rentals",student,reservation(server),"maintenance").getStatusCode().value()).isEqualTo(503);
        assertThat(post("/api/internal/servers/"+server+"/observation",student,Map.of("observedAt",now().toString(),"status","AVAILABLE"),null).getStatusCode().value()).isEqualTo(401);
    }
    @Test void startAndPeriodicHealthRequireCurrentSlotOwnershipAndFreshness() {
        configure(server,1000);long id=reserve();var a=prepare(id);post("/api/internal/lifecycle/tick",worker,null,null);
        time(Instant.parse("2026-10-08T00:00:00Z"));refresh(server);long start=job(id,"START");var c=claim(start);a=allocation(start,c);
        assertThat(post("/api/internal/jobs/"+start+"/observation",worker,observation(c,a,"RUNNING",true,null),null).getBody().get("status")).isEqualTo("ACTIVE");
        assertThat(get("/api/v1/rentals/"+id+"/connection",student).getBody().get("available")).isEqualTo(true);
        time(now().plusSeconds(121));assertThat(get("/api/v1/rentals/"+id+"/connection",student).getBody().get("available")).isEqualTo(false);
        var health=new LinkedHashMap<String,Object>();health.put("desiredRevision",0);health.put("vmid",((Map)a.get("allocation")).get("vmid"));health.put("gpuGeneration",((Map)a.get("gpu")).get("generation"));health.put("observedAt",now().toString());health.put("vmState","RUNNING");health.put("healthPassed",true);health.put("accessEnabled",true);health.put("hostKeyFingerprint","SHA256:simulation-only");
        assertThat(post("/api/internal/rentals/"+id+"/health",worker,health,null).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/rentals/"+id+"/connection",student).getBody().get("available")).isEqualTo(true);
        health.put("gpuGeneration",99);assertThat(post("/api/internal/rentals/"+id+"/health",worker,health,null).getStatusCode().value()).isEqualTo(409);
        health.put("gpuGeneration",1);assertThat(post("/api/internal/rentals/"+id+"/health",worker,health,null).getBody().get("code")).isEqualTo("STALE_OBSERVATION");
    }
    @Test void verifiedStopRetainsVmAndDiskButReleasesGpuOnlyAfterDetachment() {
        configure(server,1000);long id=((Number)post("/api/v1/rentals",student,twoSlots(),"retained").getBody().get("id")).longValue();var a=prepare(id);
        post("/api/internal/lifecycle/tick",worker,null,null);time(Instant.parse("2026-10-08T06:00:00Z"));post("/api/internal/lifecycle/tick",worker,null,null);
        long stop=job(id,"STOP");var c=claim(stop);var observed=observation(c,a,"STOPPED",false,false);
        assertThat(post("/api/internal/jobs/"+stop+"/observation",worker,observed,null).getStatusCode().value()).isEqualTo(422);
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,server)).isEqualTo("HELD");
        observed.put("gpuDetached",true);assertThat(post("/api/internal/jobs/"+stop+"/observation",worker,observed,null).getBody().get("status")).isEqualTo("RETAINED");
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,server)).isEqualTo("FREE");
        assertThat(owner.queryForObject("SELECT count(*) FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",Integer.class,id)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE rental_id=? AND released_at IS NULL",Integer.class,id)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT state FROM vm_instance WHERE rental_id=?",String.class,id)).isEqualTo("STOPPED");
    }
    @Test void unknownWorkerFailureHoldsResourcesAndPreventsGpuTheft() {
        configure(server,1000);long id=reserve(),j=job(id,"PREPARE");var c=claim(j);allocation(j,c);
        var failure=Map.of("workerId","test-worker","commandId",c.get("commandId"),"desiredRevision",0,"operationId","lost-operation","reasonCode","AGENT_TIMEOUT","observedAt",now().toString(),"outcomeKnown",false,"accessBlocked",false);
        var result=post("/api/internal/jobs/"+j+"/failure",worker,failure,null);assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(post("/api/internal/jobs/"+j+"/failure",worker,failure,null).getBody()).isEqualTo(result.getBody());
        assertThat(owner.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,server)).isEqualTo("UNKNOWN");
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE released_at IS NULL",Integer.class)).isEqualTo(1);
        post("/api/v1/admin/jobs/"+j+"/retry",admin,change(id),"retry-unknown");var retried=claim(j);
        assertThat(post("/api/internal/jobs/"+j+"/resources",worker,Map.of("workerId","test-worker","commandId",retried.get("commandId")),null).getBody().get("code")).isEqualTo("GPU_NOT_FREE");
    }
    @Test void noGpuUnknownLockExplainsResourceUncertaintyAndKeepsAllocation() {
        configure(server,1000);
        owner.update("UPDATE server_node SET gpu_enabled=false,gpu_mapping_ref=NULL WHERE id=?",server);
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);allocation(j,c);
        var before=owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",id);
        var failure=Map.of("workerId","test-worker","commandId",c.get("commandId"),"desiredRevision",0,"operationId","no-gpu-lost-operation","reasonCode","PVE_HTTP_ERROR","observedAt",now().toString(),"outcomeKnown",false,"accessBlocked",false);
        assertThat(post("/api/internal/jobs/"+j+"/failure",worker,failure,null).getStatusCode().value()).isEqualTo(200);
        post("/api/v1/admin/jobs/"+j+"/retry",admin,change(id),"retry-no-gpu-unknown");var retried=claim(j);
        var response=post("/api/internal/jobs/"+j+"/resources",worker,Map.of("workerId","test-worker","commandId",retried.get("commandId")),null);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().get("code")).isEqualTo("RESOURCE_LOCK_UNKNOWN");
        assertThat(owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",id)).isEqualTo(before);
        assertThat(owner.queryForObject("SELECT owner_rental_id FROM gpu_lease WHERE server_id=?",Long.class,server)).isEqualTo(id);
    }
    @Test void undispatchedClaimCanBeDeferredWithoutLosingOwnedResourcesOrCreatingAnAttempt() {
        configure(server,1000);long id=reserve(),j=job(id,"PREPARE");var c=claim(j);allocation(j,c);
        var before=owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",id);
        var gpu=owner.queryForMap("SELECT * FROM gpu_lease WHERE server_id=?",server);long v=version(id);
        var body=Map.of("workerId","test-worker","commandId",c.get("commandId"),"observedAt",now().toString(),"dispatchNotStarted",true,"reasonCode","SERVER_UNAVAILABLE");
        var result=post("/api/internal/jobs/"+j+"/dispatch-deferred",worker,body,null);
        assertThat(result.getStatusCode().value()).isEqualTo(200);assertThat(result.getBody()).containsEntry("status","PENDING").containsEntry("retryCount",0).containsEntry("commandId",c.get("commandId"));
        assertThat(post("/api/internal/jobs/"+j+"/dispatch-deferred",worker,body,null).getBody()).isEqualTo(result.getBody());
        assertThat(owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",id)).isEqualTo(before);
        assertThat(owner.queryForMap("SELECT * FROM gpu_lease WHERE server_id=?",server)).isEqualTo(gpu);assertThat(version(id)).isEqualTo(v);
        assertThat(owner.queryForObject("SELECT locked_until IS NULL AND worker_id IS NULL FROM job WHERE id=?",Boolean.class,j)).isTrue();
        assertThat(post("/api/internal/jobs/"+j+"/claim",worker,Map.of("workerId","test-worker"),null).getBody().get("code")).isEqualTo("JOB_NOT_DUE");
    }
    @Test void undispatchedOldPrepareCannotBlockReturnAfterIntentChanges() {
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);resources(id);
        post("/api/v1/rentals/"+id+"/return",student,change(id),"return-before-dispatch");
        var body=Map.of("workerId","test-worker","commandId",c.get("commandId"),"observedAt",now().toString(),"dispatchNotStarted",true,"reasonCode","STALE_REVISION");
        var result=post("/api/internal/jobs/"+j+"/dispatch-deferred",worker,body,null);
        assertThat(result.getStatusCode().value()).isEqualTo(200);assertThat(result.getBody().get("status")).isEqualTo("SUPERSEDED");
        assertThat(owner.queryForObject("SELECT count(*) FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",Long.class,id)).isEqualTo(1);
        assertThat(post("/api/internal/jobs/"+job(id,"RETURN")+"/claim",worker,Map.of("workerId","return-worker"),null).getStatusCode().value()).isEqualTo(200);
    }
    @Test void deferralRejectsOtherWorkersStaleCommandsAndMissingDispatchEvidence() {
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);
        var body=new LinkedHashMap<String,Object>(Map.of("workerId","test-worker","commandId",c.get("commandId"),"observedAt",now().toString(),"dispatchNotStarted",true,"reasonCode","GPU_NOT_FREE"));
        for(var change:Map.of("workerId","another-worker","commandId","job:999:attempt:0:r0","dispatchNotStarted",false,"observedAt",now().minusSeconds(121).toString()).entrySet()) {
            var invalid=new LinkedHashMap<>(body);invalid.put(change.getKey(),change.getValue());
            assertThat(post("/api/internal/jobs/"+j+"/dispatch-deferred",worker,invalid,null).getStatusCode().value()).isIn(409,422);
            assertThat(owner.queryForObject("SELECT status FROM job WHERE id=?",String.class,j)).isEqualTo("RUNNING");
        }
        assertThat(post("/api/internal/jobs/"+j+"/dispatch-deferred",student,body,null).getStatusCode().value()).isEqualTo(401);
        time(now().plusSeconds(121));body.put("observedAt",now().toString());
        assertThat(post("/api/internal/jobs/"+j+"/dispatch-deferred",worker,body,null).getBody().get("status")).isEqualTo("PENDING");
    }
    record ResourceRecoveryCase(long rental,long job,Map<String,Object> body) {}
    ResourceRecoveryCase unknownOwnedResourceLock() {
        configure(server,1000);
        owner.update("UPDATE server_node SET gpu_enabled=false,gpu_mapping_ref=NULL WHERE id=?",server);
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);allocation(j,c);
        owner.update("UPDATE resource_manifest SET volume_ids='[\"owned-disk\"]',snippet_ids='[\"owned-snippet\"]',ingress_rule_ids='[\"owned-ingress\"]' WHERE rental_id=?",id);
        var failure=Map.of("workerId","test-worker","commandId",c.get("commandId"),"desiredRevision",0,"operationId","actual-old-operation","reasonCode","AGENT_TIMEOUT","observedAt",now().toString(),"outcomeKnown",false,"accessBlocked",true);
        assertThat(post("/api/internal/jobs/"+j+"/failure",worker,failure,null).getStatusCode().value()).isEqualTo(200);
        var allocation=owner.queryForMap("SELECT * FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",id);
        var ip=owner.queryForMap("SELECT id,host(address) AS address FROM ip_lease WHERE rental_id=? AND released_at IS NULL",id);
        var body=new LinkedHashMap<String,Object>();
        body.put("expectedVersion",version(id));body.put("serverId",server);body.put("jobId",j);body.put("commandId",c.get("commandId"));
        body.put("allocationId",allocation.get("id"));body.put("vmid",allocation.get("vmid"));body.put("ipLeaseId",ip.get("id"));body.put("ip",ip.get("address"));
        body.put("gpuGeneration",owner.queryForObject("SELECT generation FROM gpu_lease WHERE server_id=?",Long.class,server));
        body.put("evidence",new LinkedHashMap<>(Map.of("observedAt",now().toString(),"recoveryReference","windows-recovery-record-1","vmState","STOPPED","ownershipConfirmed",true,"activeTaskCount",0,"unknownExecutionCount",0,"accessBlocked",true,"details","Windows recovery verified the same VM and Helper manifest ownership; no in-flight operations.")));
        body.put("reason","Windows 복구 후 동일 소유자 자원 잠금 정합성 복구");
        return new ResourceRecoveryCase(id,j,body);
    }
    String resourceLockRoute(long id){return "/api/v1/admin/rentals/"+id+"/resource-lock/reconcile";}
    void assertUnknownOwned(ResourceRecoveryCase c) {
        assertThat(owner.queryForMap("SELECT state,owner_rental_id,generation FROM gpu_lease WHERE server_id=?",server))
            .containsEntry("state","UNKNOWN").containsEntry("owner_rental_id",c.rental()).containsEntry("generation",c.body().get("gpuGeneration"));
        assertThat(owner.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,c.rental())).isEqualTo(c.body().get("expectedVersion"));
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_event WHERE rental_id=? AND reason_code='OWNED_RESOURCE_LOCK_RECONCILED'",Long.class,c.rental())).isZero();
    }
    @Test void resourceLockRecoveryPreservesOwnershipAndRequiresSeparateRealPrepareRetry() {
        var c=unknownOwnedResourceLock();
        var allocations=owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",c.rental());
        var ips=owner.queryForList("SELECT * FROM ip_lease WHERE rental_id=?",c.rental());
        var manifest=owner.queryForMap("SELECT * FROM resource_manifest WHERE rental_id=?",c.rental());
        var jobBefore=owner.queryForMap("SELECT * FROM job WHERE id=?",c.job());
        long beforeVersion=version(c.rental());
        var result=post(resourceLockRoute(c.rental()),admin,c.body(),"owned-recovery");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).containsEntry("gpuState","HELD").containsEntry("status","PROVISIONING").containsEntry("jobStatus","FAILED")
            .containsEntry("resourcesReleased",false).containsEntry("vmMutationsPerformed",false).containsEntry("retryRequired",true);
        assertThat(version(c.rental())).isEqualTo(beforeVersion+1);
        assertThat(owner.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=?",c.rental())).isEqualTo(allocations);
        assertThat(owner.queryForList("SELECT * FROM ip_lease WHERE rental_id=?",c.rental())).isEqualTo(ips);
        assertThat(owner.queryForMap("SELECT * FROM resource_manifest WHERE rental_id=?",c.rental())).isEqualTo(manifest);
        assertThat(owner.queryForMap("SELECT * FROM job WHERE id=?",c.job())).isEqualTo(jobBefore);
        assertThat(owner.queryForObject("SELECT count(*) FROM vm_instance WHERE rental_id=?",Long.class,c.rental())).isZero();
        assertThat(owner.queryForObject("SELECT reason_text FROM rental_event WHERE rental_id=? AND reason_code='OWNED_RESOURCE_LOCK_RECONCILED'",String.class,c.rental()))
            .contains("windows-recovery-record-1","ADMIN_ATTESTATION_OF_WINDOWS_RECOVERY");
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"owned-recovery").getBody()).isEqualTo(result.getBody());
        assertThat(version(c.rental())).isEqualTo(beforeVersion+1);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_event WHERE rental_id=? AND reason_code='OWNED_RESOURCE_LOCK_RECONCILED'",Long.class,c.rental())).isEqualTo(1);
        assertThat(post("/api/v1/admin/jobs/"+c.job()+"/retry",admin,change(c.rental()),"retry-after-recovery").getStatusCode().value()).isEqualTo(202);
        var claimed=claim(c.job());
        assertThat(post("/api/internal/jobs/"+c.job()+"/resources",worker,Map.of("workerId","test-worker","commandId",claimed.get("commandId")),null).getStatusCode().value()).isEqualTo(200);
    }
    @Test void resourceLockRecoveryInspectionAndMutationRequireAdmin() {
        var c=unknownOwnedResourceLock();
        for(String token:Arrays.asList(null,student,other,worker)) {
            int expected=token==null||Objects.equals(token,worker)?401:403;
            assertThat(get("/api/v1/admin/rentals/"+c.rental()+"/resource-lock",token).getStatusCode().value()).isEqualTo(expected);
            assertThat(post(resourceLockRoute(c.rental()),token,c.body(),UUID.randomUUID().toString()).getStatusCode().value()).isEqualTo(expected);
        }
        var inspection=get("/api/v1/admin/rentals/"+c.rental()+"/resource-lock",admin);
        assertThat(inspection.getStatusCode().value()).isEqualTo(200);
        assertThat(inspection.getBody().toString()).doesNotContain("password","encryptedSecret","workerToken");
        assertUnknownOwned(c);
    }
    @Test void resourceLockRecoveryRejectsChangedIdentityRevisionOrGeneration() {
        var c=unknownOwnedResourceLock();
        var cases=new LinkedHashMap<String,Object>();
        cases.put("expectedVersion",version(c.rental())+1);cases.put("serverId",otherServer);cases.put("commandId","job:"+c.job()+":attempt:999:r0");
        cases.put("gpuGeneration",((Number)c.body().get("gpuGeneration")).longValue()+1);cases.put("vmid",99999);
        cases.put("allocationId",99999);cases.put("ipLeaseId",99999);cases.put("ip","192.168.101.99");
        for(var bad:cases.entrySet()) {
            var body=new LinkedHashMap<>(c.body());body.put(bad.getKey(),bad.getValue());
            assertThat(post(resourceLockRoute(c.rental()),admin,body,UUID.randomUUID().toString()).getStatusCode().value()).as(bad.getKey()).isEqualTo(409);
            assertUnknownOwned(c);
        }
        owner.update("UPDATE job SET desired_revision=desired_revision+1 WHERE id=?",c.job());
        var body=new LinkedHashMap<>(c.body());body.put("commandId","job:"+c.job()+":attempt:0:r1");
        assertThat(post(resourceLockRoute(c.rental()),admin,body,"bad-revision").getBody().get("code")).isEqualTo("STALE_REVISION");
        assertUnknownOwned(c);
    }
    @Test void resourceLockRecoveryRejectsIncompleteStaleOrFutureWindowsEvidence() {
        var c=unknownOwnedResourceLock();
        var cases=new LinkedHashMap<String,List<Object>>();
        cases.put("ownershipConfirmed",List.of(false));cases.put("accessBlocked",List.of(false));
        cases.put("activeTaskCount",List.of(1));cases.put("unknownExecutionCount",List.of(1));
        cases.put("observedAt",List.of(now().minusSeconds(121).toString(),now().plusSeconds(6).toString()));
        for(var bad:cases.entrySet()) for(Object value:bad.getValue()) {
            var evidence=new LinkedHashMap<>((Map<String,Object>)c.body().get("evidence"));evidence.put(bad.getKey(),value);
            var body=new LinkedHashMap<>(c.body());body.put("evidence",evidence);
            assertThat(post(resourceLockRoute(c.rental()),admin,body,UUID.randomUUID().toString()).getStatusCode().value()).as(bad.getKey()).isEqualTo(409);
            assertUnknownOwned(c);
        }
        var body=new LinkedHashMap<>(c.body());body.remove("evidence");
        assertThat(post(resourceLockRoute(c.rental()),admin,body,"missing-evidence").getStatusCode().value()).isEqualTo(422);
        assertUnknownOwned(c);
    }
    @Test void resourceLockRecoveryRequiresReconciledFailedJobEvenAfterLeaseExpires() {
        var c=unknownOwnedResourceLock();
        assertThat(post("/api/v1/admin/jobs/"+c.job()+"/retry",admin,change(c.rental()),"early-retry").getStatusCode().value()).isEqualTo(202);
        var claimed=claim(c.job());c.body().put("commandId",claimed.get("commandId"));
        time(now().plusSeconds(121));
        ((Map<String,Object>)c.body().get("evidence")).put("observedAt",now().toString());
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"running-cannot-recover").getBody().get("code")).isEqualTo("JOB_NOT_RECONCILED");
        assertUnknownOwned(c);
        var reconcile=Map.of("expectedVersion",version(c.rental()),"commandId",claimed.get("commandId"),"observedAt",now().toString(),"terminalConfirmed",true,"accessBlocked",true,"operationId","windows-terminal-recovery-evidence","reason","Windows 종료 확인 후 만료 작업 수렴");
        assertThat(post("/api/v1/admin/jobs/"+c.job()+"/reconcile",admin,reconcile,null).getBody().get("status")).isEqualTo("FAILED");
        c.body().put("expectedVersion",version(c.rental()));
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"after-reconcile").getStatusCode().value()).isEqualTo(200);
    }
    @Test void resourceLockRecoveryRejectsPhysicalGpuAccessEnabledAndReturningRental() {
        var c=unknownOwnedResourceLock();
        owner.update("UPDATE server_node SET gpu_enabled=true,gpu_mapping_ref='test-gpu' WHERE id=?",server);
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"physical-gpu").getBody().get("code")).isEqualTo("GPU_RECOVERY_REQUIRES_PHYSICAL_VERIFICATION");
        owner.update("UPDATE server_node SET gpu_enabled=false,gpu_mapping_ref=NULL WHERE id=?",server);
        owner.update("INSERT INTO vm_instance(rental_id,server_id,vmid,vmid_allocation_id,ip,state,access_enabled) VALUES (?,?,?,?,?::inet,'UNKNOWN',true)",c.rental(),server,c.body().get("vmid"),c.body().get("allocationId"),c.body().get("ip"));
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"open-access").getBody().get("code")).isEqualTo("ACCESS_NOT_BLOCKED");
        owner.update("UPDATE rental SET status='RETURNING' WHERE id=?",c.rental());
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"returning").getBody().get("code")).isEqualTo("RESOURCE_RECOVERY_NOT_ALLOWED");
        assertUnknownOwned(c);
    }
    @Test void resourceLockRecoveryRequiresAnIntactManifestAndExistingOwner() {
        var c=unknownOwnedResourceLock();
        owner.update("UPDATE gpu_lease SET state='FREE',owner_rental_id=NULL,owner_vmid=NULL WHERE server_id=?",server);
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"unowned").getBody().get("code")).isEqualTo("RESOURCE_OWNER_MISMATCH");
        owner.update("UPDATE gpu_lease SET state='UNKNOWN',owner_rental_id=?,owner_vmid=? WHERE server_id=?",c.rental(),c.body().get("vmid"),server);
        owner.update("DELETE FROM resource_manifest WHERE rental_id=?",c.rental());
        assertThat(post(resourceLockRoute(c.rental()),admin,c.body(),"missing-manifest").getBody().get("code")).isEqualTo("RESOURCE_OWNERSHIP_INCOMPLETE");
        assertUnknownOwned(c);
    }
    @Test void resourceLockRecoveryIsIdempotentAcrossConcurrentRequests() throws Exception {
        var c=unknownOwnedResourceLock();long beforeVersion=version(c.rental());
        try(var executor=Executors.newFixedThreadPool(2)) {
            List<Callable<ResponseEntity<Map>>> calls=List.of(
                ()->post(resourceLockRoute(c.rental()),admin,c.body(),"concurrent-owned-recovery"),
                ()->post(resourceLockRoute(c.rental()),admin,c.body(),"concurrent-owned-recovery"));
            var results=executor.invokeAll(calls);
            var first=results.get(0).get();var second=results.get(1).get();
            assertThat(first.getStatusCode().value()).isEqualTo(200);assertThat(second.getStatusCode().value()).isEqualTo(200);
            assertThat(first.getBody()).isEqualTo(second.getBody());
        }
        assertThat(version(c.rental())).isEqualTo(beforeVersion+1);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_event WHERE rental_id=? AND reason_code='OWNED_RESOURCE_LOCK_RECONCILED'",Long.class,c.rental())).isEqualTo(1);
    }
    @Test void staleRunningCommandRequiresExplicitConfirmedReconciliation() {
        long id=reserve(),j=job(id,"PREPARE");var c=claim(j);resources(id);
        post("/api/v1/rentals/"+id+"/cancel",student,change(id),"cancel-running");long returning=job(id,"RETURN");
        assertThat(post("/api/internal/jobs/"+returning+"/claim",worker,Map.of("workerId","new-worker"),null).getStatusCode().value()).isEqualTo(409);
        var reconcile=new LinkedHashMap<String,Object>();reconcile.put("expectedVersion",version(id));reconcile.put("commandId",c.get("commandId"));reconcile.put("observedAt",now().toString());reconcile.put("terminalConfirmed",false);reconcile.put("accessBlocked",false);reconcile.put("operationId","checked-agent-operation");reconcile.put("reason","실제 Agent 결과 불명");
        assertThat(post("/api/v1/admin/jobs/"+j+"/reconcile",admin,reconcile,null).getBody().get("status")).isEqualTo("RUNNING");
        reconcile.put("expectedVersion",version(id));reconcile.put("terminalConfirmed",true);reconcile.put("accessBlocked",true);reconcile.put("reason","실제 Agent 작업 종료·접근 차단 확인");
        assertThat(post("/api/v1/admin/jobs/"+j+"/reconcile",admin,reconcile,null).getBody().get("status")).isEqualTo("SUPERSEDED");
        assertThat(post("/api/internal/jobs/"+returning+"/claim",worker,Map.of("workerId","new-worker"),null).getStatusCode().value()).isEqualTo(200);
        assertThat(owner.queryForObject("SELECT count(*) FROM ip_lease WHERE released_at IS NULL",Integer.class)).isEqualTo(1);
    }

    @Test void expiredPreparationIsRejectedBeforeTheNextSchedulerTick() {
        long id=reserve(),j=job(id,"PREPARE");time(NOW.plus(Duration.ofDays(4)));
        var result=post("/api/internal/jobs/"+j+"/claim",worker,Map.of("workerId","test-worker"),null);
        assertThat(result.getStatusCode().value()).isEqualTo(409);assertThat(result.getBody().get("code")).isEqualTo("CONTRACT_EXPIRED");
        assertThat(owner.queryForObject("SELECT status FROM job WHERE id=?",String.class,j)).isEqualTo("PENDING");
    }
    @Test void healthCannotClearUnknownCredentialOrOpenAccessOutsideTheSlot() {
        configure(server,1000);long id=reserve();var a=prepare(id);time(NOW.plusSeconds(1));
        owner.update("UPDATE rental SET reason_code='CREDENTIAL_APPLY_UNKNOWN' WHERE id=?",id);
        var report=new LinkedHashMap<String,Object>();report.put("desiredRevision",0);report.put("vmid",((Map)a.get("allocation")).get("vmid"));report.put("gpuGeneration",1);report.put("observedAt",now().toString());report.put("vmState","RUNNING");report.put("healthPassed",true);report.put("accessEnabled",false);report.put("hostKeyFingerprint","SHA256:simulation-only");
        assertThat(post("/api/internal/rentals/"+id+"/health",worker,report,null).getStatusCode().value()).isEqualTo(200);
        assertThat(owner.queryForObject("SELECT reason_code FROM rental WHERE id=?",String.class,id)).isEqualTo("CREDENTIAL_APPLY_UNKNOWN");
        assertThat(get("/api/v1/rentals/"+id+"/credential",student).getStatusCode().value()).isEqualTo(409);
        report.put("observedAt",now().plusSeconds(1).toString());report.put("accessEnabled",true);
        assertThat(post("/api/internal/rentals/"+id+"/health",worker,report,null).getStatusCode().value()).isEqualTo(422);
    }

    @Test void catalogAdministrationAuditAndNotificationsHaveUsableHttpResponses() {
        long id=reserve();
        java.util.function.BiFunction<String,String,Object> read=(path,token)->{
            var headers=new HttpHeaders();headers.setBearerAuth(token);
            var response=http.exchange(path,HttpMethod.GET,new HttpEntity<>(headers),String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            var body=json.read(response.getBody());capture("GET",path,200,body);return body;
        };
        assertThat(((Map)read.apply("/api/v1/me",student)).get("memberId")).isEqualTo(1001);
        for(String path:List.of("/api/v1/categories","/api/v1/servers","/api/v1/rentals","/api/v1/rentals/"+id+"/events","/api/v1/rentals/"+id+"/return-verifications"))assertThat(read.apply(path,student)).isInstanceOf(List.class);
        for(String path:List.of("/api/v1/admin/servers","/api/v1/admin/rentals","/api/v1/admin/events"))assertThat(read.apply(path,admin)).isInstanceOf(List.class);
        var notifications=(List<Map>)read.apply("/api/v1/notifications",student);assertThat(notifications).hasSize(1);
        assertThat(post("/api/v1/notifications/"+notifications.getFirst().get("id")+"/read",student,null,null).getBody().get("read")).isEqualTo(true);
        var category=post("/api/v1/admin/categories",admin,Map.of("name","새 분류","description","관리 등록 시험"),"new-category");assertThat(category.getStatusCode().value()).isEqualTo(201);
        var node=post("/api/v1/admin/servers",admin,Map.of("categoryId",category.getBody().get("id"),"displayName","새 서버","nodeName","test-new-node","cpuModel","test-cpu","memoryGb",64,"gpuModel","test-gpu"),"new-server");assertThat(node.getStatusCode().value()).isEqualTo(201);assertThat(node.getBody().get("status")).isEqualTo("UNKNOWN");
        var state=request(HttpMethod.PATCH,"/api/v1/admin/servers/"+node.getBody().get("id")+"/status",admin,Map.of("expectedVersion",0,"status","MAINTENANCE","reason","장비 연결 전 점검"),null);assertThat(state.getStatusCode().value()).isEqualTo(200);
    }
    long serverVersion(long id){return owner.queryForObject("SELECT version FROM server_node WHERE id=?",Long.class,id);}
    Map<String,Object> metadata(long id) {
        var r=owner.queryForMap("SELECT * FROM server_node WHERE id=?",id);
        return new LinkedHashMap<>(Map.of("expectedVersion",r.get("version"),"categoryId",r.get("category_id"),"displayName",r.get("display_name"),"cpuModel",r.get("cpu_model"),"memoryGb",r.get("memory_gb"),"gpuModel",r.get("gpu_model"),"reason","관리 표시 정보 갱신"));
    }
    ResponseEntity<Map> deleteServer(long id,String token,long version,String key) {
        return request(HttpMethod.DELETE,"/api/v1/admin/servers/"+id,token,Map.of("expectedVersion",version,"reason","운영 종료"),key);
    }
    @Test void adminMetadataEditReturnsFreshModelAndChecksRoleAndCategory() {
        var body=metadata(server);body.put("displayName","  갱신 서버  ");body.put("cpuModel","CPU 수정");body.put("memoryGb",128);body.put("gpuModel","없음");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,student,body,null).getStatusCode().value()).isEqualTo(403);
        var response=request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,body,null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);assertThat(response.getBody()).containsEntry("displayName","갱신 서버").containsEntry("memoryGb",128).containsEntry("gpuModel","없음").containsEntry("reservationAllowed",true);
        assertThat(owner.queryForObject("SELECT node_name FROM server_node WHERE id=?",String.class,server)).isEqualTo("demo-node-1");
        var invalid=metadata(server);invalid.put("categoryId",99999);
        var error=request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,invalid,null);
        assertThat(error.getStatusCode().value()).isEqualTo(422);assertThat(error.getBody().get("code")).isEqualTo("INVALID_CATEGORY");assertThat(error.getBody().get("requestId")).isEqualTo(error.getHeaders().getFirst("X-Request-Id"));
        assertThat(owner.queryForObject("SELECT count(*) FROM server_metadata_event",Integer.class)).isEqualTo(1);
    }
    @Test void metadataValidationRejectsPartialNullUnknownAndFractionalMemoryInputs() {
        var partial=metadata(server);partial.remove("memoryGb");
        var nullGpu=metadata(server);nullGpu.put("gpuModel",null);
        var fractional=metadata(server);fractional.put("memoryGb",1.5);
        var identity=metadata(server);identity.put("nodeName","different-node");
        for(var invalid:List.of(partial,nullGpu,fractional,identity))assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,invalid,null).getStatusCode().value()).isEqualTo(422);
        assertThat(serverVersion(server)).isZero();
    }
    @Test void metadataAndStatusShareVersionAndMissingServersReturn404() {
        var stale=metadata(server);
        request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server+"/status",admin,Map.of("expectedVersion",0,"status","MAINTENANCE","reason","점검"),null);
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,stale,null).getBody().get("code")).isEqualTo("VERSION_CONFLICT");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/99999",admin,stale,null).getStatusCode().value()).isEqualTo(404);
        assertThat(deleteServer(99999,admin,0,"missing").getStatusCode().value()).isEqualTo(404);
    }
    @Test void activeRentalAllowsDisplayNameButRejectsSpecificationAndCategoryChanges() {
        long rental=reserve();var body=metadata(server);body.put("displayName","현재 표시명 수정");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,body,null).getStatusCode().value()).isEqualTo(200);
        var snapshot=(Map)get("/api/v1/rentals/"+rental,student).getBody().get("serverSnapshot");assertThat(snapshot.get("displayName")).isNotEqualTo("현재 표시명 수정");
        body=metadata(server);body.put("memoryGb",512);
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,body,null).getBody().get("code")).isEqualTo("SERVER_IN_USE");
        body=metadata(server);body.put("categoryId",((Number)metadata(otherServer).get("categoryId")).longValue());body.put("cpuModel","물리 사양 변경");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,body,null).getStatusCode().value()).isEqualTo(409);
    }
    @Test void everyNonterminalRentalStateBlocksDeactivation() {
        long rental=reserve();
        for(String state:List.of("RESERVED","PROVISIONING","READY","ACTIVE","RETAINED","RETURNING")) {
            owner.update("UPDATE rental SET status=?,retained_reason=? WHERE id=?",state,state.equals("RETAINED")?"BETWEEN_SLOTS":null,rental);
            var result=deleteServer(server,admin,serverVersion(server),"delete-"+state);
            assertThat(result.getStatusCode().value()).isEqualTo(409);assertThat(result.getBody().get("code")).isEqualTo("SERVER_IN_USE");
        }
        assertThat(owner.queryForObject("SELECT deleted_at FROM server_node WHERE id=?",java.sql.Timestamp.class,server)).isNull();
    }
    @Test void deleteIs204IdempotentAndPreservesRegistration() {
        assertThat(deleteServer(server,student,0,"unauthorized").getStatusCode().value()).isEqualTo(403);
        var response=deleteServer(server,admin,0,"delete-once");assertThat(response.getStatusCode().value()).isEqualTo(204);assertThat(response.getBody()).isNull();
        assertThat(deleteServer(server,admin,0,"delete-once").getStatusCode().value()).isEqualTo(204);
        assertThat(deleteServer(server,admin,0,"delete-again").getStatusCode().value()).isEqualTo(204);
        assertThat(deleteServer(server,admin,1,"delete-once").getBody().get("code")).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(serverVersion(server)).isEqualTo(1);assertThat(owner.queryForObject("SELECT count(*) FROM server_metadata_event",Integer.class)).isEqualTo(1);
    }
    @Test void deletedServersDisappearFromListsAndRejectAvailabilityAndNewReservations() {
        deleteServer(server,admin,0,"deleted");
        for(String path:List.of("/api/v1/servers","/api/v1/admin/servers","/api/internal/inventory")) {
            var h=new HttpHeaders();h.setBearerAuth(path.contains("internal")?worker:path.contains("admin")?admin:student);
            var response=http.exchange(path,HttpMethod.GET,new HttpEntity<>(null,h),List.class);
            assertThat(response.getBody()).noneMatch(x->((Number)((Map)x).get("id")).longValue()==server);capture("GET",path,200,response.getBody());
        }
        assertThat(get("/api/v1/servers/"+server+"/availability?from=2026-10-07T00:00:00Z&to=2026-10-09T00:00:00Z",student).getStatusCode().value()).isEqualTo(404);
        assertThat(post("/api/v1/rentals",student,reservation(server),"after-delete").getStatusCode().value()).isEqualTo(404);
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,metadata(server),null).getStatusCode().value()).isEqualTo(404);
        assertThat(post("/api/internal/servers/"+server+"/observation",worker,Map.of("observedAt",NOW.toString(),"status","AVAILABLE"),null).getStatusCode().value()).isEqualTo(404);
    }
    @Test void oldRentalSnapshotAndServerNameSurviveEditAndDeletion() {
        long rental=reserve();var before=(Map)get("/api/v1/rentals/"+rental,student).getBody().get("serverSnapshot");
        post("/api/v1/rentals/"+rental+"/cancel",student,change(rental),"cancel");
        var edit=metadata(server);edit.put("displayName","이후 표시명");edit.put("gpuModel","다른 GPU 표시");
        request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,edit,null);deleteServer(server,admin,1,"deleted");
        assertThat(get("/api/v1/rentals/"+rental,student).getBody().get("serverSnapshot")).isEqualTo(before);
        var h=new HttpHeaders();h.setBearerAuth(student);var list=http.exchange("/api/v1/rentals",HttpMethod.GET,new HttpEntity<>(null,h),List.class);
        assertThat(((Map)list.getBody().getFirst()).get("serverName")).isEqualTo(before.get("displayName"));
        assertThat(owner.queryForObject("SELECT count(*) FROM rental WHERE id=?",Integer.class,rental)).isEqualTo(1);
        assertThatThrownBy(()->owner.update("UPDATE rental SET server_snapshot='{}'::jsonb WHERE id=?",rental)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void newRentalCapturesEditedMetadataAndDuplicateDisplayNamesAreAllowed() {
        var edit=metadata(server);edit.put("displayName",metadata(otherServer).get("displayName"));edit.put("cpuModel","새 CPU");
        assertThat(request(HttpMethod.PATCH,"/api/v1/admin/servers/"+server,admin,edit,null).getStatusCode().value()).isEqualTo(200);
        long rental=reserve();var snapshot=(Map)get("/api/v1/rentals/"+rental,student).getBody().get("serverSnapshot");assertThat(snapshot).containsEntry("cpuModel","새 CPU").containsEntry("origin","RESERVATION");
    }
    @Test void unknownResourcesBlockDeletionEvenWithoutAnActiveRental() {
        owner.update("UPDATE gpu_lease SET state='UNKNOWN' WHERE server_id=?",server);
        assertThat(deleteServer(server,admin,0,"unknown").getBody().get("code")).isEqualTo("SERVER_RESOURCES_IN_USE");
        assertThatThrownBy(()->owner.update("UPDATE server_node SET deleted_at=?,deleted_by=9001,deleted_reason='unsafe' WHERE id=?",Rows.ts(NOW),server)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void concurrentIdenticalDeleteCommitsOneAuditAndOneVersion() throws Exception {
        try(var executor=Executors.newFixedThreadPool(12)) {
            List<Callable<Integer>> tasks=new ArrayList<>();for(int i=0;i<12;i++)tasks.add(()->deleteServer(server,admin,0,"concurrent-delete").getStatusCode().value());
            for(var future:executor.invokeAll(tasks))assertThat(future.get()).isEqualTo(204);
        }
        assertThat(serverVersion(server)).isEqualTo(1);assertThat(owner.queryForObject("SELECT count(*) FROM server_metadata_event",Integer.class)).isEqualTo(1);
    }
    @Test void deletionAndReservationRaceCannotDeactivateAnActiveServer() throws Exception {
        long category=((Number)metadata(server).get("categoryId")).longValue();
        for(int iteration=0;iteration<12;iteration++) {
            long node=owner.queryForObject("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,status,inventory_source) VALUES (?,'race',?,'CPU',64,'GPU','AVAILABLE','DEMO') RETURNING id",Long.class,category,"delete-race-"+iteration);
            owner.update("INSERT INTO gpu_lease(server_id) VALUES (?)",node);
            try(var pool=Executors.newFixedThreadPool(2)) {
                var gate=new CountDownLatch(1);
                var deleting=pool.submit(()->{gate.await();return deleteServer(node,admin,0,"race-delete-"+node).getStatusCode().value();});
                var booking=pool.submit(()->{gate.await();return post("/api/v1/rentals",student,reservation(node),"race-book-"+node).getStatusCode().value();});gate.countDown();
                int d=deleting.get(),b=booking.get();assertThat((d==204&&b==404)||(d==409&&b==201)).isTrue();
                if(b==201) {
                    long id=owner.queryForObject("SELECT id FROM rental WHERE server_id=?",Long.class,node);
                    assertThat(post("/api/v1/rentals/"+id+"/cancel",student,change(id),"race-cancel-"+node).getStatusCode().value()).isLessThan(300);
                }
            }
            assertThat(owner.queryForObject("SELECT count(*) FROM server_node s JOIN rental r ON r.server_id=s.id WHERE s.id=? AND s.deleted_at IS NOT NULL AND r.status NOT IN ('RETURNED','CANCELLED')",Integer.class,node)).isZero();
        }
    }
    @Test void availabilityReturnsAuthoritativeClockAndExactSwitchGapIsAccepted() {
        reserve();var result=get("/api/v1/servers/"+server+"/availability?from=2026-10-08T00:00:00Z&to=2026-10-09T00:00:00Z",student);
        assertThat(Instant.parse(result.getBody().get("serverNow").toString())).isEqualTo(NOW);
        assertThat(Instant.parse(result.getBody().get("reservationHorizonAt").toString())).isEqualTo(NOW.atZone(ReservationCompiler.ZONE).plusMonths(2).toInstant());
        var body=new HashMap<>(reservation(server));body.put("slots",List.of(Map.of("startAt","2026-10-08T07:00:00Z","endAt","2026-10-08T08:00:00Z")));
        assertThat(post("/api/v1/rental-previews",other,body,null).getBody().get("canReserve")).isEqualTo(true);
        body.put("slots",List.of(Map.of("startAt","2026-10-08T06:59:59.999999999Z","endAt","2026-10-08T08:00:00Z")));
        assertThat(post("/api/v1/rental-previews",other,body,null).getBody().get("canReserve")).isEqualTo(false);
    }
    @Test void continuousLongRentalStaysActiveAndConnectableAtKoreanMidnight() {
        configure(server,1000);
        var body=new HashMap<>(reservation(server));body.put("slots",List.of(Map.of("startAt","2026-10-07T00:00:00+09:00","endAt","2026-10-10T00:00:00+09:00")));
        long rental=((Number)post("/api/v1/rentals",student,body,"long").getBody().get("id")).longValue();prepare(rental);post("/api/internal/lifecycle/tick",worker,null,null);
        time(Instant.parse("2026-10-08T15:01:00Z"));refresh(server);long start=job(rental,"START");var c=claim(start);var a=allocation(start,c);
        assertThat(post("/api/internal/jobs/"+start+"/observation",worker,observation(c,a,"RUNNING",true,null),null).getBody().get("status")).isEqualTo("ACTIVE");
        post("/api/internal/lifecycle/tick",worker,null,null);
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(true);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='STOP'",Integer.class,rental)).isZero();
    }

    Map<String,Object> prepareNowInput(long rental) {
        return Map.of("expectedVersion",version(rental),"serverId",server,"memberId",1001,"reason","기존 예약 즉시 준비 시험");
    }
    @Test void prepareNowReusesOneJobPreservesUsageTimesAndReservesPreparationInterval() {
        long rental=reserve(),prepare=job(rental,"PREPARE");var input=prepareNowInput(rental);
        var path="/api/v1/admin/rentals/"+rental+"/prepare-now";
        var response=post(path,admin,input,"prepare-now");
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).containsEntry("jobId",(int)prepare).containsEntry("status","RESERVED");
        assertThat(post(path,admin,input,"prepare-now").getBody()).isEqualTo(response.getBody());
        assertThat(post(path,admin,prepareNowInput(rental),"prepare-again").getStatusCode().value()).isEqualTo(202);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='PREPARE'",Integer.class,rental)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT next_run_at FROM job WHERE id=?",java.sql.Timestamp.class,prepare).toInstant()).isEqualTo(NOW);
        assertThat(owner.queryForObject("SELECT contract_start FROM rental WHERE id=?",java.sql.Timestamp.class,rental).toInstant()).isEqualTo(Instant.parse("2026-10-08T00:00:00Z"));
        assertThat(owner.queryForObject("SELECT blocked_start FROM rental_slot WHERE rental_id=?",java.sql.Timestamp.class,rental).toInstant()).isEqualTo(NOW);
        assertThat(version(rental)).isEqualTo(1);
        var during=new HashMap<>(reservation(server));during.put("slots",List.of(Map.of("startAt",NOW.plusSeconds(7200).toString(),"endAt",NOW.plusSeconds(10800).toString())));
        assertThat(post("/api/v1/rentals",other,during,"during-prepare").getStatusCode().value()).isEqualTo(409);
        var claim=post("/api/internal/jobs/"+prepare+"/claim",worker,Map.of("workerId","test-worker"),null);
        assertThat(claim.getStatusCode().value()).isEqualTo(200);
        assertThat(get("/api/v1/rentals/"+rental,student).getBody().get("status")).isEqualTo("PROVISIONING");
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(false);
    }
    @Test void concurrentPrepareNowRequestsShareOneJobAndOneChange() throws Exception {
        long rental=reserve();var body=prepareNowInput(rental);String path="/api/v1/admin/rentals/"+rental+"/prepare-now";
        try(var executor=Executors.newFixedThreadPool(2)) {
            var requests=List.<Callable<Integer>>of(()->post(path,admin,body,"same-prepare").getStatusCode().value(),()->post(path,admin,body,"same-prepare").getStatusCode().value());
            for(var result:executor.invokeAll(requests))assertThat(result.get()).isEqualTo(202);
        }
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='PREPARE'",Integer.class,rental)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM rental_event WHERE rental_id=? AND reason_code='ADMIN_PREPARE_NOW'",Integer.class,rental)).isEqualTo(1);
        assertThat(version(rental)).isEqualTo(1);
    }
    long reserveEarlier() {
        var body=new HashMap<>(reservation(server));body.put("slots",List.of(Map.of("startAt",NOW.plusSeconds(3600).toString(),"endAt",NOW.plusSeconds(5400).toString())));
        var response=post("/api/v1/rentals",other,body,"earlier");assertThat(response.getStatusCode().value()).isEqualTo(201);return ((Number)response.getBody().get("id")).longValue();
    }
    @Test void prepareNowChecksOtherReservationsAndRollsBackChangesOnConflict() {
        long rental=reserve(),prepare=job(rental,"PREPARE");reserveEarlier();
        var before=owner.queryForMap("SELECT next_run_at FROM job WHERE id=?",prepare);
        var result=post("/api/v1/admin/rentals/"+rental+"/prepare-now",admin,prepareNowInput(rental),"conflict");
        assertThat(result.getStatusCode().value()).isEqualTo(409);assertThat(result.getBody().get("code")).isEqualTo("SLOT_CONFLICT");
        assertThat(owner.queryForMap("SELECT next_run_at FROM job WHERE id=?",prepare)).isEqualTo(before);
        assertThat(version(rental)).isZero();
    }
    @Test void prepareNowKeepsMinimumSwitchTimeAndDoesNotPreemptForeignResources() {
        long rental=reserve(),earlier=reserveEarlier();time(NOW.plusSeconds(7200));
        String path="/api/v1/admin/rentals/"+rental+"/prepare-now";
        var result=post(path,admin,prepareNowInput(rental),"short-gap");
        assertThat(result.getStatusCode().value()).isEqualTo(409);assertThat(result.getBody().get("code")).isEqualTo("INSUFFICIENT_SWITCH_TIME");
        time(NOW.plusSeconds(10800));resources(earlier);
        result=post(path,admin,prepareNowInput(rental),"owned-resource");
        assertThat(result.getStatusCode().value()).isEqualTo(409);assertThat(result.getBody().get("code")).isEqualTo("SERVER_RESOURCES_IN_USE");
        assertThat(version(rental)).isZero();
    }
    @Test void prepareNowRequiresAdminMatchingOwnerServerAndCurrentVersion() {
        long rental=reserve();String path="/api/v1/admin/rentals/"+rental+"/prepare-now";
        assertThat(post(path,student,prepareNowInput(rental),"no-admin").getStatusCode().value()).isEqualTo(403);
        for(String field:List.of("serverId","memberId","expectedVersion")) {
            var body=new HashMap<>(prepareNowInput(rental));body.put(field,999999);
            assertThat(post(path,admin,body,"wrong-"+field).getStatusCode().value()).isEqualTo(409);
        }
        assertThat(version(rental)).isZero();
        post("/api/v1/rentals/"+rental+"/cancel",student,change(rental),"cancel");
        assertThat(post(path,admin,prepareNowInput(rental),"cancelled").getStatusCode().value()).isEqualTo(409);
    }
    @Test void prepareNowDoesNotRecreateFailedOrDuplicatePreparationJobs() {
        long rental=reserve(),prepare=job(rental,"PREPARE");String path="/api/v1/admin/rentals/"+rental+"/prepare-now";
        owner.update("UPDATE job SET status='FAILED' WHERE id=?",prepare);
        assertThat(post(path,admin,prepareNowInput(rental),"failed-prepare").getStatusCode().value()).isEqualTo(409);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='PREPARE'",Integer.class,rental)).isEqualTo(1);
        assertThat(version(rental)).isZero();
    }

    @Test void startNowReusesStartJobAndOpensConnectionOnlyAfterActualWorkerObservation() {
        configure(server,1000);long rental=reserve();var allocation=prepare(rental);
        post("/api/internal/lifecycle/tick",worker,null,null);long start=job(rental,"START");
        var input=prepareNowInput(rental);String path="/api/v1/admin/rentals/"+rental+"/start-now";
        var response=post(path,admin,input,"start-now");assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().get("jobId")).isEqualTo((int)start);
        assertThat(post(path,admin,input,"start-now").getBody()).isEqualTo(response.getBody());
        assertThat(owner.queryForObject("SELECT contract_start FROM rental WHERE id=?",java.sql.Timestamp.class,rental).toInstant()).isEqualTo(NOW);
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(false);
        var c=post("/api/internal/jobs/"+start+"/claim",worker,Map.of("workerId","test-worker"),null);assertThat(c.getStatusCode().value()).isEqualTo(200);
        assertThat(post("/api/internal/jobs/"+start+"/observation",worker,observation(c.getBody(),allocation,"RUNNING",true,null),null).getBody().get("status")).isEqualTo("ACTIVE");
        assertThat(get("/api/v1/rentals/"+rental+"/connection",student).getBody().get("available")).isEqualTo(true);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='PREPARE'",Integer.class,rental)).isEqualTo(1);
        assertThat(owner.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='START'",Integer.class,rental)).isEqualTo(1);
    }
    @Test void startNowPreservesOwnershipReadinessAndConflictGuards() {
        long rental=reserve();String path="/api/v1/admin/rentals/"+rental+"/start-now";
        assertThat(post(path,student,prepareNowInput(rental),"student-start").getStatusCode().value()).isEqualTo(403);
        assertThat(post(path,admin,prepareNowInput(rental),"unprepared").getStatusCode().value()).isEqualTo(409);
        configure(server,1000);prepare(rental);post("/api/internal/lifecycle/tick",worker,null,null);reserveEarlier();
        var response=post(path,admin,prepareNowInput(rental),"conflicting-start");
        assertThat(response.getStatusCode().value()).isEqualTo(409);assertThat(response.getBody().get("code")).isEqualTo("SLOT_CONFLICT");
        assertThat(owner.queryForObject("SELECT contract_start FROM rental WHERE id=?",java.sql.Timestamp.class,rental).toInstant()).isEqualTo(Instant.parse("2026-10-08T00:00:00Z"));
    }

    @Test void adminPatchDeletePreflightAndUnauthorizedRequestIdAreReadableFromAllowedOrigin() {
        for(String method:List.of("PATCH","DELETE")) {
            var h=new HttpHeaders();h.setOrigin("http://127.0.0.1:3001");h.set("Access-Control-Request-Method",method);h.set("Access-Control-Request-Headers","authorization,content-type,idempotency-key");
            var response=http.exchange("/api/v1/admin/servers/"+server,HttpMethod.OPTIONS,new HttpEntity<>(null,h),String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://127.0.0.1:3001");
        }
        var h=new HttpHeaders();h.setOrigin("http://127.0.0.1:3001");
        var response=http.exchange("/api/v1/admin/servers",HttpMethod.GET,new HttpEntity<>(null,h),Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);assertThat(response.getHeaders().getAccessControlExposeHeaders()).contains("X-Request-Id");
        assertThat(response.getBody().get("requestId")).isEqualTo(response.getHeaders().getFirst("X-Request-Id"));
    }

}
