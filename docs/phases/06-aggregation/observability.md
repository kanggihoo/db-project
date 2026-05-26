# 6단계 관측 기준

## 주 증거

| 증거 | 목적 |
|---|---|
| `EXPLAIN (ANALYZE, BUFFERS)` | 스캔, 조인, 정렬, 집계 노드와 실행시간 비교 |
| 데이터 프로파일 | 행 수와 분포를 기준으로 플래너 선택 해석 |
| 측정 조건별 `pg_stat_statements` 초기화 | 쿼리 단위 스냅샷 분리 |
| k6 요약 | 단순 인덱스와 쿼리 형태 맞춤 인덱스의 대표 API p95/p99 비교 |
| Grafana 스크린샷 | API 증거에서 Hikari와 DB 테이블 스캔/인덱스 스캔 추이 확인 |

## SQL 실행계획 해석

- `Planning Time`과 `Execution Time`을 기록한다.
- 공유 hit/read 버퍼를 기록한다.
- 스캔 노드를 기록한다: `Seq Scan`, `Index Scan`, `Bitmap Index Scan`, `Bitmap Heap Scan`.
- 집계 노드를 기록한다: `HashAggregate` 또는 `GroupAggregate`.
- `Sort` 존재 여부와 `Sort Method`를 기록한다.
- 쿼리 형태 맞춤 인덱스가 항상 `GroupAggregate`를 강제한다고 가정하지 않는다.

## API 해석

k6는 보조 증거다. 실제 후보 인덱스가 API p95/p99에 영향을 주는지 확인한다. k6는 SQL 전용 실행계획 증거를 대체하지 않는다.
