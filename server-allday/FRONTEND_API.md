# Server AllDay 프론트 API

`AllDay 세션 쿠키`를 사용합니다. 로그인은 같은 오리진의 `GET /api/v1/sso/start`에서 시작합니다. 로그인 상태는 `GET /api/v1/me`에서 확인합니다. 상태를 변경하는 요청에는 브라우저가 대여 사이트의 `Origin` 헤더를 보냅니다. 카테고리 관리 API에는 SEBoard DB의 `ROLE_ADMIN`이 필요합니다.

| 요청 | 본문 | 성공 |
| --- | --- | --- |
| `GET /api/v1/categories` | 없음 | `200` 대여 화면용 카테고리 배열 |
| `GET /api/v1/admin/categories` | 없음 | `200` 관리용 카테고리 배열. 장비가 배정되지 않은 항목도 반환 |
| `POST /api/v1/admin/categories` | `{"name":"GPU 서버","description":"실습용"}` | `201` 생성된 카테고리 |
| `PATCH /api/v1/admin/categories/{id}` | `{"name":"GPU 서버","description":null,"expectedVersion":0}` | `200` 수정된 카테고리 |
| `DELETE /api/v1/admin/categories/{id}` | `{"expectedVersion":1}` | `204` |

카테고리 객체는 `id`, `name`, `description`, `version`을 반환합니다. 이름은 필수이며 최대 100자, 설명은 최대 2,000자입니다. 설명은 `null` 또는 빈 문자열을 보내면 지울 수 있습니다. 수정할 때 현재 이름과 설명을 함께 보내고 목록에서 받은 버전을 `expectedVersion`에 사용합니다.

등록·수정·삭제에는 `Idempotency-Key`가 필요합니다. 네트워크 실패로 같은 요청을 다시 보낼 때는 키와 본문을 재사용합니다. 버전 충돌이 나면 목록을 다시 조회합니다. 연결된 활성 장비가 있으면 다른 카테고리로 옮긴 뒤 삭제합니다. 삭제된 장비와 과거 대여 기록은 보존합니다.

| 상태 | 코드 | 의미 |
| --- | --- | --- |
| `401` | `UNAUTHENTICATED`, `TOKEN_EXPIRED` | 로그인 또는 토큰 갱신 필요 |
| `403` | `FORBIDDEN` | 관리자 권한 필요 |
| `404` | `NOT_FOUND` | 카테고리가 없거나 삭제됨 |
| `409` | `CATEGORY_NAME_EXISTS` | 활성 카테고리 이름 중복 |
| `409` | `CATEGORY_IN_USE` | 활성 장비에서 참조 중 |
| `409` | `VERSION_CONFLICT` | 다른 변경으로 버전이 바뀜 |
| `422` | `INVALID_INPUT` | 요청 필드 또는 버전 형식 오류 |

예시 오류 응답

```json
{
  "status": 409,
  "code": "CATEGORY_IN_USE",
  "message": "연결된 서버를 다른 카테고리로 옮긴 뒤 삭제하세요.",
  "requestId": "b3f7eb49-9c6c-4b37-8494-dc76a0476290"
}
```

회원별 예약 한도는 `POST /api/v1/rental-previews`의 `canReserve: false` 및 `conflicts[].code: "MEMBER_RENTAL_LIMIT"`에 반영됩니다. `POST /api/v1/rentals`에서는 같은 코드로 `409`를 반환합니다. 미래 예약, 준비·이용 및 반납 진행 중인 계약도 한 건으로 계산합니다. 동일한 예약의 여러 시간 슬롯은 한 건에 포함됩니다.

장비 응답의 `gpuEnabled`는 패스스루 사용 여부입니다. 관리자 인벤토리 조회와 설정 응답에 `gpuMappingRef`도 포함됩니다. 새 장비 등록 시 `gpuEnabled: false`로 CPU 전용 장비를 등록할 수 있습니다. 기존 등록 클라이언트가 이 필드를 생략하면 기본값은 `true`입니다.
