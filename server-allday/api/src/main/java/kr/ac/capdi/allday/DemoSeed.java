package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Order(20) @ConditionalOnProperty(name="allday.demo-seed-enabled",havingValue="true")
public class DemoSeed implements ApplicationRunner {
    public static final String PASSWORD="Demo1234!"; // Public, isolated presentation accounts only.
    private final JdbcTemplate db;private final PasswordEncoder passwords;private final RentalService rentals;private final InventoryService inventory;private final Clock clock;private final DemoJwt jwt;
    public DemoSeed(JdbcTemplate db,PasswordEncoder passwords,RentalService rentals,InventoryService inventory,Clock clock,DemoJwt jwt){this.db=db;this.passwords=passwords;this.rentals=rentals;this.inventory=inventory;this.clock=clock;this.jwt=jwt;}
    static void guard(JdbcTemplate db){String name=db.queryForObject("SELECT current_database()",String.class);if(!Set.of("capdi_demo","capdi_test").contains(name))throw new IllegalStateException("데모 데이터와 worker는 capdi_demo 또는 분리된 capdi_test에서만 실행할 수 있습니다.");}
    @Override @Transactional public void run(ApplicationArguments args){
        jwt.configured();guard(db);
        db.execute("SELECT pg_advisory_xact_lock(177093402)");
        for(String role:List.of("ROLE_USER","ROLE_KUMOH","ROLE_ADMIN"))db.update("INSERT INTO roles(name,description) VALUES (?,?) ON CONFLICT(name) DO NOTHING",role,"발표용 "+role);
        long student=member("junseok@demo.local","박준석","준석",List.of("ROLE_USER","ROLE_KUMOH"),"NORMAL",true);
        long secondStudent=member("student2@demo.local","김학생","학생 B",List.of("ROLE_USER","ROLE_KUMOH"),"NORMAL",true);
        long retainedStudent=member("retained@demo.local","이학생","학생 C",List.of("ROLE_USER","ROLE_KUMOH"),"NORMAL",true);
        long returningStudent=member("returning@demo.local","최학생","학생 D",List.of("ROLE_USER","ROLE_KUMOH"),"NORMAL",true);
        long admin=member("admin@demo.local","데모 관리자","관리자",List.of("ROLE_USER","ROLE_KUMOH","ROLE_ADMIN"),"NORMAL",true);
        member("guest@demo.local","미인증 회원","미인증 회원",List.of("ROLE_USER"),"NORMAL",false);
        member("suspended@demo.local","정지 회원","정지 회원",List.of("ROLE_USER","ROLE_KUMOH"),"SUSPENDED",true);
        long category=db.queryForObject("INSERT INTO category(name,description) VALUES ('발표용 GPU 서버','모든 장비와 VM 결과는 DEMO 시뮬레이션입니다.') ON CONFLICT (name) WHERE deleted_at IS NULL DO UPDATE SET name=excluded.name RETURNING id",Long.class);
        long[] nodes=new long[8];var adminActor=new Actor(admin,"ADMIN");
        for(int i=1;i<=8;i++){
            long node=db.queryForObject("INSERT INTO server_node(category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,status,observed_at,inventory_source) VALUES (?,?,?,'DEMO Ryzen 9',64,'DEMO RTX 3090','AVAILABLE',?,'DEMO') ON CONFLICT(node_name) DO UPDATE SET node_name=excluded.node_name RETURNING id",Long.class,category,"데모 GPU 서버 "+i,"presentation-node-"+i,Rows.ts(clock.instant()));nodes[i-1]=node;
            db.update("INSERT INTO gpu_lease(server_id) VALUES (?) ON CONFLICT DO NOTHING",node);
            var s=rentals.server(node,false);
            if(s.get("active_template_version_id")==null){
                inventory.configure(node,adminActor,new Requests.Inventory(Rows.number(s,"version"),"10.77."+i+".0/24","10.77."+i+".10","10.77."+i+".100",20000+i*100,20099+i*100,true,"DEMO-GPU-"+i,"demo-vm.invalid",2200+i,"발표용 가상 인벤토리"));
                inventory.template(node,adminActor,new Requests.Template(10000+i,"DEMO-IMAGE-SHA256","DEMO-DRIVER",Rows.number(rentals.server(node,false),"version"),"실장비 검증이 아닌 데모 템플릿"));
                db.update("UPDATE server_node SET inventory_source='DEMO',observed_status='AVAILABLE',observed_at=? WHERE id=?",Rows.ts(clock.instant()),node);
            }
        }
        // Seed once; API restart preserves reservations and edits rather than resetting them.
        if(!db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental WHERE purpose LIKE '[발표 초기 데이터]%')",Boolean.class)){
            var actor=new Actor(student,"STUDENT");var now=clock.instant();
            reserve(new Actor(secondStudent,"STUDENT"),nodes[1],"예약 대기·중복 예약 차단",now.plus(Duration.ofDays(1)),now.plus(Duration.ofDays(1)).plusSeconds(3600));
            reserve(adminActor,nodes[3],"VM 준비 완료·이용 시간 전",now.plusSeconds(1800),now.plusSeconds(3*3600));
            var returningActor=new Actor(returningStudent,"STUDENT");
            long returned=reserve(returningActor,nodes[4],"반납 완료·자원 검증",now.plusSeconds(30),now.plusSeconds(3600));
            long prepare=Rows.number(Rows.one(db,"SELECT id FROM job WHERE rental_id=? AND type='PREPARE'",returned),"id");
            db.update("UPDATE job SET status='SUPERSEDED' WHERE id=?",prepare);
            rentals.finish(returned,returningActor,new Requests.Change(0L,"발표용 반납 흐름"),"demo-seed",false,false);
            long cancelled=reserve(actor,nodes[5],"준비 전 예약 취소",now.plus(Duration.ofDays(1)),now.plus(Duration.ofDays(1)).plusSeconds(3600));
            rentals.finish(cancelled,actor,new Requests.Change(0L,"발표용 즉시 취소"),"demo-seed",true,false);
            reserve(actor,nodes[2],"이용 중·접속 정보·암호 재설정",now.plusSeconds(3),now.plusSeconds(4*3600));
            db.update("UPDATE server_node SET status='MAINTENANCE',status_reason='발표용 점검 상태' WHERE id=?",nodes[6]);
        }
        if(!db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental WHERE purpose='[발표 초기 데이터] 슬롯 사이 VM 보존')",Boolean.class)){
            var now=clock.instant();rentals.create(new Actor(retainedStudent,"STUDENT"),new Requests.Reservation(nodes[7],"[발표 초기 데이터] 슬롯 사이 VM 보존",List.of(new Requests.Slot(now.plusSeconds(3),now.plusSeconds(30)),new Requests.Slot(now.plusSeconds(3600),now.plusSeconds(7200))),null),"demo-seed");
        }
    }
    long reserve(Actor actor,long server,String purpose,Instant start,Instant end){return ((Number)((Map<?,?>)rentals.create(actor,new Requests.Reservation(server,"[발표 초기 데이터] "+purpose,List.of(new Requests.Slot(start,end)),null),"demo-seed").body()).get("id")).longValue();}
    long member(String login,String name,String nickname,List<String> roles,String status,boolean eligible){
        var existing=db.queryForList("SELECT b.member_id FROM accounts a JOIN board_users b USING(account_id) WHERE a.login_id=?",login);if(!existing.isEmpty())return Rows.number(existing.getFirst(),"member_id");
        var accounts=db.queryForList("SELECT account_id FROM accounts WHERE login_id=?",login);
        long account=accounts.isEmpty()?db.queryForObject("INSERT INTO accounts(login_id,name,password_hash,status) VALUES (?,?,?,?) RETURNING account_id",Long.class,login,name,passwords.encode(PASSWORD),status):Rows.number(accounts.getFirst(),"account_id");
        long member=db.queryForObject("INSERT INTO external_subject_binding(issuer,subject,last_eligibility,last_checked_at) VALUES ('server-allday-demo',?,?,?) RETURNING member_id",Long.class,Long.toString(account),eligible,Rows.ts(clock.instant()));
        db.update("INSERT INTO board_users(account_id,member_id,nickname) VALUES (?,?,?)",account,member,nickname);
        for(String role:roles)db.update("INSERT INTO role_accounts(account_id,role_id) SELECT ?,role_id FROM roles WHERE name=? ON CONFLICT DO NOTHING",account,role);
        return member;
    }
}
