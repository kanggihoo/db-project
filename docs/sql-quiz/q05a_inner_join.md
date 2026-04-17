# Q05a. INNER JOIN (2테이블)

## 난이도
중급

## 주제
INNER JOIN, 테이블 별칭

## 문제

`users` 테이블과 `orders` 테이블을 조인하여,
각 주문의 주문자 이름, 이메일, 주문 상태, 최종 결제 금액을 조회하세요.

- `status`가 `'PAID'` 또는 `'DELIVERED'` 인 주문만 포함
- `final_price` 내림차순 정렬

출력 컬럼:
- `order_id`
- `user_name`
- `email`
- `status`
- `final_price`

## 기대 결과 형태

| order_id | user_name | email              | status    | final_price |
|----------|-----------|--------------------|-----------|-------------|
| 23       | 홍길동    | hong@example.com   | DELIVERED | 189000      |
| 7        | 김철수    | kim@example.com    | PAID      | 145000      |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    o.id          AS order_id,
    u.name        AS user_name,
    u.email,
    o.status,
    o.final_price
FROM orders o
INNER JOIN users u ON o.user_id = u.id
WHERE o.status IN ('PAID', 'DELIVERED')
ORDER BY o.final_price DESC;
```

## 해설

- `INNER JOIN 테이블 ON 조건` : 두 테이블에서 조인 조건을 만족하는 행만 반환.
  - 어느 한쪽에만 있는 데이터는 결과에서 제외됨.
- 테이블 별칭(alias): `FROM orders o` → `o` 로 축약. 컬럼 앞에 `o.` / `u.` 로 어느 테이블 컬럼인지 명시.
  - 두 테이블에 같은 이름의 컬럼이 있을 때 (예: `id`) 별칭 없이 쓰면 오류.
- `JOIN` 만 써도 기본값이 `INNER JOIN` 이다.

> **핵심:** INNER JOIN은 교집합. 조인 키(`user_id ↔ id`)가 양쪽 테이블에 모두 존재하는 행만 나온다.
