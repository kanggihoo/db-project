# Phase 7 Report

## Summary

Phase 7은 `point_history`에서 Offset deep page 비용과 Cursor pagination 개선 효과를 확인했다.

핵심 결과:

- hot user `374`의 **Point History** `1912`건을 기준으로 API/k6를 실행했다.
- Offset API는 `Page<T>`를 유지하므로 data query 외에 count query가 발생했다.
- Cursor API는 `size + 1` 조회로 `hasNext`를 판단하며 count query를 발생시키지 않았다.
- k6 p95는 Offset deep `11.77 ms`, Cursor `5.92 ms`로 Cursor가 더 낮았다.
- global SQL-only Cursor 결과는 cursor 기준값을 얻기 위해 내부에서 `OFFSET 100000`을 먼저 수행하므로, Cursor pagination 자체의 유의미한 개선 evidence로 보지 않는다.

## Measurement Condition

- seed preset: `loadtest`
- table: `point_history`
- selected user: `374`
- selected user point count: `1912`
- size: `20`
- maxPage: `95`
- midPage: `47`
- deepPage: `76`
- deepOffset: `1520`
- API runtime: local Spring Boot on `localhost:8080`
- k6 runtime: Docker compose `grafana/k6`
- k6 load: `50 rps`, `5m`, `100` pre-allocated VUs, `300` max VUs

## SQL-only Result

| 실험 | evidence | 주요 결과 |
|---|---|---|
| global offset page0 | [global-offset-page0.txt](../../evidence/phase-07/explain/global-offset-page0.txt) | Execution Time `0.127 ms` |
| global offset deep | [global-offset-deep.txt](../../evidence/phase-07/explain/global-offset-deep.txt) | Execution Time `220.430 ms` |
| global cursor deep | [global-cursor-deep.txt](../../evidence/phase-07/explain/global-cursor-deep.txt) | Execution Time `199.605 ms`; cursor-source lookup 포함 |
| user offset page0 | [user-offset-page0.txt](../../evidence/phase-07/explain/user-offset-page0.txt) | Execution Time `0.175 ms` |
| user offset deep | [user-offset-deep.txt](../../evidence/phase-07/explain/user-offset-deep.txt) | Execution Time `6.889 ms` |
| user cursor deep | [user-cursor-deep.txt](../../evidence/phase-07/explain/user-cursor-deep.txt) | Execution Time `3.430 ms`; cursor-source lookup 포함 |

## API/k6 Result

| 실험 | p95 | evidence |
|---|---:|---|
| offset page0 | `6.9 ms` | [offset-page0-summary.txt](../../evidence/phase-07/k6/offset-page0-summary.txt) |
| offset mid | `10.15 ms` | [offset-mid-summary.txt](../../evidence/phase-07/k6/offset-mid-summary.txt) |
| offset deep | `11.77 ms` | [offset-deep-summary.txt](../../evidence/phase-07/k6/offset-deep-summary.txt) |
| cursor | `5.92 ms` | [cursor-summary.txt](../../evidence/phase-07/k6/cursor-summary.txt) |

## COUNT Query Result

| 조건 | count query 호출 | evidence |
|---|---:|---|
| Offset/Page | `45,003` calls, mean `0.31 ms`, total `14,032.30 ms` | [offset-page-api.txt](../../evidence/phase-07/pg-stat-statements/offset-page-api.txt) |
| Cursor | `0` count calls observed | [cursor-api.txt](../../evidence/phase-07/pg-stat-statements/cursor-api.txt) |

## Interpretation

Offset deep page는 같은 user 범위에서도 더 많은 row를 skip하면서 page0보다 실행 시간이 증가했다. Cursor 방식은 `created_at DESC, id DESC` 순서를 기준으로 마지막 row 이후를 조회하므로 deep page 위치에서도 count query 없이 다음 slice를 가져올 수 있다.

