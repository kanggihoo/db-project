# 이커머스 DB 최적화 학습 로드맵

## Phase 12. Outbox Pattern

> "DB 저장은 성공했는데 이벤트 발행은 실패하면 어떻게 되는가?"

주문 생성이나 결제 완료 후 외부 시스템에 이벤트를 발행해야 할 때, DB 트랜잭션과 메시지 발행은 하나의 원자적 작업이 아니다. 이 Phase에서는 Transactional Outbox로 도메인 상태 변경과 이벤트 저장을 같은 DB 트랜잭션에 묶고, 별도 publisher가 안전하게 이벤트를 발행하도록 만든다.

이 Phase의 목표는 Toxiproxy 같은 네트워크 장애 도구를 도입하는 것이 아니라, DB 저장과 외부 이벤트 발행 사이의 불일치를 구조적으로 막는 것이다. 장애는 publisher 중단, Kafka 중단 또는 fake publisher 실패, 강제 예외, backlog 적체로 재현한다.

### 문제 상황

```text
1. 주문 생성 또는 결제 완료 DB 저장 성공
2. Kafka 또는 외부 이벤트 발행 시도
3. 네트워크 장애로 발행 실패

결과:
DB에는 주문/결제 상태가 변경되어 있지만 외부 시스템은 그 사실을 모름
```

### Outbox 테이블

```sql
CREATE TABLE outbox_event (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);
```

### 처리 흐름

```text
주문 생성 트랜잭션
  ├─ orders insert
  ├─ order_item insert
  └─ outbox_event ORDER_CREATED insert

결제 완료 트랜잭션
  ├─ payments.status = COMPLETED
  ├─ orders.status = PAID
  └─ outbox_event PAYMENT_COMPLETED insert

Outbox publisher
  ├─ PENDING 이벤트 조회
  ├─ 외부 시스템/Kafka로 발행
  ├─ 성공 시 PUBLISHED
  └─ 실패 시 retry_count 증가
```

### Health / Readiness 기준

Outbox 구조에서는 API 서버와 publisher의 의존성을 분리한다. Kafka가 내려가도 API 서버는 DB에 outbox event를 저장할 수 있어야 하며, 이벤트 발행 지연은 publisher 쪽 문제로 격리한다.

| 대상 | READY 기준 | 장애 시 기대 상태 |
| --- | --- | --- |
| api-server | PostgreSQL 연결 가능 | Kafka 장애와 무관하게 READY 유지 |
| outbox-publisher | PostgreSQL 연결 가능, Kafka 또는 fake publisher 발행 가능 | Kafka 장애 시 NOT_READY 또는 DEGRADED |
| outbox backlog | pending count, oldest pending seconds | 오래된 PENDING 이벤트가 alert 후보 |

### 단계별 실험

| # | 실험 | 방법 | 관찰할 것 |
| --- | --- | --- | --- |
| 1 | 이벤트 발행 실패 문제 재현 | 주문 저장 후 publisher 실패 | 데이터 불일치 |
| 2 | Outbox 저장 | 주문과 이벤트를 같은 트랜잭션에 저장 | 원자성 확보 |
| 3 | 결제 완료 이벤트 저장 | `payment COMPLETED`, `order PAID`, `PAYMENT_COMPLETED`를 같은 트랜잭션에 저장 | payment/order/outbox 정합성 |
| 4 | Publisher 구현 | PENDING 이벤트 발행 후 상태 변경 | 재시도 가능성 |
| 5 | 중복 발행 방지 | unique key, idempotent consumer 설계 | at-least-once 처리 |
| 6 | Publisher 재시작 | 중간 실패 후 재실행 | 이어서 처리 |

### 장애 시나리오

| 장애 | 발생 방법 | 기대 동작 |
| --- | --- | --- |
| publisher 중단 | publisher 프로세스 중지 | API는 성공하고 `PENDING` 이벤트가 DB에 남아 재시작 후 발행 |
| Kafka 중단 또는 fake publisher 실패 | Kafka 컨테이너 중지 또는 lab 프로필 실패 모드 | outbox retry가 증가하고 복구 후 재발행 |
| 발행 성공 후 상태 업데이트 실패 | 발행 후 `PUBLISHED` 변경 전 강제 예외 | 중복 발행 가능, consumer idempotency 필요 |
| outbox insert 실패 | 도메인 상태 변경 후 outbox 저장 전 강제 예외 | payment/order 변경도 rollback |
| payload 직렬화 실패 | 잘못된 payload 데이터 저장 또는 serializer 실패 | `FAILED` 상태로 분리하고 원인 기록 |
| 대량 이벤트 적체 | publisher 중단 후 요청 지속 | pending count, oldest pending seconds 증가 |

### Phase 12에서 하지 않는 것

결제 도메인은 Outbox 예시로만 사용한다. PG Mock, PG timeout, webhook 중복/지연, 결제 취소 보상 트랜잭션, DLQ까지 포함한 결제 장애 대응은 별도 결제 프로젝트의 범위로 둔다.

Toxiproxy도 필수 범위가 아니다. 네트워크 latency, timeout, reset을 정교하게 재현해야 할 때 Phase 9의 장애 주입 확장 또는 별도 고급 실험에서 도입한다.

### 산출물

- `docs/PHASE12_OUTBOX_RESULT.md`
- `docs/evidence/phase12/01_outbox_pending.png`
- `docs/evidence/phase12/02_publisher_retry.png`
- `docs/evidence/phase12/03_duplicate_prevention.png`
- `docs/evidence/phase12/04_payment_completed_outbox.png`
- `docs/evidence/phase12/05_publisher_health.png`

### 완료 조건

- [ ] 주문 생성과 outbox event 저장이 같은 트랜잭션에서 처리된다.
- [ ] 결제 완료 시 `payments`, `orders`, `outbox_event` 변경이 같은 트랜잭션에서 처리된다.
- [ ] Kafka 또는 publisher 장애 중에도 API 서버는 DB 저장과 outbox event 저장에 성공한다.
- [ ] api-server와 outbox-publisher의 health/readiness 기준을 분리했다.
- [ ] publisher 장애 후 재시작하면 미발행 이벤트를 이어서 처리한다.
- [ ] outbox insert 실패 시 도메인 상태 변경도 rollback된다.
- [ ] 실패한 이벤트 재발행 정책을 문서화했다.
- [ ] 중복 발행 가능성과 consumer idempotency 필요성을 설명할 수 있다.
- [ ] outbox event 처리 상태와 지연 시간을 모니터링한다.
