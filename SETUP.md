# Phase 0. 프로젝트 초기 셋팅 가이드

> 모든 Phase의 전제조건. 최초 1회 실행하면 Docker 볼륨으로 데이터가 유지된다.

---

## 0. 의존성 추가 (build.gradle 참고)

VS Code에서 Q클래스 생성 방법

IntelliJ처럼 IDE가 자동으로 생성해주지 않으므로 터미널에서 직접 실행해야 합니다.

# 프로젝트 루트(ecommerce/)에서

./gradlew compileJava

실행 후 src/main/generated/ 하위에 QProduct.java 등이 생성됩니다.

▎ 주의: 엔티티를 새로 추가하거나 필드를 변경할 때마다 다시 실행해야 Q클래스가 갱신됩니다.

VS Code Java Extension Pack을 쓰는 경우, compileJava 실행 후 생성된 src/main/generated 폴더를 자동으로 source root로
인식합니다 (sourceSets에 이미 추가했으므로).

## 1. 프로젝트 구조

```
db-project/
├── docker-compose.yml
├── docker/
│   ├── postgres/
│   │   └── init.sql              # 스키마 DDL (Docker 최초 기동 시 자동 실행)
│   ├── prometheus/
│   │   └── prometheus.yml        # Prometheus 설정
│   └── grafana/
│       └── provisioning/         # 대시보드, 데이터소스 자동 프로비저닝
├── src/
│   └── main/java/com/example/
│       ├── entity/               # JPA 엔티티
│       ├── repository/           # Spring Data JPA Repository
│       ├── seeder/               # 더미 데이터 삽입 (DataSeeder)
│       └── config/               # DB, JPA 설정
├── k6/                           # k6 부하 테스트 스크립트
├── DB_README.md                  # 학습 로드맵
└── SETUP.md                      # 이 파일
```

---

## 2. 인프라 구성 (Docker Compose)

### 서비스 목록

| 서비스            | 이미지                                | 포트 | 용도                  |
| ----------------- | ------------------------------------- | ---- | --------------------- |
| postgres          | postgres:17-alpine                    | 5432 | RDBMS                 |
| postgres-exporter | prometheuscommunity/postgres-exporter | 9187 | DB 메트릭 수집        |
| prometheus        | prom/prometheus:latest                | 9090 | 메트릭 저장소         |
| grafana           | grafana/grafana:latest                | 3000 | 대시보드              |
| k6                | grafana/k6:latest                     | -    | 부하 테스트 (필요 시) |

### Docker 볼륨

```yaml
volumes:
  postgres-data: # DB 데이터 영속화 — 컨테이너 재시작해도 데이터 유지
  grafana-data: # Grafana 대시보드/설정 유지
  prometheus-data: # Prometheus 메트릭 데이터 유지
```

> **핵심:** 더미 데이터를 한번 삽입하면 `postgres-data` 볼륨에 유지된다. Phase 1~7에서 컨테이너를 재시작해도 데이터는 살아있다.

### PostgreSQL 설정

```yaml
postgres:
  image: postgres:17-alpine
  environment:
    POSTGRES_DB: ecommerce
    POSTGRES_USER: app
    POSTGRES_PASSWORD: app1234
  ports:
    - "5432:5432"
  volumes:
    - postgres-data:/var/lib/postgresql/data
    - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
  command:
    - "postgres"
    - "-c"
    - "shared_preload_libraries=pg_stat_statements" # 쿼리 통계 수집 활성화
    - "-c"
    - "pg_stat_statements.track=all"
```

> `init.sql`은 Docker가 **볼륨이 비어있을 때만** 실행한다. 이미 데이터가 있으면 무시된다.

---

## 3. 스키마 생성 전략

### 방법: `init.sql` + Spring JPA `validate` 조합

| 단계 | 무엇을 하는가                         | 도구                                     |
| ---- | ------------------------------------- | ---------------------------------------- |
| 1    | DDL(테이블, 제약조건, CHECK) 정의     | `docker/postgres/init.sql`               |
| 2    | JPA 엔티티가 스키마와 일치하는지 검증 | `spring.jpa.hibernate.ddl-auto=validate` |

