import json, os, sqlite3, threading, uuid
from datetime import datetime,timezone
from contextlib import contextmanager
from pathlib import Path
from .common import canonical, digest, now, Problem

class Store:
    def __init__(self,path):
        self.path=Path(path);self.path.parent.mkdir(parents=True,exist_ok=True);self.lock=threading.RLock()
        with self.connect() as db:
            db.executescript('''PRAGMA journal_mode=WAL;
CREATE TABLE IF NOT EXISTS operations(id TEXT PRIMARY KEY,command_id TEXT UNIQUE NOT NULL,server_id INTEGER NOT NULL,payload_hash TEXT NOT NULL,payload TEXT NOT NULL,status TEXT NOT NULL,result TEXT,reason TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS server_inflight ON operations(server_id) WHERE status IN ('PENDING','RUNNING','UNKNOWN');
CREATE TABLE IF NOT EXISTS jobs(job_id INTEGER PRIMARY KEY,command_id TEXT NOT NULL,operation_id TEXT,stage TEXT NOT NULL,payload TEXT NOT NULL,updated_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS effects(operation_id TEXT NOT NULL,step TEXT NOT NULL,upid TEXT NOT NULL,status TEXT NOT NULL,PRIMARY KEY(operation_id,step));
CREATE TABLE IF NOT EXISTS recovery_audits(id TEXT PRIMARY KEY,operation_id TEXT UNIQUE NOT NULL,previous_status TEXT NOT NULL,resolution TEXT NOT NULL,previous_reason TEXT,operator TEXT NOT NULL,reason TEXT NOT NULL,evidence TEXT NOT NULL,created_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS demo_vms(vmid INTEGER PRIMARY KEY,rental_id INTEGER NOT NULL,server_id INTEGER NOT NULL,state TEXT NOT NULL,access_enabled INTEGER NOT NULL,ip TEXT NOT NULL,gpu_attached INTEGER NOT NULL,credential_version INTEGER NOT NULL DEFAULT 0,credential_hash TEXT,host_key TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS demo_resources(id TEXT PRIMARY KEY,vmid INTEGER NOT NULL);
''')
        self.path.chmod(0o600)
    @contextmanager
    def connect(self):
        with self.lock:
            db=sqlite3.connect(self.path,timeout=20);db.row_factory=sqlite3.Row
            try:yield db;db.commit()
            except Exception:db.rollback();raise
            finally:db.close()
    def row(self,sql,*args):
        with self.connect() as db:
            row=db.execute(sql,args).fetchone();return dict(row) if row else None
    def rows(self,sql,*args):
        with self.connect() as db:return [dict(r) for r in db.execute(sql,args)]
    def execute(self,sql,*args):
        with self.connect() as db:db.execute(sql,args)
    def submit(self,payload):
        encoded=canonical(payload);hash_value=digest(payload)
        with self.connect() as db:
            prior=db.execute('SELECT * FROM operations WHERE command_id=?',(payload['commandId'],)).fetchone()
            if prior:
                if prior['payload_hash']!=hash_value:raise Problem('COMMAND_PAYLOAD_CHANGED')
                return dict(prior)
            identifier='op-'+str(uuid.uuid4())
            try:db.execute('INSERT INTO operations VALUES (?,?,?,?,?,?,NULL,NULL,?,?)',(identifier,payload['commandId'],payload['serverId'],hash_value,encoded,'PENDING',now(),now()))
            except sqlite3.IntegrityError:raise Problem('SERVER_OPERATION_PENDING') from None
            return dict(db.execute('SELECT * FROM operations WHERE id=?',(identifier,)).fetchone())
    def finish(self,identifier,status,result=None,reason=None):self.execute('UPDATE operations SET status=?,result=?,reason=?,updated_at=? WHERE id=?',status,None if result is None else canonical(result),reason,now(),identifier)
    def recover(self):
        # Never blindly repeat a mutation sent before process termination.
        self.execute("UPDATE operations SET status='UNKNOWN',reason='PROCESS_INTERRUPTED',updated_at=? WHERE status='RUNNING'",now())
    def resolve_incomplete_return(self,identifier,payload_hash,evidence,operator,reason):
        """Compatibility entry point restricted to the two RETURN recovery profiles."""
        if not isinstance(evidence,dict) or evidence.get('recoveryKind','PRE_DELETE_INCOMPLETE') not in {'PRE_DELETE_INCOMPLETE','DELETE_CONFIRMED_CLEANUP_INCOMPLETE'}:raise Problem('RECOVERY_NOT_SUPPORTED')
        return self.resolve_incomplete_operation(identifier,payload_hash,evidence,operator,reason)
    def resolve_incomplete_operation(self,identifier,payload_hash,evidence,operator,reason):
        """Operator-only terminal failure, never a retry or successful physical operation."""
        if not isinstance(reason,str) or not 10<=len(reason)<=500 or not operator:raise Problem('RECOVERY_REASON_REQUIRED',422)
        if not isinstance(evidence,dict):raise Problem('RECOVERY_EVIDENCE_INCOMPLETE')
        kind=evidence.get('recoveryKind','PRE_DELETE_INCOMPLETE')
        if kind=='PRE_DELETE_INCOMPLETE':
            required={'vmStopped','ownerMatches','disksMatch','noBlockingTasks','noDeleteTask','accessBlocked','oldLeaseInvalid'}
            expected_reason='PVE_HTTP_ERROR'
            expected_type='RETURN';resolved_reason='RECOVERED_RETURN_INCOMPLETE'
        elif kind=='DELETE_CONFIRMED_CLEANUP_INCOMPLETE':
            required={'vmAbsent','volumesAbsent','deleteSucceeded','ownerMatches','noBlockingTasks','accessBlocked','oldLeaseInvalid','helperCleanupIncomplete'}
            expected_reason='HELPER_TASK_IN_PROGRESS'
            expected_type='RETURN';resolved_reason='RECOVERED_RETURN_INCOMPLETE'
        elif kind=='PREPARE_BEFORE_CLONE_INCOMPLETE':
            required={'vmAbsent','volumesAbsent','noCloneTask','noBlockingTasks','accessBlocked','oldLeaseInvalid','helperUnclaimed','snippetsPreserved','poolReady','registrationMatches'}
            expected_reason='PVE_HTTP_ERROR';expected_type='PREPARE';resolved_reason='RECOVERED_PREPARE_INCOMPLETE'
        elif kind=='START_IP_FAILED_RETURN_PENDING':
            required={'vmOwned','disksMatch','tasksFinished','noBlockingTasks','accessBlocked','oldLeaseInvalid','returnLeaseInvalid','helperOwnerMatches','registrationMatches','returnIntentMatches'}
            expected_reason='GUEST_IP_UNCONFIRMED';expected_type='START';resolved_reason='RECOVERED_START_INCOMPLETE'
        elif kind=='START_GENERATION_REJECTED_RETURN_PENDING':
            required={'vmOwned','vmStopped','disksMatch','tasksFinished','noBlockingTasks','accessBlocked','oldLeaseInvalid','returnLeaseInvalid','helperOwnerMatches','registrationMatches','returnIntentMatches','generationUnchanged','startNotSubmitted'}
            expected_reason='HELPER_TASK_IN_PROGRESS';expected_type='START';resolved_reason='RECOVERED_START_INCOMPLETE'
        elif kind=='PREPARE_STARTED_TRANSPORT_INCOMPLETE':
            required={'vmOwned','disksMatch','tasksFinished','noBlockingTasks','accessBlocked','oldLeaseInvalid','helperOwnerMatches','registrationMatches','snippetsPreserved'}
            expected_reason='TRANSPORT_UNCERTAIN';expected_type='PREPARE';resolved_reason='RECOVERED_PREPARE_INCOMPLETE'
        else:raise Problem('RECOVERY_NOT_SUPPORTED')
        if not isinstance(evidence,dict) or any(evidence.get(k) is not True for k in required):raise Problem('RECOVERY_EVIDENCE_INCOMPLETE')
        try:age=(datetime.now(timezone.utc)-datetime.fromisoformat(evidence['checkedAt'])).total_seconds()
        except (KeyError,ValueError,TypeError):raise Problem('RECOVERY_EVIDENCE_STALE') from None
        if not 0<=age<=60:raise Problem('RECOVERY_EVIDENCE_STALE')
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            row=db.execute('SELECT * FROM operations WHERE id=?',(identifier,)).fetchone()
            if row is None:raise Problem('NOT_FOUND',404)
            if row['payload_hash']!=payload_hash:raise Problem('RECOVERY_OPERATION_CHANGED')
            prior=db.execute('SELECT * FROM recovery_audits WHERE operation_id=?',(identifier,)).fetchone()
            if prior:
                if row['status']!='FAILED' or row['reason']!=resolved_reason or json.loads(prior['evidence']).get('recoveryKind','PRE_DELETE_INCOMPLETE')!=kind:raise Problem('RECOVERY_OPERATION_CHANGED')
                return {'operationId':identifier,'status':'FAILED','auditId':prior['id'],'alreadyResolved':True}
            p=json.loads(row['payload'])
            if row['status']!='UNKNOWN' or row['reason']!=expected_reason or p['type']!=expected_type:raise Problem('RECOVERY_NOT_SUPPORTED')
            if evidence.get('operationId')!=identifier or evidence.get('jobId')!=p['jobId'] or evidence.get('rentalId')!=p['rentalId'] or evidence.get('serverId')!=p['serverId'] or evidence.get('vmid')!=p['vmid']:raise Problem('RECOVERY_EVIDENCE_MISMATCH')
            deleted=db.execute("SELECT * FROM effects WHERE operation_id=? AND step='delete'",(identifier,)).fetchone()
            if kind=='PRE_DELETE_INCOMPLETE' and deleted:raise Problem('RECOVERY_DELETE_ALREADY_SUBMITTED')
            if kind=='DELETE_CONFIRMED_CLEANUP_INCOMPLETE' and (not deleted or deleted['status']!='SUCCEEDED' or deleted['upid']!=evidence.get('deleteUpid')):raise Problem('RECOVERY_DELETE_UNCONFIRMED')
            if kind=='PREPARE_BEFORE_CLONE_INCOMPLETE' and db.execute('SELECT 1 FROM effects WHERE operation_id=?',(identifier,)).fetchone():raise Problem('RECOVERY_PHYSICAL_EFFECT_EXISTS')
            if kind=='PREPARE_STARTED_TRANSPORT_INCOMPLETE':
                effects=db.execute('SELECT * FROM effects WHERE operation_id=?',(identifier,)).fetchall()
                if {e['step'] for e in effects}!={'clone','start'} or any(e['status']!='SUCCEEDED' for e in effects):raise Problem('RECOVERY_TASKS_UNCONFIRMED')
                if {e['step']:e['upid'] for e in effects}!=evidence.get('taskUpids'):raise Problem('RECOVERY_EVIDENCE_MISMATCH')
            if kind in {'START_IP_FAILED_RETURN_PENDING','START_GENERATION_REJECTED_RETURN_PENDING'}:
                before_generation=kind=='START_GENERATION_REJECTED_RETURN_PENDING'
                effects=db.execute('SELECT * FROM effects WHERE operation_id=?',(identifier,)).fetchall()
                if {e['step'] for e in effects}!=({'quiesce'} if before_generation else {'quiesce','start'}) or any(e['status']!='SUCCEEDED' for e in effects):raise Problem('RECOVERY_TASKS_UNCONFIRMED')
                if {e['step']:e['upid'] for e in effects}!=evidence.get('taskUpids'):raise Problem('RECOVERY_EVIDENCE_MISMATCH')
                if before_generation:
                    wf=db.execute('SELECT * FROM helper_workflows WHERE operation_id=?',(identifier,)).fetchone()
                    claim_step=db.execute("SELECT request,response FROM helper_steps WHERE operation_id=? AND step='claim'",(identifier,)).fetchone()
                    if not wf or wf['current_generation']!=wf['base_generation'] or not claim_step or claim_step['response'] is not None:raise Problem('RECOVERY_HELPER_OWNER_CHANGED')
                    request=json.loads(claim_step['request'])
                    if wf['server_id']!=p['serverId'] or wf['rental_id']!=p['rentalId'] or evidence.get('helperGeneration')!=wf['base_generation'] or evidence.get('helperControlRevision')!=wf['control_revision'] or request.get('operation')!='ADVANCE_GENERATION' or request.get('generation')!=wf['base_generation']+1 or request.get('controlRevision')!=wf['control_revision'] or request.get('payload')!={'rentalId':p['rentalId'],'expectedGeneration':wf['base_generation']}:raise Problem('RECOVERY_HELPER_OWNER_CHANGED')
                pending=db.execute('SELECT * FROM jobs WHERE job_id=?',(evidence.get('returnJobId'),)).fetchone()
                claim=json.loads(pending['payload']) if pending else {}
                if not pending or pending['stage'] not in ({'BLOCKED','DEFERRED'} if before_generation else {'BLOCKED'}) or pending['operation_id'] is not None or pending['command_id']!=evidence.get('returnCommandId') or any(claim.get(k)!=v for k,v in {'id':evidence.get('returnJobId'),'type':'RETURN','rentalId':p['rentalId'],'serverId':p['serverId']}.items()) or claim.get('desiredRevision',-1)<=p['desiredRevision']:raise Problem('RECOVERY_RETURN_INTENT_CHANGED')
            job=db.execute('SELECT * FROM jobs WHERE job_id=?',(p['jobId'],)).fetchone()
            if not job or job['stage']!='BLOCKED' or job['operation_id']!=identifier or job['command_id']!=p['commandId']:raise Problem('RECOVERY_JOB_CHANGED')
            audit='recovery-'+str(uuid.uuid4());timestamp=now()
            db.execute('INSERT INTO recovery_audits VALUES (?,?,?,?,?,?,?,?,?)',(audit,identifier,'UNKNOWN','FAILED',row['reason'],operator,reason,canonical(evidence),timestamp))
            db.execute("UPDATE operations SET status='FAILED',reason=?,updated_at=? WHERE id=?",(resolved_reason,timestamp,identifier))
            # Keep result, command, payload, job and all effects/helper records intact.
            return {'operationId':identifier,'status':'FAILED','auditId':audit,'alreadyResolved':False}
    def job(self,job_id,command,stage,payload,operation=None):
        self.execute('INSERT INTO jobs VALUES (?,?,?,?,?,?) ON CONFLICT(job_id) DO UPDATE SET command_id=excluded.command_id,operation_id=excluded.operation_id,stage=excluded.stage,payload=excluded.payload,updated_at=excluded.updated_at',job_id,command,operation,stage,canonical(payload),now())
    def public(self,row):
        return {k:row.get(k) for k in ['id','command_id','server_id','status','reason','created_at','updated_at']}|{'result':json.loads(row['result']) if row.get('result') else None}
