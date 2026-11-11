import unittest
from manager.common import Problem
from manager.worker import Worker


class WorkerDiagnosticTests(unittest.TestCase):
    def test_blocked_job_logs_public_code_without_context(self):
        worker=object.__new__(Worker)
        with self.assertLogs('allday-manager.worker',level='WARNING') as log:
            worker.log_blocked(759287,'CLAIMED',Problem('RESOURCE_OUTCOME_UNKNOWN',409))
        self.assertIn('jobId=759287',log.output[0]);self.assertIn('code=RESOURCE_OUTCOME_UNKNOWN',log.output[0])

    def test_arbitrary_remote_error_text_is_not_logged(self):
        worker=object.__new__(Worker)
        with self.assertLogs('allday-manager.worker',level='WARNING') as log:
            worker.log_blocked(759287,'CLAIMED',Problem('Bearer fixture-secret\nprivate-body',409))
        self.assertIn('code=REMOTE_ERROR',log.output[0]);self.assertNotIn('fixture-secret',log.output[0]);self.assertNotIn('private-body',log.output[0])


if __name__=='__main__':unittest.main()
