package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {
    private final RentalService rentals;private final IdempotencyService idem;private final JdbcTemplate db;
    public ApiController(RentalService rentals,IdempotencyService idem,JdbcTemplate db) {this.rentals=rentals;this.idem=idem;this.db=db;}
    ResponseEntity<?> response(IdempotencyService.Result result) {return ResponseEntity.status(result.status()).body(result.body());}
    @GetMapping("/health") public Object health() {db.queryForObject("SELECT 1",Integer.class);return Map.of("status","UP","database","UP","vmIntegration","NOT_VERIFIED");}
    @GetMapping("/api/v1/me") public Actor me(HttpServletRequest r) {return Actor.from(r);}
    @GetMapping("/api/v1/categories") public Object categories() {return rentals.categories();}
    @GetMapping("/api/v1/servers") public Object servers(@RequestParam(required=false) Long categoryId) {return rentals.servers(categoryId);}
    @GetMapping("/api/v1/servers/{id}/availability") public Object availability(@PathVariable long id,@RequestParam Instant from,@RequestParam Instant to) {return rentals.availability(id,from,to);}
    @PostMapping("/api/v1/rental-previews") public Object preview(@Valid @RequestBody Requests.Reservation request,HttpServletRequest r) {return rentals.preview(Actor.from(r),request);}
    @PostMapping("/api/v1/rentals") public ResponseEntity<?> reserve(@Valid @RequestBody Requests.Reservation request,
        @RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);return response(idem.run(actor,"POST /api/v1/rentals",key,request,()->rentals.create(actor,request,Actor.requestId(r))));
    }
    @GetMapping("/api/v1/rentals") public Object mine(@RequestParam(defaultValue="50") int limit,@RequestParam(defaultValue="0") int offset,HttpServletRequest r) {return rentals.list(Actor.from(r),false,limit,offset);}
    @GetMapping("/api/v1/rentals/{id}") public Object detail(@PathVariable long id,HttpServletRequest r) {return rentals.detail(id,Actor.from(r));}
    @PostMapping("/api/v1/rentals/{id}/cancel") public ResponseEntity<?> cancel(@PathVariable long id,
        @Valid @RequestBody Requests.Change request,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);return response(idem.run(actor,"POST /api/v1/rentals/"+id+"/cancel",key,request,()->rentals.finish(id,actor,request,Actor.requestId(r),true,false)));
    }
    @PostMapping("/api/v1/rentals/{id}/return") public ResponseEntity<?> finish(@PathVariable long id,
        @Valid @RequestBody Requests.Change request,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);return response(idem.run(actor,"POST /api/v1/rentals/"+id+"/return",key,request,()->rentals.finish(id,actor,request,Actor.requestId(r),false,false)));
    }
    @GetMapping("/api/v1/rentals/{id}/credential") public Object credential(@PathVariable long id,HttpServletRequest r) {return rentals.credential(id,Actor.from(r),Actor.requestId(r));}
    @PostMapping("/api/v1/rentals/{id}/credential-resets") public ResponseEntity<?> reset(@PathVariable long id,
        @Valid @RequestBody Requests.Change request,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);return response(idem.run(actor,"POST /api/v1/rentals/"+id+"/credential-resets",key,request,()->rentals.reset(id,actor,request,Actor.requestId(r))));
    }
    @GetMapping("/api/v1/rentals/{id}/connection") public Object connection(@PathVariable long id,HttpServletRequest r) {return rentals.connection(id,Actor.from(r));}
    @GetMapping("/api/v1/rentals/{id}/events") public Object events(@PathVariable long id,HttpServletRequest r) {return rentals.events(id,Actor.from(r));}
    @GetMapping("/api/v1/rentals/{id}/return-verifications") public Object verifications(@PathVariable long id,HttpServletRequest r) {return rentals.verifications(id,Actor.from(r));}
    @GetMapping("/api/v1/admin/rentals") public Object all(@RequestParam(defaultValue="50") int limit,@RequestParam(defaultValue="0") int offset,HttpServletRequest r) {return rentals.list(Actor.from(r),true,limit,offset);}
    @PostMapping("/api/v1/admin/rentals/{id}/force-return") public ResponseEntity<?> force(@PathVariable long id,
        @Valid @RequestBody Requests.Change request,@RequestHeader("Idempotency-Key") String key,HttpServletRequest r) {
        Actor actor=Actor.from(r);actor.requireAdmin();return response(idem.run(actor,"POST /api/v1/admin/rentals/"+id+"/force-return",key,request,()->rentals.finish(id,actor,request,Actor.requestId(r),false,true)));
    }
}
