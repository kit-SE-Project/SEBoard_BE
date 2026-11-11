package kr.ac.capdi.allday;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ImmediateReservationPolicyTest {
    static final Instant NOW=Instant.parse("2026-10-27T12:00:00Z");
    final Actor student=new Actor(10000,"STUDENT");
    final List<Requests.Slot> slots=List.of(new Requests.Slot(NOW.plusSeconds(3600),NOW.plusSeconds(7200)));
    @Test void onlyConfiguredAccountAndEquipmentUseNowAndRetainEndTime() {
        var mode=new ImmediateReservationPolicy(Clock.fixed(NOW,ZoneOffset.UTC),NOW.plusSeconds(1800).toString(),10000,57,false);
        assertThat(mode.slots(student,57,slots)).containsExactly(new Requests.Slot(NOW,NOW.plusSeconds(7200)));
        assertThat(mode.slots(new Actor(10001,"STUDENT"),57,slots)).isSameAs(slots);
        assertThat(mode.slots(student,58,slots)).isSameAs(slots);
    }
    @Test void deadlineAutomaticallyRestoresNormalReservationTimes() {
        var mode=new ImmediateReservationPolicy(Clock.fixed(NOW,ZoneOffset.UTC),NOW.toString(),10000,57,false);
        assertThat(mode.slots(student,57,slots)).isSameAs(slots);
        assertThat(new ImmediateReservationPolicy(Clock.fixed(NOW,ZoneOffset.UTC),"",0,0,false).slots(student,57,slots)).isSameAs(slots);
    }
    @Test void noExpiryModeRemainsEnabledUntilExplicitlyDisabledAndKeepsScope() {
        var later=NOW.plus(Duration.ofDays(30));
        var mode=new ImmediateReservationPolicy(Clock.fixed(later,ZoneOffset.UTC),"",10000,57,true);
        var future=List.of(new Requests.Slot(later.plusSeconds(3600),later.plusSeconds(7200)));
        assertThat(mode.slots(student,57,future).getFirst().startAt()).isEqualTo(later);
        assertThat(mode.slots(new Actor(10001,"STUDENT"),57,future)).isSameAs(future);
        assertThat(mode.slots(student,58,future)).isSameAs(future);
        assertThat(new ImmediateReservationPolicy(Clock.fixed(later,ZoneOffset.UTC),"",10000,57,false).slots(student,57,future)).isSameAs(future);
    }
    @Test void bothExplicitlyApprovedTestAccountsCanRentImmediately() {
        var mode=new ImmediateReservationPolicy(Clock.fixed(NOW,ZoneOffset.UTC),"",0,57,true,"10000,10001");
        for(long member:List.of(10000L,10001L))assertThat(mode.slots(new Actor(member,"STUDENT"),57,slots).getFirst().startAt()).isEqualTo(NOW);
        assertThat(mode.slots(new Actor(10002,"ADMIN"),57,slots)).isSameAs(slots);
        assertThat(mode.slots(student,58,slots)).isSameAs(slots);
    }
}
