# Phase 7 페이지네이션 재측정 Spec

## 목적

Phase 7 재측정은 `point_history`에서 Offset/Page와 Cursor를 단순히 "누가 더 빠른가"로 비교하지 않는다. 두 방식의 UX 목적과 비용 구조가 다르기 때문이다.

이 spec은 Phase 7 보고서를 다시 작성하기 위한 A/B/C 테스트 기준을 정의한다.

- A. Offset/Page 방식에서 page depth가 커질수록 어떤 비용이 나타나는지 확인한다.
- B. Cursor 방식에서 이미 cursor 값을 가진 상태의 next-slice 조회 비용이 logical depth와 어떻게 관계되는지 확인한다.
- C. `Page<T>`가 만드는 count query 비용과 Cursor 방식의 count query 제거 효과를 분리해서 확인한다.

## 핵심 해석 원칙

Offset/Page는 page 번호로 임의 위치에 접근할 수 있고 total count 기반 UI를 만들기 쉽다. 대신 deep page에서는 앞쪽 row를 지나가는 skip 비용과 `Page<T>`의 count query 비용을 부담한다.

Cursor는 임의 page 번호 점프를 대체하지 않는다. Cursor는 이전 응답의 마지막 row가 가진 정렬 키를 기준으로 다음 slice를 가져오는 방식이다. 따라서 "deep page로 바로 이동"이 아니라 "이미 cursor를 가진 상태에서 다음 slice를 조회"하는 비용으로 평가해야 한다.

보고서의 최종 결론은 다음 형태로 작성한다.

```text
Page와 Cursor는 UX 목적이 다르다.
Page는 임의 page 접근과 total count 제공을 위해 skip/count 비용을 부담한다.
Cursor는 임의 page 점프를 포기하는 대신, 순차 탐색에서 다음 slice를 count 없이 size 중심으로 조회한다.
Phase 7은 이 trade-off를 point_history hot user 조건에서 관측했다.
```

## 공통 측정 조건

기준 테이블은 `point_history`다. API/k6 실험은 Phase 7 전용 amplified hot user를 사용한다.

```text
user_id: 707000
point count: 100000
size: 20
maxPage: 4999
```

Offset과 Cursor는 같은 logical order를 사용한다.

```sql
ORDER BY created_at DESC, id DESC
```

Offset API도 Cursor API와 같은 정렬을 사용해야 한다. 정렬이 다르면 같은 logical position 비교가 아니다.

```java
PageRequest.of(
    page,
    size,
    Sort.by(Sort.Direction.DESC, "createdAt")
        .and(Sort.by(Sort.Direction.DESC, "id"))
)
```

hot user 실험용 인덱스는 아래 조건을 전제로 한다.

```sql
CREATE INDEX IF NOT EXISTS idx_point_history_user_created_id
ON point_history (user_id, created_at DESC, id DESC);
```

## Phase 7 전용 데이터와 Cleanup

`user_id=707000`의 100,000건 `point_history`는 Phase 7 재측정을 위한 amplified fixture다. 기본 `loadtest` seed에 속한 데이터로 해석하지 않는다.

Docker volume을 매 Phase마다 재생성하지 않고 계속 사용하는 경우, Phase 7은 데이터를 추가하는 스크립트와 제거하는 스크립트를 함께 제공해야 한다.

```text
scripts/phase-07/05-hot-user-amplify.sql
scripts/phase-07/06-hot-user-cleanup.sql
```

cleanup SQL은 Phase 7 fixture만 삭제한다. 최소 조건은 아래 값을 함께 사용한다.

```text
user_id = 707000
point_history id range = 707000001..707100000
description = 'phase7 hot user amplification'
user email = 'phase7-hot-user@example.com'
```

cleanup은 evidence 수집 전에 실행하지 않는다. Phase 7 evidence와 report를 저장한 뒤 같은 Docker volume으로 다른 Phase를 진행할 때 실행한다. cleanup 후에는 아래 조건을 확인한다.

```sql
SELECT COUNT(*) AS phase7_point_count
FROM point_history
WHERE user_id = 707000;

SELECT COUNT(*) AS phase7_user_count
FROM users
WHERE id = 707000;
```

