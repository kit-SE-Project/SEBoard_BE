# 관리 Agent와 Node Helper 계약 v1 / 구현 v0.2.0

이 문서는 이번 구현이 제공하는 계약입니다. 기존 Agent에 아직 연결된 계약이라고 가정하지 않습니다. Proxmox REST API의 요청 형식과는 다른, 팀 자체 JSON 프로토콜입니다.

## 전송

관리 Agent는 Proxmox 호스트의 **내부 관리 IP:22**로 SSH 연결합니다. `allday-helper` 전용 계정과 전용 키를 사용합니다. 학생의 외부 접속 포트 7540과 별개입니다.

```bash
ssh -T -p 22 -i /etc/server-allday/keys/node-helper \
  -o BatchMode=yes -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile=/etc/server-allday/keys/known_hosts \
  -o ConnectTimeout=5 \
  allday-helper@NODE_MANAGEMENT_IP < request.json
```

원격 명령 문자열은 필요 없습니다. SSH 키의 고정 실행 설정이 설치된 Helper를 실행합니다. B 서버는 입력 스트림에 JSON을 쓰고 닫아야 합니다. Java에서는 `ProcessBuilder`의 인자 배열로 위 SSH 프로그램을 실행하고 Jackson `ObjectMapper`로 stdin/stdout JSON을 직렬화·역직렬화하면 됩니다. 동시 요청·시간 제한·stderr 읽기·프로세스 종료는 Agent의 책임입니다. stdin 내용을 로그에 남기지 않습니다.

## 공통 입력과 출력

```json
{
  "schemaVersion": 1,
  "commandId": "r1042-apply-38",
  "serverId": 1,
  "operation": "APPLY_INGRESS",
  "generation": 38,
  "payload": {"rentalId": 1042}
}
```

- `commandId`: 작업 단계마다 고유한 ID. 전송 결과를 잃은 **같은 요청**은 같은 ID로 재전송합니다. 입력이나 세대가 바뀌면 새 ID입니다.
- `serverId`: 호스트 설정과 같아야 합니다. 다른 서버 요청은 거부합니다.
- `generation`: 현재 호스트 세대입니다. 모든 변경 명령에서 필수이며 조회 명령에서는 선택입니다.
- `controlRevision`: 선택적인 서버별 단조 증가 작업 의도 번호입니다. 처음 이 필드를 보낸 후에는 해당 호스트의 모든 변경 요청에서 필수가 되며 재부팅 후에도 유지합니다. 백엔드 작업 번호를 generation 자리에 넣지 않습니다. `WINDOWS_INTEGRATION.md`를 참고합니다.
- `payload`: 명령별 허용 필드만 받습니다. 최대 입력은 64 KiB입니다. 중복 JSON 키·추가 필드·임의 명령은 거부합니다.

```json
{
  "schemaVersion": 1,
  "commandId": "r1042-apply-38",
  "serverId": 1,
  "operation": "APPLY_INGRESS",
  "mode": "system",
  "ok": true,
  "code": "OK",
  "replayed": false,
  "result": {
    "enabled": true,
    "rentalId": 1042,
    "generation": 38,
    "sshReady": true,
    "ingress": {"target": "10.70.1.120", "ingressRuleCount": 1, "mappingCount": 1, "guardCount": 1, "ruleCount": 25}
  }
}
```

`ruleCount`는 호스트 DNS·차단 대역 설정에 따라 바뀌므로 예시 숫자를 하드코딩하지 않습니다. Agent는 `mode=simulation` 결과를 실장비 성공으로 저장하지 않습니다. `sshReady`는 SSH 프로토콜 응답을 확인한 것이며 학생 암호 로그인, GPU 또는 Jupyter 검사 성공을 뜻하지 않습니다.

실패 출력은 `ok=false`, `code`, `message`, 필요한 최소한의 `details`를 포함합니다. `VERIFY_CLEANUP`의 `ok=true`는 검사가 실행됐다는 뜻이며 반납 성공은 **`result.result=PASS`**까지 확인해야 합니다.

## 명령별 payload

