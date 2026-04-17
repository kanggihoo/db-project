# Q02a. ORDER BY 정렬

## 난이도
초급

## 주제
ORDER BY, 단일/복합 정렬

## 문제

`product` 테이블에서 판매 중인 상품(`status = 'ON_SALE'`, `is_deleted = false`)을 조회하되,
아래 기준으로 정렬하세요.

1. `base_price` 내림차순 (비싼 것 먼저)
2. 가격이 같으면 `name` 오름차순 (가나다 순)

출력 컬럼: `id`, `name`, `base_price`, `status`

## 기대 결과 형태

| id | name        | base_price | status  |
|----|-------------|------------|---------|
| .. | 프리미엄 코트 | 299000    | ON_SALE |
| .. | 가죽 재킷    | 199000     | ON_SALE |
| .. | 기모 후드티  | 89000      | ON_SALE |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT id, name, base_price, status
FROM product
WHERE status = 'ON_SALE'
  AND is_deleted = false
ORDER BY base_price DESC, name ASC;
```

## 해설

- `ORDER BY 컬럼1 DESC, 컬럼2 ASC` : 복합 정렬. 컬럼1 기준 정렬 후, 값이 같은 경우 컬럼2로 2차 정렬.
- `ASC` : 오름차순 (기본값, 생략 가능)
- `DESC` : 내림차순 (명시 필요)
- `ORDER BY` 는 항상 `WHERE` 다음, `LIMIT` 전에 작성.

> **팁:** `ORDER BY` 에 컬럼 별칭(alias)도 사용 가능. 예: `SELECT base_price AS price ... ORDER BY price DESC`
