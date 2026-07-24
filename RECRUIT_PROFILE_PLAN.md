# 구인구직 & 프로필 개편 실행 계획

---

## 개요

- **구인구직 기능**: 팀 모집, 채용 공고, 팀 참여 희망, 취업·인턴 희망 게시글
- **프로필 페이지 개편**: 설정 페이지 → 자기소개 페이지로 재설계
- **개발자 프로필**: 기술스택, GitHub, 포트폴리오 등 개발자 정보

---

## Phase 1. 백엔드 - 기반 엔티티

### 1-1. 기술 스택 (SkillTag)

```
skill_tags
├── id
├── name         (Java, React, Spring 등)
├── category     (FRONTEND, BACKEND, DB, INFRA, OTHER)
└── isActive     (비활성화 시 신규 게시글에서 미노출, 기존 데이터 유지)
```

- [ ] `SkillTag` 엔티티, Repository, Service
- [ ] CRUD API (`/admin/skills`)

---

### 1-2. 개발자 프로필 (DeveloperProfile)

```
developer_profiles
├── id
├── account_id       (FK, 1:1)
├── intro            (한 줄 소개)
├── bio              (상세 소개, TEXT)
├── github_url
├── portfolio_url
├── grade            (1~4, 대학원, 졸업)
└── status           (SEEKING, OPEN, INACTIVE)

developer_profile_skills (중간 테이블)
├── profile_id
└── skill_tag_id
```

- [ ] `DeveloperProfile`, `DeveloperProfileSkill` 엔티티
- [ ] CRUD API (`/profile/developer`)
- [ ] SeekPost 작성 시 스킬 자동 채움 로직

---

### 1-3. 구인구직 게시글 (RecruitPost)

```
recruit_posts (상위 공통)
├── id
├── type             (RECRUIT | SEEK)
├── tag              (TEAM_RECRUIT | JOB_POSTING | TEAM_JOIN | JOB_SEEK)
├── title
├── contents         (위지윅 TEXT)
├── status           (ACTIVE | CLOSED)
├── account_id
├── expose_option_id (기존 ExposeOption 재활용)
├── created_at, modified_at
│
├── [RECRUIT 전용]
│   ├── headcount    (모집 인원)
│   ├── start_date
│   └── end_date
│
└── [SEEK 전용]
    └── portfolio_url

recruit_post_skills (중간 테이블)
├── recruit_post_id
└── skill_tag_id
```

**첨부파일**: 기존 `FileMetaData` 폴리모픽 활용 (`attachable_type = RECRUIT_POST`)

**댓글**: `RecruitComment` 별도 엔티티 (기존 Comment는 post_id에 묶여있어 분리)

```
recruit_comments
├── id
├── recruit_post_id
├── account_id
├── contents (TEXT)
├── is_read_only_author
├── created_at, modified_at
└── parent_comment_id (대댓글용)
```

- [ ] `RecruitPost` 엔티티 (SINGLE_TABLE 전략)
- [ ] `RecruitPostSkill` 중간 테이블
- [ ] `RecruitComment` 엔티티
- [ ] CRUD API (`/recruit`)
- [ ] 댓글 API (`/recruit/{id}/comments`)
- [ ] 마감 처리 API (`PATCH /recruit/{id}/close`)

---

### 1-4. 구인구직 메뉴 (RecruitMenu)

```java
@DiscriminatorValue("RECRUIT")
public class RecruitMenu extends InternalSiteMenu {
    // urlInfo = "recruit" 고정
    // 기존 MenuAuthorization 시스템 그대로 적용
}
```

- [ ] `RecruitMenu` 엔티티 추가
- [ ] 기존 Admin 메뉴 관리에서 생성 가능하도록 처리

---

### 1-5. 핀 게시글 (PinnedPost)

```
pinned_posts
├── id
├── account_id
├── post_id
└── order_index
```

- [ ] `PinnedPost` 엔티티
- [ ] 핀 추가/삭제/순서 변경 API (`/profile/pins`)

---

### 1-6. 알림 연동

- [ ] `NotificationType`에 `TEAM_RECRUIT` 추가
- [ ] RecruitPost 작성 시: 매칭 스킬 가진 유저 조회 → Redis Streams 이벤트 발행
  ```
  recruit_post.skills ∩ developer_profile.skills ≥ 1 → 알림
  ```
- [ ] 알림 서버 `NotificationEventListener`에서 처리

---

### 1-7. 메인 페이지 구인구직 설정

```
recruit_main_page_config
├── id
├── is_visible       (표시 여부)
├── display_count    (표시 개수)
└── sort_type        (LATEST | DEADLINE)
```

- [ ] `RecruitMainPageConfig` 엔티티 (단일 row)
- [ ] 설정 조회/수정 API (`/admin/recruit/main-config`)
- [ ] 메인 페이지 API에 구인구직 카드 데이터 포함

---

## Phase 2. 관리자 페이지

