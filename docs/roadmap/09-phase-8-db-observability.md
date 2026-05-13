# 이커머스 DB 최적화 학습 로드맵

## Phase 8. DB Observability

> "DB에 문제가 생겼을 때 어디서부터 봐야 하는가?"

Phase 0~7에서 쿼리 성능을 개선했다면, 이제는 운영 중인 DB를 관측하는 방법을 학습한다. 목표는 장애가 발생했을 때 감으로 추측하지 않고, PostgreSQL 내부 지표와 애플리케이션 지표를 연결해 원인을 좁히는 것이다.

### 전제

Phase 8은 장애를 크게 만드는 단계가 아니라, 장애를 관측할 준비를 갖추는 단계다. 느린 쿼리, 장기 쿼리, 락 대기, 커넥션 풀 포화가 발생했을 때 어떤 화면과 SQL을 먼저 볼지 고정한다.

이 Phase에서 만든 대시보드와 확인 쿼리는 Phase 9의 장애 주입 실험에서 그대로 사용한다.

### 핵심 관측 도구

| 도구 | 확인하는 것 |
| --- | --- |
| `pg_stat_statements` | 누적 실행시간, 평균 실행시간, 호출 횟수, rows |
| `pg_stat_activity` | 현재 실행 중인 쿼리, 상태, 대기 이벤트, 장기 트랜잭션 |
| `pg_locks` | 락 보유/대기 관계, blocked query |
| PostgreSQL logs | slow query, deadlock, connection failure |
| HikariCP metrics | active, idle, pending, timeout |
| Grafana dashboard | 앱/DB 지표의 시간축 상관관계 |

### 단계별 실험

| # | 실험 | 방법 | 관찰할 것 |
| --- | --- | --- | --- |
| 1 | Slow query 관측 | 인덱스 없는 상품 검색 부하 발생 | `pg_stat_statements.total_exec_time` 상위 쿼리 |
| 2 | Active query 관측 | `pg_sleep`, 장기 SELECT 실행 | `pg_stat_activity.state`, `wait_event` |
| 3 | Lock 관측 | 한 세션에서 UPDATE 후 COMMIT 지연 | `pg_locks`, blocking PID |
| 4 | 커넥션 풀 관측 | VU를 증가시켜 요청 집중 | Hikari active/pending connection |
| 5 | Alert rule 구성 | 임계치 기반 Grafana alert | 장애 감지 기준의 적절성 |

### 핵심 쿼리

```sql
-- 누적 부하가 큰 쿼리
SELECT query, calls, total_exec_time, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 10;

-- 현재 실행 중이거나 대기 중인 쿼리
SELECT pid, state, wait_event_type, wait_event, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state <> 'idle'
ORDER BY duration DESC;

-- 락 상태 확인
SELECT locktype, relation::regclass, mode, granted, pid
FROM pg_locks
ORDER BY granted, pid;
```

### Grafana 패널

| 패널 | 목적 |
| --- | --- |
| HTTP p95 latency | 사용자 관점 지연 확인 |
| Hikari active/pending | 앱 커넥션 풀 병목 확인 |
| DB CPU / I/O | DB 자원 병목 확인 |
| PostgreSQL active sessions | DB 세션 증가 확인 |
| Top SQL latency | 느린 SQL 후보 확인 |
| Lock wait count | 락 대기 발생 여부 확인 |

### 관측 루틴

장애가 의심될 때는 다음 순서로 본다.

```text
1. HTTP p95와 에러율 확인
2. Hikari active/pending/timeout 확인
3. DB active sessions와 CPU/I/O 확인
4. pg_stat_activity로 현재 실행 중인 쿼리 확인
5. pg_locks로 blocking 세션 확인
6. pg_stat_statements로 누적 부하가 큰 쿼리 확인
7. PostgreSQL log에서 deadlock, connection failure, slow query 확인
```

### 산출물

- `docs/evidence/phase8/01_dashboard.png`
- `docs/evidence/phase8/02_pg_stat_statements.png`
- `docs/evidence/phase8/03_pg_stat_activity.png`
- `docs/evidence/phase8/04_pg_locks.png`
- `docs/PHASE8_OBSERVABILITY_RESULT.md`

### 완료 조건

- [ ] `pg_stat_statements`로 가장 비싼 쿼리 Top 10을 설명할 수 있다.
- [ ] `pg_stat_activity`로 장기 실행 쿼리와 대기 상태를 찾을 수 있다.
- [ ] `pg_locks`로 blocked/blocking 세션을 구분할 수 있다.
- [ ] Grafana에서 앱 지연과 DB 지표를 같은 시간축으로 연결해 설명할 수 있다.
- [ ] 최소 3개 이상의 alert rule 초안을 작성했다.
