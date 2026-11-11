package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"allday.real-test-enabled=true","allday.real-rental-member-ids=10000,10001,10002","allday.reservation-open-server-ids=2","allday.demo-auth-enabled=true","allday.demo-seed-enabled=false","allday.demo-worker-enabled=false","allday.dev-auth-enabled=false","allday.dev-seed-enabled=false","allday.scheduler-enabled=false"})
class ReservationOpeningIntegrationTest extends RealTestIntegrationTest {
    @Override @Test void userAndWorkerInventoryExcludePresentationFixturesAndEmptyCategories() {
        var servers=list("/api/v1/servers",studentToken());assertThat(servers.getStatusCode().value()).isEqualTo(200);
        assertThat(servers.getBody()).hasSize(1);assertThat((Map)servers.getBody().getFirst()).containsEntry("reservationAllowed",true).containsEntry("status","UNKNOWN").containsEntry("observedStatus","UNKNOWN").containsEntry("observedAt",null);
    }
    @Test void reservationCanBeAcceptedWithoutInventingEquipmentReadinessOrAllocatingVm() {
        var h=new HttpHeaders();h.setBearerAuth(studentToken());h.setContentType(MediaType.APPLICATION_JSON);h.set("Idempotency-Key",UUID.randomUUID().toString());
        Instant start=Instant.now().plusSeconds(600);
        var body=Map.of("serverId",real,"purpose","예약 접수 검사","slots",List.of(Map.of("startAt",start.toString(),"endAt",start.plusSeconds(3600).toString())));
        var created=http.exchange("/api/v1/rentals",HttpMethod.POST,new HttpEntity<>(body,h),Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);assertThat(created.getBody()).containsEntry("status","RESERVED");
        long job=((Number)created.getBody().get("jobId")).longValue();
        h=new HttpHeaders();h.setBearerAuth(System.getenv("WORKER_TOKEN"));h.setContentType(MediaType.APPLICATION_JSON);
        var claimed=http.exchange("/api/internal/jobs/"+job+"/claim",HttpMethod.POST,new HttpEntity<>(Map.of("workerId","reservation-open-test"),h),Map.class);
        assertThat(claimed.getStatusCode().value()).isEqualTo(200);
        var allocation=http.exchange("/api/internal/jobs/"+job+"/resources",HttpMethod.POST,new HttpEntity<>(Map.of("workerId","reservation-open-test","commandId",claimed.getBody().get("commandId")),h),Map.class);
        assertThat(allocation.getStatusCode().value()).isEqualTo(503);assertThat(allocation.getBody()).containsEntry("code","SERVER_UNAVAILABLE");
        assertThat(db.queryForObject("SELECT count(*) FROM vmid_allocation WHERE kind='RENTAL'",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT count(*) FROM vm_instance",Integer.class)).isZero();
    }
}