Global cursor SQL-only 결과는 cursor 값을 찾기 위한 source lookup을 같은 SQL에 포함한다. `13-global-cursor-deep-explain.sql`은 deep 위치의 `created_at`, `id`를 알 수 없는 상태에서 비교를 만들기 위해 먼저 `OFFSET 100000`으로 cursor source row를 찾고, 그 다음 cursor 조건으로 다음 rows를 조회한다. 이 방식은 실제 Cursor API처럼 client가 이전 응답의 `lastCreatedAt`, `lastId`를 넘기는 흐름이 아니므로 Offset 비용이 결과에 섞인다.

따라서 global Offset deep `220.430 ms`와 global Cursor deep `199.605 ms`의 차이는 작고, Cursor pagination 개선 효과를 입증하는 유의미한 결과로 해석하지 않는다. 이 값은 오히려 “cursor 기준값을 deep offset으로 다시 찾으면 Cursor 방식의 이점이 거의 사라진다”는 한계 evidence로 본다. Cursor API의 primary evidence는 hot user API/k6 p95와 `pg_stat_statements`의 count query 제거 여부다.

Grafana screenshot은 이번 run에서 남기지 않았다. k6는 stdout summary 중심으로 Docker compose `k6` 컨테이너에서 실행했고 Prometheus remote write를 사용하지 않았다. 이후 Grafana evidence가 필요하면 같은 preset을 `prometheus` 모드로 재실행해 shared `DB Lab Overview`를 캡처한다.

## Amplified Hot User Follow-up

자연 발생 hot user `374`는 `point_history`가 `1912`건이라 deep page 차이가 작게 나타날 수 있다. Phase 7의 pagination contrast를 더 명확히 보기 위해 기존 `loadtest` seed는 유지하고, Phase 7 전용 가상 hot user를 추가한다.

추가 계획:

- plan: [006-hot-user-amplification.md](../../superpowers/plans/phase-07-pagination/006-hot-user-amplification.md)
- SQL: `scripts/phase-07/05-hot-user-amplify.sql`
- user_id: `707000`
- target point count: `100000`
- size: `20`
- maxPage: `4999`
- midPage: `2499`
- deepPage: `3999`
- deepOffset: `79980`

이 follow-up run은 기존 seed 분포를 바꾸지 않고 `point_history` 단일 user의 deep offset 비용만 확대한다. 따라서 Phase 7의 최종 pagination 판단은 natural hot user run과 amplified hot user run을 분리해서 기록한다.

### Amplified SQL-only Result

| 실험 | evidence | 주요 결과 |
|---|---|---|
| amplified user offset page0 | [amplified-user-offset-page0.txt](../../evidence/phase-07/explain/amplified-user-offset-page0.txt) | Execution Time `0.097 ms` |
| amplified user offset deep | [amplified-user-offset-deep.txt](../../evidence/phase-07/explain/amplified-user-offset-deep.txt) | Execution Time `33.426 ms` |
| amplified user cursor deep | [amplified-user-cursor-deep.txt](../../evidence/phase-07/explain/amplified-user-cursor-deep.txt) | Execution Time `36.383 ms`; cursor-source lookup 포함 |

SQL-only cursor deep은 이번에도 cursor 기준값을 얻기 위해 `OFFSET 79980`으로 source row를 먼저 찾는다. 따라서 `33.426 ms` vs `36.383 ms`는 Cursor API 개선 효과를 보여주는 결과가 아니다. 이 결과는 cursor 기준값을 offset으로 구하면 Cursor 방식의 장점이 사라진다는 한계를 다시 확인한다.

### Amplified API/k6 Result

| 실험 | p95 | evidence |
|---|---:|---|
| amplified offset page0 | `13.93 ms` | [amplified-offset-page0-summary.txt](../../evidence/phase-07/k6/amplified-offset-page0-summary.txt) |
| amplified offset mid | `85.31 ms` | [amplified-offset-mid-summary.txt](../../evidence/phase-07/k6/amplified-offset-mid-summary.txt) |
| amplified offset deep | `38.68 ms` | [amplified-offset-deep-summary.txt](../../evidence/phase-07/k6/amplified-offset-deep-summary.txt) |
| amplified cursor | `4.95 ms` | [amplified-cursor-summary.txt](../../evidence/phase-07/k6/amplified-cursor-summary.txt) |

