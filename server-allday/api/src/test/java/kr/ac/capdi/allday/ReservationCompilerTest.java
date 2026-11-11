package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReservationCompilerTest {
    final Clock clock=Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"),ZoneOffset.UTC);
    final ReservationCompiler compiler=new ReservationCompiler(clock);
    Requests.Slot slot(String start,String end){return new Requests.Slot(Instant.parse(start),Instant.parse(end));}
    Requests.Reservation raw(List<Requests.Slot> slots){return new Requests.Reservation(1,"test",slots,null);}
    @Test void mergesMorningAfternoonOfSameContractWithoutMaintenanceStop() {
        var rule=new Requests.Rule(LocalDate.parse("2026-10-07"),LocalDate.parse("2026-10-07"),Set.of(DayOfWeek.WEDNESDAY),Set.of(Requests.Band.MORNING,Requests.Band.AFTERNOON));
        var slots=compiler.compile(new Requests.Reservation(1,"test",null,rule));
        assertThat(slots).containsExactly(slot("2026-10-06T23:00:00Z","2026-10-07T14:00:00Z"));
    }
    @Test void preservesNonContinuousSlotsAcrossDays() {
        var slots=compiler.compile(raw(List.of(slot("2026-10-07T00:00:00Z","2026-10-07T06:00:00Z"),slot("2026-10-08T00:00:00Z","2026-10-08T06:00:00Z"))));
        assertThat(slots).hasSize(2);
    }
    @Test void rejectsPastEmptyAndReversedSlots() {
        assertThatThrownBy(()->compiler.compile(raw(List.of()))).isInstanceOf(ApiProblem.class);
        assertThatThrownBy(()->compiler.compile(raw(List.of(slot("2026-10-05T00:00:00Z","2026-10-05T06:00:00Z"))))).isInstanceOf(ApiProblem.class);
        assertThatThrownBy(()->compiler.compile(raw(List.of(slot("2026-10-07T06:00:00Z","2026-10-07T00:00:00Z"))))).isInstanceOf(ApiProblem.class);
    }
    @Test void usesCalendarMonthsRatherThanSixtyDays() {
        var c=new ReservationCompiler(Clock.fixed(Instant.parse("2026-01-31T00:00:00Z"),ZoneOffset.UTC));
        assertThat(c.compile(raw(List.of(slot("2026-03-30T00:00:00Z","2026-03-30T01:00:00Z"))))).hasSize(1);
        assertThatThrownBy(()->c.compile(raw(List.of(slot("2026-03-31T00:00:00Z","2026-03-31T01:00:00Z"))))).isInstanceOf(ApiProblem.class);
    }
    @Test void requiresExactlyOneInputRepresentation() {
        assertThatThrownBy(()->compiler.compile(new Requests.Reservation(1,"test",null,null))).isInstanceOf(ApiProblem.class);
    }
    @Test void monthEndHorizonAcceptsExactInstantAndRejectsOneNanosecondLater() {
        var at=ZonedDateTime.of(2026,12,31,10,12,13,0,ReservationCompiler.ZONE).toInstant();var c=new ReservationCompiler(Clock.fixed(at,ZoneOffset.UTC));var horizon=at.atZone(ReservationCompiler.ZONE).plusMonths(2).toInstant();
        assertThat(horizon.atZone(ReservationCompiler.ZONE).toLocalDate()).isEqualTo(LocalDate.of(2027,2,28));
        assertThat(c.compile(raw(List.of(new Requests.Slot(at,horizon))))).hasSize(1);
        assertThatThrownBy(()->c.compile(raw(List.of(new Requests.Slot(at,horizon.plusNanos(1)))))).isInstanceOf(ApiProblem.class);
    }
    @Test void continuousMultiDaySlotDoesNotSplitAtNight() {
        var start=OffsetDateTime.parse("2026-10-20T00:00:00+09:00").toInstant();var end=OffsetDateTime.parse("2026-10-23T00:00:00+09:00").toInstant();
        assertThat(compiler.compile(raw(List.of(new Requests.Slot(start,end))))).containsExactly(new Requests.Slot(start,end));
    }

}