**왜 `init.sql`로 직접 관리하는가:**

- `ddl-auto=create`는 CHECK 제약, 부분 인덱스, 표현식 인덱스를 지원하지 않음
- 스키마를 SQL로 명시하면 PostgreSQL 고유 기능을 활용할 수 있음
- `validate` 모드로 엔티티와 스키마 불일치를 즉시 감지

### init.sql 작성 순서

FK 의존성 때문에 **부모 테이블부터 순서대로** 생성해야 한다.

```sql
-- ============================================
-- Layer 0: 의존성 없는 독립 테이블
-- ============================================
CREATE TABLE users ( ... );
CREATE TABLE category ( ... );
CREATE TABLE coupon ( ... );

-- ============================================
-- Layer 1: Layer 0에 의존
-- ============================================
CREATE TABLE user_address ( ... REFERENCES users(id) );
CREATE TABLE user_coupon ( ... REFERENCES users(id), REFERENCES coupon(id) );
CREATE TABLE product ( ... REFERENCES category(id) );
CREATE TABLE point_history ( ... REFERENCES users(id) );

-- ============================================
-- Layer 2: Layer 1에 의존
-- ============================================
CREATE TABLE product_option ( ... REFERENCES product(id) );
CREATE TABLE product_image ( ... REFERENCES product(id) );
CREATE TABLE product_sku ( ... REFERENCES product(id) );
CREATE TABLE cart ( ... REFERENCES users(id) );

-- ============================================
-- Layer 3: Layer 2에 의존
-- ============================================
CREATE TABLE product_option_value ( ... REFERENCES product_option(id) );
CREATE TABLE product_sku_option ( ... REFERENCES product_sku(id), REFERENCES product_option_value(id) );
CREATE TABLE cart_item ( ... REFERENCES cart(id), REFERENCES product_sku(id) );
CREATE TABLE orders ( ... REFERENCES users(id), REFERENCES user_address(id), REFERENCES user_coupon(id) );

-- ============================================
-- Layer 4: Layer 3에 의존
-- ============================================
CREATE TABLE order_item ( ... REFERENCES orders(id), REFERENCES product_sku(id) );
CREATE TABLE payment ( ... REFERENCES orders(id) );
CREATE TABLE delivery ( ... REFERENCES orders(id) );

-- ============================================
-- Layer 5: Layer 4에 의존
-- ============================================
CREATE TABLE refund ( ... REFERENCES payment(id), REFERENCES order_item(id) );
CREATE TABLE delivery_tracking ( ... REFERENCES delivery(id) );
CREATE TABLE review ( ... REFERENCES users(id), REFERENCES product(id), REFERENCES order_item(id) );

-- ============================================
-- Layer 6: Layer 5에 의존
-- ============================================
CREATE TABLE review_image ( ... REFERENCES review(id) );
CREATE TABLE review_like ( ... REFERENCES review(id), REFERENCES users(id) );

-- ============================================
-- CHECK 제약
-- ============================================
ALTER TABLE product_sku ADD CONSTRAINT chk_stock_non_negative CHECK (stock_quantity >= 0);

-- ============================================
-- pg_stat_statements 활성화
-- ============================================
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

### FK 인덱스 처리 방향

> **⚠️ 의도적 결정:** PostgreSQL은 FK 컬럼에 인덱스를 자동 생성하지 않는다.

Phase 0의 `init.sql`에는 **FK 인덱스를 포함하지 않는다.** 이유:

- Phase 1의 목표는 "최적화 없는 베이스라인" 수치 확보다.
- FK 인덱스가 없는 상태에서 JOIN/DELETE 성능이 얼마나 나쁜지를 Grafana로 직접 확인한다.
- Phase 2에서 인덱스를 추가하며 Before/After를 정량적으로 비교한다.

Phase 2에서 추가할 FK 인덱스 예시:

```sql
-- Phase 2에서 추가 (Phase 1 베이스라인 측정 후)
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_sku_id ON order_item(sku_id);
CREATE INDEX idx_delivery_order_id ON delivery(order_id);
CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_point_history_user_id ON point_history(user_id);
CREATE INDEX idx_review_user_id ON review(user_id);
CREATE INDEX idx_review_product_id ON review(product_id);
CREATE INDEX idx_cart_item_cart_id ON cart_item(cart_id);
CREATE INDEX idx_cart_item_sku_id ON cart_item(sku_id);
CREATE INDEX idx_product_category_id ON product(category_id);
```

> **요약:** Phase 0 = FK 인덱스 없음(의도적). Phase 2 = 위 인덱스 추가 후 성능 비교.

---

## 4. 더미 데이터 삽입 전략

### 핵심 문제: FK 의존성 + 대용량

테이블 간 외래키가 깊게 엮여 있어서, 삽입 순서를 지키지 않으면 FK 위반 에러가 발생한다.

```
Layer 0: users, category, coupon
Layer 1: user_address, user_coupon, product, point_history
Layer 2: product_option, product_image, product_sku, cart
Layer 3: product_option_value, product_sku_option, cart_item, orders
Layer 4: order_item, payment, delivery
Layer 5: refund, delivery_tracking, review
Layer 6: review_image, review_like
```

### 삽입 방식: Spring Boot DataSeeder (CommandLineRunner)

Java Faker + Spring Boot의 `CommandLineRunner`로 애플리케이션 시작 시 더미 데이터를 삽입한다.

```java
@Component
@Profile("seeder")  // --spring.profiles.active=seeder 일 때만 실행
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (isAlreadySeeded()) {
            log.info("데이터 이미 존재 — 삽입 생략");
            SpringApplication.exit(context, () -> 0);
            return;
        }
        seedLayer0();  // users, category, coupon
        seedLayer1();  // user_address, user_coupon, product, point_history
        seedLayer2();  // product_option, product_image, product_sku, cart
        seedLayer3();  // product_option_value, product_sku_option, cart_item, orders
        seedLayer4();  // order_item, payment, delivery
        seedLayer5();  // refund, delivery_tracking, review
        seedLayer6();  // review_image, review_like
        log.info("시딩 완료 — 앱 종료");
        SpringApplication.exit(context, () -> 0);  // 삽입 완료 후 자동 종료
    }

    private boolean isAlreadySeeded() {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users", Long.class);
        return count != null && count > 0;
    }
}
```

### 대용량 삽입 성능 최적화

100만 건 이상을 JPA `save()` 단건으로 넣으면 시간이 과다하게 소요된다.

```java
// ❌ 느린 방식: JPA save() 단건 (건당 INSERT + 영속성 컨텍스트 관리)
for (int i = 0; i < 1_000_000; i++) {
    pointHistoryRepository.save(new PointHistory(...));
}

