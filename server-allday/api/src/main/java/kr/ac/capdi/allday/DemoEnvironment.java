package kr.ac.capdi.allday;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @Order(0) @ConditionalOnProperty(name="allday.demo-auth-enabled",havingValue="true")
public class DemoEnvironment implements ApplicationRunner {
    private final JdbcTemplate db;private final RealTestMode mode;private final String primaryDatabase;
    public DemoEnvironment(JdbcTemplate db,RealTestMode mode,@Value("${allday.primary-database:capdi_main}") String primaryDatabase){
        this.db=db;this.mode=mode;this.primaryDatabase=primaryDatabase;
    }
    static void validatePrimaryDatabase(String actual,String configured){
        if(!configured.matches("[a-z][a-z0-9_]{0,62}")||java.util.Set.of("capdi_demo","capdi_test","capdi_dev").contains(configured)||!configured.equals(actual))
            throw new IllegalStateException("메인 실행은 지정한 전용 DB에서만 허용됩니다.");
    }
    @Override public void run(ApplicationArguments args){
        if(mode.primary)validatePrimaryDatabase(db.queryForObject("SELECT current_database()",String.class),primaryDatabase);
        else DemoSeed.guard(db);
    }
}
