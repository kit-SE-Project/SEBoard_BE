import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock

from manager.common import Problem,RemoteProblem,canonical
from manager.helper_client import HelperClient
from manager.recovery import inspect_uncloned_prepare
from manager.store import Store


class PrepareRecoveryTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        self.store=Store(Path(temp.name)/'state.sqlite3');HelperClient(self.store,{'57':Mock()})
        self.p={'commandId':'job:747021:attempt:0:r0','jobId':747021,'serverId':57,'rentalId':20,
                'desiredRevision':0,'type':'PREPARE','vmid':10000,'ip':'10.10.0.101','gpuGeneration':2,'accessEnabled':False}
        self.operation=self.store.submit(self.p);self.identifier=self.operation['id']
        self.store.finish(self.identifier,'UNKNOWN',reason='PVE_HTTP_ERROR')
        self.store.job(747021,self.p['commandId'],'BLOCKED',{},self.identifier)
        self.store.execute('INSERT INTO helper_workflows VALUES (?,?,?,?,?,?)',self.identifier,57,20,6,2,2)
        register={'operation':'REGISTER_RESOURCE','payload':{'rentalId':20,'vmId':10000,'vmIp':'10.10.0.101','volumeIds':[]}}
        self.refs={k:f'local:snippets/allday-r20-{k}.yaml' for k in ['user','network','meta']}
        for step,request,response in [
            ('initial-close',{}, {'enabled':False,'oldConnectionsCleared':True}),
            ('register-empty',register,{'rentalId':20,'vmId':10000,'vmIp':'10.10.0.101'}),
            ('snippets',{'passwordHash':'fixture-private-hash'},{'rentalId':20,'verified':True,'references':self.refs}),
        ]:self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,step,canonical(request),canonical(response))
        self.cfg={'mode':'PROXMOX','gateway_protocol':'NODE_HELPER_V02','worker_id':'fixture','server_ids':[57],
                  'nodes':{'57':{'node_name':'pve01','vm_pool':'allday-node57','helper':{}}}}
        self.backend=Mock();self.backend.query.side_effect=RemoteProblem('WORKER_LEASE_INVALID',409)
        self.tasks=[];self.history=[];self.vms=[{'vmid':100,'template':1}];self.content=[]
        self.permissions={'/pool/allday-node57':{'VM.Allocate':1,'Pool.Audit':1}}
        self.pool={'poolid':'allday-node57','members':[]}
        self.helper={'ownerRentalId':None,'generation':2,'controlRevision':6,'accessEnabled':False,
                     'needsRecovery':False,'pendingCleanup':[],'activeTaskCount':0,
                     'ingress':{'target':None,'mappingCount':0,'leaseValid':False}}
        self.checks=[{'kind':k,'status':'PASS'} for k in ['VM','TASKS','VOLUMES:local','VOLUMES:local-lvm','INGRESS','CONNECTIONS','LOCAL_STATE']]
        self.checks.append({'kind':'SNIPPETS','status':'FAIL','remaining':[v.rsplit('/',1)[-1] for v in self.refs.values()]})
        self.api=Mock();self.api.call.side_effect=self.api_call

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if path.startswith('/access/permissions?'):return copy.deepcopy(self.permissions)
        if path=='/pools/allday-node57':return copy.deepcopy(self.pool)
        if 'source=active' in path:return copy.deepcopy(self.tasks)
        if 'source=all' in path:return copy.deepcopy(self.history)
        if path.endswith('/qemu'):return copy.deepcopy(self.vms)
        if path.endswith('/storage'):return [{'storage':'local','active':1},{'storage':'local-lvm','active':1}]
        if path.endswith('/content'):return copy.deepcopy(self.content)
        self.fail(path)

    def transport(self,request):
        self.assertIn(request['operation'],['READ_STATUS','VERIFY_CLEANUP'])
        result=self.helper if request['operation']=='READ_STATUS' else {'rentalId':20,'result':'FAIL','checks':self.checks}
        return dict(request,ok=True,mode='system',result=copy.deepcopy(result))

    def inspect(self):
        return inspect_uncloned_prepare(self.store,self.cfg,self.identifier,self.p['commandId'],self.backend,
                                        api=self.api,transport=self.transport)

    def resolve(self,evidence):
        return self.store.resolve_incomplete_operation(self.identifier,self.operation['payload_hash'],evidence,
                                                       'test-operator','Pool permission restored; no clone or VM; registration and snippets retained')

    def test_check_only_preserves_unknown_and_never_leaks_password_hash(self):
        evidence=self.inspect()
        self.assertTrue(evidence['poolReady']);self.assertTrue(evidence['snippetsPreserved'])
        self.assertNotIn('fixture-private-hash',json.dumps(evidence))
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        self.backend.call.assert_not_called()

    def test_audited_failure_preserves_registration_and_allows_new_prepare(self):
        response=self.resolve(self.inspect());self.assertEqual('FAILED',response['status'])
        op=self.store.row('SELECT * FROM operations WHERE id=?',self.identifier)
        self.assertEqual('RECOVERED_PREPARE_INCOMPLETE',op['reason']);self.assertIsNone(op['result'])
        self.assertEqual(3,len(self.store.rows('SELECT * FROM helper_steps WHERE operation_id=?',self.identifier)))
        self.assertEqual('BLOCKED',self.store.row('SELECT stage FROM jobs WHERE job_id=747021')['stage'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM recovery_audits')))
        fresh=self.store.submit(dict(self.p,commandId='job:747021:attempt:1:r0'))
        self.assertNotEqual(self.identifier,fresh['id'])

    def test_return_only_entry_point_cannot_resolve_prepare(self):
        with self.assertRaisesRegex(Problem,'RECOVERY_NOT_SUPPORTED'):
            self.store.resolve_incomplete_return(self.identifier,self.operation['payload_hash'],self.inspect(),'operator','Not a RETURN profile')

    def test_pool_not_ready_or_not_empty_is_refused(self):
        self.permissions={}
        with self.assertRaisesRegex(Problem,'RECOVERY_POOL_NOT_READY'):self.inspect()
        self.permissions={'/pool/allday-node57':{'VM.Allocate':1,'Pool.Audit':1}};self.pool['members']=[{'vmid':10001}]
        with self.assertRaisesRegex(Problem,'RECOVERY_POOL_NOT_EMPTY'):self.inspect()

    def test_clone_source_task_or_registered_effect_is_refused(self):
        self.history=[{'type':'qmclone','id':'100'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_CLONE_TASK_EXISTS'):self.inspect()
        self.history=[];self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,'clone','UPID:clone','RUNNING')
        with self.assertRaisesRegex(Problem,'RECOVERY_PHYSICAL_EFFECT_EXISTS'):self.inspect()

    def test_remaining_vm_and_disks_are_refused(self):
        self.vms.append({'vmid':10000})
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_STILL_PRESENT'):self.inspect()
        self.vms.pop();self.content=[{'vmid':10000,'volid':'local-lvm:vm-10000-disk-0'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_VOLUMES_STILL_PRESENT'):self.inspect()

    def test_claimed_helper_or_live_task_is_refused(self):
        self.helper['ownerRentalId']=20
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()
        self.helper['ownerRentalId']=None;self.tasks=[{'type':'vzdump'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_ACTIVE_TASKS'):self.inspect()

    def test_missing_snippet_or_uncertain_helper_evidence_is_refused(self):
        self.checks[-1]['remaining'].pop()
        with self.assertRaisesRegex(Problem,'RECOVERY_SNIPPETS_UNCONFIRMED'):self.inspect()
        self.checks[-1]['remaining'].append('allday-r20-meta.yaml');self.checks[0]['status']='UNKNOWN'
        with self.assertRaisesRegex(Problem,'RECOVERY_PHYSICAL_STATE_UNCONFIRMED'):self.inspect()

    def test_valid_old_lease_blocks_recovery(self):
        self.backend.query.side_effect=None;self.backend.query.return_value={}
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_STILL_VALID'):self.inspect()


if __name__=='__main__':unittest.main()
