# Proxmox 시험 장비 설치 절차

대상은 Proxmox 호스트 OS입니다. 학생 Ubuntu VM에 설치하지 않습니다. 이 문서의 명령은 실제 시험 장비에서 관리자가 실행하는 절차입니다.

## 1. 장비 값을 먼저 확정

`config/proxmox.example.json`을 별도 실제 설정 파일로 복사해 값을 채웁니다. `192.0.2.20`은 설명용 주소입니다. 아래 항목이 실제 장비와 일치해야 설치 사전 검사가 통과합니다.

| 설정 | 의미 / 확인 |
|---|---|
| serverId | 서비스 DB의 물리 서버 식별자 |
| nodeName | 실제 Proxmox 노드명. `hostname`의 첫 부분과 일치 |
| nodeIp | 학교 라우터가 전달한 패킷이 C에서 도착하는 실제 IPv4 주소. 공인 주소와 같다고 가정하지 않음 |
| nodeIngressPort | 학교 NAT 이후 C가 받는 학생 포트. 외부 포트와 다를 수 있음 |
| uplinkBridge | 학교망 브리지, 일반적으로 vmbr0 |
| vmBridge | VM 전용 격리 브리지, 일반적으로 vmbr1 |
| vmSubnet / vmGateway | 노드 전용 사설 대역과 vmbr1의 게이트웨이 주소 |
| vmInterface | Ubuntu 템플릿 안의 실제 NIC 이름. `ip link`로 확인 |
| nameservers | 학교가 허용하는 DNS. VM에서 사용할 DNS |
| blockedNetworks | 학생 VM에서 접근하지 못하게 할 학교망·A/B 관리망·공인 관리 서버 대역. 사설 주소만 막는 것으로 충분하지 않음 |
| storageIds | 디스크와 백업 잔존 여부를 모두 확인할 저장소 목록 |
| snippetStorage / snippetDirectory | snippets content를 지원하는 Proxmox 저장소 ID와 실제 로컬 디렉터리 |
| vmIdMin / vmIdMax | 이 서비스의 학생 VM 전용 범위. 예시 10000~19999. 모든 학생 VM은 범위 안, 템플릿은 범위 밖 |
| conntrackZones | 해당 호스트에서 NAT 연결이 기록되는 zone. 예시는 0·1이며 실장비에서 확인 |

호스트 관리 SSH는 내부망의 22 포트, 학생 진입은 예시 7540으로 분리합니다. 기존 관리 접속이 7540이라면 다른 관리 경로를 먼저 확보해야 합니다. Helper는 사용자 진입 포트가 호스트 로그인으로 연결되지 않게 차단합니다.

## 2. 학교망 브리지와 VM 브리지

기존 vmbr0의 학교 지정 IP·서브넷·게이트웨이·DNS·물리 NIC를 유지합니다. VM 전용 브리지는 별도로 구성합니다. 아래는 `/etc/network/interfaces`에 추가할 예시입니다.

```text
auto vmbr1
iface vmbr1 inet static
    address 10.70.1.1/24
    bridge-ports none
    bridge-stp off
    bridge-fd 0
```

`bridge-ports none`으로 학교 물리 NIC에 VM 사설망을 연결하지 않습니다. 기존 학교망·다른 노드 VM망·K3s Pod망과 대역이 겹치지 않아야 합니다. 네트워크 적용은 장비 콘솔 접속이 확보된 상태에서 운영 절차에 따라 수행합니다. 이 프로젝트 설치 프로그램은 학교망 설정을 변경하지 않습니다.

IPv4 포워딩을 유지하려면 `/etc/sysctl.d/90-server-allday.conf`에 다음 값을 설정하고 적용합니다.

```text
net.ipv4.ip_forward = 1
```

```bash
sysctl --system
```

예시 포트 7540을 nftables로 VM의 22로 전달합니다. VM sshd의 포트는 22이며 학교 외부 포트와 같게 바꾸지 않습니다. 외부 주소·포트 → 학교 라우터 → nodeIp:nodeIngressPort → VM IP:22가 실제 접속 경로입니다.

