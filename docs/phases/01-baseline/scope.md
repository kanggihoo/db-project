# Phase 1 범위

## 목표

Phase 1의 목표는 최적화 없는 나이브 구현의 Baseline을 측정 가능한 증거로 남기는 것이다.

- 주문 목록 조회에서 N+1 형태의 반복 SQL을 재현한다.
- 상품 검색에서 인덱스 없는 필터 조회 기준선을 확보한다.
- 포인트 내역에서 Offset pagination과 count query 비용을 확인한다.
- k6, Grafana, `pg_stat_statements`로 Phase Evidence를 남긴다.

## 전제 조건

- Phase 0 완료
- `loadtest` seed preset 준비
- Spring profile `pool10` 기준 서버 실행
- k6 metric label: `phase=phase-01`, `pool=pool10`
- Grafana dashboard: `DB Lab Overview`

## 대상 API와 테이블

| API | 주요 테이블 | 관측하려는 문제 |
|---|---|---|
| `GET /api/orders?userId=` | `orders`, `order_item` | 반복 단건 조회와 Hikari pool 점유 |
| `GET /api/products?categoryId=&status=` | `product` | 인덱스 없는 필터 조회 |
| `GET /api/points?userId=&page=&size=` | `point_history` | deep offset과 count query 비용 |

## 의도적 나이브 제약

Phase 1에서는 문제를 보이게 하기 위해 다음 최적화를 적용하지 않는다.

- `@BatchSize`
- `@EntityGraph`
- `JOIN FETCH`
- DTO projection 기반 조회 최적화
- DB 복합 인덱스, 커버링 인덱스, 부분 인덱스
- Cursor pagination

N+1은 JPA 연관관계 최적화 문제가 아니라 서비스 레이어에서 반복적으로 `order_item` 조회를 발생시키는 방식으로 재현한다.

## 구현 범위

| 영역 | 결과 |
|---|---|
| Repository | `OrderRepository`, `OrderItemRepository`, `ProductRepository`, `PointHistoryRepository` |
| Service | `OrderService`, `ProductService`, `PointService` |
| Controller | `OrderController`, `ProductController`, `PointController` |
| Response | `OrderResponse`, `ProductResponse`, `PointHistoryResponse` |
| Test | Repository 테스트와 쿼리 수 검증 |
| k6 | `orders`, `products`, `points` 시나리오와 preset |

## 완료 조건

- [x] 주문 목록 API에서 반복적인 `order_item` 조회가 관측된다.
- [x] 상품 검색 API에서 Phase 2 인덱스 실험의 기준 SQL 시간을 확보한다.
- [x] `points-page0`과 `points-page500`의 비용 차이를 확인한다.
- [x] k6 summary, SQL snapshot, Grafana screenshot이 `docs/evidence/phase-01/` 아래에 저장된다.
- [x] [report.md](./report.md)에 다음 Phase에서 비교할 병목이 정리된다.

## 제외 범위

- Phase 1에서는 병목을 해결하지 않는다.
- Phase 2의 인덱스 설계, Phase 3의 N+1 제거, Phase 7의 pagination 개선은 여기서 구현하지 않는다.
- Evidence 파일의 최종 디렉토리 규칙은 별도 evidence 구조화 작업에서 정리한다.