// ✅ 빠른 방식: JDBC Batch Insert (JPA 우회)
@Repository
public class BulkInsertRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertPointHistory(List<PointHistory> batch) {
        String sql = "INSERT INTO point_history (user_id, type, amount, balance_after, description, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PointHistory ph = batch.get(i);
                ps.setLong(1, ph.getUserId());
                ps.setString(2, ph.getType().name());
                ps.setInt(3, ph.getAmount());
                ps.setInt(4, ph.getBalanceAfter());
                ps.setString(5, ph.getDescription());
                ps.setTimestamp(6, Timestamp.valueOf(ph.getCreatedAt()));
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }
}
```

### 배치 사이즈 가이드

| 테이블              | 목표 건수 | 배치 사이즈 | 예상 배치 수 |
| ------------------- | --------- | ----------- | ------------ |
| users               | 100,000   | 5,000       | 20           |
| product             | 50,000    | 5,000       | 10           |
| orders + order_item | 500,000   | 5,000       | 100          |
| delivery_tracking   | 1,000,000 | 10,000      | 100          |
| point_history       | 1,000,000 | 10,000      | 100          |

> **배치 사이즈 5,000~10,000이 적정.** 너무 크면 메모리 부족, 너무 작으면 DB 왕복이 많아짐.

### FK 제약 일시 비활성화 (선택적)

삽입 순서를 지키면 FK 비활성화 없이도 삽입 가능하지만, 삽입 속도를 더 높이고 싶다면:

```sql
-- 삽입 전: FK 체크 비활성화 (세션 단위)
SET session_replication_role = 'replica';