두 값은 모두 `0`이어야 한다. Phase 간 완전 격리가 필요하면 cleanup 대신 `docker compose down -v` 후 `./scripts/seed.sh loadtest`로 DB를 재생성한다.

## Cache 조건

`pg_stat_statements_reset()`은 SQL 통계만 초기화한다. PostgreSQL shared buffers, OS page cache, JVM, connection pool 상태는 초기화하지 않는다.

Phase 7 재측정은 cold read 성능이 아니라 warm-cache 성격의 반복 부하 결과로 해석한다. 보고서에는 아래 조건을 명시한다.

```text
각 k6 run은 warm-cache 반복 부하 조건이다.
pg_stat_statements_reset()은 run별 SQL 통계를 분리하기 위한 것이며,
shared buffers와 OS page cache는 초기화하지 않았다.
```

page depth 자체를 더 엄밀히 확인해야 하면 같은 sampling test를 최소 3회 반복하고, 실행 순서를 바꾼 뒤 median p95를 보고서에 사용한다.

## A. Offset/Page Depth 테스트

### 가설

Offset/Page는 page 번호로 원하는 logical position에 접근할 수 있다. 그러나 page 번호가 커질수록 data query는 더 많은 row를 지나가야 하고, `Page<T>`는 page depth와 무관하게 count query도 함께 실행한다.

### SQL-only 확인

대표 지점에서 `EXPLAIN (ANALYZE, BUFFERS)`를 저장한다.

```text
page0: page=0, offset=0
mid: page=2499, offset=49980
deep: page=3999, offset=79980
last: page=4999, offset=99980
```

SQL 형태:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = 707000
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :offset;
```

확인 항목:

- `Execution Time`
- `actual rows`
- `Buffers`
- index scan 여부
- page depth 증가에 따른 읽고 지나간 row 수

### k6 sampling

고정된 3개 page만 비교하지 않고 page 범위를 샘플링한다.

기본 sample set:

```text
pages = [0, 10, 50, 100, 500, 1000, 2000, 3000, 4000, 4999]
```

k6는 각 iteration에서 sample page 중 하나를 선택하고 같은 API를 호출한다.

```text
GET /api/points?userId=707000&page={page}&size=20
```

page별 latency를 보기 위해 k6 custom Trend를 사용한다.

```text
points_offset_page_0_duration
points_offset_page_10_duration
points_offset_page_50_duration
points_offset_page_100_duration
points_offset_page_500_duration
points_offset_page_1000_duration
points_offset_page_2000_duration
points_offset_page_3000_duration
points_offset_page_4000_duration
points_offset_page_4999_duration
```

Prometheus label을 사용할 경우 page 번호 전체를 고카디널리티 label로 만들지 않는다. 위 sample set처럼 제한된 bucket만 사용한다.

### A의 판정 기준

보고서에는 "Offset/Page는 page가 깊어질수록 항상 단조 증가한다"고 단정하지 않는다. k6 p95는 cache, JVM, GC, Docker scheduling, connection pool, 일시적 stall 영향을 받는다.

대신 아래를 판정한다.

- SQL-only evidence에서 offset이 커질수록 DB가 더 많은 row를 지나가는가
- k6 sampling에서 deep page bucket들이 shallow bucket보다 불리한 경향을 보이는가
- Offset/Page run에서 data query와 count query가 함께 발생하는가

## B. Cursor Next-Slice 테스트

### 가설

Cursor는 page 번호로 임의 위치에 이동하는 방식이 아니다. Cursor는 이전 응답에서 받은 `lastCreatedAt`, `lastId` 이후의 다음 slice를 조회한다.

따라서 B 테스트는 cursor source lookup 비용을 포함하지 않고, 이미 cursor 값을 알고 있는 상태의 next-slice 비용만 측정한다.

### Cursor sample 생성

A와 같은 logical page sample을 사용한다.

```text
pages = [0, 10, 50, 100, 500, 1000, 2000, 3000, 4000, 4999]
size = 20
```

각 page와 같은 slice를 Cursor API로 가져오려면 cursor source offset은 아래와 같다.

```text
cursor_source_offset = page * size - 1
```

예:

```text
page=0    -> cursor 없음
page=10   -> cursor source offset=199
page=50   -> cursor source offset=999
page=100  -> cursor source offset=1999
page=500  -> cursor source offset=9999
page=1000 -> cursor source offset=19999
page=4999 -> cursor source offset=99979
```

cursor sample은 측정 준비 단계에서 미리 생성한다.

```sql
WITH sample_pages(page_no) AS (
    VALUES
        (0),
        (10),
        (50),
        (100),
        (500),
        (1000),
        (2000),
        (3000),
        (4000),
        (4999)
),
sample_offsets AS (
    SELECT page_no,
           page_no * 20 AS target_offset,
           page_no * 20 - 1 AS cursor_source_offset
    FROM sample_pages
)
SELECT s.page_no,
       s.target_offset,
       s.cursor_source_offset,
       ph.created_at AS last_created_at,
       ph.id AS last_id
