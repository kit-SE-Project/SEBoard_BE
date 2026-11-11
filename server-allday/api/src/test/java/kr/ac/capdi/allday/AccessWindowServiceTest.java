package kr.ac.capdi.allday;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccessWindowServiceTest {
    final Instant now=Instant.parse("2026-10-23T03:00:00Z");
    @Test void validWindowNeverExceedsSixtySecondsOrSlotEnd() {
        assertThat(AccessWindowService.calculate(now,now.plusSeconds(1000))).containsEntry("validUntil",now.plusSeconds(60)).containsEntry("maxLeaseSeconds",60L);
        assertThat(AccessWindowService.calculate(now,now.plusSeconds(17))).containsEntry("validUntil",now.plusSeconds(17)).containsEntry("maxLeaseSeconds",17L);
    }
    @Test void missingOrNearlyExpiredSlotCannotProduceHelperLease() {
        for(Instant end:new Instant[]{null,now,now.plusMillis(4999)})
            assertThat(AccessWindowService.calculate(now,end)).containsEntry("maxLeaseSeconds",0L);
        assertThat(AccessWindowService.calculate(now,now.plusSeconds(5))).containsEntry("maxLeaseSeconds",5L);
    }
}
