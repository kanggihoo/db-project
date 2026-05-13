# 이커머스 DB 최적화 학습 로드맵

## Phase 13. CDC + Kafka

> "DB 변경사항을 애플리케이션 polling 없이 이벤트 스트림으로 흘릴 수 있는가?"

Outbox publisher를 애플리케이션이 직접 polling할 수도 있지만, 운영 환경에서는 Debezium 같은 CDC 도구로 PostgreSQL WAL 변경을 Kafka로 스트리밍할 수 있다. 이 Phase에서는 Postgres WAL, replication slot, Debezium, Kafka topic, consumer replay를 학습한다.

### 핵심 구성요소

| 구성요소 | 역할 |
| --- | --- |
| PostgreSQL WAL | DB 변경 이력 |
| logical replication slot | Debezium이 읽을 변경 스트림 위치 관리 |
| Debezium connector | WAL 변경을 Kafka message로 변환 |
| Kafka topic | outbox event stream 저장 |
| Consumer | 이벤트 처리 및 replay |

### 전체 흐름

```text
orders/outbox_event 변경
  → PostgreSQL WAL
  → Debezium connector
  → Kafka topic
  → consumer
  → 외부 처리 또는 projection 갱신
```

### 단계별 실험

| # | 실험 | 방법 | 관찰할 것 |
| --- | --- | --- | --- |
| 1 | WAL 설정 | `wal_level=logical` 활성화 | replication 가능 여부 |
| 2 | Debezium 연결 | outbox table connector 구성 | topic 생성 |
| 3 | 이벤트 생성 | 주문 생성으로 outbox row insert | Kafka message |
| 4 | Consumer 처리 | topic 구독 후 이벤트 처리 | offset, replay |
| 5 | 장애 복구 | consumer 중단 후 재시작 | 누락 없는 재처리 |
| 6 | 중복 처리 | 같은 이벤트 재처리 | idempotent consumer |

### PostgreSQL 설정 예시

```yaml
postgres:
  command:
    - "postgres"
    - "-c"
    - "wal_level=logical"
    - "-c"
    - "max_replication_slots=4"
    - "-c"
    - "max_wal_senders=4"
```

### Debezium Outbox 방향

권장 흐름은 전체 테이블 CDC가 아니라 `outbox_event` 테이블만 이벤트 소스로 사용하는 것이다.

```text
애플리케이션 트랜잭션
  └─ outbox_event insert

Debezium
  └─ outbox_event insert를 Kafka event로 변환
```

이렇게 하면 도메인 이벤트의 스키마와 발행 기준을 애플리케이션이 통제할 수 있다.

### 장애와 복구 포인트

| 장애 | 확인할 것 | 복구 방향 |
| --- | --- | --- |
| Debezium 중단 | replication slot lag | connector 재시작 |
| Kafka 중단 | connector error, topic lag | Kafka 복구 후 재전송 |
| Consumer 실패 | consumer lag, offset | 재시작 후 replay |
| 중복 이벤트 | event id 중복 | idempotency table |
| WAL 적체 | disk usage, slot lag | connector 복구 또는 slot 정리 |

### 산출물

- `docker-compose.cdc.yml`
- `docs/PHASE13_CDC_KAFKA_RESULT.md`
- `docs/evidence/phase13/01_debezium_connector.png`
- `docs/evidence/phase13/02_kafka_topic.png`
- `docs/evidence/phase13/03_consumer_replay.png`

### 완료 조건

- [ ] PostgreSQL logical replication 설정을 적용했다.
- [ ] Debezium이 `outbox_event` 변경을 Kafka topic으로 발행한다.
- [ ] Consumer 중단 후 재시작해도 이벤트 누락 없이 처리된다.
- [ ] 중복 이벤트 처리 전략을 구현하거나 문서화했다.
- [ ] replication slot lag과 consumer lag을 관측할 수 있다.
