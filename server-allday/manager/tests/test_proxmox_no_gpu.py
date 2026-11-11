"""Exercise real adapter decisions with a fake PVE API; never touches equipment."""
import json
import unittest
from unittest.mock import Mock
from manager.proxmox import ProxmoxDriver
from manager.common import Problem


class NoGpuProxmoxTests(unittest.TestCase):
    def setUp(self):
        self.driver=object.__new__(ProxmoxDriver)
        self.node={'node_name':'pve01','gpu_enabled':False,'gpu_mapping':None,'gateway':'10.10.0.1','vm_bridge':'vmbr1','public_ssh_port':2201}
        self.driver.nodes={'57':self.node}
        self.driver.api=Mock()
        self.driver.gateway=Mock();self.driver.gateway.call.side_effect=lambda action,*args:{'accessBlocked':True,'accessEnabled':action=='open'}
        self.cfg={'description':'Server Allday rental=7 server=57','net0':'virtio=AA:BB:CC:DD:EE:FF,bridge=vmbr0,firewall=1'}
        self.driver.owned=Mock(side_effect=lambda *args:self.cfg)
        self.driver.task=Mock(return_value='UPID:fixture')
        self.driver.fingerprint=Mock(return_value='SHA256:fixture')
        def call(method,path,body=None):
            if path.endswith('/status/current'):return {'status':'running' if self.kind=='START' else 'stopped'}
            if method=='PUT':
                for key in (body.get('delete') or '').split(','):self.cfg.pop(key,None)
                self.cfg.update({k:v for k,v in body.items() if k!='delete'})
            return {}
        self.driver.api.call.side_effect=call
        self.context={'server':{'id':57,'inventorySource':'MANUAL','nodeName':'pve01','gpuEnabled':False,'gpuMappingRef':None,'vmSubnet':'10.10.0.0/24'},'resources':{'manifest':None}}

    def execute(self,kind='START'):
        self.kind=kind
        p={'serverId':57,'rentalId':7,'vmid':2000,'ip':'10.10.0.10','type':kind,'accessEnabled':kind=='START'}
        return self.driver.execute({'id':'fixture','payload':json.dumps(p)},self.context,None,lambda:None)

    def test_start_has_no_pci_configuration_and_preserves_nic_with_vmbr1(self):
        result=self.execute()
        self.assertTrue(result['observation']['healthPassed'])
        self.assertTrue(result['observation']['accessEnabled'])
        updates=[c.args[2] for c in self.driver.api.call.call_args_list if c.args[0]=='PUT']
        self.assertEqual(1,len(updates));self.assertNotIn('hostpci0',updates[0])
        self.assertEqual('virtio=AA:BB:CC:DD:EE:FF,firewall=1,bridge=vmbr1',updates[0]['net0'])
        self.assertEqual('ip=10.10.0.10/24,gw=10.10.0.1',updates[0]['ipconfig0'])

    def test_disabled_gpu_rejects_inherited_pci_before_start_or_network_update(self):
        self.cfg['hostpci2']='mapping=unexpected'
        with self.assertRaisesRegex(Problem,'PVE_GPU_DISABLED_BUT_ATTACHED'):self.execute()
        self.driver.task.assert_not_called()
        self.assertFalse(any(c.args[0]=='PUT' for c in self.driver.api.call.call_args_list))

    def test_stop_without_gpu_does_not_request_nonexistent_pci_deletion(self):
        result=self.execute('STOP')
        self.assertTrue(result['observation']['gpuDetached'])
        self.assertFalse(any(c.args[0]=='PUT' for c in self.driver.api.call.call_args_list))

    def test_stop_verifies_all_owned_pci_devices_are_detached(self):
        self.cfg.update(hostpci0='mapping=old',hostpci3='mapping=another')
        self.assertTrue(self.execute('STOP')['observation']['gpuDetached'])
        self.assertEqual([],self.driver.pci_devices(self.cfg))

    def test_no_gpu_must_match_backend_and_requires_json_null_mapping(self):
        self.driver.check_source(self.context)
        self.context['server']['gpuEnabled']=True
        with self.assertRaises(Problem):self.driver.check_source(self.context)
        self.node['gpu_mapping']='null'
        with self.assertRaises(Problem):self.driver.node(57)

    def test_inventory_observation_distinguishes_cpu_and_two_gpu_nodes(self):
        self.driver.api.call.side_effect=None
        self.driver.api.call.return_value={'uptime': 30}
        cpu=self.context['server']
        self.assertEqual('AVAILABLE',self.driver.server_status(cpu))
        for server_id in (58,59):
            node_name=f'pve-{server_id}'
            mapping=f'gpu-{server_id}'
            self.driver.nodes[str(server_id)]={'node_name':node_name,'gpu_enabled':True,'gpu_mapping':mapping}
            server={'id':server_id,'nodeName':node_name,'inventorySource':'MANUAL','gpuEnabled':True,'gpuMappingRef':mapping}
            self.assertEqual('AVAILABLE',self.driver.server_status(server))
            self.driver.api.call.assert_called_with('GET',f'/nodes/{node_name}/status')
            self.assertEqual('UNKNOWN',self.driver.server_status({**server,'gpuMappingRef':'wrong-mapping'}))

    def test_gpu_start_rejects_extra_passthrough_devices_before_mutation(self):
        self.node.update(gpu_enabled=True,gpu_mapping='gpu0')
        self.context['server'].update(gpuEnabled=True,gpuMappingRef='gpu0')
        self.cfg.update(hostpci0='mapping=gpu0,pcie=1',hostpci1='mapping=foreign,pcie=1')
        with self.assertRaisesRegex(Problem,'PVE_GPU_MAPPING_MISMATCH'):
            self.execute()
        self.driver.task.assert_not_called()

    def test_legacy_gpu_mode_still_requires_matching_mapping(self):
        self.node.pop('gpu_enabled');self.node['gpu_mapping']='gpu0'
        self.assertFalse(self.driver.gpu_ready({},self.node))
        self.assertFalse(self.driver.gpu_ready({'hostpci0':'mapping=other,pcie=1'},self.node))
        self.assertTrue(self.driver.gpu_ready({'hostpci0':'mapping=gpu0,pcie=1'},self.node))


if __name__=='__main__':unittest.main()
