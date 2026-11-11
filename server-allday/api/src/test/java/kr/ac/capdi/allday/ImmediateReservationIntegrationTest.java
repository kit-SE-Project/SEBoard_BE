package kr.ac.capdi.allday;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"allday.real-test-enabled=true","allday.real-rental-member-ids=10000","allday.demo-auth-enabled=true","allday.demo-seed-enabled=false","allday.demo-worker-enabled=false","allday.dev-auth-enabled=false","allday.dev-seed-enabled=false","allday.scheduler-enabled=false","allday.immediate-reservation-no-expiry=true","allday.immediate-reservation-member-id=10000","allday.immediate-reservation-server-id=2"})
class ImmediateReservationIntegrationTest extends RealTestIntegrationTest {
    Map<String,Object> body(int days) {
        var start=Instant.now().plus(Duration.ofDays(days));
        return Map.of("serverId",real,"purpose","촬영용 즉시 예약 시험","slots",List.of(Map.of("startAt",start.toString(),"endAt",start.plusSeconds(3600).toString())));
    }
    HttpHeaders headers(String token,String key) {
        var h=new HttpHeaders();h.setBearerAuth(token);h.setContentType(MediaType.APPLICATION_JSON);if(key!=null)h.set("Idempotency-Key",key);return h;
    }
    void allow() {db.update("UPDATE server_node SET status='AVAILABLE',observed_status='AVAILABLE',observed_at=now() WHERE id=?",real);}
    @Test void previewAndCreateUseImmediateTimeAndOnlyOnePreparationJob() {
        allow();var token=studentToken();var body=body(1);Instant before=Instant.now();
        var preview=http.exchange("/api/v1/rental-previews",HttpMethod.POST,new HttpEntity<>(body,headers(token,null)),Map.class);
        assertThat(preview.getBody().get("canReserve")).isEqualTo(true);
        var create=http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(body,headers(token,"immediate-create")),Map.class);
        assertThat(create.getStatusCode().value()).isEqualTo(201);long id=((Number)create.getBody().get("id")).longValue();
        var replay=http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(body,headers(token,"immediate-create")),Map.class);
        assertThat(replay.getBody()).isEqualTo(create.getBody());
        var start=db.queryForObject("SELECT contract_start FROM rental WHERE id=?",java.sql.Timestamp.class,id).toInstant();
        assertThat(start).isBetween(before,Instant.now());
        assertThat(db.queryForObject("SELECT count(*) FROM job WHERE rental_id=? AND type='PREPARE' AND next_run_at<=now()",Integer.class,id)).isEqualTo(1);
        assertThat(db.queryForObject("SELECT count(*) FROM vm_instance",Integer.class)).isZero();
    }
    @Test void shiftedReservationStillEnforcesMemberLimit() {
        allow();var token=studentToken();
        var first=http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(body(1),headers(token,"first")),Map.class);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        var second=http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(body(3),headers(token,"second")),Map.class);
        assertThat(second.getStatusCode().value()).isEqualTo(409);assertThat(second.getBody().get("code")).isEqualTo("MEMBER_RENTAL_LIMIT");
        assertThat(db.queryForObject("SELECT count(*) FROM job WHERE type='PREPARE'",Integer.class)).isEqualTo(1);
    }
}