## 3. 도구와 템플릿 준비

시험 노드에서 필요한 공식 패키지를 설치합니다.

```bash
apt-get update
apt-get install python3 nftables conntrack sudo
```

Proxmox가 제공하는 `pvesh`도 사용합니다. Python 패키지 설치는 필요 없습니다. `nftables.service`를 활성화해 과거 활성 대상 규칙을 복원하지 않습니다. 기존 서비스가 이미 관리 중이면 부팅 순서와 저장된 규칙을 먼저 확인합니다. 프로젝트는 다른 방화벽 테이블이나 `pve-firewall`을 삭제/중지하지 않습니다.

이 버전의 snippet 저장은 Proxmox의 로컬 `dir` 저장소를 사용합니다. 저장소 설정의 기존 content 목록을 유지하면서 `snippets`를 추가하고, 해당 저장소의 실제 `path/snippets`를 snippetDirectory로 지정합니다. 예시 local 저장소 경로는 `/var/lib/vz/snippets`입니다. installer가 Proxmox의 실제 저장소 설정을 조회해 두 값이 맞는지 검사합니다.

Ubuntu cloud 이미지 템플릿에는 cloud-init 드라이브, QEMU Guest Agent, SSH 서버, JupyterLab 및 GPU 환경을 사전 구성합니다. JupyterLab은 `127.0.0.1:8888`에서 실행합니다. 학생은 공개 SSH 포트 하나로 접속한 뒤 로컬 SSH 포워딩으로 Jupyter를 사용할 수 있습니다. Helper가 Jupyter를 설치하거나 실행하지는 않습니다.

```bash
ssh -p EXTERNAL_PORT -L 18888:127.0.0.1:8888 r1042@EXTERNAL_HOST
```

노트북 브라우저에서 `http://localhost:18888`을 엽니다. Jupyter 자체의 인증 정책은 템플릿/Agent에서 정합니다.

## 4. Helper 파일 배포

ZIP 소스를 시험 노드에 전달하거나 제공된 Debian 설치 파일을 사용할 수 있습니다.

```bash
apt install ./server-allday-node-helper_0.2.0_all.deb
cd /opt/server-allday/node-helper
```

Debian 패키지는 코드·예제·설명서를 설치하며 계정·관리 키·접속 규칙을 자동 활성화하지 않습니다. 아래 설정 단계에서 장비별 값과 B 공개키를 지정합니다. 패키지를 설치했어도 아래 사전 검사와 Helper 설정은 반드시 진행합니다.

ZIP을 사용한다면 압축을 푼 프로젝트 폴더에서 같은 설정 명령을 실행하면 됩니다. 실제 설정 파일은 root 소유이며 다른 사용자가 수정할 수 없어야 합니다. 관리 서버에서 전용 Ed25519 키를 생성하고 **공개키만** Proxmox 노드로 전달합니다. 개인키는 B에 남깁니다.

```bash
install -d -m 0700 /etc/server-allday/keys
ssh-keygen -t ed25519 -f /etc/server-allday/keys/node-helper
```

개인키의 무인 사용·보관 권한은 B의 서비스 계정 정책에 맞춥니다. 학생 접속 암호와 이 관리 키는 별개입니다.

시험 노드에서 먼저 사전 검사만 실행합니다.

```bash
python3 deploy/install.py \
  --config /root/allday-node.json \
  --public-key /root/node-helper.pub \
  --manager-ip B_INTERNAL_SOURCE_IP \
  --check-only
```

실제 브리지 IP, VM 사설망, 포워딩, 관리 키, 전용 계정 충돌, 학생 포트의 호스트 리스너를 검사합니다. 통과하면 `--check-only`를 빼고 실행합니다. 설정 경로·키·IP는 실제 값을 사용합니다. 이 설치는 코드·키 제한·sudoers·부팅 서비스를 설치하며 아직 방화벽을 활성화하지 않습니다.

