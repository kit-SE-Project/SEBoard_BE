# Server AllDay

SEBoard의 로그인과 회원 권한으로 Proxmox 컴퓨터를 예약·대여하는 서비스입니다.

| 위치 | 역할 | 실행 환경 |
| --- | --- | --- |
| `src/main/java/com/seproject/allday` | SEBoard의 현재 계정 ID와 역할을 반환하는 내부 인증 API | 기존 SEBoard |
| `server-allday/api` | 장비·카테고리·대여·작업 API 및 Flyway 마이그레이션 | Java 21, Maven, PostgreSQL |
| `server-allday/manager` | Proxmox REST API와 노드 Helper를 사용하는 작업 실행기 | Python 3.12+ 및 OpenSSH 클라이언트 |
| `server-allday/node-helper` | 노드의 Cloud-Init, 학생 SSH 진입과 반납 검증 | 각 Proxmox 호스트 |

## SEBoard 통합 로그인

게시판 메뉴의 서버 대여 링크는 AllDay의 `GET /api/v1/sso/start`로 이동합니다. AllDay가 로그인 요청을 브라우저 쿠키와 연결하고 SE 게시판으로 이동합니다. SE 프론트는 기존 로그인 상태로 코드를 요청합니다. SE 로그인이 만료되었으면 게시판 로그인 화면을 거쳐 요청을 이어갑니다.

- SEBoard는 등록된 AllDay 콜백으로만 일회용 코드와 `state`를 보냅니다. 코드는 최대 60초 동안 유효하며, AllDay가 백엔드에서 공유 비밀과 PKCE S256 검증값으로 교환합니다.
- 브라우저는 AllDay의 호스트 전용 `HttpOnly` 세션 쿠키를 받습니다. AllDay가 요청마다 SEBoard에 현재 계정과 역할을 조회하며, `memberId`는 SEBoard `accounts.account_id`입니다. 관리자 역할 변경과 계정 삭제는 다음 요청부터 반영됩니다.
- AllDay 세션은 발급에 사용한 SEBoard 로그인 토큰의 만료 시각까지 유효합니다. SEBoard에서 로그아웃하면 해당 계정의 기존 대여 세션도 무효화됩니다. AllDay 로그아웃은 현재 브라우저의 대여 세션을 종료합니다.
- `ROLE_ADMIN`은 장비 및 카테고리를 관리할 수 있습니다. 대여 자격은 `SEBOARD_RENTAL_AUTHORITY`에 지정한 DB 역할로 확인하며 기본값은 `ROLE_KUMOH`입니다. 실장비를 지정 계정에만 개방할 때는 `REAL_RENTAL_MEMBER_IDS`를 설정합니다.
- 기존 대여 DB의 개발·데모 회원 ID가 SEBoard 계정 ID와 충돌하면 새 세션을 거부합니다. 기존 대여를 이전할 때는 대여 소유자와 실제 SEBoard 계정 매핑을 먼저 확인하세요.

### 웹 라우팅과 설정

운영에서는 두 웹사이트 모두 HTTPS를 사용합니다. AllDay 웹과 `/api/v1/`은 같은 공개 오리진에서 제공하고, `/api/internal/`은 매니저 전용으로 제공합니다. SE 웹의 `/allday/sso`는 프론트 SPA 경로이고, `POST /allday/sso/authorize`는 기존 SE API 주소를 통해 SE 백엔드로 전달합니다. `POST /internal/allday/sso/exchange`와 `POST /internal/allday/sso/identity`는 백엔드 간 경로이며 공개 프론트에서 라우팅하지 않습니다.

| 설정 위치 | 환경 변수 | 예시 값 |
| --- | --- | --- |
| SE 백엔드 | `ALLDAY_SSO_CALLBACK_URL` | `https://rental.example/api/v1/sso/callback` |
| SE 백엔드, AllDay 백엔드 | `ALLDAY_SSO_CLIENT_SECRET` | 두 서버에 같은 전용 무작위 비밀값 |
| SE 프론트 빌드 | `REACT_APP_ALLDAY_ORIGIN` | `https://rental.example` |
| AllDay 백엔드 | `SEBOARD_SSO_BACKEND_ORIGIN` | `https://seboard-api.internal.example` |
| AllDay 백엔드 | `SEBOARD_SSO_PORTAL_URL` | `https://board.example/allday/sso` |
| AllDay 백엔드 | `ALLDAY_SSO_PUBLIC_ORIGIN`, `CORS_ORIGIN` | `https://rental.example` |
| AllDay 웹 빌드 | `EXPO_PUBLIC_SEBOARD_SSO_ENABLED` | `true` |

