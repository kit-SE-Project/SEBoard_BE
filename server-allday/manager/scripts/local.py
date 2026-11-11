#!/usr/bin/env python3
import argparse,json,os,secrets,signal,subprocess,sys,time,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];LOCAL=ROOT/'.local';CONFIG=LOCAL/'config.json';PID=LOCAL/'manager.pid'
BACKEND=ROOT.parent/'api'
sys.path.insert(0,str(ROOT))

def check_configuration(path):
    from manager.server import load_config
    from manager.common import HttpClient
    from manager.proxmox import ProxmoxDriver
    cfg=load_config(path)
    HttpClient(cfg['backend_url'],cfg['worker_token'],ca=cfg.get('backend_ca_file'))
    if type(cfg.get('port',19090)) is not int or not 1024<=cfg.get('port',19090)<=65535:raise ValueError('Invalid management API port')
    if not isinstance(cfg.get('state_path'),str) or not cfg['state_path'].strip():raise ValueError('Operation store path required')
    if cfg['mode']=='PROXMOX':
        from manager.helper_driver import driver_class,HelperProxmoxDriver
        cls=driver_class(cfg)
        if cls is HelperProxmoxDriver:
            from passlib.hash import sha512_crypt
            driver=cls(None,cfg,None)
        else:driver=cls(None,cfg)
        for server in cfg['server_ids']:
            if type(server) is not int or server<1:raise ValueError('Positive assigned server IDs required')
            driver.node(server)
    return cfg

def check_config():
    if not CONFIG.exists():raise RuntimeError('먼저 setup --connection-file 연결파일 경로로 설정하세요.')
    cfg=check_configuration(CONFIG)
    print(json.dumps({'configuration':'VALID','mode':cfg['mode'],'backendUrl':cfg['backend_url'],'port':cfg.get('port',19090),'networkTested':False,'realHardwareVerified':False},ensure_ascii=False))
    return cfg

def run():
    check_config()
    print('관리 서버를 현재 터미널에서 실행합니다. 종료: Ctrl+C',flush=True)
    try:subprocess.run([sys.executable,'-m','manager.server','--config',str(CONFIG)],cwd=ROOT,check=True)
    except KeyboardInterrupt:print('\n관리 서버 종료 요청. 작업 기록은 유지됩니다.')