FROM sample_offsets s
LEFT JOIN LATERAL (
    SELECT created_at, id
    FROM point_history
    WHERE user_id = 707000
    ORDER BY created_at DESC, id DESC
    LIMIT 1 OFFSET GREATEST(s.cursor_source_offset, 0)
) ph ON s.page_no > 0
ORDER BY s.page_no;
```

`page=0`은 cursor 없이 첫 페이지를 요청한다. `page > 0`은 위 결과의 `last_created_at`, `last_id`를 API 요청에 넣는다.

cursor sample lookup은 API latency 측정에 포함하지 않는다. 보고서에는 아래 조건을 명시한다.

```text
Cursor sampling의 cursor values는 측정 준비 단계에서 사전 계산했다.
이는 deep page로 직접 점프하는 비용을 측정하기 위한 것이 아니라,
각 logical depth에서 이미 cursor를 가진 상태의 next-slice 조회 비용을 비교하기 위한 조건이다.
```

### SQL-only 확인

Cursor next-slice 자체 비용만 보기 위해 `OFFSET`을 CTE에 포함하지 않는다. 사전 계산한 cursor 값을 psql 변수로 주입한다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT *
FROM point_history
WHERE user_id = 707000
  AND (created_at, id) < (:'lastCreatedAt'::timestamp, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

대표적으로 shallow, mid, deep 위치의 cursor 값을 넣어 실행한다.

```text
page=10
page=1000
page=4999
```

확인 항목:

- cursor lookup 없이 next-slice query만 실행되는가
- logical depth가 깊어져도 `LIMIT 20` 중심의 비용으로 실행되는가
- index condition이 정렬 키와 맞게 사용되는가

### k6 sampling

k6는 preset 파일에 저장된 cursor samples 중 하나를 선택한다.

예시 preset 구조:

```json
{
  "baseUrl": "http://host.docker.internal:8080",
  "userId": 707000,
  "size": 20,
  "rate": 50,
  "duration": "5m",
  "preAllocatedVUs": 100,
  "maxVUs": 300,
  "samples": [
    { "page": 0, "lastCreatedAt": null, "lastId": null },
    { "page": 10, "lastCreatedAt": "2026-05-27T10:12:33.123456", "lastId": 707012345 }
  ]
}
```

요청 형태:

```text
page=0:
GET /api/points/cursor?userId=707000&size=20

page>0:
GET /api/points/cursor?userId=707000&size=20&lastCreatedAt={lastCreatedAt}&lastId={lastId}
```

현재처럼 한 iteration에서 first request와 next request를 함께 보내지 않는다. Cursor sampling test는 iteration당 하나의 cursor request만 보낸다.

Cursor도 page bucket별 Trend를 남긴다.

```text
points_cursor_page_0_duration
points_cursor_page_10_duration
points_cursor_page_50_duration
points_cursor_page_100_duration
points_cursor_page_500_duration
points_cursor_page_1000_duration
points_cursor_page_2000_duration
points_cursor_page_3000_duration
points_cursor_page_4000_duration
points_cursor_page_4999_duration
```

### B의 판정 기준

보고서에는 "Cursor가 page 번호 점프를 제공한다"고 쓰지 않는다.

아래를 판정한다.

- 이미 cursor를 가진 상태에서 next-slice 조회가 logical depth와 크게 무관한가
- Cursor k6 sampling에서 deep cursor bucket이 shallow bucket 대비 급격히 나빠지지 않는가
- `pg_stat_statements`에서 count query 없이 first cursor query와 next cursor query만 관측되는가

## C. COUNT Query 비용 테스트

### 가설

`Page<T>`는 total elements와 total pages를 제공하기 위해 data query 외에 count query를 실행한다. Cursor는 total count를 제공하지 않고 `size + 1` 조회로 `hasNext`를 판단하므로 count query를 실행하지 않는다.

### pg_stat_statements 분리

각 run 전 아래 쿼리를 실행한다.

```sql
SELECT pg_stat_statements_reset();
```

run 후 snapshot을 저장한다.

```sql
SELECT calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       rows,
       query