| 설치 위치 | 내용 |
|---|---|
| /opt/server-allday/node-helper | root 소유 Python 코드 |
| /etc/server-allday/node-helper/config.json | root 전용 호스트 설정 |
| /var/lib/server-allday/node-helper | SQLite 명령 결과·세대·점유·감사 기록 |
| /var/lib/server-allday/helper-login/.ssh/authorized_keys | B의 출발지 IP와 고정 명령으로 제한된 공개키 |
| /usr/local/sbin/allday-node-helper | 인자 없는 SSH 실행 진입점 |
| /etc/sudoers.d/server-allday-helper | 해당 진입점만 root로 실행 허용 |
| /etc/systemd/system/allday-node-helper-bootstrap.service | 부팅 시 빈 접속 대상 적용 |

일반 계정이 코드·설정·키·진입점을 수정할 수 없으며 전용 계정은 임의 명령을 실행할 수 없습니다. installer는 sshd 전역 설정을 변경하거나 관리 SSH를 재시작하지 않습니다.

## 5. 접속 차단 상태로 초기화

모든 학생 VM의 `onboot=0`을 확인합니다. 예기치 않은 호스트 재부팅에서 학생 VM이 동시에 시작되지 않도록 하는 설정입니다. 그 뒤 시험 노드에서 부팅 서비스를 활성화합니다.

```bash
systemctl enable --now allday-node-helper-bootstrap.service
systemctl status allday-node-helper-bootstrap.service
nft -j list table inet allday
```

`active_vm` set과 `ingress_target` map이 비어 있어야 합니다. 프로젝트는 `table inet allday`만 관리합니다. inet family를 사용해 IPv4 NAT와 VM IPv6 차단을 함께 처리합니다. 이전 설계의 `table ip allday` 명령과 섞어 쓰지 않습니다. 기존 ip family의 동명 프로젝트 규칙이 있다면 관리자가 기존 설계를 확인하고 정리한 뒤 설치합니다.

v0.2.0부터 `access_lease` timeout set도 비어 있어야 합니다. 기존 버전의 규칙과 구조가 다르므로 패키지만 업그레이드한 상태로 접속을 열지 않습니다. 점검 시간에 기존 접속 차단과 VM 상태 확인 후 installer 재실행, 부팅 서비스 재시작 순서로 규칙을 갱신합니다. SQLite 파일을 삭제하지 않습니다. 상세 순서는 `WINDOWS_INTEGRATION.md`를 따릅니다.

프로그래밍상 규칙 적용은 JSON 배치를 `nft -j -c -f -`로 검사하고 `nft -j -f -`로 한 트랜잭션에서 적용합니다. 사용자 변경 때 규칙을 append하지 않고 map·set 원소를 동시에 교체합니다. 적용 직후 전체 관리 규칙과 대상 IP를 읽어 비교합니다.

## 6. 관리 Agent 연결

장비 콘솔에서 SSH 호스트키 지문을 확인해 B의 `known_hosts`에 신뢰된 키를 등록합니다. `StrictHostKeyChecking=no`로 우회하지 않습니다. `docs/API.md`의 SSH 호출로 `READ_STATUS`를 실행합니다. `mode=system`인지 확인합니다.

`from=`에 등록하는 B 출발지 IP는 C가 실제로 관측한 주소입니다. 관리 구간에도 NAT가 있다면 B의 로컬 주소와 다를 수 있습니다. Agent가 파일을 저장하는 용도로 일반 SFTP를 시도할 필요는 없습니다. 고정 Helper에 JSON을 보내고 Helper가 파일을 생성합니다.

첫 연동은 GPU 없이 VM 하나로 진행합니다. REGISTER_RESOURCE → PUT_SNIPPET → Proxmox clone/config → DISABLE_INGRESS → ADVANCE_GENERATION → Proxmox start/건강 검사 → APPLY_INGRESS 순서를 연결합니다. 그 다음 A/B 전환, GPU, 최종 반납으로 넓힙니다.

