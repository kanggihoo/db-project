# Architecture

## 전체 구조

프로젝트는 로컬 Docker Compose 인프라와 Spring Boot 애플리케이션으로 구성된다.

- PostgreSQL: 운영 스키마와 더미 데이터 저장
- postgres-exporter: PostgreSQL 메트릭 수집
- Prometheus: Spring Actuator, postgres-exporter, k6 메트릭 수집
- Grafana: Phase별 병목과 개선 효과 시각화
- Spring Boot 애플리케이션: API, JPA 엔티티, 시더, 테스트 대상
- k6: Phase별 부하 테스트

## 데이터 모델 책임

- 회원/혜택: `users`, `user_address`, `coupon`, `user_coupon`, `point_history`
- 상품/옵션/SKU: `category`, `product`, `product_option`, `product_option_value`, `product_sku`, `product_sku_option`, `product_image`
- 장바구니: `cart`, `cart_item`
- 주문/결제/배송: `orders`, `order_item`, `payment`, `refund`, `delivery`, `delivery_tracking`
- 리뷰: `review`, `review_image`, `review_like`

## 주요 설계 의도

- 주문 내역은 `order_item`에 상품명, 옵션, 단가를 스냅샷으로 저장해 주문 당시 정보를 보존한다.
- `Delivery`는 최신 배송 상태, `DeliveryTracking`은 append-only 이력으로 분리한다.
- `Product`는 전시 상품, `ProductSku`는 실제 재고 단위로 분리한다.
- `PointHistory`와 `DeliveryTracking`은 계속 쌓이는 이력 테이블로 Phase 7 페이지네이션 실험 대상이다.

## 시딩 아키텍처

- DDL은 Layer 0부터 Layer 6까지 FK 의존성 순서로 생성한다.
- 시더는 `@Profile("seeder")`의 `CommandLineRunner`로 실행한다.
- `BulkInsertRepository`는 JDBC Batch Insert와 시퀀스 ID 사전 할당을 담당한다.
- `DataSeeder`는 Layer 순서대로 ID 리스트를 넘기며 더미 데이터를 생성한다.
- 시딩 완료 후 이미 데이터가 있으면 재삽입을 건너뛰는 멱등성을 가진다.

## Phase 1 API 경계

- `OrderController`/`OrderService`: N+1 재현을 위한 주문 목록 API
- `ProductController`/`ProductService`: 인덱스 없는 필터 조회로 Seq Scan 재현
- `PointController`/`PointService`: Offset 페이징 병목 재현
- Repository 테스트는 구현 정확성과 의도적 병목 발생을 검증한다.

## 위험한 경계

- Phase 2 전에는 인덱스를 추가하면 Phase 1 베이스라인이 오염된다.
- Phase 3 전에는 Fetch Join, BatchSize, EntityGraph를 적용하면 N+1 실험 기준이 흐려진다.
- 시더 데이터 건수를 바꾸면 Phase 간 성능 비교 재현성이 깨질 수 있다.
- Docker 볼륨이 남아 있으면 `init.sql`은 다시 실행되지 않는다.
