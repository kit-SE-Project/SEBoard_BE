import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock

from manager.common import Problem,RemoteProblem,canonical,now
from manager.helper_client import HelperClient
from manager.recovery import inspect_incomplete_return,inspect_deleted_return
from manager.store import Store


class ReturnRecoveryTests(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        self.store=Store(Path(temp.name)/'state.sqlite3');HelperClient(self.store,{'57':Mock()})
        self.p={'commandId':'job:744324:attempt:0:r1','jobId':744324,'serverId':57,'rentalId':18,
                'desiredRevision':1,'type':'RETURN','vmid':10000,'ip':'10.10.0.101','gpuGeneration':2,'accessEnabled':False}
        prepared=self.store.submit(dict(self.p,type='PREPARE',commandId='prepare',jobId=1))
        self.volumes=['local-lvm:vm-10000-cloudinit','local-lvm:vm-10000-disk-0']
        self.store.finish(prepared['id'],'SUCCEEDED',{'manifest':{'volumeIds':self.volumes}})
        self.operation=self.store.submit(self.p);self.identifier=self.operation['id']
        self.store.finish(self.identifier,'UNKNOWN',reason='PVE_HTTP_ERROR')
        self.store.job(744324,self.p['commandId'],'BLOCKED',{'id':744324},self.identifier)
        self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,'return-stop','UPID:stop','SUCCEEDED')
        self.store.execute('INSERT INTO helper_workflows VALUES (?,?,?,?,?,?)',self.identifier,57,18,3,2,2)
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,'initial-close','{}',canonical({'enabled':False,'oldConnectionsCleared':True}))
        self.cfg={'mode':'PROXMOX','gateway_protocol':'NODE_HELPER_V02','worker_id':'test-worker','server_ids':[57],
                  'nodes':{'57':{'node_name':'pve01','helper':{}}}}
        self.backend=Mock();self.backend.query.side_effect=RemoteProblem('WORKER_LEASE_INVALID',409)
        self.vm={'description':'Server Allday rental=18 server=57','scsi0':'local-lvm:vm-10000-disk-0,size=32G',
                 'ide2':'local-lvm:vm-10000-cloudinit,media=cdrom'}
        self.vm_state='stopped';self.tasks=[];self.history=[{'type':'qmstop'}]
        self.helper={'ownerRentalId':18,'generation':2,'controlRevision':3,'accessEnabled':False,
                     'needsRecovery':False,'pendingCleanup':[],'activeTaskCount':0,
                     'ingress':{'target':None,'mappingCount':0,'leaseValid':False}}
        self.api=Mock();self.api.call.side_effect=self.api_call

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if 'source=active' in path:return copy.deepcopy(self.tasks)
        if 'source=all' in path:return copy.deepcopy(self.history)
        if '/tasks/' in path and path.endswith('/status'):return {'status':'stopped','exitstatus':'OK'}
        if path.endswith('/config'):return copy.deepcopy(self.vm)
        if path.endswith('/status/current'):return {'status':self.vm_state}
        self.fail(path)

    def transport(self,request):
        self.assertEqual('READ_STATUS',request['operation']);self.assertEqual({},request['payload'])
        return dict(request,mode='system',ok=True,result=copy.deepcopy(self.helper))

    def inspect(self):
        return inspect_incomplete_return(self.store,self.cfg,self.identifier,self.p['commandId'],self.backend,
                                         api=self.api,transport=self.transport)

    def resolve(self,evidence=None):
        return self.store.resolve_incomplete_return(self.identifier,self.operation['payload_hash'],evidence or self.inspect(),
                                                   'test-operator','Observed incomplete return; backend FAILED confirmed')

    def test_check_only_has_no_mutation(self):
        evidence=self.inspect()
        self.assertTrue(evidence['vmStopped']);self.assertTrue(evidence['accessBlocked'])
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        self.assertEqual([],self.store.rows('SELECT * FROM recovery_audits'))
        self.backend.call.assert_not_called()

    def test_resolution_preserves_records_and_unlocks_only_with_failure_audit(self):
        old=self.store.row('SELECT * FROM operations WHERE id=?',self.identifier)
        response=self.resolve();row=self.store.row('SELECT * FROM operations WHERE id=?',self.identifier)
        self.assertEqual('FAILED',row['status']);self.assertFalse(response['alreadyResolved'])
        for key in ['id','command_id','payload','payload_hash','result']:self.assertEqual(old[key],row[key])
        self.assertEqual('BLOCKED',self.store.row('SELECT stage FROM jobs WHERE job_id=744324')['stage'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM effects WHERE operation_id=?',self.identifier)))
        self.assertEqual(1,len(self.store.rows('SELECT * FROM helper_workflows WHERE operation_id=?',self.identifier)))
        audit=self.store.row('SELECT * FROM recovery_audits WHERE operation_id=?',self.identifier)
        self.assertEqual(('UNKNOWN','FAILED','PVE_HTTP_ERROR'),(audit['previous_status'],audit['resolution'],audit['previous_reason']))
        self.assertNotIn('verification',json.loads(audit['evidence']))
        # Same job with the backend's new command can now get a new local operation.
        fresh=self.store.submit(dict(self.p,commandId='job:744324:attempt:1:r1'))
        self.assertNotEqual(self.identifier,fresh['id'])

    def test_resolution_is_idempotent(self):
        evidence=self.inspect();first=self.resolve(evidence);again=self.resolve(evidence)
        self.assertEqual(first['auditId'],again['auditId']);self.assertTrue(again['alreadyResolved'])
        self.assertEqual(1,len(self.store.rows('SELECT * FROM recovery_audits')))

    def test_active_vm_and_task_are_refused(self):
        for state,tasks in [('running',[]),('stopped',[{'type':'qmdestroy'}])]:
            with self.subTest(state=state,tasks=tasks):
                self.vm_state=state;self.tasks=tasks
                with self.assertRaises(Problem):self.inspect()

    def test_wrong_owner_and_changed_manifest_are_refused(self):
        for change in [{'description':'foreign-owner'},{'scsi0':'local-lvm:vm-10001-disk-0'},
                       {'unused0':'local-lvm:vm-10000-unrecorded'}]:
            with self.subTest(change=change):
                old=copy.deepcopy(self.vm);self.vm.update(change)
                with self.assertRaises(Problem):self.inspect()
                self.vm=old

    def test_helper_open_or_changed_generation_is_refused(self):
        for change in [{'accessEnabled':True},{'ownerRentalId':19},{'generation':3},
                       {'controlRevision':4},{'needsRecovery':True},{'pendingCleanup':['pending']}]:
            with self.subTest(change=change):
                old=copy.deepcopy(self.helper);self.helper.update(change)
                with self.assertRaises(Problem):self.inspect()
                self.helper=old

    def test_remaining_mapping_is_refused_even_if_access_flag_false(self):
        self.helper['ingress']['mappingCount']=1
        with self.assertRaises(Problem):self.inspect()

    def test_valid_old_lease_is_refused(self):
        self.backend.query.side_effect=None;self.backend.query.return_value={}
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_STILL_VALID'):self.inspect()

    def test_backend_transport_error_is_not_lease_proof(self):
        self.backend.query.side_effect=RemoteProblem('TRANSPORT_UNCERTAIN',503)
        with self.assertRaisesRegex(Problem,'RECOVERY_OLD_LEASE_UNCONFIRMED'):self.inspect()

    def test_previous_delete_submission_and_task_history_require_other_procedure(self):
        self.history=[{'type':'qmdestroy','status':'OK'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_DELETE_TASK_EXISTS'):self.inspect()
        self.history=[]
        self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,'delete','UPID:delete','RUNNING')
        with self.assertRaisesRegex(Problem,'RECOVERY_DELETE_ALREADY_SUBMITTED'):self.inspect()

    def test_stale_and_tampered_evidence_does_not_unlock(self):
        for change in [{'checkedAt':'2000-01-01T00:00:00+00:00'},{'accessBlocked':False},{'rentalId':19}]:
            with self.subTest(change=change):
                evidence=self.inspect();evidence.update(change)
                with self.assertRaises(Problem):self.resolve(evidence)
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])


class DeletedReturnRecoveryTests(unittest.TestCase):
    def setUp(self):
        self.fixture=ReturnRecoveryTests('test_check_only_has_no_mutation');self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)
        f=self.fixture;self.store=f.store;self.identifier=f.identifier
        self.delete_upid='UPID:pve01:fixture:qmdestroy:10000:test:'
        self.store.finish(self.identifier,'UNKNOWN',reason='HELPER_TASK_IN_PROGRESS')
        self.store.execute('DELETE FROM effects WHERE operation_id=?',self.identifier)
        self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',self.identifier,'delete',self.delete_upid,'SUCCEEDED')
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,'remove-snippets',
                           canonical({'operation':'REMOVE_SNIPPET','payload':{'rentalId':18}}),None)
        self.task={'status':'stopped','exitstatus':'OK','type':'qmdestroy','id':'10000'}
        self.vms=[{'vmid':100,'template':1,'status':'stopped'}];self.content=[]
        f.api.call.side_effect=self.api_call

    def api_call(self,method,path):
        self.assertEqual('GET',method)
        if 'source=active' in path:return copy.deepcopy(self.fixture.tasks)
        if '/tasks/' in path and path.endswith('/status'):return copy.deepcopy(self.task)
        if path.endswith('/qemu'):return copy.deepcopy(self.vms)
        if '/storage/' in path and path.endswith('/content'):return copy.deepcopy(self.content)
        self.fail(path)

    def inspect(self):
        f=self.fixture
        return inspect_deleted_return(self.store,f.cfg,self.identifier,f.p['commandId'],f.backend,
                                      api=f.api,transport=f.transport)

    def resolve(self,evidence=None):
        return self.store.resolve_incomplete_return(self.identifier,self.fixture.operation['payload_hash'],
                        evidence or self.inspect(),'test-operator','Confirmed delete; remaining Helper cleanup requires fresh backend retry')

    def test_read_only_probe_and_audited_failure_never_mark_return_success(self):
        evidence=self.inspect()
        self.assertTrue(evidence['vmAbsent']);self.assertTrue(evidence['volumesAbsent'])
        self.assertEqual('UNKNOWN',self.store.row('SELECT status FROM operations WHERE id=?',self.identifier)['status'])
        result=self.resolve(evidence)
        self.assertEqual('FAILED',result['status'])
        row=self.store.row('SELECT * FROM recovery_audits WHERE operation_id=?',self.identifier)
        self.assertEqual('HELPER_TASK_IN_PROGRESS',row['previous_reason'])
        self.assertEqual(self.delete_upid,json.loads(row['evidence'])['deleteUpid'])
        self.assertIsNone(self.store.row('SELECT result FROM operations WHERE id=?',self.identifier)['result'])
        self.assertEqual('SUCCEEDED',self.store.row("SELECT status FROM effects WHERE operation_id=? AND step='delete'",self.identifier)['status'])

    def test_vm_still_present_is_refused(self):
        self.vms.append({'vmid':10000,'status':'stopped'})
        with self.assertRaisesRegex(Problem,'RECOVERY_VM_STILL_PRESENT'):self.inspect()

    def test_registered_and_unregistered_remaining_volumes_are_refused(self):
        for volume in ['local-lvm:vm-10000-disk-0','local-lvm:vm-10000-extra']:
            self.content=[{'volid':volume}]
            with self.subTest(volume=volume), self.assertRaisesRegex(Problem,'RECOVERY_VOLUMES_STILL_PRESENT'):self.inspect()

    def test_running_failed_or_wrong_delete_task_is_refused(self):
        for change in [{'status':'running'},{'exitstatus':'ERROR'},{'type':'qmstop'},{'id':'10001'}]:
            with self.subTest(change=change):
                old=copy.deepcopy(self.task);self.task.update(change)
                with self.assertRaisesRegex(Problem,'RECOVERY_DELETE_UNCONFIRMED'):self.inspect()
                self.task=old

    def test_web_shell_or_open_ingress_is_refused(self):
        self.fixture.tasks=[{'type':'vzdump'}]
        with self.assertRaisesRegex(Problem,'RECOVERY_ACTIVE_TASKS'):self.inspect()
        self.fixture.tasks=[];self.fixture.helper['accessEnabled']=True
        with self.assertRaisesRegex(Problem,'RECOVERY_ACCESS_BLOCK_UNCONFIRMED'):self.inspect()

    def test_helper_owner_changed_is_refused(self):
        self.fixture.helper['ownerRentalId']=19
        with self.assertRaisesRegex(Problem,'RECOVERY_HELPER_OWNER_CHANGED'):self.inspect()

    def test_later_cleanup_phase_requires_different_recovery(self):
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',self.identifier,'verify','{}',None)
        with self.assertRaisesRegex(Problem,'RECOVERY_CLEANUP_PHASE_CHANGED'):self.inspect()

    def test_wrong_recovery_profile_does_not_accept_completed_delete(self):
        with self.assertRaises(Problem):self.fixture.inspect()
        evidence=self.inspect();evidence['recoveryKind']='PRE_DELETE_INCOMPLETE'
        with self.assertRaises(Problem):self.resolve(evidence)

    def test_tampered_delete_reference_cannot_unlock(self):
        evidence=self.inspect();evidence['deleteUpid']='another-task'
        with self.assertRaisesRegex(Problem,'RECOVERY_DELETE_UNCONFIRMED'):self.resolve(evidence)
        self.assertEqual([],self.store.rows('SELECT * FROM recovery_audits'))

    def audited_partial_prepare(self):
        old=self.store.row("SELECT id,payload FROM operations WHERE status='SUCCEEDED'");p=json.loads(old['payload'])
        self.store.finish(old['id'],'FAILED',reason='RECOVERED_PREPARE_INCOMPLETE')
        resource={'rentalId':p['rentalId'],'vmId':p['vmid'],'vmIp':p['ip'],'volumeIds':self.fixture.volumes}
        self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,?)',old['id'],'register-volumes',canonical({'operation':'REGISTER_RESOURCE','payload':resource}),canonical({k:resource[k] for k in ['rentalId','vmId','vmIp']}))
        evidence={'recoveryKind':'PREPARE_STARTED_TRANSPORT_INCOMPLETE','operationId':old['id'],'jobId':p['jobId'],'serverId':p['serverId'],'rentalId':p['rentalId'],'vmid':p['vmid'],'volumeIds':sorted(self.fixture.volumes),'vmOwned':True,'disksMatch':True,'tasksFinished':True,'registrationMatches':True}
        self.store.execute('INSERT INTO recovery_audits VALUES (?,?,?,?,?,?,?,?,?)','fixture-audit',old['id'],'UNKNOWN','FAILED','TRANSPORT_UNCERTAIN','test-operator','fixture audited checkpoint',canonical(evidence),now())
        return old['id'],evidence

    def test_deleted_vm_with_audited_partial_prepare_checkpoint_is_supported(self):
        self.audited_partial_prepare();evidence=self.inspect()
        self.assertTrue(evidence['volumesAbsent']);self.assertEqual(sorted(self.fixture.volumes),evidence['volumeIds'])
        self.assertEqual('FAILED',self.resolve(evidence)['status'])

    def test_failed_prepare_without_matching_audit_cannot_supply_manifest(self):
        old=self.store.row("SELECT id FROM operations WHERE status='SUCCEEDED'");self.store.finish(old['id'],'FAILED',reason='RECOVERED_PREPARE_INCOMPLETE')
        with self.assertRaisesRegex(Problem,'RECOVERY_MANIFEST_MISMATCH'):self.inspect()

    def test_audit_different_rental_or_volume_checkpoint_is_refused(self):
        identifier,evidence=self.audited_partial_prepare();evidence['rentalId']=999
        self.store.execute('UPDATE recovery_audits SET evidence=? WHERE operation_id=?',canonical(evidence),identifier)
        with self.assertRaisesRegex(Problem,'RECOVERY_MANIFEST_MISMATCH'):self.inspect()
        evidence['rentalId']=18;evidence['volumeIds']=['local-lvm:vm-99999-disk-0']
        self.store.execute('UPDATE recovery_audits SET evidence=? WHERE operation_id=?',canonical(evidence),identifier)
        with self.assertRaisesRegex(Problem,'RECOVERY_MANIFEST_MISMATCH'):self.inspect()


if __name__=='__main__':unittest.main()
