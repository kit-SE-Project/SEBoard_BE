# DB 마이그레이션 가이드

구 서버(단일 서버) → K3s PostgreSQL 마이그레이션 절차

## 환경 정보

| 항목 | 값 |
|---|---|
| DB 유저 | `se` |
| DB 이름 | `seboard` |
| 구 서버 IP | `[구서버IP]` |
| 구 서버 유저 | `[구서버유저]` |
| se1 IP | `[se1LAN_IP]` |
| se1 유저 | `[se1유저]` |

---

## 구 스키마 vs 신 스키마 차이점

### 기존 테이블 변경

| 테이블 | 변경 내용 |
|---|---|
| `file_meta_data` | `post_id` 제거 → `attachable_type VARCHAR(20)`, `attachable_id BIGINT` 추가 (폴리모픽 구조) |
| `roles` | `badge_type VARCHAR(255)`, `badge_priority INTEGER` 추가 (nullable, Hibernate 자동 처리) |
| `members` | `tier VARCHAR(255)`, `activity_score BIGINT`, `equipped_frame_id BIGINT` (FK→frames) 추가 |
| `menus` | `popular_post_enabled BOOLEAN NOT NULL DEFAULT false` 추가 |

### 신규 테이블 (Hibernate ddl-auto: update 가 자동 생성)

| 테이블 | 설명 |
|---|---|
| `frames` | 프로필 프레임 (grade/event/admin 종류) |
| `member_frames` | 회원-프레임 소유 관계 |
| `skill_tags` | 기술 스택 태그 (name UNIQUE, category, icon_slug, is_active) |
| `recruit_posts` | 구인구직 게시글 (type, tag, status: ACTIVE/CLOSED/DELETED, board_user_id, expose_option_id 등) |
| `recruit_comments` | 구인구직 댓글 (parent_comment_id 자기참조, is_deleted) |
| `recruit_post_skills` | recruit_posts ↔ skill_tags 중간 테이블 |
| `recruit_main_page_config` | 메인 페이지 구인구직 노출 설정 (visible, display_count, sort_type) |
| `developer_profiles` | 개발자 프로필 (account_id UNIQUE, intro, github_url, portfolio_url, readme_content 등) |
| `developer_profile_skills` | developer_profiles ↔ skill_tags 중간 테이블 |
| `pinned_posts` | 프로필 핀 게시글 (account_id, post_id, order_index, UNIQUE(account_id, post_id)) |

---

## 마이그레이션 절차

### 1단계: 백엔드 중단

```bash
kubectl scale deployment seboard-backend -n seboard --replicas=0
```

### 2단계: K3s DB 초기화

다른 세션이 연결돼 있으면 DROP이 실패하므로, 백엔드를 먼저 내린 후 실행한다.

```bash
kubectl exec -it -n seboard postgres-0 -- psql -U se -d postgres -c "DROP DATABASE seboard;"
kubectl exec -it -n seboard postgres-0 -- psql -U se -d postgres -c "CREATE DATABASE seboard OWNER se;"
```

### 3단계: 구 서버에서 Full Dump

`--data-only` 없이 스키마+데이터 전체 덤프한다.

```bash
# 구 서버에서 실행
pg_dump -U se -d se -F c -f ~/seboard_full.dump
```

> **주의**: `--data-only`로 덤프하면 FK 순환 참조(comments 자기참조, menus 자기참조) 문제로 복구가 어렵다. Full dump를 사용한다.

### 4단계: 덤프 파일 전송

```bash
# 맥으로 다운로드 (로컬 맥에서 실행)
scp [구서버유저]@[구서버IP]:~/seboard_full.dump ~/Desktop/seboard_full.dump

# se1으로 업로드 (로컬 맥에서 실행)
scp ~/Desktop/seboard_full.dump [se1유저]@[se1LAN_IP]:~/
```

### 5단계: K3s postgres pod으로 복사

```bash
# se1에서 실행
kubectl cp ~/seboard_full.dump seboard/postgres-0:/tmp/seboard_full.dump
```

### 6단계: 복구

```bash
kubectl exec -n seboard postgres-0 -- pg_restore \
  -U se -d seboard \
  --disable-triggers \
  /tmp/seboard_full.dump
```

> `--disable-triggers`: 복구 중 FK 제약 체크를 비활성화한다. comments/menus 자기참조 FK 문제 방지용.

### 7단계: 스키마 마이그레이션 SQL 실행

> 신규 테이블(skill_tags, recruit_posts 등)은 8단계 백엔드 재시작 시 Hibernate가 자동 생성하므로 아래 SQL을 먼저 실행한다.

#### file_meta_data: post_id → attachable_type/attachable_id 변환

```bash
kubectl exec -n seboard postgres-0 -- psql -U se -d seboard -c "
ALTER TABLE file_meta_data ADD COLUMN IF NOT EXISTS attachable_type VARCHAR(20);
ALTER TABLE file_meta_data ADD COLUMN IF NOT EXISTS attachable_id BIGINT;
UPDATE file_meta_data SET attachable_type='POST', attachable_id=post_id WHERE post_id IS NOT NULL;
ALTER TABLE file_meta_data DROP COLUMN IF EXISTS post_id;
"
```

#### roles: badge 컬럼 추가

nullable이라 Hibernate가 자동 추가하지만, 복구된 스키마에 없을 경우 수동으로 추가한다.

