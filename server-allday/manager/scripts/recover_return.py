#!/usr/bin/env python3
"""Offline operator recovery. Never deletes a VM, claims work or marks a return successful."""
import argparse
import getpass
import json
from pathlib import Path
import sqlite3
import sys

ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from manager.common import HttpClient,Problem
from manager.server import load_config,process_lock
from manager.store import Store
from manager.recovery import inspect_incomplete_return,inspect_deleted_return,inspect_uncloned_prepare,inspect_started_prepare


class ReadOnlyStore:
    def __init__(self,path):self.path=Path(path).resolve()
    def rows(self,sql,*args):
        db=sqlite3.connect(self.path.as_uri()+'?mode=ro',uri=True)
        try:
            db.row_factory=sqlite3.Row
            return [dict(row) for row in db.execute(sql,args)]
        finally:db.close()
    def row(self,sql,*args):
        rows=self.rows(sql,*args)
        return rows[0] if rows else None


def main(prepare=False):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config',type=Path,default=ROOT/'.local/config.json')
    parser.add_argument('--operation',required=True)
    parser.add_argument('--expected-command',required=True)
    parser.add_argument('--execute',action='store_true')
    if not prepare:parser.add_argument('--after-delete',action='store_true',help='Require a confirmed successful delete, absent VM/disks and incomplete Helper cleanup')
    if prepare:parser.add_argument('--after-start',action='store_true',help='Require completed clone/start, owned VM/disks, closed ingress and expired lease after backend transport loss')
    parser.add_argument('--confirm')
    parser.add_argument('--reason',default='')
    args=parser.parse_args();cfg=load_config(args.config)
    if args.execute and (args.confirm!=args.operation or not 10<=len(args.reason)<=500):raise Problem('RECOVERY_EXPLICIT_CONFIRMATION_REQUIRED',422)
    path=Path(cfg['state_path']);view=ReadOnlyStore(path)
    # The same lock used by the runtime forbids recovery alongside a live manager.
    with process_lock(path):
        previous=view.row('SELECT * FROM operations WHERE id=?',args.operation)
        resolved_reason='RECOVERED_PREPARE_INCOMPLETE' if prepare else 'RECOVERED_RETURN_INCOMPLETE'
        if previous and previous['status']=='FAILED' and previous['reason']==resolved_reason:
            if json.loads(previous['payload']).get('commandId')!=args.expected_command:raise Problem('RECOVERY_COMMAND_MISMATCH')
            audit=view.row('SELECT id FROM recovery_audits WHERE operation_id=?',args.operation)
            if not audit:raise Problem('RECOVERY_AUDIT_MISSING')
            print(json.dumps({'readOnly':True,'alreadyResolved':True,'operationId':args.operation,'status':'FAILED','auditId':audit['id']}));return
        backend=HttpClient(cfg['backend_url'],cfg['worker_token'],ca=cfg.get('backend_ca_file'))
        inspect=(inspect_started_prepare if args.after_start else inspect_uncloned_prepare) if prepare else inspect_deleted_return if args.after_delete else inspect_incomplete_return
        evidence=inspect(view,cfg,args.operation,args.expected_command,backend)
        print(json.dumps({'readOnly':True,'preflight':'PASS','evidence':evidence},ensure_ascii=False,indent=2))
        if not args.execute:
            print('CHECK ONLY: no operation state or VM/resource changed. Stop manager and use --execute with explicit confirmation.');return
        store=Store(path)
        result=store.resolve_incomplete_operation(args.operation,previous['payload_hash'],evidence,getpass.getuser(),args.reason)
        print(json.dumps(dict(result,vmMutationsPerformed=False,backendMutationsPerformed=False,ownershipReleased=False)))


def run_cli(prepare=False):
    try:main(prepare=prepare)
    except Problem as error:print('STOP: '+error.code,file=sys.stderr);sys.exit(1)
    except (OSError,ValueError,sqlite3.Error,RuntimeError):
        print('STOP: Recovery failed or manager is running. No automatic retries; inspect before proceeding.',file=sys.stderr);sys.exit(1)


if __name__=='__main__':run_cli()
