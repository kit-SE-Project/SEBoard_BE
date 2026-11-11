import json, threading, ipaddress
from .common import Problem, RemoteProblem, now

TYPES={'PREPARE','START','STOP','RESET_CREDENTIAL','RETURN'}
FIELDS={'commandId','jobId','serverId','rentalId','desiredRevision','type','vmid','ip','gpuGeneration','accessEnabled'}

def validate(payload):
    if not isinstance(payload,dict) or set(payload)!=FIELDS:raise Problem('INVALID_OPERATION',422)
    for key in ['jobId','serverId','rentalId']:
        if type(payload.get(key)) is not int or payload[key]<=0:raise Problem('INVALID_OPERATION',422)
    for key in ['desiredRevision','gpuGeneration']:
        if type(payload.get(key)) is not int or payload[key]<0:raise Problem('INVALID_OPERATION',422)
    if payload.get('type') not in TYPES or not isinstance(payload.get('commandId'),str) or not 1<=len(payload['commandId'])<=150 or type(payload.get('accessEnabled')) is not bool:raise Problem('INVALID_OPERATION',422)
    if payload.get('vmid') is not None and (type(payload['vmid']) is not int or payload['vmid']<100):raise Problem('ALLOCATION_REQUIRED',422)
    if payload['type']!='RETURN' and payload['vmid'] is None:raise Problem('ALLOCATION_REQUIRED',422)
    if payload.get('ip') is not None:
        try:
            if not isinstance(payload['ip'],str) or ipaddress.ip_address(payload['ip']).version!=4:raise ValueError()
        except ValueError:raise Problem('INVALID_VM_IP',422) from None
    if payload['type']!='RETURN' and payload['ip'] is None:raise Problem('ALLOCATION_REQUIRED',422)
    return payload

class Agent:
    def __init__(self,store,backend,driver,worker_id,allowed):self.store=store;self.backend=backend;self.driver=driver;self.worker_id=worker_id;self.allowed=allowed
    def context(self,p):
        result=self.backend.query(f"/api/internal/jobs/{p['jobId']}/context",workerId=self.worker_id,commandId=p['commandId'])
        job=result['job'];r=result['rental'];server=result['server']
        if job['commandId']!=p['commandId'] or r['id']!=p['rentalId'] or server['id']!=p['serverId'] or job['type']!=p['type'] or r['desiredRevision']!=p['desiredRevision']:raise Problem('BACKEND_CONTEXT_MISMATCH')
        if self.allowed and server['id'] not in self.allowed:raise Problem('SERVER_NOT_ASSIGNED',403)
        allocation=result['resources']['allocation'];gpu=result['resources']['gpu']
        manifest=result['resources']['manifest'];ip=result['resources']['ip']
        expected_vmid=allocation['vmid'] if allocation else manifest['vmid'] if manifest else None
        if p['vmid']!=expected_vmid or p['gpuGeneration']!=gpu['generation'] or p['ip']!=(ip['address'] if ip else None):raise Problem('RESOURCE_CONTEXT_MISMATCH')
        if p['accessEnabled'] and (p['type'] not in ['PREPARE','START'] or not result['inSlot']):raise Problem('ACCESS_NOT_AUTHORIZED')
        self.driver.check_source(result);return result
    def submit(self,payload):
        validate(payload)
        # Replay an already verified command even if the lease has since finished.
        prior=self.store.row('SELECT * FROM operations WHERE command_id=?',payload['commandId'])
        if prior:return self.store.public(self.store.submit(payload))
        self.context(payload);return self.store.public(self.store.submit(payload))
    def run_pending(self):
        for operation in self.store.rows("SELECT * FROM operations WHERE status='PENDING' ORDER BY created_at LIMIT 20"):
            p=json.loads(operation['payload']);credential=None
            try:
                context=self.context(p)
                self.store.finish(operation['id'],'RUNNING')
                def guard():
                    fresh=self.context(p)
                    self.backend.call('POST',f"/api/internal/jobs/{p['jobId']}/heartbeat",{'workerId':self.worker_id,'commandId':p['commandId']})
                    return fresh
                if p['type'] in ['PREPARE','RESET_CREDENTIAL']:
                    credential=self.backend.query(f"/api/internal/jobs/{p['jobId']}/pending-credential",workerId=self.worker_id,commandId=p['commandId'])
                result=self.driver.execute(operation,context,credential,guard)
                guard();result['observedAt']=now();self.store.finish(operation['id'],'SUCCEEDED',result)
            except (Problem,RemoteProblem) as e:
                diagnostic={'error':{'code':e.code,'httpStatus':e.status}} if isinstance(e,RemoteProblem) and e.code=='PVE_HTTP_ERROR' else None
                self.store.finish(operation['id'],'UNKNOWN',result=diagnostic,reason=e.code)
            except Exception:
                self.store.finish(operation['id'],'UNKNOWN',reason='EXECUTION_UNCERTAIN')
            finally:credential=None
