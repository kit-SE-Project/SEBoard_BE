"""PVE VM lifecycle using the team's installed Node Helper v0.2 protocol."""
import json,re,time,uuid
from .common import Problem,RemoteProblem
from .proxmox import ProxmoxDriver
from .helper_client import HelperClient,HelperTransport,lease_seconds,password_hash


class HelperProxmoxDriver(ProxmoxDriver):
    def __init__(self,store,config,backend):
        super().__init__(store,config);self.backend=backend
        if config.get('gateway_protocol')!='NODE_HELPER_V02':raise ValueError('Explicit Node Helper protocol required')
        self.helper=HelperClient(store,{key:HelperTransport(self.node(int(key))['helper']) for key in self.nodes}) if store else None
        if store is None:
            for key in self.nodes:HelperTransport(self.node(int(key))['helper'])
        for node in self.nodes.values():
            if type(node.get('public_ssh_port')) is not int or not 1<=node['public_ssh_port']<=65535:raise ValueError('Actual student SSH port required')
    def check_source(self,context):
        super().check_source(context)
        if context['server'].get('publicSshPort')!=self.node(context['server']['id'])['public_ssh_port']:raise Problem('REAL_SSH_ROUTE_MISMATCH')
    def checkpoint(self,p,manifest,guard):
        guard();self.backend.call('POST',f'/api/internal/jobs/{p["jobId"]}/manifest',{'workerId':self.config['worker_id'],'commandId':p['commandId'],**manifest})
    def refs(self,operation):
        row=self.store.row("SELECT response FROM helper_steps WHERE operation_id=? AND step='snippets'",operation['id'])
        if not row or not row['response']:return {}
        return json.loads(row['response']).get('references',{})
    def vm_manifest(self,cfg,p,refs):
        value=self.manifest(cfg,p);value['snippetIds']=sorted(refs.values());return value
    def snippet_payload(self,operation,p,credential):
        # Snippets are immutable for a rental. Reuse their persisted salted hash
        # across PREPARE attempts instead of inventing a different hash each time.
        previous=self.store.row("SELECT s.request FROM helper_steps s JOIN helper_workflows w ON w.operation_id=s.operation_id WHERE w.server_id=? AND w.rental_id=? AND s.step='snippets' ORDER BY w.control_revision DESC LIMIT 1",p['serverId'],p['rentalId'])
        if previous:
            saved=json.loads(previous['request'])['payload']
            if saved.get('rentalId')!=p['rentalId'] or saved.get('username')!=credential['username'] or saved.get('hostname')!=f'allday-r{p["rentalId"]}':raise Problem('HELPER_SNIPPET_OWNER_MISMATCH')
            return saved
        return {'rentalId':p['rentalId'],'username':credential['username'],'passwordHash':password_hash(credential['password']),'hostname':f'allday-r{p["rentalId"]}'}
    def close_verified(self,operation,step):
        self.helper.close(operation,step)
        status=self.helper.status(self.helper.workflow(operation)['server_id'])
        if status.get('accessEnabled') is not False or status.get('needsRecovery') is not False or status.get('pendingCleanup'):raise Problem('ACCESS_BLOCK_UNCONFIRMED')
    def guest_command(self,prefix,command,guard,timeout=30):
        guard();started=self.api.call('POST',prefix+'/agent/exec',{'command':command})
        if not isinstance(started,dict) or type(started.get('pid')) is not int or started['pid']<=0:raise Problem('GUEST_COMMAND_UNCONFIRMED')
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            guard();result=self.api.call('GET',prefix+'/agent/exec-status',{'pid':started['pid']})
            if result.get('exited'):
                if result.get('out-truncated') or result.get('signal') is not None:raise Problem('GUEST_COMMAND_UNCONFIRMED')
                return result
            time.sleep(1)
        raise Problem('GUEST_COMMAND_TIMEOUT')
    def wait_cloud_init(self,prefix,guard,timeout=120):
        # QGA and even the assigned address can be ready before first-boot files
        # have finished being written. Never advance PREPARE on agent/ping alone.
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            result=self.guest_command(prefix,['/usr/bin/cloud-init','status','--format=json'],guard)
            try:state=json.loads(result.get('out-data',''))
            except (ValueError,TypeError):raise Problem('CLOUD_INIT_UNCONFIRMED') from None
            if not isinstance(state,dict):raise Problem('CLOUD_INIT_UNCONFIRMED')
            if state.get('status') in {'error','disabled'} or state.get('errors'):raise Problem('CLOUD_INIT_FAILED')
            if state.get('status')=='done' and result.get('exitcode') in {0,2}:return
            if result.get('exitcode') not in {0,2}:raise Problem('CLOUD_INIT_UNCONFIRMED')
            time.sleep(2)
        raise Problem('CLOUD_INIT_NOT_READY')
    def wait_guest_ip(self,prefix,address,guard,timeout=90):
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            guard()
            try:
                interfaces=self.api.call('GET',prefix+'/agent/network-get-interfaces').get('result',[])
                if any(a.get('ip-address')==address for interface in interfaces for a in interface.get('ip-addresses',[])):return
            except RemoteProblem:pass
            time.sleep(2)
        raise Problem('GUEST_IP_UNCONFIRMED')
    def open_verified(self,operation,p,window):
        seconds=lease_seconds(window)
        if seconds<5:raise Problem('ACCESS_WINDOW_CLOSED')
        result=self.helper.open(operation,p['rentalId'],seconds)
        status=self.helper.status(p['serverId']);wf=self.helper.workflow(operation)
        if result.get('enabled') is not True or status.get('accessEnabled') is not True or status.get('ownerRentalId')!=p['rentalId'] or status.get('generation')!=wf['current_generation'] or status.get('ingress',{}).get('target')!=p['ip']:
            self.close_verified(operation,'unconfirmed-open-'+str(uuid.uuid4()));raise Problem('ACCESS_OPEN_UNCONFIRMED')
    def execute(self,operation,context,credential,guard):
        p=json.loads(operation['payload'])
        with self.helper.locks[str(p['serverId'])]:return self.execute_locked(operation,p,context,credential,guard)
    def execute_locked(self,operation,p,context,credential,guard):
        self.check_source(context);node=self.node(p['serverId']);name=node['node_name'];vmid=p['vmid'];kind=p['type'];prefix=f'/nodes/{name}/qemu/{vmid}';upid=None
        if kind=='RETURN' and vmid is None:
            return self.return_unallocated(p,context,guard)
        if kind=='PREPARE' and self.config_vm(name,vmid) is None:self.check_clone_pool(node)
        guard();self.helper.begin(operation,p);self.close_verified(operation,'initial-close')
        cfg=self.owned(p,node,True) if vmid else None
        if kind=='RETURN':return self.return_vm(operation,p,context,cfg,guard)
        if kind=='PREPARE' and cfg is None:
            template=context.get('template')
            if not template or not template.get('approvedAt') or template.get('deprecatedAt'):raise Problem('APPROVED_TEMPLATE_REQUIRED')
            template_cfg=self.config_vm(name,template['templateVmid'])
            if not template_cfg or template_cfg.get('template')!=1:raise Problem('PVE_TEMPLATE_UNCONFIRMED')
            self.validate_pci_config(template_cfg,node)
        elif cfg is None:raise Problem('PVE_VM_ABSENT')
        if kind in ['PREPARE','START','STOP']:
            if cfg and self.api.call('GET',prefix+'/status/current')['status']!='stopped':
                # stop is a power cut: a START immediately after PREPARE could
                # lose unflushed cloud-init/netplan cache files. Fail closed if
                # graceful shutdown times out; do not fall back to forceStop.
                upid=self.task(operation,'quiesce',name,'POST',prefix+'/status/shutdown',{'timeout':90,'forceStop':0},guard)
                if self.api.call('GET',prefix+'/status/current')['status']!='stopped':raise Problem('PVE_STOP_UNCONFIRMED')
            if kind=='STOP':
                cfg=self.owned(p,node);devices=self.pci_devices(cfg)
                if devices:guard();self.api.call('PUT',prefix+'/config',{'delete':','.join(devices)})
                detached=not self.pci_devices(self.owned(p,node))
                return {'simulation':False,'upid':upid,'observation':{'vmid':vmid,'ip':p['ip'],'vmState':'STOPPED','healthPassed':detached,'accessEnabled':False,'hostKeyFingerprint':None,'gpuDetached':detached}}
        refs={}
        if kind=='PREPARE':
            volumes=self.manifest(cfg,p)['volumeIds'] if cfg else []
            guard();self.helper.call(operation,'register-empty','REGISTER_RESOURCE',{'rentalId':p['rentalId'],'vmId':vmid,'vmIp':p['ip'],'volumeIds':volumes})
            payload=self.snippet_payload(operation,p,credential)
            guard();snippets=self.helper.call(operation,'snippets','PUT_SNIPPET',payload);refs=snippets.get('references',{})
            if snippets.get('verified') is not True or set(refs)!={'user','network','meta'} or any(not isinstance(v,str) or not re.fullmatch(r'[A-Za-z0-9_-]+:snippets/allday-r'+str(p['rentalId'])+r'-(user|network|meta)\.yaml',v) for v in refs.values()):raise Problem('HELPER_SNIPPETS_UNCONFIRMED')
            self.checkpoint(p,{'volumeIds':[],'snippetIds':sorted(refs.values()),'ingressRuleIds':[f'allday-r{p["rentalId"]}-s{p["serverId"]}']},guard)
            if cfg is None:
                upid=self.task(operation,'clone',name,'POST',f'/nodes/{name}/qemu/{template["templateVmid"]}/clone',self.clone_parameters(p,node),guard)
                cfg=self.owned(p,node)
            manifest=self.vm_manifest(cfg,p,refs);self.checkpoint(p,manifest,guard)
            guard();self.helper.call(operation,'register-volumes','REGISTER_RESOURCE',{'rentalId':p['rentalId'],'vmId':vmid,'vmIp':p['ip'],'volumeIds':manifest['volumeIds']})
        if kind in ['PREPARE','START']:
            import ipaddress
            network=ipaddress.ip_network(context['server']['vmSubnet']);ip=ipaddress.ip_address(p['ip']);gateway=ipaddress.ip_address(node['gateway'])
            if ip not in network or gateway not in network or ip in [network.network_address,network.broadcast_address,gateway]:raise Problem('VM_NETWORK_MISMATCH')
            if not node.get('vm_bridge'):raise Problem('VM_BRIDGE_REQUIRED')
            if not cfg.get('net0'):raise Problem('VM_NETWORK_INTERFACE_REQUIRED')
            settings={'onboot':0,'agent':'enabled=1','ciuser':'student','ipconfig0':f'ip={ip}/{network.prefixlen},gw={gateway}','net0':','.join([v for v in cfg['net0'].split(',') if not v.startswith('bridge=')]+['bridge='+node['vm_bridge']])}
            if refs:settings['cicustom']=','.join(k+'='+refs[k] for k in ['user','network','meta'])
            self.validate_pci_config(cfg,node)
            if node.get('gpu_enabled',True):settings['hostpci0']='mapping='+node['gpu_mapping']+',pcie=1'
            guard();self.api.call('PUT',prefix+'/config',settings)
            # Helper generation is deliberately separate from backend gpuGeneration.
            wf=self.helper.workflow(operation);guard();self.helper.call(operation,'claim','ADVANCE_GENERATION',{'rentalId':p['rentalId'],'expectedGeneration':wf['base_generation']},advance=True)
            upid=self.task(operation,'start',name,'POST',prefix+'/status/start',{},guard)
        for _ in range(60):
            guard()
            try:self.api.call('POST',prefix+'/agent/ping',{});break
            except RemoteProblem:time.sleep(2)
        else:raise Problem('GUEST_AGENT_NOT_READY')
        if kind in ['PREPARE','START']:
            self.wait_cloud_init(prefix,guard)
            self.wait_guest_ip(prefix,p['ip'],guard)
            # Persist first-boot network/cache files before success is reported
            # and another backend job is allowed to reboot this VM.
            flushed=self.guest_command(prefix,['/usr/bin/sync'],guard)
            if flushed.get('exitcode')!=0:raise Problem('GUEST_SYNC_UNCONFIRMED')
        if kind in ['PREPARE','RESET_CREDENTIAL']:
            guard();self.api.call('POST',prefix+'/agent/set-user-password',{'username':credential['username'],'password':credential['password'],'crypted':0})
        result={'simulation':False,'upid':upid}
        if kind in ['PREPARE','RESET_CREDENTIAL']:result['credential']={'credentialVersion':credential['credentialVersion'],'passed':True}
        if kind=='RESET_CREDENTIAL':return result
        self.wait_guest_ip(prefix,p['ip'],guard)
        key=self.fingerprint(name,vmid);cfg=self.owned(p,node);status=self.api.call('GET',prefix+'/status/current')
        if status.get('status')!='running' or not self.gpu_ready(cfg,node):raise Problem('VM_READY_UNCONFIRMED')
        access=False
        if p['accessEnabled']:
            fresh=guard()
            if not fresh or not fresh.get('inSlot'):raise Problem('ACCESS_NOT_AUTHORIZED')
            self.open_verified(operation,p,fresh.get('accessWindow'));access=True
        result['manifest']=self.vm_manifest(cfg,p,refs) if refs else self.manifest(cfg,p)
        result['observation']={'vmid':vmid,'ip':p['ip'],'vmState':'RUNNING','healthPassed':True,'accessEnabled':access,'hostKeyFingerprint':key,'gpuDetached':None}
        return result
    def return_unallocated(self,p,context,guard):
        # Cancelling before allocation must not close another rental's ingress.
        resources=context['resources']
        if any(resources.get(k) is not None for k in ['allocation','ip','manifest']):raise Problem('RESOURCE_CONTEXT_MISMATCH')
        for operation in self.store.rows('SELECT payload FROM operations WHERE server_id=?',p['serverId']):
            previous=json.loads(operation['payload'])
            if previous.get('rentalId')==p['rentalId'] and previous.get('vmid') is not None:raise Problem('UNALLOCATED_RETURN_UNCONFIRMED')
        guard();status=self.helper.status(p['serverId'])
        if 'ownerRentalId' not in status or type(status.get('generation')) is not int:raise Problem('HELPER_INVALID_RESPONSE')
        if status.get('ownerRentalId')==p['rentalId'] or resources.get('gpu',{}).get('ownerRentalId')==p['rentalId']:raise Problem('UNALLOCATED_RETURN_UNCONFIRMED')
        items=[]
        for kind in ['ACCESS_BLOCKED','VM_ABSENT','VOLUMES_ABSENT','SNIPPETS_ABSENT','INGRESS_ABSENT']:
            evidence={'checked':True,'unallocatedRentalId':p['rentalId']} if kind=='ACCESS_BLOCKED' else {'vmid':None} if kind=='VM_ABSENT' else {'checkedIds':[]}
            items.append({'kind':kind,'passed':True,'evidence':evidence})
        return {'simulation':False,'upid':None,'verification':items}
    def return_vm(self,operation,p,context,cfg,guard):
        node=self.node(p['serverId']);name=node['node_name'];vmid=p['vmid'];prefix=f'/nodes/{name}/qemu/{vmid}';upid=None
        manifest=context['resources']['manifest'] or {'volumeIds':[],'snippetIds':[],'ingressRuleIds':[]}
        if cfg:
            if self.api.call('GET',prefix+'/status/current')['status']!='stopped':upid=self.task(operation,'return-stop',name,'POST',prefix+'/status/stop',{},guard)
            upid=self.task(operation,'delete',name,'DELETE',prefix,{'purge':1,'destroy-unreferenced-disks':1},guard)
        if vmid is not None and self.config_vm(name,vmid) is not None:raise Problem('PVE_DELETE_UNCONFIRMED')
        already_released=False
        try:
            guard();self.helper.call(operation,'remove-snippets','REMOVE_SNIPPET',{'rentalId':p['rentalId']})
        except Problem as error:
            # A prior RETURN may have completed physical cleanup but lost its
            # backend report. VERIFY_CLEANUP can read the retained released record.
            if error.code!='HELPER_RESOURCE_NOT_FOUND' or cfg is not None:raise
            already_released=True
        guard();verification=self.helper.call(operation,'verify','VERIFY_CLEANUP',{'rentalId':p['rentalId']})
        checks=verification.get('checks',[]);by_kind={item.get('kind'):item.get('status') for item in checks}
        required={'VM','TASKS','SNIPPETS','INGRESS','CONNECTIONS','LOCAL_STATE'}
        if verification.get('rentalId')!=p['rentalId'] or verification.get('result')!='PASS' or not required.issubset(by_kind) or not any(k.startswith('VOLUMES:') for k in by_kind) or any(v!='PASS' for v in by_kind.values()):raise Problem('HELPER_CLEANUP_INCOMPLETE')
        if already_released:
            status=self.helper.status(p['serverId'])
            if status.get('ownerRentalId') is not None or status.get('accessEnabled') is not False or status.get('needsRecovery') is not False or status.get('pendingCleanup'):raise Problem('HELPER_RELEASE_UNCONFIRMED')
        else:
            guard();released=self.helper.call(operation,'release','RELEASE_RESOURCE',{'rentalId':p['rentalId']})
            if released.get('released') is not True or released.get('verification',{}).get('result')!='PASS':raise Problem('HELPER_RELEASE_UNCONFIRMED')
        self.close_verified(operation,'final-close')
        items=[]
        for kind in ['ACCESS_BLOCKED','VM_ABSENT','VOLUMES_ABSENT','SNIPPETS_ABSENT','INGRESS_ABSENT']:
            evidence={'checked':True} if kind=='ACCESS_BLOCKED' else {'vmid':vmid} if kind=='VM_ABSENT' else {'checkedIds':manifest[{'VOLUMES_ABSENT':'volumeIds','SNIPPETS_ABSENT':'snippetIds','INGRESS_ABSENT':'ingressRuleIds'}[kind]]}
            items.append({'kind':kind,'passed':True,'evidence':evidence})
        return {'simulation':False,'upid':upid,'verification':items}
    def server_status(self,server):
        try:
            with self.helper.locks[str(server['id'])]:
                status=self.helper.status(server['id'])
                if status.get('needsRecovery') is not False or status.get('pendingCleanup'):return 'UNKNOWN'
                return super().server_status(server)
        except (Problem,KeyError):return 'UNKNOWN'
    def block_access(self,p):
        with self.helper.locks[str(p['serverId'])]:
            row=self.store.row('SELECT operation_id FROM helper_workflows WHERE server_id=? ORDER BY control_revision DESC LIMIT 1',p['serverId'])
            if row:self.close_verified({'id':row['operation_id']},'offline-close-'+str(uuid.uuid4()))
    def health(self,target):
        with self.helper.locks[str(target['serverId'])]:return self.health_locked(target)
    def health_locked(self,target):
        if self.store.row("SELECT id FROM operations WHERE server_id=? AND status IN ('PENDING','RUNNING','UNKNOWN')",target['serverId']):return None
        row=self.store.row("SELECT * FROM operations WHERE server_id=? AND status='SUCCEEDED' ORDER BY created_at DESC LIMIT 1",target['serverId'])
        if not row:return None
        p=json.loads(row['payload'])
        if any(p[k]!=target[k] for k in ['rentalId','vmid','desiredRevision','gpuGeneration']):return None
        report=self.store.row('SELECT stage FROM jobs WHERE operation_id=?',row['id'])
        if report and report['stage']!='DONE':return None
        # health() may have waited behind a long START while holding an old
        # READY/accessAllowed=false target. Reauthorize after acquiring the node
        # lock so an old health cycle cannot close freshly opened access.
        identity=['serverId','rentalId','vmid','desiredRevision','gpuGeneration']
        fresh=next((t for t in self.backend.call('GET','/api/internal/health-targets')
                    if all(t.get(k)==target.get(k) for k in identity)),None)
        if fresh is None:return None
        target=fresh
        node=self.node(p['serverId']);cfg=self.owned(p,node);prefix=f'/nodes/{node["node_name"]}/qemu/{p["vmid"]}'
        state=self.api.call('GET',prefix+'/status/current')['status'];running=state=='running';healthy=self.gpu_ready(cfg,node) if running else state=='stopped'
        if running:self.api.call('POST',prefix+'/agent/ping',{})
        status=self.helper.status(p['serverId']);wf=self.helper.workflow(row);access=False
        if target.get('accessAllowed') and running and healthy:
            seconds=lease_seconds(target.get('accessWindow'))
            if status.get('ownerRentalId')!=p['rentalId'] or status.get('generation')!=wf['current_generation'] or status.get('controlRevision')!=wf['control_revision']:raise Problem('HELPER_LEASE_MISMATCH')
            if seconds>=5:
                if status.get('accessEnabled') is True:
                    result=self.helper.open(row,p['rentalId'],seconds,renew=True);access=result.get('enabled') is True
                else:
                    self.close_verified(row,'expired-close-'+str(uuid.uuid4()));self.open_verified(row,p,target['accessWindow']);access=True
            else:self.close_verified(row,'window-close-'+str(uuid.uuid4()))
        else:self.close_verified(row,'health-close-'+str(uuid.uuid4()))
        return {'vmState':'RUNNING' if running else 'STOPPED' if state=='stopped' else 'UNKNOWN','healthPassed':healthy,'accessEnabled':access,'hostKeyFingerprint':self.fingerprint(node['node_name'],p['vmid']) if running else None}


def driver_class(config):
    return HelperProxmoxDriver if config.get('gateway_protocol')=='NODE_HELPER_V02' else ProxmoxDriver
