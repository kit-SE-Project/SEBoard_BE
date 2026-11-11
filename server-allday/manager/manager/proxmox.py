"""Configured Proxmox adapter. No insecure TLS or unowned-VM mutations."""
import base64,hashlib,ipaddress,json,re,ssl,subprocess,time,urllib.request,urllib.error,urllib.parse
from .common import Problem, RemoteProblem, canonical

class PveClient:
    def __init__(self,config):
        self.base=config['url'].rstrip('/')+'/api2/json';parsed=urllib.parse.urlsplit(self.base)
        if parsed.scheme!='https' or parsed.username or parsed.password:raise ValueError('Proxmox requires HTTPS')
        self.auth='PVEAPIToken='+config['token_id']+'='+config['token_secret'];self.context=ssl.create_default_context(cafile=config['ca_file']);self.timeout=config.get('timeout',20)
    def call(self,method,path,body=None):
        method=method.upper();data=None
        if body is not None:
            encoded=urllib.parse.urlencode(body,doseq=True)
            if method in ('GET','DELETE'):
                if encoded:path+=('&' if '?' in path else '?')+encoded
            elif method in ('POST','PUT'):data=encoded.encode()
            else:raise ValueError('Unsupported Proxmox request method with parameters')
        req=urllib.request.Request(self.base+path,data=data,headers={'Authorization':self.auth,'Content-Type':'application/x-www-form-urlencoded'},method=method)
        try:
            with urllib.request.urlopen(req,context=self.context,timeout=self.timeout) as response:return json.load(response)['data']
        except urllib.error.HTTPError as error:raise RemoteProblem('PVE_HTTP_ERROR',error.code) from None
        except (urllib.error.URLError,OSError,TimeoutError):raise RemoteProblem('PVE_TRANSPORT_UNCERTAIN',503) from None

class Gateway:
    def __init__(self,command):
        if not command or not isinstance(command,list) or not all(isinstance(x,str) for x in command):raise ValueError('Configured gateway command required')
        self.command=command
    def call(self,action,p,node):
        data={'action':action,'rentalId':p['rentalId'],'serverId':p['serverId'],'vmid':p.get('vmid'),'ip':p.get('ip'),'port':node['public_ssh_port']}
        result=subprocess.run(self.command,input=canonical(data),text=True,capture_output=True,timeout=30)
        if result.returncode:raise Problem('GATEWAY_UNCONFIRMED')
        try:return json.loads(result.stdout)
        except ValueError:raise Problem('GATEWAY_UNCONFIRMED') from None

