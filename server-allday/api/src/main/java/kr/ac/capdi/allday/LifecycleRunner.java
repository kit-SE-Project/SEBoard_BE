package kr.ac.capdi.allday;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component @EnableScheduling
@ConditionalOnProperty(name="allday.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class LifecycleRunner {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(LifecycleRunner.class);
    private final LifecycleService service;
    public LifecycleRunner(LifecycleService service) {this.service=service;}
    @Scheduled(fixedDelayString="${allday.scheduler-delay-ms:5000}",initialDelay=5000)
    public void run() {
        try {service.tick();}
        catch(Exception e) {log.warn("lifecycle scheduling failed type={}",e.getClass().getSimpleName());}
    }
}
