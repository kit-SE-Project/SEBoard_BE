package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final JdbcTemplate db;private final JsonCodec json;private final Clock clock;
    public NotificationController(JdbcTemplate db,JsonCodec json,Clock clock){this.db=db;this.json=json;this.clock=clock;}
    @GetMapping public Object list(HttpServletRequest r){return db.queryForList("SELECT id,event_key,payload,delivered_at,read_at FROM notification_outbox WHERE member_id=? ORDER BY id DESC LIMIT 100",Actor.from(r).memberId()).stream().map(row->{var api=Rows.api(row);api.put("payload",json.read(row.get("payload").toString()));return api;}).toList();}
    @PostMapping("/{id}/read") public Object read(@PathVariable long id,HttpServletRequest r) {
        if(db.update("UPDATE notification_outbox SET read_at=coalesce(read_at,?) WHERE id=? AND member_id=?",Rows.ts(clock.instant()),id,Actor.from(r).memberId())==0)throw ApiProblem.notFound();
        return Map.of("id",id,"read",true);
    }
}
