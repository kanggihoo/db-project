# 이커머스 DB 최적화 학습 로드맵

> **핵심 철학:** "최적화 없는 나이브한 구현"에서 시작하여, 각 Phase에서 **발생하는 문제를 수치로 직접 확인**하고, 그 해결책을 **하나씩** 도입한다.
> 단순히 "개선했다"가 아닌, **k6 + Grafana로 Before/After를 정량적으로 증명**하는 것이 목표다.

---

## 기술 스택 (Tech Stack)

### Backend

- **Framework**: Spring Boot 4.x
- **Build Tool**: Gradle 9.x
- **Language**: Java 21 (LTS)
- **Libraries**: Spring Data JPA, QueryDSL, Micrometer Prometheus
- **Testing**: JUnit 6, Testcontainers, Spring Boot Test, AssertJ

### Infrastructure (Docker Compose)

- **RDBMS**: PostgreSQL 17-alpine
- **Monitoring**: Prometheus (latest), Grafana (latest)
- **Load Testing**: k6 (latest, Docker 이미지 사용)
- **Dummy Data**: Java Faker (100만 건 이상 대용량 데이터 생성)

### Monitoring & Observability

> **모든 Phase에서 Prometheus + Grafana를 상시 가동.**
> 각 Phase의 문제가 Grafana 대시보드에서 수치로 보여야 "인사이트"가 완성된다.

- **Spring Metrics**: Micrometer → Prometheus → Grafana
- **DB Metrics**: postgres-exporter → Prometheus
- **k6 결과**: `--out experimental-prometheus-rw` → Prometheus → Grafana
- **쿼리 분석**: PostgreSQL `pg_stat_statements`, `EXPLAIN ANALYZE`

#### Phase별 핵심 Grafana 패널

| Phase | 핵심 지표                         | 무엇을 보는가                                          |
| ----- | --------------------------------- | ------------------------------------------------------ |
| 0     | DB 커넥션 수, 데이터 건수         | 인프라 정상 가동 + 더미 데이터 삽입 확인               |
| 1     | k6 p95 응답시간, DB 커넥션 수     | 최적화 없는 베이스라인 수치 확보                       |
| 2     | 쿼리 실행시간 (인덱스 전/후)      | 풀스캔 → 인덱스 스캔 ms 차이                           |
| 3     | 발생 쿼리 수 (N+1 전/후)          | 주문 100건 조회 시 쿼리 101번 → 1번                    |
| 4     | 격리 수준별 에러율, 동시 처리 RPS | SERIALIZABLE 직렬화 실패 빈도, 격리 수준별 처리량 변화 |
| 5     | 응답시간, 데이터 전송량           | Entity vs DTO, 단건 루프 vs 벌크 차이                  |
| 6     | 집계 쿼리 실행시간 (인덱스 전/후) | GROUP BY에 인덱스가 미치는 영향                        |
| 7     | p95 응답시간 (페이지 번호별)      | 1페이지 vs 1000페이지 응답시간                         |

### Testing & Verification

- **Integration Test**: JUnit 5 + Testcontainers
- **쿼리 검증**: `@DataJpaTest` + 실행 쿼리 카운트 검증
- **부하 테스트**: k6 시나리오별 VU, 지속 부하

---

## ERD

### 회원 도메인

```
USER
  id, email, password, name, phone, gender(MALE/FEMALE/OTHER), birth_date,
  grade(BRONZE/SILVER/GOLD/VIP), point_balance, created_at, updated_at

USER_ADDRESS
  id, user_id, address, detail_address,
  is_default, receiver_name, receiver_phone
```

### 상품 도메인

