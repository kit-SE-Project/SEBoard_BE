package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"allday.scheduler-enabled=false","allday.dev-auth-enabled=false","allday.dev-seed-enabled=false","allday.demo-auth-enabled=true","allday.demo-seed-enabled=true","allday.demo-worker-enabled=true","allday.demo-runner-enabled=false"})
@Import(DemoAuthIntegrationTest.TimeConfiguration.class)
@EnabledIfEnvironmentVariable(named="DB_URL",matches=".*[/]capdi_test")
class DemoAuthIntegrationTest {
    static final Instant NOW=Instant.parse("2026-10-12T03:00:00Z");
    static class MutableClock extends Clock{Instant at=NOW;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return at;}}
    @TestConfiguration static class TimeConfiguration{@Bean @Primary Clock clockForDemo(){return new MutableClock();}}
    @Autowired TestRestTemplate http;@Autowired DemoSeed seed;@Autowired DemoWorker worker;@Autowired LifecycleService lifecycle;@Autowired Clock clock;
    JdbcTemplate db;
    @BeforeEach void reset(){
        ((MutableClock)clock).at=NOW;
        db=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_MIGRATION_USER"),System.getenv("DB_MIGRATION_PASSWORD")));
        assertThat(db.queryForObject("SELECT current_database()",String.class)).isEqualTo("capdi_test");
        db.execute("TRUNCATE category,external_subject_binding,accounts,roles,auth_login_throttle RESTART IDENTITY CASCADE");seed.run(null);
    }
    ResponseEntity<Map> login(String user,String password,boolean large){
        var h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);var form=new LinkedMultiValueMap<String,String>();form.add("username",user);form.add("password",password);form.add("largeLogin",Boolean.toString(large));
        return http.postForEntity("/formLogin",new HttpEntity<>(form,h),Map.class);
    }
    Map tokens(String user){var r=login(user,DemoSeed.PASSWORD,false);assertThat(r.getStatusCode().value()).isEqualTo(200);return r.getBody();}
    ResponseEntity<Map> request(HttpMethod method,String path,String token,Object body){var h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);if(token!=null)h.setBearerAuth(token);h.set("Idempotency-Key",UUID.randomUUID().toString());return http.exchange(path,method,new HttpEntity<>(body,h),Map.class);}
    ResponseEntity<Map> get(String path,String token){return request(HttpMethod.GET,path,token,null);}
    ResponseEntity<Map> refresh(String token){return request(HttpMethod.POST,"/refresh",null,Map.of("refreshToken",token));}
    String access(Map tokens){return tokens.get("accessToken").toString();}
    String refreshToken(Map tokens){return tokens.get("refreshToken").toString();}
    long account(String login){return db.queryForObject("SELECT account_id FROM accounts WHERE login_id=?",Long.class,login);}
    long node(int number){return db.queryForObject("SELECT id FROM server_node WHERE node_name=?",Long.class,"presentation-node-"+number);}
    long rental(String text){return db.queryForObject("SELECT id FROM rental WHERE purpose LIKE ?",Long.class,"%"+text+"%");}
    void drain(){
        lifecycle.tick();
        for(var row:db.queryForList("SELECT id FROM job WHERE status='PENDING' AND next_run_at<=? ORDER BY priority DESC,id",Rows.ts(clock.instant()))) {
            try{worker.claim(Rows.number(row,"id"));worker.complete(Rows.number(row,"id"));}catch(ApiProblem e){if(!Set.of("STALE_REVISION","PREPARATION_NOT_READY","JOB_NOT_PENDING","CONTRACT_EXPIRED").contains(e.code))throw e;}
        }
    }
    @Test void formLoginUsesBcryptAndReturnsCurrentRolesWithDistinctMemberMapping(){
        var t=tokens("junseok@demo.local");assertThat(access(t).split("\\.")).hasSize(3);
        var me=get("/mypage",access(t));assertThat(me.getStatusCode().value()).isEqualTo(200);assertThat(me.getBody().get("roles")).asList().contains("ROLE_USER","ROLE_KUMOH");assertThat(me.getBody().get("rentalEligible")).isEqualTo(true);
        assertThat(((Number)me.getBody().get("accountId")).longValue()).isNotEqualTo(((Number)me.getBody().get("memberId")).longValue());
        assertThat(db.queryForObject("SELECT password_hash FROM accounts WHERE login_id='junseok@demo.local'",String.class)).startsWith("$2").doesNotContain(DemoSeed.PASSWORD);
        assertThat(me.getBody()).doesNotContainKeys("password","passwordHash","refreshToken");
        assertThat(db.queryForObject("SELECT refresh_hash FROM auth_session LIMIT 1",String.class)).doesNotContain(refreshToken(t));
    }
    @Test void workerDatabaseRoleCannotReadPasswordsOrAuthenticationSessions(){
        assertThat(db.queryForObject("SELECT has_table_privilege('capdi_worker','accounts','SELECT') OR has_table_privilege('capdi_worker','auth_session','SELECT')",Boolean.class)).isFalse();
        assertThat(db.queryForObject("SELECT has_table_privilege('capdi_app','accounts','SELECT')",Boolean.class)).isTrue();
    }
    @Test void failedLoginIsPersistedAndFifthAttemptLocksAccount(){
        for(int i=0;i<5;i++)assertThat(login("junseok@demo.local","incorrect",false).getStatusCode().value()).isEqualTo(401);
        assertThat(login("junseok@demo.local",DemoSeed.PASSWORD,false).getStatusCode().value()).isEqualTo(429);
        ((MutableClock)clock).at=NOW.plusSeconds(601);assertThat(login("junseok@demo.local",DemoSeed.PASSWORD,false).getStatusCode().value()).isEqualTo(200);
    }
    @Test void loginRequiresFormEncodingAndReportsClientErrorsInsteadOfServerFailure(){
        assertThat(request(HttpMethod.POST,"/formLogin",null,Map.of("username","junseok@demo.local","password",DemoSeed.PASSWORD)).getStatusCode().value()).isEqualTo(415);
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(http.postForEntity("/formLogin",new HttpEntity<>(new LinkedMultiValueMap<String,String>(),headers),Map.class).getStatusCode().value()).isEqualTo(422);
    }
    @Test void suspendedAccountCannotLoginAndDeletingAccountRevokesExistingAccess(){
        assertThat(login("suspended@demo.local",DemoSeed.PASSWORD,false).getStatusCode().value()).isEqualTo(401);
        var t=tokens("junseok@demo.local");db.update("UPDATE accounts SET status='TEMP_DELETED' WHERE account_id=?",account("junseok@demo.local"));
        assertThat(get("/mypage",access(t)).getStatusCode().value()).isEqualTo(401);assertThat(refresh(refreshToken(t)).getStatusCode().value()).isEqualTo(401);
    }
    @Test void rejectsMissingForgedExpiredAndLegacyTokens(){
        assertThat(get("/api/v1/me",null).getStatusCode().value()).isEqualTo(401);assertThat(get("/api/v1/me","bad.jwt.token").getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/me",System.getenv("DEV_STUDENT_ONE_TOKEN")).getStatusCode().value()).isEqualTo(401);
        var t=tokens("junseok@demo.local");String[] bits=access(t).split("\\.");bits[1]=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"accountId\":1,\"authorities\":[\"ROLE_ADMIN\"]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(get("/api/v1/me",String.join(".",bits)).getStatusCode().value()).isEqualTo(401);
        ((MutableClock)clock).at=NOW.plusSeconds(1801);assertThat(get("/api/v1/me",access(t)).getStatusCode().value()).isEqualTo(401);
    }
    @Test void refreshRotatesAtomicallyAndTokenTypesCannotBeInterchanged(){
        var t=tokens("junseok@demo.local");assertThat(get("/api/v1/me",refreshToken(t)).getStatusCode().value()).isEqualTo(401);assertThat(refresh(access(t)).getStatusCode().value()).isEqualTo(401);
        var next=refresh(refreshToken(t));assertThat(next.getStatusCode().value()).isEqualTo(200);assertThat(refreshToken(next.getBody())).isNotEqualTo(refreshToken(t));
        assertThat(refresh(refreshToken(t)).getStatusCode().value()).isEqualTo(401);assertThat(get("/mypage",access(next.getBody())).getStatusCode().value()).isEqualTo(200);
    }
    @Test void twoConcurrentRefreshRequestsHaveExactlyOneSuccess()throws Exception{
        var t=tokens("junseok@demo.local");try(var executor=Executors.newFixedThreadPool(2)){
            var statuses=new ArrayList<Integer>();for(var future:executor.invokeAll(List.<Callable<Integer>>of(()->refresh(refreshToken(t)).getStatusCode().value(),()->refresh(refreshToken(t)).getStatusCode().value())))statuses.add(future.get());
            assertThat(statuses).containsExactlyInAnyOrder(200,401);
        }
    }
    @Test void logoutRejectsAnotherSessionAndRevokesAccessAndRefresh(){
        var t=tokens("junseok@demo.local");var other=tokens("student2@demo.local");assertThat(request(HttpMethod.POST,"/logoutProc",access(t),Map.of("refreshToken",refreshToken(other))).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.POST,"/logoutProc",access(t),Map.of("refreshToken",refreshToken(t))).getStatusCode().value()).isEqualTo(200);
        assertThat(get("/mypage",access(t)).getStatusCode().value()).isEqualTo(401);assertThat(refresh(refreshToken(t)).getStatusCode().value()).isEqualTo(401);assertThat(get("/mypage",access(other)).getStatusCode().value()).isEqualTo(200);
    }
    @Test void thirtyDayRefreshWorksAfterOrdinaryRefreshHasExpired(){
        var t=login("junseok@demo.local",DemoSeed.PASSWORD,true).getBody();var normal=tokens("student2@demo.local");((MutableClock)clock).at=NOW.plus(Duration.ofDays(2));
        assertThat(refresh(refreshToken(t)).getStatusCode().value()).isEqualTo(200);assertThat(refresh(refreshToken(normal)).getStatusCode().value()).isEqualTo(401);
    }
    @Test void currentDatabaseRoleRemovalRevokesAdminDespiteOldJwtClaim(){
        var t=tokens("admin@demo.local");var headers=new HttpHeaders();headers.setBearerAuth(access(t));assertThat(http.exchange("/api/v1/admin/servers",HttpMethod.GET,new HttpEntity<>(headers),String.class).getStatusCode().value()).isEqualTo(200);
        db.update("DELETE FROM role_accounts WHERE account_id=? AND role_id=(SELECT role_id FROM roles WHERE name='ROLE_ADMIN')",account("admin@demo.local"));
        assertThat(get("/api/v1/admin/servers",access(t)).getStatusCode().value()).isEqualTo(403);assertThat(get("/mypage",access(t)).getBody().get("roles")).asList().doesNotContain("ROLE_ADMIN");
    }
    @Test void eligibilityAndOwnershipAndWorkerBoundaryUseVerifiedUser(){
        var student=tokens("junseok@demo.local");var other=tokens("student2@demo.local");var guest=tokens("guest@demo.local");
        var input=Map.of("serverId",node(1),"purpose","발표 예약","slots",List.of(Map.of("startAt",NOW.plusSeconds(60).toString(),"endAt",NOW.plusSeconds(3600).toString())));
        assertThat(request(HttpMethod.POST,"/api/v1/rentals",access(guest),input).getStatusCode().value()).isEqualTo(403);
        assertThat(request(HttpMethod.POST,"/api/v1/rental-previews",access(guest),input).getStatusCode().value()).isEqualTo(403);
        long active=rental("이용 중");
        long version=db.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,active);
        assertThat(request(HttpMethod.POST,"/api/v1/rentals/"+active+"/cancel",access(student),Map.of("expectedVersion",version)).getStatusCode().value()).isLessThan(300);
        long id=((Number)request(HttpMethod.POST,"/api/v1/rentals",access(student),input).getBody().get("id")).longValue();
        assertThat(get("/api/v1/rentals/"+id,access(other)).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/internal/jobs",access(student)).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/me",System.getenv("WORKER_TOKEN")).getStatusCode().value()).isEqualTo(401);
        var spoof=new HashMap<>(input);spoof.put("memberId",10001);assertThat(request(HttpMethod.POST,"/api/v1/rentals",access(student),spoof).getStatusCode().value()).isEqualTo(422);
    }
    @Test void demoWorkerCompletesRealReservationResetAndFiveItemReturnFlow(){
        ((MutableClock)clock).at=NOW.plusSeconds(10);drain();var t=tokens("junseok@demo.local");long id=rental("이용 중");
        assertThat(get("/api/v1/rentals/"+id,access(t)).getBody().get("status")).isEqualTo("ACTIVE");assertThat(get("/api/v1/rentals/"+id+"/connection",access(t)).getBody().get("available")).isEqualTo(true);
        String initial=get("/api/v1/rentals/"+id+"/credential",access(t)).getBody().get("password").toString();assertThat(initial).hasSize(13);
        long version=db.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,id);
        assertThat(request(HttpMethod.POST,"/api/v1/rentals/"+id+"/credential-resets",access(t),Map.of("expectedVersion",version)).getStatusCode().value()).isEqualTo(202);drain();
        assertThat(get("/api/v1/rentals/"+id+"/credential",access(t)).getBody().get("password")).isNotEqualTo(initial);
        version=db.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,id);
        assertThat(request(HttpMethod.POST,"/api/v1/rentals/"+id+"/return",access(t),Map.of("expectedVersion",version)).getStatusCode().value()).isEqualTo(202);drain();
        assertThat(get("/api/v1/rentals/"+id,access(t)).getBody().get("status")).isEqualTo("RETURNED");assertThat(get("/api/v1/rentals/"+id+"/credential",access(t)).getStatusCode().value()).isEqualTo(403);
        assertThat(db.queryForObject("SELECT count(*) FROM verification_item i JOIN return_verification v ON v.id=i.verification_id WHERE v.rental_id=? AND i.passed AND (i.evidence->>'simulation')::boolean",Integer.class,id)).isEqualTo(5);
        assertThat(db.queryForObject("SELECT count(*) FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",Integer.class,id)).isZero();
    }
    @Test void simulatorCannotClaimRealInventory(){long id=db.queryForObject("SELECT id FROM job WHERE rental_id=? AND type='PREPARE'",Long.class,rental("이용 중"));db.update("UPDATE server_node SET inventory_source='MANUAL' WHERE id=?",node(3));assertThatThrownBy(()->worker.claim(id)).isInstanceOf(ApiProblem.class).hasMessageContaining("실제 인벤토리");}
    @Test void cancellationBetweenSimulationClaimAndCompletionDoesNotBlockReturn(){
        long id=rental("이용 중");long prepare=db.queryForObject("SELECT id FROM job WHERE rental_id=? AND type='PREPARE'",Long.class,id);worker.claim(prepare);
        var t=tokens("junseok@demo.local");long version=db.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,id);
        assertThat(request(HttpMethod.POST,"/api/v1/rentals/"+id+"/cancel",access(t),Map.of("expectedVersion",version)).getStatusCode().value()).isEqualTo(202);
        worker.complete(prepare);drain();assertThat(get("/api/v1/rentals/"+id,access(t)).getBody().get("status")).isEqualTo("RETURNED");
        assertThat(db.queryForObject("SELECT status FROM job WHERE id=?",String.class,prepare)).isEqualTo("SUPERSEDED");
    }
    @Test void resetDuringClaimedStartConvergesAndLongPasswordsFailAsInputErrors(){
        assertThat(login("junseok@demo.local","가".repeat(30),false).getStatusCode().value()).isEqualTo(422);
        ((MutableClock)clock).at=NOW.plusSeconds(10);drain();long id=rental("이용 중");var t=tokens("junseok@demo.local");
        long job=db.queryForObject("INSERT INTO job(server_id,rental_id,intent_key,type,desired_revision,priority,next_run_at,slot_id) SELECT r.server_id,r.id,'demo-test-start','START',r.desired_revision,10,?,s.id FROM rental r JOIN rental_slot s ON s.rental_id=r.id WHERE r.id=? RETURNING job.id",Long.class,Rows.ts(clock.instant()),id);
        worker.claim(job);long version=db.queryForObject("SELECT version FROM rental WHERE id=?",Long.class,id);
        assertThat(request(HttpMethod.POST,"/api/v1/rentals/"+id+"/credential-resets",access(t),Map.of("expectedVersion",version)).getStatusCode().value()).isEqualTo(202);
        worker.complete(job);drain();drain();assertThat(db.queryForObject("SELECT status FROM job WHERE id=?",String.class,job)).isEqualTo("SUPERSEDED");
        assertThat(get("/api/v1/rentals/"+id+"/credential",access(t)).getBody().get("credentialVersion")).isEqualTo(2);
        assertThat(get("/api/v1/rentals/"+id+"/connection",access(t)).getBody().get("available")).isEqualTo(true);
    }
    @Test void seedIsIdempotentAndDoesNotResetPasswordsOrReservations(){long before=db.queryForObject("SELECT count(*) FROM rental",Long.class);db.update("UPDATE accounts SET name='변경 보존' WHERE login_id='junseok@demo.local'");seed.run(null);assertThat(db.queryForObject("SELECT count(*) FROM rental",Long.class)).isEqualTo(before);assertThat(db.queryForObject("SELECT name FROM accounts WHERE login_id='junseok@demo.local'",String.class)).isEqualTo("변경 보존");}
    @Test void betweenSlotsRetainsSameVmAndCredentialWhileGpuIsReleased(){
        ((MutableClock)clock).at=NOW.plusSeconds(10);drain();long id=rental("슬롯 사이 VM 보존");var t=tokens("retained@demo.local");long vmid=db.queryForObject("SELECT vmid FROM vm_instance WHERE rental_id=?",Long.class,id);String secret=get("/api/v1/rentals/"+id+"/credential",access(t)).getBody().get("password").toString();
        ((MutableClock)clock).at=NOW.plusSeconds(45);drain();assertThat(get("/api/v1/rentals/"+id,access(t)).getBody().get("status")).isEqualTo("RETAINED");assertThat(db.queryForObject("SELECT state FROM gpu_lease WHERE server_id=?",String.class,node(8))).isEqualTo("FREE");
        assertThat(get("/api/v1/rentals/"+id+"/connection",access(t)).getBody().get("available")).isEqualTo(false);
        ((MutableClock)clock).at=NOW.plusSeconds(3605);drain();t=tokens("retained@demo.local");assertThat(db.queryForObject("SELECT vmid FROM vm_instance WHERE rental_id=?",Long.class,id)).isEqualTo(vmid);assertThat(get("/api/v1/rentals/"+id+"/credential",access(t)).getBody().get("password")).isEqualTo(secret);assertThat(get("/api/v1/rentals/"+id,access(t)).getBody().get("status")).isEqualTo("ACTIVE");
    }
    @Test void loginCorsAllowsOnlyConfiguredFrontend(){
        var headers=new HttpHeaders();headers.setOrigin("http://localhost:3000");headers.setAccessControlRequestMethod(HttpMethod.POST);headers.setAccessControlRequestHeaders(List.of("content-type"));
        var ok=http.exchange("/formLogin",HttpMethod.OPTIONS,new HttpEntity<>(headers),String.class);assertThat(ok.getStatusCode().value()).isEqualTo(200);assertThat(ok.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        var missing=http.exchange("/mypage",HttpMethod.GET,new HttpEntity<>(headers),String.class);assertThat(missing.getStatusCode().value()).isEqualTo(401);assertThat(missing.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        headers.setOrigin("https://untrusted.invalid");assertThat(http.exchange("/formLogin",HttpMethod.OPTIONS,new HttpEntity<>(headers),String.class).getStatusCode().value()).isEqualTo(403);
    }
}
