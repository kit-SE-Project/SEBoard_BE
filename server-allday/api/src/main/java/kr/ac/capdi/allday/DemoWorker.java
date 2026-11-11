package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Simulated equipment, using the same fencing and result handlers as an actual worker. */
@Service @ConditionalOnProperty(name="allday.demo-worker-enabled",havingValue="true")
public class DemoWorker {
    static final String WORKER="presentation-simulator";
    private final JdbcTemplate db;private final WorkerService workers;private final ResourceService resources;private final RentalService rentals;private final HealthService health;private final Clock clock;private final AuditService audit;
    public DemoWorker(JdbcTemplate db,WorkerService workers,ResourceService resources,RentalService rentals,HealthService health,Clock clock,AuditService audit){this.db=db;this.workers=workers;this.resources=resources;this.rentals=rentals;this.health=health;this.clock=clock;this.audit=audit;DemoSeed.guard(db);}
    void guard(long job){if(!db.queryForObject("SELECT EXISTS(SELECT 1 FROM job j JOIN server_node s ON s.id=j.server_id WHERE j.id=? AND s.inventory_source='DEMO')",Boolean.class,job))throw new ApiProblem(403,"NOT_DEMO_INVENTORY","데모 worker는 실제 인벤토리를 처리할 수 없습니다.");}
    @Transactional public void claim(long id){guard(id);workers.claim(id,WORKER);}
    @SuppressWarnings("unchecked") @Transactional public void complete(long id){
        guard(id);var c=workers.locked(id);
        // Simulation has no external side effect: complete() is one DB transaction.
        // An obsolete claim can therefore terminate safely without releasing its resources.
        if("RUNNING".equals(c.job().get("status"))&&WORKER.equals(c.job().get("worker_id"))&&Rows.number(c.job(),"desired_revision")!=Rows.number(c.rental(),"desired_revision")){
            db.update("UPDATE job SET status='SUPERSEDED',step='SIMULATION_SUPERSEDED',worker_id=NULL,locked_until=NULL,operation_id=?,reason_code='SIMULATION_SUPERSEDED' WHERE id=?","SIMULATED:obsolete:"+id,id);
            db.update("UPDATE vm_instance SET access_enabled=false WHERE rental_id=?",c.rental().get("id"));
            audit.event(Rows.number(c.rental(),"id"),id,null,"demo-worker",c.rental().get("status").toString(),c.rental().get("status").toString(),"SIMULATION_SUPERSEDED");return;
        }
        var claim=(Map<String,Object>)workers.claim(id,WORKER);String command=claim.get("commandId").toString();long rental=((Number)claim.get("rentalId")).longValue();long revision=((Number)claim.get("desiredRevision")).longValue();String type=claim.get("type").toString();String op="SIMULATED:"+command;
        if(type.equals("RETURN")){
            var view=resources.resources(rental);var manifest=(Map<String,Object>)view.get("manifest");var items=new ArrayList<Requests.VerificationItem>();
            items.add(new Requests.VerificationItem("ACCESS_BLOCKED",true,Map.of("checked",true,"simulation",true)));
            items.add(new Requests.VerificationItem("VM_ABSENT",true,manifest==null?Map.of("simulation",true):Map.of("vmid",manifest.get("vmid"),"simulation",true)));
            for(String kind:List.of("VOLUMES_ABSENT","SNIPPETS_ABSENT","INGRESS_ABSENT")){
                String field=switch(kind){case "VOLUMES_ABSENT"->"volumeIds";case "SNIPPETS_ABSENT"->"snippetIds";default->"ingressRuleIds";};
                items.add(new Requests.VerificationItem(kind,true,Map.of("checkedIds",manifest==null?List.of():manifest.get(field),"simulation",true)));
            }
            workers.verify(id,new Requests.Verification(WORKER,revision,command,op,null,clock.instant(),items),"demo-worker");return;
        }
        if(type.equals("PREPARE")||type.equals("START"))resources.allocate(id,new Requests.Allocate(WORKER,command));
        if(type.equals("PREPARE")||type.equals("RESET_CREDENTIAL")){
            var credential=(Map<String,Object>)workers.pendingCredential(id,WORKER,command);
            workers.applied(id,new Requests.CredentialApplied(WORKER,((Number)credential.get("credentialVersion")).intValue(),command,revision,op,null,true),"demo-worker");
            if(type.equals("RESET_CREDENTIAL"))return;
        }
        var view=resources.resources(rental);var allocation=(Map<String,Object>)view.get("allocation");var ip=(Map<String,Object>)view.get("ip");var gpu=(Map<String,Object>)view.get("gpu");
        int vmid=((Number)allocation.get("vmid")).intValue();
        resources.append(id,new Requests.Manifest(WORKER,command,List.of("DEMO-volume-"+vmid),List.of("DEMO-snippet-"+vmid),List.of("DEMO-ingress-"+vmid)));
        boolean inSlot=db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,rental,Rows.ts(clock.instant()),Rows.ts(clock.instant()));boolean stop=type.equals("STOP");
        workers.observe(id,new Requests.Observation(WORKER,command,revision,vmid,ip.get("address").toString(),((Number)gpu.get("generation")).longValue(),clock.instant(),stop?"STOPPED":"RUNNING",true,!stop&&inSlot,"SHA256:DEMO-HOST-KEY-"+vmid,op,null,stop?true:null),"demo-worker");
    }
    @Transactional public void heartbeat(long rental){
        var rows=db.queryForList("SELECT v.*,r.desired_revision,r.status,g.generation,s.inventory_source FROM vm_instance v JOIN rental r ON r.id=v.rental_id JOIN gpu_lease g ON g.server_id=r.server_id JOIN server_node s ON s.id=r.server_id WHERE r.id=? AND s.inventory_source='DEMO'",rental);
        if(rows.isEmpty())return;var v=rows.getFirst();if(Set.of("RETURNING","RETURNED","CANCELLED").contains(v.get("status")))return;
        var previous=Rows.instant(v,"observed_at");if(previous!=null&&!clock.instant().isAfter(previous))return;
        boolean slot=db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,rental,Rows.ts(clock.instant()),Rows.ts(clock.instant()));
        boolean access="ACTIVE".equals(v.get("status"))&&slot&&"RUNNING".equals(v.get("state"));
        health.report(rental,new Requests.VmHealth(Rows.number(v,"desired_revision"),((Number)v.get("vmid")).intValue(),Rows.number(v,"generation"),clock.instant(),v.get("state").toString(),true,access,(String)v.get("host_key_fingerprint")),"demo-worker");
    }
}
