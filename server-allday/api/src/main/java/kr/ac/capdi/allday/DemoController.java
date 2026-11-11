package kr.ac.capdi.allday;

import java.time.Clock;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController @ConditionalOnProperty(name="allday.demo-auth-enabled",havingValue="true")
public class DemoController {
    private final JdbcTemplate db;private final RentalService rentals;private final Clock clock;private final RealTestMode mode;
    public DemoController(JdbcTemplate db,RentalService rentals,Clock clock,RealTestMode mode){this.db=db;this.rentals=rentals;this.clock=clock;this.mode=mode;}
    @GetMapping("/api/v1/demo/scenarios") public Object scenarios(HttpServletRequest request){
        Actor actor=Actor.from(request);
        return Map.of("mode",mode.name(),"hardwareIntegration",mode.enabled?"NOT_VERIFIED":"SIMULATED","now",clock.instant(),"timezone","Asia/Seoul",
            "servers",rentals.servers(null),"rentals",rentals.list(actor,false,100,0));
    }
}
