# Phase 7 보고서

## 1. 요약

Phase 7 재측정은 Offset/Page와 Cursor를 단순 속도 경쟁으로 비교하지 않는다.

Offset/Page는 번호 기반 임의 페이지 접근과 전체 개수 기반 UI를 제공하지만, 깊은 페이지로 갈수록 앞 row를 지나가는 비용과 `Page<T>`의 count query 비용을 부담한다. Cursor는 번호 기반 페이지 점프를 제공하지 않는다. 대신 클라이언트가 이전 응답에서 받은 cursor를 이미 가지고 있을 때, count query 없이 다음 slice를 조회한다.

이번 재측정은 `point_history` hot user `707000` 조건에서 이 trade-off를 A/B/C 증거로 분리해 확인했다.

- A. Offset/Page depth: SQL-only 증거에서 깊은 offset일수록 더 많은 row를 읽거나 지나가는 것을 확인했다.
- B. Cursor next-slice: cursor 값을 사전 계산해 API 측정에서 cursor-source `OFFSET` lookup을 제외했다.
- C. Count query: Offset/Page에서는 count query가 발생했고 Cursor에서는 발생하지 않았다.

## 2. 측정 조건

| 항목 | 값 |
|---|---|
| table | `point_history` |
| hot user | `707000` |
| point count | `100000` |
| size | `20` |
| max page | `4999` |
| logical order | `created_at DESC, id DESC` |
| index | `(user_id, created_at DESC, id DESC)` 기준 `idx_point_history_user_created_id` |
| API 실행 환경 | local Spring Boot on `localhost:8080` |
| k6 실행 환경 | Docker compose `grafana/k6` |
| k6 load | `50 rps`, `5m`, `100` pre-allocated VUs, `300` max VUs |
| cache 조건 | warm-cache 반복 부하 |

`pg_stat_statements_reset()`은 run별 SQL 통계를 분리하기 위해서만 사용했다. PostgreSQL shared buffers, OS page cache, JVM 상태, connection pool 상태를 초기화하지 않았다.

## 3. Page와 Cursor가 같은 UX가 아닌 이유

Offset/Page는 "N번째 페이지를 보여줘"라는 요구를 처리하고, total elements와 total pages를 제공할 수 있다. 번호 기반 navigation에는 유리하지만, 깊은 페이지에서는 앞 row를 skip해야 하고 `Page<T>`가 count query 비용을 추가한다.

Cursor는 "이 마지막 row 다음 slice를 보여줘"라는 요구를 처리한다. 순차 탐색과 infinite scroll에는 적합하지만, 임의 page 번호 점프를 대체하지 않는다. Cursor가 page `4999`로 직접 점프한다고 가정해 측정하면 일반적인 cursor navigation에 없는 cursor source lookup 비용이 섞인다.

따라서 이번 재측정에서는 cursor sample을 준비 단계에서 사전 계산했고, Cursor API latency에는 그 lookup 비용을 포함하지 않았다.

## 4. A. Offset/Page Depth 결과

### 측정 조건

Offset SQL과 API는 아래 정렬 기준을 사용했다.

```sql
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET ...
```

k6 sampling run은 warm-cache 조건에서 `[0,10,50,100,500,1000,2000,3000,4000,4999]` page 중 하나를 매 iteration마다 무작위 선택했다.

### 핵심 수치

| 증거 | Page | Offset | 결과 |
|---|---:|---:|---|
| SQL-only | `0` | `0` | 실행 시간 `0.122 ms`; index scan rows `20` |
| SQL-only | `1000` | `20000` | 실행 시간 `2.841 ms`; index scan rows `20020` |
| SQL-only | `4999` | `99980` | 실행 시간 `49.515 ms`; rows `100000`, external sort 관측 |
| k6 p95 | `0` | `0` | `16.99 ms` |
| k6 p95 | `1000` | `20000` | `19.45 ms` |
| k6 p95 | `4999` | `99980` | `31.84 ms` |
| k6 run | 전체 sample | mixed | `15000` 요청, 실패율 `0.10%`, threshold `<5%` 통과 |

### 원본 증거

- [retest-offset-sampling.txt](../../evidence/phase-07/explain/retest-offset-sampling.txt)
- [retest-offset-sampling-summary.txt](../../evidence/phase-07/k6/retest-offset-sampling-summary.txt)
- [retest-offset-sampling-api.txt](../../evidence/phase-07/pg-stat-statements/retest-offset-sampling-api.txt)