class ProxmoxDriver:
    mode='PROXMOX'
    def __init__(self,store,config):
        self.store=store;self.config=config
        if config.get('allow_real_mutations') is not True or config.get('network_isolation_confirmed') is not True:raise ValueError('Real equipment and isolated VM network must be configured explicitly')
        self.api=PveClient(config['proxmox']);self.gateway=None if config.get('gateway_protocol')=='NODE_HELPER_V02' else Gateway(config['gateway_command']);self.nodes=config['nodes']
        if not self.nodes:raise ValueError('Assigned node configuration required')
    def node(self,server):
        node=self.nodes.get(str(server))
        if not node or not isinstance(node.get('node_name'),str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,64}',node['node_name']):raise Problem('NODE_NOT_CONFIGURED')
        enabled=node.get('gpu_enabled',True);mapping=node.get('gpu_mapping')
        if type(enabled) is not bool or (enabled and (not isinstance(mapping,str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,64}',mapping))) or (not enabled and mapping is not None):raise Problem('NODE_NOT_CONFIGURED')
        if node.get('vm_bridge') is not None and (not isinstance(node['vm_bridge'],str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,32}',node['vm_bridge'])):raise Problem('NODE_NOT_CONFIGURED')
        if node.get('vm_pool') is not None and (not isinstance(node['vm_pool'],str) or not re.fullmatch(r'[A-Za-z0-9_.-]{1,64}',node['vm_pool'])):raise Problem('NODE_NOT_CONFIGURED')
        return node
    def clone_parameters(self,p,node):
        body={'newid':p['vmid'],'full':1,'name':f'allday-{p["rentalId"]}',
              'description':f'Server Allday rental={p["rentalId"]} server={p["serverId"]}'}
        if node.get('vm_pool'):body['pool']=node['vm_pool']
        return body
    def check_clone_pool(self,node):
        if not node.get('vm_pool'):return
        path='/pool/'+node['vm_pool']
        permissions=self.api.call('GET','/access/permissions?'+urllib.parse.urlencode({'path':path}))
        if permissions.get(path,{}).get('VM.Allocate')!=1:raise Problem('PVE_RENTAL_POOL_PERMISSION_REQUIRED',403)
    def check_source(self,context):
        s=context['server'];node=self.node(s['id'])
        enabled=s.get('gpuEnabled',True)
        if s.get('inventorySource') not in ['MANUAL','AGENT'] or s['nodeName']!=node['node_name'] or type(enabled) is not bool or enabled!=node.get('gpu_enabled',True) or s.get('gpuMappingRef')!=node.get('gpu_mapping'):raise Problem('REAL_INVENTORY_MISMATCH',403)
    def pci_devices(self,cfg):
        return sorted(key for key in cfg if re.fullmatch(r'hostpci\d+',key))
    def gpu_ready(self,cfg,node):
        devices=self.pci_devices(cfg)
        if not node.get('gpu_enabled',True):return not devices
        return devices == ['hostpci0'] and 'mapping='+node['gpu_mapping'] in str(cfg['hostpci0']).split(',')

    def validate_pci_config(self,cfg,node):
        if not self.pci_devices(cfg) or self.gpu_ready(cfg,node):
            return
        if not node.get('gpu_enabled',True):
            raise Problem('PVE_GPU_DISABLED_BUT_ATTACHED')
        raise Problem('PVE_GPU_MAPPING_MISMATCH')

    def config_vm(self,node,vmid):
        # Proxmox often uses 500 for an absent config; prove absence through inventory.
        listing=self.api.call('GET',f'/nodes/{node}/qemu')
        if not any(int(vm['vmid'])==vmid for vm in listing):return None
        try:return self.api.call('GET',f'/nodes/{node}/qemu/{vmid}/config')
        except RemoteProblem:
            listing=self.api.call('GET',f'/nodes/{node}/qemu')
            if not any(int(vm['vmid'])==vmid for vm in listing):return None
            raise
    def owned(self,p,node,allow_absent=False):
        cfg=self.config_vm(node['node_name'],p['vmid'])
        if cfg is None and allow_absent:return None
        marker=f"Server Allday rental={p['rentalId']} server={p['serverId']}"
        if cfg is None or cfg.get('description')!=marker:raise Problem('PVE_VM_OWNER_MISMATCH')
        return cfg
    def task(self,operation,step,node,method,path,body,guard):
        guard();prior=self.store.row('SELECT * FROM effects WHERE operation_id=? AND step=?',operation['id'],step)
        if prior:upid=prior['upid']
        else:
            upid=self.api.call(method,path,body)
            if not isinstance(upid,str) or not upid.startswith('UPID:'):raise Problem('PVE_UPID_REQUIRED')
            self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',operation['id'],step,upid,'RUNNING')
        for _ in range(150):
            guard();status=self.api.call('GET',f'/nodes/{node}/tasks/{urllib.parse.quote(upid,safe="")}/status')
            if status['status']=='stopped':
                if status.get('exitstatus')!='OK':raise Problem('PVE_TASK_FAILED')
                self.store.execute("UPDATE effects SET status='SUCCEEDED' WHERE operation_id=? AND step=?",operation['id'],step);return upid
            time.sleep(2)
        raise Problem('PVE_TASK_TIMEOUT')
    def fingerprint(self,node,vmid):
        data=self.api.call('GET',f'/nodes/{node}/qemu/{vmid}/agent/file-read?'+urllib.parse.urlencode({'file':'/etc/ssh/ssh_host_ed25519_key.pub','count':4096,'decode':1}))
        parts=data['content'].strip().split()
        if len(parts)<2 or parts[0]!='ssh-ed25519':raise Problem('VM_HOST_KEY_UNCONFIRMED')
        return 'SHA256:'+base64.b64encode(hashlib.sha256(base64.b64decode(parts[1],validate=True)).digest()).decode().rstrip('=')
    def manifest(self,cfg,p):
        volumes=[]
        for key,value in cfg.items():
            if re.fullmatch(r'(?:scsi|sata|virtio|ide|unused)\d+',key) and isinstance(value,str):
                volume=value.split(',')[0]
                if ':' in volume and f'vm-{p["vmid"]}-' in volume:volumes.append(volume)
        return {'volumeIds':sorted(set(volumes)),'snippetIds':[],'ingressRuleIds':[f"allday-r{p['rentalId']}-s{p['serverId']}"]}
    def execute(self,operation,context,credential,guard):
        self.check_source(context);p=json.loads(operation['payload']);node=self.node(p['serverId']);name=node['node_name'];vmid=p['vmid'];kind=p['type'];prefix=f'/nodes/{name}/qemu/{vmid}';upid=None
        guard();closed=self.gateway.call('close',p,node)
        if closed.get('accessBlocked') is not True:raise Problem('ACCESS_BLOCK_UNCONFIRMED')
        if kind=='RETURN':
            cfg=self.owned(p,node,True) if vmid else None
            if cfg:
                upid=self.task(operation,'return-stop',name,'POST',prefix+'/status/stop',{},guard)
                upid=self.task(operation,'delete',name,'DELETE',prefix,{'purge':1,'destroy-unreferenced-disks':1},guard)
            absent=vmid is None or self.config_vm(name,vmid) is None
            manifest=context['resources']['manifest'] or {'volumeIds':[],'snippetIds':[],'ingressRuleIds':[]}
            volumes_absent=True
            for volume in manifest['volumeIds']:
                storage=volume.split(':')[0]
                contents=self.api.call('GET',f'/nodes/{name}/storage/{urllib.parse.quote(storage,safe="")}/content')
                volumes_absent=volumes_absent and all(item.get('volid')!=volume for item in contents)
            # This adapter does not create custom snippets; unexpected ones require operator recovery.
            snippets_absent=not manifest['snippetIds'];rules=self.gateway.call('check',p,node)
            ingress_absent=rules.get('accessBlocked') is True and not rules.get('ruleIds')
            flags={'ACCESS_BLOCKED':closed['accessBlocked'],'VM_ABSENT':absent,'VOLUMES_ABSENT':volumes_absent,'SNIPPETS_ABSENT':snippets_absent,'INGRESS_ABSENT':ingress_absent}
            items=[]
            for key,passed in flags.items():
                evidence={'checked':passed} if key=='ACCESS_BLOCKED' else {'vmid':vmid} if key=='VM_ABSENT' else {'checkedIds':manifest[{'VOLUMES_ABSENT':'volumeIds','SNIPPETS_ABSENT':'snippetIds','INGRESS_ABSENT':'ingressRuleIds'}[key]]}
                items.append({'kind':key,'passed':passed,'evidence':evidence})
            return {'simulation':False,'verification':items,'upid':upid}
        cfg=self.owned(p,node,True)
        if kind=='PREPARE' and cfg is None:
            self.check_clone_pool(node)
            template=context['template']
            if not template or not template['approvedAt'] or template['deprecatedAt']:raise Problem('APPROVED_TEMPLATE_REQUIRED')
            template_cfg=self.config_vm(name,template['templateVmid'])
            if not template_cfg or template_cfg.get('template')!=1:raise Problem('PVE_TEMPLATE_UNCONFIRMED')
            self.validate_pci_config(template_cfg,node)
            upid=self.task(operation,'clone',name,'POST',f'/nodes/{name}/qemu/{template["templateVmid"]}/clone',self.clone_parameters(p,node),guard)
            cfg=self.owned(p,node)
        if cfg is None:raise Problem('PVE_VM_ABSENT')
        if kind=='STOP':
            upid=self.task(operation,'stop',name,'POST',prefix+'/status/stop',{},guard)
            cfg=self.owned(p,node);devices=self.pci_devices(cfg)
            if devices:
                guard();self.api.call('PUT',prefix+'/config',{'delete':','.join(devices)})
            cfg=self.owned(p,node)
            status=self.api.call('GET',prefix+'/status/current');detached=not self.pci_devices(cfg)
            return {'simulation':False,'observation':{'vmid':vmid,'ip':p['ip'],'vmState':'STOPPED' if status['status']=='stopped' else 'UNKNOWN','healthPassed':status['status']=='stopped' and detached,'accessEnabled':False,'hostKeyFingerprint':None,'gpuDetached':detached},'upid':upid}
        if kind in ['PREPARE','START']:
            guard();network=ipaddress.ip_network(context['server']['vmSubnet']);ip=ipaddress.ip_address(p['ip']);gateway=ipaddress.ip_address(node['gateway'])
            if ip not in network or gateway not in network:raise Problem('VM_NETWORK_MISMATCH')
            settings={'ipconfig0':f'ip={ip}/{network.prefixlen},gw={gateway}','ciuser':'student','agent':'enabled=1'}
            self.validate_pci_config(cfg,node)
            if node.get('gpu_enabled',True):settings['hostpci0']='mapping='+node['gpu_mapping']+',pcie=1'
            if node.get('vm_bridge'):
                if not cfg.get('net0'):raise Problem('VM_NETWORK_INTERFACE_REQUIRED')
                settings['net0']=','.join([part for part in cfg['net0'].split(',') if not part.startswith('bridge=')]+['bridge='+node['vm_bridge']])
            self.api.call('PUT',prefix+'/config',settings)
            upid=self.task(operation,'start',name,'POST',prefix+'/status/start',{},guard)
        for _ in range(60):
            guard()
            try:self.api.call('POST',prefix+'/agent/ping',{});break
            except RemoteProblem:time.sleep(2)
        else:raise Problem('GUEST_AGENT_NOT_READY')
        result={'simulation':False,'upid':upid}
        if kind in ['PREPARE','RESET_CREDENTIAL']:
            guard();self.api.call('POST',prefix+'/agent/set-user-password',{'username':credential['username'],'password':credential['password'],'crypted':0});result['credential']={'credentialVersion':credential['credentialVersion'],'passed':True}
        if kind=='RESET_CREDENTIAL':return result
        key=self.fingerprint(name,vmid);cfg=self.owned(p,node);status=self.api.call('GET',prefix+'/status/current')
        ready=status['status']=='running' and self.gpu_ready(cfg,node)
        access=False
        if p['accessEnabled'] and ready:
            guard();opened=self.gateway.call('open',p,node);access=opened.get('accessEnabled') is True
            if not access:raise Problem('ACCESS_OPEN_UNCONFIRMED')
        result['manifest']=self.manifest(cfg,p);result['observation']={'vmid':vmid,'ip':p['ip'],'vmState':'RUNNING' if ready else 'UNKNOWN','healthPassed':ready,'accessEnabled':access,'hostKeyFingerprint':key,'gpuDetached':None}
        return result
    def server_status(self,server):
        try:
            self.check_source({'server':server})
            status=self.api.call('GET',f'/nodes/{server["nodeName"]}/status')
            return 'AVAILABLE' if status else 'UNKNOWN'
        except Problem:
            return 'UNKNOWN'
    def block_access(self,p):
        result=self.gateway.call('close',p,self.node(p['serverId']))
        if result.get('accessBlocked') is not True:raise Problem('ACCESS_BLOCK_UNCONFIRMED')
    def health(self,target):
        row=self.store.row("SELECT payload FROM operations WHERE server_id=? AND status='SUCCEEDED' ORDER BY created_at DESC LIMIT 1",target['serverId'])
        if not row:return None
        p=json.loads(row['payload'])
        if p['rentalId']!=target['rentalId'] or p.get('vmid')!=target['vmid']:return None
        node=self.node(p['serverId']);cfg=self.owned(p,node);prefix=f'/nodes/{node["node_name"]}/qemu/{p["vmid"]}'
        status=self.api.call('GET',prefix+'/status/current');running=status['status']=='running';healthy=not running
        if running:self.api.call('POST',prefix+'/agent/ping',{});healthy=self.gpu_ready(cfg,node)
        gateway=self.gateway.call('open' if running and healthy and target['accessAllowed'] else 'close',p,node)
        return {'vmState':'RUNNING' if running else 'STOPPED','healthPassed':healthy,'accessEnabled':running and healthy and target['accessAllowed'] and gateway.get('accessEnabled') is True,'hostKeyFingerprint':self.fingerprint(node['node_name'],p['vmid']) if running else None}
