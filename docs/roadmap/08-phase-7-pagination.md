# 이커머스 DB 최적화 학습 로드맵

## Phase 7. 페이지네이션 최적화

> "Offset 방식은 왜 뒤로 갈수록 느려지는가? Cursor는 어떻게 이걸 해결하는가?"

### 이전 Phase의 문제를 어떻게 해결하는가

**Point History**처럼 계속 쌓이는 이력 목록에서 Offset 방식의 한계를 측정하고, Cursor 방식으로 전환했을 때 깊은 페이지 조회 비용이 어떻게 달라지는지 비교한다.

**Delivery Tracking**도 pagination optimization target이지만, 배송 1건의 tracking history는 row 수가 적을 수 있다. Phase 7의 주 실험은 hot user의 **Point History**로 진행하고, **Delivery Tracking**은 전체/상태별 배송 이벤트 목록처럼 한 조건에 많은 row가 걸리는 경우의 보조 후보로 둔다.

### 실험 대상 선정

Phase 7은 **Point History** 단일 테이블로 진행하되, 두 층의 실험으로 나눈다.

- 전체 `point_history` SQL-only 실험: Offset과 Cursor의 동작 원리를 가장 단순하게 확인한다.
- hot user의 **Point History** API 실험: 실제 "내 포인트 이력" 목록에서 p95 응답시간과 COUNT 쿼리 비용을 확인한다.

전체 테이블 pagination은 실제 사용자 기능이라기보다 원리 확인용 실험이다. API/k6 증거는 하나의 조회 조건에 매칭되는 row가 충분히 많은 hot user 기준으로 남긴다.

먼저 포인트 이력이 많은 hot user를 찾고, 그 user의 row 수를 기준으로 비교할 페이지를 정한다.

```sql
SELECT user_id, COUNT(*) AS point_count
FROM point_history
GROUP BY user_id
ORDER BY point_count DESC
LIMIT 20;
```

선정한 `user_id`에 대해 전체 row 수를 다시 확인한다.

```sql
SELECT COUNT(*) AS point_count
FROM point_history
WHERE user_id = :userId;
```

`size = 20`일 때 깊은 페이지는 마지막 페이지 자체보다 전체 페이지의 70~80% 지점으로 잡는다. 마지막 페이지는 결과가 0건이거나 `size`보다 적을 수 있어 비교가 애매해질 수 있다.

```text
maxPage = floor((pointCount - 1) / size)
midPage = floor(maxPage * 0.5)
deepPage = floor(maxPage * 0.8)
```

### 왜 Offset이 느린가

Offset은 인덱스가 있어도 앞쪽 row를 지나간 뒤 버리고, 그 다음 `LIMIT` 수만큼 반환한다. Phase 7에서는 필터/정렬 병목과 pagination 병목을 섞지 않기 위해 Offset과 Cursor 모두 같은 정렬 기준과 같은 인덱스 조건에서 비교한다.

```sql
-- 전체 point_history SQL-only 실험 기준 인덱스
CREATE INDEX idx_point_history_created_id
ON point_history (created_at DESC, id DESC);

-- hot user Point History API 실험 기준 인덱스
CREATE INDEX idx_point_history_user_created_id
ON point_history (user_id, created_at DESC, id DESC);
```

전체 테이블 SQL-only 실험은 `WHERE` 없이 전체 이력 피드를 대상으로 한다.

```sql
-- 전체 테이블 얕은 페이지
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

-- 전체 테이블 깊은 페이지
SELECT *
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :globalDeepOffset;
```

hot user API 실험은 `user_id` 조건이 있는 실제 목록 조회를 대상으로 한다.

```sql
-- hot user 얕은 페이지
SELECT *
FROM point_history
WHERE user_id = :userId
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET 0;

-- hot user 깊은 페이지
SELECT *
FROM point_history
WHERE user_id = :userId
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :deepOffset;
```

> **핵심:** `OFFSET :deepOffset`은 해당 위치까지의 row를 비용 없이 건너뛰는 것이 아니다. 반환하지 않을 앞쪽 row를 지나간 뒤 `LIMIT` row를 반환한다.

### Cursor 방식으로 전환

Cursor 방식은 마지막으로 본 row의 정렬 키를 다음 조회 조건으로 사용한다. Offset 예제와 같은 목록을 비교하려면 `id`만 기준으로 삼지 않고, 정렬 기준인 `(created_at, id)`를 함께 사용한다.

