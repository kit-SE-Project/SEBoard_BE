package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Scoped recording mode changes reservation times, never equipment observations. */
@Component
public class ImmediateReservationPolicy {
    private final Clock clock;private final Instant until;private final Set<Long> memberIds;private final long serverId;private final boolean noExpiry;
    @Autowired
    public ImmediateReservationPolicy(Clock clock,@Value("${allday.immediate-reservation-until:}") String until,
        @Value("${allday.immediate-reservation-member-id:0}") long memberId,@Value("${allday.immediate-reservation-server-id:0}") long serverId,
        @Value("${allday.immediate-reservation-no-expiry:false}") boolean noExpiry,
        @Value("${allday.immediate-reservation-member-ids:}") String configuredIds) {
        this.clock=clock;this.until=until.isBlank()?Instant.MIN:Instant.parse(until);this.serverId=serverId;this.noExpiry=noExpiry;
        var ids=new HashSet<Long>();if(memberId>0)ids.add(memberId);
        if(!configuredIds.isBlank())for(String value:configuredIds.split(",",-1)) {
            try{long id=Long.parseLong(value.trim());if(id<=0)throw new NumberFormatException();ids.add(id);}
            catch(NumberFormatException e){throw new IllegalArgumentException("즉시 대여 회원 ID는 양의 정수 목록이어야 합니다.");}
        }
        memberIds=Set.copyOf(ids);
        if((noExpiry||!until.isBlank())&&(memberIds.isEmpty()||serverId<=0))throw new IllegalArgumentException("즉시 예약의 회원·장비 설정을 확인하세요.");
    }
    public ImmediateReservationPolicy(Clock clock,String until,long memberId,long serverId,boolean noExpiry) {this(clock,until,memberId,serverId,noExpiry,"");}
    boolean enabled(Actor actor,long server) {return memberIds.contains(actor.memberId())&&server==serverId&&(noExpiry||clock.instant().isBefore(until));}
    List<Requests.Slot> slots(Actor actor,long server,List<Requests.Slot> validated) {
        if(!enabled(actor,server))return validated;
        var now=clock.instant();var first=validated.getFirst();
        if(!now.isBefore(first.endAt()))throw ApiProblem.invalid("즉시 예약의 종료 시각은 현재보다 늦어야 합니다.");
        var result=new ArrayList<>(validated);result.set(0,new Requests.Slot(now,first.endAt()));return List.copyOf(result);
    }
}
