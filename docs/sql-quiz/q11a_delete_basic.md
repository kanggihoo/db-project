# Q11a. DELETE 기초

## 난이도
중급

## 주제
DELETE, 조건부 삭제

## 문제

### 문제 1
`cart_item` 테이블에서 추가된 지 30일이 넘은 장바구니 아이템을 삭제하세요.
- `added_at` 기준으로 현재 시간에서 30일 이전인 항목 삭제
- PostgreSQL의 날짜 함수 활용

### 문제 2
`review_like` 테이블에서 특정 사용자(`user_id = 5`)가 누른 좋아요를 모두 삭제하세요.

## 실행 전 확인

```sql
-- 문제 1 대상 확인
SELECT id, cart_id, sku_id, added_at
FROM cart_item
WHERE added_at < NOW() - INTERVAL '30 days';

-- 문제 2 대상 확인
SELECT id, review_id, user_id FROM review_like WHERE user_id = 5;
```

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- 문제 1: 30일 이상 된 장바구니 아이템 삭제
DELETE FROM cart_item
WHERE added_at < NOW() - INTERVAL '30 days';

-- 문제 2: 특정 유저의 좋아요 전체 삭제
DELETE FROM review_like
WHERE user_id = 5;
```

## 해설

- `DELETE FROM 테이블 WHERE 조건` 기본 구조.
- **WHERE 없이 DELETE 하면 테이블 전체 데이터 삭제!** 절대 주의.
- `NOW()` : 현재 타임스탬프 반환.
- `INTERVAL '30 days'` : PostgreSQL의 날짜 간격 표현.
  - `NOW() - INTERVAL '30 days'` = 30일 전 시각
  - `INTERVAL '1 year'`, `INTERVAL '3 months'`, `INTERVAL '1 hour'` 등 다양하게 활용

### DELETE vs TRUNCATE

| | DELETE | TRUNCATE |
|--|--|--|
| WHERE 조건 | 가능 | 불가능 (전체만) |
| 롤백 | 가능 (트랜잭션 내) | 일부 DB에서 불가 |
| 속도 | 느림 (행 단위) | 빠름 |
| 용도 | 조건부 삭제 | 테이블 전체 비우기 |

> **안전한 DELETE 프로세스:**
> 1. SELECT로 대상 확인
> 2. BEGIN;
> 3. DELETE 실행
> 4. 영향받은 행 수 확인 (`DELETE N` 메시지)
> 5. COMMIT; 또는 ROLLBACK;