```
CATEGORY
  id, parent_id(self-join), name, depth
  └─ parent_id 셀프 조인으로 대/중/소 계층 표현

PRODUCT
  id, category_id, name, description,
  base_price, status(ON_SALE/SOLD_OUT/DISCONTINUED),
  is_deleted(boolean, default false), created_at, updated_at
  └─ is_deleted: Soft Delete용. Phase 2에서 부분 인덱스 실험 대상

PRODUCT_OPTION
  id, product_id, option_name   -- 색상, 사이즈 등 옵션 종류

PRODUCT_OPTION_VALUE
  id, option_id, value          -- 빨강, 파랑, M, L 등 실제 값

PRODUCT_SKU
  id, product_id, sku_code, stock_quantity, extra_price
  └─ 옵션 조합마다 재고와 추가 금액 별도 관리
  └─ CHECK (stock_quantity >= 0) — 음수 재고 방지, Phase 4 격리 수준 실험 전제조건

PRODUCT_SKU_OPTION
  id, sku_id, option_value_id   -- SKU ↔ 옵션값 연결 중간 테이블

PRODUCT_IMAGE
  id, product_id, image_url, is_main, sort_order
```

### 주문 도메인

```
CART
  id, user_id, created_at

CART_ITEM
  id, cart_id, sku_id, quantity, added_at

ORDER
  id, user_id, address_id, used_coupon_id(nullable, USER_COUPON 참조),
  total_price, discount_price, final_price,
  status(PENDING/PAID/PREPARING/SHIPPED/DELIVERED/CANCELLED), created_at
  └─ used_coupon_id: 주문 1건당 쿠폰 1장만 적용 가능

ORDER_ITEM
  id, order_id, sku_id, product_name, option_info,
  quantity, unit_price, status
  └─ 주문 시점 상품명/옵션을 스냅샷 저장 (이후 상품 변경과 무관)

DELIVERY
  id, order_id, status(PREPARING/SHIPPED/DELIVERING/DELIVERED),
  tracking_number, created_at

DELIVERY_TRACKING
  id, delivery_id, status, location, created_at
  └─ 배송 이력이 계속 쌓이는 구조 → 페이지네이션 실험에 최적
```

### 결제 도메인

```
PAYMENT
  id, order_id, method(CARD/KAKAO_PAY/NAVER_PAY),
  amount, status(PENDING/COMPLETED/FAILED/REFUNDED),
  pg_transaction_id, created_at, paid_at
  └─ paid_at: 결제 완료 시각 (created_at은 결제 시도 시각)

REFUND
  id, payment_id, order_item_id, amount, reason, status, created_at
```

### 혜택 도메인

```
COUPON
  id, name, discount_type(RATE/FIXED),
  discount_value, min_order_amount,
  started_at, expired_at,
  max_issue_count, issued_count
  └─ started_at ~ expired_at: 쿠폰 유효 기간
  └─ max_issue_count / issued_count: 선착순 발급 제한

USER_COUPON
  id, user_id, coupon_id, is_used, used_at

POINT_HISTORY
  id, user_id, type(EARN/USE/EXPIRE),
  amount, balance_after, description, created_at
  └─ 계속 쌓이는 이력 테이블 → 페이지네이션 실험에 최적
```

### 리뷰 도메인

```
REVIEW
  id, user_id, product_id, order_item_id,
  rating, content, created_at

REVIEW_IMAGE
  id, review_id, image_url

REVIEW_LIKE
  id, review_id, user_id, created_at
```

### ERD 최적화 포인트 요약

| 포인트            | 테이블                                                              | Phase   |
| ----------------- | ------------------------------------------------------------------- | ------- |
| N+1 대표 케이스   | ORDER → ORDER_ITEM → SKU → PRODUCT → PRODUCT_IMAGE                  | Phase 3 |
| 복합 인덱스 실험  | ORDER(user_id + created_at), PRODUCT(category_id + status)          | Phase 2 |
| 격리 수준 실험    | ORDER(동시 주문 시 Phantom Read), PRODUCT_SKU(재고 읽기 일관성)     | Phase 4 |
| 집계 쿼리 실험    | REVIEW(상품별 평점), ORDER(등급별 구매통계), PRODUCT(카테고리별 수) | Phase 6 |
| Soft Delete 함정  | PRODUCT(is_deleted + 부분 인덱스)                                   | Phase 2 |
| 페이지네이션 실험 | DELIVERY_TRACKING, POINT_HISTORY (계속 쌓이는 이력)                 | Phase 7 |

