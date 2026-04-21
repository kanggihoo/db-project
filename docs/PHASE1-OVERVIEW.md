# Phase 1 구현 Overview — 직접 확인 가이드

> 이 문서는 Phase 1 완료 조건 체크리스트를 기준으로,
> **무엇이 구현되었고 / 무엇을 직접 실행해야 하는지**를 명확히 구분한다.

---

## 체크리스트 현황 요약

| # | 항목 | 상태 | 비고 |
|---|------|------|------|
| 1 | 재현환경 (VACUUM ANALYZE + 데이터 고정) | ⬜ 미완 | 부하테스트 직전 수동 실행 필요 |
| 2 | 변인통제 (show_sql: false, hikari max=10) | ✅ 완료 | `application.yaml` 이미 적용 |
| 3 | TDD (getPrepareStatementCount N개 단언) | ✅ 완료 | `OrderRepositoryTest` GREEN |
| 4 | k6 분포 (파라미터 랜덤 배분) | ✅ 완료 | 3개 스크립트 가중 분포 적용 |
| 5 | k6 스펙 (VU 50, 5분, Ramp-up 30초) | ✅ 완료 | 3개 스크립트 동일 스펙 |
| 6 | 증빙 스크린샷 4장 보관 | ⬜ 미완 | 부하테스트 실행 후 직접 캡처 필요 |
| 7 | BASELINE.md 정량 표 작성 | ⬜ 미완 | 부하테스트 결과값 채워넣기 필요 |

---

## ✅ 항목 2 — 변인통제 설정 확인

**파일:** `ecommerce/src/main/resources/application.yaml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10   # ✅ 고정
  jpa:
    show-sql: false           # ✅ 로깅 I/O 병목 제거
    properties:
      hibernate:
        format_sql: false
```

→ **별도 조치 불필요.** 이미 올바른 값으로 설정됨.

---

## ✅ 항목 3 — TDD 검증 코드

**파일:** `ecommerce/src/test/java/com/dblab/ecommerce/repository/OrderRepositoryTest.java`

```
핵심 로직:
  statistics.clear()
  orders = orderRepository.findByUserId(userId)        ← 1번 쿼리
  sqlCountBeforeLoop = getPrepareStatementCount()
  for (order : orders) { orderItemRepository.findByOrderId(order.getId()) }  ← N번 쿼리
  extraSqlCount = getPrepareStatementCount() - sqlCountBeforeLoop
  assertThat(extraSqlCount).isEqualTo(orders.size())   ← N개 단언
```

**실행 방법:**
```bash
cd ecommerce
./gradlew test --tests "com.dblab.ecommerce.repository.OrderRepositoryTest"
```

---

## ✅ 항목 4, 5 — k6 스크립트

**파일 위치:** `k6/` 디렉토리

| 스크립트 | 대상 API | 파라미터 분포 |
|----------|----------|--------------|
| `orders-test.js` | `GET /api/orders?userId=` | userId: 1~1000 균등 랜덤 |
| `products-test.js` | `GET /api/products?categoryId=&status=` | categoryId: 1~20 랜덤, status: 3종 랜덤 |
| `points-test.js` | `GET /api/points?userId=&page=` | page: 50%→초반, 30%→중반, 20%→후반 가중 |

**공통 부하 스펙 (3개 동일):**
```
stages: [
  { duration: '30s', target: 50 },   ← Ramp-up
  { duration: '4m30s', target: 50 }, ← 지속
]
timeout: '5s' per request
```

---

## ⬜ 항목 1 — 재현환경 구성 (직접 실행)

부하 테스트 직전, 아래 순서로 실행한다.

### Step 1. 인프라 기동
```bash
# 프로젝트 루트에서
docker-compose up -d
```

### Step 2. 더미 데이터 삽입
```bash
cd ecommerce
./gradlew bootRun --args='--spring.profiles.active=seeder'
# "Seeding complete" 로그 확인 후 Ctrl+C
```

### Step 3. VACUUM ANALYZE (DB 쿼리 플랜 캐시 일관성 확보)
```bash
# PostgreSQL 접속
docker compose exec postgres psql -U app -d ecommerce

# 아래 명령 실행
VACUUM ANALYZE;
\q
```