-- 대량 삽입 실행 ...

-- 삽입 후: FK 체크 복원
SET session_replication_role = 'origin';

-- FK 정합성 검증 (삽입 후 반드시 확인)
-- 고아 데이터가 없는지 확인하는 쿼리 실행
SELECT COUNT(*) FROM order_item oi
  LEFT JOIN orders o ON o.id = oi.order_id
  WHERE o.id IS NULL;  -- 0건이어야 정상
```

> **주의:** FK 비활성화 시 삽입 순서를 안 지켜도 되지만, 삽입 후 반드시 정합성 검증을 해야 한다. 순서를 지키면서 JDBC Batch Insert를 쓰는 것만으로도 충분히 빠르므로, FK 비활성화는 정말 필요할 때만 사용한다.

> **⚠️ 커넥션 풀 주의사항:** `SET session_replication_role`은 **세션(커넥션) 단위**로 적용된다. Spring Boot의 `JdbcTemplate`은 커넥션 풀(`HikariCP`)을 사용하므로, 단순히 두 번 `execute()`를 호출하면 서로 다른 커넥션에서 실행될 수 있다. 반드시 **단일 커넥션을 명시적으로 확보**한 상태에서 실행해야 한다.
>
> ```java
> // ❌ 잘못된 방식: 두 execute()가 서로 다른 커넥션을 쓸 수 있음
> jdbcTemplate.execute("SET session_replication_role = 'replica'");
> bulkInsert();
> jdbcTemplate.execute("SET session_replication_role = 'origin'");
>
> // ✅ 올바른 방식: DataSourceUtils로 단일 커넥션 확보
> Connection conn = DataSourceUtils.getConnection(dataSource);
> try {
>     conn.createStatement().execute("SET session_replication_role = 'replica'");
>     // bulk insert using conn directly ...
>     conn.createStatement().execute("SET session_replication_role = 'origin'");
> } finally {
>     DataSourceUtils.releaseConnection(conn, dataSource);
> }
> ```

---

## 5. 데이터 간 관계 연결 전략

더미 데이터에서 가장 까다로운 부분은 **테이블 간 ID 참조**다.

### 전략: 생성 시 ID 수집 → 다음 Layer에서 참조

```java
public class DataSeeder implements CommandLineRunner {

    // Layer 0에서 생성된 ID를 저장
    private List<Long> userIds;
    private List<Long> categoryIds;
    private List<Long> couponIds;

    private void seedLayer0() {
        userIds = bulkInsertUsers(100_000);       // INSERT 후 생성된 ID 리스트 반환
        categoryIds = bulkInsertCategories(200);
        couponIds = bulkInsertCoupons(50);
    }

    private void seedLayer1() {
        // userIds에서 랜덤 선택하여 FK 연결
        bulkInsertUserAddresses(userIds);
        bulkInsertUserCoupons(userIds, couponIds);

        productIds = bulkInsertProducts(categoryIds, 50_000);
        bulkInsertPointHistory(userIds, 1_000_000);
    }

