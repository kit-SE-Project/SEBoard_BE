"""Converge a superseded failed START only toward an authoritative RETURN intent."""
from datetime import datetime,timezone
import json
from pathlib import Path
import sqlite3
import time
from .common import Problem
from .recovery import inspect_start_for_return


def recover_for_return(worker):
    if not worker.config.get('auto_recover_return',False) or worker.config.get('mode')!='PROXMOX' or worker.config.get('gateway_protocol')!='NODE_HELPER_V02':return
    candidates=worker.store.rows("SELECT * FROM operations WHERE status='UNKNOWN' AND json_extract(payload,'$.type')='START' AND reason IN ('HELPER_TASK_IN_PROGRESS','GUEST_IP_UNCONFIRMED')")
    if not candidates:return
    due=worker.backend.call('GET','/api/internal/jobs')
    attempts=getattr(worker,'return_recovery_attempts',{});worker.return_recovery_attempts=attempts
    for operation in candidates:
        payload=json.loads(operation['payload']);server=payload['serverId']
        if worker.config.get('server_ids') and server not in worker.config['server_ids']:continue
        pending=next((j for j in due if j.get('type')=='RETURN' and j.get('serverId')==server and j.get('rentalId')==payload['rentalId'] and j.get('desiredRevision',-1)>payload['desiredRevision']),None)
        if not pending:continue
        local=worker.store.row('SELECT * FROM jobs WHERE job_id=?',pending['id'])
        if not local or local['stage'] not in {'BLOCKED','DEFERRED'} or local['operation_id'] is not None:continue
        if time.monotonic()-attempts.get(operation['id'],-1000)<20:continue
        attempts[operation['id']]=time.monotonic()
        try:
            # Runtime owns the process lock; serialize the complete inspection
            # against Agent/health and local writes while preserving other nodes.
            with worker.agent.driver.helper.locks[str(server)],worker.store.lock:
                current=worker.store.row('SELECT * FROM operations WHERE id=?',operation['id'])
                if current['status']!='UNKNOWN':continue
                if worker.store.row("SELECT id FROM operations WHERE server_id=? AND status IN ('PENDING','RUNNING')",server):continue
                evidence=inspect_start_for_return(worker.store,worker.config,operation['id'],payload['commandId'],pending['id'],worker.backend,
                    api=worker.agent.driver.api,transport=worker.agent.driver.helper.transports[str(server)],before_generation=operation['reason']=='HELPER_TASK_IN_PROGRESS')
                folder=Path(worker.store.path).parent/'recovery-backups';folder.mkdir(parents=True,exist_ok=True)
                backup=folder/('before-return-convergence-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'.sqlite3')
                with backup.open('xb'):pass
                backup.chmod(0o600)
                source=sqlite3.connect(Path(worker.store.path).resolve().as_uri()+'?mode=ro',uri=True);target=sqlite3.connect(backup)
                try:source.backup(target)
                finally:target.close();source.close()
                result=worker.store.resolve_incomplete_operation(operation['id'],operation['payload_hash'],evidence,worker.worker_id,
                    'Authoritative newer RETURN is pending. Verified stopped/owned resources, closed access and invalid leases. Resolve the old failed START only; let the existing RETURN perform and verify cleanup.')
                worker.log_return_recovery(payload['jobId'],pending['id'],result['auditId'])
        except Problem as error:
            worker.log_blocked(payload['jobId'],'RETURN_RECOVERY_WAIT',error)
        except (OSError,sqlite3.Error,ValueError):
            worker.log_blocked(payload['jobId'],'RETURN_RECOVERY_WAIT',Problem('RETURN_RECOVERY_CHECK_UNAVAILABLE'))