### 해석

SQL-only 증거는 offset skip 비용을 직접 보여준다. page `1000`은 `20020` rows를 지나갔고, page `4999`는 `100000` rows를 처리했다. k6 sampling에서도 깊은 page bucket이 얕은 bucket보다 불리해지는 경향이 보였다.

### 한계

k6 run은 warm-cache 반복 부하이며 cold read 증거가 아니다. 또한 `15000` HTTP 요청 중 `15`개가 실패했으므로 API summary에는 약간의 noise가 섞여 있다. page depth skip 동작의 primary 증거는 SQL-only EXPLAIN이다.

## 5. B. Cursor Next-Slice 결과

### 측정 조건

Cursor sample은 Offset과 같은 logical page set 기준으로 사전 계산했다. page `0`은 cursor 없이 요청한다. page `> 0`은 이전 logical row의 `lastCreatedAt`, `lastId`를 요청에 넣는다.

Cursor sample lookup은 측정 준비 작업이다. API latency에는 포함하지 않았다.

### 핵심 수치

| 증거 | Logical page | Cursor | 결과 |
|---|---:|---|---|
| SQL-only | `10` | `2026-05-26T23:56:40`, `707000200` | 실행 시간 `0.122 ms` |
| SQL-only | `1000` | `2026-05-26T18:26:40`, `707020000` | 실행 시간 `0.117 ms` |
| SQL-only | `4999` | `2026-05-25T20:13:40`, `707099980` | 실행 시간 `0.158 ms` |
| k6 p95 | `0` | 없음 | `11.27 ms` |
| k6 p95 | `1000` | 사전 계산 | `16.46 ms` |
| k6 p95 | `4999` | 사전 계산 | `31.93 ms` |
| k6 run | 전체 sample | mixed | `15001` 요청, 실패율 `0.00%` |

### 원본 증거

- [retest-cursor-samples.txt](../../evidence/phase-07/data-profile/retest-cursor-samples.txt)
- [retest-cursor-samples.json](../../evidence/phase-07/data-profile/retest-cursor-samples.json)
- [retest-cursor-page10.txt](../../evidence/phase-07/explain/retest-cursor-page10.txt)
- [retest-cursor-page1000.txt](../../evidence/phase-07/explain/retest-cursor-page1000.txt)
- [retest-cursor-page4999.txt](../../evidence/phase-07/explain/retest-cursor-page4999.txt)
- [retest-cursor-sampling-summary.txt](../../evidence/phase-07/k6/retest-cursor-sampling-summary.txt)
- [retest-cursor-sampling-api.txt](../../evidence/phase-07/pg-stat-statements/retest-cursor-sampling-api.txt)

### 해석

Cursor SQL-only script에는 CTE `OFFSET` source lookup이 없다. 아래 next-slice query 자체만 측정한다.

```sql
WHERE user_id = 707000
  AND (created_at, id) < (:last_created_at, :last_id)
ORDER BY created_at DESC, id DESC
LIMIT 20
```

page `10`, `1000`, `4999`에서 SQL 실행 시간은 약 `0.1 ms` 수준을 유지했다. 이는 row-value predicate 형태의 Cursor next-slice 조회가 page 번호 skip이 아니라, 제공된 cursor key와 `LIMIT 20` 중심으로 실행될 수 있다는 primary 증거다.

단, 이 SQL-only 증거는 실제 API가 생성한 Hibernate SQL과 완전히 같은 shape가 아니다. SQL-only script는 `(created_at, id) < (...)` 형태를 사용하지만, 현재 JPQL 기반 Cursor API는 아래와 같은 OR 조건으로 SQL을 생성한다.

```sql
WHERE user_id = ?
  AND (
    created_at < ?
    OR (created_at = ? AND id < ?)
  )
ORDER BY created_at DESC, id DESC
FETCH FIRST ? ROWS ONLY
```

추가 확인 결과, page `4999` cursor 값으로 실제 API SQL shape를 EXPLAIN하면 `Index Cond`가 `user_id`에만 걸리고 cursor 조건은 `Filter`로 처리됐다. 이때 `Rows Removed by Filter: 99980`, 실행 시간 `70.718 ms`가 관측됐다. 따라서 현재 k6에서 깊은 Cursor bucket의 p95가 상승한 원인은 cursor-source lookup이 아니라, 실제 API SQL shape가 복합 인덱스 seek로 최적화되지 않는 문제로 보는 것이 더 타당하다.

