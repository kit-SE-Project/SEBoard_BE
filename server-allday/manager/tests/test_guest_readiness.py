import json
import unittest
from unittest.mock import Mock,patch

from manager.common import Problem,RemoteProblem
from manager.helper_driver import HelperProxmoxDriver


class GuestReadinessTests(unittest.TestCase):
    def setUp(self):
        self.driver=object.__new__(HelperProxmoxDriver);self.driver.api=Mock();self.guard=Mock()
        self.prefix='/nodes/pve01/qemu/10000'

    def test_guest_exec_polls_the_exact_pid_with_guard(self):
        self.driver.api.call.side_effect=[{'pid':19},{'exited':False},{'exited':True,'exitcode':0,'out-data':'fixture'}]
        with patch('manager.helper_driver.time.sleep'):
            result=self.driver.guest_command(self.prefix,['/usr/bin/sync'],self.guard)
        self.assertEqual(0,result['exitcode']);self.assertEqual(3,self.guard.call_count)
        self.assertEqual({'pid':19},self.driver.api.call.call_args.args[2])

    def test_invalid_pid_or_truncated_output_is_refused(self):
        self.driver.api.call.return_value={'pid':True}
        with self.assertRaisesRegex(Problem,'GUEST_COMMAND_UNCONFIRMED'):self.driver.guest_command(self.prefix,['fixture'],self.guard)
        self.driver.api.call.side_effect=[{'pid':1},{'exited':True,'exitcode':0,'out-truncated':True}]
        with self.assertRaisesRegex(Problem,'GUEST_COMMAND_UNCONFIRMED'):self.driver.guest_command(self.prefix,['fixture'],self.guard)

    def test_running_cloud_init_is_not_ready_even_when_agent_is_up(self):
        self.driver.guest_command=Mock(side_effect=[{'exitcode':0,'out-data':json.dumps({'status':'running'})},
                                                   {'exitcode':0,'out-data':json.dumps({'status':'done','errors':[]})}])
        with patch('manager.helper_driver.time.sleep'):
            self.driver.wait_cloud_init(self.prefix,self.guard)
        self.assertEqual(2,self.driver.guest_command.call_count)

    def test_deprecation_only_cloud_init_done_is_allowed(self):
        self.driver.guest_command=Mock(return_value={'exitcode':2,'out-data':json.dumps({'status':'done','errors':[],'recoverable_errors':{'DEPRECATED':['fixture']}})})
        self.driver.wait_cloud_init(self.prefix,self.guard)

    def test_cloud_init_failure_disabled_or_invalid_response_is_refused(self):
        for state in [{'status':'disabled'},{'status':'error'},{'status':'done','errors':['fixture-error']}]:
            self.driver.guest_command=Mock(return_value={'exitcode':0,'out-data':json.dumps(state)})
            with self.assertRaisesRegex(Problem,'CLOUD_INIT_FAILED'):self.driver.wait_cloud_init(self.prefix,self.guard)
        self.driver.guest_command=Mock(return_value={'exitcode':0,'out-data':'not-json'})
        with self.assertRaisesRegex(Problem,'CLOUD_INIT_UNCONFIRMED'):self.driver.wait_cloud_init(self.prefix,self.guard)

    def test_late_ip_assignment_is_polled_instead_of_single_shot_failure(self):
        self.driver.api.call.side_effect=[{'result':[{'ip-addresses':[]}]},RemoteProblem('PVE_HTTP_ERROR',500),
                                          {'result':[{'ip-addresses':[{'ip-address':'10.10.0.101'}]}]}]
        with patch('manager.helper_driver.time.sleep'):
            self.driver.wait_guest_ip(self.prefix,'10.10.0.101',self.guard)
        self.assertEqual(3,self.guard.call_count)

    def test_guest_command_timeout_fails_closed(self):
        self.driver.api.call.side_effect=[{'pid':1},{'exited':False}]
        with patch('manager.helper_driver.time.monotonic',side_effect=[0,0,2]),patch('manager.helper_driver.time.sleep'):
            with self.assertRaisesRegex(Problem,'GUEST_COMMAND_TIMEOUT'):self.driver.guest_command(self.prefix,['fixture'],self.guard,timeout=1)

    def test_missing_ip_and_cloud_init_are_bounded(self):
        with self.assertRaisesRegex(Problem,'GUEST_IP_UNCONFIRMED'):self.driver.wait_guest_ip(self.prefix,'10.10.0.101',self.guard,timeout=0)
        with self.assertRaisesRegex(Problem,'CLOUD_INIT_NOT_READY'):self.driver.wait_cloud_init(self.prefix,self.guard,timeout=0)


if __name__=='__main__':unittest.main()