    private void seedLayer3() {
        // orders 생성 시 userIds, addressIds, userCouponIds에서 랜덤 선택
        // 전체 주문 중 30%만 쿠폰 사용 (나머지는 used_coupon_id = null)
        orderIds = bulkInsertOrders(userIds, addressIds, userCouponIds, 500_000);
        // ...
    }
}
```

### JDBC Batch Insert에서 생성된 ID 수집

```java
public List<Long> bulkInsertUsers(int count) {
    // 방법 1: RETURNING 절 사용 (PostgreSQL)
    String sql = "INSERT INTO users (...) VALUES (...) RETURNING id";

    // 방법 2: 시퀀스 범위 미리 확보
    // SELECT nextval('users_id_seq') FROM generate_series(1, 100000)
    // → ID를 미리 알고 있으므로 삽입 후 수집 불필요
}
```

> **추천: 방법 2 (시퀀스 범위 미리 확보)**
>
> - ID를 미리 확보하면 INSERT 시 ID를 직접 지정할 수 있음
> - 다음 Layer에서 참조할 ID를 삽입 전에 이미 알고 있음
> - RETURNING으로 대량 ID를 받는 것보다 메모리 효율이 좋음

**방법 2 구체적 구현:**

```java
// 시퀀스 N개를 미리 확보하여 ID 리스트 반환
public List<Long> reserveSequence(String sequenceName, int count) {
    String sql = "SELECT nextval('" + sequenceName + "') " +
                 "FROM generate_series(1, " + count + ")";
    return jdbcTemplate.queryForList(sql, Long.class);
}

// Layer 0: ID를 미리 확보 후 INSERT에 직접 지정
private void seedLayer0() {
    userIds = reserveSequence("users_id_seq", 100_000);
    // userIds를 INSERT의 id 컬럼에 직접 사용
    bulkInsertUsers(userIds);

    categoryIds = reserveSequence("category_id_seq", 200);
    bulkInsertCategories(categoryIds);

    couponIds = reserveSequence("coupon_id_seq", 50);
    bulkInsertCoupons(couponIds);
}

// Layer 1: Layer 0의 ID를 FK로 참조
private void seedLayer1() {
    productIds = reserveSequence("product_id_seq", 50_000);
    bulkInsertProducts(productIds, categoryIds);  // categoryIds 참조

    bulkInsertPointHistory(userIds, 1_000_000);   // userIds 참조

    // Layer 1 완료 후 더 이상 필요 없는 리스트 해제 (메모리 절약)
    // categoryIds와 couponIds는 Layer 3까지 필요하므로 유지
}
```

### ID 리스트 메모리 해제 전략

Layer 완료 후 더 이상 참조하지 않는 ID 리스트는 즉시 `null`로 해제한다. 모든 Layer ID를 동시에 유지하면 최대 수백 MB의 힙을 점유한다.

| Layer 완료 시점 | 해제 가능한 리스트                         | 이유                   |
| --------------- | ------------------------------------------ | ---------------------- |
| Layer 1 완료    | `categoryIds`                              | Layer 2 이후 사용 없음 |
| Layer 3 완료    | `couponIds`, `addressIds`, `userCouponIds` | Layer 4 이후 사용 없음 |
| Layer 4 완료    | `orderIds`, `skuIds`                       | Layer 5 이후 사용 없음 |
| Layer 5 완료    | `userIds`, `productIds`                    | Layer 6 이후 사용 없음 |

```java
private void seedLayer1() {
    // ...
    categoryIds = null;  // Layer 2부터 불필요 — GC 대상으로 해제
}

private void seedLayer4() {
    // ...
    couponIds = null;
    addressIds = null;
    userCouponIds = null;
}
```

> **대안:** `userIds`처럼 크기가 큰(100,000건) 리스트를 Layer 전반에 걸쳐 참조해야 한다면, 메모리에 유지하는 대신 DB에서 `SELECT id FROM users ORDER BY RANDOM() LIMIT n`으로 직접 샘플링하는 방식도 고려한다.

---

## 6. 더미 데이터 현실성 가이드

단순 랜덤이 아닌, 이커머스에 맞는 분포를 적용해야 이후 Phase의 실험 결과가 의미 있다.

### 데이터 분포

| 테이블             | 분포 전략                                                                      |
| ------------------ | ------------------------------------------------------------------------------ |
| users.grade        | BRONZE 60%, SILVER 25%, GOLD 10%, VIP 5%                                       |
| users.gender       | MALE 48%, FEMALE 48%, OTHER 4%                                                 |
| product.status     | ON_SALE 80%, SOLD_OUT 15%, DISCONTINUED 5%                                     |
| product.is_deleted | false 95%, true 5% (Soft Delete 실험용)                                        |
| order.status       | DELIVERED 50%, SHIPPED 15%, PREPARING 10%, PAID 10%, PENDING 10%, CANCELLED 5% |
| coupon 사용율      | 전체 주문의 30%만 쿠폰 적용                                                    |

### 시간 분포

| 데이터                       | 기간       | 이유                         |
| ---------------------------- | ---------- | ---------------------------- |
| users.created_at             | 최근 3년   | 신규/구 회원 비율 차이       |
| orders.created_at            | 최근 2년   | 월별 통계 집계 실험용        |
| point_history.created_at     | 최근 1년   | 페이지네이션에서 충분한 깊이 |
| delivery_tracking.created_at | 최근 6개월 | 이력이 밀집된 최근 데이터    |

---

## 7. 삽입 완료 검증 쿼리

데이터 삽입 후 반드시 실행하여 정합성을 확인한다.

```sql
-- 1. 테이블별 건수 확인
SELECT 'users' AS tbl, COUNT(*) FROM users
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_item', COUNT(*) FROM order_item
UNION ALL SELECT 'delivery_tracking', COUNT(*) FROM delivery_tracking
UNION ALL SELECT 'point_history', COUNT(*) FROM point_history;

