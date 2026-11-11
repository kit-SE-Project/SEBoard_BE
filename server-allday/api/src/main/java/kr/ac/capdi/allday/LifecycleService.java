package kr.ac.capdi.allday;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates durable intents only. External commands belong to the worker/Agent. */
@Service
public class LifecycleService {
    private final JdbcTemplate db; private final RentalService rentals; private final AuditService audit; private final Clock clock;
    public LifecycleService(JdbcTemplate db,RentalService rentals,AuditService audit,Clock clock) {this.db=db;this.rentals=rentals;this.audit=audit;this.clock=clock;}
    @Transactional public Object tick() {
        int created=0,expired=0;
        // Stable server order matches API/worker locks across multiple API instances.
        var candidates=db.queryForList("SELECT id,server_id FROM rental WHERE status NOT IN ('RETURNED','CANCELLED','RETURNING') ORDER BY server_id,id");
        for(var candidate:candidates) {
            rentals.server(Rows.number(candidate,"server_id"),true);
            var r=Rows.one(db,"SELECT * FROM rental WHERE id=? FOR UPDATE",candidate.get("id"));
            if(Set.of("RETURNED","CANCELLED","RETURNING").contains(r.get("status")))continue;
            long id=Rows.number(r,"id"),revision=Rows.number(r,"desired_revision");
            if(!clock.instant().isBefore(Rows.instant(r,"contract_end"))) {
                boolean empty="RESERVED".equals(r.get("status"))&&rentals.noResources(id);
                String after=empty?"CANCELLED":"RETURNING";long next=revision+1;
                db.update("UPDATE rental SET status=?,retained_reason=NULL,desired_revision=?,version=version+1,reason_code='CONTRACT_EXPIRED' WHERE id=?",after,next,id);
                db.update("UPDATE job SET status='SUPERSEDED',step='INVALIDATED' WHERE rental_id=? AND status='PENDING'",id);
                Long job=null;
                if(empty) {db.update("UPDATE rental_slot SET state='CANCELLED' WHERE rental_id=?",id);db.update("UPDATE vm_credential SET status='RETIRED' WHERE rental_id=?",id);}
                else job=rentals.createJob(id,Rows.number(r,"server_id"),next,"RETURN",clock.instant());
                audit.event(id,job,null,UUID.randomUUID().toString(),r.get("status").toString(),after,"CONTRACT_EXPIRED");
                rentals.notify(Rows.number(r,"member_id"),"rental:"+id+":expired:r"+next,Map.of("rentalId",id,"status",after));expired++;continue;
            }
            db.update("UPDATE job j SET status='SUPERSEDED',step='MISSED_SLOT' FROM rental_slot s WHERE j.slot_id=s.id AND j.type='START' AND j.status='PENDING' AND j.rental_id=? AND s.end_at<=?",id,Rows.ts(clock.instant()));
            var slots=db.queryForList("SELECT * FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' ORDER BY start_at",id);
            boolean activeRevision="ACTIVE".equals(r.get("status"))&&db.queryForObject("SELECT EXISTS(SELECT 1 FROM job WHERE rental_id=? AND desired_revision=? AND type IN ('PREPARE','START') AND status='SUCCEEDED')",Boolean.class,id,revision);
            for(int i=0;i<slots.size();i++) {
                var slot=slots.get(i);long slotId=Rows.number(slot,"id");
                boolean alreadyActive=activeRevision&&"ACTIVE".equals(slot.get("state"))&&!Rows.instant(slot,"start_at").isAfter(clock.instant())&&Rows.instant(slot,"end_at").isAfter(clock.instant());
                if(Rows.instant(slot,"end_at").isAfter(clock.instant()))created+=intent(r,slotId,"START",Rows.instant(slot,"start_at"),10,alreadyActive);
                if(i<slots.size()-1)created+=intent(r,slotId,"STOP",Rows.instant(slot,"end_at"),80,false);
            }
        }
        return Map.of("createdJobs",created,"expiredRentals",expired,"checkedAt",clock.instant());
    }
    int intent(Map<String,Object> rental,long slot,String type,java.time.Instant at,int priority,boolean satisfied) {
        return db.update("INSERT INTO job(server_id,rental_id,slot_id,intent_key,type,desired_revision,priority,next_run_at,status,step,reason_code) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (intent_key) DO NOTHING",
            rental.get("server_id"),rental.get("id"),slot,"rental:"+rental.get("id")+":r"+rental.get("desired_revision")+":slot:"+slot+":"+type.toLowerCase(Locale.ROOT),type,rental.get("desired_revision"),priority,Rows.ts(at),satisfied?"SUPERSEDED":"PENDING",satisfied?"SLOT_ALREADY_ACTIVE":"QUEUED",satisfied?"START_ALREADY_SATISFIED":null);
    }
}
