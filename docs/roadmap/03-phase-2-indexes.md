# 이커머스 DB 최적화 학습 로드맵

## Phase 2. 인덱스 설계 + 실행계획 분석

> "EXPLAIN ANALYZE가 보여주는 것을 읽을 수 있는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

풀스캔으로 확인된 쿼리에 인덱스를 설계하고, 실행계획이 바뀌는 것을 직접 확인한다.

### 단계별 실험

| #   | 실험                      | 방법                                                             | 관찰할 것                         |
| --- | ------------------------- | ---------------------------------------------------------------- | --------------------------------- |
| 1   | **풀스캔 확인**           | `EXPLAIN ANALYZE SELECT * FROM product WHERE status = 'ON_SALE'` | Seq Scan 확인                     |
| 2   | **단일 인덱스 추가**      | `CREATE INDEX idx_product_status ON product(status)`             | Index Scan으로 변경 여부          |
| 3   | **복합 인덱스 순서 함정** | `(category_id, status)` vs `(status, category_id)` 비교          | 쿼리 패턴에 따른 성능 차이        |
| 4   | **커버링 인덱스**         | SELECT 컬럼을 인덱스에 포함                                      | Index Only Scan 달성              |
| 5   | **Soft Delete 함정**      | `is_deleted = false` 조건의 인덱스 효과 측정                     | 부분 인덱스(Partial Index)로 해결 |

### 핵심 실험 쿼리

```sql
-- 실험 1: 복합 인덱스 순서
-- 쿼리 패턴: category_id 고정, status 필터
EXPLAIN ANALYZE
SELECT id, name, base_price
FROM product
WHERE category_id = 10 AND status = 'ON_SALE'
ORDER BY created_at DESC;

-- (category_id, status) 인덱스 → Index Scan ✅
-- (status, category_id) 인덱스 → 풀스캔 또는 비효율 ❌

-- 실험 2: 커버링 인덱스
-- SELECT 컬럼(id, name, base_price)을 인덱스에 포함시키면
-- 테이블 접근 없이 Index Only Scan 달성
CREATE INDEX idx_covering
  ON product(category_id, status)
  INCLUDE (name, base_price);

-- 실험 3: 부분 인덱스 (Soft Delete 함정 해결)
-- 일반 인덱스: is_deleted = false가 99%면 옵티마이저가 풀스캔 선택
-- 부분 인덱스: 처음부터 false인 행만 인덱싱
CREATE INDEX idx_product_active
  ON product(category_id, status)
  WHERE is_deleted = false;
```

### 모니터링으로 확인하는 것

- `pg_stat_statements`로 쿼리별 실행시간 Before/After 비교
- Grafana: 인덱스 추가 전후 평균 쿼리 시간 (ms)
- k6: 상품 검색 API 응답시간 분포 변화

### 이 Phase에서 얻는 인사이트

- 인덱스가 항상 빠른 게 아닌 이유 — 옵티마이저가 선택하는 기준
- 복합 인덱스는 **왼쪽 컬럼부터** 사용해야 효과가 있다
- Soft Delete 구현 방식이 인덱스 설계에 영향을 미친다

### 측정 지표 (회고용)

- 인덱스 추가 전/후 쿼리 실행시간 (ms)
- Seq Scan → Index Scan → Index Only Scan 전환 확인
- 복합 인덱스 순서 차이로 인한 성능 배율

### 남은 문제 → Phase 3으로

> "인덱스를 다 붙였는데도 주문 목록 조회가 느리다. 쿼리가 왜 이렇게 많이 나가지?"

### 완료 조건

- [ ] 상품 검색 대표 쿼리의 인덱스 적용 전 실행계획과 실행시간을 기록했다.
- [ ] 단일, 복합, 커버링, 부분 인덱스 중 실험 대상 인덱스를 적용했다.
- [ ] Seq Scan, Index Scan, Index Only Scan 전환 여부를 `EXPLAIN ANALYZE`로 확인했다.
- [ ] 인덱스 적용 전/후 응답시간 또는 쿼리 실행시간을 비교했다.
- [ ] 남아 있는 병목이 쿼리 수 문제인지 확인하고 Phase 3으로 넘길 근거를 기록했다.

---