Amplified API run에서는 Cursor p95가 Offset page0/mid/deep보다 모두 낮았다. Offset mid p95가 deep보다 높게 나온 것은 Offset pagination의 이론적 특성과 맞지 않으므로, page depth 자체의 단조 증가 evidence로 해석하지 않는다. 두 Offset deep-page 계열 모두 Cursor보다 현저히 높다는 점만 이번 run의 안정적인 결론으로 둔다.

#### Why Offset mid was slower than Offset deep

Offset pagination은 같은 조건에서 `OFFSET` 값이 커질수록 더 많은 row를 skip하므로 일반적으로 뒤 페이지가 더 비싸진다. 그런데 이번 amplified k6 run에서는 mid page `2499`의 p95가 `85.31 ms`, deep page `3999`의 p95가 `38.68 ms`로 mid가 더 느렸다.

가능한 원인:

- 순차 실행에 따른 PostgreSQL buffer cache와 OS page cache 차이: page0, mid, deep 순서로 실행했기 때문에 deep run 시점에는 `point_history` index/table page가 더 많이 cache에 올라와 있었을 수 있다.
- `Page<T>` count query 영향: Offset API는 page depth와 무관하게 매 요청마다 `count(*) from point_history where user_id = ?`를 실행한다. amplified Offset run에서 count query 총 실행 시간은 `400,662.47 ms`였고, 이 비용이 p95에 섞여 page offset 비용만 분리해 보여주지 않는다.
- JVM, GC, Docker scheduling, connection pool 상태 차이: 각 preset을 별도 5분 run으로 순차 실행했기 때문에 동일한 runtime condition으로 보기 어렵다.
- tail latency 민감도: p95는 일시적인 stall에 민감하다. mid run의 max latency는 `3.12s`, deep run의 max latency는 `2s`였으므로 mid run에 더 큰 spike가 있었다.

따라서 이번 amplified k6 결과는 “Offset page가 깊어질수록 항상 더 느리다”는 단조 증가 증거가 아니라, “큰 hot user 조건에서 Offset/Page 계열은 Cursor보다 p95와 count query 비용이 크게 불리하다”는 증거로 해석한다.

#### Recommended rerun strategy

page depth 비용을 더 엄밀히 확인하려면 아래 방식으로 재시도한다.

- Offset `page0`, `mid`, `deep`, Cursor를 최소 3회 반복 실행하고 median p95를 비교한다.
- 실행 순서를 바꾼다. 예: `deep -> mid -> page0 -> cursor`, `cursor -> page0 -> deep -> mid`.
- 각 run 전 `pg_stat_statements_reset()`을 실행하고, run별 `pg_stat_statements` snapshot을 분리 저장한다.
- Offset/Page의 count query 비용과 data query 비용을 따로 해석한다.
- 필요하면 count 없는 Offset baseline, 예를 들어 `Slice` 또는 `List` 기반 Offset API를 추가해 page depth skip 비용만 Cursor와 비교한다.
- cache warm/cold 조건을 명시한다. 운영 재현보다 원리 확인이 목적이면 warm-up run을 버리고 두 번째 run부터 evidence로 사용한다.

### Amplified COUNT Query Result

| 조건 | count query 호출 | evidence |
|---|---:|---|
| amplified Offset/Page | `45,003` calls, mean `8.90 ms`, total `400,662.47 ms` | [amplified-offset-page-api.txt](../../evidence/phase-07/pg-stat-statements/amplified-offset-page-api.txt) |
| amplified Cursor | `0` count calls observed | [amplified-cursor-api.txt](../../evidence/phase-07/pg-stat-statements/amplified-cursor-api.txt) |

100,000건 hot user에서는 `Page<T>` count query 비용이 더 크게 드러났다. Offset/Page run의 count query 총 실행 시간은 `400,662.47 ms`로, Cursor API가 count query를 제거하는 효과가 natural hot user run보다 더 명확하다.

## Phase 8 Handoff

Phase 8에서는 HTTP p95, Hikari pending/active, `pg_stat_activity`, `pg_stat_statements`를 연결해 병목 관측 흐름을 정리한다.
