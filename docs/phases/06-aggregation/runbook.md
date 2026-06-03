# 6단계 실행 절차

별도 언급이 없으면 명령은 저장소 루트에서 실행한다.

## 측정 데이터베이스 초기화

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## SQL 전용 증거

데이터 프로파일:

```bash
make phase-sql PHASE=phase-06 SCENARIO=data-profile ACTION=profile OUTPUT=docs/evidence/phase-06/data-profile/row-counts.txt
```

상품 리뷰 요약:

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/baseline/explain.txt

make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/naive-index/explain.txt

make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt
```

월별 주문 집계:

```bash
make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=baseline ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt

make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=naive-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=naive-index ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt

make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=query-shaped-index ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=monthly-order-aggregate CONDITION=query-shaped-index ACTION=explain OUTPUT=docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt
```

## API 테스트

`ecommerce/`에서 실행한다.

```bash
rtk gradlew test --tests "*ProductReviewSummaryTest"
```

## k6 증거

단순 인덱스 API 증거를 실행한다.

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=naive-index ACTION=prepare
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
```

쿼리 형태 맞춤 인덱스 API 증거를 실행한다.

```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=query-shaped-index ACTION=prepare
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index
```
