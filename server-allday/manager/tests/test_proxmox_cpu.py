"""CPU-only PVE workflows against a recording fake, never physical equipment."""
import copy
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock

from manager.common import Problem
from manager.proxmox import ProxmoxDriver
from manager.store import Store


class FakePve:
    def __init__(self):
        self.calls=[]
        self.configs={100:{'template':1,'scsi0':'local-lvm:vm-100-disk-0'}}
        self.states={100:'stopped'}

    def call(self,method,path,body=None):
        self.calls.append((method,path,copy.deepcopy(body)))
        if path=='/nodes/pve01/qemu':return [{'vmid':vmid} for vmid in self.configs]
        if '/tasks/' in path:return {'status':'stopped','exitstatus':'OK'}
        vmid=int(path.split('/qemu/')[1].split('/')[0])
        if path.endswith('/clone'):
            target=body['newid']
            self.configs[target]={'description':body['description'],'scsi0':f'local-lvm:vm-{target}-disk-0'}
            self.states[target]='stopped'
            return 'UPID:clone'
        if path.endswith('/config'):
            if method=='GET':return copy.deepcopy(self.configs[vmid])
            for key,value in body.items():
                if key=='delete':self.configs[vmid].pop(value,None)
                else:self.configs[vmid][key]=value
            return None
        if path.endswith('/status/current'):return {'status':self.states[vmid]}
        if path.endswith('/status/start') or path.endswith('/status/stop'):
            self.states[vmid]='running' if path.endswith('/start') else 'stopped'
            return 'UPID:power'
        if path.endswith('/agent/ping') or path.endswith('/agent/set-user-password'):return None
        raise AssertionError((method,path,body))