- [ ] **기술 스택 관리**: 목록/추가/수정/활성화 토글 UI
- [ ] **구인구직 게시글 관리**: 목록, 강제 마감, 강제 삭제
- [ ] **메인 페이지 구인구직 설정**: 표시 여부, 개수, 정렬 기준

---

## Phase 3. 프론트엔드 - 구인구직

### 3-1. 구인구직 목록 페이지 (`/recruit`)

- [ ] 탭: 전체 / 모집 / 구직
- [ ] 스킬 필터 (predefined 태그 선택)
- [ ] 상태 필터: 진행중 / 마감 포함
- [ ] 카드 UI (태그 배지, 스킬 태그, 작성자, 기간)

### 3-2. 구인구직 상세 페이지 (`/recruit/:id`)

- [ ] 공개 범위 처리
- [ ] 본문 위지윅 렌더링
- [ ] 스킬 태그 표시
- [ ] 댓글/대댓글
- [ ] 첨부파일
- [ ] 작성자 프로필 카드 (DeveloperProfile)

### 3-3. 구인구직 작성/수정 폼

- [ ] 타입 선택 (모집/구직)
- [ ] 태그 선택 (타입에 따라 옵션 변경)
- [ ] 스킬 태그 선택 (predefined 목록)
- [ ] SEEK: DeveloperProfile 스킬 자동 채움
- [ ] 기간 선택 (RECRUIT만)
- [ ] 위지윅 에디터
- [ ] 첨부파일
- [ ] 공개 범위

### 3-4. 메인 페이지 구인구직 섹션

- [ ] 가로 스크롤 카드 섹션
- [ ] 모집/구직 혼합, 최신 ACTIVE 포스트
- [ ] 우측 "더보기 →" 링크
- [ ] 모집(주황)/구직(파랑) 컬러 구분

---

## Phase 4. 프론트엔드 - 프로필 개편

### 4-1. 프로필 페이지 (`/profile/:userId`) 재설계

**데스크탑 (2컬럼)**
```
[좌측 사이드바 ~280px, sticky]     [우측 메인 flex-1]
─────────────────────────────────────────────────────
아바타 (120px)                      [게시글][댓글][구인구직] 탭
이름 + 뱃지 + 구직상태 배지
한 줄 소개                          핀 게시글 (아코디언)
스킬 태그들
GitHub / Portfolio 링크             탭 컨텐츠
활동점수 / 티어
─────────────
게시글 N / 댓글 N / 북마크 N
─────────────
구인구직 활동 카드 (있을 때)
─────────────
[프로필 편집] (내 프로필만)
```

**모바일 (1컬럼)**
- 아바타 → 이름/뱃지 → 소개 → 스킬 → Stats → 탭 순서

- [ ] 컨테이너 `maxW="900px"`로 확장
- [ ] `Flex direction={{ base: "column", md: "row" }}` 레이아웃
- [ ] 사이드바 `position="sticky" top="1rem"` 처리
- [ ] 구직 상태 배지 (SEEKING: 초록, OPEN: 노랑)

### 4-2. 핀 게시글 섹션

- [ ] 아코디언 (`allowToggle`, 첫 번째 기본 오픈)
- [ ] 본문 위지윅 HTML 그대로 렌더링
- [ ] 아코디언 헤더: 제목 + 좋아요 + 댓글 수 + 날짜
- [ ] 본문 하단 "게시글로 이동" 링크
- [ ] 내 프로필: 핀 추가/제거/순서 변경 UI

### 4-3. 개발자 프로필 편집 (`/settings` or 인라인)

- [ ] 한 줄 소개, 상세 소개 입력
- [ ] 스킬 태그 선택 (predefined)
- [ ] GitHub / Portfolio URL 입력
- [ ] 학년 선택
- [ ] 구직 상태 선택

### 4-4. Settings 페이지 분리 (`/settings`)

현재 프로필에 섞여있는 설정 항목들 이전:
- [ ] 프레임 보관함
- [ ] 알림 설정
- [ ] 비밀번호 변경
- [ ] 금오인 인증
- [ ] 회원탈퇴

---

## 예상 공수

| Phase | 작업 | 예상 |
|---|---|---|
| 1 | 백엔드 엔티티 + API | 3~4일 |
| 2 | 관리자 페이지 | 1~2일 |
| 3 | 구인구직 프론트 | 3~4일 |
| 4 | 프로필 개편 프론트 | 2~3일 |
| **전체** | | **9~13일** |

---

## 구현 순서 권장

```
1. SkillTag 엔티티 + 관리자 스킬 관리 (기반이 되는 것)
2. DeveloperProfile 엔티티 + API
3. RecruitPost 엔티티 + API + RecruitComment
4. RecruitMenu 추가
5. PinnedPost 엔티티 + API
6. RecruitMainPageConfig + 관리자 설정
7. 알림 연동 (NotificationType.TEAM_RECRUIT)
8. [프론트] 구인구직 목록/상세/작성
9. [프론트] 메인 페이지 카드 섹션
10. [프론트] 프로필 페이지 개편 + Settings 분리
11. [프론트] 핀 게시글 아코디언
12. [프론트] 개발자 프로필 편집 UI
```
