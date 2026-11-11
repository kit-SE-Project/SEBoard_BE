#!/usr/bin/env python3
"""Real local backend HTTP + separate manager; preserve unrelated presentation records."""
import json,sys,time,secrets,urllib.request,urllib.error,urllib.parse
from pathlib import Path
from datetime import datetime,timedelta,timezone
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT));from manager.common import HttpClient

def main():
    cfg=json.loads((ROOT/'.local/config.json').read_text())
    if cfg['mode']!='DEMO':raise RuntimeError('DEMO mode only')
    backend=HttpClient(cfg['backend_url'],'');agent=HttpClient('http://127.0.0.1:'+str(cfg['port']),cfg['agent_token'])
    data=urllib.parse.urlencode({'username':'junseok@demo.local','password':'Demo1234!','largeLogin':'false'}).encode()
    req=urllib.request.Request(cfg['backend_url']+'/formLogin',data=data,headers={'Content-Type':'application/x-www-form-urlencoded','ngrok-skip-browser-warning':'true'})
    with urllib.request.urlopen(req,context=backend.context,timeout=15) as response:tokens=json.load(response)
    backend.token=tokens['accessToken'];checks=[]
    def check(name,condition):
        if not condition:raise AssertionError(name)
        checks.append({'name':name,'status':'PASSED'})
    def mutation(path,body):
        request=urllib.request.Request(cfg['backend_url']+path,data=json.dumps(body).encode(),headers={'Authorization':'Bearer '+backend.token,'Content-Type':'application/json','Idempotency-Key':secrets.token_hex(16),'ngrok-skip-browser-warning':'true'})
        with urllib.request.urlopen(request,context=backend.context,timeout=15) as response:return json.load(response)
    scenario=backend.call('GET','/api/v1/demo/scenarios')
    at=datetime.now(timezone.utc)+timedelta(seconds=5)
    slots=[{'startAt':at.isoformat(),'endAt':(at+timedelta(seconds=18)).isoformat()},
           {'startAt':(at+timedelta(seconds=40)).isoformat(),'endAt':(at+timedelta(minutes=5)).isoformat()}]
    server=None
    for candidate in scenario['servers']:
        preview=backend.call('POST','/api/v1/rental-previews',{'serverId':candidate['id'],'purpose':'별도 관리 서버 STOP/START HTTP 검증','slots':slots})
        if preview['canReserve']:server=candidate;break
    if server is None:raise RuntimeError('No free DEMO server satisfying switch-time policy')
    created=mutation('/api/v1/rentals',{'serverId':server['id'],'purpose':'별도 관리 서버 STOP/START HTTP 검증','slots':slots});rental=created['id']
    def wait(expected,timeout=70):
        until=time.monotonic()+timeout
        while time.monotonic()<until:
            detail=backend.call('GET',f'/api/v1/rentals/{rental}')
            if detail['status']==expected:return detail
            time.sleep(.5)
        raise AssertionError('Expected '+expected+', last status '+detail['status'])
    first=wait('ACTIVE');vmid=first['vm']['vmid'];check('첫 슬롯 ACTIVE와 별도 관리 VM 생성',vmid is not None)
    retained=wait('RETAINED');check('슬롯 종료 STOPPED 및 GPU 분리',retained['vm']['state']=='STOPPED')
    operations=agent.call('GET','/v1/operations');check('관리 서버 STOP 결과 GPU 분리 확인',any(o['status']=='SUCCEEDED' and o['result'] and o['result'].get('observation',{}).get('gpuDetached') is True for o in operations))
    second=wait('ACTIVE');check('다음 슬롯에서 같은 VMID 재사용',second['vm']['vmid']==vmid)
    check('다음 슬롯 접속 안내',backend.call('GET',f'/api/v1/rentals/{rental}/connection')['available'])
    mutation(f'/api/v1/rentals/{rental}/return',{'expectedVersion':second['version']});wait('RETURNED')
    verification=backend.call('GET',f'/api/v1/rentals/{rental}/return-verifications')
    check('반납 5항목은 명시적으로 시뮬레이션',all(i['passed'] and i['evidence']['simulation'] for i in verification[0]['items']))
    jobs=agent.call('GET','/v1/jobs');check('반납 결과 전송 완료',any(j['stage']=='DONE' for j in jobs))
    (ROOT/'docs').mkdir(exist_ok=True)
    (ROOT/'docs/demo-e2e.json').write_text(json.dumps({'executedAt':datetime.now(timezone.utc).isoformat(),'backend':cfg['backend_url'],'mode':'DEMO','rentalId':rental,'vmid':vmid,'checks':checks,'realHardwareVerified':False},ensure_ascii=False,indent=2)+'\n')
    backend.call('POST','/logoutProc',{'refreshToken':tokens['refreshToken']})
    print('별도 관리 서버 실제 HTTP STOP/START 검증:',len(checks),'개 통과. 실제 장비는 미검증.')

if __name__=='__main__':main()
