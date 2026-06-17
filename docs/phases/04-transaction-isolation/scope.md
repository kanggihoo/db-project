# Phase 4 범위

## 목표

Phase 4의 목표는 PostgreSQL에서 `READ COMMITTED`와 `REPEATABLE READ` 격리 수준이 읽기 일관성과 동시 갱신 충돌에 어떤 차이를 만드는지 재현하고 기록하는 것이다. `SERIALIZABLE`은 이번 구현 테스트의 측정 대상이 아니라 문서상 비교 대상으로만 둔다.

## 대상 시나리오

| 시나리오 | 대상 | 관찰할 것 |
|---|---|---|
| Dirty Read | 미커밋 상품 가격 변경 | PostgreSQL에서 커밋 전 변경값이 보이지 않는지 |
| Non-Repeatable Read | 주문 가격 스냅샷 생성 전 live 상품 가격 조회 | 같은 트랜잭션 안에서 가격 조회 결과가 바뀌는지 |
| Phantom Read | 카테고리별 판매 상품 수 집계 중 상품 추가 | 같은 트랜잭션 안에서 집계 대상 row 수가 바뀌는지 |
| Lost Update | 같은 Product SKU row에 대한 naive read-modify-write | REPEATABLE READ에서 조용한 덮어쓰기가 아니라 concurrent update failure가 발생하는지 |

## 제외 범위

- 비관적 락과 낙관적 락의 실무 전략 비교는 Phase 11에서 다룬다.
- Atomic UPDATE 전략 비교는 Phase 11에서 다룬다.
- 주문 생성 전체 workflow를 완성하지 않는다.
- 결제, 배송, 쿠폰 정합성은 Phase 4의 직접 범위가 아니다.
- QueryDSL DTO projection 최적화는 Phase 5로 남긴다.

## 완료 조건

- [x] READ COMMITTED와 REPEATABLE READ에서 Non-Repeatable Read 차이를 재현했다.
- [x] PostgreSQL REPEATABLE READ의 Phantom Read 방지 특성을 확인했다.
- [x] PostgreSQL에서 Dirty Read가 발생하지 않음을 확인했다.
- [x] REPEATABLE READ에서 Lost Update가 조용히 발생하지 않고 concurrent update failure로 방지됨을 확인했다.
- [x] 격리 수준별 결과를 integration test evidence로 기록했다.
- [x] 락 전략 비교는 Phase 11 범위로 분리해 문서상 경계를 명확히 했다.
