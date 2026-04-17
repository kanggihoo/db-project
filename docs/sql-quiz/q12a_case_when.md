# Q12a. CASE WHEN 기초

## 난이도
중급

## 주제
CASE WHEN, 조건별 값 분류

## 문제

`users` 테이블에서 포인트 잔액을 기준으로 등급을 분류하는 레이블을 추가하세요.

포인트 구간:
- 10000 이상 → `'프리미엄'`
- 5000 이상 10000 미만 → `'일반+'`
- 1000 이상 5000 미만 → `'일반'`
- 1000 미만 → `'신규'`

출력 컬럼:
- `id`
- `name`
- `grade`
- `point_balance`
- `point_tier` : CASE WHEN으로 분류한 레이블

결과는 `point_balance` 내림차순으로 정렬하세요.

## 기대 결과 형태

| id | name   | grade | point_balance | point_tier |
|----|--------|-------|---------------|------------|
| 3  | 홍길동 | VIP   | 15000         | 프리미엄   |
| 7  | 이영희 | GOLD  | 7500          | 일반+      |
| 2  | 김철수 | SILVER| 2000          | 일반       |
| 9  | 박민수 | BRONZE| 500           | 신규       |

---

## 정답 쿼리 (먼저 풀고 확인!)

```sql
SELECT
    id,
    name,
    grade,
    point_balance,
    CASE
        WHEN point_balance >= 10000 THEN '프리미엄'
        WHEN point_balance >= 5000  THEN '일반+'
        WHEN point_balance >= 1000  THEN '일반'
        ELSE '신규'
    END AS point_tier
FROM users
ORDER BY point_balance DESC;
```

## 해설

- `CASE WHEN 조건 THEN 값 ... ELSE 기본값 END` 기본 구조.
- 조건은 위에서 아래로 순서대로 평가되며, **처음 true인 조건에서 멈춘다.**
  - `WHEN point_balance >= 5000` 은 이미 `>= 10000` 을 통과한 행에는 적용 안 됨.
- `ELSE` 는 모든 조건에 해당하지 않을 때. 생략 시 NULL 반환.
- `END` 로 CASE 블록을 반드시 닫아야 한다.

### 두 가지 CASE 형태

```sql
-- 형태 1: 검색 CASE (조건식)
CASE WHEN grade = 'VIP' THEN '최상위'
     WHEN grade = 'GOLD' THEN '상위'
     ELSE '일반'
END

-- 형태 2: 단순 CASE (값 비교)
CASE grade
    WHEN 'VIP'    THEN '최상위'
    WHEN 'GOLD'   THEN '상위'
    ELSE '일반'
END
```

> **팁:** 형태 2는 등호(=) 비교만 가능. 범위 비교(`>=`, `<`)가 필요하면 형태 1을 써야 한다.