-- 2. FK 정합성 확인 (고아 데이터 없는지)
SELECT 'order_item orphan' AS check_name, COUNT(*)
FROM order_item oi LEFT JOIN orders o ON o.id = oi.order_id WHERE o.id IS NULL
UNION ALL
SELECT 'payment orphan', COUNT(*)
FROM payment p LEFT JOIN orders o ON o.id = p.order_id WHERE o.id IS NULL
UNION ALL
SELECT 'delivery orphan', COUNT(*)
FROM delivery d LEFT JOIN orders o ON o.id = d.order_id WHERE o.id IS NULL;

-- 3. CHECK 제약 검증
SELECT COUNT(*) AS negative_stock FROM product_sku WHERE stock_quantity < 0;
-- 결과: 0이어야 정상

-- 4. 데이터 분포 확인
SELECT grade, COUNT(*), ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER() * 100, 1) AS pct
FROM users GROUP BY grade ORDER BY pct DESC;

SELECT status, COUNT(*), ROUND(COUNT(*)::numeric / SUM(COUNT(*)) OVER() * 100, 1) AS pct
FROM product GROUP BY status ORDER BY pct DESC;
```

---

## 8. 실행 방법 요약

```bash
# 1. 인프라 기동
docker-compose up -d

# 2. Grafana 접속 확인 (초기 admin/admin)
open http://localhost:3000

# 3. Spring Boot 앱 실행 (seeder 프로필로 더미 데이터 삽입)
./gradlew bootRun --args='--spring.profiles.active=seeder'

# 4. 삽입 완료 후 검증
docker exec -it postgres psql -U app -d ecommerce -f /tmp/verify.sql

# 5. 이후 Phase에서는 seeder 프로필 없이 실행
./gradlew bootRun
```

> **데이터 초기화가 필요한 경우:**
>
> ```bash
> docker-compose down -v   # 볼륨까지 삭제
> docker-compose up -d     # init.sql이 다시 실행됨
> # seeder 프로필로 다시 삽입
> ```

---

## 9. 체크리스트

| #   | 확인 사항                                         | 완료 |
| --- | ------------------------------------------------- | ---- |
| 1   | `docker-compose up -d` 후 모든 컨테이너 정상 가동 | [ ]  |
| 2   | PostgreSQL 접속 가능 (`psql -U app -d ecommerce`) | [ ]  |
| 3   | `pg_stat_statements` 확장 활성화 확인             | [ ]  |
| 4   | Prometheus → PostgreSQL exporter 메트릭 수집 확인 | [ ]  |
| 5   | Grafana 대시보드 접속 + 데이터소스 연결 확인      | [ ]  |
| 6   | 더미 데이터 삽입 완료 (건수 확인)                 | [ ]  |
| 7   | FK 정합성 검증 쿼리 통과                          | [ ]  |
| 8   | 데이터 분포 확인 (grade, status 비율)             | [ ]  |
