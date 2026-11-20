# 티켓팅 API 명세

> 맨 아래 "적용한 기본값"을 기준으로 구현합니다. 축제 운영 방식이 정해지면 해당 항목을 수정합니다.

## 0. 기존 서버와 맞춘 규칙

인증 서버(`second_festival_backend`)와 프론트엔드(`second_festival_frontend`)를 기준으로 맞췄습니다.

| 항목 | 규칙 | 근거 |
|---|---|---|
| 경로 | `/api/...` | 인증 서버와 같은 형식 |
| 인증 | `Authorization: Bearer <사용자 Access Token>` | 인증 서버가 발급한 JWT를 그대로 검증 |
| 응답 형식 | `ApiResponse` (`success`, `code`, `message`, `data`, `error`) | 프론트 `apiFetch`가 이 형식을 해석함 |
| 오류 코드 | `도메인-HTTP상태` (예: `TICKET-410`) | 인증 서버 `ErrorCode` 형식 |
| 시각 | Asia/Seoul, ISO-8601, 밀리초 3자리 (예: `2026-11-20T10:00:00.000`) | 인증 서버 `serverTime` 형식 |
| CORS | 허용 Origin은 환경변수, `allowCredentials=true` | 프론트 `apiFetch`가 `credentials: "include"`로 요청함 |
| 베이스 URL | 별도 도메인 (예: `https://ticket.chungwoon-festival.com`) | 프론트 `apiFetch`의 `baseUrl` 옵션으로 호출 |

### 공통 응답 형식

```json
// 성공
{ "success": true, "code": "SUCCESS", "message": "...", "data": { ... }, "error": null }

// 실패
{ "success": false, "code": "TICKET-410", "message": "티켓이 모두 소진되었어요",
  "data": null, "error": { "code": "TICKET-410", "message": "티켓이 모두 소진되었어요", "fieldErrors": [] } }
```

### JWT 검증 규칙

인증 서버 `JwtTokenProvider` 기준입니다.

| 검증 | 내용 |
|---|---|
| 서명 | 인증 서버와 같은 `JWT_SECRET` (HMAC, jjwt가 키 길이로 알고리즘 선택) |
| 발급자 | `iss` = `JWT_ISSUER` (기본 `auth-backend`) |
| 만료 | `exp` 지나면 401 |
| **Refresh 토큰 거부** | `type` = `refresh`면 401. Access와 Refresh가 같은 키로 서명되므로 반드시 확인 |
| 사용 클레임 | `userId`(Long), `studentType`(`UNDERGRADUATE` / `GRADUATE` / `ON_LEAVE`) |
| `studentType` 없음 | 카카오 가입 후 추가 정보를 등록하지 않은 사용자 → 403 `TICKET-403` |

401을 받으면 프론트 `apiFetch`가 토큰을 재발급하고 같은 요청을 한 번 더 보냅니다. 발급 API는 멱등이라 재시도해도 안전합니다.

---

## 1. 사용자 API

### 1.1 티켓팅 일정 조회

```
GET /api/tickets/events
인증: 불필요
```

화면에 오픈 시각과 카운트다운을 보여주기 위한 API입니다. 카운트다운은 기기 시계 대신 `serverTime`을 기준으로 맞춥니다.

```json
{
  "serverTime": "2026-11-20T09:59:58.123",
  "events": [
    {
      "eventId": "day1",
      "name": "1일차",
      "totalCount": 900,
      "windows": [
        { "studentType": "UNDERGRADUATE", "opensAt": "2026-11-20T10:00:00.000", "closesAt": "2026-11-20T15:00:00.000" },
        { "studentType": "GRADUATE",      "opensAt": "2026-11-20T10:00:00.000", "closesAt": "2026-11-20T15:00:00.000" },
        { "studentType": "ON_LEAVE",      "opensAt": "2026-11-20T12:00:00.000", "closesAt": "2026-11-20T15:00:00.000" }
      ],
      "status": "UPCOMING"
    }
  ]
}
```

| `status` | 의미 |
|---|---|
| `UPCOMING` | 아직 아무 학생 유형도 열리지 않음 |
| `OPEN` | 한 유형 이상 열려 있고 재고 있음 |
| `SOLD_OUT` | 매진 |
| `CLOSED` | 마감 시각 지남 |

### 1.2 티켓 발급

```
POST /api/tickets/events/{eventId}
인증: 필요
요청 본문: 없음
```

Redis Lua 스크립트 1회로 "중복 확인 → 재고 확인 → 발급"을 원자적으로 처리하고 결과를 즉시 응답합니다.

**성공 (200)**: 새로 발급됐을 때와 이미 발급받은 경우 모두 200이에요. 이미 받은 사람이 다시 요청해도 같은 티켓을 돌려줍니다(멱등).

```json
{
  "status": "ISSUED",
  "eventId": "day1",
  "rank": 123,
  "issuedAt": "2026-11-20T10:00:00.412"
}
```

| `status` | 의미 |
|---|---|
| `ISSUED` | 이번 요청으로 발급됨 |
| `ALREADY_ISSUED` | 이전에 이미 발급받음 (`rank`, `issuedAt`은 처음 발급된 값) |

