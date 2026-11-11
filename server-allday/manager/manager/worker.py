import json, logging, re, time, urllib.parse
from datetime import datetime,timezone
from .common import Problem, RemoteProblem, now

log=logging.getLogger('allday-manager.worker')

class Worker:
    def __init__(self,store,backend,agent,config):self.store=store;self.backend=backend;self.agent=agent;self.config=config;self.worker_id=config['worker_id'];self.last_heartbeat=0;self.last_health=0
    def permitted(self,server):
        ids=self.config.get('server_ids',[])
        return (not ids or server['id'] in ids) and (server.get('inventorySource')=='DEMO' if self.config['mode']=='DEMO' else server.get('inventorySource') in ['MANUAL','AGENT'])
    def log_blocked(self,job,stage,error):
        # Log identifiers/codes only, never claim/context bodies or tokens.
        code=error.code if isinstance(error.code,str) and re.fullmatch(r'[A-Z0-9_]{1,80}',error.code) else 'REMOTE_ERROR'
        log.warning('job blocked: jobId=%s stage=%s code=%s httpStatus=%s',job,stage,code,error.status)
    def awaiting_local_prepare(self,pending):
        if pending.get('type')!='START':return False
        for row in self.store.rows('SELECT id,payload,status FROM operations WHERE server_id=? ORDER BY created_at DESC',pending['serverId']):
            p=json.loads(row['payload'])
            if p.get('rentalId')==pending.get('rentalId') and p.get('type')=='PREPARE':
                if row['status']!='SUCCEEDED':return True
                report=self.store.row('SELECT stage FROM jobs WHERE operation_id=?',row['id'])
                return report is not None and report['stage']!='DONE'
        return False
    def claim_one(self,pending):
        # A known incomplete local PREPARE must not be overtaken by START or by
        # replay of a saved START claim intent. Backend remains authoritative.
        if self.awaiting_local_prepare(pending):
            self.store.job(pending['id'],pending['commandId'],'BLOCKED',pending)
            return False
        try:
            claim=self.backend.call('POST',f"/api/internal/jobs/{pending['id']}/claim",{'workerId':self.worker_id})
            self.store.job(claim['id'],claim['commandId'],'CLAIMED',claim)
            return True
        except RemoteProblem as e:
            if e.status>=500:raise
            if e.code in ['WORKER_LEASE_INVALID','STALE_REVISION']:
                record=self.store.row('SELECT * FROM jobs WHERE job_id=?',pending['id'])
                try:
                    if record and self.defer_undispatched(record,e):return False
                except Problem:pass
            self.log_blocked(pending['id'],'CLAIMING',e)
            self.store.job(pending['id'],pending['commandId'],'BLOCKED',pending)
            return False
    def claim_due(self):
        # Persist intent before claim so a lost HTTP response can replay the same claim.
        for record in self.store.rows("SELECT * FROM jobs WHERE stage='CLAIMING'"):
            self.claim_one(json.loads(record['payload']))
        busy={json.loads(row['payload']).get('serverId') for row in self.store.rows("SELECT payload FROM jobs WHERE stage IN ('CLAIMED','WAITING_AGENT')")}
        for row in self.store.rows("SELECT payload FROM jobs WHERE stage='REPORTING'"):
            payload=json.loads(row['payload']);busy.add(payload.get('claim',payload).get('serverId'))
        inventory={s['id']:s for s in self.backend.call('GET','/api/internal/inventory') if self.permitted(s)}
        for pending in self.backend.call('GET','/api/internal/jobs'):
            if pending['serverId'] not in inventory or pending['serverId'] in busy:continue
            saved=self.store.row('SELECT * FROM jobs WHERE job_id=?',pending['id'])
            if saved and saved['stage'] not in ['DONE','BLOCKED','DEFERRED']:continue
            self.store.job(pending['id'],pending['commandId'],'CLAIMING',pending)
            if self.claim_one(pending):busy.add(pending['serverId'])
    def defer_undispatched(self,record,error):
        # Agent.submit is local and persists its operation before returning. A
        # missing command is positive evidence that no VM mutation was dispatched.
        if record.get('operation_id') or self.store.row('SELECT id FROM operations WHERE command_id=?',record['command_id']):return False
        code=error.code if isinstance(error.code,str) and re.fullmatch(r'[A-Z0-9_]{1,100}',error.code) else 'PREFLIGHT_UNAVAILABLE'
        self.backend.call('POST',f"/api/internal/jobs/{record['job_id']}/dispatch-deferred",{
            'workerId':self.worker_id,'commandId':record['command_id'],'observedAt':now(),
            'dispatchNotStarted':True,'reasonCode':code})
        self.store.job(record['job_id'],record['command_id'],'DEFERRED',json.loads(record['payload']))
        return True
    def recover_undispatched(self):
        # Also recover pre-dispatch leases left BLOCKED by older manager versions.
        for record in self.store.rows("SELECT * FROM jobs WHERE stage='BLOCKED' AND operation_id IS NULL"):
            if json.loads(record['payload']).get('status')!='RUNNING':continue
            try:self.defer_undispatched(record,Problem('PREFLIGHT_RECHECK'))
            except Problem:pass
    def submit(self,record):
        claim=json.loads(record['payload']);job=claim['id'];command=claim['commandId'];path=f'/api/internal/jobs/{job}'
        context=self.backend.query(path+'/context',workerId=self.worker_id,commandId=command)
        if claim['type'] in ['PREPARE','START']:resources=self.backend.call('POST',path+'/resources',{'workerId':self.worker_id,'commandId':command})
        else:resources=context['resources']
        allocation=resources['allocation'];manifest=resources['manifest'];gpu=resources['gpu'];r=context['rental']
        vmid=allocation['vmid'] if allocation else manifest['vmid'] if manifest else None
        # Slot data is already authoritative and validated by the backend.
        # Include stable intent: START opens access; PREPARE asks the backend's current slot.
        access=claim['type']=='START'
        if claim['type']=='PREPARE':
            access=bool(context.get('inSlot',False))
        payload={'commandId':command,'jobId':job,'serverId':claim['serverId'],'rentalId':claim['rentalId'],'desiredRevision':claim['desiredRevision'],'type':claim['type'],'vmid':vmid,'ip':resources['ip']['address'] if resources['ip'] else None,'gpuGeneration':gpu['generation'],'accessEnabled':access}
        operation=self.agent.submit(payload)
        self.store.job(job,command,'WAITING_AGENT',claim,operation['id'])
    def report(self,record,operation):
        saved=json.loads(record['payload']);claim=saved.get('claim',saved);job=claim['id'];command=claim['commandId'];result=json.loads(operation['result']);path=f'/api/internal/jobs/{job}'
        if record['stage']!='REPORTING':
            common={'workerId':self.worker_id,'commandId':command,'desiredRevision':claim['desiredRevision'],'operationId':operation['id'],'upid':result.get('upid')};steps=[]
            if 'manifest' in result and claim['type']!='RETURN':steps.append({'path':path+'/manifest','body':{'workerId':self.worker_id,'commandId':command,**{k:result['manifest'][k] for k in ['volumeIds','snippetIds','ingressRuleIds']}}})
            if 'credential' in result:steps.append({'path':path+'/credential-applied','body':{**common,**result['credential']}})
            if 'observation' in result:
                p=json.loads(operation['payload']);steps.append({'path':path+'/observation','body':{**common,**result['observation'],'gpuGeneration':p['gpuGeneration'],'observedAt':result['observedAt']}})
            if 'verification' in result:steps.append({'path':path+'/return-verification','body':{**common,'observedAt':result['observedAt'],'items':result['verification']}})
            saved={'claim':claim,'steps':steps,'stepIndex':0};self.store.job(job,command,'REPORTING',saved,operation['id'])
        while saved['stepIndex']<len(saved['steps']):
            step=saved['steps'][saved['stepIndex']];self.backend.call('POST',step['path'],step['body']);saved['stepIndex']+=1;self.store.job(job,command,'REPORTING',saved,operation['id'])
        self.store.job(job,command,'DONE',claim,operation['id'])
    def tick(self):
        self.recover_undispatched()
        from .return_recovery import recover_for_return
        recover_for_return(self)
        if time.monotonic()-self.last_health>20:
            self.health();self.last_health=time.monotonic()
        self.claim_due()
        heartbeat_due=time.monotonic()-self.last_heartbeat>20
        for record in self.store.rows("SELECT * FROM jobs WHERE stage IN ('CLAIMED','WAITING_AGENT','REPORTING')"):
            try:
                if record['stage']=='CLAIMED':self.submit(record);continue
                operation=self.store.row('SELECT * FROM operations WHERE id=?',record['operation_id'])
                if operation['status']=='SUCCEEDED':self.report(record,operation)
                elif operation['status']=='UNKNOWN':
                    claim=json.loads(record['payload'])
                    reported=operation.get('reason')
                    code=reported if isinstance(reported,str) and re.fullmatch(r'[A-Z0-9_]{1,100}',reported) else 'MANAGER_OPERATION_UNKNOWN'
                    try:self.backend.call('POST',f"/api/internal/jobs/{record['job_id']}/failure",{'workerId':self.worker_id,'commandId':record['command_id'],'desiredRevision':claim['desiredRevision'],'operationId':operation['id'],'observedAt':now(),'reasonCode':code,'outcomeKnown':False,'accessBlocked':False})
                    except RemoteProblem:pass
                    self.store.job(record['job_id'],record['command_id'],'BLOCKED',claim,operation['id'])
                elif heartbeat_due:self.backend.call('POST',f"/api/internal/jobs/{record['job_id']}/heartbeat",{'workerId':self.worker_id,'commandId':record['command_id']})
            except RemoteProblem as e:
                if record['stage']=='CLAIMED' and e.status!=401:
                    try:
                        if self.defer_undispatched(record,e):continue
                    except Problem:pass  # Keep CLAIMED and retry the same defer after a lost response.
                if e.status in [401,403,409,422] and record['stage']!='CLAIMED':
                    self.store.job(record['job_id'],record['command_id'],'BLOCKED',json.loads(record['payload']),record['operation_id'])
                self.log_blocked(record['job_id'],record['stage'],e)
            except Problem as e:
                if record['stage']=='CLAIMED':
                    try:
                        if self.defer_undispatched(record,e):continue
                    except Problem:pass
                self.log_blocked(record['job_id'],record['stage'],e)
                if record['stage']!='CLAIMED':self.store.job(record['job_id'],record['command_id'],'BLOCKED',json.loads(record['payload']),record['operation_id'])
        if heartbeat_due:self.last_heartbeat=time.monotonic()
    def log_return_recovery(self,start_job,return_job,audit):
        log.warning('audited return convergence: failedStartJobId=%s returnJobId=%s auditId=%s; old START remains failed; RETURN must verify cleanup',start_job,return_job,audit)
    def health(self):
        for server in self.backend.call('GET','/api/internal/inventory'):
            if self.permitted(server):
                try:self.backend.call('POST',f"/api/internal/servers/{server['id']}/observation",{'status':'AVAILABLE' if self.config['mode']=='DEMO' else self.agent.driver.server_status(server),'observedAt':now()})
                except RemoteProblem:pass
        for target in self.backend.call('GET','/api/internal/health-targets'):
            if not self.permitted({'id':target['serverId'],'inventorySource':target['inventorySource']}):continue
            try:
                report=self.agent.driver.health(target)
                if report:self.backend.call('POST',f"/api/internal/rentals/{target['rentalId']}/health",{**report,'desiredRevision':target['desiredRevision'],'vmid':target['vmid'],'gpuGeneration':target['gpuGeneration'],'observedAt':now()})
            except (Problem,RemoteProblem):pass
    def fail_closed(self):
        # Backend connectivity loss must not leave previously opened routes enabled.
        seen=set()
        for row in self.store.rows('SELECT payload,server_id FROM operations ORDER BY created_at DESC'):
            if row['server_id'] in seen:continue
            seen.add(row['server_id'])
            try:self.agent.driver.block_access(json.loads(row['payload']))
            except Exception:pass  # Linux gateway expiry timer supplies the independent bound.