---

## 문제 → 해결 흐름

```
Phase 0: 프로젝트 초기 셋팅
  └─ 인프라(Docker Compose), 스키마 생성, 더미 데이터 삽입
  └─ 상세: SETUP.md 참고
       │
Phase 1: 나이브한 구현 + 베이스라인 확보
  └─ 발견: 풀스캔, N+1, 느린 페이지네이션 — 베이스라인 수치 확보
       │
Phase 2: 인덱스 설계 + 실행계획 분석
  └─ 해결: 풀스캔 → 인덱스 스캔
  └─ 발견: 복합 인덱스 순서 함정, Soft Delete가 인덱스를 무력화
       │
Phase 3: N+1 + 로딩 전략 최적화
  └─ 해결: 쿼리 수 101개 → 1개
  └─ 발견: 무분별한 Fetch Join의 페이징 경고, 로딩 전략 선택 기준
       │
Phase 4: 트랜잭션 격리 수준
  └─ 발견: 쿼리 수는 줄었는데, 동시 요청 시 데이터 정합성은 어떻게 되는가?
  └─ 해결: 격리 수준에 따라 동일 쿼리의 읽기 결과가 달라지는 현상을 실험
  └─ 발견: SERIALIZABLE은 정합성은 완벽하지만 재시도 로직 필수
       │
Phase 5: 쿼리 최적화 + QueryDSL
  └─ 해결: DTO Projection, 동적 쿼리, 벌크 연산
  └─ 발견: 필요 없는 컬럼까지 가져오는 문제, 동적 조건의 JPQL 한계
       │
Phase 6: 집계 쿼리 최적화
  └─ 해결: GROUP BY + 인덱스 최적화
  └─ 발견: 복잡한 집계 쿼리에서 인덱스가 GROUP BY에 미치는 영향
       │
Phase 7: 페이지네이션 최적화
  └─ 해결: Offset → Cursor 전환, Count 쿼리 분리
  └─ 발견: Cursor 방식의 트레이드오프 (정렬 제약, 구현 복잡도)
```

---

## Phase 0. 프로젝트 초기 셋팅

> 모든 Phase의 전제조건. 최초 1회 실행하면 Docker 볼륨으로 유지된다.

- 인프라 구성, 의존성 설정, 스키마 생성, 더미 데이터 삽입
- **상세 가이드: [SETUP.md](../../../../SETUP.md) 참고**

---

## Phase 1. 나이브한 구현 + 베이스라인 확보

> "최적화 없이 구현하면 수치가 어떻게 나오는가?"

### 전제조건

- Phase 0 완료 (인프라 가동, 더미 데이터 삽입 완료)

### 구현 대상

- ERD 기반 엔티티 구현 (인덱스, 최적화 일절 없이)
- 베이스라인 측정용 API 구현 (최적화 없는 나이브한 코드)
- k6 부하 테스트 시나리오 작성

### 베이스라인으로 측정할 것

| API                              | 측정 지표              | 예상 현상                 |
| -------------------------------- | ---------------------- | ------------------------- |
| 주문 목록 조회                   | 발생 쿼리 수, 응답시간 | N+1 다발                  |
| 상품 검색 (카테고리 + 상태 필터) | 실행계획               | 풀스캔                    |
| 포인트 내역 100페이지            | p95 응답시간           | 뒤로 갈수록 급격히 느려짐 |

### 이 Phase에서 얻는 인사이트

- 아무것도 하지 않았을 때의 정확한 수치 — 이후 Phase의 "Before" 기준선
- 어디서 문제가 터지는지 직접 눈으로 확인

---



# Phase 1. 나이브한 구현 및 베이스라인 측정 가이드

