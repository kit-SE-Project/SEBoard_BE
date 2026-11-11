package kr.ac.capdi.allday;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/internal/jobs")
public class WorkerController {
    private final WorkerService service;
    public WorkerController(WorkerService service){this.service=service;}
    @GetMapping public Object due(){return service.due();}
    @PostMapping("/{id}/claim") public Object claim(@PathVariable long id,@Valid @RequestBody Requests.Claim input){return service.claim(id,input.workerId());}
    @PostMapping("/{id}/heartbeat") public Object heartbeat(@PathVariable long id,@Valid @RequestBody Requests.Heartbeat input){return service.heartbeat(id,input.workerId(),input.commandId());}
    @PostMapping("/{id}/dispatch-deferred") public Object defer(@PathVariable long id,@Valid @RequestBody Requests.DispatchDeferred input,HttpServletRequest r){return service.defer(id,input,Actor.requestId(r));}
    @GetMapping("/{id}/pending-credential") public Object pending(@PathVariable long id,@RequestParam String workerId,@RequestParam String commandId){return service.pendingCredential(id,workerId,commandId);}
    @PostMapping("/{id}/credential-applied") public Object applied(@PathVariable long id,@Valid @RequestBody Requests.CredentialApplied input,HttpServletRequest r){return service.applied(id,input,Actor.requestId(r));}
    @PostMapping("/{id}/return-verification") public Object verify(@PathVariable long id,@Valid @RequestBody Requests.Verification input,HttpServletRequest r){return service.verify(id,input,Actor.requestId(r));}
    @PostMapping("/{id}/observation") public Object observe(@PathVariable long id,@Valid @RequestBody Requests.Observation input,HttpServletRequest r){return service.observe(id,input,Actor.requestId(r));}
}
