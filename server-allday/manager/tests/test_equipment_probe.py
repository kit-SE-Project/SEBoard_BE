import importlib.util,json,unittest
from pathlib import Path
from unittest.mock import Mock,patch

path=Path(__file__).resolve().parents[1]/'scripts/probe_equipment.py'
spec=importlib.util.spec_from_file_location('equipment_probe',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)


class EquipmentProbeTests(unittest.TestCase):
    def test_probe_uses_read_only_requests_and_omits_template_passwords(self):
        backend=Mock();backend.call.return_value=[{'id':57,'nodeName':'pve01'}]
        api=Mock()
        def call(method,path,body=None):
            self.assertEqual('GET',method)
            if path.endswith('/config'):return {'template':1,'cipassword':'NEVER_EXPORT_THIS_SECRET','sshkeys':'DO_NOT_EXPORT','ide2':'local-lvm:vm-100-cloudinit'}
            return [] if path.endswith('/qemu') or path.endswith('/network') else {'uptime':1}
        api.call.side_effect=call
        def transport(request):return {'mode':'system','ok':True,'serverId':57,'commandId':request['commandId'],'operation':'READ_STATUS','result':{'generation':0,'accessEnabled':False,'needsRecovery':False}}
        with patch.object(module,'HttpClient',return_value=backend),patch.object(module,'PveClient',return_value=api),patch.object(module,'HelperTransport',return_value=transport):
            report=module.probe({'backend_url':'https://example.test','worker_token':'private','proxmox':{},'nodes':{'57':{'helper':{}}},'server_ids':[57]})
        self.assertTrue(report['readOnly']);self.assertFalse(report['vmMutationsPerformed']);self.assertTrue(report['allConnectionsResponded'])
        self.assertNotIn('NEVER_EXPORT',json.dumps(report));self.assertNotIn('DO_NOT_EXPORT',json.dumps(report));self.assertNotIn('private',json.dumps(report))

    def test_missing_real_settings_are_reported_without_changing_demo_configuration(self):
        config={'mode':'DEMO'};report=module.probe(config)
        self.assertEqual({'mode':'DEMO'},config);self.assertFalse(report['allConnectionsResponded']);self.assertEqual(3,len(report['checks']))
