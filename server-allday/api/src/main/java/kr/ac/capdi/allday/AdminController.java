package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService service;private final IdempotencyService idem;
    public AdminController(AdminService service,IdempotencyService idem) {this.service=service;this.idem=idem;}
    @GetMapping("/categories")
    public Object categories(HttpServletRequest request) {
        return service.categories(Actor.from(request));
    }

    @PatchMapping("/categories/{id}")
    public ResponseEntity<?> editCategory(@PathVariable long id,@Valid @RequestBody Requests.CategoryEdit input,
            @RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        Actor actor=Actor.from(request);
        actor.requireAdmin();
        var result=idem.run(actor,"PATCH /api/v1/admin/categories/"+id,key,input,
            ()->new IdempotencyService.Result(200,service.editCategory(actor,id,input)));
        return ResponseEntity.status(result.status()).body(result.body());
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable long id,@Valid @RequestBody Requests.CategoryDelete input,
            @RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        Actor actor=Actor.from(request);
        actor.requireAdmin();
        var result=idem.run(actor,"DELETE /api/v1/admin/categories/"+id,key,input,()->{
            service.deleteCategory(actor,id,input);
            return new IdempotencyService.Result(204,null);
        });
        return ResponseEntity.status(result.status()).build();
    }

    @PostMapping("/categories") public ResponseEntity<?> category(@Valid @RequestBody Requests.CategoryInput input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();var result=idem.run(actor,"POST /api/v1/admin/categories",key,input,()->new IdempotencyService.Result(201,service.category(actor,input)));return ResponseEntity.status(result.status()).body(result.body());
    }
    @PostMapping("/servers") public ResponseEntity<?> server(@Valid @RequestBody Requests.ServerInput input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();var result=idem.run(actor,"POST /api/v1/admin/servers",key,input,()->new IdempotencyService.Result(201,service.server(actor,input)));return ResponseEntity.status(result.status()).body(result.body());
    }
    @PatchMapping("/servers/{id}/status") public Object status(@PathVariable long id,@Valid @RequestBody Requests.ServerStatus input,HttpServletRequest r) {return service.status(Actor.from(r),id,input);}
    @PatchMapping("/servers/{id}") public Object edit(@PathVariable long id,@Valid @RequestBody Requests.ServerEdit input,HttpServletRequest r) {
        return service.edit(Actor.from(r),id,input,Actor.requestId(r));
    }
    @DeleteMapping("/servers/{id}") public ResponseEntity<?> delete(@PathVariable long id,@Valid @RequestBody Requests.ServerDelete input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();
        var result=idem.run(actor,"DELETE /api/v1/admin/servers/"+id,key,input,()->{service.deactivate(actor,id,input,Actor.requestId(r));return new IdempotencyService.Result(204,null);});
        return ResponseEntity.status(result.status()).build();
    }
    @PostMapping("/jobs/{id}/retry") public ResponseEntity<?> retry(@PathVariable long id,@Valid @RequestBody Requests.Change input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();var result=idem.run(actor,"POST /api/v1/admin/jobs/"+id+"/retry",key,input,()->new IdempotencyService.Result(202,service.retry(actor,id,input,Actor.requestId(r))));return ResponseEntity.status(result.status()).body(result.body());
    }
    @GetMapping("/events") public Object events(@RequestParam(defaultValue="100") int limit,HttpServletRequest r) {return service.events(Actor.from(r),limit);}
    @PostMapping("/rentals/{id}/start-now") public ResponseEntity<?> startNow(@PathVariable long id,@Valid @RequestBody Requests.PrepareNow input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();
        var result=idem.run(actor,"POST /api/v1/admin/rentals/"+id+"/start-now",key,input,()->new IdempotencyService.Result(202,service.startNow(actor,id,input,Actor.requestId(r))));
        return ResponseEntity.status(result.status()).body(result.body());
    }
    @PostMapping("/rentals/{id}/prepare-now") public ResponseEntity<?> prepareNow(@PathVariable long id,@Valid @RequestBody Requests.PrepareNow input,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();
        var result=idem.run(actor,"POST /api/v1/admin/rentals/"+id+"/prepare-now",key,input,()->new IdempotencyService.Result(202,service.prepareNow(actor,id,input,Actor.requestId(r))));
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