- `rank`: 발급 순번 (1 ~ `totalCount`, 빈 번호 없음)
- `issuedAt`: Redis 서버 시각으로 기록한 발급 시각 (앱 서버 시계 차이로 순번과 시각이 어긋나지 않도록)

**실패**

| HTTP | 코드 | 상황 | 메시지 (예시) |
|---|---|---|---|
| 401 | `AUTH-402` / `AUTH-403` | 토큰 없음·위조·만료·Refresh 토큰 | 인증 서버와 같은 코드 |
| 403 | `TICKET-403` | 신청 대상이 아닌 학생 유형, 또는 추가 정보 미등록 | 신청 대상이 아니에요 |
| 404 | `TICKET-404` | 없는 `eventId` | 없는 티켓팅이에요 |
| 409 | `TICKET-409` | 아직 내 유형의 오픈 전, 또는 마감 후 | 아직 신청 시간이 아니에요 / 신청이 마감되었어요 |
| 410 | `TICKET-410` | 매진 | 티켓이 모두 소진되었어요 |
| 429 | `TICKET-429` | 사용자별 요청 제한 초과 | 너무 빨리 요청하고 있어요. 잠시 후 다시 시도해 주세요 |
| 503 | `TICKET-503` | Redis 연결 실패·타임아웃 (서킷브레이커 열림 포함) | 잠시 후 다시 시도해 주세요 |

> 이미 발급받은 사용자는 오픈 시간 밖이나 매진 후에도 `ALREADY_ISSUED`를 받습니다. (시간·재고 검사보다 중복 확인이 먼저)

### 1.3 내 티켓 조회

```
GET /api/tickets/me
인증: 필요
```

```json
{
  "tickets": [
    { "eventId": "day1", "rank": 123, "issuedAt": "2026-11-20T10:00:00.412" }
  ]
}
```

- 발급받은 게 없으면 `tickets: []` (404 아님)
- 축제 중에는 Redis, Redis를 내린 뒤에는 RDS에서 조회

---

## 2. 운영(관리자) API

인터넷에 노출하지 않습니다. ALB 리스너 규칙은 `/api/*`와 헬스체크만 앱으로 넘기고, `/internal/*`은 VPC 안에서만(SSM 포트 포워딩) 호출합니다. 추가로 `X-Internal-Key` 헤더를 확인합니다.

| Method | Path | 설명 |
|---|---|---|
| GET | `/internal/events/{eventId}` | 설정 + 현황 (발급 수, Stream 길이, Archiver 미처리 수) |
| PUT | `/internal/events/{eventId}` | 이벤트 설정 생성·수정 (총 수량, 학생 유형별 시간대, 이름) |
| POST | `/internal/events/{eventId}/reset` | 발급 데이터 초기화. **테스트 환경 전용** (운영 프로필에서는 비활성) |

이벤트 설정은 Redis(`ticket:{eventId}:config`)에 저장해 재배포 없이 바꿀 수 있게 합니다. 앱은 설정을 짧게(예: 1초) 캐시합니다.

## 3. 헬스체크

```
GET /actuator/health
```

ALB 헬스체크용. Redis 연결 상태를 포함합니다.

---

## 4. Redis 키 (API 기준 정리)

| 키 | 타입 | 용도 |
|---|---|---|
| `ticket:{eventId}:config` | HASH | 총 수량, 시간대, 이름 |
| `ticket:{eventId}:count` | STRING | 발급 수 (= 마지막 순번) |
| `ticket:{eventId}:issued` | HASH | `userId → "rank|issuedAt"` |
| `ticket:{eventId}:log` | STREAM | 발급 장부 → Archiver |
| `ratelimit:ticket:{userId}` | STRING (TTL) | 사용자별 요청 제한 |

`{eventId}` 중괄호는 Redis Cluster 해시 태그 역할도 합니다 (같은 이벤트 키를 한 슬롯에).

---

## 5. 적용한 기본값

| # | 항목 | 기본값 |
|---|---|---|
| A1 | 일자별 티켓 | `eventId`로 구분 (`day1`, `day2`). 하루만 운영해도 동작 |
| A2 | 여러 일자 신청 | 일자별로 1장씩 허용 |
| A3 | 학생 유형별 시간대 | 학부·대학원 10~15시, 휴학 12~15시. 이벤트 설정으로 변경 가능 |
| A4 | 남은 수량 노출 | 노출하지 않고 `status`만 제공. 새로고침 요청 증가 방지 |
| A5 | 이미 발급받은 사용자의 재요청 | `200` + `ALREADY_ISSUED`. 재시도에 안전 |
| A6 | 매진 HTTP 상태 | `410` (`TICKET-410`). 오픈 전·마감(`409`)과 코드로 구분 |
| A7 | 순번 노출 | 응답에 포함. 화면 노출 여부는 프론트에서 결정 |
| A8 | 요청 제한 | 사용자별 1초 3회, 초과 시 `429` |
| A9 | 티켓팅 도메인 | `ticket.chungwoon-festival.com` |
