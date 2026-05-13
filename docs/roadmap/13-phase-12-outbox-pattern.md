# 이커머스 DB 최적화 학습 로드맵

## Phase 12. Outbox Pattern

> "DB 저장은 성공했는데 이벤트 발행은 실패하면 어떻게 되는가?"

주문 생성 후 외부 시스템에 이벤트를 발행해야 할 때, DB 트랜잭션과 메시지 발행은 하나의 원자적 작업이 아니다. 이 Phase에서는 Transactional Outbox로 주문 저장과 이벤트 저장을 같은 DB 트랜잭션에 묶고, publisher가 안전하게 이벤트를 발행하도록 만든다.

### 문제 상황

```text
1. 주문 INSERT 성공
2. Kafka 또는 외부 이벤트 발행 시도
3. 네트워크 장애로 발행 실패

결과:
DB에는 주문이 있지만 외부 시스템은 주문 생성 사실을 모름
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
  └─ outbox_event insert

Outbox publisher
  ├─ PENDING 이벤트 조회
  ├─ 외부 시스템/Kafka로 발행
  ├─ 성공 시 PUBLISHED
  └─ 실패 시 retry_count 증가
```

### 단계별 실험

| # | 실험 | 방법 | 관찰할 것 |
| --- | --- | --- | --- |
| 1 | 이벤트 발행 실패 문제 재현 | 주문 저장 후 publisher 실패 | 데이터 불일치 |
| 2 | Outbox 저장 | 주문과 이벤트를 같은 트랜잭션에 저장 | 원자성 확보 |
| 3 | Publisher 구현 | PENDING 이벤트 발행 후 상태 변경 | 재시도 가능성 |
| 4 | 중복 발행 방지 | unique key, idempotent consumer 설계 | at-least-once 처리 |
| 5 | Publisher 재시작 | 중간 실패 후 재실행 | 이어서 처리 |

### 장애 시나리오

| 장애 | 기대 동작 |
| --- | --- |
| publisher 중단 | `PENDING` 이벤트가 DB에 남아 재시작 후 발행 |
| 발행 성공 후 상태 업데이트 실패 | 중복 발행 가능, consumer idempotency 필요 |
| payload 직렬화 실패 | `FAILED` 상태로 분리하고 원인 기록 |
| 대량 이벤트 적체 | batch size, polling interval 조정 |

### 산출물

- `docs/PHASE12_OUTBOX_RESULT.md`
- `docs/evidence/phase12/01_outbox_pending.png`
- `docs/evidence/phase12/02_publisher_retry.png`
- `docs/evidence/phase12/03_duplicate_prevention.png`

### 완료 조건

- [ ] 주문 생성과 outbox event 저장이 같은 트랜잭션에서 처리된다.
- [ ] publisher 장애 후 재시작하면 미발행 이벤트를 이어서 처리한다.
- [ ] 실패한 이벤트 재발행 정책을 문서화했다.
- [ ] 중복 발행 가능성과 consumer idempotency 필요성을 설명할 수 있다.
- [ ] outbox event 처리 상태와 지연 시간을 모니터링한다.
