package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Absolute deadline for helper leases; never a substitute for job/owner/credential checks. */
@Service
public class AccessWindowService {
    private final JdbcTemplate db;private final Clock clock;
    public AccessWindowService(JdbcTemplate db,Clock clock){this.db=db;this.clock=clock;}
    public Map<String,Object> forRental(long rentalId) {
        Instant checked=clock.instant();
        var row=Rows.one(db,"SELECT min(LEAST(sl.end_at,r.contract_end)) AS slot_end_at FROM rental r JOIN rental_slot sl ON sl.rental_id=r.id WHERE r.id=? AND sl.state<>'CANCELLED' AND sl.start_at<=? AND sl.end_at>? AND r.contract_end>?",rentalId,Rows.ts(checked),Rows.ts(checked),Rows.ts(checked));
        return calculate(checked,Rows.instant(row,"slot_end_at"));
    }
    static Map<String,Object> calculate(Instant checked,Instant slotEnd) {
        Instant until=slotEnd==null?checked:(slotEnd.isBefore(checked.plusSeconds(60))?slotEnd:checked.plusSeconds(60));
        long seconds=Math.max(0,Duration.between(checked,until).getSeconds());
        var out=new LinkedHashMap<String,Object>();
        out.put("checkedAt",checked);out.put("slotEndAt",slotEnd);out.put("validUntil",until);
        out.put("maxLeaseSeconds",seconds>=5?seconds:0);return out;
    }
}
