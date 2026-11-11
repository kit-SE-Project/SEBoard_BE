package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ReservationCompiler {
    public static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private final Clock clock;
    public ReservationCompiler(Clock clock) {this.clock=clock;}
    public List<Requests.Slot> compile(Requests.Reservation request) {
        if((request.slots()==null)==(request.rule()==null)) throw ApiProblem.invalid("slots와 rule 중 하나만 입력하세요.");
        List<Requests.Slot> raw=new ArrayList<>();
        Instant horizon=clock.instant().atZone(ZONE).plusMonths(2).toInstant();
        if(request.rule()!=null) {
            var rule=request.rule();
            if(rule.from()==null||rule.to()==null||rule.weekdays()==null||rule.bands()==null||rule.weekdays().isEmpty()||rule.bands().isEmpty())
                throw ApiProblem.invalid("반복 예약 날짜·요일·시간대를 입력하세요.");
            if(rule.to().isBefore(rule.from()) || rule.to().isAfter(horizon.atZone(ZONE).toLocalDate()) ||
                rule.from().isBefore(clock.instant().atZone(ZONE).toLocalDate())) throw ApiProblem.invalid("반복 예약 기간이 올바르지 않습니다.");
            for(LocalDate day=rule.from();!day.isAfter(rule.to());day=day.plusDays(1)) {
                if(!rule.weekdays().contains(day.getDayOfWeek())) continue;
                for(var band:rule.bands()) {
                    int start=band==Requests.Band.AFTERNOON?16:8;
                    int end=band==Requests.Band.MORNING?15:23;
                    raw.add(new Requests.Slot(day.atTime(start,0).atZone(ZONE).toInstant(),day.atTime(end,0).atZone(ZONE).toInstant()));
                }
            }
        } else raw.addAll(request.slots());
        if(raw.isEmpty()||raw.size()>256) throw ApiProblem.invalid("예약 슬롯은 1–256개여야 합니다.");
        for(var slot:raw) {
            if(slot==null||slot.startAt()==null||slot.endAt()==null||!slot.startAt().isBefore(slot.endAt())) throw ApiProblem.invalid("시작 시각은 종료보다 앞서야 합니다.");
            if(slot.startAt().isBefore(clock.instant())||slot.endAt().isAfter(horizon)) throw ApiProblem.invalid("과거 또는 현재부터 달력 2개월을 넘는 예약은 허용하지 않습니다.");
        }
        raw.sort(Comparator.comparing(Requests.Slot::startAt));
        List<Requests.Slot> merged=new ArrayList<>();
        for(var slot:raw) {
            if(merged.isEmpty()) {merged.add(slot);continue;}
            var previous=merged.getLast();
            var end=previous.endAt().atZone(ZONE);var start=slot.startAt().atZone(ZONE);
            boolean sameContractMaintenance=end.toLocalDate().equals(start.toLocalDate()) &&
                end.toLocalTime().equals(LocalTime.of(15,0))&&start.toLocalTime().equals(LocalTime.of(16,0));
            if(!slot.startAt().isAfter(previous.endAt())||sameContractMaintenance)
                merged.set(merged.size()-1,new Requests.Slot(previous.startAt(),previous.endAt().isAfter(slot.endAt())?previous.endAt():slot.endAt()));
            else merged.add(slot);
        }
        return List.copyOf(merged);
    }
}
