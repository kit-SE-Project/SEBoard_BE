"""Narrow, read-only evidence collection for incomplete RETURN operations."""
import json
import re
import uuid
from datetime import datetime
from urllib.parse import quote

from .common import Problem,now
from .proxmox import PveClient
from .helper_client import HelperTransport
from .pve_tasks import TASK_POLICY, blocking_tasks, blocking_task_count


def require(condition,code):
    if not condition:raise Problem(code)


def inspect_incomplete_return(store,config,identifier,expected_command,backend,api=None,transport=None):
    operation=store.row('SELECT * FROM operations WHERE id=?',identifier)
    require(operation is not None,'NOT_FOUND')
    p=json.loads(operation['payload'])
    require(p.get('commandId')==expected_command,'RECOVERY_COMMAND_MISMATCH')
    require(config.get('mode')=='PROXMOX' and config.get('gateway_protocol')=='NODE_HELPER_V02','RECOVERY_REAL_HELPER_REQUIRED')
    require(p.get('type')=='RETURN' and operation['status']=='UNKNOWN' and operation['reason']=='PVE_HTTP_ERROR','RECOVERY_NOT_SUPPORTED')
    require(p['serverId'] in config.get('server_ids',[]) and type(p.get('vmid')) is int,'RECOVERY_ASSIGNMENT_MISMATCH')
    node=config['nodes'][str(p['serverId'])];name=node['node_name'];prefix=f'/nodes/{name}/qemu/{p["vmid"]}'
    require(not store.row("SELECT * FROM effects WHERE operation_id=? AND step='delete'",identifier),'RECOVERY_DELETE_ALREADY_SUBMITTED')
    stop=store.row("SELECT * FROM effects WHERE operation_id=? AND step='return-stop'",identifier)
    require(stop is not None and stop['status']=='SUCCEEDED','RECOVERY_STOP_UNCONFIRMED')
    job=store.row('SELECT * FROM jobs WHERE job_id=?',p['jobId'])
    require(job is not None and job['stage']=='BLOCKED' and job['operation_id']==identifier and job['command_id']==expected_command,'RECOVERY_JOB_CHANGED')
    wf=store.row('SELECT * FROM helper_workflows WHERE operation_id=?',identifier)
    require(wf is not None and wf['server_id']==p['serverId'] and wf['rental_id']==p['rentalId'],'RECOVERY_HELPER_WORKFLOW_MISSING')
    receipt=store.row("SELECT response FROM helper_steps WHERE operation_id=? AND step='initial-close'",identifier)
    close=json.loads(receipt['response']) if receipt and receipt['response'] else {}
    require(close.get('enabled') is False and close.get('oldConnectionsCleared') is True,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    try:
        backend.query(f'/api/internal/jobs/{p["jobId"]}/context',workerId=config['worker_id'],commandId=expected_command)
    except Problem as error:
        require(error.code=='WORKER_LEASE_INVALID' and error.status==409,'RECOVERY_OLD_LEASE_UNCONFIRMED')
    else:raise Problem('RECOVERY_OLD_LEASE_STILL_VALID')
    api=api or PveClient(config['proxmox'])
    transport=transport or HelperTransport(node['helper'])
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    task=api.call('GET',f'/nodes/{name}/tasks/{quote(stop["upid"],safe="")}/status')
    require(task.get('status')=='stopped' and task.get('exitstatus')=='OK','RECOVERY_STOP_UNCONFIRMED')
    history=api.call('GET',f'/nodes/{name}/tasks?source=all&vmid={p["vmid"]}&limit=1000')
    require(isinstance(history,list) and len(history)<1000,'RECOVERY_TASK_HISTORY_INCOMPLETE')
    # Deliberately conservative: any delete task for this VMID requires a different recovery procedure.
    require(not any(t.get('type')=='qmdestroy' for t in history),'RECOVERY_DELETE_TASK_EXISTS')
    vm=api.call('GET',prefix+'/config')
    require(vm.get('description')==f'Server Allday rental={p["rentalId"]} server={p["serverId"]}' and not vm.get('lock'),'RECOVERY_VM_OWNER_MISMATCH')
    require(api.call('GET',prefix+'/status/current').get('status')=='stopped','RECOVERY_VM_NOT_STOPPED')
    actual=sorted(v.split(',')[0] for k,v in vm.items() if re.fullmatch(r'(?:scsi|sata|virtio|ide|unused)\d+',k) and isinstance(v,str) and ':' in v.split(',')[0])
    require(bool(actual) and all(f'vm-{p["vmid"]}-' in v for v in actual),'RECOVERY_VOLUME_OWNER_MISMATCH')
    recorded=[]
    for row in store.rows("SELECT payload,result FROM operations WHERE server_id=? AND status='SUCCEEDED' ORDER BY created_at",p['serverId']):
        previous=json.loads(row['payload'])
        if previous.get('rentalId')==p['rentalId'] and previous.get('vmid')==p['vmid']:
            manifest=(json.loads(row['result']) if row['result'] else {}).get('manifest',{})
            if manifest.get('volumeIds'):recorded.append(sorted(manifest['volumeIds']))
    require(bool(recorded) and all(volumes==actual for volumes in recorded),'RECOVERY_MANIFEST_MISMATCH')
    req={'schemaVersion':1,'commandId':'recovery-read-'+str(uuid.uuid4()),'serverId':p['serverId'],'operation':'READ_STATUS','payload':{}}
    response=transport(req)
    require(response.get('ok') is True and response.get('mode')=='system' and all(response.get(k)==req[k] for k in ['commandId','serverId','operation']),'RECOVERY_HELPER_UNCONFIRMED')
    helper=response['result'];ingress=helper.get('ingress',{})
    require(helper.get('ownerRentalId')==p['rentalId'] and helper.get('generation')==wf['current_generation'] and helper.get('controlRevision')==wf['control_revision'],'RECOVERY_HELPER_OWNER_CHANGED')
    require(helper.get('accessEnabled') is False and helper.get('needsRecovery') is False and helper.get('pendingCleanup')==[] and blocking_task_count(helper)==0,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    require(ingress.get('target') is None and ingress.get('mappingCount')==0 and ingress.get('leaseValid') is False,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    return {'operationId':identifier,'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],
            'recoveryKind':'PRE_DELETE_INCOMPLETE',
            'checkedAt':now(),'vmStopped':True,'ownerMatches':True,'disksMatch':True,'noBlockingTasks':True,'taskPolicy':TASK_POLICY,
            'noDeleteTask':True,'accessBlocked':True,'oldLeaseInvalid':True,'volumeIds':actual,
            'helperGeneration':helper['generation'],'helperControlRevision':helper['controlRevision'],
            'backendStateSource':'Operator confirmed FAILED; old command lease independently rejected',
            'resolution':'FAILED_INCOMPLETE_RETURN_NOT_SUCCESS'}


def inspect_uncloned_prepare(store,config,identifier,expected_command,backend,api=None,transport=None):
    """Failed pool-less clone request with no VM: preserve registration and snippets."""
    operation=store.row('SELECT * FROM operations WHERE id=?',identifier)
    require(operation is not None,'NOT_FOUND');p=json.loads(operation['payload'])
    require(p.get('commandId')==expected_command,'RECOVERY_COMMAND_MISMATCH')
    require(config.get('mode')=='PROXMOX' and config.get('gateway_protocol')=='NODE_HELPER_V02','RECOVERY_REAL_HELPER_REQUIRED')
    require(p.get('type')=='PREPARE' and operation['status']=='UNKNOWN' and operation['reason']=='PVE_HTTP_ERROR','RECOVERY_NOT_SUPPORTED')
    require(p['serverId'] in config.get('server_ids',[]) and type(p.get('vmid')) is int,'RECOVERY_ASSIGNMENT_MISMATCH')
    require(not store.row('SELECT * FROM effects WHERE operation_id=?',identifier),'RECOVERY_PHYSICAL_EFFECT_EXISTS')
    require(not store.row("SELECT * FROM helper_steps WHERE operation_id=? AND step IN ('register-volumes','claim')",identifier),'RECOVERY_PREPARE_PHASE_CHANGED')
    job=store.row('SELECT * FROM jobs WHERE job_id=?',p['jobId'])
    require(job is not None and job['stage']=='BLOCKED' and job['operation_id']==identifier and job['command_id']==expected_command,'RECOVERY_JOB_CHANGED')
    wf=store.row('SELECT * FROM helper_workflows WHERE operation_id=?',identifier)
    require(wf is not None and wf['server_id']==p['serverId'] and wf['rental_id']==p['rentalId'] and wf['base_generation']==wf['current_generation'],'RECOVERY_HELPER_WORKFLOW_MISSING')
    register=store.row("SELECT request,response FROM helper_steps WHERE operation_id=? AND step='register-empty'",identifier)
    request=json.loads(register['request']) if register else {};registered=json.loads(register['response']) if register and register['response'] else {}
    wanted={'rentalId':p['rentalId'],'vmId':p['vmid'],'vmIp':p['ip'],'volumeIds':[]}
    require(request.get('operation')=='REGISTER_RESOURCE' and request.get('payload')==wanted and all(registered.get(k)==wanted[k] for k in ['rentalId','vmId','vmIp']),'RECOVERY_REGISTRATION_MISMATCH')
    snippets=store.row("SELECT response FROM helper_steps WHERE operation_id=? AND step='snippets'",identifier)
    response=json.loads(snippets['response']) if snippets and snippets['response'] else {};refs=response.get('references',{})
    require(response.get('verified') is True and response.get('rentalId')==p['rentalId'] and set(refs)=={'user','network','meta'},'RECOVERY_SNIPPETS_UNCONFIRMED')
    require(all(isinstance(refs[k],str) and re.fullmatch(r'[A-Za-z0-9_-]+:snippets/allday-r'+str(p['rentalId'])+'-'+k+r'\.yaml',refs[k]) for k in refs),'RECOVERY_SNIPPETS_UNCONFIRMED')
    closed=store.row("SELECT response FROM helper_steps WHERE operation_id=? AND step='initial-close'",identifier)
    receipt=json.loads(closed['response']) if closed and closed['response'] else {}
    require(receipt.get('enabled') is False and receipt.get('oldConnectionsCleared') is True,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    try:
        backend.query(f'/api/internal/jobs/{p["jobId"]}/context',workerId=config['worker_id'],commandId=expected_command)
    except Problem as error:
        require(error.code=='WORKER_LEASE_INVALID' and error.status==409,'RECOVERY_OLD_LEASE_UNCONFIRMED')
    else:raise Problem('RECOVERY_OLD_LEASE_STILL_VALID')
    node=config['nodes'][str(p['serverId'])];name=node['node_name'];pool=node.get('vm_pool')
    require(isinstance(pool,str) and re.fullmatch(r'[A-Za-z0-9_.-]{1,64}',pool),'RECOVERY_POOL_REQUIRED')
    api=api or PveClient(config['proxmox']);transport=transport or HelperTransport(node['helper'])
    permissions=api.call('GET','/access/permissions?path='+quote('/pool/'+pool,safe='')).get('/pool/'+pool,{})
    require(permissions.get('VM.Allocate')==1 and permissions.get('Pool.Audit')==1,'RECOVERY_POOL_NOT_READY')
    pool_info=api.call('GET','/pools/'+pool)
    require(pool_info.get('poolid')==pool and pool_info.get('members')==[],'RECOVERY_POOL_NOT_EMPTY')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    since=int(datetime.fromisoformat(operation['created_at']).timestamp())-120
    history=api.call('GET',f'/nodes/{name}/tasks?source=all&since={since}&limit=1000')
    require(isinstance(history,list) and len(history)<1000,'RECOVERY_TASK_HISTORY_INCOMPLETE')
    # Clone tasks are indexed by source VM, not necessarily by the new VMID.
    require(not any(t.get('type')=='qmclone' or t.get('type') in {'qmcreate','qmrestore'} and str(t.get('id'))==str(p['vmid']) for t in history),'RECOVERY_CLONE_TASK_EXISTS')
    require(not any(int(v['vmid'])==p['vmid'] for v in api.call('GET',f'/nodes/{name}/qemu')),'RECOVERY_VM_STILL_PRESENT')
    storages=api.call('GET',f'/nodes/{name}/storage')
    require(isinstance(storages,list) and bool(storages) and all(s.get('active')==1 for s in storages),'RECOVERY_STORAGE_UNCONFIRMED')
    checked=[]
    for storage in storages:
        sid=storage['storage'];content=api.call('GET',f'/nodes/{name}/storage/{quote(sid,safe="")}/content')
        require(isinstance(content,list),'RECOVERY_STORAGE_UNCONFIRMED')
        require(not any(str(v.get('vmid'))==str(p['vmid']) or f'vm-{p["vmid"]}-' in v.get('volid','') for v in content),'RECOVERY_VOLUMES_STILL_PRESENT')
        checked.append(sid)
    def read_helper(operation,payload):
        query={'schemaVersion':1,'commandId':'prepare-recovery-'+str(uuid.uuid4()),'serverId':p['serverId'],'operation':operation,'payload':payload}
        reply=transport(query)
        require(reply.get('ok') is True and reply.get('mode')=='system' and all(reply.get(k)==query[k] for k in ['commandId','serverId','operation']),'RECOVERY_HELPER_UNCONFIRMED')
        return reply['result']
    helper=read_helper('READ_STATUS',{});ingress=helper.get('ingress',{})
    require(helper.get('ownerRentalId') is None and helper.get('generation')==wf['current_generation'] and helper.get('controlRevision')==wf['control_revision'],'RECOVERY_HELPER_OWNER_CHANGED')
    require(helper.get('accessEnabled') is False and helper.get('needsRecovery') is False and helper.get('pendingCleanup')==[] and blocking_task_count(helper)==0,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    require(ingress.get('target') is None and ingress.get('mappingCount')==0 and ingress.get('leaseValid') is False,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    verification=read_helper('VERIFY_CLEANUP',{'rentalId':p['rentalId']})
    require(verification.get('rentalId')==p['rentalId'],'RECOVERY_REGISTRATION_MISMATCH')
    checks={c.get('kind'):c for c in verification.get('checks',[])}
    needed={'VM','TASKS','INGRESS','CONNECTIONS','LOCAL_STATE'}|{'VOLUMES:'+s for s in checked}
    require(all(checks.get(k,{}).get('status')=='PASS' for k in needed),'RECOVERY_PHYSICAL_STATE_UNCONFIRMED')
    expected_files=sorted(v.rsplit('/',1)[-1] for v in refs.values())
    require(verification.get('result')=='FAIL' and checks.get('SNIPPETS',{}).get('status')=='FAIL' and sorted(checks['SNIPPETS'].get('remaining',[]))==expected_files,'RECOVERY_SNIPPETS_UNCONFIRMED')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    return {'operationId':identifier,'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],
            'recoveryKind':'PREPARE_BEFORE_CLONE_INCOMPLETE','checkedAt':now(),'vmAbsent':True,'volumesAbsent':True,
            'noCloneTask':True,'noBlockingTasks':True,'taskPolicy':TASK_POLICY,'accessBlocked':True,'oldLeaseInvalid':True,'helperUnclaimed':True,
            'snippetsPreserved':True,'poolReady':True,'registrationMatches':True,'pool':pool,'checkedStorages':checked,
            'snippetIds':sorted(refs.values()),'helperGeneration':helper['generation'],'helperControlRevision':helper['controlRevision'],
            'backendStateSource':'Old command lease independently rejected; backend retry must recheck rental state/version',
            'resolution':'FAILED_INCOMPLETE_PREPARE_NOT_SUCCESS'}


def inspect_started_prepare(store,config,identifier,expected_command,backend,api=None,transport=None):
    """A known cloned VM after backend transport loss; retain every resource for retry."""
    operation=store.row('SELECT * FROM operations WHERE id=?',identifier)
    require(operation is not None,'NOT_FOUND');p=json.loads(operation['payload'])
    require(p.get('commandId')==expected_command,'RECOVERY_COMMAND_MISMATCH')
    require(config.get('mode')=='PROXMOX' and config.get('gateway_protocol')=='NODE_HELPER_V02','RECOVERY_REAL_HELPER_REQUIRED')
    require(p.get('type')=='PREPARE' and operation['status']=='UNKNOWN' and operation['reason']=='TRANSPORT_UNCERTAIN','RECOVERY_NOT_SUPPORTED')
    require(p['serverId'] in config.get('server_ids',[]) and type(p.get('vmid')) is int,'RECOVERY_ASSIGNMENT_MISMATCH')
    job=store.row('SELECT * FROM jobs WHERE job_id=?',p['jobId'])
    require(job is not None and job['stage']=='BLOCKED' and job['operation_id']==identifier and job['command_id']==expected_command,'RECOVERY_JOB_CHANGED')
    wf=store.row('SELECT * FROM helper_workflows WHERE operation_id=?',identifier)
    require(wf is not None and wf['server_id']==p['serverId'] and wf['rental_id']==p['rentalId'] and wf['current_generation']==wf['base_generation']+1,'RECOVERY_HELPER_WORKFLOW_MISSING')
    rows={r['step']:r for r in store.rows('SELECT step,request,response FROM helper_steps WHERE operation_id=?',identifier)}
    def receipt(step):
        row=rows.get(step);require(row is not None and row['response'] is not None,'RECOVERY_HELPER_RECEIPT_MISMATCH')
        return json.loads(row['request']),json.loads(row['response'])
    for step,kind in [('initial-close','DISABLE_INGRESS'),('register-empty','REGISTER_RESOURCE'),('register-volumes','REGISTER_RESOURCE'),('snippets','PUT_SNIPPET'),('claim','ADVANCE_GENERATION')]:
        req,res=receipt(step);require(req.get('operation')==kind,'RECOVERY_HELPER_RECEIPT_MISMATCH')
    require(all(json.loads(r['request']).get('operation') in {'DISABLE_INGRESS','REGISTER_RESOURCE','PUT_SNIPPET','ADVANCE_GENERATION'} for r in rows.values()),'RECOVERY_PREPARE_PHASE_CHANGED')
    _,closed=receipt('initial-close');require(closed.get('enabled') is False and closed.get('oldConnectionsCleared') is True,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    req,claimed=receipt('claim')
    require(req.get('payload')=={'rentalId':p['rentalId'],'expectedGeneration':wf['base_generation']} and claimed.get('generation')==wf['current_generation'] and claimed.get('ownerRentalId')==p['rentalId'] and claimed.get('stopVerified') is True,'RECOVERY_HELPER_OWNER_CHANGED')
    reg,registered=receipt('register-volumes');resource=reg.get('payload',{});volumes=resource.get('volumeIds',[])
    require(all(resource.get(k)==v and registered.get(k)==v for k,v in {'rentalId':p['rentalId'],'vmId':p['vmid'],'vmIp':p['ip']}.items()) and isinstance(volumes,list) and bool(volumes) and len(set(volumes))==len(volumes) and all(isinstance(v,str) and f'vm-{p["vmid"]}-' in v for v in volumes),'RECOVERY_REGISTRATION_MISMATCH')
    _,snippets=receipt('snippets');refs=snippets.get('references',{})
    require(snippets.get('rentalId')==p['rentalId'] and snippets.get('verified') is True and set(refs)=={'user','network','meta'} and all(isinstance(v,str) and re.fullmatch(r'[A-Za-z0-9_-]+:snippets/allday-r'+str(p['rentalId'])+'-'+k+r'\.yaml',v) for k,v in refs.items()),'RECOVERY_SNIPPETS_UNCONFIRMED')
    try:backend.query(f'/api/internal/jobs/{p["jobId"]}/context',workerId=config['worker_id'],commandId=expected_command)
    except Problem as error:require(error.code=='WORKER_LEASE_INVALID' and error.status==409,'RECOVERY_OLD_LEASE_UNCONFIRMED')
    else:raise Problem('RECOVERY_OLD_LEASE_STILL_VALID')
    node=config['nodes'][str(p['serverId'])];name=node['node_name'];prefix=f'/nodes/{name}/qemu/{p["vmid"]}'
    api=api or PveClient(config['proxmox']);transport=transport or HelperTransport(node['helper'])
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    effects=store.rows('SELECT * FROM effects WHERE operation_id=?',identifier)
    require({e['step'] for e in effects}=={'clone','start'} and all(e['status']=='SUCCEEDED' for e in effects),'RECOVERY_TASKS_UNCONFIRMED')
    for e in effects:
        kind='qmclone' if e['step']=='clone' else 'qmstart'
        task=api.call('GET',f'/nodes/{name}/tasks/{quote(e["upid"],safe="")}/status')
        require(task.get('status')=='stopped' and task.get('exitstatus')=='OK' and task.get('type')==kind,'RECOVERY_TASKS_UNCONFIRMED')
        if kind=='qmstart':require(str(task.get('id'))==str(p['vmid']),'RECOVERY_TASKS_UNCONFIRMED')
    vm=api.call('GET',prefix+'/config');state=api.call('GET',prefix+'/status/current').get('status')
    require(vm.get('description')==f'Server Allday rental={p["rentalId"]} server={p["serverId"]}' and not vm.get('lock') and not vm.get('template') and state in {'running','stopped'},'RECOVERY_VM_OWNER_MISMATCH')
    actual=sorted(v.split(',')[0] for k,v in vm.items() if re.fullmatch(r'(?:scsi|sata|virtio|ide|unused)\d+',k) and isinstance(v,str) and ':' in v.split(',')[0])
    require(actual==sorted(volumes),'RECOVERY_MANIFEST_MISMATCH')
    require(sorted(v.split('=',1)[1] for v in vm.get('cicustom','').split(',') if '=' in v)==sorted(refs.values()),'RECOVERY_SNIPPETS_UNCONFIRMED')
    for sid in sorted({v.split(':')[0] for v in volumes}):
        content=api.call('GET',f'/nodes/{name}/storage/{quote(sid,safe="")}/content')
        require(sorted(v.get('volid') for v in content if f'vm-{p["vmid"]}-' in v.get('volid',''))==sorted(v for v in volumes if v.startswith(sid+':')),'RECOVERY_MANIFEST_MISMATCH')
    def read_helper(kind,payload):
        req={'schemaVersion':1,'commandId':'prepare-started-recovery-'+str(uuid.uuid4()),'serverId':p['serverId'],'operation':kind,'payload':payload};reply=transport(req)
        require(reply.get('schemaVersion')==1 and reply.get('ok') is True and reply.get('mode')=='system' and all(reply.get(k)==req[k] for k in ['commandId','serverId','operation']),'RECOVERY_HELPER_UNCONFIRMED')
        return reply['result']
    helper=read_helper('READ_STATUS',{});ingress=helper.get('ingress',{})
    require(helper.get('ownerRentalId')==p['rentalId'] and helper.get('generation')==wf['current_generation'] and helper.get('controlRevision')==wf['control_revision'],'RECOVERY_HELPER_OWNER_CHANGED')
    require(helper.get('accessEnabled') is False and helper.get('needsRecovery') is False and helper.get('pendingCleanup')==[] and blocking_task_count(helper)==0 and ingress.get('target') is None and ingress.get('mappingCount')==0 and ingress.get('leaseValid') is False,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    ver=read_helper('VERIFY_CLEANUP',{'rentalId':p['rentalId']});checks={x.get('kind'):x for x in ver.get('checks',[])}
    require(ver.get('rentalId')==p['rentalId'] and ver.get('result')=='FAIL' and checks.get('VM',{}).get('status')=='FAIL' and checks['VM'].get('remaining')==[p['vmid']],'RECOVERY_REGISTRATION_MISMATCH')
    require(all(checks.get(k,{}).get('status')=='PASS' for k in ['TASKS','INGRESS','CONNECTIONS','LOCAL_STATE']),'RECOVERY_PHYSICAL_STATE_UNCONFIRMED')
    require(checks.get('SNIPPETS',{}).get('status')=='FAIL' and sorted(checks['SNIPPETS'].get('remaining',[]))==sorted(v.rsplit('/',1)[-1] for v in refs.values()),'RECOVERY_SNIPPETS_UNCONFIRMED')
    registered=sorted(v for k,x in checks.items() if isinstance(k,str) and k.startswith('VOLUMES:') for v in x.get('remaining',[]))
    require(registered==sorted(volumes) and all(x.get('status') in {'PASS','FAIL'} for k,x in checks.items() if isinstance(k,str) and k.startswith('VOLUMES:')),'RECOVERY_MANIFEST_MISMATCH')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    return {'operationId':identifier,'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],
            'recoveryKind':'PREPARE_STARTED_TRANSPORT_INCOMPLETE','checkedAt':now(),'vmOwned':True,'disksMatch':True,'tasksFinished':True,
            'noBlockingTasks':True,'taskPolicy':TASK_POLICY,'accessBlocked':True,'oldLeaseInvalid':True,'helperOwnerMatches':True,'registrationMatches':True,'snippetsPreserved':True,
            'vmState':state,'volumeIds':sorted(volumes),'snippetIds':sorted(refs.values()),'taskUpids':{e['step']:e['upid'] for e in effects},
            'helperGeneration':helper['generation'],'helperControlRevision':helper['controlRevision'],'resolution':'FAILED_INCOMPLETE_PREPARE_NOT_SUCCESS',
            'backendStateSource':'Old lease rejected; backend must recheck FAILED and authorize retry without releasing ownership'}


def inspect_start_for_return(store,config,identifier,expected_command,return_job_id,backend,api=None,transport=None,before_generation=False):
    """Resolve a failed START only, retaining its owned VM for an already requested RETURN."""
    operation=store.row('SELECT * FROM operations WHERE id=?',identifier)
    require(operation is not None,'NOT_FOUND');p=json.loads(operation['payload'])
    require(p.get('commandId')==expected_command,'RECOVERY_COMMAND_MISMATCH')
    require(config.get('mode')=='PROXMOX' and config.get('gateway_protocol')=='NODE_HELPER_V02','RECOVERY_REAL_HELPER_REQUIRED')
    expected_reason='HELPER_TASK_IN_PROGRESS' if before_generation else 'GUEST_IP_UNCONFIRMED'
    require(p.get('type')=='START' and operation['status']=='UNKNOWN' and operation['reason']==expected_reason,'RECOVERY_NOT_SUPPORTED')
    require(p['serverId'] in config.get('server_ids',[]) and type(p.get('vmid')) is int,'RECOVERY_ASSIGNMENT_MISMATCH')
    job=store.row('SELECT * FROM jobs WHERE job_id=?',p['jobId'])
    require(job is not None and job['stage']=='BLOCKED' and job['operation_id']==identifier and job['command_id']==expected_command,'RECOVERY_JOB_CHANGED')
    pending=store.row('SELECT * FROM jobs WHERE job_id=?',return_job_id)
    claim=json.loads(pending['payload']) if pending else {}
    stages={'BLOCKED','DEFERRED'} if before_generation else {'BLOCKED'}
    require(pending is not None and pending['stage'] in stages and pending['operation_id'] is None and pending['command_id']==claim.get('commandId') and all(claim.get(k)==v for k,v in {'id':return_job_id,'type':'RETURN','rentalId':p['rentalId'],'serverId':p['serverId']}.items()) and type(claim.get('desiredRevision')) is int and claim['desiredRevision']>p['desiredRevision'],'RECOVERY_RETURN_INTENT_CHANGED')
    if before_generation:
        authoritative=next((j for j in backend.call('GET','/api/internal/jobs') if j.get('id')==return_job_id),None)
        require(authoritative is not None and all(authoritative.get(k)==claim.get(k) for k in ['id','commandId','type','rentalId','serverId','desiredRevision']) and authoritative.get('status')=='PENDING','RECOVERY_RETURN_INTENT_CHANGED')
    wf=store.row('SELECT * FROM helper_workflows WHERE operation_id=?',identifier)
    require(wf is not None and wf['server_id']==p['serverId'] and wf['rental_id']==p['rentalId'] and wf['current_generation']==wf['base_generation']+(0 if before_generation else 1),'RECOVERY_HELPER_WORKFLOW_MISSING')
    for step,kind in [('initial-close','DISABLE_INGRESS'),('claim','ADVANCE_GENERATION')]:
        row=store.row('SELECT request,response FROM helper_steps WHERE operation_id=? AND step=?',identifier,step)
        request=json.loads(row['request']) if row else {};receipt=json.loads(row['response']) if row and row['response'] else {}
        require(request.get('operation')==kind,'RECOVERY_HELPER_RECEIPT_MISMATCH')
        if step=='initial-close':require(receipt.get('enabled') is False and receipt.get('oldConnectionsCleared') is True,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
        elif before_generation:
            require(request.get('payload')=={'rentalId':p['rentalId'],'expectedGeneration':wf['base_generation']} and request.get('generation')==wf['base_generation']+1 and request.get('controlRevision')==wf['control_revision'] and row['response'] is None,'RECOVERY_HELPER_OWNER_CHANGED')
        else:require(request.get('payload')=={'rentalId':p['rentalId'],'expectedGeneration':wf['base_generation']} and receipt.get('generation')==wf['current_generation'] and receipt.get('ownerRentalId')==p['rentalId'] and receipt.get('stopVerified') is True,'RECOVERY_HELPER_OWNER_CHANGED')
    for row in store.rows('SELECT request FROM helper_steps WHERE operation_id=?',identifier):
        require(json.loads(row['request']).get('operation') in {'DISABLE_INGRESS','ADVANCE_GENERATION'},'RECOVERY_START_PHASE_CHANGED')
    for jid,command in [(p['jobId'],expected_command),(return_job_id,claim['commandId'])]:
        try:backend.query(f'/api/internal/jobs/{jid}/context',workerId=config['worker_id'],commandId=command)
        except Problem as error:require(error.code=='WORKER_LEASE_INVALID' and error.status==409,'RECOVERY_OLD_LEASE_UNCONFIRMED')
        else:raise Problem('RECOVERY_OLD_LEASE_STILL_VALID')
    node=config['nodes'][str(p['serverId'])];name=node['node_name'];prefix=f'/nodes/{name}/qemu/{p["vmid"]}'
    api=api or PveClient(config['proxmox']);transport=transport or HelperTransport(node['helper'])
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    effects=store.rows('SELECT * FROM effects WHERE operation_id=?',identifier)
    require({e['step'] for e in effects}==({'quiesce'} if before_generation else {'quiesce','start'}) and all(e['status']=='SUCCEEDED' for e in effects),'RECOVERY_TASKS_UNCONFIRMED')
    for effect in effects:
        kind=('qmshutdown' if before_generation or ':qmshutdown:' in effect['upid'] else 'qmstop') if effect['step']=='quiesce' else 'qmstart'
        require(f':{kind}:{p["vmid"]}:' in effect['upid'],'RECOVERY_TASKS_UNCONFIRMED')
        task=api.call('GET',f'/nodes/{name}/tasks/{quote(effect["upid"],safe="")}/status')
        require(task.get('status')=='stopped' and task.get('exitstatus')=='OK' and task.get('type')==kind and str(task.get('id'))==str(p['vmid']),'RECOVERY_TASKS_UNCONFIRMED')
    vm=api.call('GET',prefix+'/config');state=api.call('GET',prefix+'/status/current').get('status')
    require(vm.get('description')==f'Server Allday rental={p["rentalId"]} server={p["serverId"]}' and not vm.get('lock') and not vm.get('template') and state in {'running','stopped'},'RECOVERY_VM_OWNER_MISMATCH')
    if before_generation:
        require(state=='stopped','RECOVERY_STOP_UNCONFIRMED')
        history=api.call('GET',f'/nodes/{name}/tasks?source=all&vmid={p["vmid"]}&limit=1000')
        require(isinstance(history,list) and len(history)<1000,'RECOVERY_TASK_HISTORY_INCOMPLETE')
        since=datetime.fromisoformat(operation['created_at']).timestamp()
        require(not any(t.get('type') in {'qmstart','qmresume','qmreset','qmdestroy'} and t.get('starttime',0)>=since for t in history),'RECOVERY_START_PHASE_CHANGED')
    volumes=sorted(v.split(',')[0] for k,v in vm.items() if re.fullmatch(r'(?:scsi|sata|virtio|ide|unused)\d+',k) and isinstance(v,str) and ':' in v.split(',')[0])
    require(bool(volumes) and all(f'vm-{p["vmid"]}-' in v for v in volumes),'RECOVERY_VOLUME_OWNER_MISMATCH')
    manifests=[]
    for row in store.rows("SELECT payload,result FROM operations WHERE server_id=? AND status='SUCCEEDED' ORDER BY created_at",p['serverId']):
        prior=json.loads(row['payload']);result=json.loads(row['result']) if row['result'] else {}
        if prior.get('type')=='PREPARE' and all(prior.get(k)==p[k] for k in ['rentalId','vmid','ip','desiredRevision']):manifests.append(result.get('manifest',{}))
    require(bool(manifests) and all(sorted(m.get('volumeIds',[]))==volumes for m in manifests),'RECOVERY_MANIFEST_MISMATCH')
    snippet_ids=sorted(manifests[-1].get('snippetIds',[]))
    require(len(snippet_ids)==3 and {v.rsplit('/',1)[-1] for v in snippet_ids}=={f'allday-r{p["rentalId"]}-{k}.yaml' for k in ['user','network','meta']},'RECOVERY_SNIPPETS_UNCONFIRMED')
    require(sorted(v.split('=',1)[1] for v in vm.get('cicustom','').split(',') if '=' in v)==snippet_ids,'RECOVERY_SNIPPETS_UNCONFIRMED')
    def read_helper(kind,payload):
        req={'schemaVersion':1,'commandId':'start-recovery-'+str(uuid.uuid4()),'serverId':p['serverId'],'operation':kind,'payload':payload};reply=transport(req)
        require(reply.get('schemaVersion')==1 and reply.get('ok') is True and reply.get('mode')=='system' and all(reply.get(k)==req[k] for k in ['commandId','serverId','operation']),'RECOVERY_HELPER_UNCONFIRMED')
        return reply['result']
    helper=read_helper('READ_STATUS',{});ingress=helper.get('ingress',{})
    require(helper.get('ownerRentalId')==p['rentalId'] and helper.get('generation')==wf['current_generation'] and helper.get('controlRevision')==wf['control_revision'],'RECOVERY_HELPER_OWNER_CHANGED')
    require(helper.get('accessEnabled') is False and helper.get('needsRecovery') is False and helper.get('pendingCleanup')==[] and blocking_task_count(helper)==0 and ingress.get('target') is None and ingress.get('mappingCount')==0 and ingress.get('leaseValid') is False,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    verification=read_helper('VERIFY_CLEANUP',{'rentalId':p['rentalId']});checks={x.get('kind'):x for x in verification.get('checks',[])}
    require(verification.get('rentalId')==p['rentalId'] and verification.get('result')=='FAIL','RECOVERY_REGISTRATION_MISMATCH')
    require(all(checks.get(k,{}).get('status')=='PASS' for k in ['TASKS','INGRESS','CONNECTIONS','LOCAL_STATE']),'RECOVERY_PHYSICAL_STATE_UNCONFIRMED')
    require(checks.get('VM',{}).get('status')=='FAIL' and checks['VM'].get('remaining')==[p['vmid']],'RECOVERY_REGISTRATION_MISMATCH')
    require(checks.get('SNIPPETS',{}).get('status')=='FAIL' and sorted(checks['SNIPPETS'].get('remaining',[]))==sorted(v.rsplit('/',1)[-1] for v in snippet_ids),'RECOVERY_SNIPPETS_UNCONFIRMED')
    registered=sorted(v for k,x in checks.items() if isinstance(k,str) and k.startswith('VOLUMES:') for v in x.get('remaining',[]))
    require(registered==volumes and all(x.get('status') in {'PASS','FAIL'} for k,x in checks.items() if isinstance(k,str) and k.startswith('VOLUMES:')),'RECOVERY_MANIFEST_MISMATCH')
    for sid in sorted({v.split(':')[0] for v in volumes}):
        content=api.call('GET',f'/nodes/{name}/storage/{quote(sid,safe="")}/content')
        require(sorted(v.get('volid') for v in content if f'vm-{p["vmid"]}-' in v.get('volid',''))==sorted(v for v in volumes if v.startswith(sid+':')),'RECOVERY_MANIFEST_MISMATCH')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    return {'operationId':identifier,'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],
            'recoveryKind':'START_GENERATION_REJECTED_RETURN_PENDING' if before_generation else 'START_IP_FAILED_RETURN_PENDING',
            'generationUnchanged':before_generation,'startNotSubmitted':before_generation,'vmStopped':state=='stopped',
            'checkedAt':now(),'vmOwned':True,'disksMatch':True,'tasksFinished':True,'noBlockingTasks':True,'taskPolicy':TASK_POLICY,
            'accessBlocked':True,'oldLeaseInvalid':True,'returnLeaseInvalid':True,'helperOwnerMatches':True,'registrationMatches':True,'returnIntentMatches':True,
            'returnJobId':return_job_id,'returnCommandId':claim['commandId'],'vmState':state,'volumeIds':volumes,'snippetIds':snippet_ids,
            'taskUpids':{e['step']:e['upid'] for e in effects},'helperGeneration':helper['generation'],'helperControlRevision':helper['controlRevision'],
            'resolution':'FAILED_INCOMPLETE_START_NOT_SUCCESS','backendStateSource':'Both old leases rejected; backend must authorize a fresh RETURN attempt'}


def inspect_deleted_return(store,config,identifier,expected_command,backend,api=None,transport=None):
    """Confirmed deletion plus closed ingress; never certifies remaining cleanup as successful."""
    operation=store.row('SELECT * FROM operations WHERE id=?',identifier)
    require(operation is not None,'NOT_FOUND')
    p=json.loads(operation['payload'])
    require(p.get('commandId')==expected_command,'RECOVERY_COMMAND_MISMATCH')
    require(config.get('mode')=='PROXMOX' and config.get('gateway_protocol')=='NODE_HELPER_V02','RECOVERY_REAL_HELPER_REQUIRED')
    require(p.get('type')=='RETURN' and operation['status']=='UNKNOWN' and operation['reason']=='HELPER_TASK_IN_PROGRESS','RECOVERY_NOT_SUPPORTED')
    require(p['serverId'] in config.get('server_ids',[]) and type(p.get('vmid')) is int,'RECOVERY_ASSIGNMENT_MISMATCH')
    job=store.row('SELECT * FROM jobs WHERE job_id=?',p['jobId'])
    require(job is not None and job['stage']=='BLOCKED' and job['operation_id']==identifier and job['command_id']==expected_command,'RECOVERY_JOB_CHANGED')
    deleted=store.row("SELECT * FROM effects WHERE operation_id=? AND step='delete'",identifier)
    require(deleted is not None and deleted['status']=='SUCCEEDED' and f':qmdestroy:{p["vmid"]}:' in deleted['upid'],'RECOVERY_DELETE_UNCONFIRMED')
    pending=store.row("SELECT request,response FROM helper_steps WHERE operation_id=? AND step='remove-snippets'",identifier)
    request=json.loads(pending['request']) if pending else {}
    require(pending is not None and pending['response'] is None and request.get('operation')=='REMOVE_SNIPPET' and request.get('payload',{}).get('rentalId')==p['rentalId'],'RECOVERY_CLEANUP_PHASE_CHANGED')
    require(not store.row("SELECT * FROM helper_steps WHERE operation_id=? AND step IN ('verify','release')",identifier),'RECOVERY_CLEANUP_PHASE_CHANGED')
    wf=store.row('SELECT * FROM helper_workflows WHERE operation_id=?',identifier)
    require(wf is not None and wf['server_id']==p['serverId'] and wf['rental_id']==p['rentalId'],'RECOVERY_HELPER_WORKFLOW_MISSING')
    closed=store.row("SELECT response FROM helper_steps WHERE operation_id=? AND step='initial-close'",identifier)
    receipt=json.loads(closed['response']) if closed and closed['response'] else {}
    require(receipt.get('enabled') is False and receipt.get('oldConnectionsCleared') is True,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    try:
        backend.query(f'/api/internal/jobs/{p["jobId"]}/context',workerId=config['worker_id'],commandId=expected_command)
    except Problem as error:
        require(error.code=='WORKER_LEASE_INVALID' and error.status==409,'RECOVERY_OLD_LEASE_UNCONFIRMED')
    else:raise Problem('RECOVERY_OLD_LEASE_STILL_VALID')
    node=config['nodes'][str(p['serverId'])];name=node['node_name']
    api=api or PveClient(config['proxmox']);transport=transport or HelperTransport(node['helper'])
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    task=api.call('GET',f'/nodes/{name}/tasks/{quote(deleted["upid"],safe="")}/status')
    require(task.get('status')=='stopped' and task.get('exitstatus')=='OK' and task.get('type')=='qmdestroy' and str(task.get('id'))==str(p['vmid']),'RECOVERY_DELETE_UNCONFIRMED')
    require(not any(int(v['vmid'])==p['vmid'] for v in api.call('GET',f'/nodes/{name}/qemu')),'RECOVERY_VM_STILL_PRESENT')
    manifests=[]
    for row in store.rows("SELECT payload,result FROM operations WHERE server_id=? AND status='SUCCEEDED' ORDER BY created_at",p['serverId']):
        old=json.loads(row['payload'])
        if old.get('rentalId')==p['rentalId'] and old.get('vmid')==p['vmid']:
            manifest=(json.loads(row['result']) if row['result'] else {}).get('manifest',{})
            if manifest.get('volumeIds'):manifests.append(sorted(manifest['volumeIds']))
    if not manifests:
        # PREPARE may have checkpointed its volumes before transport loss without
        # ever becoming SUCCEEDED. Accept only its existing, matching failure
        # audit plus the immutable REGISTER_RESOURCE receipt, never a bare FAILED.
        for row in store.rows("SELECT o.id,o.payload,o.reason,a.evidence,a.previous_status,a.previous_reason,a.resolution FROM operations o JOIN recovery_audits a ON a.operation_id=o.id WHERE o.server_id=? AND o.status='FAILED'",p['serverId']):
            old=json.loads(row['payload']);evidence=json.loads(row['evidence'])
            if old.get('type')!='PREPARE' or any(old.get(k)!=p[k] for k in ['serverId','rentalId','vmid']):continue
            require(row['reason']=='RECOVERED_PREPARE_INCOMPLETE' and row['previous_status']=='UNKNOWN' and row['previous_reason']=='TRANSPORT_UNCERTAIN' and row['resolution']=='FAILED','RECOVERY_MANIFEST_MISMATCH')
            require(evidence.get('recoveryKind')=='PREPARE_STARTED_TRANSPORT_INCOMPLETE' and evidence.get('operationId')==row['id'] and evidence.get('jobId')==old['jobId'] and all(evidence.get(k)==p[k] for k in ['serverId','rentalId','vmid']) and all(evidence.get(k) is True for k in ['vmOwned','disksMatch','tasksFinished','registrationMatches']),'RECOVERY_MANIFEST_MISMATCH')
            registered=store.row("SELECT request,response FROM helper_steps WHERE operation_id=? AND step='register-volumes'",row['id'])
            request=json.loads(registered['request']) if registered else {};receipt=json.loads(registered['response']) if registered and registered['response'] else {};resource=request.get('payload',{})
            require(request.get('operation')=='REGISTER_RESOURCE' and all(resource.get(k)==v and receipt.get(k)==v for k,v in {'rentalId':p['rentalId'],'vmId':p['vmid'],'vmIp':old.get('ip')}.items()),'RECOVERY_MANIFEST_MISMATCH')
            ids=resource.get('volumeIds',[])
            require(isinstance(ids,list) and bool(ids) and all(isinstance(v,str) for v in ids) and sorted(ids)==evidence.get('volumeIds'),'RECOVERY_MANIFEST_MISMATCH')
            manifests.append(sorted(ids))
    require(bool(manifests) and all(ids==manifests[0] for ids in manifests),'RECOVERY_MANIFEST_MISMATCH')
    volumes=manifests[0]
    require(all(':' in v and f'vm-{p["vmid"]}-' in v for v in volumes),'RECOVERY_VOLUME_OWNER_MISMATCH')
    for storage in sorted({v.split(':')[0] for v in volumes}):
        content=api.call('GET',f'/nodes/{name}/storage/{quote(storage,safe="")}/content')
        require(isinstance(content,list),'RECOVERY_STORAGE_UNCONFIRMED')
        require(not any(v.get('volid') in volumes or f'vm-{p["vmid"]}-' in v.get('volid','') for v in content),'RECOVERY_VOLUMES_STILL_PRESENT')
    req={'schemaVersion':1,'commandId':'recovery-read-'+str(uuid.uuid4()),'serverId':p['serverId'],'operation':'READ_STATUS','payload':{}}
    response=transport(req)
    require(response.get('ok') is True and response.get('mode')=='system' and all(response.get(k)==req[k] for k in ['commandId','serverId','operation']),'RECOVERY_HELPER_UNCONFIRMED')
    helper=response['result'];ingress=helper.get('ingress',{})
    require(helper.get('ownerRentalId')==p['rentalId'] and helper.get('generation')==wf['current_generation'] and helper.get('controlRevision')==wf['control_revision'],'RECOVERY_HELPER_OWNER_CHANGED')
    require(helper.get('accessEnabled') is False and helper.get('needsRecovery') is False and helper.get('pendingCleanup')==[] and blocking_task_count(helper)==0,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    require(ingress.get('target') is None and ingress.get('mappingCount')==0 and ingress.get('leaseValid') is False,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED')
    require(not blocking_tasks(api.call('GET',f'/nodes/{name}/tasks?source=active&limit=1000')),'RECOVERY_ACTIVE_TASKS')
    return {'operationId':identifier,'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],
            'recoveryKind':'DELETE_CONFIRMED_CLEANUP_INCOMPLETE','checkedAt':now(),
            'vmAbsent':True,'volumesAbsent':True,'deleteSucceeded':True,'deleteUpid':deleted['upid'],
            'ownerMatches':True,'noBlockingTasks':True,'taskPolicy':TASK_POLICY,'accessBlocked':True,'oldLeaseInvalid':True,
            'helperCleanupIncomplete':True,'volumeIds':volumes,'helperGeneration':helper['generation'],
            'helperControlRevision':helper['controlRevision'],
            'backendStateSource':'Old command lease independently rejected; backend retry must recheck FAILED and version',
            'resolution':'FAILED_INCOMPLETE_RETURN_NOT_SUCCESS'}
