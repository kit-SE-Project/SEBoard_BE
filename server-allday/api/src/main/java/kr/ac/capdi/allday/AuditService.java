package kr.ac.capdi.allday;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate db;
    public AuditService(JdbcTemplate db) {this.db=db;}
    public void event(long rentalId,Long jobId,Long actorId,String requestId,String before,String after,String reason) {
        event(rentalId,jobId,actorId,requestId,before,after,reason,null);
    }
    public void event(long rentalId,Long jobId,Long actorId,String requestId,String before,String after,String reason,String note) {
        db.update("INSERT INTO rental_event(rental_id,job_id,actor_id,source,request_id,before_status,after_status,reason_code,reason_text) VALUES (?,?,?,?,?,?,?,?,?)",
            rentalId,jobId,actorId,actorId==null?"WORKER":"API",requestId,before,after,reason,note);
    }
}
