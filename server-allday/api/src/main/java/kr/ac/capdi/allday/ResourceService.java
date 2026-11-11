package kr.ac.capdi.allday;

import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {
    private final JdbcTemplate db; private final WorkerService workers; private final RentalService rentals;
    private final JsonCodec json; private final Clock clock;private final AccessWindowService accessWindows;
    public ResourceService(JdbcTemplate db,WorkerService workers,RentalService rentals,JsonCodec json,Clock clock,AccessWindowService accessWindows) {this.db=db;this.workers=workers;this.rentals=rentals;this.json=json;this.clock=clock;this.accessWindows=accessWindows;}
    WorkerService.Context checked(long id,String worker,String command) {
        var c=workers.locked(id);workers.command(c,command);workers.lease(c,worker);return c;
    }
    Map<String,Object> resources(long rental) {
        var manifest=db.queryForList("SELECT * FROM resource_manifest WHERE rental_id=?",rental);
        var result=new LinkedHashMap<String,Object>();
        result.put("allocation",db.queryForList("SELECT id,vmid,server_id,rental_id,kind FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",rental).stream().map(Rows::api).findFirst().orElse(null));
        result.put("ip",db.queryForList("SELECT host(address) AS address FROM ip_lease WHERE rental_id=? AND released_at IS NULL",rental).stream().map(Rows::api).findFirst().orElse(null));
        result.put("manifest",manifest.isEmpty()?null:manifest(manifest.getFirst()));
        result.put("gpu",Rows.api(Rows.one(db,"SELECT g.* FROM gpu_lease g JOIN rental r ON r.server_id=g.server_id WHERE r.id=?",rental)));return result;
    }
    Map<String,Object> manifest(Map<String,Object> row) {
        var out=Rows.api(row);for(String key:List.of("volumeIds","snippetIds","ingressRuleIds","createdByCommandIds"))out.put(key,json.read(out.get(key).toString()));return out;
    }
    @Transactional public Object context(long id,String worker,String command) {
        var c=checked(id,worker,command);var result=new LinkedHashMap<String,Object>();
        result.put("job",workers.jobView(c.job()));result.put("rental",rentals.rentalView(c.rental()));
        result.put("inSlot",db.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_slot WHERE rental_id=? AND state<>'CANCELLED' AND start_at<=? AND end_at>?)",Boolean.class,c.rental().get("id"),Rows.ts(clock.instant()),Rows.ts(clock.instant())));
        result.put("accessWindow",accessWindows.forRental(Rows.number(c.rental(),"id")));
        result.put("server",Rows.api(Rows.one(db,"SELECT id,node_name,inventory_source,vm_subnet::text AS vm_subnet,gpu_enabled,gpu_mapping_ref,public_ssh_host,public_ssh_port,active_template_version_id,status,observed_status,observed_at FROM server_node WHERE id=?",c.rental().get("server_id"))));
        long server=Rows.number(c.rental(),"server_id");
        var templates=db.queryForList("SELECT t.id,t.template_vmid,t.image_hash,t.driver_version,t.approved_at,t.deprecated_at FROM template_version t JOIN server_node s ON s.active_template_version_id=t.id WHERE s.id=?",server);
        result.put("template",templates.isEmpty()?null:Rows.api(templates.getFirst()));result.put("resources",resources(Rows.number(c.rental(),"id")));return result;
    }
    @Transactional public Object allocate(long id,Requests.Allocate input) {
        var c=checked(id,input.workerId(),input.commandId());var r=c.rental();long rental=Rows.number(r,"id"),server=Rows.number(r,"server_id");
        if(!Set.of("PREPARE","START").contains(c.job().get("type"))||Set.of("RETURNING","RETURNED","CANCELLED").contains(r.get("status"))||!clock.instant().isBefore(Rows.instant(r,"contract_end")))throw ApiProblem.conflict("INVALID_STATE","현재 준비·시작 작업에서만 할당할 수 있습니다.");
        var node=rentals.server(server,false);
        if(!rentals.executionReady(node))throw new ApiProblem(503,"SERVER_UNAVAILABLE","서버 상태와 최신 관측을 확인하세요.");
        if(node.get("vmid_start")==null||node.get("ip_pool_start")==null||(Boolean.TRUE.equals(node.get("gpu_enabled"))&&node.get("gpu_mapping_ref")==null)||node.get("active_template_version_id")==null)
            throw ApiProblem.conflict("INVENTORY_NOT_CONFIGURED","실제 자원 pool·GPU 사용 여부·승인 템플릿을 먼저 등록하세요.");
        var template=Rows.one(db,"SELECT * FROM template_version WHERE id=?",node.get("active_template_version_id"));
        if(template.get("approved_at")==null||template.get("deprecated_at")!=null)throw ApiProblem.conflict("TEMPLATE_NOT_APPROVED","승인된 활성 템플릿이 필요합니다.");
        var gpu=Rows.one(db,"SELECT * FROM gpu_lease WHERE server_id=? FOR UPDATE",server);
        if(!Boolean.TRUE.equals(node.get("gpu_enabled"))&&"UNKNOWN".equals(gpu.get("state")))
            throw ApiProblem.conflict("RESOURCE_LOCK_UNKNOWN","VM 자원 소유 상태가 미확인입니다. 현재 예약의 반납 또는 공식 복구를 먼저 완료하세요.");
        if(!"FREE".equals(gpu.get("state"))&&(gpu.get("owner_rental_id")==null||Rows.number(gpu,"owner_rental_id")!=rental||!"HELD".equals(gpu.get("state"))))
            throw ApiProblem.conflict("GPU_NOT_FREE","이전 VM의 정지·GPU 분리를 확인하기 전에는 넘길 수 없습니다.");
        db.execute("SELECT pg_advisory_xact_lock(177093401)");
        var allocated=db.queryForList("SELECT * FROM vmid_allocation WHERE rental_id=? AND released_at IS NULL",rental);
        int vmid;
        if(allocated.isEmpty()) {
            var available=db.queryForList("SELECT candidate AS vmid FROM generate_series(?::integer,?::integer) candidate WHERE NOT EXISTS(SELECT 1 FROM vmid_allocation a WHERE a.vmid=candidate AND a.released_at IS NULL) ORDER BY candidate LIMIT 1",node.get("vmid_start"),node.get("vmid_end"));
            if(available.isEmpty())throw ApiProblem.conflict("VMID_POOL_EXHAUSTED","VMID pool에 여유가 없습니다.");vmid=((Number)available.getFirst().get("vmid")).intValue();
            db.update("INSERT INTO vmid_allocation(vmid,server_id,rental_id,kind,allocated_at) VALUES (?,?,?,'RENTAL',?)",vmid,server,rental,Rows.ts(clock.instant()));
        } else vmid=((Number)allocated.getFirst().get("vmid")).intValue();
        if(db.queryForObject("SELECT count(*) FROM ip_lease WHERE rental_id=? AND released_at IS NULL",Integer.class,rental)==0) {
            var ips=db.queryForList("SELECT host(s.ip_pool_start+offset_value::integer) AS address FROM server_node s CROSS JOIN LATERAL generate_series(0,(s.ip_pool_end-s.ip_pool_start)::integer) offset_value WHERE s.id=? AND NOT EXISTS(SELECT 1 FROM ip_lease i WHERE i.released_at IS NULL AND i.server_id=s.id AND i.address=s.ip_pool_start+offset_value::integer) ORDER BY offset_value LIMIT 1",server);
            if(ips.isEmpty())throw ApiProblem.conflict("IP_POOL_EXHAUSTED","IP pool에 여유가 없습니다.");
            db.update("INSERT INTO ip_lease(server_id,rental_id,address,allocated_at) VALUES (?,?,?::inet,?)",server,rental,ips.getFirst().get("address"),Rows.ts(clock.instant()));
        }
        db.update("INSERT INTO resource_manifest(rental_id,server_id,vmid,created_by_command_ids) VALUES (?,?,?,?::jsonb) ON CONFLICT (rental_id) DO NOTHING",rental,server,vmid,json.write(List.of(input.commandId())));
        if("FREE".equals(gpu.get("state")))db.update("UPDATE gpu_lease SET state='HELD',owner_rental_id=?,owner_vmid=?,generation=generation+1,changed_at=? WHERE server_id=?",rental,vmid,Rows.ts(clock.instant()),server);
        return resources(rental);
    }
    @SuppressWarnings("unchecked") List<String> merge(Object current,List<String> additions) {
        var ids=new LinkedHashSet<String>((List<String>)json.read(current.toString()));ids.addAll(additions);
        if(ids.size()>512)throw ApiProblem.invalid("manifest 항목이 512개를 초과했습니다.");return List.copyOf(ids);
    }
    @Transactional public Object append(long id,Requests.Manifest input) {
        var c=checked(id,input.workerId(),input.commandId());long rental=Rows.number(c.rental(),"id");
        var m=Rows.one(db,"SELECT * FROM resource_manifest WHERE rental_id=? FOR UPDATE",rental);
        db.update("UPDATE resource_manifest SET volume_ids=?::jsonb,snippet_ids=?::jsonb,ingress_rule_ids=?::jsonb,created_by_command_ids=?::jsonb WHERE rental_id=?",
            json.write(merge(m.get("volume_ids"),input.volumeIds())),json.write(merge(m.get("snippet_ids"),input.snippetIds())),json.write(merge(m.get("ingress_rule_ids"),input.ingressRuleIds())),json.write(merge(m.get("created_by_command_ids"),List.of(input.commandId()))),rental);
        return manifest(Rows.one(db,"SELECT * FROM resource_manifest WHERE rental_id=?",rental));
    }
}
