package kr.ac.capdi.allday;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @ConditionalOnProperty(name="allday.dev-seed-enabled",havingValue="true")
public class DevSeed implements ApplicationRunner {
    private final JdbcTemplate db;
    public DevSeed(JdbcTemplate db){this.db=db;}
    @Override @Transactional public void run(ApplicationArguments args) {
        for(long id:new long[]{1001,1002,9001})db.update("INSERT INTO external_subject_binding(member_id,issuer,subject,last_eligibility,last_checked_at) VALUES (?,'development',?,true,now()) ON CONFLICT (member_id) DO NOTHING",id,"dev-"+id);
        long category=db.queryForObject("INSERT INTO category(name,description) VALUES ('개발용 GPU 서버','실장비가 아닌 로컬 API 시험용 인벤토리') ON CONFLICT (name) WHERE deleted_at IS NULL DO UPDATE SET description=excluded.description RETURNING id",Long.class);
        for(int i=1;i<=2;i++) {
            long id=db.queryForObject("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,status,observed_at,inventory_source,vm_subnet) VALUES (?,?,?,'DEMO CPU',64,'DEMO GPU','AVAILABLE',now(),'DEMO',?::cidr) ON CONFLICT (node_name) DO UPDATE SET node_name=excluded.node_name RETURNING id",Long.class,
                category,"연습 서버 "+i,"demo-node-"+i,"192.168."+(100+i)+".0/24");
            db.update("INSERT INTO gpu_lease(server_id) VALUES (?) ON CONFLICT (server_id) DO NOTHING",id);
        }
    }
}
