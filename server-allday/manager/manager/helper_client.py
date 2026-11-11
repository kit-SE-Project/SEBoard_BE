"""Node Helper v0.2 JSON/SSH client. No PVE or shell mutation is performed here."""
import hashlib,ipaddress,json,math,re,subprocess,threading,uuid
from datetime import datetime,timezone
from pathlib import Path
from .common import Problem,canonical


class HelperTransport:
    def __init__(self,config):
        self.host=str(ipaddress.ip_address(config['host']))
        self.key=Path(config['key_file']).resolve(strict=True)
        self.known=Path(config['known_hosts_file']).resolve(strict=True)
        self.port=config.get('port',22);self.timeout=config.get('timeout',30)
        if type(self.port) is not int or not 1<=self.port<=65535 or type(self.timeout) is not int or not 5<=self.timeout<=120:raise ValueError('Invalid Helper SSH settings')
    def __call__(self,request):
        args=['ssh','-T','-p',str(self.port),'-i',str(self.key),'-o','BatchMode=yes','-o','IdentitiesOnly=yes','-o','StrictHostKeyChecking=yes','-o','UserKnownHostsFile='+str(self.known),'-o','ConnectTimeout=5','-o','ServerAliveInterval=5','-o','ServerAliveCountMax=2','allday-helper@'+self.host]
        try:result=subprocess.run(args,input=canonical(request).encode(),capture_output=True,timeout=self.timeout,shell=False)
        except (OSError,subprocess.TimeoutExpired):raise Problem('HELPER_TRANSPORT_UNCERTAIN') from None
        if result.returncode not in [0,1] or len(result.stdout)>65536:raise Problem('HELPER_TRANSPORT_UNCERTAIN')
        try:response=json.loads(result.stdout)
        except (ValueError,UnicodeError):raise Problem('HELPER_INVALID_RESPONSE') from None
        if result.returncode!=(0 if response.get('ok') is True else 1):raise Problem('HELPER_INVALID_RESPONSE')
        return response


