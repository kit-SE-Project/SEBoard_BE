import hashlib, json, ssl, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path
from datetime import datetime, timezone

def now():return datetime.now(timezone.utc).isoformat()
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'),ensure_ascii=False)
def digest(value):return hashlib.sha256(canonical(value).encode()).hexdigest()
class Problem(Exception):
    def __init__(self,code,status=409):self.code=code;self.status=status;super().__init__(code)
class RemoteProblem(Problem):pass
class HttpClient:
    def __init__(self,base,token,timeout=15,ca=None):
        parsed=urllib.parse.urlsplit(base)
        if parsed.scheme!='https' and not(parsed.scheme=='http' and parsed.hostname in ['localhost','127.0.0.1','::1']):raise ValueError('Remote service requires HTTPS')
        if parsed.username or parsed.password or parsed.query or parsed.fragment:raise ValueError('Invalid base URL')
        if ca is None and ssl.get_default_verify_paths().cafile is None:ca=next((p for p in ['/etc/ssl/cert.pem','/opt/homebrew/etc/openssl@3/cert.pem'] if Path(p).exists()),None)
        self.base=base.rstrip('/');self.token=token;self.timeout=timeout;self.context=ssl.create_default_context(cafile=ca)
    def call(self,method,path,body=None):
        headers={'Authorization':'Bearer '+self.token,'ngrok-skip-browser-warning':'true','Content-Type':'application/json'}
        request=urllib.request.Request(self.base+path,data=None if body is None else canonical(body).encode(),headers=headers,method=method)
        try:
            with urllib.request.urlopen(request,timeout=self.timeout,context=self.context) as r:return json.load(r)
        except urllib.error.HTTPError as e:
            try:code=json.loads(e.read(65536)).get('code','HTTP_ERROR')
            except Exception:code='HTTP_ERROR'
            raise RemoteProblem(code,e.code) from None
        except (urllib.error.URLError,TimeoutError,OSError):raise RemoteProblem('TRANSPORT_UNCERTAIN',503) from None
    def query(self,path,**params):return self.call('GET',path+'?'+urllib.parse.urlencode(params))
