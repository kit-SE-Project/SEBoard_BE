#!/usr/bin/env python3
"""Read-only Windows equipment check. No VM/DB/config mutations or secret output."""
import argparse,json,re,sys
from datetime import datetime,timezone
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
from manager.common import HttpClient,Problem
from manager.proxmox import PveClient
from manager.helper_client import HelperTransport


def probe(config,server=57,node='pve01',template=100):
    report={'checkedAt':datetime.now(timezone.utc).isoformat(),'readOnly':True,'vmMutationsPerformed':False,'managerMode':config.get('mode'),'gatewayProtocol':config.get('gateway_protocol','LEGACY_GATEWAY'),'serverId':server,'nodeName':node,'templateVmid':template,'checks':[]}
    def run(label,fn):
        try:result=fn();report['checks'].append({'name':label,'passed':True,'result':result});return result
        except Problem as e:report['checks'].append({'name':label,'passed':False,'code':e.code,'httpStatus':e.status})
        except (KeyError,ValueError,OSError,TypeError):report['checks'].append({'name':label,'passed':False,'code':'MISSING_OR_INVALID_LOCAL_CONFIGURATION'})
    def backend():
        client=HttpClient(config['backend_url'],config['worker_token'],ca=config.get('backend_ca_file'))
        inventory=client.call('GET','/api/internal/inventory')
        selected=[s for s in inventory if s.get('id')==server]
        return {'equipmentFound':bool(selected),'equipment':selected,'assignedHere':server in config.get('server_ids',[])}
    run('backend HTTPS / worker token / assigned equipment',backend)
    def pve_checks():
        api=PveClient(config['proxmox']);status=api.call('GET',f'/nodes/{node}/status')
        vms=api.call('GET',f'/nodes/{node}/qemu')
        cfg=api.call('GET',f'/nodes/{node}/qemu/{template}/config')
        # Never copy cipassword, SSH keys, tokens, raw VM configs or transport stderr.
        disks={k:v.split(',')[0] for k,v in cfg.items() if re.fullmatch(r'(scsi|sata|virtio|ide)\d+',k) and isinstance(v,str)}
        return {'nodeResponding':bool(status),'existingVms':[{'vmid':v.get('vmid'),'status':v.get('status'),'template':v.get('template',0)} for v in vms],
                'template':{'confirmed':cfg.get('template')==1,'osType':cfg.get('ostype'),'agent':cfg.get('agent'),'memoryMb':cfg.get('memory'),'net0':cfg.get('net0'),'ipconfig0':cfg.get('ipconfig0'),'hasCloudInitDrive':any('cloudinit' in v for v in disks.values()),'pciDevices':[k for k in cfg if re.fullmatch(r'hostpci\d+',k)],'diskReferences':disks},
                'network':[{'iface':i.get('iface'),'type':i.get('type'),'active':i.get('active'),'address':i.get('address'),'cidr':i.get('cidr'),'bridge_ports':i.get('bridge_ports')} for i in api.call('GET',f'/nodes/{node}/network')]}
    run('Proxmox HTTPS / token / template / bridge',pve_checks)
    def helper_check():
        transport=HelperTransport(config['nodes'][str(server)]['helper'])
        req={'schemaVersion':1,'commandId':'equipment-read-'+datetime.now(timezone.utc).strftime('%Y%m%d%H%M%S%f'),'serverId':server,'operation':'READ_STATUS','payload':{}}
        response=transport(req)
        if response.get('mode')!='system' or response.get('ok') is not True or response.get('serverId')!=server or response.get('commandId')!=req['commandId'] or response.get('operation')!='READ_STATUS':raise Problem('HELPER_STATUS_UNCONFIRMED')
        r=response['result'];return {k:r.get(k) for k in ['generation','controlRevision','ownerRentalId','accessEnabled','needsRecovery','pendingCleanup','activeTaskCount','vms']}
    run('Node Helper v0.2 / SSH key / correct serverId',helper_check)
    report['allConnectionsResponded']=all(c['passed'] for c in report['checks'])
    report['realVmLifecycleVerified']=False
    return report


def main():
    parser=argparse.ArgumentParser(description='실제 Windows 관리 서버에서 실행. VM을 생성하거나 설정을 변경하지 않습니다.')
    parser.add_argument('--config',type=Path,default=ROOT/'.local/config.json');parser.add_argument('--output',type=Path,default=ROOT/'.local/equipment-report.json')
    parser.add_argument('--server-id',type=int,default=57);parser.add_argument('--node',default='pve01');parser.add_argument('--template-vmid',type=int,default=100);args=parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,32}',args.node) or args.server_id<1 or args.template_vmid<100:raise ValueError('Invalid equipment IDs')
    report=probe(json.loads(args.config.read_text(encoding='utf-8-sig')),args.server_id,args.node,args.template_vmid)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))
    print('확인 결과 파일:',args.output)
    return 0 if report['allConnectionsResponded'] else 2


if __name__=='__main__':
    try:sys.exit(main())
    except (OSError,ValueError):print('설정 파일 또는 입력값을 확인하세요. 비밀 설정 내용은 출력하지 않았습니다.',file=sys.stderr);sys.exit(2)