```sql
-- 전체 테이블 deep page 위치의 cursor 값을 먼저 확인
SELECT created_at, id
FROM point_history
ORDER BY created_at DESC, id DESC
LIMIT 1 OFFSET :globalDeepOffset;

-- 전체 테이블 Cursor 조회
SELECT *
FROM point_history
WHERE (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

```sql
-- hot user deep page 위치의 cursor 값을 먼저 확인
SELECT created_at, id
FROM point_history
WHERE user_id = :userId
ORDER BY created_at DESC, id DESC
LIMIT 1 OFFSET :deepOffset;
```

```sql
-- Cursor 방식: 마지막으로 읽은 row 이후의 다음 페이지 조회
SELECT *
FROM point_history
WHERE user_id = :userId
  AND (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

### COUNT 쿼리 비용

Spring Data JPA에서 `Page<T>`를 사용하면 실제 목록 조회 외에 전체 개수 계산용 `COUNT(*)` 쿼리가 함께 발생할 수 있다. 화면에는 20건만 필요해도, 전체 페이지 수를 계산하려고 해당 조건의 전체 row 수를 세는 비용이 추가된다.

```java
// Page 방식: data query + count query가 발생할 수 있음
Page<PointHistory> findByUserId(Long userId, Pageable pageable);
```

예상 SQL은 다음처럼 나뉜다.

```sql
-- data query
SELECT *
FROM point_history
WHERE user_id = :userId
ORDER BY created_at DESC, id DESC
LIMIT 20 OFFSET :offset;

-- count query
SELECT COUNT(*)
FROM point_history
WHERE user_id = :userId;
```

Cursor 또는 `Slice` 방식은 전체 개수를 세지 않고 `size + 1`개를 조회해 다음 페이지 존재 여부만 판단할 수 있다.

```sql
SELECT *
FROM point_history
WHERE user_id = :userId
  AND (created_at, id) < (:lastCreatedAt, :lastId)
ORDER BY created_at DESC, id DESC
LIMIT 21;
```

`size = 20`일 때 21건이 조회되면 `hasNext = true`, 20건 이하이면 `hasNext = false`로 판단한다.

### 단계별 실험

| # | 실험 | 측정 방법 |
|---|---|---|
| 1 | 전체 `point_history` SQL-only 원리 확인 | Offset shallow/deep과 Cursor deep의 `EXPLAIN (ANALYZE, BUFFERS)` 비교 |
| 2 | hot user 선정 및 `point_count` 확인 | `GROUP BY user_id`로 row 수가 많은 user 선정 |
| 3 | Offset 얕은/중간/깊은 페이지 응답시간 | k6로 `page=0`, `page=midPage`, `page=deepPage` p95 비교 |
| 4 | hot user Offset/Cursor 실행계획 | deep page 위치의 cursor 값으로 range scan 실행계획과 응답시간 비교 |
| 5 | COUNT 쿼리 비용 | `Page` 방식의 count query와 count 없는 Cursor/Slice 방식 비교 |

### Cursor 방식의 트레이드오프

| 항목 | Offset | Cursor |
|---|---|---|
| 성능 | 깊은 페이지로 갈수록 skip 비용 증가 | cursor 이후 범위를 page size 중심으로 조회 |
| 임의 페이지 이동 | 가능 ("3페이지로 이동") | 어려움 (순차 탐색 중심) |
| 정렬 기준 변경 | 비교적 자유로움 | cursor에 포함할 안정 정렬 키 필요 |
| 구현 복잡도 | 단순 | 상대적으로 복잡 |
| 적합한 UX | 페이지 번호 UI | 무한 스크롤, 더보기 |

### 모니터링으로 확인하는 것

- k6: `page=0`, `page=midPage`, `page=deepPage`의 p95 응답시간 비교
- Grafana: shared `DB Lab Overview` dashboard의 Phase 7 row에서 HTTP p95와 DB 지표 확인
- `EXPLAIN (ANALYZE, BUFFERS)`: 전체 테이블과 hot user 조건에서 Offset deep page와 Cursor 조회의 `actual rows`, `actual time`, `Buffers` 비교
- `pg_stat_statements`: data query와 count query의 호출 수, 평균 실행시간, 누적 실행시간 비교

### 이 Phase에서 얻는 인사이트

- Offset은 "건너뛰는 게 아니라 지나가고 버리는 것"이라는 이해
- 인덱스가 있어도 deep offset의 skip 비용은 남을 수 있다는 점
- Cursor 방식이 무한 스크롤/더보기 UX에 적합한 기술적 이유
- `Page<T>`의 COUNT 쿼리가 목록 조회의 숨은 병목이 될 수 있는 패턴

### 측정 지표 (회고용)

- 선정한 hot user의 `point_count`
- 전체 `point_history` SQL-only 실험의 Offset deep vs Cursor deep 실행시간 차이
- Offset: `page=0`, `page=midPage`, `page=deepPage` p95 응답시간과 배율 차이
- Cursor: deep page 위치 cursor 조회 응답시간
- `EXPLAIN ANALYZE`: Offset deep page와 Cursor 조회의 실행시간, scan row, buffer 사용량
- COUNT 쿼리: `Page` 방식 count query의 평균/누적 실행시간과 Cursor/Slice 전환 후 차이

### 완료 조건

- [ ] 전체 `point_history` SQL-only 실험에서 Offset shallow/deep과 Cursor deep 실행계획을 비교했다.
- [ ] hot user 후보를 조회하고 Phase 7 API 실험 대상 `user_id`, `point_count`, `midPage`, `deepPage`를 기록했다.
- [ ] Offset 방식에서 얕은/중간/깊은 페이지의 k6 p95 차이를 측정했다.
- [ ] 전체 테이블 실험은 `(created_at DESC, id DESC)`, hot user 실험은 `(user_id, created_at DESC, id DESC)` 인덱스 조건에서 비교했다.
- [ ] `EXPLAIN (ANALYZE, BUFFERS)`로 Offset deep page와 Cursor 조회의 실행계획 차이를 확인했다.
- [ ] `Page` 방식에서 발생하는 COUNT 쿼리 비용을 확인하고, count 없는 Cursor/Slice 방식과 비교했다.
- [ ] Phase 7 evidence를 `docs/evidence/phase-07/` 아래에 기록했다.
- [ ] Phase 8에서 관측할 핵심 DB 지표 후보를 정리했다.

---
