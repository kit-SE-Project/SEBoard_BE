#!/usr/bin/env python3
"""Offline audit of an IP-unconfirmed START superseded by a requested RETURN.

Never starts/stops/deletes a VM, releases ownership, or retries a backend job.
"""
import argparse
from datetime import datetime,timezone
import getpass
import json
from pathlib import Path
import sqlite3
import sys

ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from manager.common import HttpClient,Problem
from manager.server import load_config,process_lock
from manager.store import Store
from manager.recovery import inspect_start_for_return
from recover_return import ReadOnlyStore


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config',type=Path,default=ROOT/'.local/config.json')
    parser.add_argument('--operation',required=True)
    parser.add_argument('--expected-command',required=True)
    parser.add_argument('--return-job',type=int,required=True)
    parser.add_argument('--before-generation',action='store_true',help='Require a task-rejected generation claim, unchanged Helper generation, stopped VM and no submitted START')
    parser.add_argument('--execute',action='store_true')
    parser.add_argument('--confirm')
    parser.add_argument('--reason',default='')
    args=parser.parse_args();cfg=load_config(args.config);path=Path(cfg['state_path']);view=ReadOnlyStore(path)
    if args.execute and (args.confirm!=args.operation or not 10<=len(args.reason)<=500):raise Problem('RECOVERY_EXPLICIT_CONFIRMATION_REQUIRED',422)
    with process_lock(path):
        previous=view.row('SELECT * FROM operations WHERE id=?',args.operation)
        if previous and previous['status']=='FAILED' and previous['reason']=='RECOVERED_START_INCOMPLETE':
            audit=view.row('SELECT id,evidence FROM recovery_audits WHERE operation_id=?',args.operation)
            if json.loads(previous['payload']).get('commandId')!=args.expected_command or not audit or json.loads(audit['evidence']).get('returnJobId')!=args.return_job:raise Problem('RECOVERY_OPERATION_CHANGED')
            print(json.dumps({'readOnly':True,'alreadyResolved':True,'operationId':args.operation,'status':'FAILED','auditId':audit['id']}));return
        backend=HttpClient(cfg['backend_url'],cfg['worker_token'],ca=cfg.get('backend_ca_file'))
        evidence=inspect_start_for_return(view,cfg,args.operation,args.expected_command,args.return_job,backend,before_generation=args.before_generation)
        print(json.dumps({'readOnly':True,'preflight':'PASS','evidence':evidence},indent=2))
        if not args.execute:
            print('CHECK ONLY: no operation, VM, ownership or backend state changed.');return
        # Private consistent backup before the first write. No raw payload is printed.
        stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        backup=path.with_name('state-before-start-recovery-'+stamp+'.sqlite3')
        with backup.open('xb'):pass
        backup.chmod(0o600)
        source=sqlite3.connect(path.resolve().as_uri()+'?mode=ro',uri=True);dest=sqlite3.connect(backup)
        try:source.backup(dest)
        finally:dest.close();source.close()
        result=Store(path).resolve_incomplete_operation(args.operation,previous['payload_hash'],evidence,getpass.getuser(),args.reason)
        print(json.dumps(dict(result,backupFile=backup.name,vmMutationsPerformed=False,backendMutationsPerformed=False,ownershipReleased=False)))


if __name__=='__main__':
    try:main()
    except Problem as error:print('STOP: '+error.code,file=sys.stderr);sys.exit(1)
    except (OSError,ValueError,sqlite3.Error,RuntimeError):
        print('STOP: Recovery failed or manager is running. No automatic retries.',file=sys.stderr);sys.exit(1)
