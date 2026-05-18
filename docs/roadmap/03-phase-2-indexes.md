# 이커머스 DB 최적화 학습 로드맵

## Phase 2. 인덱스 설계 + 실행계획 분석

> "EXPLAIN ANALYZE가 보여주는 것을 읽을 수 있는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

Phase 1의 `GET /api/products?categoryId=&status=` API가 발생시키는 `product(category_id, status)` 필터 쿼리를 대상으로 인덱스를 설계하고, 실행계획과 측정 지표가 어떻게 바뀌는지 확인한다.

이 Phase의 메인 비교 실험에서는 Phase 1의 API, Controller, Service, Repository 흐름을 유지한다. 변경 대상은 DB 인덱스와 측정 문서이며, API 구현을 바꿔 성능을 개선하지 않는다.

보조 실험에서는 API를 통하지 않고 `psql`에서 SQL을 직접 실행한다. 보조 실험의 목적은 단일 인덱스 선택도, 복합 인덱스 컬럼 순서, 커버링 인덱스, 부분 인덱스의 원리를 `EXPLAIN ANALYZE`로 확인하는 것이다.

### 단계별 실험

| # | 구분 | 실험 | 방법 | 관찰할 것 |
|---|---|---|---|---|
| 1 | 메인 | Phase 1 상품 검색 기준선 재확인 | 실제 API SQL을 `EXPLAIN (ANALYZE, BUFFERS)`로 확인 | 인덱스 적용 전 Seq Scan과 실행시간 |
| 2 | 메인 | 복합 인덱스 적용 | `CREATE INDEX idx_product_category_status ON product(category_id, status)` | Seq Scan에서 Index Scan 계열로 전환되는지 |
| 3 | 메인 | Phase 1 결과와 비교 | 같은 `loadtest`, `pool10`, `products baseline` 조건으로 k6 실행 | `pg_stat_statements`, k6 p95, Grafana 지표 변화 |
| 4 | 보조 | 단일 인덱스 선택도 | `product(status)` 인덱스와 `status = 'ON_SALE'` 조회 비교 | 선택도가 낮으면 인덱스가 있어도 Seq Scan이 선택될 수 있음 |
| 5 | 보조 | 복합 인덱스 순서 | 동등 조건만이 아니라 단독 조건, range 조건, 정렬 조건을 포함해 비교 | 왼쪽 컬럼, range 조건, `ORDER BY`가 인덱스 사용에 미치는 영향 |
| 6 | 보조 | 커버링 인덱스 | 필요한 컬럼만 조회하는 SQL과 `INCLUDE` 인덱스 비교 | 테이블 접근 감소와 Index Only Scan 가능성 |
| 7 | 보조 | Soft Delete 부분 인덱스 | `is_deleted = false` 조건을 포함한 SQL과 부분 인덱스 비교 | 부분 인덱스가 인덱스 크기와 실행계획에 미치는 영향 |

### 메인 비교 실험

```sql
-- Phase 1 API가 실제로 만드는 상품 검색 쿼리 형태
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';

-- Phase 2의 메인 비교 대상 인덱스
CREATE INDEX idx_product_category_status
  ON product(category_id, status);
```

메인 비교는 Phase 1 Evidence의 `products/pool10-baseline` 결과를 기준으로 한다.

| 항목 | Phase 1 기준값 |
|---|---:|
| k6 scenario | `products` |
| preset | `baseline` |
| pool | `pool10` |
| request rate | 50 rps |
| API p95 | 17.24ms |
| SQL mean time | 7.90ms |
| SQL total time | 118,489.64ms |
| SQL shape | `product where category_id = ? and status = ?` |

### SQL-only 보조 실험

보조 실험은 API 성능 비교가 아니라 인덱스 원리 확인이다. `psql`로 직접 실행한 `EXPLAIN (ANALYZE, BUFFERS)` 결과를 Evidence로 남기며, Phase 1 API 결과와 직접 비교하지 않는다.

각 보조 실험은 서로 영향을 주지 않도록 대상 인덱스만 남긴 상태에서 실행한다. 실험 사이에는 불필요한 인덱스를 `DROP INDEX IF EXISTS ...`로 제거하고, 필요한 경우 `ANALYZE product`와 `pg_stat_statements_reset()`을 실행해 측정 조건을 분리한다.

#### 단일 인덱스 선택도

```sql
CREATE INDEX idx_product_status
  ON product(status);

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, base_price, category_id, created_at, description,
       is_deleted, name, status, updated_at
FROM product
WHERE status = 'ON_SALE';
```

