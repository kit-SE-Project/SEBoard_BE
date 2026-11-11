package kr.ac.capdi.allday;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Retained login accounts can exercise physical equipment without presentation fixtures. */
@Service
public class RealTestMode {
    public final boolean enabled;
    public final boolean primary;
    public RealTestMode(boolean enabled,boolean seed,boolean worker,boolean devSeed) {
        this(enabled,seed,worker,devSeed,false);
    }
    @Autowired
    public RealTestMode(@Value("${allday.real-test-enabled:false}") boolean enabled,
            @Value("${allday.demo-seed-enabled:false}") boolean seed,
            @Value("${allday.demo-worker-enabled:false}") boolean worker,
            @Value("${allday.dev-seed-enabled:false}") boolean devSeed,
            @Value("${allday.primary-enabled:false}") boolean primary) {
        if((enabled||primary)&&(seed||worker||devSeed))throw new IllegalStateException("실장비 환경에서는 데모/개발 seed와 모의 worker를 끄세요.");
        this.enabled=enabled||primary;
        this.primary=primary;
    }
    public String name(){return primary?"MAIN":enabled?"REAL_TEST":"DEMO";}
}