class CpuProxmoxTest(unittest.TestCase):
    def setUp(self):
        temporary=tempfile.TemporaryDirectory();self.addCleanup(temporary.cleanup)
        self.store=Store(Path(temporary.name)/'state.sqlite3')
        self.node={'node_name':'pve01','gpu_enabled':False,'gpu_mapping':None,
                   'gateway':'10.10.0.1','public_ssh_port':2201}
        self.driver=object.__new__(ProxmoxDriver)
        self.driver.nodes={'1':self.node};self.driver.store=self.store;self.driver.api=FakePve()
        self.driver.gateway=Mock()
        self.driver.gateway.call.side_effect=lambda action,*args: {'accessBlocked':action!='open','accessEnabled':action=='open'}
        self.driver.fingerprint=Mock(return_value='SHA256:test')
        self.context={'server':{'id':1,'inventorySource':'MANUAL','nodeName':'pve01',
                                'gpuEnabled':False,'gpuMappingRef':None,'vmSubnet':'10.10.0.0/24'},
                      'template':{'templateVmid':100,'approvedAt':'approved','deprecatedAt':None}}
        self.p={'commandId':'cpu-prepare','jobId':1,'serverId':1,'rentalId':1,
                'desiredRevision':0,'type':'PREPARE','vmid':2000,'ip':'10.10.0.101',
                'gpuGeneration':0,'accessEnabled':True}

    def execute(self,kind='PREPARE'):
        p=self.p|{'type':kind,'commandId':'cpu-'+kind.lower(),'accessEnabled':kind in ('PREPARE','START')}
        op=self.store.submit(p)
        result=self.driver.execute(op,self.context,{'username':'student','password':'test-only','credentialVersion':1},Mock())
        self.store.finish(op['id'],'SUCCEEDED',result)
        return result

    def assert_code(self,code,fn):
        with self.assertRaises(Problem) as raised:fn()
        self.assertEqual(code,raised.exception.code)

    def test_explicit_cpu_setting_and_legacy_gpu_default(self):
        self.assertIs(self.node,self.driver.node(1))
        self.node.pop('gpu_enabled')
        self.assert_code('NODE_NOT_CONFIGURED',lambda:self.driver.node(1))
        self.node['gpu_mapping']='training-gpu'
        self.assertIs(self.node,self.driver.node(1))

    def test_cpu_rejects_gpu_mapping_and_non_boolean_flag(self):
        self.node['gpu_mapping']='training-gpu'
        self.assert_code('NODE_NOT_CONFIGURED',lambda:self.driver.node(1))
        self.node.update(gpu_mapping=None,gpu_enabled='false')
        self.assert_code('NODE_NOT_CONFIGURED',lambda:self.driver.node(1))

    def test_cpu_rejects_demo_or_backend_gpu_inventory(self):
        for change in [{'inventorySource':'DEMO'},{'gpuMappingRef':'training-gpu'}]:
            with self.subTest(change=change):
                context=copy.deepcopy(self.context);context['server'].update(change)
                self.assert_code('REAL_INVENTORY_MISMATCH',lambda:self.driver.check_source(context))
        self.assertEqual([],self.driver.api.calls)

    def test_cpu_prepare_clones_configures_starts_and_opens_without_pci(self):
        result=self.execute()
        self.assertFalse(result['simulation'])
        self.assertTrue(result['observation']['healthPassed'])
        self.assertTrue(result['observation']['accessEnabled'])
        settings=[body for method,path,body in self.driver.api.calls if method=='PUT' and path.endswith('/config')]
        self.assertEqual(1,len(settings))
        self.assertEqual('student',settings[0]['ciuser'])
        self.assertEqual('ip=10.10.0.101/24,gw=10.10.0.1',settings[0]['ipconfig0'])
        self.assertFalse(any(key.startswith('hostpci') for key in settings[0]))
        self.assertEqual('running',self.driver.api.states[2000])
        clone=next(i for i,call in enumerate(self.driver.api.calls) if call[1].endswith('/clone'))
        start=next(i for i,call in enumerate(self.driver.api.calls) if call[1].endswith('/status/start'))
        self.assertLess(clone,start)

    def test_cpu_stop_and_restart_do_not_delete_or_add_pci(self):
        self.execute();stopped=self.execute('STOP');self.execute('START')
        self.assertEqual('STOPPED',stopped['observation']['vmState'])
        self.assertTrue(stopped['observation']['gpuDetached'])
        settings=[body for method,path,body in self.driver.api.calls if method=='PUT']
        self.assertTrue(all('hostpci0' not in body and 'delete' not in body for body in settings))

    def test_cpu_template_with_any_passthrough_is_rejected_before_clone(self):
        self.driver.api.configs[100]['hostpci1']='0000:00:02.0'
        self.assert_code('PVE_GPU_DISABLED_BUT_ATTACHED',self.execute)
        self.assertFalse(any(path.endswith('/clone') for _,path,_ in self.driver.api.calls))

    def test_cpu_existing_vm_with_passthrough_is_not_started(self):
        self.execute();self.driver.api.configs[2000]['hostpci2']='0000:00:02.0'
        self.driver.api.calls.clear()
        self.assert_code('PVE_GPU_DISABLED_BUT_ATTACHED',lambda:self.execute('START'))
        self.assertFalse(any(method in ('PUT','POST','DELETE') for method,_,_ in self.driver.api.calls))

    def test_cpu_health_closes_access_if_passthrough_appears(self):
        self.execute()
        target={'serverId':1,'rentalId':1,'vmid':2000,'accessAllowed':True}
        self.assertTrue(self.driver.health(target)['healthPassed'])
        self.driver.api.configs[2000]['hostpci1']='0000:00:02.0'
        report=self.driver.health(target)
        self.assertFalse(report['healthPassed']);self.assertFalse(report['accessEnabled'])
        self.assertEqual('close',self.driver.gateway.call.call_args.args[0])

    def test_gpu_mode_still_attaches_and_detaches_expected_mapping(self):
        self.node.update(gpu_enabled=True,gpu_mapping='training-gpu')
        self.context['server']['gpuEnabled']=True
        self.context['server']['gpuMappingRef']='training-gpu'
        self.assertTrue(self.execute()['observation']['healthPassed'])
        self.assertEqual('mapping=training-gpu,pcie=1',self.driver.api.configs[2000]['hostpci0'])
        self.execute('STOP')
        self.assertNotIn('hostpci0',self.driver.api.configs[2000])

    def test_gpu_health_does_not_open_missing_or_wrong_mapping(self):
        self.node.update(gpu_enabled=True,gpu_mapping='training-gpu')
        self.context['server']['gpuEnabled']=True
        self.context['server']['gpuMappingRef']='training-gpu'
        self.execute()
        target={'serverId':1,'rentalId':1,'vmid':2000,'accessAllowed':True}
        for value in (None,'mapping=other-gpu,pcie=1'):
            if value is None:self.driver.api.configs[2000].pop('hostpci0',None)
            else:self.driver.api.configs[2000]['hostpci0']=value
            self.assertFalse(self.driver.health(target)['accessEnabled'])
            self.assertEqual('close',self.driver.gateway.call.call_args.args[0])
