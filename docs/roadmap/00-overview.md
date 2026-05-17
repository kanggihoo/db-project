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
- **Dummy Data**: DataFaker (100만 건 이상 대용량 데이터 생성)

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
| 8     | Top SQL, active query, lock wait | DB 문제 발생 시 원인 추적 경로                         |
| 9     | 에러율, lock wait, pending conn   | 의도적 장애 발생과 복구 절차                           |
| 10    | migration time, lock time         | 운영 중 스키마 변경의 안전성                           |
| 11    | 성공률, 실패율, 재시도 횟수       | 재고/쿠폰 동시성 처리 정확성                           |
| 12    | outbox lag, retry count           | 이벤트 발행 실패와 재처리 안정성                       |
| 13    | replication lag, consumer lag     | CDC/Kafka 기반 이벤트 스트림 복구성                    |

### Testing & Verification

- **Integration Test**: JUnit 6 + Testcontainers
- **쿼리 검증**: `@DataJpaTest` + 실행 쿼리 카운트 검증
- **부하 테스트**: k6 시나리오별 VU, 지속 부하

---

## 진행 원칙

### 문서 구조

- 전체 문서 인덱스: [docs/README.md](../README.md)
- 반복 실행 가이드: [guides](../guides)
- Phase별 작업 문서: [phases](../phases)
- 측정 증빙: `docs/evidence/`

### ERD 사용 범위

현재 ERD는 유지한다. 복잡한 이커머스 도메인은 현실적인 실험 배경을 제공하지만, 각 Phase에서 모든 테이블을 동시에 다루지는 않는다. Phase마다 대표 테이블을 좁혀서 실험하고, 나머지 테이블은 도메인 맥락과 FK 관계를 제공하는 배경으로 둔다.

| Phase | 대표 테이블 | 실험 목적 |
| --- | --- | --- |
| Phase 2 | `product`, `category` | 조건 검색, 복합 인덱스, soft delete |
| Phase 3 | `orders`, `order_item` | N+1, 로딩 전략 |
| Phase 7 | `point_history`, `delivery_tracking` | offset/cursor pagination |
| Phase 10 | `orders`, `payment` | 운영 중 스키마 변경과 backfill |
| Phase 11 | `product_sku`, `coupon`, `user_coupon` | 재고 차감, 선착순 발급, 동시성 |
| Phase 12~13 | `orders`, `payment`, `outbox_event` | Outbox, CDC, Kafka 이벤트 흐름 |

### k6 사용 시점

k6는 Phase 1부터 사용한다. 단, k6는 정답을 증명하는 첫 도구가 아니라, 테스트와 SQL 분석으로 확인한 문제가 운영 부하에서 어떻게 드러나는지 측정하는 도구다.

검증 순서는 기본적으로 다음을 따른다.

```text
1. 단위/통합 테스트로 정합성 또는 쿼리 수 검증
2. SQL/EXPLAIN/pg_stat_* 뷰로 원인 확인
3. k6로 p95, TPS, 에러율, 커넥션 풀 상태 측정
4. Grafana와 증빙 문서로 Before/After 기록
```

| Phase | k6 사용 목적 |
| --- | --- |
| Phase 1 | 최적화 없는 API 베이스라인 수집 |
| Phase 2 | 인덱스 적용 전/후 상품 검색 응답시간 비교 |
| Phase 3 | N+1 전/후 주문 조회 응답시간과 DB 부하 비교 |
| Phase 7 | offset/cursor 페이지 번호별 p95 비교 |
| Phase 9 | 커넥션 고갈, DB restart 같은 장애 상황 증폭 |
| Phase 11 | 재고/쿠폰 동시 요청으로 충돌 상황 재현 |

### 장애 주입 방식

장애는 원하는 시점에 특정 유형을 재현할 수 있어야 한다. 초기에는 별도 대규모 시스템을 만들지 않고, SQL 스크립트와 k6, Docker 명령으로 시작한다. 반복 실험이 불편해지는 시점에 `lab` 전용 API를 추가한다.

```text
1단계: SQL 스크립트 + k6 + Docker 명령
2단계: lab 프로필 전용 장애 주입 API
3단계: CDC/Kafka Phase에서 docker-compose.cdc.yml 추가
```

장애 주입 API를 만들 경우 반드시 `local`, `test`, `lab` 같은 제한된 프로필에서만 활성화한다. 운영 프로필에는 포함하지 않는다.

권장 구조:

