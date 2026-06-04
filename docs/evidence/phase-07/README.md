# Phase 7 증거

## 주요 측정 조건

| 항목 | 값 |
|---|---|
| table | `point_history` |
| retest hot user | `707000` |
| point count | `100000` |
| size | `20` |
| max page | `4999` |
| logical order | `created_at DESC, id DESC` |
| k6 load | `50 rps`, `5m`, `100` pre-allocated VUs, `300` max VUs |
| cache 조건 | warm-cache 반복 부하 |

## Fixture cleanup

Phase 7 retest hot user fixture는 long-lived Docker volume에서 다른 Phase로 이동할 때 제거할 수 있다. Evidence 캡처 전에는 cleanup을 실행하지 않는다.

- cleanup script: [scripts/phase-07/06-hot-user-cleanup.sql](../../../scripts/phase-07/06-hot-user-cleanup.sql)
- cleanup 절차와 검증 query: [Phase 7 runbook](../../phases/07-pagination/runbook.md#8-phase-7-hot-user-cleanup)
- 더 강한 격리가 필요하면 `docker compose down -v && docker compose up -d && ./scripts/seed.sh loadtest`로 새 loadtest seed를 만든다.

## Retest Sampling 결과 요약

| 비교 | 결과 |
|---|---|
| retest hot user profile | `user_id=707000`, `point_count=100000`, `max_page=4999` |
| retest cursor samples | 10개 sample page: `[0,10,50,100,500,1000,2000,3000,4000,4999]` |
| retest offset sampling EXPLAIN | page0 `0.122 ms`, page1000 `2.841 ms`, page4999 `49.515 ms` |
| retest cursor page10/page1000/page4999 EXPLAIN | page10 `0.122 ms`, page1000 `0.117 ms`, page4999 `0.158 ms`; cursor-source `OFFSET` lookup 없음 |
| retest count-only EXPLAIN | `count(*) where user_id=707000` 실행 시간 `11.195 ms` |
| retest offset sampling k6 | `15000` 요청, p95 page0 `16.99 ms`, page4999 `31.84 ms`, 실패율 `0.10%` |
| retest cursor sampling k6 | `15001` 요청, p95 page0 `11.27 ms`, page4999 `31.93 ms`, 실패율 `0.00%` |
| retest offset pg_stat_statements | count query `14985` calls, mean `7.04 ms`, total `105490.65 ms`; offset data query 관측 |
| retest cursor pg_stat_statements | next-slice query 관측; `count(*) from point_history` query 없음 |
| retest Grafana 스크린샷 | 기존 shared `DB Lab Overview`를 보조 증거로 캡처 |

## Retest 증거 목차

| 종류 | 파일 | 설명 |
|---|---|---|
| 데이터 profile | [data-profile/retest-hot-user-profile.txt](./data-profile/retest-hot-user-profile.txt) | retest hot user row count와 max page |
| 데이터 profile | [data-profile/retest-cursor-samples.txt](./data-profile/retest-cursor-samples.txt) | SQL로 생성한 cursor sample table |
| 데이터 profile | [data-profile/retest-cursor-samples.json](./data-profile/retest-cursor-samples.json) | k6 preset에 사용한 cursor sample |
| 실행계획 | [explain/retest-offset-sampling.txt](./explain/retest-offset-sampling.txt) | page0/page1000/page4999 Offset sampling EXPLAIN |
| 실행계획 | [explain/retest-cursor-page10.txt](./explain/retest-cursor-page10.txt) | logical page10 Cursor next-slice EXPLAIN |
| 실행계획 | [explain/retest-cursor-page1000.txt](./explain/retest-cursor-page1000.txt) | logical page1000 Cursor next-slice EXPLAIN |
| 실행계획 | [explain/retest-cursor-page4999.txt](./explain/retest-cursor-page4999.txt) | logical page4999 Cursor next-slice EXPLAIN |
| 실행계획 | [explain/retest-count-only.txt](./explain/retest-count-only.txt) | count-only EXPLAIN |
| 부하 테스트 | [k6/retest-offset-sampling-summary.txt](./k6/retest-offset-sampling-summary.txt) | page bucket별 Trend가 포함된 Offset sampling k6 summary |
| 부하 테스트 | [k6/retest-cursor-sampling-summary.txt](./k6/retest-cursor-sampling-summary.txt) | page bucket별 Trend가 포함된 Cursor sampling k6 summary |
| SQL 통계 | [pg-stat-statements/retest-offset-sampling-api.txt](./pg-stat-statements/retest-offset-sampling-api.txt) | Offset/Page API SQL 스냅샷 |
| SQL 통계 | [pg-stat-statements/retest-cursor-sampling-api.txt](./pg-stat-statements/retest-cursor-sampling-api.txt) | Cursor API SQL 스냅샷 |
| Grafana | [grafana/retest-db-lab-overview.png](./grafana/retest-db-lab-overview.png) | shared DB Lab Overview 보조 스크린샷 |

## 기존 증거 목차

아래 파일들은 Phase 7 기존 증거로 보존한다. 최종 보고서의 primary 증거는 위 A/B/C retest artifact다.

| 종류 | 파일 | 설명 |
|---|---|---|
| 데이터 profile | [data-profile/hot-user-point-counts.txt](./data-profile/hot-user-point-counts.txt) | natural hot user 후보 |
| 데이터 profile | [data-profile/selected-user-and-pages.txt](./data-profile/selected-user-and-pages.txt) | natural selected user와 page 계산 |
| 데이터 profile | [data-profile/amplified-user-and-pages.txt](./data-profile/amplified-user-and-pages.txt) | 기존 amplified hot user page 계산 |
| 실행계획 | [explain/global-offset-page0.txt](./explain/global-offset-page0.txt) | global Offset shallow |
| 실행계획 | [explain/global-offset-deep.txt](./explain/global-offset-deep.txt) | global Offset deep |
| 실행계획 | [explain/global-cursor-deep.txt](./explain/global-cursor-deep.txt) | cursor-source lookup이 포함된 global Cursor deep |
| 실행계획 | [explain/user-offset-page0.txt](./explain/user-offset-page0.txt) | natural hot user Offset shallow |
| 실행계획 | [explain/user-offset-deep.txt](./explain/user-offset-deep.txt) | natural hot user Offset deep |
| 실행계획 | [explain/user-cursor-deep.txt](./explain/user-cursor-deep.txt) | cursor-source lookup이 포함된 natural hot user Cursor deep |
| 실행계획 | [explain/amplified-user-offset-page0.txt](./explain/amplified-user-offset-page0.txt) | amplified hot user Offset shallow |
| 실행계획 | [explain/amplified-user-offset-deep.txt](./explain/amplified-user-offset-deep.txt) | amplified hot user Offset deep |
| 실행계획 | [explain/amplified-user-cursor-deep.txt](./explain/amplified-user-cursor-deep.txt) | cursor-source lookup이 포함된 amplified hot user Cursor deep |
| 부하 테스트 | [k6/offset-page0-summary.txt](./k6/offset-page0-summary.txt) | natural Offset page0 p95 |
| 부하 테스트 | [k6/offset-mid-summary.txt](./k6/offset-mid-summary.txt) | natural Offset mid p95 |
| 부하 테스트 | [k6/offset-deep-summary.txt](./k6/offset-deep-summary.txt) | natural Offset deep p95 |
| 부하 테스트 | [k6/cursor-summary.txt](./k6/cursor-summary.txt) | natural Cursor p95 |
| 부하 테스트 | [k6/amplified-offset-page0-summary.txt](./k6/amplified-offset-page0-summary.txt) | amplified Offset page0 p95 |
| 부하 테스트 | [k6/amplified-offset-mid-summary.txt](./k6/amplified-offset-mid-summary.txt) | amplified Offset mid p95 |
| 부하 테스트 | [k6/amplified-offset-deep-summary.txt](./k6/amplified-offset-deep-summary.txt) | amplified Offset deep p95 |
| 부하 테스트 | [k6/amplified-cursor-summary.txt](./k6/amplified-cursor-summary.txt) | amplified Cursor p95 |
| SQL 통계 | [pg-stat-statements/offset-page-api.txt](./pg-stat-statements/offset-page-api.txt) | natural Offset/Page SQL 스냅샷 |
| SQL 통계 | [pg-stat-statements/cursor-api.txt](./pg-stat-statements/cursor-api.txt) | natural Cursor SQL 스냅샷 |
| SQL 통계 | [pg-stat-statements/amplified-offset-page-api.txt](./pg-stat-statements/amplified-offset-page-api.txt) | amplified Offset/Page SQL 스냅샷 |
| SQL 통계 | [pg-stat-statements/amplified-cursor-api.txt](./pg-stat-statements/amplified-cursor-api.txt) | amplified Cursor SQL 스냅샷 |
