# 이커머스 DB 최적화 학습 로드맵

## Phase 10. Production Schema Migration

> "이미 데이터가 들어 있는 운영 DB를 서비스 중단 없이 바꿀 수 있는가?"

운영 환경에서는 테이블을 비우고 다시 만들 수 없다. 이 Phase에서는 Flyway를 도입하고, 기존 데이터의 정합성을 유지하면서 스키마를 점진적으로 변경하는 방법을 학습한다.

### 핵심 원칙

- DDL은 배포와 분리해서 생각한다.
- 큰 테이블에 한 번에 무거운 변경을 넣지 않는다.
- `expand → backfill → validate → contract` 순서로 진행한다.
- 실패 시 rollback보다 forward fix가 더 안전한 경우가 많다.
- 마이그레이션 전후 검증 쿼리를 반드시 준비한다.

### 대표 시나리오: 주문 결제 상태 컬럼 추가

상황: `orders` 테이블에 이미 수십만 건이 있고, 새 요구사항으로 `payment_status` 컬럼이 필요하다.

나쁜 방식:

```sql
ALTER TABLE orders
ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
```

실무형 방식:

```sql
-- 1. nullable 컬럼 추가
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20);

-- 2. 새 코드는 신규 주문에 payment_status를 기록

-- 3. 기존 데이터 backfill
UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status IS NULL;

-- 4. 검증
SELECT COUNT(*) FROM orders WHERE payment_status IS NULL;

-- 5. NOT NULL 제약 추가
ALTER TABLE orders
ALTER COLUMN payment_status SET NOT NULL;
```

### 단계별 실험

| # | 실험 | 방법 | 관찰할 것 |
| --- | --- | --- | --- |
| 1 | Flyway 도입 | `V1__init.sql`, `V2__add_column.sql` | schema history |
| 2 | Expand migration | nullable 컬럼/테이블 추가 | 기존 코드 영향 없음 |
| 3 | Backfill job | 배치 크기를 나눠 기존 데이터 채움 | 락 시간, 실행 시간 |
| 4 | Validation query | NULL/불일치 데이터 확인 | 정합성 증명 |
| 5 | Contract migration | NOT NULL, old column 제거 | 배포 순서 안정성 |
| 6 | 실패 재현 | 중간 실패 migration 실행 | 복구 방식 결정 |

### Backfill 설계

| 항목 | 기준 |
| --- | --- |
| batch size | 한 번에 1,000~10,000건부터 측정 |
| pause | DB 부하가 높으면 batch 사이 sleep |
| idempotency | 다시 실행해도 이미 처리된 row는 건너뜀 |
| progress | 마지막 처리 ID 또는 처리 건수 기록 |
| validation | 전체 NULL 수, 샘플 row, FK 정합성 확인 |

### 산출물

- `ecommerce/src/main/resources/db/migration/`
- `docs/PHASE10_MIGRATION_PLAN.md`
- `docs/PHASE10_MIGRATION_RESULT.md`
- `docs/evidence/phase10/01_flyway_history.png`
- `docs/evidence/phase10/02_backfill_metrics.png`

### 완료 조건

- [ ] Flyway로 스키마 버전을 관리한다.
- [ ] 이미 데이터가 있는 테이블에 expand-contract 방식으로 컬럼을 추가했다.
- [ ] Backfill job이 중간 실패 후 재실행 가능하다.
- [ ] 마이그레이션 전후 validation query 결과를 문서화했다.
- [ ] 실패한 migration에 대해 rollback/forward fix 기준을 설명할 수 있다.