def setup(connection=None,url=None):
    LOCAL.mkdir(exist_ok=True,mode=0o700)
    if os.name=='nt':
        result=subprocess.run(['powershell.exe','-NoProfile','-Command','[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value'],capture_output=True,text=True,check=True)
        user_sid=result.stdout.strip()
        if not user_sid.startswith('S-1-'):raise RuntimeError('Windows account identity unavailable')
        subprocess.run(['icacls',str(LOCAL),'/inheritance:r','/grant:r','*'+user_sid+':(OI)(CI)F','*S-1-5-18:(OI)(CI)F'],capture_output=True,check=True)
    cfg=json.loads(CONFIG.read_text(encoding='utf-8-sig')) if CONFIG.exists() else {'mode':'DEMO','worker_id':'manager-'+secrets.token_hex(6),'agent_token':secrets.token_hex(32),'bind':'127.0.0.1','port':19090,'state_path':str(LOCAL/'state.sqlite3'),'server_ids':[],'allow_real_mutations':False,'network_isolation_confirmed':False}
    if connection:info=json.loads(Path(connection).read_text(encoding='utf-8-sig'));cfg['worker_token']=info['worker_token'];cfg['backend_url']=info['backend_url']
    elif not CONFIG.exists():
        private=BACKEND/'.local/development.json'
        if not private.exists():raise RuntimeError('관리 컴퓨터에서는 setup --connection-file 비밀연결파일 경로를 지정하세요.')
        cfg['worker_token']=json.loads(private.read_text(encoding='utf-8-sig'))['worker_token']
        public=BACKEND/'docs/ngrok-current.json'
        cfg['backend_url']=json.loads(public.read_text(encoding='utf-8-sig'))['publicBaseUrl'] if public.exists() else 'http://127.0.0.1:18082'
    if url:cfg['backend_url']=url
    fd=os.open(CONFIG,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
    with os.fdopen(fd,'w') as f:json.dump(cfg,f,indent=2)
    CONFIG.chmod(0o600)
    # This is a private transfer artifact for the actual management computer, never in ZIP.
    transfer=BACKEND/'.local/manager-connection.json'
    if (BACKEND/'.local').exists():
        fd=os.open(transfer,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
        with os.fdopen(fd,'w') as f:json.dump({'backend_url':cfg['backend_url'],'worker_token':cfg['worker_token']},f,indent=2)
    print('관리 서버 비밀 설정 준비 완료. 백엔드:',cfg['backend_url']);return cfg

def own(pid):
    if os.name=='nt':return False
    result=subprocess.run(['ps','-p',str(pid),'-o','args='],text=True,capture_output=True)
    return '-m manager.server' in result.stdout and str(CONFIG) in result.stdout
def running(cfg):
    try:
        with urllib.request.urlopen(f'http://127.0.0.1:{cfg["port"]}/health',timeout=2) as response:
            return json.load(response).get('workerId')==cfg['worker_id']
    except Exception:return False

def start():
    cfg=json.loads(CONFIG.read_text(encoding='utf-8-sig')) if CONFIG.exists() else setup()
    if running(cfg):print('관리 서버가 이미 실행 중입니다.');return
    check_configuration(CONFIG)
    log=LOCAL/'manager.log';log.touch(mode=0o600,exist_ok=True);log.chmod(0o600)
    options={'creationflags':subprocess.CREATE_NEW_PROCESS_GROUP | subprocess.DETACHED_PROCESS} if os.name=='nt' else {'start_new_session':True}
    with log.open('ab') as output:process=subprocess.Popen([sys.executable,'-m','manager.server','--config',str(CONFIG)],cwd=ROOT,stdin=subprocess.DEVNULL,stdout=output,stderr=subprocess.STDOUT,**options)
    PID.write_text(str(process.pid))
    for _ in range(100):
        if process.poll() is not None:raise RuntimeError('관리 서버 실행 실패. .local/manager.log를 확인하세요.')
        if running(cfg):print('관리 서버 실행 완료: http://127.0.0.1:'+str(cfg['port']));return
        time.sleep(.2)
    raise RuntimeError('관리 서버 기동 시간 초과')
def stop():
    if CONFIG.exists():
        cfg=json.loads(CONFIG.read_text(encoding='utf-8-sig'))
        if running(cfg):
            req=urllib.request.Request(f'http://127.0.0.1:{cfg["port"]}/v1/shutdown',data=b'{}',headers={'Authorization':'Bearer '+cfg['agent_token'],'Content-Type':'application/json'},method='POST')
            with urllib.request.urlopen(req,timeout=3) as response:json.load(response)
            for _ in range(50):
                if not running(cfg):break
                time.sleep(.1)
            else:raise RuntimeError('관리 서버 중지가 지연되고 있습니다.')
    PID.unlink(missing_ok=True)
    print('관리 서버를 중지했습니다. 작업 기록은 유지됩니다.')
def status():
    cfg=json.loads(CONFIG.read_text(encoding='utf-8-sig'))
    with urllib.request.urlopen(f'http://127.0.0.1:{cfg["port"]}/health',timeout=2) as response:print(json.dumps(json.load(response),ensure_ascii=False))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('command',choices=['setup','run','check-config','start','stop','status','test']);parser.add_argument('--connection-file');parser.add_argument('--backend-url');args=parser.parse_args()
    try:
        if args.command=='setup':setup(args.connection_file,args.backend_url)
        elif args.command=='run':run()
        elif args.command=='check-config':check_config()
        elif args.command=='start':start()
        elif args.command=='stop':stop()
        elif args.command=='status':status()
        else:subprocess.run([sys.executable,str(ROOT/'scripts/test.py')],cwd=ROOT,check=True)
    except Exception as e:print(str(e),file=sys.stderr);sys.exit(1)