### 한계

k6 cursor p95는 깊은 sample bucket에서 여전히 상승했다. 이를 cursor-source lookup 비용으로 해석하면 안 된다. source lookup은 요청에 포함되지 않았기 때문이다. 현재 확인된 가장 중요한 원인은 SQL-only script와 실제 API SQL shape 차이다. 실제 API SQL은 `created_at < ? OR (created_at = ? AND id < ?)` 조건이 `Filter`로 처리될 수 있고, 깊은 cursor에서는 앞 row를 대량으로 버릴 수 있다.

## 6. C. Count Query 결과

### 측정 조건

Offset/Page는 Spring Data `Page<T>`를 사용하므로 각 page 요청에서 data query와 count query가 함께 실행될 수 있다. Cursor는 `size + 1` row로 `hasNext`를 판단하므로 total count가 필요 없다.

### 핵심 수치

| 실행 | Query shape | 호출 수 | 평균 | 총합 |
|---|---|---:|---:|---:|
| Offset/Page | `select count(*) from point_history ...` | `14985` | `7.04 ms` | `105490.65 ms` |
| Offset/Page | `offset ... fetch first ...`를 포함한 정렬 data query | `13468` | `4.50 ms` | `60608.89 ms` |
| Offset/Page | offset 없는 first-page 정렬 data query | `1517` | `0.04 ms` | `60.71 ms` |
| Cursor | next-slice data query | `13532` | `4.54 ms` | `61473.24 ms` |
| Cursor | first cursor page query | `1469` | `0.06 ms` | `86.42 ms` |
| Count-only SQL | `count(*) where user_id = 707000` | n/a | n/a | 실행 시간 `11.195 ms` |

### 원본 증거

- [retest-offset-sampling-api.txt](../../evidence/phase-07/pg-stat-statements/retest-offset-sampling-api.txt)
- [retest-cursor-sampling-api.txt](../../evidence/phase-07/pg-stat-statements/retest-cursor-sampling-api.txt)
- [retest-count-only.txt](../../evidence/phase-07/explain/retest-count-only.txt)

### 해석

Count query 비용은 page depth skip 비용과 별도 비용이다. Offset/Page는 둘 다 부담한다. data query는 skip 비용을 낼 수 있고, `Page<T>`는 total elements 계산을 위해 count 비용도 낸다. Cursor는 API path에서 count query를 제거한다.

이번 재측정에서 Cursor `pg_stat_statements`에는 `select count(*) from point_history` query가 없었다.

### 한계

Offset/Page count query calls는 HTTP 요청 수보다 약간 적었다. `pg_stat_statements`가 normalized SQL shape로 묶고, run에 소량의 실패 요청이 포함됐기 때문이다. Count-only EXPLAIN은 count 자체의 DB 비용을 분리한 증거이며, 전체 API latency와 같은 값이 아니다.

## 7. 종합 해석

Page와 Cursor는 다른 UX contract다.

Page는 임의 page 번호 접근과 total count 기반 UI가 필요할 때 적합하다. 큰 hot user 조건에서는 deep offset skip 비용과 count query 비용을 부담한다.

Cursor는 순차 탐색에 적합하다. 임의 page 번호 점프를 포기하는 대신 cursor key를 사용해 다음 slice를 count 없이 조회할 수 있다.

이번 Phase 7 재측정은 이전 cursor-source lookup SQL보다 이 trade-off를 더 명확히 보여준다. Cursor next-slice EXPLAIN에서 deep `OFFSET` lookup을 제거했기 때문이다. 다만 현재 API 구현은 JPQL OR 조건 때문에 SQL-only의 row-value predicate 실행계획과 달라질 수 있다. 따라서 현재 k6에서 Cursor와 Offset의 p95 차이가 작게 나온 것은 Cursor pagination 개념의 한계라기보다, API SQL shape가 아직 최적화되지 않은 구현 한계로 분류해야 한다.

Grafana 스크린샷은 보조 증거다. Phase 7 재측정을 위해 새 dashboard나 Phase 7 전용 panel을 추가하지 않았다. Offset/Page skip 비용은 EXPLAIN으로, count query 비용은 `pg_stat_statements`로 판정한다. Grafana는 run 중 request rate, failure rate, Hikari 상태, table access 상태를 함께 확인하기 위한 보조 자료다.

