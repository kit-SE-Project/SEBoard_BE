"""Real recovery inspector/store with simulated PVE and authoritative RETURN; no hardware."""
import json
from pathlib import Path
import threading
from types import SimpleNamespace
import unittest
from unittest.mock import Mock
import test_start_recovery as recovery_fixtures
from manager.return_recovery import recover_for_return
from manager.worker import Worker


class ReturnConvergenceTests(unittest.TestCase):
    def setUp(self):
        self.fixture=recovery_fixtures.GenerationRejectedStartRecoveryTests('test_read_only_evidence_preserves_unknown_and_secrets')
        self.fixture.setUp();self.addCleanup(self.fixture.doCleanups)
        f=self.fixture
        config=dict(f.cfg,auto_recover_return=True)
        helper=SimpleNamespace(locks={'57':threading.RLock()},transports={'57':f.transport})
        agent=SimpleNamespace(driver=SimpleNamespace(helper=helper,api=f.api))
        self.worker=Worker(f.store,f.backend,agent,config)
        self.worker.log_return_recovery=Mock();self.worker.log_blocked=Mock()

    def test_admin_or_expiry_return_can_converge_known_failed_start_without_manual_unlock(self):
        f=self.fixture;recover_for_return(self.worker)
        op=f.store.row('SELECT * FROM operations WHERE id=?',f.identifier)
        self.assertEqual('FAILED',op['status']);self.assertEqual('RECOVERED_START_INCOMPLETE',op['reason'])
        self.assertEqual('DEFERRED',f.store.row('SELECT stage FROM jobs WHERE job_id=759286')['stage'])
        self.assertEqual(1,len(f.store.rows('SELECT * FROM recovery_audits')))
        self.assertEqual(1,len(list((Path(f.store.path).parent/'recovery-backups').glob('*.sqlite3'))))
        self.worker.log_return_recovery.assert_called_once()
        # Only observations happened; no API call to start/delete/claim/report success.
        self.assertTrue(all(c.args[0]=='GET' for c in f.api.call.call_args_list))
        self.assertTrue(all(c.args[0]=='GET' for c in f.backend.call.call_args_list))
        recover_for_return(self.worker);self.assertEqual(1,len(f.store.rows('SELECT * FROM recovery_audits')))

    def test_active_backup_keeps_lock_and_never_claims_success(self):
        f=self.fixture;f.tasks=[{'type':'vzdump'}];recover_for_return(self.worker)
        self.assertEqual('UNKNOWN',f.store.row('SELECT status FROM operations WHERE id=?',f.identifier)['status'])
        self.assertEqual([],f.store.rows('SELECT * FROM recovery_audits'))
        self.worker.log_return_recovery.assert_not_called()

    def test_console_only_allows_audited_failed_start_to_converge(self):
        from manager.pve_tasks import TASK_POLICY
        f=self.fixture;f.tasks=[{'type':'vncshell'}]
        f.helper.update(activeTaskCount=1,blockingTaskCount=0,observationalTaskCount=1,taskPolicy=TASK_POLICY)
        recover_for_return(self.worker)
        self.assertEqual('FAILED',f.store.row('SELECT status FROM operations WHERE id=?',f.identifier)['status'])
        self.assertEqual(1,len(f.store.rows('SELECT * FROM recovery_audits')))

    def test_no_return_intent_or_foreign_rental_cannot_unlock(self):
        f=self.fixture;f.return_claim['rentalId']=21;recover_for_return(self.worker)
        self.assertEqual('UNKNOWN',f.store.row('SELECT status FROM operations WHERE id=?',f.identifier)['status'])

    def test_disabled_profile_and_uncertain_physical_state_cannot_unlock(self):
        f=self.fixture;self.worker.config['auto_recover_return']=False;recover_for_return(self.worker)
        f.backend.call.assert_not_called()
        self.worker.config['auto_recover_return']=True;f.helper['generation']=4;recover_for_return(self.worker)
        self.assertEqual('UNKNOWN',f.store.row('SELECT status FROM operations WHERE id=?',f.identifier)['status'])

    def test_running_agent_is_never_recovered(self):
        f=self.fixture;f.store.finish(f.identifier,'RUNNING');recover_for_return(self.worker)
        self.assertEqual('RUNNING',f.store.row('SELECT status FROM operations WHERE id=?',f.identifier)['status'])


if __name__=='__main__':unittest.main()
