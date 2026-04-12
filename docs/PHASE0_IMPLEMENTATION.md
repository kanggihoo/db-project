# Phase 0 구현 완료 가이드

> 이커머스 DB 최적화 학습 프로젝트의 기반 인프라 및 더미 데이터 삽입 파이프라인 구현 내용입니다.

---

## 구현된 파일 목록

### 인프라 (Docker)

| 파일 | 설명 |
|------|------|
| `docker-compose.yml` | PostgreSQL 17, Prometheus, Grafana, postgres-exporter 컨테이너 구성 |
| `docker/postgres/init.sql` | Layer 0~6 전체 DDL (운영 스키마, CHECK 제약, pg_stat_statements) |
| `docker/prometheus/prometheus.yml` | postgres-exporter + Spring Boot Actuator 메트릭 수집 설정 |
| `docker/grafana/provisioning/datasources/prometheus.yml` | Grafana 데이터소스 자동 프로비저닝 |

### Spring Boot 설정

| 파일 | 설명 |
|------|------|
| `ecommerce/src/main/resources/application.yaml` | DB 연결, JPA validate 모드, Actuator/Prometheus 엔드포인트 |
| `ecommerce/src/main/resources/application-seeder.yaml` | 시딩 전용 프로필: `reWriteBatchedInserts=true` (배치 성능 향상) |

### Java 소스 (`com.dblab.ecommerce`)

**Entity (23개)** — JPA 엔티티, DDL의 모든 테이블과 1:1 대응

| 패키지 | 클래스 |
|--------|--------|
| `entity/` | Users, Category, Coupon |
| `entity/` | UserAddress, UserCoupon, Product, PointHistory |
| `entity/` | ProductOption, ProductImage, ProductSku, Cart |
| `entity/` | ProductOptionValue, ProductSkuOption, CartItem, Orders |
| `entity/` | OrderItem, Payment, Delivery |
| `entity/` | Refund, DeliveryTracking, Review |
| `entity/` | ReviewImage, ReviewLike |

**Repository / Seeder**

| 파일 | 설명 |
|------|------|
| `repository/BulkInsertRepository.java` | JDBC Batch Insert 전용. `reserveSequence()` + Layer별 `bulkInsert*()` 메서드 |
| `seeder/DataSeeder.java` | `@Profile("seeder")` CommandLineRunner. Layer 0→6 순서로 더미 데이터 생성 및 삽입 |

### 테스트

| 파일 | 설명 |
|------|------|
| `src/test/java/.../TestcontainersConfiguration.java` | postgres:17-alpine + init.sql 적용하는 Testcontainers 설정 |
| `src/test/java/.../seeder/DataSeederIntegrationTest.java` | 4개 통합 테스트 (TDD GREEN 확인) |
| `src/test/resources/init.sql` | 테스트 전용 DDL (pg_stat_statements 제거, 나머지 운영 DDL 동일) |
| `src/test/resources/application-test.yaml` | 테스트 프로필: show-sql=true, format_sql=true |

---

## 실행 명령어

### 1. 인프라 기동

```bash
# db-project/ 루트에서 실행
cd /Users/kkh/Desktop/db-project

# Docker 컨테이너 기동 (최초 실행 시 init.sql로 스키마 자동 생성)
docker-compose up -d

# 컨테이너 상태 확인
docker-compose ps
```

접속 확인:
- PostgreSQL: `psql -h localhost -U app -d ecommerce` (비밀번호: `app1234`)
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (초기 계정: admin / admin)

### 2. Q클래스 생성 (QueryDSL)

```bash
cd ecommerce/
./gradlew compileJava
# build/generated/sources/annotationProcessor/java/main/ 에 QProduct 등 생성됨
```

### 3. 테스트 실행

```bash
cd ecommerce/

# 전체 테스트 (Testcontainers 자동으로 postgres:17-alpine 컨테이너 띄움)
./gradlew test

# 통합 테스트만 실행
./gradlew test --tests "com.dblab.ecommerce.seeder.DataSeederIntegrationTest"
```

테스트 리포트: `ecommerce/build/reports/tests/test/index.html`

### 4. 더미 데이터 삽입 (seeder 프로필)

```bash
cd ecommerce/

# docker-compose가 먼저 실행 중이어야 합니다
./gradlew bootRun --args='--spring.profiles.active=seeder'

# 이미 데이터가 있으면 자동으로 건너뜀 (멱등성 보장)
# 삽입 완료 후 앱이 자동 종료됩니다
```

### 5. 삽입 완료 검증

```bash
# PostgreSQL 접속
docker exec -it $(docker ps -qf "name=postgres") psql -U app -d ecommerce

# 테이블별 건수 확인
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history;

# FK 정합성 (모두 0이어야 정상)
SELECT 'order_item orphan', COUNT(*)
FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL;

# 분포 확인
SELECT grade, COUNT(*), ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER() * 100, 1) AS pct
FROM users GROUP BY grade ORDER BY pct DESC;
```

