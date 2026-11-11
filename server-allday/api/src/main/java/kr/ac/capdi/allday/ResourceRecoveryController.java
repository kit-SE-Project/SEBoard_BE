package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/rentals/{id}/resource-lock")
public class ResourceRecoveryController {
    private final ResourceRecoveryService recovery;
    private final IdempotencyService idem;
    public ResourceRecoveryController(ResourceRecoveryService recovery,IdempotencyService idem) {
        this.recovery=recovery;this.idem=idem;
    }
    @GetMapping public Object inspect(@PathVariable long id,HttpServletRequest request) {
        return recovery.inspect(id,Actor.from(request));
    }
    @PostMapping("/reconcile") public ResponseEntity<?> reconcile(@PathVariable long id,
        @Valid @RequestBody Requests.ResourceLockReconcile input,
        @RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        Actor actor=Actor.from(request);actor.requireAdmin();
        var result=idem.run(actor,"POST /api/v1/admin/rentals/"+id+"/resource-lock/reconcile",key,input,
            ()->new IdempotencyService.Result(200,recovery.reconcile(id,actor,input,Actor.requestId(request))));
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