접속이 열린 동안 B는 기본 20초마다 새 명령 ID로 RENEW_INGRESS를 보내야 합니다. 기본 유효 시간은 60초입니다. 갱신 전에 현재 예약·종료 시각·백엔드 연결·작업 revision을 확인하고, 유효함을 확인할 수 없으면 갱신하지 않습니다. Windows용 변환 예제와 두 번호의 매핑은 `WINDOWS_INTEGRATION.md`에 있습니다.

## 7. 장비 인수 시험

| 시험 | 통과 기준 |
|---|---|
| B → C 관리 SSH | 전용 키로 READ_STATUS 성공, 일반 원격 명령·SFTP·터널·PTY는 허용되지 않음 |
| C 관리 SSH / 학생 접속 | 내부 관리 SSH 유지. 학생 진입 포트에서 C 호스트 로그인 불가 |
| cloud-init 최초 부팅 | 계정·해시 로그인·IP·게이트웨이·DNS 실제 적용. sudo 없음 |
| VM 인터넷과 학교망 차단 | 허용 DNS·필요 외부 패키지 서버 접근. 학교 A/B·C 관리 주소와 다른 VM 접근 차단 |
| Proxmox 방화벽 공동 사용 | 프로젝트의 accept가 다른 방화벽의 drop을 우회하지 않음. 정상 NAT 경로는 허용됨 |
| A/B 전환 20회 | DNAT 규칙 1개, map/set 대상 하나, 이전 세션 불통, 새 세션 유지 |
| 지연·재전송 | 같은 commandId 재전송에 새 학생 세션이 끊기지 않음. 옛 세대 요청 거부 |
| 이전 VM 정지 실패 | 다음 START 및 접속 허용 보류 |
| 실제 GPU | 이전 VM stopped·작업 없음 이후 다음 VM에서 nvidia-smi 인식 |
| 최종 반납 | VM·허용 저장소 볼륨/백업·snippet·접속·연결 모두 없음. UNKNOWN이면 완료 불가 |
| 호스트 재부팅 | 활성 대상 자동 복원 없음, 학생 VM 자동 기동 없음, generation 유지 |
| B 전원 또는 통신 중단 | 마지막 성공 갱신의 TTL 이후 신규·기존 접속 트래픽 차단. VM이 자동 종료된 것으로 처리하지 않음 |
| 오래된 작업 | 새 close 이후 옛 open/renew 거부. backend revision과 helper generation은 다른 값으로 시험 |

conntrack 기록 삭제는 TCP FIN을 보내는 동작이 아닙니다. VM 전달 차단과 이후 VM 정지로 이전 학생의 트래픽과 작업을 끝냅니다. 학교 라우터가 SNAT하면 VM에서 학생 원본 IP가 보이지 않을 수 있습니다. 이 구현은 원본 학생 IP에 의존하지 않고 C의 진입 주소·진입 포트·이전 VM의 reply 주소·zone으로 기록을 선별합니다.

## 8. 오류 때 복구

규칙 구조가 바뀌었거나 상태가 불명확하면 신규 접속을 열지 않습니다. B가 현재 상태를 읽고, 같은 generation의 새 명령 ID로 DISABLE_INGRESS를 수행합니다. 실패하면 원인을 조사하고 관리자가 부팅 서비스를 다시 시작해 빈 규칙으로 초기화합니다.

```bash
systemctl restart allday-node-helper-bootstrap.service
```

이 명령은 현재 학생 연결을 닫습니다. 운영 중에는 장애 처리 대상으로 보고 수행합니다. bootstrap은 GPU를 옮기거나 VM을 강제 종료하지 않습니다. 진행 중인 Proxmox 작업과 VM 상태는 B가 별도로 관측해야 합니다. SQLite를 지워 generation을 0으로 되돌리는 방식으로 복구하지 않습니다.