### 6. 데이터 초기화

```bash
cd /Users/kkh/Desktop/db-project

docker-compose down -v   # 볼륨까지 삭제 (데이터 완전 초기화)
docker-compose up -d     # init.sql이 다시 실행됨
./gradlew bootRun --args='--spring.profiles.active=seeder'  # 재삽입
```

---

## 현재 데이터 규모 (소량 테스트 모드)

`DataSeeder.java`의 상수로 제어합니다.

| 테이블 | 현재 건수 | 실제 대량 목표 |
|--------|-----------|---------------|
| users | 100 | 100,000 |
| category | 20 | 200 |
| coupon | 10 | 50 |
| product | 200 | 50,000 |
| orders | 500 | 500,000 |
| point_history | 1,000 | 1,000,000 |
| delivery_tracking | 2,000 | 1,000,000 |

대량 삽입이 필요할 때는 `DataSeeder.java` 상단의 상수를 변경하면 됩니다:

```java
private static final int USER_COUNT          = 100_000;  // 소량: 100
private static final int ORDER_COUNT         = 500_000;  // 소량: 500
private static final int POINT_HISTORY_COUNT = 1_000_000; // 소량: 1000
```

---

## 테스트 결과 요약

| 테스트 | 검증 내용 | 결과 |
|--------|-----------|------|
| `reserveSequence_shouldReturnRequestedCountOfUniqueIds` | 시퀀스에서 N개 고유 ID 확보 | ✅ PASS |
| `bulkInsertUsers_shouldInsertAllRows` | 지정한 수만큼 DB에 삽입 | ✅ PASS |
| `fkIntegrity_orderItem_shouldHaveValidOrderId` | order_item의 고아 데이터 없음 | ✅ PASS |
| `userGradeDistribution_shouldHaveMostlyBronze` | BRONZE가 SILVER/GOLD보다 많음 | ✅ PASS |

---

## 제한사항 및 추가 검증 필요 사항

### 1. pg_stat_statements (테스트 환경)
- **현상**: 테스트용 `init.sql`에서 `CREATE EXTENSION IF NOT EXISTS pg_stat_statements` 제거됨
- **이유**: Testcontainers 기본 postgres 이미지는 `shared_preload_libraries`가 설정되지 않아 extension 생성 불가
- **대응**: 운영 환경(docker-compose)에서는 `command` 옵션으로 `shared_preload_libraries=pg_stat_statements` 적용되어 정상 동작
- **검증 필요**: `docker-compose up` 후 `SELECT * FROM pg_stat_statements LIMIT 1;` 실행 확인

### 2. application.yaml의 JPA validate 모드
- `ddl-auto=validate` 설정으로, 엔티티 필드와 DDL 컬럼이 불일치하면 앱 기동 실패
- **검증 필요**: `docker-compose up` 후 `./gradlew bootRun` 실행하여 validate 통과 확인

### 3. 대량 데이터 삽입 시 메모리
- 현재 소량 테스트 모드(100명, 500건)에서는 문제 없음
- 100만 건 삽입 시 JVM 힙 설정 필요: `./gradlew bootRun -Dorg.gradle.jvmargs="-Xmx2g"`
- Layer 완료 후 ID 리스트 `null` 처리로 GC 대상 해제 (DataSeeder 내 구현됨)

### 4. Grafana 대시보드
- postgres-exporter 메트릭은 자동 수집되나, Grafana 대시보드 패널은 수동 구성 필요
- 권장 대시보드 ID: [9628 - PostgreSQL Database](https://grafana.com/grafana/dashboards/9628) (Grafana.com에서 Import)

### 5. review_like UNIQUE 제약
- `(review_id, user_id)` 복합 UNIQUE 제약으로, 중복 좋아요 삽입 시 DB 에러 발생
- DataSeeder에서 `Set<String> seenPairs`로 중복 방지 처리됨
- 대량 삽입 시 충분한 검증 권장

---

## 아키텍처 결정 사항

| 결정 | 이유 |
|------|------|
| `ddl-auto=validate` (not create) | PostgreSQL 고유 기능(CHECK, 부분 인덱스) 보존, 스키마 명시적 관리 |
| JDBC Batch Insert (JPA 우회) | 100만 건 기준 JPA save() 대비 약 10~50배 빠름 |
| ID Pre-allocation (시퀀스 미리 확보) | Layer 간 FK 참조를 위해 INSERT 전에 ID를 알 수 있음 |
| FK 인덱스 없음 (Phase 0 의도적) | Phase 1 베이스라인 측정 → Phase 2에서 Before/After 비교 예정 |
| `reWriteBatchedInserts=true` (seeder 프로필만) | PostgreSQL JDBC 드라이버의 배치 최적화, 시딩에서만 활성화 |