SE-FE GitHub 빌드는 `ALLDAY_ORIGIN` 저장소 변수를 읽습니다. AllDay 세션 테이블은 Flyway V17이 생성합니다. SE 백엔드는 기존 PostgreSQL 계정·권한과 Redis 6.2 이상을 사용합니다. 프록시 액세스 로그에서 콜백 쿼리와 인증 헤더를 제외하세요. [OAuth 보안 권고 RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

## 데이터베이스 및 실행

SEBoard PostgreSQL 인스턴스에 AllDay 전용 데이터베이스(예: `server_allday`)를 만듭니다. SEBoard 회원과 역할은 SEBoard 자체 연결에서 읽고, AllDay의 Flyway는 전용 DB에만 적용합니다. 앱 DB 역할에는 전용 DB 접속과 해당 스키마의 DML 및 시퀀스 사용 권한을, 마이그레이션 역할에는 전용 DB와 스키마의 소유 권한을 부여합니다. 새 테이블과 시퀀스에도 권한이 부여되도록 마이그레이션 역할의 기본 권한을 설정합니다.

| 환경 변수 | 운영 값 |
| --- | --- |
| `DB_URL` | AllDay 전용 PostgreSQL JDBC URL |
| `DB_USER`, `DB_PASSWORD` | AllDay 앱 DB 계정 |
| `DB_MIGRATION_USER`, `DB_MIGRATION_PASSWORD` | 별도 Flyway 계정 |
| `PRIMARY_ENABLED` | `true` |
| `CREDENTIAL_KEY` | VM 암호 보관용 무작위 32바이트 키의 Base64 값 |
| `WORKER_TOKEN` | 관리 서버와 공유하는 별도 난수 토큰 |
| `CORS_ORIGIN` | 프론트 오리진. 여러 개일 때 쉼표로 구분 |

`DEV_AUTH_ENABLED`, `DEV_SEED_ENABLED`, `DEMO_AUTH_ENABLED`, `DEMO_SEED_ENABLED`, `DEMO_WORKER_ENABLED`는 모두 기본값인 `false`를 사용합니다. 백엔드 내부 프록시 또는 컨테이너의 네트워크 구성에 맞춰 `API_BIND_ADDRESS`와 `API_PORT`를 지정합니다.

```bash
cd server-allday/api
mvn -B -DskipTests package
java -jar target/server-allday-backend-0.1.0.jar
```

마이그레이션 V15는 카테고리 논리 삭제를 추가합니다. V16 적용 전 이미 동일 회원에게 여러 건이 열려 있다면 기존 계약은 정상 반납할 수 있고 모든 계약이 종료된 뒤 새 대여를 접수합니다.

## GPU 장비 등록

1. 각 노드에 독립적인 Proxmox 노드명, VM 사설망, VMID 구간, SSH 전달 주소·포트와 승인 템플릿을 지정합니다. [노드 Helper 설치](node-helper/docs/INSTALL.md)를 참고하세요.
2. GPU 장비는 호스트에서 IOMMU 및 PCI 패스스루를 구성하고 Proxmox에서 해당 노드의 물리 GPU를 가리키는 PCI Resource Mapping을 만듭니다. 매니저 API 토큰에 해당 매핑의 사용 권한을 부여하고, 게스트 템플릿에 장비에 맞는 드라이버를 준비합니다. [Proxmox VE PCI Passthrough 및 Resource Mapping](https://pve.proxmox.com/pve-docs/pve-admin-guide.pdf)
3. 관리자 API에서 서버를 등록하고 `PATCH /api/v1/admin/servers/{id}/configuration`에 GPU 장비는 `gpuEnabled: true`와 `gpuMappingRef`를, CPU 전용 장비는 `gpuEnabled: false`와 `gpuMappingRef: null`을 전달합니다. 템플릿은 `POST /api/v1/admin/servers/{id}/templates`에서 승인합니다. 대여 작업이나 자원이 점유된 동안 인벤토리 변경은 `409 INVENTORY_IN_USE`입니다.
4. [매니저 설정 예시](manager/config.example.json)의 `server_ids`와 `nodes`를 실제 장비로 채웁니다. 백엔드에 등록한 노드명, GPU 플래그 및 매핑 이름을 그대로 사용합니다. 같은 Proxmox 클러스터의 노드들은 하나의 매니저 설정에 넣을 수 있습니다. 독립 클러스터라면 클러스터별 Proxmox 주소·인증서·API 토큰과 서로 다른 매니저 ID·상태 파일·리스닝 포트를 사용합니다. 매니저 폴더에서 `python -m pip install --require-hashes -r requirements.txt`로 의존성을 설치합니다. 실장비와 격리망 설정을 확인한 뒤 `allow_real_mutations` 및 `network_isolation_confirmed`를 `true`로 설정하고 `python -m manager.server --config <비공개 설정 파일>`을 실행합니다.
5. 매니저가 각 노드를 정상 관측하면 관리자 API에서 장비를 `AVAILABLE`로 설정합니다. GPU 노드에서 준비·접속·정지·재시작·반납을 각각 확인하고 게스트 안에서 GPU 드라이버 인식도 확인합니다. CPU 전용 노드에서도 동일한 대여·반납 흐름을 확인합니다.

각 장비에 물리 GPU 매핑 하나를 할당합니다. VM 시작 시 매핑을 연결하고 슬롯 사이에 정지하면 PCI 장치를 분리합니다. 최종 반납에서는 기존 VM·디스크·진입 규칙 정리 검증을 완료한 뒤 점유를 해제합니다.

[프론트 API 계약](FRONTEND_API.md)