FROM pg_stat_statements
WHERE query ILIKE '%point_history%'
ORDER BY total_exec_time DESC;
```

Offset/Page sampling run에서 기대하는 query shape:

```text
- point_history data query with offset/fetch
- count(*) from point_history where user_id = ?
```

Cursor sampling run에서 기대하는 query shape:

```text
- first cursor query
- next cursor query
- count(*) from point_history 없음
```

### count-only SQL evidence

count query 자체 비용을 별도로 남긴다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM point_history
WHERE user_id = 707000;
```

이 evidence는 Offset/Page API의 전체 지연 시간을 모두 설명하기 위한 것이 아니라, `Page<T>`가 매 요청 추가로 부담하는 query의 비용을 분리해서 보여주기 위한 것이다.

### 선택 실험: count 없는 Offset baseline

필요하면 `Page<T>`가 아닌 `List` 또는 `Slice` 기반 Offset API를 별도로 추가한다.

```text
GET /api/points/offset-slice?userId=707000&page={page}&size=20
```

이 선택 실험을 추가하면 비용을 세 층으로 분해할 수 있다.

```text
Offset/Page = offset skip 비용 + count 비용
Offset/List = offset skip 비용만
Cursor = cursor 이후 size + 1 조회 비용
```

이 API는 Phase 7의 필수 요구사항은 아니다. 보고서에서 count 비용과 data query 비용이 충분히 분리되지 않을 때만 추가한다.

### C의 판정 기준

아래를 확인한다.

- Offset/Page run에서 count query calls가 HTTP request 수와 유사한가
- Cursor run에서 `count(*) from point_history`가 관측되지 않는가
- amplified hot user에서 count query mean/total time이 무시하기 어려운 수준인가
- count query 비용을 page depth 비용과 혼동하지 않고 별도 비용으로 설명했는가

## 실행 순서

권장 실행 순서:

```text
1. Offset API 정렬 기준을 Cursor와 동일하게 맞춘다.
2. amplified hot user 707000과 인덱스 상태를 확인한다.
3. A용 offset SQL-only EXPLAIN을 page sample 대표 지점에서 저장한다.
4. B용 cursor sample 값을 생성해 JSON preset으로 저장한다.
5. B용 cursor SQL-only EXPLAIN을 cursor lookup 없이 저장한다.
6. pg_stat_statements_reset() 후 A k6 offset sampling을 실행한다.
7. A run의 k6 summary와 pg_stat_statements snapshot을 저장한다.
8. pg_stat_statements_reset() 후 B k6 cursor sampling을 실행한다.
9. B run의 k6 summary와 pg_stat_statements snapshot을 저장한다.
10. C count-only EXPLAIN을 저장한다.
11. report.md는 A/B/C 구분으로 다시 작성한다.
12. 같은 Docker volume으로 다른 Phase를 진행해야 하면 Phase 7 cleanup SQL을 실행하고 삭제 결과를 확인한다.
```

반복 실행이 필요하면 A와 B의 k6 sampling을 최소 3회 실행하고, run 순서를 바꾼다.

```text
run set 1: A offset sampling -> B cursor sampling
run set 2: B cursor sampling -> A offset sampling
run set 3: A offset sampling -> B cursor sampling
```

보고서에는 단일 p95보다 median p95를 우선 사용한다.

## Evidence 저장 계획

새 evidence는 기존 Phase 7 evidence와 구분되도록 `retest` 또는 `sampling` 이름을 포함한다.

