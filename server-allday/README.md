# SEBoard 통합 로그인

게시판 메뉴의 서버 대여 링크는 AllDay의 `GET /api/v1/sso/start`로 이동합니다. AllDay가 로그인 요청을 브라우저 쿠키와 연결하고 SE 게시판으로 이동합니다. SE 프론트는 기존 로그인 상태로 코드를 요청합니다. SE access token이 만료됐거나 없으면 기존 refresh token으로 먼저 갱신합니다. 갱신 자격이 없으면 게시판 로그인 화면을 거쳐 대여 요청을 이어갑니다.

- SEBoard는 등록된 AllDay 콜백으로만 일회용 코드와 `state`를 보냅니다. 코드는 최대 60초 동안 유효하며, AllDay가 백엔드에서 공유 비밀과 PKCE S256 검증값으로 교환합니다.
- 브라우저는 AllDay의 호스트 전용 `HttpOnly` 세션 쿠키를 받습니다. AllDay의 `memberId`는 SEBoard `accounts.account_id`입니다. AllDay는 유효한 세션의 계정과 역할 조회 결과를 인스턴스별로 최대 30초 캐시합니다.
- 코드를 교환하면 SEBoard grant와 AllDay 세션을 최대 8시간 동안 사용할 수 있습니다. SEBoard는 로그아웃 시 해당 계정의 grant를 즉시 무효화하며, AllDay는 게시판 로그아웃·권한 변경·계정 삭제를 캐시 만료 후 반영합니다. AllDay에서 로그아웃하면 현재 브라우저의 대여 세션을 바로 종료합니다.
- `ROLE_ADMIN`은 장비 및 카테고리를 관리할 수 있습니다. 대여 자격은 `SEBOARD_RENTAL_AUTHORITY`에 지정한 DB 역할로 확인하며 기본값은 `ROLE_KUMOH`입니다. 실장비를 지정 계정에만 개방할 때는 `REAL_RENTAL_MEMBER_IDS`를 설정합니다.
- 기존 대여 DB의 개발·데모 회원 ID가 SEBoard 계정 ID와 충돌하면 새 세션을 거부합니다. 기존 대여를 이전할 때는 대여 소유자와 실제 SEBoard 계정 매핑을 먼저 확인하세요.

### 웹 라우팅과 설정

운영에서는 두 웹사이트 모두 HTTPS를 사용합니다. AllDay 웹과 `/api/v1/`은 같은 공개 오리진에서 제공하고, `/api/internal/`은 매니저 전용으로 제공합니다. SE 웹의 `/allday/sso`는 프론트 SPA 경로이고, `POST /allday/sso/authorize`는 기존 SE API 주소를 통해 SE 백엔드로 전달합니다. `POST /internal/allday/sso/exchange`와 `POST /internal/allday/sso/identity`는 백엔드 간 경로이며 공개 프론트에서 라우팅하지 않습니다.

| 설정 위치 | 환경 변수 | 예시 값 |
| --- | --- | --- |
| SE 백엔드 | `ALLDAY_SSO_CALLBACK_URL` | `https://rental.example/api/v1/sso/callback` |
| SE 백엔드, AllDay 백엔드 | `ALLDAY_SSO_CLIENT_SECRET` | 두 서버에 같은 32바이트 이상의 전용 무작위 비밀값 |
| SE 프론트 빌드 | `REACT_APP_ALLDAY_ORIGIN` | `https://rental.example` |
| AllDay 백엔드 | `SEBOARD_SSO_BACKEND_ORIGIN` | `https://seboard-api.internal.example` |
| AllDay 백엔드 | `SEBOARD_SSO_PORTAL_URL` | `https://board.example/allday/sso` |
| AllDay 백엔드 | `ALLDAY_SSO_PUBLIC_ORIGIN`, `CORS_ORIGIN` | `https://rental.example` |
| AllDay 웹 빌드 | `EXPO_PUBLIC_SEBOARD_SSO_ENABLED` | `true` |

SE-FE GitHub 빌드는 `ALLDAY_ORIGIN` 저장소 변수를 읽습니다. AllDay 세션 테이블은 Flyway V17이 생성합니다. SE 백엔드는 기존 PostgreSQL 계정·권한과 Redis 6.2 이상을 사용합니다. 권한 캐시가 만료된 뒤 SEBoard 조회가 실패하면 AllDay API는 `503 SEBOARD_AUTH_UNAVAILABLE`을 반환합니다. 프록시 액세스 로그에서 콜백 쿼리와 인증 헤더를 제외하세요. [OAuth 보안 권고 RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

기존 30분 버전을 배포한 환경에서는 AllDay 백엔드를 먼저 올린 다음 SE 백엔드와 SE 프론트를 적용합니다. 기존 SEBoard grant는 새 로그아웃 세대 키가 없으므로 최초 전환 시 통합 로그인을 다시 진행합니다.

## Server AllDay 저장소

대여 API, Flyway, Proxmox 매니저와 노드 Helper는 `serverAllDay_BE`에서 관리합니다. [Server AllDay 배포 및 GPU 등록](https://github.com/serverallday/serverAllDay_BE/blob/codex/server-allday-integration/server-allday-backend/docs/SEBOARD_SSO.md)

[프론트 API 명세](FRONTEND_API.md)
