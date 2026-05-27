# Phase 7 Evidence

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

## Result Summary

| 비교 | 결과 |
|---|---|
| global offset deep vs cursor deep | Offset deep `220.430 ms`, Cursor deep `199.605 ms`. Cursor SQL script includes the cursor-source lookup, so API evidence is the primary cursor comparison. |
| user offset page0 vs deep | Offset page0 `0.175 ms`, Offset deep `6.889 ms`. |
| user offset deep vs cursor deep | Offset deep `6.889 ms`, Cursor deep `3.430 ms`. |
| offset deep vs cursor API p95 | Offset deep p95 `11.77 ms`, Cursor p95 `5.92 ms`. |
| Offset/Page count query | `offset-page-api.txt` recorded `45,003` count calls, mean `0.31 ms`, total `14,032.30 ms`. |
| Cursor count query | `cursor-api.txt` recorded no `count(*) from point_history` query. |
| Grafana screenshot | Not captured in this run. Primary evidence is k6 stdout, `EXPLAIN`, and `pg_stat_statements`; Docker k6 was run without Prometheus remote write. |

## Amplified Hot User Result Summary

| 비교 | 결과 |
|---|---|
| amplified user | `user_id=707000`, `point_count=100000`, `midPage=2499`, `deepPage=3999`, `deepOffset=79980` |
| amplified SQL-only offset page0 vs deep | Offset page0 `0.097 ms`, Offset deep `33.426 ms` |
| amplified SQL-only cursor deep | Cursor deep `36.383 ms`, but this includes cursor-source lookup via deep offset and is not meaningful Cursor API evidence |
| amplified API p95 | Offset page0 `13.93 ms`, Offset mid `85.31 ms`, Offset deep `38.68 ms`, Cursor `4.95 ms` |
| amplified Offset/Page count query | `amplified-offset-page-api.txt` recorded `45,003` count calls, mean `8.90 ms`, total `400,662.47 ms` |
| amplified Cursor count query | `amplified-cursor-api.txt` recorded no `count(*) from point_history` query |

## Evidence Index

| 종류 | 파일 | 설명 |
|---|---|---|
| data profile | [data-profile/point-history-total-count.txt](./data-profile/point-history-total-count.txt) | 전체 `point_history` row 수 |
| data profile | [data-profile/hot-user-point-counts.txt](./data-profile/hot-user-point-counts.txt) | hot user 후보 |
| data profile | [data-profile/selected-user-and-pages.txt](./data-profile/selected-user-and-pages.txt) | 선정 user와 page 계산 |
| data profile | [data-profile/amplified-user-and-pages.txt](./data-profile/amplified-user-and-pages.txt) | Phase 7 전용 가상 hot user와 page 계산 |
| explain | [explain/global-offset-page0.txt](./explain/global-offset-page0.txt) | 전체 테이블 Offset shallow |
| explain | [explain/global-offset-deep.txt](./explain/global-offset-deep.txt) | 전체 테이블 Offset deep |
| explain | [explain/global-cursor-deep.txt](./explain/global-cursor-deep.txt) | 전체 테이블 Cursor deep |
| explain | [explain/user-offset-page0.txt](./explain/user-offset-page0.txt) | hot user Offset shallow |
| explain | [explain/user-offset-deep.txt](./explain/user-offset-deep.txt) | hot user Offset deep |
| explain | [explain/user-cursor-deep.txt](./explain/user-cursor-deep.txt) | hot user Cursor deep |
| explain | [explain/amplified-user-offset-page0.txt](./explain/amplified-user-offset-page0.txt) | amplified hot user Offset shallow |
| explain | [explain/amplified-user-offset-deep.txt](./explain/amplified-user-offset-deep.txt) | amplified hot user Offset deep |
| explain | [explain/amplified-user-cursor-deep.txt](./explain/amplified-user-cursor-deep.txt) | amplified hot user Cursor deep with cursor-source lookup |
| k6 | [k6/offset-page0-summary.txt](./k6/offset-page0-summary.txt) | Offset page0 p95 |
| k6 | [k6/offset-mid-summary.txt](./k6/offset-mid-summary.txt) | Offset mid p95 |
| k6 | [k6/offset-deep-summary.txt](./k6/offset-deep-summary.txt) | Offset deep p95 |
| k6 | [k6/cursor-summary.txt](./k6/cursor-summary.txt) | Cursor p95 |
| k6 | [k6/amplified-offset-page0-summary.txt](./k6/amplified-offset-page0-summary.txt) | amplified Offset page0 p95 |
| k6 | [k6/amplified-offset-mid-summary.txt](./k6/amplified-offset-mid-summary.txt) | amplified Offset mid p95 |
| k6 | [k6/amplified-offset-deep-summary.txt](./k6/amplified-offset-deep-summary.txt) | amplified Offset deep p95 |
| k6 | [k6/amplified-cursor-summary.txt](./k6/amplified-cursor-summary.txt) | amplified Cursor p95 |
| pg_stat_statements | [pg-stat-statements/offset-page-api.txt](./pg-stat-statements/offset-page-api.txt) | Offset/Page SQL snapshot |
| pg_stat_statements | [pg-stat-statements/cursor-api.txt](./pg-stat-statements/cursor-api.txt) | Cursor SQL snapshot |
| pg_stat_statements | [pg-stat-statements/amplified-offset-page-api.txt](./pg-stat-statements/amplified-offset-page-api.txt) | amplified Offset/Page SQL snapshot |
| pg_stat_statements | [pg-stat-statements/amplified-cursor-api.txt](./pg-stat-statements/amplified-cursor-api.txt) | amplified Cursor SQL snapshot |
| grafana | - | Not captured in this run |
