# Phase 7 페이지네이션 최적화 설계

## 목표

Phase 7은 **Point History** 단일 테이블을 대상으로 Offset pagination의 deep page 비용과 Cursor pagination의 개선 효과를 측정한다.

이 Phase에서 증명할 내용은 두 가지다.

- Offset 방식은 인덱스가 있어도 깊은 페이지로 갈수록 앞쪽 row를 지나가는 비용이 증가한다.
- Cursor 방식은 마지막으로 본 row의 정렬 키를 조건으로 사용해 deep offset의 skip 비용과 `Page<T>`의 COUNT 쿼리 비용을 줄일 수 있다.

## 범위

Phase 7에 포함한다.

- 전체 `point_history` SQL-only pagination 실험
- hot user의 **Point History** API pagination 실험
- Offset/Page API와 Cursor API 분리
- 전체 테이블 실험용 인덱스와 hot user 실험용 인덱스 비교
- k6를 통한 Offset 얕은/중간/깊은 페이지 p95 비교
- `EXPLAIN (ANALYZE, BUFFERS)`와 `pg_stat_statements` evidence 수집
- Phase 문서와 evidence index 작성

Phase 7에서 제외한다.

- **Delivery Tracking** 필수 실험
- seed 분포 변경 또는 Phase 7 전용 seed preset 추가
- 전체 `point_history` pagination을 사용자-facing API로 만드는 작업
- 페이지 번호 UI 또는 프론트엔드 구현
- PostgreSQL 외 RDBMS에서의 동작 비교

## 측정 조건

기준 환경은 docker compose PostgreSQL과 기존 `loadtest` seed preset이다. Phase 7에서는 seed 조건을 바꾸지 않는다.

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

실험 전에 `point_history` 전체 row 수와 hot user별 row 수를 기록한다.

```sql
SELECT COUNT(*) AS point_history_count
FROM point_history;

SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC
LIMIT 20;
```

hot user API 실험은 row 수가 가장 많은 user를 선정한 뒤 `size = 20` 기준으로 `midPage`, `deepPage`를 계산한다.

```text
maxPage = floor((pointCount - 1) / size)
midPage = floor(maxPage * 0.5)
deepPage = floor(maxPage * 0.8)
```

마지막 페이지는 결과가 0건이거나 `size`보다 적을 수 있으므로 deep page 비교 대상으로 사용하지 않는다.

## Phase 7 전용 데이터 수명주기

Phase 7은 기본 `loadtest` seed preset 자체를 바꾸지 않는다. Cursor/Offset 차이를 더 명확히 보기 위해 amplified hot user를 추가하는 경우, 그 데이터는 Phase 7 전용 측정 조건으로 취급한다.

같은 Docker volume을 계속 사용하는 흐름에서는 Phase 7 종료 후 전용 cleanup 절차를 실행할 수 있어야 한다. cleanup 책임은 다음 Phase가 아니라 Phase 7에 둔다.

```text
scripts/phase-07/05-hot-user-amplify.sql
scripts/phase-07/06-hot-user-cleanup.sql
```

cleanup은 Phase 7이 추가한 user와 `point_history`만 제거해야 한다. 삭제 조건은 `user_id=707000`, `id` 범위, `description='phase7 hot user amplification'`처럼 Phase 7 fixture를 식별할 수 있는 값으로 제한한다. 전체 `point_history`나 기본 `loadtest` seed 데이터는 삭제하지 않는다.

Phase 간 완전 격리가 필요하면 `docker compose down -v` 후 `./scripts/seed.sh loadtest`로 DB를 재생성한다. DB volume을 유지한 채 다음 Phase로 이동할 때는 Phase 7 cleanup SQL을 실행하고, `user_id=707000`의 `point_history` count가 0건인지 확인한다.

## 인덱스 전략

Phase 7에서는 Offset과 Cursor를 같은 정렬 기준과 같은 인덱스 조건에서 비교한다. 인덱스 없는 Offset과 인덱스 있는 Cursor를 비교하지 않는다.