`ON_SALE` 비율이 높으면 인덱스가 있어도 PostgreSQL이 Seq Scan을 선택할 수 있다. 이 실험은 "인덱스를 만들면 항상 Index Scan이 된다"가 아니라 "선택도가 낮은 조건에서는 Seq Scan이 더 싸게 판단될 수 있다"를 확인한다.

#### 복합 인덱스 순서

동등 조건만 사용하는 `category_id = ? AND status = ?` 쿼리는 `(category_id, status)`와 `(status, category_id)`의 차이가 작을 수 있다. 순서 차이는 단독 조건, range 조건, 정렬 조건을 함께 봐야 한다.

```sql
CREATE INDEX idx_product_category_status_created
  ON product(category_id, status, created_at DESC);

CREATE INDEX idx_product_status_category_created
  ON product(status, category_id, created_at DESC);

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
ORDER BY created_at DESC;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
ORDER BY created_at DESC;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND created_at >= TIMESTAMP '2026-01-01 00:00:00';
```

#### 커버링 인덱스

현재 API 경로는 Entity 전체 컬럼을 조회하므로 `INCLUDE (name, base_price)`만으로 API SQL의 Index Only Scan을 기대하기 어렵다. 커버링 인덱스는 필요한 컬럼만 조회하는 SQL-only 실험에서 확인한다.

```sql
CREATE INDEX idx_product_covering
  ON product(category_id, status)
  INCLUDE (id, name, base_price);

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE';
```

#### Soft Delete 부분 인덱스

현재 Phase 1 API는 `is_deleted = false` 조건을 포함하지 않는다. 따라서 부분 인덱스는 메인 API 비교가 아니라 SQL-only 보조 실험으로 둔다.

```sql
CREATE INDEX idx_product_active_category_status
  ON product(category_id, status)
  WHERE is_deleted = false;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, base_price
FROM product
WHERE category_id = 10
  AND status = 'ON_SALE'
  AND is_deleted = false;
```

### 모니터링으로 확인하는 것

- `pg_stat_statements`로 쿼리별 실행시간 Before/After 비교
- Grafana: 인덱스 추가 전후 평균 쿼리 시간 (ms)
- k6: 상품 검색 API 응답시간 분포 변화
- `EXPLAIN (ANALYZE, BUFFERS)`: 실제 실행계획, scan type, 읽은 block 수 확인

### 이 Phase에서 얻는 인사이트

- 인덱스를 만들었다고 항상 선택되는 것은 아니다
- 선택도가 낮은 단일 컬럼 인덱스는 Seq Scan보다 불리할 수 있다
- 복합 인덱스는 왼쪽 컬럼, 동등 조건, range 조건, 정렬 조건의 조합에 따라 효과가 달라진다
- 커버링 인덱스는 쿼리가 필요한 컬럼을 인덱스가 충족할 때 의미가 있다
- Soft Delete 구현 방식이 인덱스 설계에 영향을 미친다

### 측정 지표 (회고용)

- 메인 API의 인덱스 추가 전/후 쿼리 실행시간 (ms)
- 메인 API의 인덱스 추가 전/후 k6 p95 응답시간
- 메인 API의 인덱스 추가 전/후 `pg_stat_statements.mean_exec_time`
- `EXPLAIN (ANALYZE, BUFFERS)`의 Seq Scan, Index Scan, Bitmap Index Scan, Index Only Scan 여부
- SQL-only 보조 실험에서 인덱스 선택 여부와 block read 차이

### 남은 문제 → Phase 3으로

> "인덱스를 다 붙였는데도 주문 목록 조회가 느리다. 쿼리가 왜 이렇게 많이 나가지?"

### 완료 조건

- [ ] Phase 1의 `products/pool10-baseline` 결과를 비교 기준으로 사용했다.
- [ ] 상품 검색 대표 SQL의 인덱스 적용 전 `EXPLAIN (ANALYZE, BUFFERS)` 결과를 기록했다.
- [ ] `idx_product_category_status` 적용 후 실행계획과 실행시간 변화를 기록했다.
- [ ] 같은 `loadtest`, `pool10`, `products baseline` 조건에서 k6를 다시 실행했다.
- [ ] `pg_stat_statements.mean_exec_time`, k6 p95, Grafana 지표를 Phase 1 결과와 비교했다.
- [ ] 단일 인덱스 선택도, 복합 인덱스 순서, 커버링 인덱스, 부분 인덱스는 SQL-only 보조 실험으로 분리해 해석했다.
- [ ] 남아 있는 병목이 쿼리 수 문제인지 확인하고 Phase 3으로 넘길 근거를 기록했다.

---