```text
docs/failure-scenarios/
  connection-pool-exhaustion.md
  lock-wait.md
  deadlock.md
  long-transaction.md
  db-restart.md

scripts/failure/
  lock-wait-session-a.sql
  lock-wait-session-b.sql
  deadlock-session-a.sql
  deadlock-session-b.sql
  long-transaction.sql
  restart-db.ps1

ecommerce/src/main/java/.../controller/LabFailureController.java
  /api/lab/failure/slow-query
  /api/lab/failure/hold-lock
  /api/lab/failure/consume-connection
```

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
| 격리 수준 실험    | ORDER(동시 주문 시 Phantom Read), PRODUCT_SKU(SERIALIZABLE 비용 예시) | Phase 4 |
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
       │
Phase 8: DB Observability
  └─ 해결: pg_stat_statements, pg_stat_activity, pg_locks로 문제 추적
  └─ 발견: 장애 대응은 쿼리 지식보다 관측 루틴이 먼저 필요
       │
Phase 9: Failure Injection
  └─ 해결: 커넥션 고갈, Lock wait, Deadlock, DB restart를 의도적으로 재현
  └─ 발견: 장애별 즉시 조치와 재발 방지 runbook이 필요
       │
Phase 10: Production Schema Migration
  └─ 해결: Flyway, expand-contract, backfill, validation query
  └─ 발견: 운영 DB 변경은 DDL 한 줄이 아니라 배포 절차와 검증 문제
       │
Phase 11: Concurrency Control
  └─ 해결: 재고 차감, 쿠폰 발급, 락, retry, idempotency
  └─ 발견: 정합성은 격리 수준만으로 끝나지 않고 비즈니스 제약과 연결됨
       │
Phase 12: Outbox Pattern
  └─ 해결: 주문 생성/결제 완료와 이벤트 저장을 같은 트랜잭션에 묶음
  └─ 발견: Kafka 장애는 API 장애가 아니라 outbox backlog로 격리해야 함
       │
Phase 13: CDC + Kafka
  └─ 해결: Outbox polling publisher를 PostgreSQL WAL, Debezium, Kafka topic으로 확장
  └─ 발견: 이벤트 스트림은 connector lag, consumer replay, idempotency까지 관측해야 운영 가능
