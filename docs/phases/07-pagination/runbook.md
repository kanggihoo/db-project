# Phase 7 Runbook

## 1. Seed

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## 2. Data profile

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/00-data-profile.sql
```

저장 위치:

- `docs/evidence/phase-07/data-profile/point-history-total-count.txt`
- `docs/evidence/phase-07/data-profile/hot-user-point-counts.txt`
- `docs/evidence/phase-07/data-profile/selected-user-and-pages.txt`

## 3. SQL-only 실행계획

전체 테이블 실험:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/10-global-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/11-global-offset-page0-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/12-global-offset-deep-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v global_deep_offset=100000 -f /workspace/scripts/phase-07/13-global-cursor-deep-explain.sql
```

hot user 실험:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/20-user-index-prepare.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/21-user-offset-page0-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/22-user-offset-deep-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v user_id=1 -v deep_offset=1400 -f /workspace/scripts/phase-07/23-user-cursor-deep-explain.sql
```

## 4. API tests

```bash
cd ecommerce
./gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
./gradlew compileJava
```

## 5. k6

Offset:

```bash
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=page0 PRESET=presets/points-page0.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=mid PRESET=presets/points-mid.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=deep PRESET=presets/points-deep.json k6 run k6/points-test.js
```

Cursor:

```bash
PHASE=phase-07 SCENARIO=points-cursor PRESET_NAME=cursor PRESET=presets/points-cursor.json k6 run k6/points-cursor-test.js
```

## 6. pg_stat_statements

각 API run 직후 snapshot을 저장한다.

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql
```