전체 `point_history` SQL-only 실험용 인덱스:

```sql
CREATE INDEX idx_point_history_created_id
ON point_history (created_at DESC, id DESC);
```

hot user의 **Point History** API 실험용 인덱스:

```sql
CREATE INDEX idx_point_history_user_created_id
ON point_history (user_id, created_at DESC, id DESC);
```

`user_id`는 `WHERE user_id = ?` 필터를 위한 선행 컬럼이다. `created_at DESC, id DESC`는 최신순 정렬과 Cursor 조건을 위한 컬럼이다. `id`는 같은 `created_at` 값을 가진 row 사이의 안정적인 tie-breaker다.

## SQL-only 원리 확인 실험

전체 `point_history` 실험은 실제 API 시나리오가 아니라 Offset과 Cursor의 동작 원리를 확인하는 DB 레벨 실험이다.

전체 테이블 Offset shallow:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;
```

전체 테이블 Offset deep:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :globalDeepOffset;
```

전체 테이블 Cursor deep은 먼저 같은 위치의 cursor 값을 구한 뒤 실행한다.

```sql
SELECT created_at, id
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 1 OFFSET :globalDeepOffset;
```

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

확인할 내용:

- Offset deep이 Offset shallow보다 더 많은 row를 지나가는가
- Cursor deep이 같은 위치에서 page size 중심으로 조회되는가
- 각 쿼리의 `Execution Time`, `actual rows`, `Buffers`, index scan 여부가 어떻게 다른가

## API 설계

기존 Offset API와 새 Cursor API를 분리한다.

```http
GET /api/points?userId=1&page=0&size=20
GET /api/points/cursor?userId=1&size=20
GET /api/points/cursor?userId=1&size=20&lastCreatedAt=2026-05-27T10:00:00&lastId=12345
```

`GET /api/points`는 Offset/Page baseline이다.

- `page`, `size`를 받는다.
- `Page<PointHistoryResponse>` 형태를 유지한다.
- data query와 count query가 발생할 수 있다.
- k6에서 `page=0`, `page=midPage`, `page=deepPage`를 비교한다.

`GET /api/points/cursor`는 Cursor 개선안이다.

- 첫 페이지 요청에는 `lastCreatedAt`, `lastId`가 없다.
- 다음 페이지 요청에는 이전 응답의 마지막 row 기준 cursor를 전달한다.
- `LIMIT size + 1`로 조회해 `hasNext`를 판단한다.
- 전체 개수를 세지 않는다.

응답 형태:

```json
{
  "items": [],
  "nextCursor": {
    "lastCreatedAt": "2026-05-27T10:00:00",
    "lastId": 12345
  },
  "hasNext": true
}
```

마지막 페이지이면 `hasNext = false`이고 `nextCursor = null`로 응답한다.

## Cursor 조회 규칙

정렬 기준은 Offset과 Cursor 모두 동일하게 유지한다.

```sql
ORDER BY created_at DESC, id DESC
```

Cursor 조건은 정렬 기준과 같은 컬럼을 사용한다.

```sql
WHERE user_id = :userId
  AND (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT :sizePlusOne;
```

`created_at`만 cursor로 사용하지 않는다. 같은 timestamp를 가진 row가 여러 개 있을 수 있으므로 `id`를 함께 사용해 중복과 누락을 막는다.

## COUNT 쿼리 비교

Offset/Page API는 전체 페이지 수를 계산하기 위해 count query가 발생할 수 있다.

```sql
SELECT COUNT(*)
FROM point_history
WHERE user_id = :userId;
```

Cursor API는 전체 개수를 반환하지 않는다. 대신 `size + 1`건을 조회해 다음 페이지 존재 여부만 판단한다.

```sql
SELECT *
FROM point_history
WHERE user_id = :userId
  AND (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 21;
```

`pg_stat_statements` evidence는 Offset/Page API에서 count query가 호출되는지, Cursor API에서 count query가 사라지는지 확인한다.

## k6 Evidence

k6는 hot user API 실험에 사용한다. 전체 테이블 SQL-only 실험은 k6 대상이 아니다.