```

---

---

## 전체 기술 도입 흐름 요약

| Phase | 핵심 주제          | 새로 도입하는 기술/패턴                              | 해결하는 문제                   | 발견하는 새 문제                   |
| ----- | ------------------ | ---------------------------------------------------- | ------------------------------- | ---------------------------------- |
| 0     | 초기 셋팅          | Docker Compose, Faker 더미데이터, Prometheus+Grafana | -                               | -                                  |
| 1     | 베이스라인         | k6 부하 테스트, 나이브한 API 구현                    | -                               | 풀스캔, N+1, 느린 페이지네이션     |
| 2     | 인덱스 설계        | EXPLAIN ANALYZE, 복합/커버링/부분 인덱스             | 풀스캔                          | 인덱스 순서 함정, Soft Delete 함정 |
| 3     | N+1 + 로딩 전략    | Fetch Join, EntityGraph, BatchSize                   | 쿼리 N번 → 1~5번                | Fetch Join + 페이징 OOM 위험       |
| 4     | 트랜잭션 격리 수준 | 격리 수준 실험, MVCC 스냅샷                          | 동시 요청 시 데이터 정합성      | SERIALIZABLE 재시도 로직 필수      |
| 5     | 쿼리 최적화        | DTO Projection, QueryDSL, 벌크 연산                  | 불필요한 데이터 로딩, 동적 쿼리 | 집계 쿼리 + 인덱스 관계            |
| 6     | 집계 쿼리          | GROUP BY 인덱스, 표현식 인덱스, HashAggregate 분석   | 집계 쿼리 성능                  | 대용량 이력 테이블 페이지네이션    |
| 7     | 페이지네이션       | Cursor 페이지네이션, Count 쿼리 분리                 | Offset 성능 저하                | Cursor 정렬 제약, 구현 복잡도      |
| 8     | DB 관측 가능성     | pg_stat_statements, pg_stat_activity, pg_locks, alert | 문제 원인 추적                  | 장애 대응 runbook 필요             |
| 9     | 장애 주입          | 커넥션 고갈, Lock wait, Deadlock, DB restart          | 장애 재현과 복구 절차 검증      | 자동화된 감지/복구 기준 필요       |
| 10    | 운영 스키마 변경   | Flyway, expand-contract, backfill, validation query   | 운영 중 안전한 DB 변경          | 배포 순서와 rollback 전략          |
| 11    | 동시성 제어        | 비관적 락, 낙관적 락, SERIALIZABLE retry, idempotency | 재고/쿠폰 정합성                | 이벤트 발행 원자성 문제            |
| 12    | Outbox Pattern     | transactional outbox, polling publisher, health/readiness, retry | DB 저장과 이벤트 발행 불일치    | CDC 기반 발행 자동화               |
| 13    | CDC + Kafka        | PostgreSQL WAL, Debezium, Kafka, consumer replay      | Outbox 이벤트 스트리밍          | connector lag, replay, 중복 처리   |

---

---

## 학습 완료 시 답할 수 있는 질문들

> "인덱스 만들었는데 왜 안 타요?"
> → Phase 2에서 복합 인덱스 컬럼 순서 함정과 Soft Delete 무력화를 직접 측정했습니다. `EXPLAIN ANALYZE`로 Seq Scan이 선택된 이유를 확인했습니다.

> "트랜잭션 격리 수준이 뭔가요? 실무에서 뭘 써야 하나요?"
> → Phase 4에서 READ COMMITTED와 REPEATABLE READ에서 주문 중 가격 변경 시나리오를 직접 실험했습니다. PostgreSQL의 MVCC가 락 없이 스냅샷으로 격리하는 방식을 확인했고, SERIALIZABLE의 성능 비용(Serialization Failure 발생률 N%)을 k6로 측정했습니다.

> "N+1이 뭔가요? 어떻게 해결하나요?"
> → Phase 3에서 주문 100건 조회 시 쿼리가 401번 나가는 것을 직접 확인했습니다. Fetch Join, BatchSize 각각 적용 후 쿼리 수와 응답시간을 측정했습니다.

> "Fetch Join 쓰면 되지 않나요?"
> → Fetch Join과 페이징을 같이 쓰면 Hibernate가 전체 데이터를 메모리에 올립니다. Phase 3에서 경고 로그와 메모리 사용량을 직접 확인했고, BatchSize로 해결했습니다.

> "QueryDSL 왜 쓰나요? JPQL로 안 되나요?"
> → Phase 5에서 동적 검색 조건 8가지를 JPQL 문자열로 조합하다 런타임 에러를 경험했습니다. QueryDSL은 컴파일 타임에 오류를 잡아줍니다.

> "GROUP BY 쿼리가 느린데 인덱스를 어떻게 걸어야 하나요?"
> → Phase 6에서 HashAggregate와 GroupAggregate의 차이를 실행계획으로 확인했습니다. GROUP BY 컬럼에 인덱스를 추가했을 때 실행시간이 N% 개선되었고, DATE_TRUNC 같은 함수가 포함되면 표현식 인덱스가 필요함을 확인했습니다.

> "페이지네이션에서 왜 뒤로 갈수록 느려지나요?"
> → Phase 7에서 OFFSET은 앞 데이터를 전부 읽고 버리는 방식임을 `EXPLAIN ANALYZE`로 확인했습니다. 1페이지 Nms vs 1000페이지 Nms로 직접 측정했습니다.

> "Cursor 페이지네이션이 뭔가요?"
> → 마지막으로 읽은 id를 기준으로 다음 데이터를 조회하는 방식입니다. Phase 7에서 1페이지와 1000페이지 응답시간이 동일함을 k6로 증명했습니다.

> "DB 장애가 났을 때 어디서부터 봐야 하나요?"
> → Phase 8~9에서 `pg_stat_activity`, `pg_locks`, `pg_stat_statements`, HikariCP 지표를 연결해 커넥션 고갈, Lock wait, Deadlock, DB restart를 추적하고 runbook으로 정리합니다.

> "운영 중인 DB 스키마는 어떻게 안전하게 바꾸나요?"
> → Phase 10에서 Flyway와 expand-contract migration을 사용해 nullable 컬럼 추가, backfill, validation query, NOT NULL 전환 순서로 기존 데이터 정합성을 유지합니다.

> "동시에 주문하거나 쿠폰을 받으면 정합성은 어떻게 보장하나요?"
> → Phase 11에서 재고 차감과 선착순 쿠폰 발급을 대상으로 비관적 락, 낙관적 락, Atomic UPDATE, SERIALIZABLE retry, idempotency를 비교합니다.

> "DB 저장과 Kafka 이벤트 발행은 어떻게 원자적으로 처리하나요?"
> → Phase 12에서 주문 생성/결제 완료와 `outbox_event` 저장을 같은 DB 트랜잭션에 묶고, Kafka 장애를 API 장애가 아니라 PENDING backlog로 격리합니다. Phase 13에서는 Debezium CDC로 `outbox_event` WAL 변경을 Kafka topic에 흘린 뒤 connector lag, consumer replay, 중복 처리를 검증합니다.
