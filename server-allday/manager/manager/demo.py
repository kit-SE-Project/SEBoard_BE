import hashlib, json
from .common import Problem

class DemoDriver:
    mode='DEMO'
    def __init__(self,store):self.store=store
    def check_source(self,context):
        if context['server'].get('inventorySource')!='DEMO':raise Problem('DEMO_CANNOT_TOUCH_REAL_INVENTORY',403)
    def read(self,vmid):return self.store.row('SELECT * FROM demo_vms WHERE vmid=?',vmid)
    def execute(self,operation,context,credential,guard):
        self.check_source(context);guard();p=json.loads(operation['payload']);vmid=p.get('vmid');kind=p['type'];rental=p['rentalId'];server=p['serverId'];manifest=context['resources']['manifest'] or {'volumeIds':[],'snippetIds':[],'ingressRuleIds':[]}
        vm=self.read(vmid) if vmid else None
        if vm and (vm['rental_id']!=rental or vm['server_id']!=server):raise Problem('VM_OWNER_MISMATCH')
        if kind=='RETURN':
            if vm:
                self.store.execute('DELETE FROM demo_resources WHERE vmid=?',vmid);self.store.execute('DELETE FROM demo_vms WHERE vmid=?',vmid)
            return {'simulation':True,'manifest':manifest,'verification':[{'kind':k,'passed':True,'evidence':{'simulation':True}|({'checked':True} if k=='ACCESS_BLOCKED' else {'vmid':vmid} if k=='VM_ABSENT' and vmid else {'checkedIds':manifest[field]} if (field:={'VOLUMES_ABSENT':'volumeIds','SNIPPETS_ABSENT':'snippetIds','INGRESS_ABSENT':'ingressRuleIds'}.get(k)) else {})} for k in ['ACCESS_BLOCKED','VM_ABSENT','VOLUMES_ABSENT','SNIPPETS_ABSENT','INGRESS_ABSENT']]}
        if vmid is None:raise Problem('ALLOCATION_REQUIRED')
        if kind=='PREPARE':
            if not vm:self.store.execute('INSERT INTO demo_vms(vmid,rental_id,server_id,state,access_enabled,ip,gpu_attached,host_key) VALUES (?,?,?,\'RUNNING\',0,?,1,?)',vmid,rental,server,p['ip'],'SHA256:SIMULATED-'+str(vmid))
            for resource in ['DEMO-volume-'+str(vmid),'DEMO-snippet-'+str(vmid),'DEMO-ingress-'+str(vmid)]:self.store.execute('INSERT OR IGNORE INTO demo_resources VALUES (?,?)',resource,vmid)
        elif not vm:raise Problem('VM_ABSENT')
        result={'simulation':True}
        if kind in ['PREPARE','RESET_CREDENTIAL']:
            if not credential:raise Problem('CREDENTIAL_REQUIRED')
            self.store.execute('UPDATE demo_vms SET credential_version=?,credential_hash=? WHERE vmid=?',credential['credentialVersion'],hashlib.sha256(credential['password'].encode()).hexdigest(),vmid)
            result['credential']={'credentialVersion':credential['credentialVersion'],'passed':True}
        if kind=='RESET_CREDENTIAL':return result
        stop=kind=='STOP';state='STOPPED' if stop else 'RUNNING';access=bool(p['accessEnabled']) and not stop
        self.store.execute('UPDATE demo_vms SET state=?,access_enabled=?,gpu_attached=? WHERE vmid=?',state,int(access),int(not stop),vmid)
        vm=self.read(vmid)
        result['manifest']={'volumeIds':['DEMO-volume-'+str(vmid)],'snippetIds':['DEMO-snippet-'+str(vmid)],'ingressRuleIds':['DEMO-ingress-'+str(vmid)]}
        result['observation']={'vmid':vmid,'ip':vm['ip'],'vmState':vm['state'],'healthPassed':True,'accessEnabled':bool(vm['access_enabled']),'hostKeyFingerprint':vm['host_key'],'gpuDetached':stop}
        return result
    def health(self,target):
        vm=self.read(target['vmid'])
        if not vm:return None
        if vm['rental_id']!=target['rentalId'] or vm['server_id']!=target['serverId']:raise Problem('VM_OWNER_MISMATCH')
        # Close access as soon as the authoritative backend no longer permits it.
        access=bool(target['accessAllowed']) and vm['state']=='RUNNING'
        self.store.execute('UPDATE demo_vms SET access_enabled=? WHERE vmid=?',int(access),vm['vmid'])
        return {'vmState':vm['state'],'healthPassed':True,'accessEnabled':access,'hostKeyFingerprint':vm['host_key']}
    def block_access(self,p):
        self.store.execute('UPDATE demo_vms SET access_enabled=0 WHERE rental_id=? AND server_id=?',p['rentalId'],p['serverId'])