> **목표:** 엔티티 매핑의 "나이브함"을 의도적으로 유지하여 N+1 쿼리, 풀 스캔(Full Scan), 비효율적 페이지네이션 등의 문제가 정확히 시각화되는지 확인하고 **성능 베이스라인 수치(기준점)를 확보**합니다.
> 이후 Phase(2~7)에서 진행할 최적화 작업들이 "몇 % 개선되었는지" 증명하기 위한 샌드백(타겟)을 만드는 핵심 단계입니다.

---

## 0. 사전 준비 및 재현성 확보 (Prerequisites)

베이스라인 측정 수치가 객관적 의미를 가지려면 **"측정 환경과 통제 조건이 항상 동일"**해야 합니다.

### 인프라 및 DB 실행 확인
- **명령어:** `docker-compose up -d`
- **PostgreSQL Extension:** `pg_stat_statements` 확장이 로드되었는지 체크합니다.
- **Grafana 대시보드:** 기본 구성된 Grafana (`http://localhost:3000`)에 접속하여 프로비저닝된 대시보드를 띄워놓습니다.

### 측정 전 주의사항 (변인 통제)
> ⚠️ **성능 측정 전 반드시 확인할 `application.yml` 세팅**
> - **`show_sql: false`**: 로깅 I/O 병목이 수치를 왜곡할 수 있으므로 성능 측정 시 반드시 끕니다. (모든 Phase 공통 통제요소)
> - **`hikari.maximum-pool-size=10`**: HikariCP 커넥션 풀 크기를 10으로 고정하여 DB 세션 병목의 환경을 동일하게 맞춥니다.
> - **`hibernate.generate_statistics=true`**: 쿼리 횟수 확인 및 통계 측정용으로 한정 활용합니다. (로깅 부하가 있으므로 프로덕션에서는 성능 저하를 유발합니다)

- **재현성 체크리스트 (매 부하테스트 직전 수행):** 
  - 더미 데이터 수치 불일치를 방지하기 위해 마스터 데이터 스펙(`SETUP.md`)의 테이블별 Row 목표 건수를 **절대 변경하지 않고** 고정 참조합니다.
  - 실험 전 DB로 접속해 `VACUUM ANALYZE;` 명령을 수행하여 쿼리 플랜 캐시가 일관성을 유지하도록 합니다.
  - 버퍼 캐시 보정을 위해 본격적인 K6 측정 전, API 당 두세 번의 Warm-up 요청을 사전에 날려둡니다.

---

## 1. JPA 엔티티 기본 매핑 (나이브함의 정의)

**나이브(Naive)한 구현**이란 최적화 기법 없이 JPA가 제공하는 기본 설정에 의존하는 상태를 의미합니다.

- **FetchType 기본값 유지:**
  - `@ManyToOne`, `@OneToOne`: 기본 명시대로 `EAGER` 상태를 유지 (원치 않는 즉시 조인 데이터 로드).
  - `@OneToMany`, `@ManyToMany`: 기본 명시대로 `LAZY` 상태를 유지 (Service 연산 접근 시 N+1 유발).
- **최적화 금지:**
  - `@BatchSize`, `@EntityGraph`, `JOIN FETCH`, DB 복합/커버링 인덱스는 일절 적용하지 않습니다.

---

## 2. API 3종 작성 (왜 이 3가지인가?)

이 API 3종은 작성되어 배포된 `RoadMap.md`의 주요 해결 과제들과 역참조(매핑)되도록 고의로 설계되었습니다.

### A. `OrderController`: 주문 목록 조회 API (N+1 기폭제) → **주 타겟: Phase 3 / 부가 타겟: Phase 2**
- **엔드포인트:** `GET /api/orders?userId={userId}`
- **의도적 나이브 구현:** `OrderRepository.findByUserId`로 주문 목록 조회 후, Service에서 `getOrderItem()` 등 LAZY 연관 필드에 루프 처리(`forEach`)로 지속 접근하여 1+N 개의 단위 쿼리가 쏟아지게 유도합니다. (인덱스가 없으므로 Order 조회 시 Phase 2의 현상도 관여합니다.)