class HelperClient:
    def __init__(self,store,transports):
        self.store=store;self.transports=transports;self.locks={key:threading.RLock() for key in transports}
        with store.connect() as db:
            db.executescript('''CREATE TABLE IF NOT EXISTS helper_workflows(operation_id TEXT PRIMARY KEY,server_id INTEGER NOT NULL,rental_id INTEGER NOT NULL,control_revision INTEGER NOT NULL,base_generation INTEGER NOT NULL,current_generation INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS helper_steps(operation_id TEXT NOT NULL,step TEXT NOT NULL,request TEXT NOT NULL,response TEXT,PRIMARY KEY(operation_id,step));''')
    def send(self,request):
        response=self.transports[str(request['serverId'])](request)
        if not isinstance(response,dict) or response.get('mode')!='system' or type(response.get('schemaVersion')) is not int or response['schemaVersion']!=1 or type(response.get('serverId')) is not int or any(response.get(k)!=request[k] for k in ['commandId','serverId','operation']) or type(response.get('ok')) is not bool:raise Problem('HELPER_INVALID_RESPONSE')
        if response['ok'] is not True:
            code=response.get('code')
            raise Problem('HELPER_'+code if isinstance(code,str) and re.fullmatch(r'[A-Z0-9_]{1,80}',code) else 'HELPER_FAILURE')
        if response.get('code')!='OK' or not isinstance(response.get('result'),dict):raise Problem('HELPER_INVALID_RESPONSE')
        return response['result']
    def status(self,server):
        return self.send({'schemaVersion':1,'commandId':'status-'+str(uuid.uuid4()),'serverId':server,'operation':'READ_STATUS','payload':{}})
    def begin(self,operation,p):
        prior=self.store.row('SELECT * FROM helper_workflows WHERE operation_id=?',operation['id'])
        if prior:return prior
        status=self.status(p['serverId'])
        generation=status.get('generation');remote=status.get('controlRevision')
        if type(generation) is not int or generation<0 or (remote is not None and (type(remote) is not int or remote<0)):raise Problem('HELPER_INVALID_RESPONSE')
        with self.store.connect() as db:
            current=db.execute('SELECT max(control_revision) FROM helper_workflows WHERE server_id=?',(p['serverId'],)).fetchone()[0] or 0
            revision=max(current,remote or 0)+1
            db.execute('INSERT INTO helper_workflows VALUES (?,?,?,?,?,?)',(operation['id'],p['serverId'],p['rentalId'],revision,generation,generation))
        return self.workflow(operation)
    def workflow(self,operation):
        row=self.store.row('SELECT * FROM helper_workflows WHERE operation_id=?',operation['id'])
        if not row:raise Problem('HELPER_WORKFLOW_REQUIRED')
        return row
    def call(self,operation,step,kind,payload,advance=False):
        wf=self.workflow(operation)
        prior=self.store.row('SELECT * FROM helper_steps WHERE operation_id=? AND step=?',operation['id'],step)
        if prior:
            request=json.loads(prior['request'])
            if request['operation']!=kind or request['payload']!=payload:raise Problem('HELPER_STEP_CHANGED')
            if prior['response']:return json.loads(prior['response'])
        else:
            generation=wf['base_generation']+1 if advance else wf['current_generation']
            request={'schemaVersion':1,'commandId':'manager-'+hashlib.sha256((operation['id']+':'+step).encode()).hexdigest(),'serverId':wf['server_id'],'operation':kind,'generation':generation,'controlRevision':wf['control_revision'],'payload':payload}
            self.store.execute('INSERT INTO helper_steps VALUES (?,?,?,NULL)',operation['id'],step,canonical(request))
        response=self.send(request)
        if advance and (response.get('generation')!=request['generation'] or response.get('ownerRentalId')!=wf['rental_id'] or response.get('stopVerified') is not True):raise Problem('HELPER_CLAIM_UNCONFIRMED')
        with self.store.connect() as db:
            db.execute('UPDATE helper_steps SET response=? WHERE operation_id=? AND step=?',(canonical(response),operation['id'],step))
            if advance:db.execute('UPDATE helper_workflows SET current_generation=? WHERE operation_id=?',(request['generation'],operation['id']))
        return response
    def close(self,operation,step):
        result=self.call(operation,step,'DISABLE_INGRESS',{})
        if result.get('enabled') is not False or result.get('oldConnectionsCleared') is not True:raise Problem('ACCESS_BLOCK_UNCONFIRMED')
        return result
    def open(self,operation,rental,seconds,renew=False):
        # Every health renewal is a new command. Replayed APPLY does not extend TTL.
        return self.call(operation,('renew-' if renew else 'open-')+str(uuid.uuid4()),'RENEW_INGRESS' if renew else 'APPLY_INGRESS',{'rentalId':rental,'leaseSeconds':seconds})


def lease_seconds(window,now=None):
    if not isinstance(window,dict) or type(window.get('maxLeaseSeconds')) is not int:return 0
    try:
        checked=datetime.fromisoformat(window['checkedAt'].replace('Z','+00:00'))
        until=datetime.fromisoformat(window['validUntil'].replace('Z','+00:00'))
        end=datetime.fromisoformat(window['slotEndAt'].replace('Z','+00:00'))
        current=now or datetime.now(timezone.utc)
        if any(v.tzinfo is None for v in [checked,until,end,current]) or (checked-current).total_seconds()>2:return 0
        remaining=min((until-current).total_seconds(),(end-current).total_seconds(),window['maxLeaseSeconds'])
    except (ValueError,KeyError,TypeError):return 0
    seconds=min(45,math.floor(remaining)-5)
    return seconds if seconds>=5 else 0


def password_hash(password):
    try:from passlib.hash import sha512_crypt
    except ImportError:raise Problem('INSTALL_MANAGER_REQUIREMENTS') from None
    # Host-required SHA-512 crypt; this is unrelated to member BCrypt hashes.
    return sha512_crypt.using(rounds=100000).hash(password)
