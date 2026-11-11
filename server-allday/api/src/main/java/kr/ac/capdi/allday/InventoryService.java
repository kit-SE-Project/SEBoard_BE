package kr.ac.capdi.allday;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final JdbcTemplate db; private final RentalService rentals; private final Clock clock;private final RealTestMode mode;
    public InventoryService(JdbcTemplate db,RentalService rentals,Clock clock,RealTestMode mode) {this.db=db;this.rentals=rentals;this.clock=clock;this.mode=mode;}
    void version(Map<String,Object> node,long expected) {
        if(Rows.number(node,"version")!=expected)throw ApiProblem.conflict("VERSION_CONFLICT","서버 설정이 변경되었습니다.");
    }
    void noActiveRentals(long id) {
        boolean inUse=db.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM vmid_allocation WHERE server_id=? AND kind='RENTAL' AND released_at IS NULL)
                OR EXISTS(SELECT 1 FROM ip_lease WHERE server_id=? AND released_at IS NULL)
                OR EXISTS(SELECT 1 FROM gpu_lease WHERE server_id=? AND state<>'FREE')
                OR EXISTS(SELECT 1 FROM job WHERE server_id=? AND status='RUNNING')
            """,Boolean.class,id,id,id,id);
        if(inUse) throw ApiProblem.conflict("INVENTORY_IN_USE","작업을 마치고 대여 자원을 반납한 뒤 인벤토리를 변경하세요.");
    }
    @Transactional public Object configure(long id,Actor actor,Requests.Inventory input) {
        actor.requireAdmin();var node=rentals.activeServer(id,true);version(node,input.expectedVersion());noActiveRentals(id);
        boolean gpuEnabled=input.gpuEnabled()!=null?input.gpuEnabled():Boolean.TRUE.equals(node.get("gpu_enabled"));
        String mapping=input.gpuMappingRef();
        if(input.gpuEnabled()==null&&mapping==null) mapping=(String)node.get("gpu_mapping_ref");
        if(gpuEnabled&&(mapping==null||!mapping.matches("[A-Za-z0-9_-]{1,64}")))
            throw ApiProblem.invalid("GPU 사용 시 실제 GPU mapping 이름이 필요합니다.");
        if(!gpuEnabled&&mapping!=null)throw ApiProblem.invalid("GPU 미사용 시 gpuMappingRef는 JSON null이어야 합니다.");
        if(input.vmidEnd()<input.vmidStart()||(long)input.vmidEnd()-input.vmidStart()>100000)throw ApiProblem.invalid("VMID pool은 정방향 최대 100001개입니다.");
        boolean valid;
        try {valid=db.queryForObject("SELECT family(?::cidr)=4 AND masklen(?::cidr)<=30 AND family(?::inet)=4 AND family(?::inet)=4 AND masklen(?::inet)=32 AND masklen(?::inet)=32 AND ?::inet<<=?::cidr AND ?::inet<<=?::cidr AND ?::inet>host(network(?::cidr))::inet AND ?::inet<host(broadcast(?::cidr))::inet AND ?::inet>=?::inet AND ?::inet-?::inet<=4095",Boolean.class,
            input.vmSubnet(),input.vmSubnet(),input.ipPoolStart(),input.ipPoolEnd(),input.ipPoolStart(),input.ipPoolEnd(),input.ipPoolStart(),input.vmSubnet(),input.ipPoolEnd(),input.vmSubnet(),input.ipPoolStart(),input.vmSubnet(),input.ipPoolEnd(),input.vmSubnet(),input.ipPoolEnd(),input.ipPoolStart(),input.ipPoolEnd(),input.ipPoolStart());}
        catch(org.springframework.dao.DataAccessException e){throw ApiProblem.invalid("IPv4 대역과 주소 형식을 확인하세요.");}
        if(!valid)throw ApiProblem.invalid("IP pool은 대역의 호스트 주소 중 최대 4096개여야 합니다.");
        if(db.queryForObject("SELECT EXISTS(SELECT 1 FROM server_node WHERE id<>? AND vm_subnet && ?::cidr)",Boolean.class,id,input.vmSubnet()))throw ApiProblem.conflict("SUBNET_OVERLAP","노드별 사설 대역을 겹치지 않게 등록하세요.");
        db.update("UPDATE server_node SET vm_subnet=?::cidr,ip_pool_start=?::inet,ip_pool_end=?::inet,vmid_start=?,vmid_end=?,gpu_enabled=?,gpu_mapping_ref=?,public_ssh_host=?,public_ssh_port=?,inventory_source='MANUAL',observed_at=NULL,observed_status='UNKNOWN',version=version+1,status_reason=?,changed_by=?,changed_at=? WHERE id=?",
            input.vmSubnet(),input.ipPoolStart(),input.ipPoolEnd(),input.vmidStart(),input.vmidEnd(),gpuEnabled,mapping,input.publicSshHost(),input.publicSshPort(),input.reason(),actor.memberId(),Rows.ts(clock.instant()),id);
        return Rows.api(Rows.one(db,"SELECT id,version,inventory_source,observed_status,gpu_enabled,gpu_mapping_ref FROM server_node WHERE id=?",id));
    }
    @Transactional public Object template(long id,Actor actor,Requests.Template input) {
        actor.requireAdmin();var node=rentals.activeServer(id,true);version(node,input.expectedVersion());noActiveRentals(id);
        db.execute("SELECT pg_advisory_xact_lock(177093401)");
        long allocation=db.queryForObject("INSERT INTO vmid_allocation(vmid,server_id,kind,allocated_at) VALUES (?,?,'TEMPLATE',?) RETURNING id",Long.class,input.templateVmid(),id,Rows.ts(clock.instant()));
        long template=db.queryForObject("INSERT INTO template_version(server_id,template_vmid,vmid_allocation_id,image_hash,driver_version,approved_at,approved_by,approval_reason) VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long.class,
            id,input.templateVmid(),allocation,input.imageHash(),input.driverVersion(),Rows.ts(clock.instant()),actor.memberId(),input.reason());
        db.update("UPDATE server_node SET active_template_version_id=?,version=version+1,status_reason=?,changed_by=?,changed_at=? WHERE id=?",template,input.reason(),actor.memberId(),Rows.ts(clock.instant()),id);
        return Map.of("id",template,"serverId",id,"templateVmid",input.templateVmid(),"serverVersion",Rows.number(node,"version")+1,"approved",true);
    }
    @Transactional public Object observe(long id,Requests.ServerObservation input) {
        var node=rentals.activeServer(id,true);Instant at=input.observedAt();
        if(at.isBefore(clock.instant().minusSeconds(120))||at.isAfter(clock.instant().plusSeconds(5)))throw ApiProblem.invalid("최신 서버 관측 시각이 필요합니다.");
        var previous=Rows.instant(node,"observed_at");
        if(previous!=null&&!at.isAfter(previous)) {
            if(at.equals(previous)&&input.status().equals(node.get("observed_status")))return Map.of("id",id,"observedStatus",input.status(),"observedAt",at);
            throw ApiProblem.conflict("STALE_OBSERVATION","기존 서버 관측보다 새 결과가 필요합니다.");
        }
        db.update("UPDATE server_node SET observed_status=?,observed_at=? WHERE id=?",input.status(),Rows.ts(at),id);
        return Map.of("id",id,"observedStatus",input.status(),"observedAt",at);
    }
    public Object inventory(Actor actor) {
        actor.requireAdmin();return db.queryForList("SELECT id,category_id,display_name,node_name,cpu_model,memory_gb,gpu_model,status,observed_status,observed_at,inventory_source,version,host(vm_subnet::inet) AS subnet_address,masklen(vm_subnet) AS subnet_prefix,host(ip_pool_start) AS ip_pool_start,host(ip_pool_end) AS ip_pool_end,vmid_start,vmid_end,gpu_enabled,gpu_mapping_ref,active_template_version_id,public_ssh_host,public_ssh_port,status_reason,changed_by,changed_at FROM server_node WHERE deleted_at IS NULL"+(mode.enabled?" AND inventory_source<>'DEMO'":"")+" ORDER BY id").stream().map(Rows::api).toList();
    }
    public Object workerInventory(){return db.queryForList("SELECT id,node_name,inventory_source,status,observed_status,gpu_enabled,gpu_mapping_ref,active_template_version_id FROM server_node WHERE deleted_at IS NULL"+(mode.enabled?" AND inventory_source<>'DEMO'":"")+" ORDER BY id").stream().map(Rows::api).toList();}
}
