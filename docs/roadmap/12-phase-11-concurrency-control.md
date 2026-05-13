# 이커머스 DB 최적화 학습 로드맵

## Phase 11. Concurrency Control

> "동시에 주문하거나 쿠폰을 받을 때 데이터 정합성을 어떻게 지키는가?"

이 Phase는 단순 격리 수준을 넘어서, 실제 비즈니스 동시성 문제를 다룬다. 재고 차감, 선착순 쿠폰 발급, 중복 주문 방지를 대상으로 비관적 락, 낙관적 락, SERIALIZABLE retry, idempotency를 비교한다.

### 실험 대상

| 시나리오 | 깨질 수 있는 정합성 |
| --- | --- |
| 재고 차감 | 재고가 음수가 됨, 초과 판매 |
| 선착순 쿠폰 발급 | 발급 수량 초과 |
| 주문 요청 재시도 | 같은 주문이 중복 생성 |
| 결제 콜백 중복 수신 | 결제 상태 중복 처리 |

### 비교할 전략

| 전략 | 장점 | 단점 | 적합한 경우 |
| --- | --- | --- | --- |
| 비관적 락 | 충돌을 DB에서 직렬화 | 대기/데드락 가능 | 짧고 중요한 갱신 |
| 낙관적 락 | 대기 적음 | 충돌 시 재시도 필요 | 충돌이 드문 갱신 |
| SERIALIZABLE + retry | 정합성 강함 | 실패율/재시도 비용 | 강한 일관성 실험 |
| Atomic UPDATE | 빠르고 단순 | 비즈니스 로직 표현 제한 | 재고 차감 같은 조건부 갱신 |
| Idempotency key | 중복 요청 방지 | 키 저장소/정책 필요 | 결제, 주문 생성 |

### 재고 차감 실험

```sql
-- Atomic UPDATE 방식
UPDATE product_sku
SET stock_quantity = stock_quantity - :quantity
WHERE id = :skuId
  AND stock_quantity >= :quantity;
```

관찰할 것:

- 성공 요청 수
- 실패 요청 수
- 최종 재고
- DB lock wait
- deadlock 발생 여부
- p95 latency

### 선착순 쿠폰 발급 실험

```sql
UPDATE coupon
SET issued_count = issued_count + 1
WHERE id = :couponId
  AND issued_count < max_issue_count;
```

성공한 경우에만 `user_coupon`을 생성한다. 실패 시에는 쿠폰 소진 응답을 반환한다.

### Idempotency 실험

| 항목 | 설계 |
| --- | --- |
| key | 클라이언트가 보낸 `Idempotency-Key` |
| 저장 위치 | `idempotency_request` 테이블 |
| unique constraint | `(user_id, idempotency_key)` |
| 응답 재사용 | 같은 key는 기존 주문 결과 반환 |
| TTL | 실험에서는 수동 정리, 운영에서는 보관 기간 정책 필요 |

### 산출물

- `docs/PHASE11_CONCURRENCY_RESULT.md`
- `docs/evidence/phase11/01_stock_deduction.png`
- `docs/evidence/phase11/02_coupon_issue.png`
- `docs/evidence/phase11/03_idempotency.png`

### 완료 조건

- [ ] 재고 차감에서 초과 판매가 발생하는 나쁜 구현을 재현했다.
- [ ] 비관적 락, 낙관적 락, Atomic UPDATE 중 최소 2개 전략을 비교했다.
- [ ] 선착순 쿠폰 발급에서 `max_issue_count`를 넘지 않음을 증명했다.
- [ ] 중복 주문 요청을 idempotency key로 방지했다.
- [ ] 각 전략의 p95, 성공률, 실패율, lock wait을 비교했다.