```text
docs/evidence/phase-07/
  data-profile/
    retest-cursor-samples.txt
    retest-cursor-samples.json
  explain/
    retest-offset-page0.txt
    retest-offset-page1000.txt
    retest-offset-page4999.txt
    retest-cursor-page10.txt
    retest-cursor-page1000.txt
    retest-cursor-page4999.txt
    retest-count-only.txt
  k6/
    retest-offset-sampling-summary.txt
    retest-cursor-sampling-summary.txt
  pg-stat-statements/
    retest-offset-sampling-api.txt
    retest-cursor-sampling-api.txt
```

`docs/evidence/phase-07/README.md`에는 기존 natural hot user run, amplified hot user run, retest sampling run을 분리해서 기록한다.

## Grafana Evidence

Phase 7 재측정을 위해 별도 Grafana dashboard나 Phase 7 전용 panel을 추가하지 않는다. 기존 shared `DB Lab Overview`를 그대로 사용한다.

Grafana는 primary evidence가 아니라 보조 evidence다. Phase 7의 판정 기준은 아래 파일 evidence를 우선한다.

```text
Offset/Page skip 비용: EXPLAIN (ANALYZE, BUFFERS)
Cursor next-slice 비용: cursor lookup을 제외한 EXPLAIN과 k6 sampling summary
Count query 비용: pg_stat_statements snapshot과 count-only EXPLAIN
```

Grafana capture는 실행 당시 runtime 상태를 보조 확인하기 위해 남긴다.

캡처 대상:

- Run Summary
- k6 Load
- Hikari Pool
- Table Access에서 `point_history`

보고서에는 아래 해석 조건을 명시한다.

```text
Grafana screenshot은 기존 shared DB Lab Overview를 캡처한 보조 evidence다.
Phase 7 재측정을 위해 별도 dashboard나 panel은 추가하지 않았다.
Offset/Page의 skip 비용은 EXPLAIN으로, count query 비용은 pg_stat_statements로 판정한다.
Grafana는 k6 run 중 request rate, failure rate, Hikari 상태, table access 상태를 함께 확인하기 위한 보조 자료로 사용한다.
```

## Report 작성 구조

`docs/phases/07-pagination/report.md`는 아래 구조로 재정리한다.

```text
1. Summary
2. Measurement Condition
3. Why Page and Cursor Are Not the Same UX
4. A. Offset/Page Depth Result
5. B. Cursor Next-Slice Result
6. C. Count Query Result
7. Interpretation
8. Limitations
9. Phase 8 Handoff
```

`Interpretation`에는 아래 내용을 포함한다.

- Offset/Page는 page 번호 접근을 제공하지만 deep offset과 count query 비용을 부담한다.
- Cursor는 page 번호 접근을 제공하지 않지만, 이미 cursor를 가진 순차 탐색에서는 next slice를 count 없이 조회한다.
- Cursor deep sampling은 cursor source lookup을 포함하지 않는다.
- k6 결과는 warm-cache 반복 부하 결과이며 cold read 비용으로 일반화하지 않는다.
- `pg_stat_statements_reset()`은 통계 초기화일 뿐 cache 초기화가 아니다.

## 완료 조건

- [ ] Offset API가 Cursor API와 같은 정렬 기준을 사용한다.
- [ ] A offset sampling k6 summary에 page sample별 Trend p95가 기록됐다.
- [ ] B cursor samples가 사전 계산되어 evidence에 저장됐다.
- [ ] B cursor sampling k6 summary에 page sample별 Trend p95가 기록됐다.
- [ ] Cursor sampling run은 iteration당 하나의 cursor request만 보낸다.
- [ ] Cursor SQL-only EXPLAIN은 CTE 내부 `OFFSET` cursor lookup을 포함하지 않는다.
- [ ] Offset/Page run과 Cursor run의 `pg_stat_statements` snapshot이 분리 저장됐다.
- [ ] count-only EXPLAIN evidence가 저장됐다.
- [ ] 기존 shared `DB Lab Overview` Grafana screenshot이 보조 evidence로 저장됐다.
- [ ] 보고서가 A/B/C 구분과 Page-vs-Cursor trade-off 중심으로 작성됐다.
- [ ] long-lived Docker volume을 계속 사용할 때 실행할 Phase 7 cleanup SQL과 검증 절차가 문서화됐다.
