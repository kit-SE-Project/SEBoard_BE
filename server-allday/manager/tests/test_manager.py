import concurrent.futures
import copy
import importlib.util
import os
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path
from types import SimpleNamespace
from http.server import ThreadingHTTPServer
from unittest.mock import Mock, patch

from manager.common import Problem, RemoteProblem, HttpClient, canonical, now
from manager.store import Store
from manager.agent import Agent, validate
from manager.demo import DemoDriver
from manager.worker import Worker
from manager.proxmox import ProxmoxDriver
from manager.server import handler, process_lock


def payload(kind='PREPARE', command='job:1:attempt:0:r0'):
    return dict(commandId=command,jobId=1,serverId=1,rentalId=1,
                desiredRevision=0,type=kind,vmid=2000,ip='192.168.101.10',
                gpuGeneration=1,accessEnabled=kind in ['PREPARE','START'])


class Backend:
    def __init__(self):
        self.p=payload();self.calls=[]
        self.credential=dict(username='student',password='TestSecret12!',credentialVersion=1)
    def context(self):
        return {'job':{'commandId':self.p['commandId'],'type':self.p['type']},
                'rental':{'id':1,'desiredRevision':0},'server':{'id':1,'inventorySource':'DEMO'},
                'template':None,'inSlot':True,
                'resources':{'allocation':{'vmid':2000},'ip':{'address':self.p['ip']},
                             'manifest':None,'gpu':{'generation':1}}}
    def query(self,path,**params):
        self.calls.append((path,params))
        return self.credential if path.endswith('pending-credential') else self.context()
    def call(self,method,path,body=None):
        self.calls.append((path,copy.deepcopy(body)))
        return {'renewed':True}


class ManagerTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.store=Store(Path(self.temp.name)/'state.sqlite3');self.backend=Backend()
        self.driver=DemoDriver(self.store);self.agent=Agent(self.store,self.backend,self.driver,'manager-test',[])
    def assert_code(self,code,fn):
        with self.assertRaises(Problem) as raised:fn()
        self.assertEqual(code,raised.exception.code)
    def execute(self,p):
        self.backend.p=p;operation=self.agent.submit(p);self.agent.run_pending()
        row=self.store.row('SELECT * FROM operations WHERE id=?',operation['id'])
        self.assertEqual('SUCCEEDED',row['status'],row['reason']);return row

    def test_concurrent_identical_commands_create_one_operation(self):
        with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
            ids=list(pool.map(lambda _:self.store.submit(payload())['id'],range(32)))
        self.assertEqual(1,len(set(ids)))
        self.assertEqual(1,len(self.store.rows('SELECT * FROM operations')))

    def test_changed_command_payload_is_rejected(self):
        self.store.submit(payload())
        self.assert_code('COMMAND_PAYLOAD_CHANGED',lambda:self.store.submit(payload()|{'vmid':2001}))

    def test_another_command_cannot_use_busy_server(self):
        self.store.submit(payload())
        self.assert_code('SERVER_OPERATION_PENDING',lambda:self.store.submit(payload(command='new')))

    def test_restart_marks_interrupted_mutation_unknown_and_blocks_server(self):
        op=self.store.submit(payload());self.store.finish(op['id'],'RUNNING')
        self.store.recover();self.agent.run_pending()
        self.assertEqual('UNKNOWN',self.store.row('SELECT * FROM operations')['status'])
        self.assertEqual([],self.store.rows('SELECT * FROM demo_vms'))
        self.assert_code('SERVER_OPERATION_PENDING',lambda:self.store.submit(payload(command='retry')))

    def test_second_process_cannot_recover_a_live_operation_store(self):
        with process_lock(self.store.path):
            with self.assertRaises(RuntimeError):
                with process_lock(self.store.path):pass

    def test_secrets_and_bool_ids_rejected_from_operation_input(self):
        for bad in [payload()|{'password':'unsafe'},payload()|{'jobId':True},payload()|{'vmid':99}]:
            with self.assertRaises(Problem):validate(bad)

    def test_stale_revision_rejected_before_mutation(self):
        self.assert_code('BACKEND_CONTEXT_MISMATCH',lambda:self.agent.submit(payload()|{'desiredRevision':2}))
        self.assertEqual([],self.store.rows('SELECT * FROM operations'))

    def test_pve_http_status_is_preserved_without_response_secrets(self):
        self.driver.execute=Mock(side_effect=RemoteProblem('PVE_HTTP_ERROR',403))
        operation=self.agent.submit(payload());self.agent.run_pending()
        row=self.store.row('SELECT * FROM operations WHERE id=?',operation['id'])
        self.assertEqual('UNKNOWN',row['status']);self.assertEqual('PVE_HTTP_ERROR',row['reason'])
        self.assertEqual({'error':{'code':'PVE_HTTP_ERROR','httpStatus':403}},json.loads(row['result']))

    def test_demo_rejects_real_inventory(self):
        self.driver.check_source({'server':{'inventorySource':'DEMO'}})
        self.assert_code('DEMO_CANNOT_TOUCH_REAL_INVENTORY',lambda:self.driver.check_source({'server':{'inventorySource':'MANUAL'}}))

    def test_ip_must_match_backend_allocation(self):
        self.assert_code('RESOURCE_CONTEXT_MISMATCH',lambda:self.agent.submit(payload()|{'ip':'192.168.101.20'}))

    def test_access_cannot_open_outside_authoritative_slot(self):
        original=self.backend.context;self.backend.context=lambda:original()|{'inSlot':False}
        self.assert_code('ACCESS_NOT_AUTHORIZED',lambda:self.agent.submit(payload()))

    def test_non_start_operation_cannot_open_access(self):
        self.backend.p=payload('STOP','stop')
        self.assert_code('ACCESS_NOT_AUTHORIZED',lambda:self.agent.submit(self.backend.p|{'accessEnabled':True}))

    def test_missing_and_invalid_address_fields_are_rejected(self):
        incomplete=payload();incomplete.pop('ip')
        for bad in [incomplete,payload()|{'ip':'not-an-ip'},payload('RETURN')|{'vmid':True}]:
            with self.assertRaises(Problem):validate(bad)

    def test_assignment_fence(self):
        self.agent.allowed=[2]
        self.assert_code('SERVER_NOT_ASSIGNED',lambda:self.agent.submit(payload()))

    def test_prepare_stores_only_password_hash_and_replay_has_no_mutation(self):
        row=self.execute(payload());self.assertEqual(1,self.driver.read(2000)['credential_version'])
        self.assertNotIn(self.backend.credential['password'],self.store.path.read_bytes().decode('latin1'))
        before=len(self.backend.calls);again=self.agent.submit(payload())
        self.assertEqual(row['id'],again['id']);self.assertEqual(before,len(self.backend.calls))
        self.assertNotIn('password',canonical(again))

    def test_demo_full_lifecycle_retains_then_returns_owned_resources(self):
        self.execute(payload())
        self.execute(payload('STOP','stop'));vm=self.driver.read(2000)
        self.assertEqual(('STOPPED',0,0),(vm['state'],vm['access_enabled'],vm['gpu_attached']))
        self.assertEqual(3,len(self.store.rows('SELECT * FROM demo_resources')))
        self.execute(payload('START','start'));self.assertEqual(1,self.driver.read(2000)['gpu_attached'])
        self.backend.credential['credentialVersion']=2
        self.execute(payload('RESET_CREDENTIAL','reset'));self.assertEqual(2,self.driver.read(2000)['credential_version'])
        manifest=dict(volumeIds=['DEMO-volume-2000'],snippetIds=['DEMO-snippet-2000'],ingressRuleIds=['DEMO-ingress-2000'])
        original=self.backend.context
        self.backend.context=lambda:original()|{'resources':original()['resources']|{'manifest':manifest}}
        row=self.execute(payload('RETURN','return'));result=json.loads(row['result'])
        self.assertTrue(result['simulation']);self.assertEqual(5,len(result['verification']))
        self.assertTrue(all(x['passed'] and x['evidence']['simulation'] for x in result['verification']))
        self.assertEqual([],self.store.rows('SELECT * FROM demo_resources'));self.assertIsNone(self.driver.read(2000))

    def test_vm_owner_mismatch_never_changes_foreign_vm(self):
        self.execute(payload());self.store.execute('UPDATE demo_vms SET rental_id=2')
        self.backend.p=payload('STOP','stop');op=self.agent.submit(self.backend.p);self.agent.run_pending()
        self.assertEqual('VM_OWNER_MISMATCH',self.store.row('SELECT * FROM operations WHERE id=?',op['id'])['reason'])
        self.assertEqual('RUNNING',self.driver.read(2000)['state'])

    def test_authoritative_health_denial_closes_demo_access(self):
        self.execute(payload())
        result=self.driver.health(dict(vmid=2000,rentalId=1,serverId=1,accessAllowed=False))
        self.assertFalse(result['accessEnabled']);self.assertEqual(0,self.driver.read(2000)['access_enabled'])

    def test_claim_response_loss_replays_saved_claim_intent(self):
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        job=dict(id=1,serverId=1,commandId='claim-command');attempts=[]
        def call(method,path,body=None):
            if path.endswith('/inventory'):return [{'id':1,'inventorySource':'DEMO'}]
            if path.endswith('/jobs'):return [] if attempts else [job]
            attempts.append(copy.deepcopy(body))
            if len(attempts)==1:raise RemoteProblem('TRANSPORT_UNCERTAIN',503)
            return job
        self.backend.call=call
        with self.assertRaises(RemoteProblem):worker.claim_due()
        self.assertEqual('CLAIMING',self.store.row('SELECT * FROM jobs')['stage'])
        worker.claim_due();self.assertEqual('CLAIMED',self.store.row('SELECT * FROM jobs')['stage'])
        self.assertEqual(attempts[0],attempts[1])

    def test_start_claim_is_deferred_while_same_rental_prepare_is_incomplete(self):
        prepared=self.store.submit(payload());self.store.finish(prepared['id'],'UNKNOWN',reason='PVE_HTTP_ERROR')
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        start=dict(id=2,serverId=1,rentalId=1,type='START',commandId='start')
        before=len(self.backend.calls);self.assertFalse(worker.claim_one(start))
        self.assertEqual(before,len(self.backend.calls))
        saved=self.store.row('SELECT * FROM jobs WHERE job_id=2')
        self.assertEqual('BLOCKED',saved['stage']);self.assertIsNone(saved['operation_id'])
        # A saved claim intent must also stop replaying until PREPARE is complete.
        self.assertFalse(worker.claim_one(json.loads(saved['payload'])))
        self.assertEqual(before,len(self.backend.calls))

    def test_start_claim_can_proceed_after_preparation_succeeds(self):
        prepared=self.store.submit(payload());self.store.finish(prepared['id'],'SUCCEEDED',{})
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        start=dict(id=2,serverId=1,rentalId=1,type='START',commandId='start')
        self.backend.call=Mock(return_value=start)
        worker.claim_one(start)
        self.backend.call.assert_called_once_with('POST','/api/internal/jobs/2/claim',{'workerId':'manager-test'})
        self.assertEqual('CLAIMED',self.store.row('SELECT stage FROM jobs WHERE job_id=2')['stage'])

    def test_start_waits_until_successful_prepare_is_reported_to_backend(self):
        prepared=self.store.submit(payload());self.store.finish(prepared['id'],'SUCCEEDED',{})
        self.store.job(1,payload()['commandId'],'WAITING_AGENT',{},prepared['id'])
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        start=dict(id=2,serverId=1,rentalId=1,type='START',commandId='start')
        self.assertTrue(worker.awaiting_local_prepare(start))
        self.store.job(1,payload()['commandId'],'DONE',{},prepared['id'])
        self.assertFalse(worker.awaiting_local_prepare(start))

    def test_report_response_loss_replays_same_timestamp_and_payload(self):
        row=self.execute(payload());claim=payload()|{'id':1}
        self.store.job(1,claim['commandId'],'WAITING_AGENT',claim,row['id'])
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        reports=[]
        def call(method,path,body=None):
            if path.endswith('/observation'):
                reports.append(copy.deepcopy(body))
                if len(reports)==1:raise RemoteProblem('TRANSPORT_UNCERTAIN',503)
            return {}
        self.backend.call=call
        with self.assertRaises(RemoteProblem):worker.report(self.store.row('SELECT * FROM jobs'),row)
        worker.report(self.store.row('SELECT * FROM jobs'),row)
        self.assertEqual(reports[0],reports[1]);self.assertEqual('DONE',self.store.row('SELECT * FROM jobs')['stage'])

    def test_http_agent_token_required_and_user_token_rejected(self):
        runtime=SimpleNamespace(config={'agent_token':'a'*64},store=self.store,agent=self.agent,
                                health=lambda:{'status':'UP'},shutdown=lambda:None)
        server=ThreadingHTTPServer(('127.0.0.1',0),handler(runtime))
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        self.addCleanup(server.server_close);self.addCleanup(server.shutdown)
        base=f'http://127.0.0.1:{server.server_port}'
        for token in ['', 'student-jwt']:
            request=urllib.request.Request(base+'/v1/operations',headers={'Authorization':'Bearer '+token})
            with self.assertRaises(urllib.error.HTTPError) as error:urllib.request.urlopen(request)
            self.assertEqual(401,error.exception.code)
        client=HttpClient(base,'a'*64)
        first=client.call('POST','/v1/operations',payload())
        again=client.call('POST','/v1/operations',payload());self.assertEqual(first['id'],again['id'])
        self.assertEqual(1,len(client.call('GET','/v1/operations')))

    def test_offline_configuration_check_does_not_contact_backend_or_create_store(self):
        path=Path(__file__).resolve().parents[1]/'scripts/local.py'
        spec=importlib.util.spec_from_file_location('manager_launcher_fixture',path);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
        state=Path(self.temp.name)/'must-not-create.sqlite3';config=Path(self.temp.name)/'config.json'
        config.write_text(json.dumps(dict(mode='DEMO',worker_id='standalone-test',worker_token='w'*64,agent_token='a'*64,backend_url='https://backend.example.com',state_path=str(state),port=19090)))
        with patch('urllib.request.urlopen',side_effect=AssertionError('network call forbidden')):
            self.assertEqual('DEMO',launcher.check_configuration(config)['mode'])
        self.assertFalse(state.exists());self.assertFalse(state.with_suffix('.process.lock').exists())

    def test_offline_check_rejects_invalid_port_and_missing_state_path(self):
        path=Path(__file__).resolve().parents[1]/'scripts/local.py'
        spec=importlib.util.spec_from_file_location('manager_launcher_fixture2',path);launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
        config=Path(self.temp.name)/'config.json'
        base=dict(mode='DEMO',worker_id='standalone-test',worker_token='w'*64,agent_token='a'*64,backend_url='http://127.0.0.1:18082',state_path=str(self.store.path),port=19090)
        for override in [{'port':True},{'port':80},{'state_path':''}]:
            config.write_text(json.dumps(base|override))
            with self.assertRaises(ValueError):launcher.check_configuration(config)

    def test_remote_http_and_credentials_in_url_rejected(self):
        for url in ['http://192.168.64.20:8006','https://user:password@example.com','https://example.com?token=unsafe']:
            with self.assertRaises(ValueError):HttpClient(url,'test')

    def test_real_mode_requires_explicit_equipment_and_isolation_configuration(self):
        with self.assertRaises(ValueError):ProxmoxDriver(self.store,{})

    def test_backend_disconnect_closes_owned_demo_access(self):
        self.execute(payload())
        worker=Worker(self.store,self.backend,self.agent,dict(worker_id='manager-test',mode='DEMO'))
        worker.fail_closed();self.assertEqual(0,self.driver.read(2000)['access_enabled'])

    @unittest.skipIf(os.name=='nt','Gateway helper is Linux only')
    def test_linux_gateway_expiry_preserves_other_unexpired_owners(self):
        path=Path(__file__).resolve().parents[1]/'scripts/gateway.py'
        spec=importlib.util.spec_from_file_location('gateway_fixture',path);gateway=importlib.util.module_from_spec(spec);spec.loader.exec_module(gateway)
        state=Path(self.temp.name)/'gateway.json'
        state.write_text(json.dumps({'expired':{'expiresAt':0,'port':2201},'current':{'expiresAt':9999999999,'port':2202}}))
        removed=[]
        def run(*args,**kwargs):
            if '-D' in args:removed.append(args)
            return SimpleNamespace(returncode=0,stdout='')
        with patch.object(gateway,'run',side_effect=run),patch.object(gateway,'rules',side_effect=lambda table,chain,key: [['-m','comment','--comment',key]] if key=='expired' else []):
            gateway.handle({'action':'reap','rentalId':1,'serverId':1,'port':1024},{'state_path':str(state),'external_interface':'vmbr0','allowed_subnets':['192.168.101.0/24']})
        self.assertEqual(3,len(removed));self.assertEqual(['current'],list(json.loads(state.read_text())))

    @unittest.skipIf(os.name=='nt','Gateway helper is Linux only')
    def test_linux_gateway_cannot_open_without_independent_expiry_timer(self):
        path=Path(__file__).resolve().parents[1]/'scripts/gateway.py'
        spec=importlib.util.spec_from_file_location('gateway_fixture2',path);gateway=importlib.util.module_from_spec(spec);spec.loader.exec_module(gateway)
        calls=[]
        def run(*args,**kwargs):calls.append(args);return SimpleNamespace(returncode=0,stdout='')
        with patch.object(gateway,'run',side_effect=run),patch.object(gateway,'rules',return_value=[]),patch.object(gateway.subprocess,'run',return_value=SimpleNamespace(returncode=1)):
            with self.assertRaisesRegex(RuntimeError,'EXPIRY_TIMER_REQUIRED'):
                gateway.handle({'action':'open','rentalId':1,'serverId':1,'port':2201,'vmid':2000,'ip':'192.168.101.10'},{'state_path':str(Path(self.temp.name)/'routes.json'),'external_interface':'vmbr0','allowed_subnets':['192.168.101.0/24']})
        self.assertFalse(any('DNAT' in args for args in calls))

    @unittest.skipIf(os.name=='nt','Gateway helper is Linux only')
    def test_gateway_partial_open_retains_durable_expiry_cleanup_intent(self):
        path=Path(__file__).resolve().parents[1]/'scripts/gateway.py'
        spec=importlib.util.spec_from_file_location('gateway_fixture3',path);gateway=importlib.util.module_from_spec(spec);spec.loader.exec_module(gateway)
        state=Path(self.temp.name)/'routes.json'
        def run(*args,**kwargs):
            if 'DNAT' in args:raise RuntimeError('simulated firewall failure')
            return SimpleNamespace(returncode=0,stdout='')
        with patch.object(gateway,'run',side_effect=run),patch.object(gateway,'rules',return_value=[]),patch.object(gateway.subprocess,'run',return_value=SimpleNamespace(returncode=0)):
            with self.assertRaisesRegex(RuntimeError,'simulated firewall failure'):
                gateway.handle({'action':'open','rentalId':1,'serverId':1,'port':2201,'vmid':2000,'ip':'192.168.101.10'},{'state_path':str(state),'external_interface':'vmbr0','allowed_subnets':['192.168.101.0/24']})
        self.assertIn('expiresAt',json.loads(state.read_text())['allday-r1-s1'])

    def test_pve_http_500_is_not_proof_that_vm_is_absent(self):
        driver=object.__new__(ProxmoxDriver);driver.api=Mock()
        driver.api.call.side_effect=[[{'vmid':2000}],RemoteProblem('PVE_HTTP_ERROR',500),[{'vmid':2000}]]
        with self.assertRaises(RemoteProblem):driver.config_vm('pve',2000)
        driver.api.call.side_effect=[[],]
        self.assertIsNone(driver.config_vm('pve',2000))

    def test_pve_ownership_fence_and_saved_upid_reuse(self):
        driver=object.__new__(ProxmoxDriver);driver.store=self.store;driver.api=Mock()
        driver.config_vm=Mock(return_value={'description':'someone else'})
        self.assert_code('PVE_VM_OWNER_MISMATCH',lambda:driver.owned(payload(),{'node_name':'pve'}))
        op=self.store.submit(payload());self.store.execute('INSERT INTO effects VALUES (?,?,?,?)',op['id'],'clone','UPID:test','RUNNING')
        driver.api.call.return_value={'status':'stopped','exitstatus':'OK'}
        guard=Mock();self.assertEqual('UPID:test',driver.task(op,'clone','pve','POST','/clone',{},guard))
        self.assertEqual('GET',driver.api.call.call_args.args[0]);self.assertEqual(1,driver.api.call.call_count)


if __name__=='__main__':unittest.main()
