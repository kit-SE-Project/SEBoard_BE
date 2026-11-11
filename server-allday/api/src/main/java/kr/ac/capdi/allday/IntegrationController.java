package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class IntegrationController {
    private final InventoryService inventory;private final ResourceService resources;private final LifecycleService lifecycle;private final RecoveryService recovery;
    @GetMapping("/api/v1/admin/servers") public Object inventory(HttpServletRequest r) {return inventory.inventory(Actor.from(r));}
    @PatchMapping("/api/v1/admin/servers/{id}/configuration") public Object configure(@PathVariable long id,@Valid @RequestBody Requests.Inventory input,HttpServletRequest r) {return inventory.configure(id,Actor.from(r),input);}
    @PostMapping("/api/v1/admin/servers/{id}/templates") public Object template(@PathVariable long id,@Valid @RequestBody Requests.Template input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();var result=idem.run(actor,"POST /api/v1/admin/servers/"+id+"/templates",key,input,()->new IdempotencyService.Result(201,inventory.template(id,actor,input)));return org.springframework.http.ResponseEntity.status(result.status()).body(result.body());
    }
    private final IdempotencyService idem;
    private final HealthService health;
    @org.springframework.beans.factory.annotation.Autowired
    public IntegrationController(InventoryService inventory,ResourceService resources,LifecycleService lifecycle,RecoveryService recovery,IdempotencyService idem,HealthService health) {this.inventory=inventory;this.resources=resources;this.lifecycle=lifecycle;this.recovery=recovery;this.idem=idem;this.health=health;}
    @PostMapping("/api/internal/servers/{id}/observation") public Object observeServer(@PathVariable long id,@Valid @RequestBody Requests.ServerObservation input) {return inventory.observe(id,input);}
    @GetMapping("/api/internal/inventory") public Object workerInventory(){return inventory.workerInventory();}
    @GetMapping("/api/internal/health-targets") public Object healthTargets(){return health.targets();}
    @PostMapping("/api/internal/lifecycle/tick") public Object tick() {return lifecycle.tick();}
    @PostMapping("/api/internal/rentals/{id}/health") public Object health(@PathVariable long id,@Valid @RequestBody Requests.VmHealth input,HttpServletRequest r) {return health.report(id,input,Actor.requestId(r));}
    @GetMapping("/api/internal/jobs/{id}/context") public Object context(@PathVariable long id,@RequestParam String workerId,@RequestParam String commandId) {return resources.context(id,workerId,commandId);}
    @PostMapping("/api/internal/jobs/{id}/resources") public Object allocate(@PathVariable long id,@Valid @RequestBody Requests.Allocate input) {return resources.allocate(id,input);}
    @PostMapping("/api/internal/jobs/{id}/manifest") public Object manifest(@PathVariable long id,@Valid @RequestBody Requests.Manifest input) {return resources.append(id,input);}
    @PostMapping("/api/internal/jobs/{id}/failure") public Object failure(@PathVariable long id,@Valid @RequestBody Requests.Failure input,HttpServletRequest r) {return recovery.failure(id,input,Actor.requestId(r));}
    @PostMapping("/api/v1/admin/jobs/{id}/reconcile") public Object reconcile(@PathVariable long id,@Valid @RequestBody Requests.Reconcile input,HttpServletRequest r) {return recovery.reconcile(id,Actor.from(r),input,Actor.requestId(r));}
}
