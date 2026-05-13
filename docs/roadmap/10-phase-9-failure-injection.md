# 이커머스 DB 최적화 학습 로드맵

## Phase 9. Failure Injection

> "장애를 일부러 만들고, 관측하고, 복구 절차를 문서화한다."

실무에서는 장애가 완전히 사라지지 않는다. 이 Phase의 목표는 DB 장애 상황을 통제된 환경에서 재현하고, 어떤 지표가 먼저 흔들리는지 확인한 뒤, 복구 절차를 runbook으로 남기는 것이다.

### 장애 주입 원칙

장애는 원할 때 켜고 끌 수 있어야 한다. 먼저 SQL 스크립트, k6, Docker 명령으로 재현하고, 반복 실험이 불편해지면 `lab` 프로필 전용 API를 추가한다.

장애 주입 API는 운영 기능이 아니다. `local`, `test`, `lab` 프로필에서만 활성화하고, 운영 프로필에서는 Bean이 생성되지 않아야 한다.

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
  GET /api/lab/failure/slow-query?seconds=30
  POST /api/lab/failure/hold-lock?skuId=1&seconds=60
  POST /api/lab/failure/consume-connection?seconds=30
```

### 장애 시나리오

| 장애 | 유발 방법 | 관측 지표 | 복구 방향 |
| --- | --- | --- | --- |
| 커넥션 고갈 | k6 VU 증가, pool size 제한 | Hikari pending, timeout | pool 조정, 쿼리 시간 단축, rate limit |
| Lock wait | 트랜잭션 열어둔 채 UPDATE | `pg_locks`, `pg_stat_activity.wait_event` | blocking 세션 확인, kill/commit 판단 |
| Deadlock | 두 트랜잭션이 반대 순서로 row lock | PostgreSQL deadlock log | 접근 순서 통일, retry |
| Long transaction | `BEGIN` 후 장시간 유지 | transaction age, vacuum 지연 | idle in transaction 제거 |
| DB restart | PostgreSQL 컨테이너 재시작 | 앱 에러율, reconnect 시간 | retry, readiness, graceful degradation |
| Migration failure | 중간 실패하는 DDL/DML 실행 | Flyway failure, schema drift | rollback 또는 forward fix |

### 시나리오별 발생 방법

#### Connection Pool 고갈

1. Hikari pool size를 10으로 고정한다.
2. `GET /api/lab/failure/slow-query?seconds=30` 또는 `SELECT pg_sleep(30)`을 실행하는 API를 준비한다.
3. k6로 동시 요청을 pool size보다 크게 보낸다.
4. Hikari active connection이 10에 붙고 pending/timeout이 증가하는지 본다.

#### Lock wait

1. 세션 A에서 특정 `product_sku` row를 UPDATE하고 COMMIT하지 않는다.
2. 세션 B에서 같은 row를 UPDATE한다.
3. 세션 B가 대기하는 동안 `pg_stat_activity`, `pg_locks`로 blocking PID를 찾는다.

#### Deadlock

1. 세션 A는 SKU 1번을 잠근 뒤 SKU 2번을 수정한다.
2. 세션 B는 SKU 2번을 잠근 뒤 SKU 1번을 수정한다.
3. PostgreSQL이 deadlock을 감지하고 한 트랜잭션을 abort하는지 확인한다.

#### Long transaction

1. `BEGIN` 후 쿼리를 실행하고 COMMIT하지 않는다.
2. `pg_stat_activity`에서 `idle in transaction`과 transaction age를 확인한다.
3. 장기 트랜잭션이 vacuum과 lock 관측에 어떤 영향을 주는지 확인한다.

#### DB restart

1. k6 요청을 보내는 중 `docker compose restart postgres`를 실행한다.
2. 애플리케이션 에러율, Hikari reconnect, 정상 회복 시간을 측정한다.
3. recovery 후 실패 요청이 재시도 가능한 유형인지 분류한다.

### 단계별 실험

1. 커넥션 풀 고갈을 만든다.
2. Lock wait을 만들고 blocking PID를 찾는다.
3. Deadlock을 재현하고 로그를 확인한다.
4. DB 컨테이너를 재시작하고 애플리케이션 회복 시간을 측정한다.
5. 각 장애별 대응 runbook을 작성한다.

### Deadlock 예시

```sql
-- 세션 A
BEGIN;
UPDATE product_sku SET stock_quantity = stock_quantity - 1 WHERE id = 1;
-- 세션 B가 id=2를 잡을 때까지 대기
UPDATE product_sku SET stock_quantity = stock_quantity - 1 WHERE id = 2;

-- 세션 B
BEGIN;
UPDATE product_sku SET stock_quantity = stock_quantity - 1 WHERE id = 2;
UPDATE product_sku SET stock_quantity = stock_quantity - 1 WHERE id = 1;
```

### Runbook 템플릿

```markdown
# 장애 대응 Runbook: [장애명]

## 증상
- 사용자가 보는 현상
- Grafana에서 보이는 지표

## 확인 쿼리
- pg_stat_activity
- pg_locks
- pg_stat_statements

## 즉시 조치
- 중단할 작업
- kill 가능한 세션 기준
- 재시도/재기동 순서

## 재발 방지
- 코드 변경
- 인덱스/쿼리 변경
- 알림 기준 변경
```

### 장애 시나리오 문서 템플릿

각 장애는 아래 형식으로 문서화한다.

```markdown
# Failure Scenario: [장애명]

## 목적
- 어떤 운영 문제를 재현하는가

## 준비 조건
- 필요한 데이터
- 필요한 설정
- 앱/DB 실행 상태

## 장애 발생 명령
- SQL, k6, Docker 명령 또는 lab API

## 관측할 지표
- Grafana 패널
- PostgreSQL 확인 쿼리

## 복구 명령
- COMMIT/ROLLBACK
- 세션 종료
- 컨테이너 재시작
- 앱 재시작 여부

## 성공 기준
- 의도한 장애가 발생했는가
- 원인 추적이 가능한가
- 복구 후 정상 요청이 성공하는가
```

### 산출물

- `docs/runbooks/connection-pool-exhaustion.md`
- `docs/runbooks/lock-wait.md`
- `docs/runbooks/deadlock.md`
- `docs/runbooks/db-restart.md`
- `docs/failure-scenarios/connection-pool-exhaustion.md`
- `docs/failure-scenarios/lock-wait.md`
- `docs/failure-scenarios/deadlock.md`
- `docs/failure-scenarios/long-transaction.md`
- `docs/failure-scenarios/db-restart.md`
- `scripts/failure/`
- `docs/PHASE9_FAILURE_INJECTION_RESULT.md`

### 완료 조건

- [ ] 커넥션 고갈, Lock wait, Deadlock, DB restart를 각각 재현했다.
- [ ] 각 장애를 원하는 시점에 발생시키는 SQL 스크립트 또는 lab API를 준비했다.
- [ ] 각 장애에서 Grafana와 PostgreSQL 내부 뷰로 원인을 추적했다.
- [ ] 장애별 즉시 조치와 재발 방지책을 runbook으로 작성했다.
- [ ] 장애 후 애플리케이션이 정상 회복되는지 확인했다.
