import json,tempfile,time,unittest
from pathlib import Path
from unittest.mock import Mock
from manager.common import Problem,RemoteProblem
from manager.store import Store
from manager.worker import Worker


class DispatchDeferralTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.store=Store(Path(self.temp.name)/'state.sqlite3');self.backend=Mock();self.agent=Mock()
        self.worker=Worker(self.store,self.backend,self.agent,{'worker_id':'fixture','mode':'PROXMOX'})
        self.worker.claim_due=Mock();self.worker.health=Mock();self.worker.last_health=time.monotonic()
        self.claim={'id':77,'serverId':57,'rentalId':21,'type':'PREPARE','desiredRevision':0,'commandId':'job:77:attempt:1:r0','status':'RUNNING'}
        self.store.job(77,self.claim['commandId'],'CLAIMED',self.claim)
        self.backend.query.side_effect=RemoteProblem('GPU_NOT_FREE',409)
        self.backend.call.return_value={'status':'PENDING'}
    def row(self):return self.store.row('SELECT * FROM jobs WHERE job_id=?',77)
    def test_preflight_conflict_releases_execution_lease_without_submitting_vm_work(self):
        self.worker.tick()
        self.assertEqual('DEFERRED',self.row()['stage']);self.agent.submit.assert_not_called()
        request=self.backend.call.call_args.args
        self.assertEqual('/api/internal/jobs/77/dispatch-deferred',request[1]);self.assertTrue(request[2]['dispatchNotStarted'])
        self.assertEqual('GPU_NOT_FREE',request[2]['reasonCode'])
    def test_temporary_server_observation_failure_also_defers(self):
        self.backend.query.side_effect=RemoteProblem('SERVER_UNAVAILABLE',503)
        self.worker.tick();self.assertEqual('DEFERRED',self.row()['stage']);self.agent.submit.assert_not_called()
    def test_lost_deferral_response_keeps_claim_for_replay(self):
        self.backend.call.side_effect=RemoteProblem('TRANSPORT_UNCERTAIN',503)
        self.worker.tick();self.assertEqual('CLAIMED',self.row()['stage'])
        self.backend.call.side_effect=None
        self.worker.tick();self.assertEqual('DEFERRED',self.row()['stage'])
    def test_existing_agent_operation_prevents_false_not_dispatched_claim(self):
        operation=self.store.submit({'commandId':self.claim['commandId'],'serverId':57})
        self.store.finish(operation['id'],'UNKNOWN',reason='PVE_TRANSPORT_UNCERTAIN')
        self.worker.tick()
        self.assertFalse(any(c.args[1].endswith('/dispatch-deferred') for c in self.backend.call.call_args_list))
        self.assertEqual('CLAIMED',self.row()['stage'])
    def test_legacy_blocked_predispatch_record_is_recovered_once(self):
        self.store.job(77,self.claim['commandId'],'BLOCKED',self.claim)
        self.worker.recover_undispatched();self.assertEqual('DEFERRED',self.row()['stage'])
        count=self.backend.call.call_count;self.worker.recover_undispatched();self.assertEqual(count,self.backend.call.call_count)
    def test_actual_agent_error_code_is_reported_without_changing_uncertainty_flags(self):
        operation=self.store.submit({'commandId':self.claim['commandId'],'serverId':57})
        self.store.finish(operation['id'],'UNKNOWN',reason='GUEST_IP_UNCONFIRMED')
        self.store.job(77,self.claim['commandId'],'WAITING_AGENT',self.claim,operation['id'])
        self.worker.tick()
        report=[c.args[2] for c in self.backend.call.call_args_list if c.args[1].endswith('/failure')][0]
        self.assertEqual('GUEST_IP_UNCONFIRMED',report['reasonCode']);self.assertFalse(report['outcomeKnown']);self.assertFalse(report['accessBlocked'])
    def test_one_claim_per_server_keeps_start_from_overtaking_prepare(self):
        self.store.execute('DELETE FROM jobs')
        first=self.claim|{'status':'PENDING'};second=first|{'id':78,'commandId':'job:78:attempt:0:r0','type':'START'}
        def call(method,path,body=None):
            if path.endswith('/inventory'):return [{'id':57,'inventorySource':'MANUAL'}]
            if path=='/api/internal/jobs':return [first,second]
            if path.endswith('/claim'):return self.claim
            raise AssertionError(path)
        self.backend.call.side_effect=call
        Worker.claim_due(self.worker)
        claimed=[c.args[1] for c in self.backend.call.call_args_list if c.args[1].endswith('/claim')]
        self.assertEqual(['/api/internal/jobs/77/claim'],claimed)
    def test_lost_claim_response_with_expired_lease_uses_positive_no_dispatch_evidence(self):
        pending=self.claim|{'status':'PENDING'};self.store.job(77,pending['commandId'],'CLAIMING',pending)
        def call(method,path,body=None):
            if path.endswith('/claim'):raise RemoteProblem('WORKER_LEASE_INVALID',409)
            if path.endswith('/dispatch-deferred'):return {'status':'PENDING'}
            raise AssertionError(path)
        self.backend.call.side_effect=call
        self.assertFalse(self.worker.claim_one(pending));self.assertEqual('DEFERRED',self.row()['stage'])
        self.agent.submit.assert_not_called()


if __name__=='__main__':unittest.main()
