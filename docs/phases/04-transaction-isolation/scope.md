# Phase 4 범위

## 목표

Phase 4의 목표는 PostgreSQL에서 `READ COMMITTED`, `REPEATABLE READ`, `SERIALIZABLE` 격리 수준이 읽기 일관성과 동시 갱신 실패 비용에 어떤 차이를 만드는지 재현하고 기록하는 것이다.

## 대상 시나리오

| 시나리오 | 대상 | 관찰할 것 |
|---|---|---|
| Non-Repeatable Read | 주문 금액 계산 중 상품 가격 변경 | 같은 트랜잭션 안에서 가격 조회 결과가 바뀌는지 |
| Phantom Read | 카테고리별 판매 상품 수 집계 중 상품 추가 | 같은 트랜잭션 안에서 집계 대상 row 수가 바뀌는지 |
| Serializable conflict | 같은 SKU 재고 동시 차감 | serialization failure 빈도와 처리량 |

## 제외 범위

- 비관적 락과 낙관적 락의 실무 전략 비교는 Phase 11에서 다룬다.
- 주문 생성 전체 workflow를 완성하지 않는다.
- 결제, 배송, 쿠폰 정합성은 Phase 4의 직접 범위가 아니다.
- QueryDSL DTO projection 최적화는 Phase 5로 남긴다.

## 완료 조건

- [ ] READ COMMITTED와 REPEATABLE READ에서 Non-Repeatable Read 차이를 재현했다.
- [ ] PostgreSQL REPEATABLE READ의 Phantom Read 방지 특성을 확인했다.
- [ ] SERIALIZABLE에서 동시 갱신 시 Serialization Failure 발생 여부를 측정했다.
- [ ] 격리 수준별 처리량, 실패율, 쿼리 실행시간 차이를 기록했다.
- [ ] 락 전략 비교는 Phase 11 범위로 분리해 문서상 경계를 명확히 했다.
