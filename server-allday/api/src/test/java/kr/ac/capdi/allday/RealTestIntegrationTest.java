package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"allday.real-test-enabled=true","allday.real-rental-member-ids=10000,10001,10002","allday.demo-auth-enabled=true","allday.demo-seed-enabled=false","allday.demo-worker-enabled=false","allday.dev-auth-enabled=false","allday.dev-seed-enabled=false","allday.scheduler-enabled=false"})
@EnabledIfEnvironmentVariable(named="DB_URL",matches=".*[/]capdi_test")
class RealTestIntegrationTest {
    @Autowired TestRestTemplate http;@Autowired PasswordEncoder passwords;JdbcTemplate db;long real;
    @BeforeEach void reset() {
        db=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_MIGRATION_USER"),System.getenv("DB_MIGRATION_PASSWORD")));
        db.execute("TRUNCATE category,external_subject_binding,accounts,roles,auth_login_throttle RESTART IDENTITY CASCADE");
        for(String role:List.of("ROLE_USER","ROLE_KUMOH","ROLE_ADMIN"))db.update("INSERT INTO roles(name,description) VALUES (?,?)",role,role);
        db.update("INSERT INTO external_subject_binding(member_id,issuer,subject,last_eligibility) VALUES (10000,'server-allday-demo','1',true)");
        db.execute("SELECT setval('external_member_id_seq',10000,true)");
        long account=db.queryForObject("INSERT INTO accounts(login_id,name,password_hash) VALUES ('junseok@demo.local','test',?) RETURNING account_id",Long.class,passwords.encode(DemoSeed.PASSWORD));
        db.update("INSERT INTO board_users(account_id,member_id,nickname) VALUES (?,10000,'test')",account);
        db.update("INSERT INTO role_accounts SELECT ?,role_id FROM roles WHERE name IN ('ROLE_USER','ROLE_KUMOH')",account);
        long fakeCategory=db.queryForObject("INSERT INTO category(name) VALUES ('fixture-demo') RETURNING id",Long.class);
        long actualCategory=db.queryForObject("INSERT INTO category(name) VALUES ('real-computers') RETURNING id",Long.class);
        db.update("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,inventory_source) VALUES (?,'fixture','demo-fixture','fixture',64,'fixture','DEMO')",fakeCategory);
        real=db.queryForObject("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,inventory_source) VALUES (?,'대여 컴퓨터 001','001','미확인',2,'UHD Graphics 630','MANUAL') RETURNING id",Long.class,actualCategory);
    }
    ResponseEntity<List> list(String path,String token) {
        var h=new HttpHeaders();if(token!=null)h.setBearerAuth(token);
        return http.exchange(path,HttpMethod.GET,new HttpEntity<>(h),List.class);
    }
    String studentToken() {
        var h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var form=new org.springframework.util.LinkedMultiValueMap<String,String>();form.add("username","junseok@demo.local");form.add("password",DemoSeed.PASSWORD);
        var login=http.postForEntity("/formLogin",new HttpEntity<>(form,h),Map.class);assertThat(login.getStatusCode().value()).isEqualTo(200);
        return login.getBody().get("accessToken").toString();
    }
    @Test void userAndWorkerInventoryExcludePresentationFixturesAndEmptyCategories() {
        String token=studentToken();
        var servers=list("/api/v1/servers",token);assertThat(servers.getStatusCode().value()).isEqualTo(200);
        assertThat(servers.getBody()).hasSize(1);assertThat((Map)servers.getBody().getFirst()).containsEntry("nodeName","001").containsEntry("memoryGb",2).containsEntry("reservationAllowed",false);
        assertThat(list("/api/v1/categories",token).getBody()).hasSize(1);
        var inventory=list("/api/internal/inventory",System.getenv("WORKER_TOKEN"));assertThat(inventory.getStatusCode().value()).isEqualTo(200);
        assertThat(inventory.getBody()).hasSize(1);assertThat((Map)inventory.getBody().getFirst()).containsEntry("nodeName","001");
    }
    @Test void retainedAccountUsesRealTestModeAndScenarioEndpointDoesNotInventVmSuccess() {
        var h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        var form=new org.springframework.util.LinkedMultiValueMap<String,String>();form.add("username","junseok@demo.local");form.add("password",DemoSeed.PASSWORD);
        var login=http.postForEntity("/formLogin",new HttpEntity<>(form,h),Map.class);assertThat(login.getStatusCode().value()).isEqualTo(200);
        h=new HttpHeaders();h.setBearerAuth(login.getBody().get("accessToken").toString());
        var me=http.exchange("/mypage",HttpMethod.GET,new HttpEntity<>(h),Map.class);assertThat(me.getBody()).containsEntry("mode","REAL_TEST");
        var scenario=http.exchange("/api/v1/demo/scenarios",HttpMethod.GET,new HttpEntity<>(h),Map.class);
        assertThat(scenario.getBody()).containsEntry("mode","REAL_TEST").containsEntry("hardwareIntegration","NOT_VERIFIED");
        assertThat((List)scenario.getBody().get("rentals")).isEmpty();assertThat((List)scenario.getBody().get("servers")).hasSize(1);
    }
}