### B. `ProductController`: 상품 검색 필터링 API (Table Full Scan 유도) → **주 타겟: Phase 2**
- **엔드포인트:** `GET /api/products?categoryId={categoryId}&status={status}`
- **의도적 나이브 구현:** 인덱스가 전혀 없는 상태에서 대용량 데이터를 필터링하므로, PostgreSQL이 모든 Row를 뒤져보는 `Seq Scan(풀 스캔)`을 시도하게 만들어 DB CPU 부하를 일으킵니다.

### C. `PointController`: 포인트 내역 페이징 API (Offset 낭비 유도) → **주 타겟: Phase 7**
- **엔드포인트:** `GET /api/points?userId={userId}&page={page}&size={size}`
- **의도적 나이브 구현:** Spring Data JPA의 `PageRequest`를 이용해 구현합니다. 뒤로 넘어가는 페이지(`page=1000` 등) 호출 시 데이터를 앞단부터 일일이 세어 넘기다 버리는 비효율적 오프셋 병목을 유도합니다.

---

## 3. TDD 방식 검증: 실제 SQL 수행 횟수(PrepareStatement) 기반 검증

통계 기능을 통해 Hibernate 내부의 가상 논리(JPQL) 횟수가 아닌, **"네트워크를 타고 실제로 DB에 실행된 SQL 개수"**를 정확하게 단언(Assert)합니다.
`findByUserId` 시 카운트 쿼리 등 추가 쿼리가 터질 수 있는 가정을 방어하기 위해 루프 전/후의 SQL 차이값을 계산하여 빈틈없이 검증합니다.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestConfig.class)
class OrderRepositoryTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void 주문조회시_연관관계를_순회하면_추가로_정확히_N개의_쿼리가_찍혀야함() {
        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true); // 테스트 환경에서 통계 명시적 켜기
        statistics.clear(); // 카운트 초기화

        Long targetUserId = 1L;

        // when: User별 Order 조회
        List<Order> orders = orderRepository.findByUserId(targetUserId);
        
        // 추가 쿼리 등 오차를 방어하기 위해 루프 진입 전의 통계 스냅샷 기록
        long sqlCountBeforeInfo = statistics.getPrepareStatementCount();

        // 연관관계 컬렉션을 루프 돌며 순회 (N+1 기폭제)
        for (Order order : orders) {
            order.getOrderItems().size(); 
        }

        // then: 실제 DB 단에서 한방 쿼리 이후 추가된(N) PrepareStatement 개수 카운트
        long extraSqlCount = statistics.getPrepareStatementCount() - sqlCountBeforeInfo;
        
        // Assert: 실제 순회 과정에서 터진 추가 쿼리가 방금 가져온 Order의 건수(N)와 동일한가?
        assertThat(extraSqlCount).isEqualTo(orders.size());
        System.out.println("🔥 연관관계 접근 시마다 쿼리 추가 발생. 총 순회로 터진 SQL 수: " + extraSqlCount);
    }
}
```

---

## 4. k6 부하 시나리오 스펙 (차별화 및 측정 조건 통일)

API 특성에 맞춰 파라미터를 역동적으로 차별화하되, 부하를 일으키는 `VU 50명, 5분 지속` 스펙은 철저히 고정하여 성능 비교의 공정성을 유지합니다.

### k6 공통 부하 스펙
- **Virtual Users (VU):** 50명 고정
- **지속 시간 (Duration):** 5분 고정 (`30s` Ramp-up 증가 포함)
- **타임아웃 처리:** k6 스크립트 작성 시 HTTP 요청에 `timeout: '5s'` 옵션을 반드시 명시합니다. 그 후 응답 시간이 초과된 요청들을 `check()` 로직에서 추려내어 명확히 Fail(`http_req_failed`)로 분류 처리해 에러율을 수집합니다.

### k6 스크립트별 파라미터 분포(Distribution) 차별화
- **`orders-test.js`**: 균등하게 무작위 `userId`를 주입시켜 기존 캐싱 및 동일 파라미터 최적화를 우회하도록 설계합니다.
- **`products-test.js`**: `categoryId`와 `status` 조합을 랜덤으로 주입하되, 여러 조건의 풀스캔이 DB I/O를 직접적으로 찌르도록 구성합니다.
- **`points-test.js`**: 난수 배분을 적용하여 `page=1`(초반부), `page=100`(중반부), `page=1000`(후반부) 요청을 무작위 믹스 호출하게 끔 설정하여 Page Offset 후반부의 병목 현상이 수치로 극렬히 드러나는 분포 시나리오를 작성합니다.

---

## 5. 지표 수집 및 BASELINE.md 기록 (필수 증빙)

### 필수 증빙 스크린샷 저장 규칙
부하 과정에서 모니터링 된 4개의 타겟 화면은 반드시 `docs/evidence/phase1/` 경로에 아래 지정된 명명 규칙으로 저장해야 합니다. (이후 최적화 비교 입증자료로 직결됩니다.)
1. `01_k6_summary.png` (응답 속도, 요청 수, 타임아웃 에러 출력 터미널 창)
2. `02_grafana_p95.png` (전체적인 p95 지연율 파이프 시간순 그래프)
3. `03_pg_stat_statements.png` (Total Exec Time에 기반한 가장 부하가 심한 Seq Scan 탑 쿼리 화면)
4. `04_hikari_active.png` (N+1 풀점유로 인해 커넥션 풀이 Active/Pending 상태로 고갈된 상황 증명화면)

### BASELINE.md 정량 템플릿
테스트가 끝나면 추출된 값으로 프로젝트의 마스터 `BASELINE.md` 파일에 아래 표 템플릿을 채워 넣습니다.

| API 타겟 | p50 (ms) | p95 (ms) | TPS | 에러율(%) | Max Active (풀 상한 10) | 요청 단건당 SQL 발생 횟수 | DB CPU (Max %) | 원인 분석 요약 |
|---|---|---|---|---|---|---|---|---|
| `/api/orders` | - | - | - | - | - | - | - | LAZY 연관 객체 루핑에 따른 1+N SQL 증식 |
| `/api/products`| - | - | - | - | - | - | - | 인덱스 부재로 인한 테이블 전체 Seq Scan |
| `/api/points` | - | - | - | - | - | - | - | 큰 무작위 Offset 페이지 접근으로 인한 누적 병목 |

---

## 6. 완료 조건 (Phase 1 Checklist)

- [ ] (재현환경) `VACUUM ANALYZE` 처리 및 `SETUP.md` 마스터 데이터 건수를 변동 없이 고정했습니까?
- [ ] (변인통제) 성능 측정 기간 중 `show_sql: false`, `hikari.maximum-pool-size=10` 설정을 적용했습니까?
- [ ] (TDD) Hibernate `getPrepareStatementCount()`를 전후로 끊어 측정하여 정확히 N(주문 갯수) 개의 추가 SQL이 발동됨을 엄격히 단언(Assert) 하였습니까?
- [ ] (k6 분포) 각 API의 병목 특성을 극대화하기 위해 페이지 번호 난수 발생 등 파라미터를 섞어 배분하였습니까?
- [ ] (k6 스펙) 모든 API 테스트는 **VU 50, 시간 5분, Ramp-up 30초** 통일 스펙으로 엄격히 구동하였습니까?
- [ ] (증빙 규격화) `01_k6_summary.png` ~ `04_hikari_active.png` 형태의 필수 스크린샷 증빙 4장이 경로에 맞게 보관되었습니까?
- [ ] (문서화 완료) 산출물인 `BASELINE.md` 정량 표 작성이 모두 완료되어 **Phase 2로 넘어갈 요건을 정식으로 충족**했습니까?
