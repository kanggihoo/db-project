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
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/00-data-profile.sql > docs/evidence/phase-06/data-profile/row-counts.txt
```

상품 리뷰 요약:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/10-review-baseline-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/11-review-baseline-explain.sql > docs/evidence/phase-06/review-aggregate/baseline/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/12-review-naive-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/13-review-naive-explain.sql > docs/evidence/phase-06/review-aggregate/naive-index/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/14-review-query-shaped-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/15-review-query-shaped-explain.sql > docs/evidence/phase-06/review-aggregate/query-shaped-index/explain.txt
```

월별 주문 집계:

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/20-monthly-baseline-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/21-monthly-baseline-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/baseline/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/22-monthly-naive-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/23-monthly-naive-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/naive-index/explain.txt

docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/24-monthly-query-shaped-prepare.sql
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/25-monthly-query-shaped-explain.sql > docs/evidence/phase-06/monthly-order-aggregate/query-shaped-index/explain.txt
```

## API 테스트

`ecommerce/`에서 실행한다.

```bash
rtk gradlew test --tests "*ProductReviewSummaryTest"
```

## k6 증거

단순 인덱스 API 증거를 실행한다.

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/12-review-naive-prepare.sql
K6_LOG_FILE=docs/evidence/phase-06/review-summary-api/naive-index/k6-summary.txt K6_RUN_WINDOW_FILE=docs/evidence/phase-06/review-summary-api/naive-index/run-window.json PHASE=phase-06 POOL=pool10 ./k6/run.sh review-summary review-summary-baseline prometheus
```

쿼리 형태 맞춤 인덱스 API 증거를 실행한다.

```bash
docker compose exec postgres psql -U app -d ecommerce -f scripts/phase-06/14-review-query-shaped-prepare.sql
K6_LOG_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/k6-summary.txt K6_RUN_WINDOW_FILE=docs/evidence/phase-06/review-summary-api/query-shaped-index/run-window.json PHASE=phase-06 POOL=pool10 ./k6/run.sh review-summary review-summary-baseline prometheus
```
