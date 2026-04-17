# Q10b. UPDATE + JOIN (FROM절 사용)

## 난이도
중급

## 주제
UPDATE with JOIN (PostgreSQL 방식)

## 문제

사용자가 주문을 완료(`orders.status = 'DELIVERED'`)하면 포인트를 적립해주는 로직을 구현하세요.

아직 포인트가 적립되지 않은 배달 완료 주문에 대해,
해당 주문의 `final_price` 의 **1%** 를 사용자의 `point_balance` 에 더해주세요.

조건:
- `orders.status = 'DELIVERED'` 인 주문
- 해당 주문에 대한 `point_history` 기록이 아직 없는 경우만 업데이트
  (힌트: `NOT EXISTS` 서브쿼리 활용)

## 실행 전 확인

```sql
-- 대상 주문 확인
SELECT o.id, o.user_id, o.final_price, FLOOR(o.final_price * 0.01) AS earn_point
FROM orders o
WHERE o.status = 'DELIVERED'
  AND NOT EXISTS (
      SELECT 1 FROM point_history ph
      WHERE ph.user_id = o.user_id
  );
```

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
-- PostgreSQL에서 UPDATE + FROM (다른 테이블 참조)
UPDATE users u
SET point_balance = u.point_balance + FLOOR(o.final_price * 0.01)
FROM orders o
WHERE u.id = o.user_id
  AND o.status = 'DELIVERED'
  AND NOT EXISTS (
      SELECT 1
      FROM point_history ph
      WHERE ph.user_id = o.user_id
        AND ph.description LIKE '%order:' || o.id::text || '%'
  );
```

## 해설

- **PostgreSQL의 UPDATE + FROM** : 다른 테이블의 값을 참조해서 UPDATE 할 때 사용.
  ```sql
  UPDATE 테이블A
  SET 컬럼 = 새값
  FROM 테이블B
  WHERE 테이블A.키 = 테이블B.키
    AND 추가조건;
  ```
- `FLOOR(값)` : 내림 함수. `ROUND` 는 반올림, `CEIL` 은 올림.
- `o.id::text` : PostgreSQL에서 숫자를 문자열로 캐스팅. `CAST(o.id AS TEXT)` 와 동일.
- `||` : 문자열 연결 연산자 (PostgreSQL). SQL Server는 `+`, MySQL은 `CONCAT()`.

### MySQL과의 문법 차이

```sql
-- MySQL 방식
UPDATE users u
JOIN orders o ON u.id = o.user_id
SET u.point_balance = u.point_balance + FLOOR(o.final_price * 0.01)
WHERE o.status = 'DELIVERED';

-- PostgreSQL 방식
UPDATE users u
SET point_balance = u.point_balance + FLOOR(o.final_price * 0.01)
FROM orders o
WHERE u.id = o.user_id AND o.status = 'DELIVERED';
```

> **PostgreSQL 특이점:** MySQL의 `UPDATE ... JOIN` 문법이 PostgreSQL에서는 `UPDATE ... FROM` 으로 바뀐다. SQLD는 표준 SQL 기준이라 둘 다 알아두면 좋다.
