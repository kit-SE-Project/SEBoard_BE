package kr.ac.capdi.allday;

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
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "allday.scheduler-enabled=false","allday.dev-auth-enabled=true","allday.dev-seed-enabled=true",
    "allday.demo-auth-enabled=false","allday.demo-seed-enabled=false","allday.minimum-switch-seconds=0","allday.prepare-lead-seconds=180"})
@Import(ZeroSwitchIntegrationTest.TimeConfiguration.class)
@EnabledIfEnvironmentVariable(named="DB_URL",matches=".*[/]capdi_test")
class ZeroSwitchIntegrationTest {
    static final Instant NOW=Instant.parse("2026-10-06T03:00:00Z");
    @TestConfiguration static class TimeConfiguration {@Bean @Primary Clock testClock(){return Clock.fixed(NOW,ZoneOffset.UTC);}}
    @Autowired TestRestTemplate http;
    @Autowired DevSeed seed;
    JdbcTemplate owner;long server;
    @BeforeEach void reset(){
        owner=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_MIGRATION_USER"),System.getenv("DB_MIGRATION_PASSWORD")));
        assertThat(owner.queryForObject("SELECT current_database()",String.class)).isEqualTo("capdi_test");
        owner.execute("TRUNCATE category,external_subject_binding RESTART IDENTITY CASCADE");seed.run(null);
        server=owner.queryForObject("SELECT id FROM server_node WHERE node_name='demo-node-1'",Long.class);
    }
    ResponseEntity<Map> reserve(String tokenName,Instant start,Instant end){
        var headers=new HttpHeaders();headers.setBearerAuth(System.getenv(tokenName));headers.setContentType(MediaType.APPLICATION_JSON);headers.set("Idempotency-Key",UUID.randomUUID().toString());
        return http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(Map.of("serverId",server,"purpose","전환 0초 시험","slots",List.of(Map.of("startAt",start,"endAt",end))),headers),Map.class);
    }
    @Test void adjacentBookingsAllowedButOverlapStillRejected(){
        assertThat(reserve("DEV_STUDENT_ONE_TOKEN",NOW.plusSeconds(600),NOW.plusSeconds(1200)).getStatusCode().value()).isEqualTo(201);
        assertThat(reserve("DEV_STUDENT_TWO_TOKEN",NOW.plusSeconds(1200),NOW.plusSeconds(1800)).getStatusCode().value()).isEqualTo(201);
        var overlap=reserve("DEV_ADMIN_TOKEN",NOW.plusSeconds(1100),NOW.plusSeconds(1300));
        assertThat(overlap.getStatusCode().value()).isEqualTo(409);assertThat(overlap.getBody().get("code")).isEqualTo("SLOT_CONFLICT");
    }
    @Test void preparationStillRunsAheadWithZeroSwitchGap(){
        var result=reserve("DEV_STUDENT_ONE_TOKEN",NOW.plusSeconds(600),NOW.plusSeconds(1200));assertThat(result.getStatusCode().value()).isEqualTo(201);
        long id=((Number)result.getBody().get("id")).longValue();
        assertThat(owner.queryForObject("SELECT next_run_at FROM job WHERE rental_id=? AND type='PREPARE'",java.sql.Timestamp.class,id).toInstant()).isEqualTo(NOW.plusSeconds(420));
    }
}
