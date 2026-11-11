import unittest
from unittest.mock import Mock

from manager.common import Problem
from manager.proxmox import ProxmoxDriver


class RentalPoolTests(unittest.TestCase):
    def setUp(self):
        self.driver=object.__new__(ProxmoxDriver)
        self.node={'node_name':'pve01','gpu_enabled':False,'gpu_mapping':None,'vm_pool':'allday-node57'}
        self.driver.nodes={'57':self.node};self.driver.api=Mock()
        self.p={'serverId':57,'rentalId':20,'vmid':10000}

    def test_clone_uses_scoped_persistent_pool(self):
        body=self.driver.clone_parameters(self.p,self.node)
        self.assertEqual('allday-node57',body['pool'])
        self.assertEqual(10000,body['newid']);self.assertEqual(1,body['full'])
        self.assertEqual('Server Allday rental=20 server=57',body['description'])

    def test_legacy_configuration_without_pool_keeps_original_request(self):
        self.node.pop('vm_pool')
        self.assertNotIn('pool',self.driver.clone_parameters(self.p,self.node))
        self.driver.check_clone_pool(self.node);self.driver.api.call.assert_not_called()

    def test_pool_permission_replaces_deleted_per_vm_acl_for_cloning(self):
        self.driver.api.call.return_value={'/pool/allday-node57':{'VM.Allocate':1}}
        self.driver.check_clone_pool(self.node)
        self.driver.api.call.assert_called_once_with('GET','/access/permissions?path=%2Fpool%2Fallday-node57')

    def test_missing_pool_permission_fails_closed(self):
        self.driver.api.call.return_value={}
        with self.assertRaisesRegex(Problem,'PVE_RENTAL_POOL_PERMISSION_REQUIRED'):
            self.driver.check_clone_pool(self.node)

    def test_pool_path_injection_and_empty_names_rejected(self):
        for pool in ['', '../other', 'pool/other', True, 'pool?x=1']:
            self.node['vm_pool']=pool
            with self.subTest(pool=pool), self.assertRaisesRegex(Problem,'NODE_NOT_CONFIGURED'):
                self.driver.node(57)


if __name__=='__main__':unittest.main()
