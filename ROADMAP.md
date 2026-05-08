# SEBoard 개발 로드맵

> 마지막 업데이트: 2026-04-30

---

## 완료된 기능

### 서비스 기능
- [x] 티어 시스템 (브론즈 ~ 다이아몬드, 활동 점수 기반)
- [x] 프레임 시스템 (티어 달성 보상, 프로필 아바타 테두리)
- [x] 역할 뱃지 시스템 (CHECK / KUMOH_CROW, 우선순위 기반)
- [x] 인기글(Trending) 기능 (카테고리별 활성화, 게시판/메인페이지 표시)
- [x] 게시글/댓글 좋아요·싫어요
- [x] 프로필 페이지 개선 (프레임 보관함, 아바타 업로드/삭제)
- [x] 댓글 이미지 첨부

### 인프라
- [x] K3s 멀티노드 클러스터 구성 (se1 마스터, se2/se4 워커)
- [x] MetalLB LoadBalancer (LAN IP 풀 192.158.0.201~220)
- [x] Nginx Ingress Controller
- [x] Longhorn 분산 스토리지
- [x] Cloudflare Tunnel (testse.store, HTTPS 자동 처리)
- [x] GitHub Actions CI/CD (self-hosted runner, main push → 자동 배포)
- [x] Prometheus + Grafana 모니터링
- [x] pg_dump 백업 CronJob (매일 새벽 3시, Longhorn PVC 보관)
- [x] DB 마이그레이션 (구 서버 → K3s PostgreSQL)
- [x] 파일 마이그레이션 (구 서버 → Longhorn PVC)

---

## 단기 계획 (1~2개월)

### 1. 인프라 마무리

#### 도메인 전환
- [ ] 현재 `testse.store` → 최종 도메인으로 전환
- [ ] `se.kumoh.ac.kr` 연결 방안 확정 (학교 DNS 변경 요청 또는 리버스 프록시)
- [ ] `seboard.site` 구 서버 종료

#### Nginx Rate Limiting
- [ ] Ingress annotation으로 크롤러/과도한 요청 차단
- [ ] 예상 공수: 반나절

#### UptimeRobot 모니터링
- [ ] 무료 플랜으로 서버 다운 감지 + 디스코드/이메일 알림
- [ ] 예상 공수: 1시간

### 2. 알림 기능 (SSE + FCM)

#### 백엔드
- [ ] Outbox 패턴으로 알림 유실 방지 (외부 MQ 없이 DB만 사용)
  - 댓글 작성 / 새 게시글 → 같은 트랜잭션에 Outbox 이벤트 저장
  - 스케줄러가 주기적으로 미처리 이벤트 처리 + 재시도
  - Dead Letter 처리 (재시도 3회 초과 시 실패 처리)
- [ ] SSE (Server-Sent Events) 로 실시간 알림 전달
- [ ] FCM 연동으로 백그라운드/PWA 푸시 알림
- [ ] 알림 구독 설정 (카테고리별, 내 글 댓글 등)

#### 프론트엔드
- [ ] 종 아이콘 + 읽음/안읽음 알림 목록 UI
- [ ] PWA Service Worker + FCM 푸시 수신
- [ ] 알림 클릭 시 해당 게시글로 이동

### 3. 다크모드 개선
- [ ] 위지윅 에디터 본문 영역 라이트 배경 유지 (글자색 가독성)
- [ ] 신규 컴포넌트부터 Chakra UI 시맨틱 토큰 적용 시작
- [ ] 기존 컴포넌트는 점진적으로 마이그레이션

---

## 중기 계획 (3~6개월)

### 1. 서비스 분리

#### Spring Authorization Server 분리
- [ ] 현재 커스텀 JWT → OIDC 표준 플로우로 전환
- [ ] Authorization Code Flow + PKCE 적용
- [ ] 프론트엔드 인증 플로우 변경
- [ ] 외부 서비스가 우리 인증 서버로 로그인할 수 있는 기반 마련

#### 알림 서버 분리
- [ ] `@EventListener + @Async` 로 커뮤니티 서버에서 이벤트 발행
- [ ] Outbox 패턴으로 유실 방지
- [ ] 알림 서버가 독립적으로 SSE / FCM 처리
- [ ] K3s에 별도 Deployment로 배포 (내부 DNS로 통신)
- [ ] 단일 PostgreSQL 공유 (스키마만 논리적 분리: `notification.*`)

### 2. 인프라 고도화

#### K8s HPA (Horizontal Pod Autoscaler)
- [ ] 트래픽 급증 시 자동 스케일아웃

#### GPU 노드 활용 (se2 - RTX 3090)
- [ ] NVIDIA Device Plugin 설치
- [ ] AI/ML 워크로드 스케줄링 기반 마련

---

## 장기 계획 (6개월~)

### 1. Developer 페이지

> 컴공과 특성상 임팩트 있을 것으로 기대
> 현재 크롤링 서드파티 → 공식 API로 흡수

- [ ] 알림 API (새 게시글 웹훅, 댓글 웹훅)
- [ ] OAuth 로그인 API (외부 서비스가 SE 계정으로 로그인)
- [ ] 앱 등록 / API 키 발급·갱신·폐기
- [ ] 사용량 모니터링 대시보드
- [ ] Rate Limiting (API 키별 요청 제한)
- [ ] Webhook 설정 UI

### 2. 인프라 고도화

- [ ] CloudNativePG (PostgreSQL K8s 완전 편입, 자동 백업/페일오버)
- [ ] 마스터 HA 구성 (etcd 쿼럼을 위한 마스터 3대)

---

## 기술 스택 현황

| 영역 | 현재 | 목표 |
|------|------|------|
| 배포 | K3s 멀티노드 ✅ | HPA 적용 |
| 도메인 | testse.store (Cloudflare Tunnel) | se.kumoh.ac.kr |
| 인증 | 커스텀 JWT | Spring Authorization Server (OIDC) |
| 알림 | 없음 | SSE + FCM + Outbox 패턴 |
| 모니터링 | Prometheus + Grafana ✅ | 알림 임계치 설정 |
| DDoS 방어 | Cloudflare ✅ | + Nginx Rate Limiting |
| DB 백업 | pg_dump 크론잡 ✅ | CloudNativePG |
| 외부 API | 없음 | Developer 페이지 + API 키 |

---

## 우선순위 요약

```
지금 당장
├── 도메인 전환 (testse.store → 최종 도메인)
├── UptimeRobot 모니터링
└── Nginx Rate Limiting

단기 (1~2개월)
├── 알림 기능 (SSE + FCM)
└── 다크모드 개선

중기 (3~6개월)
├── Spring Authorization Server 분리
├── 알림 서버 분리
└── GPU 노드 활용

장기 (6개월~)
├── Developer 페이지
└── 인프라 고도화 (CloudNativePG, HA)
```
