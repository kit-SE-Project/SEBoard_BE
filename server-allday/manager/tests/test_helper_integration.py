"""Original team Helper state machine + fake PVE/SSH: no real hardware effects."""
import copy,json,sys,tempfile,unittest
from datetime import datetime,timedelta,timezone
from pathlib import Path
from unittest.mock import Mock
sys.path.insert(0,str(Path(__file__).parent/'fixtures/node_helper_v02'))
from node_helper.config import Config
from node_helper.host import SimulationHost
from node_helper.service import NodeHelper
from manager.common import Problem,RemoteProblem
from manager.helper_client import HelperClient,lease_seconds,password_hash
from manager.helper_driver import HelperProxmoxDriver
from manager.store import Store


def window(seconds=60):
    at=datetime.now(timezone.utc)
    return {'checkedAt':at.isoformat(),'validUntil':(at+timedelta(seconds=seconds)).isoformat(),'slotEndAt':(at+timedelta(seconds=seconds)).isoformat(),'maxLeaseSeconds':seconds}


class FakePve:
    def __init__(self,host,trace):
        self.host=host;self.trace=trace;self.status={100:'stopped'}
        self.configs={100:{'template':1,'net0':'virtio=AA:BB:CC:DD:EE:FF,bridge=vmbr0,firewall=1'}}
        self.counter=0;self.writes=[];self.guest_commands={}
    def sync(self):self.host.change(vms=[{'vmid':k,'status':v} for k,v in self.status.items()])
    def call(self,method,path,body=None):
        if method!='GET':self.writes.append((method,path,copy.deepcopy(body)));self.trace.append(path.rsplit('/',1)[-1])
        parts=path.split('/');vmid=int(parts[4]) if len(parts)>4 and parts[3]=='qemu' else None
        if '/tasks/' in path:return {'status':'stopped','exitstatus':'OK'}
        if path.endswith('/qemu'):return [{'vmid':k,'status':v} for k,v in self.status.items()]
        if path.endswith('/clone'):
            new=body['newid'];self.configs[new]={'description':body['description'],'net0':self.configs[100]['net0'],'scsi0':f'local-lvm:vm-{new}-disk-0','ide2':f'local-lvm:vm-{new}-cloudinit,media=cdrom'};self.status[new]='stopped';self.sync()
            self.host.change(volumes={'local':[],'local-lvm':[{'volid':f'local-lvm:vm-{new}-disk-0','vmid':new},{'volid':f'local-lvm:vm-{new}-cloudinit','vmid':new}]})
        elif method=='GET' and path.endswith('/config'):return copy.deepcopy(self.configs[vmid])
        elif method=='PUT' and path.endswith('/config'):
            for key in body.get('delete','').split(','):self.configs[vmid].pop(key,None)
            self.configs[vmid].update({k:v for k,v in body.items() if k!='delete'})
        elif path.endswith('/status/current'):return {'status':self.status[vmid]}
        elif path.endswith('/status/stop') or path.endswith('/status/shutdown'):self.status[vmid]='stopped';self.sync()
        elif path.endswith('/status/start'):self.status[vmid]='running';self.sync();self.host.change(sshReady=['10.10.0.10'])
        elif method=='DELETE':
            self.configs.pop(vmid);self.status.pop(vmid);self.sync();self.host.change(volumes={'local':[],'local-lvm':[]})
        elif path.endswith('/agent/ping') or path.endswith('/agent/set-user-password'):return {}
        elif path.endswith('/agent/exec'):
            pid=len(self.guest_commands)+1;self.guest_commands[pid]=body['command'];return {'pid':pid}
        elif path.endswith('/agent/exec-status'):
            command=self.guest_commands[body['pid']]
            return {'exited':True,'exitcode':0,'out-data':json.dumps({'status':'done','errors':[]}) if command[0].endswith('cloud-init') else ''}
        elif path.endswith('/agent/network-get-interfaces'):return {'result':[{'ip-addresses':[{'ip-address':'10.10.0.10'}]}]}
        elif '/agent/file-read?' in path:return {'content':'ssh-ed25519 Zml4dHVyZQ== fixture'}
        elif path.endswith('/status'):return {'uptime':1}
        else:raise AssertionError((method,path))
        if method in ['POST','DELETE']:
            self.counter+=1;return 'UPID:fixture-'+str(self.counter)
        return {}


class HelperIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=Path(tempfile.gettempdir()).resolve());self.addCleanup(self.temp.cleanup);root=Path(self.temp.name)
        cfg={'mode':'simulation','serverId':57,'nodeName':'pve01','nodeIp':'192.0.2.20','nodeIngressPort':7540,'uplinkBridge':'vmbr0','vmBridge':'vmbr1','vmSubnet':'10.10.0.0/24','vmGateway':'10.10.0.1','vmInterface':'ens18','nameservers':['1.1.1.1'],'blockedNetworks':['192.0.2.0/24'],'storageIds':['local','local-lvm'],'snippetStorage':'local','snippetDirectory':str(root/'snippets'),'stateDirectory':str(root/'helper-state'),'vmIdMin':2000,'vmIdMax':2010,'conntrackZones':[0,1]}
        config=root/'node.json';config.write_text(json.dumps(cfg));self.config=Config.load(config);self.host=SimulationHost(self.config);self.team=NodeHelper(self.config,self.host);self.addCleanup(self.team.close);self.team.bootstrap()
        self.trace=[];self.requests=[]
        def transport(request):
            self.requests.append(copy.deepcopy(request));self.trace.append(request['operation'])
            response=self.team.handle(request)
            # Only this fake SSH boundary labels the simulation as a system reply.
            # Production accepts system mode only and has no simulation bypass.
            response['mode']='system';return response
        self.transport=transport;self.store=Store(root/'manager.sqlite3');self.driver=object.__new__(HelperProxmoxDriver);self.driver.store=self.store
        self.driver.helper=HelperClient(self.store,{'57':transport});self.driver.nodes={'57':{'node_name':'pve01','gpu_enabled':False,'gpu_mapping':None,'gateway':'10.10.0.1','vm_bridge':'vmbr1','public_ssh_port':7540}}
        self.driver.config={'worker_id':'fixture'};self.driver.api=FakePve(self.host,self.trace);self.driver.backend=Mock();self.manifest={'volumeIds':[],'snippetIds':[],'ingressRuleIds':[]}
        self.health_targets=None
        def checkpoint(method,path,body=None):
            if method=='GET' and path=='/api/internal/health-targets':return copy.deepcopy(self.health_targets if self.health_targets is not None else [self.target()])
            for k in self.manifest:self.manifest[k]=sorted(set(self.manifest[k]+body.get(k,[])))
            return {}
        self.driver.backend.call.side_effect=checkpoint
        self.context={'server':{'id':57,'nodeName':'pve01','inventorySource':'MANUAL','gpuEnabled':False,'gpuMappingRef':None,'vmSubnet':'10.10.0.0/24','publicSshPort':7540},'template':{'approvedAt':'fixture','deprecatedAt':None,'templateVmid':100},'resources':{'manifest':self.manifest},'inSlot':True,'accessWindow':window()}
        self.credential={'username':'student','password':'FixturePassword12!','credentialVersion':1};self.index=0
    def operation(self,kind='PREPARE',access=True):
        self.index+=1;p={'commandId':'fixture-'+str(self.index),'jobId':self.index,'serverId':57,'rentalId':7,'desiredRevision':0,'type':kind,'vmid':2000,'ip':'10.10.0.10','gpuGeneration':999,'accessEnabled':access}
        op=self.store.submit(p);self.store.finish(op['id'],'RUNNING');return op
    def execute(self,kind='PREPARE',access=True):
        op=self.operation(kind,access)
        result=self.driver.execute(op,self.context,self.credential,lambda:self.context|{'accessWindow':window()})
        self.store.finish(op['id'],'SUCCEEDED',result);return op,result
    def target(self,access=True):
        return {'serverId':57,'rentalId':7,'vmid':2000,'desiredRevision':0,'gpuGeneration':999,'accessAllowed':access,'accessWindow':window()}

    def test_prepare_creates_snippets_clones_starts_and_opens_with_original_helper(self):
        op,result=self.execute()
        self.assertTrue(result['observation']['healthPassed']);self.assertTrue(result['observation']['accessEnabled'])
        cfg=self.driver.api.configs[2000];self.assertEqual(0,cfg['onboot']);self.assertIn('bridge=vmbr1',cfg['net0']);self.assertNotIn('hostpci0',cfg)
        self.assertEqual(3,len(self.manifest['snippetIds']));self.assertEqual(2,len(self.manifest['volumeIds']))
        self.assertIn('user=local:snippets/allday-r7-user.yaml',cfg['cicustom'])
        self.assertLess(self.trace.index('PUT_SNIPPET'),self.trace.index('clone'));self.assertLess(self.trace.index('ADVANCE_GENERATION'),self.trace.index('start'))
        self.assertEqual(1,self.driver.helper.workflow(op)['current_generation']);self.assertEqual(999,json.loads(op['payload'])['gpuGeneration'])
        self.assertNotIn(self.credential['password'].encode(),self.store.path.read_bytes())

    def test_prepare_passes_pool_to_clone_after_pool_permission_check(self):
        self.driver.nodes['57']['vm_pool']='allday-node57';original=self.driver.api.call
        def with_pool(method,path,body=None):
            if path.startswith('/access/permissions?'):return {'/pool/allday-node57':{'VM.Allocate':1}}
            return original(method,path,body)
        self.driver.api.call=with_pool
        self.execute()
        clones=[write for write in self.driver.api.writes if write[1].endswith('/clone')]
        self.assertEqual('allday-node57',clones[0][2]['pool'])

    def test_start_uses_graceful_shutdown_never_power_cut(self):
        self.execute();_,result=self.execute('START',False)
        shutdown=[w for w in self.driver.api.writes if w[1].endswith('/status/shutdown')]
        self.assertEqual(1,len(shutdown));self.assertEqual({'timeout':90,'forceStop':0},shutdown[0][2])
        self.assertFalse(any(w[1].endswith('/status/stop') for w in self.driver.api.writes))
        self.assertTrue(result['observation']['healthPassed'])

    def test_cloud_init_and_disk_flush_precede_access_open(self):
        self.execute()
        commands=list(self.driver.api.guest_commands.values())
        self.assertEqual(['/usr/bin/cloud-init','status','--format=json'],commands[0])
        self.assertEqual(['/usr/bin/sync'],commands[1])
        self.assertLess(self.trace.index('exec'),self.trace.index('APPLY_INGRESS'))

    def test_graceful_shutdown_failure_does_not_fall_back_to_force_stop(self):
        self.execute();original=self.driver.api.call;before=len(self.driver.api.writes)
        def refuse(method,path,body=None):
            if path.endswith('/status/shutdown'):raise RemoteProblem('PVE_HTTP_ERROR',500)
            return original(method,path,body)
        self.driver.api.call=refuse
        with self.assertRaisesRegex(RemoteProblem,'PVE_HTTP_ERROR'):self.execute('START',False)
        self.assertFalse(any(w[1].endswith('/status/stop') for w in self.driver.api.writes[before:]))
        self.assertFalse(self.host.ingress()['leaseValid'])

    def test_failed_sync_cannot_open_student_access(self):
        original=self.driver.api.call
        def fail_sync(method,path,body=None):
            if path.endswith('/agent/exec-status') and self.driver.api.guest_commands[body['pid']]==['/usr/bin/sync']:
                return {'exited':True,'exitcode':1}
            return original(method,path,body)
        self.driver.api.call=fail_sync
        with self.assertRaisesRegex(Problem,'GUEST_SYNC_UNCONFIRMED'):self.execute()
        self.assertNotIn('APPLY_INGRESS',self.trace)

    def test_missing_pool_rights_rejected_before_any_helper_mutation(self):
        self.driver.nodes['57']['vm_pool']='allday-node57';original=self.driver.api.call
        def without_pool(method,path,body=None):
            if path.startswith('/access/permissions?'):return {}
            return original(method,path,body)
        self.driver.api.call=without_pool
        with self.assertRaisesRegex(Problem,'PVE_RENTAL_POOL_PERMISSION_REQUIRED'):self.execute()
        self.assertEqual([],self.requests);self.assertEqual([],self.driver.api.writes)

    def test_new_prepare_attempt_reuses_preserved_snippets_after_rejected_clone(self):
        self.driver.nodes['57']['vm_pool']='allday-node57';original=self.driver.api.call;deny=[True]
        def permission_restored(method,path,body=None):
            if path.startswith('/access/permissions?'):return {'/pool/allday-node57':{'VM.Allocate':1}}
            if path.endswith('/clone') and deny[0]:raise RemoteProblem('PVE_HTTP_ERROR',403)
            return original(method,path,body)
        self.driver.api.call=permission_restored;old=self.operation('PREPARE',False)
        with self.assertRaisesRegex(Problem,'PVE_HTTP_ERROR'):
            self.driver.execute(old,self.context,self.credential,lambda:self.context)
        self.store.finish(old['id'],'UNKNOWN',reason='PVE_HTTP_ERROR')
        self.assertNotIn(2000,self.driver.api.configs);self.assertTrue(self.team.snippets.paths(7)['user'].exists())
        # Operator recovery is verified separately; this fixture models its terminal failure.
        self.store.finish(old['id'],'FAILED',reason='RECOVERED_PREPARE_INCOMPLETE');deny[0]=False
        _,result=self.execute('PREPARE',False)
        self.assertTrue(result['observation']['healthPassed']);self.assertEqual('running',self.driver.api.status[2000])
        clones=[write for write in self.driver.api.writes if write[1].endswith('/clone')]
        self.assertEqual(1,len(clones));self.assertEqual('allday-node57',clones[0][2]['pool'])

    def test_stop_start_keeps_same_vm_and_snippets_and_advances_helper_epoch_only_on_start(self):
        first,_=self.execute();self.execute('STOP',False)
        self.assertEqual('stopped',self.driver.api.status[2000]);self.assertIsNone(self.host.ingress()['target']);self.assertTrue(self.team.snippets.paths(7)['user'].exists())
        op,result=self.execute('START',True);self.assertTrue(result['observation']['accessEnabled']);self.assertEqual(2,self.driver.helper.workflow(op)['current_generation'])
        self.assertEqual(1,len([x for x in self.trace if x=='clone']))

    def test_return_removes_owned_vm_volumes_snippets_then_verifies_before_release(self):
        self.execute();op,result=self.execute('RETURN',False)
        self.assertNotIn(2000,self.driver.api.configs);self.assertFalse(self.team.snippets.paths(7)['user'].exists())
        self.assertTrue(all(i['passed'] for i in result['verification']))
        self.assertLess(self.trace.index('REMOVE_SNIPPET'),self.trace.index('VERIFY_CLEANUP'));self.assertLess(self.trace.index('VERIFY_CLEANUP'),self.trace.index('RELEASE_RESOURCE'))
        self.assertIsNone(self.team.store.state()['owner'])

    def test_return_then_another_rental_reuses_vmid_ip_without_pci_passthrough(self):
        self.execute();self.execute('RETURN',False)
        self.manifest.update(volumeIds=[],snippetIds=[],ingressRuleIds=[])
        def next_rental(kind,access):
            self.index+=1
            p={'commandId':'cycle2-'+str(self.index),'jobId':self.index,'serverId':57,'rentalId':8,'desiredRevision':0,'type':kind,'vmid':2000,'ip':'10.10.0.10','gpuGeneration':1001,'accessEnabled':access}
            operation=self.store.submit(p);self.store.finish(operation['id'],'RUNNING')
            result=self.driver.execute(operation,self.context,self.credential,lambda:self.context|{'accessWindow':window()})
            self.store.finish(operation['id'],'SUCCEEDED',result);return result
        result=next_rental('PREPARE',True)
        self.assertTrue(result['observation']['accessEnabled']);self.assertNotIn('hostpci0',self.driver.api.configs[2000])
        self.assertEqual(8,self.team.store.state()['owner']);self.assertIn('rental=8',self.driver.api.configs[2000]['description'])
        returned=next_rental('RETURN',False)
        self.assertTrue(all(item['passed'] for item in returned['verification']))
        self.assertNotIn(2000,self.driver.api.configs);self.assertIsNone(self.team.store.state()['owner'])
        self.assertEqual(2,len([w for w in self.driver.api.writes if w[1].endswith('/clone')]))
        self.assertEqual(2,len([w for w in self.driver.api.writes if w[0]=='DELETE']))

    def test_return_replay_after_helper_release_reverifies_without_deleting_twice(self):
        self.execute();self.execute('RETURN',False)
        deletes=len([w for w in self.driver.api.writes if w[0]=='DELETE'])
        _,result=self.execute('RETURN',False)
        self.assertTrue(all(item['passed'] for item in result['verification']))
        self.assertEqual(deletes,len([w for w in self.driver.api.writes if w[0]=='DELETE']))
        self.assertIsNone(self.team.store.state()['owner'])

    def test_prepare_resume_keeps_registered_disks_and_exact_snippet_hash(self):
        self.execute();before=self.team.snippets.paths(7)['user'].read_bytes()
        _,result=self.execute('PREPARE',True)
        self.assertTrue(result['observation']['healthPassed']);self.assertEqual(before,self.team.snippets.paths(7)['user'].read_bytes())
        self.assertEqual(2,len(self.team.store.resource(7)['volumeIds']))
        self.assertEqual(1,len([w for w in self.driver.api.writes if w[1].endswith('/clone')]))

    def test_return_before_any_allocation_has_no_physical_side_effects(self):
        p={'commandId':'unallocated','jobId':77,'serverId':57,'rentalId':8,'desiredRevision':1,'type':'RETURN','vmid':None,'ip':None,'gpuGeneration':0,'accessEnabled':False}
        op=self.store.submit(p)
        context=self.context|{'resources':{'allocation':None,'manifest':None,'ip':None,'gpu':{'ownerRentalId':None}}}
        result=self.driver.execute(op,context,None,lambda:context)
        self.assertTrue(all(item['passed'] for item in result['verification']))
        self.assertEqual([],self.driver.api.writes);self.assertEqual(['READ_STATUS'],self.trace)

    def test_returning_unallocated_future_rental_keeps_current_rentals_access(self):
        self.execute();before=list(self.driver.api.writes);target=self.host.ingress()['target']
        p={'commandId':'future-return','jobId':77,'serverId':57,'rentalId':8,'desiredRevision':1,'type':'RETURN','vmid':None,'ip':None,'gpuGeneration':999,'accessEnabled':False}
        op=self.store.submit(p);context=self.context|{'resources':{'allocation':None,'manifest':None,'ip':None,'gpu':{'ownerRentalId':7}}}
        result=self.driver.execute(op,context,None,lambda:context)
        self.assertTrue(all(item['passed'] for item in result['verification']))
        self.assertEqual(before,self.driver.api.writes);self.assertEqual(target,self.host.ingress()['target']);self.assertTrue(self.host.ingress()['leaseValid'])

    def test_new_return_attempt_after_delete_resumes_cleanup_without_second_delete(self):
        self.execute();original=self.driver.helper.transports['57']
        def shell_opens(request):
            if request['operation']=='REMOVE_SNIPPET':self.host.change(tasks=[{'upid':'fixture-shell','type':'vncshell'}])
            return original(request)
        self.driver.helper.transports['57']=shell_opens
        old=self.operation('RETURN',False)
        with self.assertRaisesRegex(Problem,'HELPER_TASK_IN_PROGRESS'):
            self.driver.execute(old,self.context,None,lambda:self.context)
        self.store.finish(old['id'],'UNKNOWN',reason='HELPER_TASK_IN_PROGRESS')
        self.assertNotIn(2000,self.driver.api.configs)
        # Represent the audited local FAILED resolution, which has its own recovery tests.
        self.store.finish(old['id'],'FAILED',reason='RECOVERED_RETURN_INCOMPLETE')
        self.host.change(tasks=[]);self.driver.helper.transports['57']=original
        _,result=self.execute('RETURN',False)
        self.assertTrue(all(item['passed'] for item in result['verification']))
        self.assertEqual(1,len([write for write in self.driver.api.writes if write[0]=='DELETE']))
        self.assertIsNone(self.team.store.state()['owner'])
        self.assertFalse(self.team.snippets.paths(7)['user'].exists())

    def test_health_renews_with_new_command_without_new_generation(self):
        op,_=self.execute();self.driver.health(self.target());self.driver.health(self.target())
        renew=[r for r in self.requests if r['operation']=='RENEW_INGRESS'];self.assertEqual(2,len(renew));self.assertNotEqual(renew[0]['commandId'],renew[1]['commandId'])
        self.assertTrue(all(5<=r['payload']['leaseSeconds']<=45 for r in renew));self.assertEqual(1,self.team.store.state()['generation'])

    def test_backend_access_revoked_closes_and_never_renews(self):
        self.execute();self.health_targets=[self.target(False)];self.driver.health(self.target(False));self.assertFalse(self.host.ingress()['leaseValid'])
        self.assertFalse(any(r['operation']=='RENEW_INGRESS' for r in self.requests))

    def test_health_waiting_behind_start_does_not_close_newly_authorized_access(self):
        self.execute();before=len(self.requests)
        report=self.driver.health(self.target(False))
        self.assertTrue(report['accessEnabled'])
        self.assertTrue(self.host.ingress()['leaseValid'])
        self.assertEqual(['READ_STATUS','RENEW_INGRESS'],[r['operation'] for r in self.requests[before:]])

    def test_health_uses_fresh_revocation_even_when_collected_target_allowed_access(self):
        self.execute();self.health_targets=[self.target(False)]
        report=self.driver.health(self.target())
        self.assertFalse(report['accessEnabled'])
        self.assertFalse(self.host.ingress()['leaseValid'])

    def test_removed_or_revised_health_target_cannot_mutate_access(self):
        self.execute();before=len(self.requests)
        for targets in [[],[self.target()|{'desiredRevision':1}]]:
            self.health_targets=targets
            self.assertIsNone(self.driver.health(self.target()))
        self.assertEqual(before,len(self.requests))

    def test_completed_agent_must_be_reported_before_health_can_reinterpret_backend_ready_state(self):
        operation,_=self.execute();payload=json.loads(operation['payload'])
        self.store.job(payload['jobId'],payload['commandId'],'WAITING_AGENT',payload,operation['id'])
        self.health_targets=[self.target(False)];before=len(self.requests)
        self.assertIsNone(self.driver.health(self.target(False)))
        self.assertEqual(before,len(self.requests))
        self.assertTrue(self.host.ingress()['leaseValid'])
        self.store.job(payload['jobId'],payload['commandId'],'DONE',payload,operation['id'])
        self.health_targets=[self.target()]
        self.assertTrue(self.driver.health(self.target(False))['accessEnabled'])

    def test_expired_lease_uses_disable_then_apply_after_fresh_authorization(self):
        self.execute();self.host.change(leaseDeadline=0);before=len(self.trace);result=self.driver.health(self.target())
        self.assertTrue(result['accessEnabled']);sub=self.trace[before:];self.assertIn('DISABLE_INGRESS',sub);self.assertIn('APPLY_INGRESS',sub);self.assertNotIn('RENEW_INGRESS',sub)

    def test_stale_backend_revision_or_gpu_owner_cannot_renew(self):
        self.execute();before=len(self.requests)
        self.assertIsNone(self.driver.health(self.target()|{'desiredRevision':1}))
        self.assertIsNone(self.driver.health(self.target()|{'gpuGeneration':1000}))
        self.assertEqual(before,len(self.requests))

    def test_backend_disconnect_closes_using_saved_revision(self):
        op,_=self.execute();self.driver.block_access(json.loads(op['payload']));self.assertFalse(self.host.ingress()['leaseValid'])

    def test_helper_simulation_response_is_rejected_in_production_client(self):
        helper=HelperClient(self.store,{'57':lambda r:self.team.handle(r)})
        with self.assertRaisesRegex(Problem,'HELPER_INVALID_RESPONSE'):helper.status(57)

    def test_hash_matches_ubuntu_format_and_not_plaintext(self):
        from passlib.hash import sha512_crypt
        result=password_hash('FixturePassword12!');self.assertTrue(result.startswith('$6$'));self.assertTrue(sha512_crypt.verify('FixturePassword12!',result))

    def test_cleanup_unknown_never_releases_resource_or_emits_pass(self):
        self.execute();original=self.driver.api.call
        def keep_volume(method,path,body=None):
            result=original(method,path,body)
            if method=='DELETE':self.host.change(volumes={'local':[],'local-lvm':[{'volid':'local-lvm:vm-2000-leftover','vmid':2000}]})
            return result
        self.driver.api.call=keep_volume
        with self.assertRaisesRegex(Problem,'HELPER_CLEANUP_INCOMPLETE'):self.execute('RETURN',False)
        self.assertNotIn('RELEASE_RESOURCE',self.trace)

    def test_immutable_workflow_survives_store_reopen_and_is_not_rebased(self):
        op,_=self.execute();before=self.driver.helper.workflow(op)
        helper=HelperClient(Store(self.store.path),{'57':self.transport});self.assertEqual(before,helper.begin(op,json.loads(op['payload'])))

    def test_lost_helper_response_reuses_exact_persisted_request(self):
        op=self.operation();p=json.loads(op['payload']);client=self.driver.helper;client.begin(op,p)
        original=client.transports['57'];sent=[]
        def lose(request):
            sent.append(copy.deepcopy(request));original(request);raise Problem('HELPER_TRANSPORT_UNCERTAIN')
        client.transports['57']=lose
        payload={'rentalId':7,'vmId':2000,'vmIp':'10.10.0.10','volumeIds':[]}
        with self.assertRaises(Problem):client.call(op,'register','REGISTER_RESOURCE',payload)
        def replay(request):sent.append(copy.deepcopy(request));return original(request)
        client.transports['57']=replay
        self.assertEqual(7,client.call(op,'register','REGISTER_RESOURCE',payload)['rentalId']);self.assertEqual(sent[0],sent[1])

    def test_foreign_vm_is_never_changed_or_deleted(self):
        self.driver.api.configs[2000]={'description':'foreign owner','net0':'fixture'};self.driver.api.status[2000]='stopped';self.driver.api.sync()
        with self.assertRaisesRegex(Problem,'PVE_VM_OWNER_MISMATCH'):self.execute()
        self.assertEqual([],self.driver.api.writes)

    def test_newer_remote_revision_cannot_be_overwritten_by_old_health(self):
        self.execute();state=self.team.store.state();state['controlRevision']+=5;self.team.store.save_state(state)
        with self.assertRaisesRegex(Problem,'HELPER_LEASE_MISMATCH'):self.driver.health(self.target())
        self.assertFalse(any(r['operation']=='RENEW_INGRESS' for r in self.requests))

    def test_short_or_untrusted_windows_time_cannot_open_lease(self):
        self.assertEqual(0,lease_seconds(window(9)));self.assertEqual(0,lease_seconds({}))
        w=window();future=(datetime.now(timezone.utc)+timedelta(seconds=60)).isoformat();w['checkedAt']=future
        self.assertEqual(0,lease_seconds(w))


if __name__=='__main__':unittest.main()
