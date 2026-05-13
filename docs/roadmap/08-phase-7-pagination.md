# 이커머스 DB 최적화 학습 로드맵

## Phase 7. 페이지네이션 최적화

> "Offset 방식은 왜 뒤로 갈수록 느려지는가? Cursor는 어떻게 이걸 해결하는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

POINT_HISTORY, DELIVERY_TRACKING처럼 계속 쌓이는 테이블에서 Offset 방식의 한계를 측정하고, Cursor 방식으로 전환하여 정량적으로 비교한다.

### 왜 Offset이 느린가

```sql
-- 1페이지: POINT_HISTORY에서 10건
SELECT * FROM point_history WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;        -- 10건만 읽음 → 빠름

-- 1000페이지: 10,000번째부터 10건
SELECT * FROM point_history WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 9990;     -- 10,000건을 읽고 버린 뒤 10건 반환 → 느림
```

> **핵심:** OFFSET 9990이면 앞의 9990건을 전부 읽고 버린다. 페이지가 뒤로 갈수록 버리는 양이 늘어난다.

### Cursor 방식으로 전환

```sql
-- Cursor 방식: 마지막으로 읽은 id를 기준으로 다음 페이지 조회
-- 몇 페이지든 항상 인덱스에서 딱 10건만 읽음
SELECT * FROM point_history
WHERE user_id = 1
  AND id < :lastId          -- 마지막으로 읽은 id
ORDER BY id DESC
LIMIT 10;
```

### Count 쿼리 최적화

```java
// Spring Data JPA Pageable 사용 시 COUNT 쿼리가 자동 발생
// 복잡한 JOIN이 있으면 COUNT도 똑같이 JOIN → 병목

// Before: COUNT 쿼리에도 전체 JOIN 포함
Page<Order> findByUserId(Long userId, Pageable pageable);

// After: COUNT 쿼리 분리 (단순하게)
@Query(value = "SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.user.id = :userId",
       countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);
```

### 단계별 실험

| #   | 실험                                               | 측정 방법                            |
| --- | -------------------------------------------------- | ------------------------------------ |
| 1   | Offset 1페이지 vs 100페이지 vs 1000페이지 응답시간 | k6로 페이지별 p95 측정               |
| 2   | Cursor 방식 동일 조건 응답시간                     | 1페이지와 1000페이지가 동일한지 확인 |
| 3   | COUNT 쿼리 분리 전/후                              | 전체 목록 API 응답시간 비교          |

### Cursor 방식의 트레이드오프

| 항목             | Offset                  | Cursor               |
| ---------------- | ----------------------- | -------------------- |
| 성능             | 뒤로 갈수록 O(n)        | 항상 O(1)            |
| 임의 페이지 이동 | 가능 ("3페이지로 이동") | 불가능 (순차 탐색만) |
| 정렬 기준 변경   | 자유롭게 가능           | 커서 컬럼 고정 필요  |
| 구현 복잡도      | 단순                    | 상대적으로 복잡      |
| 적합한 UX        | 페이지 번호 UI          | 무한 스크롤, 더보기  |

### 모니터링으로 확인하는 것

- k6: 페이지 번호별 응답시간 그래프 (Offset은 우상향, Cursor는 수평)
- Grafana: Offset 1000페이지 vs Cursor 1000페이지 p95 응답시간 비교
- `EXPLAIN ANALYZE`: Offset의 `rows removed by filter` 수치 확인

### 이 Phase에서 얻는 인사이트

- Offset은 "건너뛰는 게 아니라 읽고 버리는 것"이라는 이해
- Cursor 방식이 무한 스크롤 UX에 적합한 기술적 이유
- COUNT 쿼리가 숨어서 병목을 만드는 패턴

### 측정 지표 (회고용)

- Offset: 1페이지(Nms) vs 1000페이지(Nms) — 배율 차이
- Cursor: 1페이지 vs 1000페이지 응답시간 (거의 동일함을 증명)
- COUNT 쿼리 분리 전/후 응답시간 차이 (ms)

### 완료 조건

- [ ] Offset 방식에서 앞쪽 페이지와 뒤쪽 페이지의 p95 차이를 측정했다.
- [ ] Cursor 방식으로 동일 조건을 조회하고 페이지 위치에 따른 응답시간 차이를 비교했다.
- [ ] `EXPLAIN ANALYZE`로 Offset이 읽고 버리는 row 수를 확인했다.
- [ ] COUNT 쿼리 분리 전/후 목록 API 응답시간 또는 실행계획을 비교했다.
- [ ] Phase 8에서 관측할 핵심 DB 지표 후보를 정리했다.

---