| operation | payload | 선행 조건 / 결과 |
|---|---|---|
| REGISTER_RESOURCE | rentalId, vmId, vmIp, volumeIds[] | VMID는 설정 범위, IP는 해당 노드 사설망. 점유 중 중복 불가 |
| PUT_SNIPPET | rentalId, username, passwordHash, hostname | VM 없음 또는 stopped, 진행 작업 없음, 접속 닫힘. 설정 파일 3개 참조 반환 |
| REMOVE_SNIPPET | rentalId | VM 삭제 확인 후 설정 파일 삭제 |
| ADVANCE_GENERATION | rentalId, expectedGeneration | envelope의 generation=expectedGeneration+1. 접속 닫힘, 연결 정리 완료, 모든 학생 VM stopped, 진행 작업 없음 |
| APPLY_INGRESS | rentalId | 현재 소유자·세대, 해당 VM만 running, 설정 파일 일치, SSH 응답 |
| RENEW_INGRESS | rentalId, 선택 leaseSeconds | 이미 열린 유효 접속만 갱신. 닫힘·만료 접속은 거부 |
| DISABLE_INGRESS | 빈 객체 | 현 세대에서 활성 VM 접속 닫기·이전 연결 삭제 |
| READ_STATUS | 빈 객체 | 현재 실제 상태, 세대·소유자·복구 필요 여부 |
| VERIFY_CLEANUP | rentalId | 항목별 PASS/FAIL/UNKNOWN. 전체 결과도 반환 |
| RELEASE_RESOURCE | rentalId | 전체 검사 PASS 후 로컬 점유 해제 |

`REGISTER_RESOURCE.volumeIds`에는 생성 직전 빈 목록을 줄 수 있습니다. 복제 완료 후 같은 대여·VMID·IP로 실제 디스크 목록을 **추가**합니다. 이미 기록된 디스크를 목록에서 지우거나 VMID/IP를 바꾸는 것은 거부합니다. 점유 해제 후 같은 rentalId를 새 계약으로 사용하지 않습니다.

`APPLY_INGRESS`도 `leaseSeconds`를 선택적으로 받습니다. 기본 60초, 정수 5~120초이며 무제한 허용은 없습니다. 중복 APPLY는 기존 타이머를 연장하지 않습니다. 주기적 갱신은 반드시 새 commandId의 RENEW_INGRESS로 수행합니다. 동일 commandId 재전송은 당시 응답만 반환하고 타이머를 다시 갱신하지 않습니다.

READ_STATUS 결과에는 `controlRevision`, `accessEnabled`가 추가됩니다. ingress의 `leaseValid`, `leaseRemainingSeconds`는 읽은 시점의 커널 관측값입니다. nft 출력의 초 단위 반올림 때문에 1초 미만 잔여 시간은 보수적으로 만료로 볼 수 있습니다. `target`은 DNAT 매핑 대상이므로 값이 남았다는 이유만으로 접속 허용으로 판단하지 않습니다. 만료 시 target이 남아도 accessEnabled=false입니다. 기존 예시 응답의 ingress 필드는 이 세 필드를 추가해서 읽습니다.

`passwordHash`는 `$6$...` 형식의 SHA-512 crypt 값입니다. BCrypt 회원 암호와 다릅니다. 13자리 실제 비밀번호는 서비스가 생성해 암호화 저장하며, Ubuntu용 해시는 관리 서버의 검증된 암호 라이브러리로 만듭니다. Helper는 실제 비밀번호를 입력받거나 발급하지 않습니다. 기존 VM 암호 재설정은 관리 Agent의 QEMU Guest Agent 처리 경계입니다.

## 최초 준비부터 접속 허용

1. Agent가 `READ_STATUS`로 현 generation을 읽습니다.
2. 서비스가 지정한 서버에서 VMID/IP를 점유하고, Helper에 `REGISTER_RESOURCE`를 보냅니다.
3. Agent가 `PUT_SNIPPET`을 호출해 파일 참조를 받습니다.
4. Agent가 Proxmox API `POST /api2/json/nodes/{node}/qemu/{templateVmid}/clone`에 new VMID를 전달합니다. 반환 UPID 완료를 확인합니다.
5. 복제 VM 설정을 조회해 실제 디스크 목록을 `REGISTER_RESOURCE`로 추가합니다.
6. `PUT_SNIPPET`의 참조를 아래와 같이 `PUT /api2/json/nodes/{node}/qemu/{vmid}/config`의 `cicustom` 값으로 연결합니다. `onboot=0`, VM 브리지·MAC·GPU는 Agent의 검증된 인벤토리 값으로 설정합니다.

```text
user=local:snippets/allday-r1042-user.yaml,network=local:snippets/allday-r1042-network.yaml,meta=local:snippets/allday-r1042-meta.yaml
```

