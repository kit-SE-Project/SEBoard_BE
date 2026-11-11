package kr.ac.capdi.allday;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Explicit test-user access to physical equipment; DEMO permissions stay unchanged. */
@Service
public class RealRentalPolicy {
    private final Set<Long> memberIds;
    private final boolean seboard;

    public RealRentalPolicy(String configured) {
        this(configured,false);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RealRentalPolicy(@Value("${allday.real-rental-member-ids:}") String configured,SeBoardAuthentication authentication) {
        this(configured,authentication.enabled());
    }

    private RealRentalPolicy(String configured,boolean seboard) {
        this.seboard=seboard;
        var ids=new HashSet<Long>();
        if(!configured.isBlank()) for(String item:configured.split(",",-1)) {
            try {
                long id=Long.parseLong(item.trim());
                if(id<=0)throw new NumberFormatException();
                ids.add(id);
            } catch(NumberFormatException e) {throw new IllegalArgumentException("REAL_RENTAL_MEMBER_IDS에는 양의 memberId를 쉼표로 구분해 넣으세요.");}
        }
        memberIds=Set.copyOf(ids);
    }
    public void require(Actor actor,Map<String,Object> server) {
        actor.requireRentalEligibility();
        if(!"DEMO".equals(server.get("inventory_source"))&&!(seboard&&memberIds.isEmpty())&&!memberIds.contains(actor.memberId()))
            throw new ApiProblem(403,"REAL_RENTAL_NOT_APPROVED","실장비 시험에 승인된 회원만 해당 서버를 예약할 수 있습니다.");
    }
}
