import argparse,hmac,json,logging,re,signal,threading,time,os
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
from .common import HttpClient,Problem,RemoteProblem,now
from .store import Store
from .agent import Agent
from .worker import Worker
from .demo import DemoDriver
from .proxmox import ProxmoxDriver
from .helper_driver import driver_class,HelperProxmoxDriver

log=logging.getLogger('allday-manager')
@contextmanager
def process_lock(state_path):
    path=Path(state_path).with_suffix('.process.lock');path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('a+b') as handle:
        handle.seek(0);handle.write(b'0');handle.flush();handle.seek(0)
        try:
            if os.name=='nt':
                import msvcrt
                msvcrt.locking(handle.fileno(),msvcrt.LK_NBLCK,1)
            else:
                import fcntl
                fcntl.flock(handle,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except OSError:raise RuntimeError('This operation store already has a running manager') from None
        try:yield
        finally:
            handle.seek(0)
            if os.name=='nt':msvcrt.locking(handle.fileno(),msvcrt.LK_UNLCK,1)
            else:fcntl.flock(handle,fcntl.LOCK_UN)

def load_config(path):
    cfg=json.loads(Path(path).read_text(encoding='utf-8-sig'))
    if cfg.get('mode') not in ['DEMO','PROXMOX'] or not re.fullmatch(r'[A-Za-z0-9_.-]{1,100}',cfg.get('worker_id','')):raise ValueError('Invalid manager mode or worker ID')
    if cfg.get('gateway_protocol','LEGACY_GATEWAY') not in ['LEGACY_GATEWAY','NODE_HELPER_V02']:raise ValueError('Invalid gateway protocol')
    if len(cfg.get('agent_token',''))<32 or len(cfg.get('worker_token',''))<24 or cfg['agent_token']==cfg['worker_token']:raise ValueError('Separate agent and worker tokens required')
    if cfg['mode']=='PROXMOX' and not cfg.get('server_ids'):raise ValueError('Real mode requires assigned server IDs')
    if type(cfg.get('auto_recover_return',False)) is not bool:raise ValueError('auto_recover_return must be boolean')
    return cfg

class Runtime:
    def __init__(self,cfg):
        self.config=cfg;self.store=Store(cfg['state_path']);self.store.recover();self.backend=HttpClient(cfg['backend_url'],cfg['worker_token'],ca=cfg.get('backend_ca_file'))
        self.driver=DemoDriver(self.store) if cfg['mode']=='DEMO' else (HelperProxmoxDriver(self.store,cfg,self.backend) if driver_class(cfg) is HelperProxmoxDriver else ProxmoxDriver(self.store,cfg))
        self.agent=Agent(self.store,self.backend,self.driver,cfg['worker_id'],cfg.get('server_ids',[]));self.worker=Worker(self.store,self.backend,self.agent,cfg);self.stop_event=threading.Event();self.backend_status='CONNECTING';self.threads=[]
    def run_worker(self):
        while not self.stop_event.is_set():
            try:self.worker.tick();self.backend_status='CONNECTED'
            except Problem as e:self.backend_status=e.code;self.worker.fail_closed();log.warning('backend cycle deferred: %s',e.code)
            except Exception:self.backend_status='INTERNAL_ERROR';self.worker.fail_closed();log.warning('backend cycle deferred: INTERNAL_ERROR')
            self.stop_event.wait(2)
    def run_agent(self):
        while not self.stop_event.is_set():
            try:self.agent.run_pending()
            except Exception:log.warning('agent cycle deferred: INTERNAL_ERROR')
            self.stop_event.wait(.5)
    def start(self):
        for target in [self.run_worker,self.run_agent]:
            t=threading.Thread(target=target,daemon=True);t.start();self.threads.append(t)
    def health(self):return {'status':'UP','mode':self.config['mode'],'backend':self.backend_status,'workerId':self.config['worker_id'],'operationStore':'SQLite','realHardwareVerified':False}

def handler(runtime):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self,*args):pass
        def send(self,status,body):
            encoded=json.dumps(body,ensure_ascii=False).encode();self.send_response(status);self.send_header('Content-Type','application/json; charset=utf-8');self.send_header('Content-Length',str(len(encoded)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff');self.end_headers();self.wfile.write(encoded)
        def authorized(self):
            header=self.headers.get('Authorization','');token=header[7:] if header.startswith('Bearer ') else ''
            if not hmac.compare_digest(token,runtime.config['agent_token']):raise Problem('UNAUTHENTICATED',401)
        def route(self):
            path=self.path.split('?')[0]
            if path=='/health' and self.command=='GET':return self.send(200,runtime.health())
            self.authorized()
            if self.command=='GET' and path in ['/v1/operations','/v1/recovery']:
                where="WHERE status='UNKNOWN'" if path.endswith('recovery') else ''
                return self.send(200,[runtime.store.public(x) for x in runtime.store.rows(f'SELECT * FROM operations {where} ORDER BY created_at DESC LIMIT 100')])
            if self.command=='GET' and path=='/v1/jobs':return self.send(200,runtime.store.rows('SELECT job_id,command_id,operation_id,stage,updated_at FROM jobs ORDER BY job_id DESC LIMIT 100'))
            if self.command=='GET' and re.fullmatch(r'/v1/operations/op-[a-f0-9-]{36}',path):
                row=runtime.store.row('SELECT * FROM operations WHERE id=?',path.rsplit('/',1)[1])
                if row is None:raise Problem('NOT_FOUND',404)
                return self.send(200,runtime.store.public(row))
            if self.command=='POST' and path=='/v1/shutdown':
                if self.client_address[0] not in ['127.0.0.1','::1']:raise Problem('LOCAL_ONLY',403)
                self.send(200,{'stopping':True});runtime.shutdown();return
            if self.command=='POST' and path=='/v1/operations':
                length=int(self.headers.get('Content-Length','0'))
                if length<1 or length>65536:raise Problem('INVALID_BODY_SIZE',413)
                if self.headers.get('Content-Type','').split(';')[0]!='application/json':raise Problem('JSON_REQUIRED',415)
                payload=json.loads(self.rfile.read(length));return self.send(202,runtime.agent.submit(payload))
            raise Problem('NOT_FOUND',404)
        def dispatch(self):
            try:self.route()
            except Problem as e:self.send(e.status,{'code':e.code})
            except (ValueError,KeyError,TypeError):self.send(422,{'code':'INVALID_INPUT'})
            except Exception:self.send(500,{'code':'INTERNAL_ERROR'})
        do_GET=dispatch;do_POST=dispatch
    return Handler

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--config',default='.local/config.json');args=parser.parse_args();cfg=load_config(args.config)
    with process_lock(cfg['state_path']):serve(cfg)

def serve(cfg):
    runtime=Runtime(cfg);server=ThreadingHTTPServer((cfg.get('bind','127.0.0.1'),cfg.get('port',19090)),handler(runtime));runtime.start()
    def stop(*args):runtime.stop_event.set();threading.Thread(target=server.shutdown,daemon=True).start()
    runtime.shutdown=stop
    signal.signal(signal.SIGTERM,stop);signal.signal(signal.SIGINT,stop)
    logging.basicConfig(level=logging.INFO,format='%(levelname)s %(message)s')
    log.info('management server started mode=%s port=%s',cfg['mode'],cfg.get('port',19090))
    try:server.serve_forever()
    finally:runtime.stop_event.set();server.server_close()

if __name__=='__main__':main()