```bash
kubectl exec -n seboard postgres-0 -- psql -U se -d seboard -c "
ALTER TABLE roles ADD COLUMN IF NOT EXISTS badge_type VARCHAR(255);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS badge_priority INTEGER;
"
```

#### members: 신규 컬럼 추가 및 기본값 설정

```bash
kubectl exec -n seboard postgres-0 -- psql -U se -d seboard -c "
ALTER TABLE members ADD COLUMN IF NOT EXISTS tier VARCHAR(255);
ALTER TABLE members ADD COLUMN IF NOT EXISTS activity_score BIGINT;
ALTER TABLE members ADD COLUMN IF NOT EXISTS equipped_frame_id BIGINT;
UPDATE members SET tier='BRONZE' WHERE tier IS NULL;
UPDATE members SET activity_score=0 WHERE activity_score IS NULL;
"
```

#### menus: popular_post_enabled 추가

```bash
kubectl exec -n seboard postgres-0 -- psql -U se -d seboard -c "
ALTER TABLE menus ADD COLUMN IF NOT EXISTS popular_post_enabled BOOLEAN DEFAULT false;
UPDATE menus SET popular_post_enabled = false WHERE popular_post_enabled IS NULL;
ALTER TABLE menus ALTER COLUMN popular_post_enabled SET NOT NULL;
"
```

> **menus.menu_type**: 기존 컬럼(VARCHAR 31)이 그대로 유지되며, 새 값 `RECRUIT`는 코드에서 `@DiscriminatorValue("RECRUIT")`로 처리된다. 별도 DDL 불필요.

### 8단계: 백엔드 재시작

Hibernate `ddl-auto: update`가 신규 테이블(skill_tags, recruit_posts, recruit_comments, recruit_post_skills, recruit_main_page_config, developer_profiles, developer_profile_skills, pinned_posts, frames, member_frames)을 자동 생성하고 나머지 누락 컬럼을 추가한다.

```bash
kubectl scale deployment seboard-backend -n seboard --replicas=1
kubectl rollout status deployment/seboard-backend -n seboard
```

### 9단계: 초기 데이터 확인

백엔드 기동 시 `InitRequiredData`가 실행되며 아래 데이터를 자동 삽입한다.

- 기본 Role (ROLE_ADMIN, ROLE_KUMOH, ROLE_USER 등)
- 108개 SkillTag (category별 기술 스택)
- RecruitMenu (구인구직 메뉴)
- DashBoardMenu (관리자 대시보드 메뉴)
- FileConfiguration, LoginSetting 등 기본 설정

> 이미 존재하는 항목은 `existsByName` / `existsByXxx` 체크로 중복 삽입을 건너뜀.

### 10단계: 로그 확인

```bash
kubectl logs -n seboard deployment/seboard-backend --tail=200
```

에러 없이 `Started SeBoardApplication` 로그가 뜨면 성공.

---

## 업로드 파일 이전

DB 외에 `/app/files` 디렉토리의 실제 파일도 이전해야 한다.

```bash
# 구 서버 파일을 맥으로 다운로드
rsync -avz [구서버유저]@[구서버IP]:/경로/to/files/ ~/Desktop/seboard-files/

# se1으로 업로드
scp -r ~/Desktop/seboard-files/ [se1유저]@[se1LAN_IP]:~/

# se1에서 백엔드 pod으로 복사
kubectl cp ~/seboard-files/ seboard/[백엔드pod이름]:/app/files/
```

백엔드 pod 이름 확인:
```bash
kubectl get pod -n seboard -l app=seboard-backend
```

---

## DNS 절체

1. 구 서버 점검 모드 전환 (신규 글 작성 차단)
2. 최종 DB 덤프 → K3s 복구 (위 절차 반복)
3. 업로드 파일 최종 동기화
4. DNS 레코드 변경: `seboard.site` → Cloudflare Tunnel 또는 새 IP
5. 전체 기능 점검

---

## 트러블슈팅

| 에러 | 원인 | 해결 |
|---|---|---|
| `duplicate key value` | DB가 비어있지 않음 (Hibernate 초기 데이터 존재) | 2단계부터 다시: backend 내리고 DB drop & recreate |
| `Can not set boolean field ... to null value` | 구 스키마에 없는 NOT NULL boolean 컬럼 | 해당 컬럼을 `DEFAULT false`로 ALTER TABLE 추가 |
| `pg_dump 경고: 참조키가 서로 교차` | comments/menus 자기참조 FK | 경고만 뜨고 덤프는 정상 생성됨. 복구 시 `--disable-triggers` 사용 |
| `database is being accessed by other users` | 백엔드 pod이 DB에 연결 중 | backend scale 0으로 내린 후 DROP |
| `column popular_post_enabled contains null values` | NOT NULL 컬럼인데 기존 rows에 값 없음 | `UPDATE menus SET popular_post_enabled=false` 후 `ALTER COLUMN SET NOT NULL` |
| `operator does not exist: character varying = integer` | JPA Enum 타입 매핑 오류 | `@Enumerated(EnumType.STRING)` 확인 |
| Hibernate `schema-validation` 실패 | ddl-auto가 validate인 경우 신규 컬럼 미존재 | prod는 update 모드 사용 또는 수동 DDL 먼저 실행 |
