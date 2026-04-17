# Q01b. SELECT + LIKE / IN 패턴 매칭

## 난이도
초급

## 주제
SELECT, LIKE, IN

## 문제

`product` 테이블에서 아래 조건을 만족하는 상품을 조회하세요.

- 상품명(`name`)에 `'셔츠'` 또는 `'티셔츠'` 가 포함된 상품
- **또는** 카테고리 ID(`category_id`)가 `1`, `2`, `3` 중 하나인 상품
- 단, 삭제된 상품(`is_deleted = true`)은 제외

아래 컬럼만 출력하세요.
- `id`
- `name`
- `category_id`
- `base_price`
- `status`

## 기대 결과 형태

| id | name       | category_id | base_price | status  |
|----|------------|-------------|------------|---------|
| .. | 기본 티셔츠 | 1           | 29000      | ON_SALE |
| .. | 린넨 셔츠  | 2           | 49000      | ON_SALE |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT id, name, category_id, base_price, status
FROM product
WHERE (name LIKE '%셔츠%' OR category_id IN (1, 2, 3))
  AND is_deleted = false;
```

## 해설

- `LIKE '%셔츠%'` : `%` 는 0개 이상의 임의 문자. 앞뒤에 붙이면 "포함" 검색.
  - `LIKE '셔츠%'` → 셔츠로 시작
  - `LIKE '%셔츠'` → 셔츠로 끝남
  - `LIKE '%셔츠%'` → 어디든 포함
- `OR` 와 `AND` 를 같이 쓸 때는 **괄호**로 우선순위를 명확히 해야 한다.
  - 괄호 없이 쓰면 `AND` 가 `OR` 보다 우선 적용되어 의도치 않은 결과가 나올 수 있다.

> **자주 하는 실수:** `WHERE name LIKE '%셔츠%' OR category_id IN (1,2,3) AND is_deleted = false`
> 이렇게 쓰면 `AND` 가 먼저 적용되어 `(name LIKE '%셔츠%') OR (category_id IN (1,2,3) AND is_deleted = false)` 로 해석된다. 삭제된 셔츠 상품이 포함될 수 있음.
