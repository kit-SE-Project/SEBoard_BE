package kr.ac.capdi.allday;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component @EnableScheduling @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("${allday.demo-worker-enabled:false} && ${allday.demo-runner-enabled:true}")
public class DemoRunner {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(DemoRunner.class);
    private final JdbcTemplate db;private final DemoWorker worker;
    public DemoRunner(JdbcTemplate db,DemoWorker worker){this.db=db;this.worker=worker;}
    @Scheduled(fixedDelay=3000,initialDelay=8000) public void tick(){
        var jobs=db.queryForList("SELECT j.id,j.status FROM job j JOIN rental r ON r.id=j.rental_id JOIN server_node s ON s.id=j.server_id WHERE s.inventory_source='DEMO' AND ((j.status='PENDING' AND j.desired_revision=r.desired_revision AND j.next_run_at<=now()) OR (j.status='RUNNING' AND j.worker_id=?)) ORDER BY j.priority DESC,j.id LIMIT 50",DemoWorker.WORKER);
        for(var job:jobs)try{if("PENDING".equals(job.get("status")))worker.claim(Rows.number(job,"id"));else worker.complete(Rows.number(job,"id"));}catch(ApiProblem e){log.debug("demo job {} deferred {}",job.get("id"),e.code);}
        for(var row:db.queryForList("SELECT r.id FROM rental r JOIN server_node s ON s.id=r.server_id JOIN vm_instance v ON v.rental_id=r.id WHERE s.inventory_source='DEMO' AND r.status IN ('READY','ACTIVE','RETAINED')"))try{worker.heartbeat(Rows.number(row,"id"));}catch(ApiProblem e){log.debug("demo heartbeat {} deferred {}",row.get("id"),e.code);}
    }
}
