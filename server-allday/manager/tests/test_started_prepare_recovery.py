import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock

from manager.common import Problem,RemoteProblem,canonical
from manager.helper_client import HelperClient
from manager.recovery import inspect_started_prepare
from manager.store import Store


class StartedPrepareRecoveryTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        self.store=Store(Path(temp.name)/'state.sqlite3');HelperClient(self.store,{'57':Mock()})
        self.p={'commandId':'job:759287:attempt:0:r0','jobId':759287,'serverId':57,'rentalId':21,
                'desiredRevision':0,'type':'PREPARE','vmid':10000,'ip':'10.10.0.101','gpuGeneration':0,'accessEnabled':True}
        self.operation=self.store.submit(self.p);self.identifier=self.operation['id']
        self.store.finish(self.identifier,'UNKNOWN',reason='TRANSPORT_UNCERTAIN')
        self.store.job(759287,self.p['commandId'],'BLOCKED',self.p,self.identifier)
        self.store.execute('INSERT INTO helper_workflows VALUES (?,?,?,?,?,?)',self.identifier,57,21,10,4,5)
        self.volumes=['local-lvm:vm-10000-cloudinit','local-lvm:vm-10000-disk-0'];self.refs={k:f'local:snippets/allday-r21-{k}.yaml' for k in ['user','network','meta']}
        resource={'rentalId':21,'vmId':10000,'vmIp':'10.10.0.101'}
        for step,kind,payload,response in [
            ('initial-close','DISABLE_INGRESS',{}, {'enabled':False,'oldConnectionsCleared':True}),
            ('register-empty','REGISTER_RESOURCE',dict(resource,volumeIds=[]),resource),
            ('register-volumes','REGISTER_RESOURCE',dict(resource,volumeIds=self.volumes),resource),
            ('snippets','PUT_SNIPPET',{'passwordHash':'fixture-never-export'}, {'rentalId':21,'verified':True,'references':self.refs}),
            ('claim','ADVANCE_GENERATION',{'rentalId':21,'expectedGeneration':4},{'generation':5,'ownerRentalId':21,'stopVerified':True}),
        ]:self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,step,canonical({'operation':kind,'payload':payload}),canonical(response))
        for step,kind,vmid in [('clone','qmclone',100),('start','qmstart',10000)]:self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,step,f'UPID:pve01:fixture:{kind}:{vmid}:token:','SUCCEEDED')
        self.cfg={'mode':'PROXMOX','gateway_protocol':'NODE_HELPER_V02','worker_id':'fixture','server_ids':[57],'nodes':{'57':{'node_name':'pve01','helper':{}}}}
        self.backend=Mock();self.backend.query.side_effect=RemoteProblem('WORKER_LEASE_INVALID',409)
        self.tasks=[];self.task_ok=True
        self.vm={'description':'Server Allday rental=21 server=57','scsi0':self.volumes[1],'ide2':self.volumes[0], 'cicustom':','.join(k+'='+v for k,v in self.refs.items())}
        self.content=[{'volid':v} for v in self.volumes]
        self.helper={'ownerRentalId':21,'generation':5,'controlRevision':10,'accessEnabled':False,'needsRecovery':False,'pendingCleanup':[],
                     'activeTaskCount':0,'ingress':{'target':None,'mappingCount':0,'leaseValid':False}}
        self.checks=[{'kind':k,'status':'PASS','remaining':[]} for k in ['TASKS','INGRESS','CONNECTIONS','LOCAL_STATE','VOLUMES:local']]
        self.checks.extend([{'kind':'VM','status':'FAIL','remaining':[10000]}, {'kind':'VOLUMES:local-lvm','status':'FAIL','remaining':self.volumes.copy()},
                            {'kind':'SNIPPETS','status':'FAIL','remaining':[v.rsplit('/',1)[-1] for v in self.refs.values()]}])
        self.api=Mock();self.api.call.side_effect=self.api_call

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if 'source=active' in path:return copy.deepcopy(self.tasks)
        if '/tasks/' in path:
            clone='qmclone' in path
            return {'status':'stopped','exitstatus':'OK' if self.task_ok else 'ERROR','type':'qmclone' if clone else 'qmstart','id':'100' if clone else '10000'}
        if path.endswith('/config'):return copy.deepcopy(self.vm)
        if path.endswith('/status/current'):return {'status':'running'}
        if path.endswith('/content'):return copy.deepcopy(self.content)
        self.fail(path)

    def transport(self,request):
        self.assertIn(request['operation'],['READ_STATUS','VERIFY_CLEANUP'])
        result=self.helper if request['operation']=='READ_STATUS' else {'rentalId':21,'result':'FAIL','checks':self.checks}
        return dict(request,ok=True,mode='system',result=copy.deepcopy(result))

    def inspect(self):return inspect_started_prepare(self.store,self.cfg,self.identifier,self.p['commandId'],self.backend,api=self.api,transport=self.transport)
    def resolve(self,evidence):return self.store.resolve_incomplete_operation(self.identifier,self.operation['payload_hash'],evidence,'test-operator','Transport interrupted PREPARE; owned VM and all resources verified; retain ownership for retry')

    def test_read_only_and_audited_failure_preserve_vm_resources_and_history(self):
        evidence=self.inspect();self.assertNotIn('fixture-never-export',json.dumps(evidence));self.backend.call.assert_not_called()
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        result=self.resolve(evidence);self.assertEqual('FAILED',result['status'])
        self.assertEqual('RECOVERED_PREPARE_INCOMPLETE',self.store.row('SELECT reason FROM operations WHERE id=?',self.identifier)['reason'])
        self.assertEqual(2,len(self.store.rows('SELECT * FROM effects')));self.assertEqual(5,len(self.store.rows('SELECT * FROM helper_steps')))
        self.assertEqual('BLOCKED',self.store.row('SELECT stage FROM jobs WHERE job_id=759287')['stage'])
        self.assertNotEqual(self.identifier,self.store.submit(dict(self.p,commandId='job:759287:attempt:1:r0'))['id'])

    def test_live_or_uncertain_old_lease_is_refused(self):
        self.backend.query.side_effect=None;self.backend.query.return_value={}
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_STILL_VALID'):self.inspect()
        self.backend.query.side_effect=RemoteProblem('TRANSPORT_UNCERTAIN',503)
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_UNCONFIRMED'):self.inspect()

    def test_foreign_or_locked_vm_is_refused(self):
        self.vm['description']='foreign'
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_OWNER_MISMATCH'):self.inspect()
        self.vm['description']='Server Allday rental=21 server=57';self.vm['lock']='backup'
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_OWNER_MISMATCH'):self.inspect()

    def test_task_in_progress_and_failed_task_are_refused(self):
        self.tasks=[{'type':'vzdump'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_ACTIVE_TASKS'):self.inspect()
        self.tasks=[];self.task_ok=False
        with self.assertRaisesRegex(Problem,'RECOVERY_TASKS_UNCONFIRMED'):self.inspect()

    def test_missing_volume_or_snippet_is_refused(self):
        self.content.pop()
        with self.assertRaisesRegex(Problem,'RECOVERY_MANIFEST_MISMATCH'):self.inspect()
        self.content=[{'volid':v} for v in self.volumes];self.checks[-1]['remaining'].pop()
        with self.assertRaisesRegex(Problem,'RECOVERY_SNIPPETS_UNCONFIRMED'):self.inspect()

    def test_open_access_and_helper_generation_drift_are_refused(self):
        self.helper['accessEnabled']=True
        with self.assertRaisesRegex(Problem,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED'):self.inspect()
        self.helper['accessEnabled']=False;self.helper['generation']+=1
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()

    def test_later_access_open_phase_is_refused(self):
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,'open-fixture',canonical({'operation':'APPLY_INGRESS'}),'{}')
        with self.assertRaisesRegex(Problem,'RECOVERY_PREPARE_PHASE_CHANGED'):self.inspect()

    def test_store_rejects_changed_effect_proof(self):
        evidence=self.inspect();evidence['taskUpids']['start']='UPID:foreign'
        with self.assertRaisesRegex(Problem,'RECOVERY_EVIDENCE_MISMATCH'):self.resolve(evidence)

    def test_store_rejects_changed_operation_and_preserves_unknown(self):
        evidence=self.inspect();self.store.execute("UPDATE operations SET reason='PVE_HTTP_ERROR' WHERE id=?",self.identifier)
        with self.assertRaisesRegex(Problem,'RECOVERY_NOT_SUPPORTED'):self.resolve(evidence)
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])


if __name__=='__main__':unittest.main()
