#!/usr/bin/env python3
"""Run on a Linux gateway with an isolated VM network; only manages ALD_* chains."""
import argparse,fcntl,ipaddress,json,os,re,shlex,subprocess,sys,time
from pathlib import Path

def run(*args,allow=False):
    result=subprocess.run(['iptables','-w','5',*args],capture_output=True,text=True)
    if result.returncode and not allow:raise RuntimeError('IPTABLES_UNCONFIRMED')
    return result
def rules(table,chain,key):
    result=run('-t',table,'-S',chain)
    return [shlex.split(line)[2:] for line in result.stdout.splitlines() if line.startswith('-A '+chain+' ') and key in shlex.split(line)]

def persist(state,records):
    temporary=state.with_suffix('.tmp');temporary.write_text(json.dumps(records));temporary.chmod(0o600);temporary.replace(state)

def handle(p,settings):
    if p.get('action') not in ['open','close','check','reap'] or any(type(p.get(x)) is not int or p[x]<=0 for x in ['rentalId','serverId','port']):raise ValueError('INVALID_GATEWAY_REQUEST')
    if not 1024<=p['port']<=65535:raise ValueError('MANAGED_PORT_MUST_BE_1024_OR_HIGHER')
    key=f"allday-r{p['rentalId']}-s{p['serverId']}";ip=p.get('ip');networks=[ipaddress.ip_network(x) for x in settings['allowed_subnets']]
    if ip is not None:
        address=ipaddress.ip_address(ip)
        if address.version!=4 or not any(address in n for n in networks):raise ValueError('VM_IP_NOT_ALLOWED')
    interface=settings['external_interface']
    if not re.fullmatch(r'[A-Za-z0-9_.-]{1,30}',interface):raise ValueError('INVALID_INTERFACE')
    state=Path(settings['state_path']);state.parent.mkdir(parents=True,exist_ok=True)
    lock=state.with_suffix('.lock')
    with lock.open('a') as f:
        fcntl.flock(f,fcntl.LOCK_EX)
        for table,chain in [('nat','ALD_NAT'),('filter','ALD_ACCESS'),('nat','ALD_SNAT')]:
            run('-t',table,'-N',chain,allow=True)
        if run('-t','nat','-C','PREROUTING','-i',interface,'-j','ALD_NAT',allow=True).returncode:run('-t','nat','-I','PREROUTING','1','-i',interface,'-j','ALD_NAT')
        if run('-t','nat','-C','POSTROUTING','-j','ALD_SNAT',allow=True).returncode:run('-t','nat','-I','POSTROUTING','1','-j','ALD_SNAT')
        for network in networks:
            jump=['-d',str(network),'-p','tcp','--dport','22','-j','ALD_ACCESS']
            if run('-C','FORWARD',*jump,allow=True).returncode:run('-I','FORWARD','1',*jump)
        if run('-C','ALD_ACCESS','-j','DROP',allow=True).returncode:run('-A','ALD_ACCESS','-j','DROP')
        records=json.loads(state.read_text()) if state.exists() else {}
        # Expired rules are removed independently by the Linux timer, even if Windows is off.
        for expired,record in list(records.items()):
            if record.get('expiresAt',0)<=time.time():
                for table,chain in [('nat','ALD_NAT'),('filter','ALD_ACCESS'),('nat','ALD_SNAT')]:
                    for rule in rules(table,chain,expired):run('-t',table,'-D',chain,*rule)
                records.pop(expired,None)
        if p['action']=='open':
            lease=int(settings.get('lease_seconds',45))
            if not 30<=lease<=60:raise ValueError('INVALID_LEASE_SECONDS')
            timer=subprocess.run(['systemctl','is-active','--quiet','allday-gateway-expiry.timer'],capture_output=True)
            if timer.returncode:raise RuntimeError('EXPIRY_TIMER_REQUIRED')
            if ip is None:raise ValueError('VM_IP_REQUIRED')
            for k,v in records.items():
                if k!=key and v['port']==p['port']:raise ValueError('PUBLIC_PORT_IN_USE')
            if key in records and any(records[key][k]!=p[k] for k in ['ip','port','vmid']):raise ValueError('ROUTE_OWNER_MISMATCH')
        if p['action'] in ['open','close']:
            for table,chain in [('nat','ALD_NAT'),('filter','ALD_ACCESS'),('nat','ALD_SNAT')]:
                for rule in rules(table,chain,key):run('-t',table,'-D',chain,*rule)
            records.pop(key,None)
            if p['action']=='open':
                # Durable cleanup intent must exist before even the first firewall mutation.
                records[key]={'ip':ip,'port':p['port'],'vmid':p.get('vmid'),'expiresAt':time.time()+lease}
                persist(state,records)
                run('-t','nat','-A','ALD_NAT','-p','tcp','--dport',str(p['port']),'-m','comment','--comment',key,'-j','DNAT','--to-destination',ip+':22')
                run('-I','ALD_ACCESS','1','-d',ip,'-p','tcp','--dport','22','-m','comment','--comment',key,'-j','ACCEPT')
                run('-t','nat','-A','ALD_SNAT','-d',ip,'-p','tcp','--dport','22','-m','comment','--comment',key,'-j','MASQUERADE')
        if p['action'] in ['open','close','check','reap']:
            persist(state,records)
        counts=[len(rules(table,chain,key)) for table,chain in [('nat','ALD_NAT'),('filter','ALD_ACCESS'),('nat','ALD_SNAT')]]
        return {'accessEnabled':counts==[1,1,1],'accessBlocked':counts==[0,0,0],'ruleIds':[key] if any(counts) else []}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--settings',required=True);parser.add_argument('--reap',action='store_true');args=parser.parse_args()
    try:
        if os.geteuid()!=0:raise RuntimeError('GATEWAY_ROOT_REQUIRED')
        payload={'action':'reap','rentalId':1,'serverId':1,'port':1024} if args.reap else json.loads(sys.stdin.read(16384));settings=json.loads(Path(args.settings).read_text());print(json.dumps(handle(payload,settings)))
    except Exception:print(json.dumps({'error':'GATEWAY_UNCONFIRMED'}));sys.exit(1)
