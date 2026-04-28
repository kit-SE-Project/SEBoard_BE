# SEBoard 개발 로드맵

> 마지막 업데이트: 2026-04-28

---

## 완료된 기능

- [x] 티어 시스템 (브론즈 ~ 다이아몬드, 활동 점수 기반)
- [x] 프레임 시스템 (티어 달성 보상, 프로필 아바타 테두리)
- [x] 역할 뱃지 시스템 (CHECK / KUMOH_CROW, 우선순위 기반)
- [x] 인기글(Trending) 기능 (카테고리별 활성화, 게시판/메인페이지 표시)
- [x] 게시글/댓글 좋아요·싫어요
- [x] 프로필 페이지 개선 (프레임 보관함, 아바타 업로드/삭제)
- [x] 댓글 이미지 첨부

---

## 단기 계획 (1~2개월)

### 1. 인프라 안정화 ⚠️ 최우선

#### DB 백업
- `pg_dump` 크론잡 + 외부 스토리지(NAS or 외장HDD) 저장
- 하루 1회, 최소 7일치 보관
- 예상 공수: 반나절

#### 기본 모니터링
- UptimeRobot (무료) 으로 서버 다운 감지 + 이메일/디스코드 알림
- 예상 공수: 1시간

#### Cloudflare Tunnel 도입
- 공인 IP 1개 문제 해결
- DDoS 방어 + HTTPS 자동 처리
- 포트포워딩 없이 외부 접근 가능
- 예상 공수: 반나절

#### Nginx Rate Limiting
- 크롤러/과도한 요청 차단
- 예상 공수: 반나절

### 2. 알림 기능 (SSE + FCM)

#### 백엔드
- Outbox 패턴으로 알림 유실 방지 (외부 MQ 없이 DB만 사용)
  - 댓글 작성 / 새 게시글 → 같은 트랜잭션에 Outbox 이벤트 저장
  - 스케줄러가 주기적으로 미처리 이벤트 처리 + 재시도
  - Dead Letter 처리 (재시도 3회 초과 시 실패 처리)
- SSE (Server-Sent Events) 로 실시간 알림 전달
- FCM 연동으로 백그라운드/PWA 푸시 알림
- 알림 구독 설정 (카테고리별, 내 글 댓글 등)

#### 프론트엔드
- 종 아이콘 + 읽음/안읽음 알림 목록 UI
- PWA Service Worker + FCM 푸시 수신
- 알림 클릭 시 해당 게시글로 이동

### 3. 다크모드 개선
- 위지윅 에디터 본문 영역 라이트 배경 유지 (글자색 가독성)
- 신규 컴포넌트부터 Chakra UI 시맨틱 토큰 적용 시작
- 기존 컴포넌트는 점진적으로 마이그레이션

---

## 중기 계획 (3~6개월)

### 1. 인프라 멀티노드 전환

#### Docker Compose 도입 (K3s 전환 전 단계)
- 현재 서비스 컨테이너화
- `docker-compose.yml` 로 전체 스택 관리
- 배포 자동화 (GitHub Actions → 서버 docker-compose pull & up)

#### K3s 멀티노드 구성
```
[PC1 - 마스터]          [PC2 - 워커]           [PC3 - 워커]
├── K3s 컨트롤 플레인   ├── 커뮤니티 서버      ├── PostgreSQL
├── Nginx Ingress       ├── 인증 서버          └── Prometheus
└── Cloudflare Tunnel   └── 알림 서버              + Grafana
```
- PostgreSQL은 초기에 K8s 밖에서 직접 설치 (안정성 우선)
- 마스터 단일 노드 (HA는 장기로 미룸)

#### Prometheus + Grafana 모니터링
- 서버 메트릭 (CPU, RAM, 디스크)
- Spring Boot Actuator 연동 (API 응답시간, 에러율)
- 알림 설정 (임계치 초과 시 디스코드/이메일)

### 2. 서비스 분리

#### Spring Authorization Server 분리
- 현재 커스텀 JWT → OIDC 표준 플로우로 전환
- Authorization Code Flow + PKCE 적용
- 프론트엔드 인증 플로우 변경
- 외부 서비스가 우리 인증 서버로 로그인할 수 있는 기반 마련

#### 알림 서버 분리
- `@EventListener + @Async` 로 커뮤니티 서버에서 이벤트 발행
- Outbox 패턴으로 유실 방지
- 알림 서버가 독립적으로 SSE / FCM 처리
- 단일 PostgreSQL 공유 (스키마만 논리적 분리: `notification.*`)

---

## 장기 계획 (6개월~)

### 1. Developer 페이지

> 컴공과 특성상 임팩트 있을 것으로 기대
> 현재 크롤링 서드파티 → 공식 API로 흡수

#### 외부 API 제공
- 알림 API (새 게시글 웹훅, 댓글 웹훅)
  - 현재 디스코드 봇 등 크롤링 기반 서드파티를 공식 API로 대체
  - 사용량 추적 가능해짐
- OAuth 로그인 API (외부 서비스가 SE 계정으로 로그인)

#### Developer 페이지 구성
- 앱 등록 / API 키 발급·갱신·폐기
- 사용량 모니터링 대시보드
- API 문서 (Swagger 기반)
- Rate Limiting (API 키별 요청 제한)
- Webhook 설정 UI

### 2. 인프라 고도화

#### CloudNativePG
- PostgreSQL을 K8s 안으로 편입
- 자동 백업, 페일오버 구성

#### 마스터 HA 구성
- PC가 충분히 확보되면 etcd 쿼럼을 위한 마스터 3대 구성

#### K8s HPA (Horizontal Pod Autoscaler)
- 트래픽 급증 시 자동 스케일아웃

---

## 기술 스택 목표

| 영역 | 현재 | 목표 |
|------|------|------|
| 배포 | 직접 jar 실행 | K3s (멀티노드) |
| 인증 | 커스텀 JWT | Spring Authorization Server (OIDC) |
| 알림 | 없음 | SSE + FCM + Outbox 패턴 |
| 모니터링 | 없음 | Prometheus + Grafana |
| 외부 접근 | 공인 IP 직접 | Cloudflare Tunnel |
| DDoS 방어 | 없음 | Cloudflare + Nginx Rate Limiting |
| DB 백업 | 없음 | pg_dump 크론잡 |
| 외부 API | 없음 | Developer 페이지 + API 키 |

---

## 우선순위 요약

```
지금 당장 (이번 주)
├── DB 백업 크론잡
├── UptimeRobot 모니터링
└── Cloudflare Tunnel

단기 (1~2개월)
├── 알림 기능 (SSE + FCM)
├── Nginx Rate Limiting
└── Docker Compose 도입

중기 (3~6개월)
├── K3s 멀티노드 전환
├── Prometheus + Grafana
├── Spring Authorization Server 분리
└── 알림 서버 분리

장기 (6개월~)
├── Developer 페이지
├── 외부 API 제공
└── 인프라 고도화 (CloudNativePG, HA)
```