### Step 4. 데이터 건수 확인 (SETUP.md 목표 건수 대조)
```sql
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking;
```

---

## ⬜ 항목 6 — 스크린샷 4장 캡처 순서

앱 실행 후 k6를 돌리면서 아래 4장을 `docs/evidence/phase1/` 에 저장한다.

```bash
# 앱 실행 (별도 터미널)
cd ecommerce && ./gradlew bootRun

# k6 실행 (Docker)
docker run --rm -i --network host grafana/k6 run - < k6/orders-test.js
docker run --rm -i --network host grafana/k6 run - < k6/products-test.js
docker run --rm -i --network host grafana/k6 run - < k6/points-test.js
```

| 파일명 | 캡처 대상 |
|--------|----------|
| `01_k6_summary.png` | k6 종료 후 터미널 출력 전체 (p95, TPS, 에러율) |
| `02_grafana_p95.png` | Grafana → p95 응답시간 시계열 그래프 |
| `03_pg_stat_statements.png` | psql: `SELECT * FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;` |
| `04_hikari_active.png` | Grafana → HikariCP Active Connections 패널 |

> **Grafana 접속:** http://localhost:3000 (admin/admin)
> **pg_stat_statements 활성화 확인:** `SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';`

---

## ⬜ 항목 7 — BASELINE.md 기록 (k6 결과값 채워넣기)

k6 종료 시 출력되는 값으로 아래 표를 채운다.

| API 타겟 | p50 (ms) | p95 (ms) | TPS | 에러율(%) | Max Active (풀 상한 10) | 요청 단건당 SQL 수 | DB CPU (Max %) | 원인 |
|----------|----------|----------|-----|-----------|--------------------------|-------------------|----------------|------|
| `/api/orders` | - | - | - | - | - | 1+N | - | LAZY 루프 N+1 |
| `/api/products` | - | - | - | - | - | 1 | - | 인덱스 없는 Seq Scan |
| `/api/points` | - | - | - | - | - | 1 | - | Offset 누적 병목 |

> **p50/p95:** k6 출력 `http_req_duration` 항목에서 확인
> **TPS:** k6 출력 `http_reqs` 항목의 `rate` 값
> **에러율:** k6 출력 `http_req_failed` 항목의 `rate` 값 × 100
> **SQL 수:** `OrderRepositoryTest` 출력 콘솔 로그 참조

---

## 현재 구현된 파일 전체 목록

```
ecommerce/src/main/java/com/dblab/ecommerce/
├── controller/
│   ├── OrderController.java        GET /api/orders?userId=
│   ├── ProductController.java      GET /api/products?categoryId=&status=
│   └── PointController.java        GET /api/points?userId=&page=&size=
├── service/
│   ├── OrderService.java           N+1 의도적 유발 (findByUserId → 루프 findByOrderId)
│   ├── ProductService.java         Seq Scan 유발 (인덱스 없는 필터)
│   └── PointService.java           Offset 페이징
├── repository/
│   ├── OrderRepository.java        findByUserId
│   ├── OrderItemRepository.java    findByOrderId
│   ├── ProductRepository.java      findByCategoryIdAndStatus
│   └── PointHistoryRepository.java findByUserId(Pageable)
└── dto/
    ├── OrderResponse.java
    ├── ProductResponse.java
    └── PointHistoryResponse.java

ecommerce/src/test/java/com/dblab/ecommerce/repository/
├── OrderRepositoryTest.java        N+1 쿼리 수 단언 ✅ GREEN
├── ProductRepositoryTest.java      Seq Scan 결과 건수 단언 ✅ GREEN
└── PointHistoryRepositoryTest.java Offset 페이지 반환 단언 ✅ GREEN

k6/
├── orders-test.js                  userId 1~1000 균등 랜덤
├── products-test.js                categoryId + status 조합 랜덤
└── points-test.js                  page 가중 분포 (초반 50%/중반 30%/후반 20%)
```

---

## Phase 2 진입 조건

아래 3가지가 충족되면 Phase 2(인덱스 설계)로 넘어간다.

1. ✅ `./gradlew test` 전체 GREEN
2. ⬜ `docs/evidence/phase1/` 스크린샷 4장 저장 완료
3. ⬜ `BASELINE.md` 정량 표 수치 기입 완료