필수 k6 시나리오:

- Offset shallow: `GET /api/points?userId={userId}&page=0&size=20`
- Offset mid: `GET /api/points?userId={userId}&page={midPage}&size=20`
- Offset deep: `GET /api/points?userId={userId}&page={deepPage}&size=20`
- Cursor: `GET /api/points/cursor?userId={userId}&size=20` 이후 응답 cursor를 사용한 다음 페이지 요청

k6 summary에는 최소한 다음 값을 남긴다.

- `http_req_duration` p95
- 요청 수
- 실패율
- VU, duration, preset, 대상 `user_id`

## Evidence 구조

```text
docs/evidence/phase-07/
  README.md
  data-profile/
    point-history-total-count.txt
    hot-user-point-counts.txt
    selected-user-and-pages.txt
  explain/
    global-offset-page0.txt
    global-offset-deep.txt
    global-cursor-deep.txt
    user-offset-page0.txt
    user-offset-deep.txt
    user-cursor-deep.txt
  k6/
    offset-page0-summary.txt
    offset-mid-summary.txt
    offset-deep-summary.txt
    cursor-summary.txt
  pg-stat-statements/
    offset-page-api.txt
    cursor-api.txt
  grafana/
    db-lab-overview-phase-7.png
```

`README.md`에는 각 evidence 파일이 어떤 측정 조건에서 생성됐는지 기록한다. ADR 0005에 맞춰 k6 metric label은 `phase`, `scenario`, `preset`, `pool`처럼 low-cardinality 값만 사용한다.

## 테스트

코드 테스트는 API 동작을 검증하고, 실행계획 자체는 SQL evidence로 검증한다.

필수 API 테스트:

- `GET /api/points`가 기존 Offset/Page 응답을 반환한다.
- `GET /api/points/cursor` 첫 페이지 요청이 `items`, `hasNext`, `nextCursor`를 반환한다.
- Cursor 다음 페이지 요청이 이전 응답의 cursor 이후 데이터를 반환한다.
- Cursor 응답은 `size`개 이하의 `items`만 노출한다.
- `created_at`이 같은 row가 있어도 `id` tie-breaker로 정렬이 안정적이다.
- 마지막 페이지에서 `hasNext = false`, `nextCursor = null`이 된다.

SQL 스크립트는 docker compose PostgreSQL에서 실행하고, 기대한 evidence 파일이 생성됐는지 확인한다.

## 문서 업데이트

생성 또는 갱신할 문서:

- `docs/roadmap/08-phase-7-pagination.md`
- `docs/phases/07-pagination/README.md`
- `docs/phases/07-pagination/scope.md`
- `docs/phases/07-pagination/runbook.md`
- `docs/phases/07-pagination/observability.md`
- `docs/phases/07-pagination/report.md`
- `docs/evidence/phase-07/README.md`
- Phase 7 k6 시나리오가 추가되는 경우 `docs/guides/k6-load-testing.md`

Phase 디렉터리는 표준 5개 문서만 둔다.

## 결정 사항

- Phase 7의 주 대상은 **Point History** 단일 테이블이다.
- **Delivery Tracking**은 이번 Phase의 필수 실험에서 제외한다.
- 기존 seed 조건은 유지한다.
- amplified hot user 데이터는 Phase 7 전용 fixture이며, long-lived Docker volume에서는 Phase 7 cleanup 절차로 제거한다.
- 전체 `point_history` SQL-only 실험은 필수 evidence로 둔다.
- API/k6 실험은 hot user의 **Point History** 목록으로 진행한다.
- Offset API와 Cursor API는 분리한다.
- Cursor API는 `lastCreatedAt`, `lastId`를 명시 파라미터로 받는다.
- 정렬 기준은 `created_at DESC, id DESC`로 고정한다.
- 전체 테이블 실험과 hot user 실험은 서로 다른 복합 인덱스를 사용한다.
- 정량 성공 기준은 고정 ms가 아니라 동일 조건의 evidence 비교로 판단한다.