7. 다른 이용자가 있다면 `DISABLE_INGRESS` → Proxmox shutdown → stopped·UPID 확인을 먼저 수행합니다.
8. Agent가 서비스 DB의 GPU Lease와 협의한 다음 세대로 `ADVANCE_GENERATION`을 보냅니다. Helper 응답을 확인한 뒤에만 다음 VM을 시작합니다. DB와 Helper는 하나의 트랜잭션이 아니므로 실패 시 양쪽 실제 값을 관측해 복구합니다.
9. Proxmox start UPID와 VM running을 확인하고 GPU·Jupyter·실제 로그인 검사를 수행합니다.
10. Helper의 `APPLY_INGRESS` 성공 후에만 서비스가 외부 접속 정보를 이용 가능으로 표시합니다.
11. 예약과 작업 의도가 여전히 유효할 때만 주기적으로 `RENEW_INGRESS`를 보냅니다. 갱신이 멈추면 커널이 VM 전달 트래픽을 차단합니다. 만료 후에는 상태 관측·DISABLE_INGRESS·현재 예약 확인을 거쳐 새 APPLY를 수행합니다.

Ubuntu의 cloud-init은 최초 부팅 때 파일 내용을 읽습니다. Helper가 파일을 저장했다는 응답만으로 OS 적용 완료로 판단하지 않습니다. 생성한 파일은 JSON 문법으로 직렬화된 YAML이며 user-data에는 `#cloud-config` 헤더가 있습니다. Ubuntu Netplan용 network v2를 사용하며 템플릿의 실제 NIC 이름을 설정과 맞춰야 합니다.

## 시간대 종료와 최종 반납

시간대 하나 종료: `DISABLE_INGRESS` → Proxmox shutdown 완료 확인. **같은 대여 계약의** 다음 시간대를 위해 VM·디스크·설정 파일을 유지합니다. 다음 계약에서 기존 VM을 재사용하는 기능은 없습니다.

최종 반납: `DISABLE_INGRESS` → Proxmox shutdown·정지 확인 → VM/해당 디스크 삭제 UPID 확인 → `REMOVE_SNIPPET` → `VERIFY_CLEANUP` → `RELEASE_RESOURCE`. PASS 이후 서비스 DB에서 IP/VMID 점유를 해제하고 RETURNED로 변경합니다. 반납 후 별도 보존 기간은 없습니다. Helper는 디스크를 직접 삭제하지 않습니다.

## 오류와 복구

| code | Agent 동작 |
|---|---|
| STALE_GENERATION / NOT_LEASE_OWNER | 실제 세대·DB Lease 재조회. 옛 작업을 새 세대로 자동 재포장하지 않음 |
| STALE_CONTROL_REVISION / CONTROL_REVISION_REQUIRED | 오래된 작업 폐기. 현재 작업의 번호를 명시. 옛 작업 번호를 최신 값으로 재작성하지 않음 |
| LEASE_EXPIRED / LEASE_NOT_ACTIVE | 자동 재개 금지. 현 예약 재확인 후 DISABLE_INGRESS로 정리하고 새 APPLY 여부 결정 |
| COMMAND_ID_CONFLICT | 요청 ID 생성 오류 수정. 같은 ID의 다른 입력 금지 |
| OPERATION_UNCERTAIN | READ_STATUS·실제 파일/VM 상태 확인. 새 ID로 안전한 차단/복구 처리 |
| TASK_IN_PROGRESS / VM_NOT_STOPPED | 기존 UPID·VM 관측. 다음 START 보류 |
| VM_NOT_EXCLUSIVE / SSH_NOT_READY | 학생 접속 보류. VM·부팅·SSH 원인 조사 |
| RECOVERY_REQUIRED | DISABLE_INGRESS로 차단·정리 확인. 여전히 불명확하면 관리자 bootstrap·조사 |
| RULES_DRIFT / BOOTSTRAP_REQUIRED | 접속 보류. 관리자 초기화와 실제 부팅 상태 확인 |
| CLEANUP_INCOMPLETE | FAIL/UNKNOWN 항목 확인. IP/VMID 반환·반납 완료 보류 |
| HOST_TIMEOUT / HOST_COMMAND_FAILED | 바로 같은 시스템 명령 반복하지 않고 상태 관측 |

실패 결과도 receipt에 저장합니다. 상태를 바꿔 새로 시도할 때는 새 commandId를 사용합니다. 기존 성공 receipt를 재수신해도 그때의 대상이 현재 활성이라는 뜻은 아니며 READ_STATUS를 확인해야 합니다.

## 근거 문서

- [OpenSSH 고정 명령·키 제한](https://man.openbsd.org/sshd.8)
- [nftables 원자 적용](https://wiki.nftables.org/wiki-nftables/index.php/Atomic_rule_replacement)
- [nftables JSON 형식](https://manpages.debian.org/trixie/libnftables1/libnftables-json.5.en.html)
- [nftables timeout set](https://wiki.nftables.org/wiki-nftables/index.php/Sets)
- [conntrack 필터와 XML 출력](https://netfilter.org/projects/conntrack-tools/conntrack-manpage.html)
- [cloud-init network v2](https://docs.cloud-init.io/en/latest/reference/network-config-format-v2.html)
