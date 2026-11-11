import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock

from manager.common import Problem,RemoteProblem,canonical
from manager.helper_client import HelperClient
from manager.recovery import inspect_start_for_return
from manager.store import Store


class StartRecoveryTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        self.store=Store(Path(temp.name)/'state.sqlite3');HelperClient(self.store,{'57':Mock()})
        self.p={'commandId':'job:747022:attempt:0:r0','jobId':747022,'serverId':57,'rentalId':20,
                'desiredRevision':0,'type':'START','vmid':10000,'ip':'10.10.0.101','gpuGeneration':0,'accessEnabled':True}
        self.volumes=['local-lvm:vm-10000-cloudinit','local-lvm:vm-10000-disk-0']
        self.snippets=sorted(f'local:snippets/allday-r20-{k}.yaml' for k in ['user','network','meta'])
        prepared=self.store.submit(dict(self.p,commandId='prepare-fixture',jobId=747021,type='PREPARE'))
        self.store.finish(prepared['id'],'SUCCEEDED',{'manifest':{'volumeIds':self.volumes,'snippetIds':self.snippets},'credential':{'password':'fixture-must-not-be-printed'}})
        self.operation=self.store.submit(self.p);self.identifier=self.operation['id']
        self.store.finish(self.identifier,'UNKNOWN',reason='GUEST_IP_UNCONFIRMED')
        self.store.job(747022,self.p['commandId'],'BLOCKED',self.p,self.identifier)
        self.return_claim={'id':759286,'commandId':'job:759286:attempt:0:r1','type':'RETURN','serverId':57,'rentalId':20,'desiredRevision':1}
        self.store.job(759286,self.return_claim['commandId'],'BLOCKED',self.return_claim)
        self.store.execute('INSERT INTO helper_workflows VALUES (?,?,?,?,?,?)',self.identifier,57,20,8,3,4)
        for step,kind,response in [('initial-close','DISABLE_INGRESS',{'enabled':False,'oldConnectionsCleared':True}),('claim','ADVANCE_GENERATION',{'generation':4,'ownerRentalId':20,'stopVerified':True})]:
            request={'operation':kind,'payload':{} if step=='initial-close' else {'rentalId':20,'expectedGeneration':3}}
            self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,step,canonical(request),canonical(response))
        for step,kind in [('quiesce','qmstop'),('start','qmstart')]:self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,step,f'UPID:pve01:fixture:{kind}:10000:token:','SUCCEEDED')
        self.cfg={'mode':'PROXMOX','gateway_protocol':'NODE_HELPER_V02','worker_id':'fixture','server_ids':[57],
                  'nodes':{'57':{'node_name':'pve01','helper':{}}}}
        self.backend=Mock();self.backend.query.side_effect=RemoteProblem('WORKER_LEASE_INVALID',409)
        self.tasks=[];self.task_ok=True
        self.vm={'description':'Server Allday rental=20 server=57','scsi0':self.volumes[1]+',size=32G','ide2':self.volumes[0]+',media=cdrom',
                 'cicustom':','.join(k+'=local:snippets/allday-r20-'+k+'.yaml' for k in ['user','network','meta'])}
        self.content=[{'volid':v} for v in self.volumes]
        self.helper={'ownerRentalId':20,'generation':4,'controlRevision':8,'accessEnabled':False,'needsRecovery':False,'pendingCleanup':[],
                     'activeTaskCount':0,'ingress':{'target':None,'mappingCount':0,'leaseValid':False}}
        self.checks=[{'kind':k,'status':'PASS','remaining':[]} for k in ['TASKS','INGRESS','CONNECTIONS','LOCAL_STATE','VOLUMES:local']]
        self.checks.extend([{'kind':'VM','status':'FAIL','remaining':[10000]},
                            {'kind':'VOLUMES:local-lvm','status':'FAIL','remaining':self.volumes.copy()},
                            {'kind':'SNIPPETS','status':'FAIL','remaining':[v.rsplit('/',1)[-1] for v in self.snippets]}])
        self.api=Mock();self.api.call.side_effect=self.api_call

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if 'source=active' in path:return copy.deepcopy(self.tasks)
        if '/tasks/' in path and path.endswith('/status'):
            return {'status':'stopped','exitstatus':'OK' if self.task_ok else 'ERROR','type':'qmstop' if 'qmstop' in path else 'qmstart','id':'10000'}
        if path.endswith('/config'):return copy.deepcopy(self.vm)
        if path.endswith('/status/current'):return {'status':'running'}
        if path.endswith('/content'):return copy.deepcopy(self.content)
        self.fail(path)

    def transport(self,request):
        self.assertIn(request['operation'],['READ_STATUS','VERIFY_CLEANUP'])
        result=self.helper if request['operation']=='READ_STATUS' else {'rentalId':20,'result':'FAIL','checks':self.checks}
        return dict(request,ok=True,mode='system',result=copy.deepcopy(result))

    def inspect(self):
        return inspect_start_for_return(self.store,self.cfg,self.identifier,self.p['commandId'],759286,self.backend,api=self.api,transport=self.transport)

    def resolve(self,evidence):
        return self.store.resolve_incomplete_operation(self.identifier,self.operation['payload_hash'],evidence,'test-operator',
                                                       'START failed IP check; completed tasks and closed access verified; keep resources for RETURN')

    def test_read_only_evidence_preserves_unknown_and_secrets(self):
        evidence=self.inspect()
        self.assertTrue(evidence['returnIntentMatches']);self.assertEqual('running',evidence['vmState'])
        self.assertNotIn('fixture-must-not-be-printed',json.dumps(evidence))
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        self.backend.call.assert_not_called();self.assertEqual(2,self.backend.query.call_count)

    def test_audit_marks_failure_only_and_preserves_all_records(self):
        result=self.resolve(self.inspect());self.assertEqual('FAILED',result['status'])
        op=self.store.row('SELECT * FROM operations WHERE id=?',self.identifier)
        self.assertEqual('RECOVERED_START_INCOMPLETE',op['reason']);self.assertIsNone(op['result'])
        self.assertEqual(2,len(self.store.rows('SELECT * FROM effects WHERE operation_id=?',self.identifier)))
        self.assertEqual(2,len(self.store.rows('SELECT * FROM helper_steps WHERE operation_id=?',self.identifier)))
        self.assertEqual('BLOCKED',self.store.row('SELECT stage FROM jobs WHERE job_id=759286')['stage'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM recovery_audits')))
        retry=self.store.submit(dict(self.p,type='RETURN',jobId=759286,commandId='job:759286:attempt:1:r1',desiredRevision=1,accessEnabled=False))
        self.assertNotEqual(self.identifier,retry['id'])

    def test_resolution_is_idempotent(self):
        evidence=self.inspect();first=self.resolve(evidence);second=self.resolve(evidence)
        self.assertEqual(first['auditId'],second['auditId']);self.assertTrue(second['alreadyResolved'])

    def test_missing_or_foreign_return_is_refused(self):
        self.return_claim['rentalId']=21;self.store.job(759286,self.return_claim['commandId'],'BLOCKED',self.return_claim)
        with self.assertRaisesRegex(Problem,'RECOVERY_RETURN_INTENT_CHANGED'):self.inspect()

    def test_return_revision_must_supersede_start(self):
        self.return_claim['desiredRevision']=0;self.store.job(759286,self.return_claim['commandId'],'BLOCKED',self.return_claim)
        with self.assertRaisesRegex(Problem,'RECOVERY_RETURN_INTENT_CHANGED'):self.inspect()

    def test_return_changed_after_evidence_is_refused_atomically(self):
        evidence=self.inspect();self.store.job(759286,self.return_claim['commandId'],'CLAIMED',self.return_claim)
        with self.assertRaisesRegex(Problem,'RECOVERY_RETURN_INTENT_CHANGED'):self.resolve(evidence)
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        self.assertFalse(self.store.rows('SELECT * FROM recovery_audits'))

    def test_live_or_uncertain_lease_is_refused(self):
        self.backend.query.side_effect=[RemoteProblem('WORKER_LEASE_INVALID',409),{}]
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_STILL_VALID'):self.inspect()
        self.backend.query.side_effect=RemoteProblem('TRANSPORT_UNCERTAIN',503)
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_UNCONFIRMED'):self.inspect()

    def test_active_or_unfinished_task_is_refused(self):
        self.tasks=[{'type':'vzdump'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_ACTIVE_TASKS'):self.inspect()
        self.tasks=[];self.task_ok=False
        with self.assertRaisesRegex(Problem,'RECOVERY_TASKS_UNCONFIRMED'):self.inspect()

    def test_console_tasks_allow_evidence_but_old_helper_remains_strict(self):
        from manager.pve_tasks import TASK_POLICY
        self.tasks=[{'type':kind} for kind in ['vncshell','vncproxy','spiceproxy']]
        self.helper.update(activeTaskCount=3,blockingTaskCount=0,observationalTaskCount=3,taskPolicy=TASK_POLICY)
        evidence=self.inspect()
        self.assertTrue(evidence['noBlockingTasks']);self.assertEqual(TASK_POLICY,evidence['taskPolicy'])
        self.assertNotIn('noActiveTasks',evidence)
        self.helper.pop('taskPolicy')
        with self.assertRaisesRegex(Problem,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED'):self.inspect()

    def test_foreign_or_locked_vm_is_refused(self):
        self.vm['description']='Server Allday rental=21 server=57'
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_OWNER_MISMATCH'):self.inspect()
        self.vm['description']='Server Allday rental=20 server=57';self.vm['lock']='backup'
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_OWNER_MISMATCH'):self.inspect()

    def test_missing_or_extra_volume_is_refused(self):
        self.content.pop()
        with self.assertRaisesRegex(Problem,'RECOVERY_MANIFEST_MISMATCH'):self.inspect()

    def test_open_ingress_or_owner_drift_is_refused(self):
        self.helper['accessEnabled']=True
        with self.assertRaisesRegex(Problem,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED'):self.inspect()
        self.helper['accessEnabled']=False;self.helper['ownerRentalId']=21
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()

    def test_open_step_or_unexpected_effect_is_refused(self):
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,'open-fixture',canonical({'operation':'APPLY_INGRESS'}),canonical({}))
        with self.assertRaisesRegex(Problem,'RECOVERY_START_PHASE_CHANGED'):self.inspect()
        self.store.execute('DELETE FROM helper_steps WHERE operation_id=? AND step=?',self.identifier,'open-fixture')
        self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,'delete','UPID:delete','SUCCEEDED')
        with self.assertRaisesRegex(Problem,'RECOVERY_TASKS_UNCONFIRMED'):self.inspect()

    def test_evidence_cannot_replace_recorded_task_upid(self):
        evidence=self.inspect();evidence['taskUpids']['start']='UPID:foreign'
        with self.assertRaisesRegex(Problem,'RECOVERY_EVIDENCE_MISMATCH'):self.resolve(evidence)


class GenerationRejectedStartRecoveryTests(StartRecoveryTests):
    def setUp(self):
        super().setUp()
        self.store.finish(self.identifier,'UNKNOWN',reason='HELPER_TASK_IN_PROGRESS')
        self.store.execute('UPDATE helper_workflows SET current_generation=base_generation WHERE operation_id=?',self.identifier)
        request={'operation':'ADVANCE_GENERATION','generation':4,'controlRevision':8,'payload':{'rentalId':20,'expectedGeneration':3}}
        self.store.execute("UPDATE helper_steps SET request=?,response=NULL WHERE operation_id=? AND step='claim'",canonical(request),self.identifier)
        self.store.execute("DELETE FROM effects WHERE operation_id=? AND step='start'",self.identifier)
        self.store.execute("UPDATE effects SET upid='UPID:pve01:fixture:qmshutdown:10000:token:' WHERE operation_id=? AND step='quiesce'",self.identifier)
        self.store.job(759286,self.return_claim['commandId'],'DEFERRED',self.return_claim)
        self.helper['generation']=3
        self.task_history=[];self.vm_state='stopped'
        self.backend.call.side_effect=self.backend_call

    def backend_call(self,method,path):
        self.assertEqual(('GET','/api/internal/jobs'),(method,path))
        return [dict(self.return_claim,status='PENDING')]

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if 'source=all' in path:return copy.deepcopy(self.task_history)
        if path.endswith('/status/current'):return {'status':self.vm_state}
        if 'qmshutdown' in path and path.endswith('/status'):
            return {'status':'stopped','exitstatus':'OK' if self.task_ok else 'ERROR','type':'qmshutdown','id':'10000'}
        return super().api_call(method,path)

    def inspect(self):
        return inspect_start_for_return(self.store,self.cfg,self.identifier,self.p['commandId'],759286,self.backend,api=self.api,transport=self.transport,before_generation=True)

    def test_read_only_evidence_preserves_unknown_and_secrets(self):
        evidence=self.inspect()
        self.assertTrue(evidence['generationUnchanged'] and evidence['startNotSubmitted'] and evidence['vmStopped'])
        self.assertNotIn('fixture-must-not-be-printed',json.dumps(evidence))
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        self.backend.call.assert_called_once_with('GET','/api/internal/jobs')

    def test_audit_marks_failure_only_and_preserves_all_records(self):
        result=self.resolve(self.inspect());self.assertEqual('FAILED',result['status'])
        self.assertEqual('RECOVERED_START_INCOMPLETE',self.store.row('SELECT reason FROM operations WHERE id=?',self.identifier)['reason'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM effects WHERE operation_id=?',self.identifier)))
        self.assertEqual('DEFERRED',self.store.row('SELECT stage FROM jobs WHERE job_id=759286')['stage'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM recovery_audits')))
        self.store.submit(dict(self.p,type='RETURN',jobId=759286,commandId='return-after-audit',desiredRevision=1,accessEnabled=False))

    def test_running_vm_is_refused(self):
        self.vm_state='running'
        with self.assertRaisesRegex(Problem,'RECOVERY_STOP_UNCONFIRMED'):self.inspect()

    def test_generation_advance_or_claim_receipt_is_refused(self):
        self.helper['generation']=4
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()
        self.helper['generation']=3
        self.store.execute("UPDATE helper_steps SET response=? WHERE operation_id=? AND step='claim'",canonical({'generation':4}),self.identifier)
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()

    def test_later_start_task_is_refused(self):
        self.task_history=[{'type':'qmstart','starttime':9999999999}]
        with self.assertRaisesRegex(Problem,'RECOVERY_START_PHASE_CHANGED'):self.inspect()

    def test_backend_must_still_offer_the_same_return(self):
        self.backend.call.side_effect=None;self.backend.call.return_value=[]
        with self.assertRaisesRegex(Problem,'RECOVERY_RETURN_INTENT_CHANGED'):self.inspect()

    def test_changed_claim_after_evidence_is_refused_atomically(self):
        evidence=self.inspect()
        self.store.execute("UPDATE helper_steps SET request=? WHERE operation_id=? AND step='claim'",canonical({'operation':'ADVANCE_GENERATION','payload':{'rentalId':21}}),self.identifier)
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.resolve(evidence)
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])


if __name__=='__main__':unittest.main()