증거:

- [retest-db-lab-overview.png](../../evidence/phase-07/grafana/retest-db-lab-overview.png)

## 8. 개선 수정 필요 사항

### Cursor API SQL shape 개선

현재 `PointHistoryRepository.findNextCursorPage`는 JPQL로 cursor 조건을 표현한다. Hibernate는 이를 `created_at < ? OR (created_at = ? AND id < ?)` 형태로 생성하며, PostgreSQL에서 복합 인덱스 `(user_id, created_at DESC, id DESC)`의 cursor 범위 조건으로 충분히 밀어 넣지 못할 수 있다.

개선 방향은 Cursor next-slice 조회를 native query로 분리해 실제 SQL이 row-value predicate를 사용하도록 고정하는 것이다.

```sql
WHERE user_id = ?
  AND (created_at, id) < (?, ?)
ORDER BY created_at DESC, id DESC
LIMIT ?
```

수정 후에는 다음을 다시 수집해야 한다.

- 실제 API SQL shape의 `EXPLAIN (ANALYZE, BUFFERS)`
- page `1000`, `4999`에서 `Rows Removed by Filter`가 사라지거나 크게 줄었는지 여부
- Offset/Page와 Cursor k6 sampling 재실행 결과
- `pg_stat_statements`에서 Cursor next-slice query 평균 시간이 감소했는지 여부

이 수정이 끝나기 전까지 현재 k6 결과는 "Cursor가 Offset보다 근본적으로 빠르지 않다"는 결론의 근거로 사용하면 안 된다. 현재 결과는 "Cursor API에서 count query는 제거됐지만, 깊은 cursor에서 실제 SQL shape가 seek로 최적화되지 않았다"는 구현 개선 근거로 해석해야 한다.

### Offset/Page 측정 분리

Offset/Page API는 `Page<T>`를 사용하므로 data query와 count query가 함께 섞인다. Offset skip 비용만 비교하려면 `Page<T>` 기반 API와 별도로 `Slice<T>` 또는 `List` 기반 offset 측정 API를 준비해 count query를 제거한 조건을 추가해야 한다.

### k6 측정 보정

Offset k6 custom trend에는 실패 응답의 duration이 page bucket trend에 들어갈 수 있다. 다음 재측정에서는 `res.status === 200`일 때만 page별 custom trend에 값을 추가하고, 실패 응답은 별도 counter로 분리해야 한다.

## 9. 한계

- 이번 결과는 warm-cache 반복 부하이며 cold read 동작이 아니다.
- `pg_stat_statements_reset()`은 run 통계를 분리했을 뿐 cache를 초기화하지 않았다.
- Offset k6에는 실패율 `0.10%`가 있었지만 설정된 failure threshold는 통과했다.
- Cursor sample 값은 사전 계산했다. 이는 next-slice 측정을 위한 의도적 조건이며, 이 증거는 직접 page jump 비용을 측정하지 않는다.
- Cursor SQL-only script는 row-value predicate를 사용하지만, 현재 API SQL은 JPQL OR 조건으로 생성된다. 이 차이 때문에 SQL-only Cursor 결과와 k6 Cursor 결과를 직접 연결하면 안 된다.
- k6 sampling run은 전략별 1회만 수행했다. 더 엄밀한 latency 결론을 내려면 Offset과 Cursor sampling을 최소 3회 반복하고, 실행 순서를 바꾼 뒤 median p95를 비교해야 한다.
- 기존 natural hot user와 amplified one-page run은 기존 증거로 남긴다. 최종 Phase 7 해석의 primary 증거는 retest A/B/C artifact다.

## 10. Phase 8 인계

Phase 8에서는 HTTP p95, Hikari pending/active 상태, `pg_stat_activity`, `pg_stat_statements`를 하나의 병목 관측 흐름으로 연결한다.

Phase 7에서 넘길 관측 기준은 아래와 같다.

- SQL shape 증거와 API latency를 분리한다.
- Grafana는 query cost의 주요 증명 자료가 아니라 runtime context로 본다.
- Count query 비용과 data query skip 비용을 분리한다.
- p95를 비교하기 전에 cache 조건과 run order를 명시한다.
- Cursor API는 native query 또는 동등한 SQL shape 개선 후 다시 측정한다.
