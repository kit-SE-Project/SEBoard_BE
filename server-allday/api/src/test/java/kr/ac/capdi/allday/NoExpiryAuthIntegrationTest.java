package kr.ac.capdi.allday;

import com.nimbusds.jwt.SignedJWT;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "allday.scheduler-enabled=false","allday.dev-auth-enabled=false","allday.dev-seed-enabled=false",
    "allday.demo-auth-enabled=true","allday.demo-seed-enabled=false","allday.demo-worker-enabled=false",
    "allday.demo-runner-enabled=false","allday.real-test-enabled=true","allday.test-auth-no-expiry=true"})
@Import(NoExpiryAuthIntegrationTest.TimeConfiguration.class)
@EnabledIfEnvironmentVariable(named="DB_URL",matches=".*[/]capdi_test")
class NoExpiryAuthIntegrationTest {
    static final Instant NOW=Instant.parse("2026-10-28T00:00:00Z");
    static class MutableClock extends Clock {
        Instant at=NOW;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return at;}
    }
    @TestConfiguration static class TimeConfiguration {@Bean @Primary Clock testClock(){return new MutableClock();}}
    @Autowired TestRestTemplate http;
    @Autowired PasswordEncoder passwords;
    @Autowired Clock clock;
    JdbcTemplate db;
    @BeforeEach void reset() {
        ((MutableClock)clock).at=NOW;
        db=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_MIGRATION_USER"),System.getenv("DB_MIGRATION_PASSWORD")));
        assertThat(db.queryForObject("SELECT current_database()",String.class)).isEqualTo("capdi_test");
        db.execute("TRUNCATE category,external_subject_binding,accounts,roles,auth_login_throttle RESTART IDENTITY CASCADE");
        for(String role:List.of("ROLE_USER","ROLE_KUMOH","ROLE_ADMIN"))db.update("INSERT INTO roles(name,description) VALUES (?,?)",role,role);
        var users=List.of("junseok@demo.local","student2@demo.local","admin@demo.local");
        for(int i=0;i<users.size();i++) {
            String login=users.get(i);long member=10000L+i;
            db.update("INSERT INTO external_subject_binding(member_id,issuer,subject,last_eligibility) VALUES (?,'server-allday-demo',?,true)",member,login);
            long account=db.queryForObject("INSERT INTO accounts(login_id,name,password_hash) VALUES (?,?,?) RETURNING account_id",Long.class,login,login,passwords.encode(DemoSeed.PASSWORD));
            db.update("INSERT INTO board_users(account_id,member_id,nickname) VALUES (?,?,?)",account,member,login);
            db.update("INSERT INTO role_accounts SELECT ?,role_id FROM roles WHERE name IN ('ROLE_USER','ROLE_KUMOH') OR (name='ROLE_ADMIN' AND ?)",account,i==2);
        }
    }
    Map login(String user) {
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var form=new LinkedMultiValueMap<String,String>();form.add("username",user);form.add("password",DemoSeed.PASSWORD);
        var response=http.postForEntity("/formLogin",new HttpEntity<>(form,headers),Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);return response.getBody();
    }
    ResponseEntity<Map> request(HttpMethod method,String path,String token,Object body) {
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);
        if(token!=null)headers.setBearerAuth(token);
        return http.exchange(path,method,new HttpEntity<>(body,headers),Map.class);
    }
    String access(Map pair){return pair.get("accessToken").toString();}
    String refresh(Map pair){return pair.get("refreshToken").toString();}
    void advanceYears(){((MutableClock)clock).at=NOW.plus(Duration.ofDays(36525));}
    @Test void adminAndBothStudentSessionsRemainUsableWithoutExpiry() throws Exception {
        var pairs=new ArrayList<Map>();
        for(String login:List.of("junseok@demo.local","student2@demo.local","admin@demo.local")) {
            var pair=login(login);pairs.add(pair);
            for(String token:List.of(access(pair),refresh(pair))) {
                var claims=SignedJWT.parse(token).getJWTClaimsSet();
                assertThat(claims.getExpirationTime()).isNull();assertThat(claims.getClaim("test_no_expiry")).isEqualTo(true);
            }
        }
        assertThat(db.queryForObject("SELECT count(*) FROM auth_session WHERE no_expiry AND expires_at IS NULL",Long.class)).isEqualTo(3);
        advanceYears();
        for(var pair:pairs)assertThat(request(HttpMethod.GET,"/mypage",access(pair),null).getStatusCode().value()).isEqualTo(200);
        var headers=new HttpHeaders();headers.setBearerAuth(access(pairs.get(2)));
        assertThat(http.exchange("/api/v1/admin/servers",HttpMethod.GET,new HttpEntity<>(headers),List.class).getStatusCode().value()).isEqualTo(200);
    }
    @Test void refreshWorksWithoutExpiryAndStillRotatesTheRefreshToken() throws Exception {
        var pair=login("admin@demo.local");advanceYears();
        var refreshed=request(HttpMethod.POST,"/refresh",null,Map.of("refreshToken",refresh(pair)));
        assertThat(refreshed.getStatusCode().value()).isEqualTo(200);
        assertThat(SignedJWT.parse(access(refreshed.getBody())).getJWTClaimsSet().getExpirationTime()).isNull();
        assertThat(request(HttpMethod.POST,"/refresh",null,Map.of("refreshToken",refresh(pair))).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.GET,"/mypage",access(refreshed.getBody()),null).getStatusCode().value()).isEqualTo(200);
    }
    @Test void explicitLogoutStillRevokesNonExpiringAccessAndRefreshTokens() {
        var pair=login("admin@demo.local");advanceYears();
        assertThat(request(HttpMethod.POST,"/logoutProc",access(pair),Map.of("refreshToken",refresh(pair))).getStatusCode().value()).isEqualTo(200);
        assertThat(request(HttpMethod.GET,"/mypage",access(pair),null).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.POST,"/refresh",null,Map.of("refreshToken",refresh(pair))).getStatusCode().value()).isEqualTo(401);
    }
    @Test void databaseRoleRemovalAppliesImmediatelyToPermanentAdminTokens() {
        var pair=login("admin@demo.local");advanceYears();
        db.update("DELETE FROM role_accounts WHERE account_id=(SELECT account_id FROM accounts WHERE login_id='admin@demo.local') AND role_id=(SELECT role_id FROM roles WHERE name='ROLE_ADMIN')");
        var headers=new HttpHeaders();headers.setBearerAuth(access(pair));
        assertThat(http.exchange("/api/v1/admin/servers",HttpMethod.GET,new HttpEntity<>(headers),Map.class).getStatusCode().value()).isEqualTo(403);
    }
    @Test void disabledAccountAndMissingSessionStillInvalidatePermanentTokens() {
        var pair=login("junseok@demo.local");
        db.update("UPDATE accounts SET status='TEMP_DELETED' WHERE login_id='junseok@demo.local'");
        assertThat(request(HttpMethod.GET,"/mypage",access(pair),null).getStatusCode().value()).isEqualTo(401);
        var admin=login("admin@demo.local");db.update("DELETE FROM auth_session WHERE account_id=(SELECT account_id FROM accounts WHERE login_id='admin@demo.local')");
        assertThat(request(HttpMethod.GET,"/mypage",access(admin),null).getStatusCode().value()).isEqualTo(401);
    }
    @Test void malformedTokensAndAccessRefreshTypeConfusionRemainRejected() {
        var pair=login("admin@demo.local");String[] pieces=access(pair).split("\\.");
        pieces[1]=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"test_no_expiry\":true,\"accountId\":3}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(request(HttpMethod.GET,"/mypage",String.join(".",pieces),null).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.GET,"/mypage",refresh(pair),null).getStatusCode().value()).isEqualTo(401);
        assertThat(request(HttpMethod.POST,"/refresh",null,Map.of("refreshToken",access(pair))).getStatusCode().value()).isEqualTo(401);
    }
}
