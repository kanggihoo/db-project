# Phase 7 실행 절차

## 1. Seed 준비

```bash
docker compose down -v
docker compose up -d
./scripts/seed.sh loadtest
```

## 2. 데이터 profile

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

## 4. API 테스트

```bash
cd ecommerce
./gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
./gradlew compileJava
```

## 5. k6 실행

Offset 실행:

```bash
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=page0 PRESET=presets/points-page0.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=mid PRESET=presets/points-mid.json k6 run k6/points-test.js
PHASE=phase-07 SCENARIO=points-offset PRESET_NAME=deep PRESET=presets/points-deep.json k6 run k6/points-test.js
```

Cursor 실행:

```bash
PHASE=phase-07 SCENARIO=points-cursor PRESET_NAME=cursor PRESET=presets/points-cursor.json k6 run k6/points-cursor-test.js
```

## 6. pg_stat_statements

각 API run 직후 SQL 스냅샷을 저장한다.

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/30-pg-stat-statements.sql
```

## 7. 재측정 sampling

Phase 7 재측정은 `user_id=707000`, `size=20`, sample pages `[0,10,50,100,500,1000,2000,3000,4000,4999]`를 사용한다.

현재 `docker-compose.yml`은 repository root를 postgres container의 `/workspace`로 mount하지 않는다. SQL script는 실행 전에 container로 복사한다.

```bash
docker compose cp scripts/phase-07/31-retest-offset-sampling-explain.sql postgres:/tmp/31-retest-offset-sampling-explain.sql
docker compose cp scripts/phase-07/32-retest-cursor-samples.sql postgres:/tmp/32-retest-cursor-samples.sql
docker compose cp scripts/phase-07/33-retest-cursor-next-slice-explain.sql postgres:/tmp/33-retest-cursor-next-slice-explain.sql
docker compose cp scripts/phase-07/34-retest-count-only-explain.sql postgres:/tmp/34-retest-count-only-explain.sql
docker compose cp scripts/phase-07/30-pg-stat-statements.sql postgres:/tmp/30-pg-stat-statements.sql
```

SQL-only 증거 실행:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/31-retest-offset-sampling-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/32-retest-cursor-samples.sql
docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/34-retest-count-only-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v last_created_at='2026-05-26 23:56:40' -v last_id=707000200 -f /tmp/33-retest-cursor-next-slice-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v last_created_at='2026-05-26 18:26:40' -v last_id=707020000 -f /tmp/33-retest-cursor-next-slice-explain.sql
docker compose exec -T postgres psql -U app -d ecommerce -v last_created_at='2026-05-25 20:13:40' -v last_id=707099980 -f /tmp/33-retest-cursor-next-slice-explain.sql
```

k6 sampling 실행:

```bash
docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose run --rm -e PHASE=phase-07 -e SCENARIO=points-offset-sampling -e PRESET_NAME=offset-sampling -e PRESET=presets/points-offset-sampling.json k6 run --out experimental-prometheus-rw /scripts/points-offset-sampling-test.js
docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/30-pg-stat-statements.sql

docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT pg_stat_statements_reset();"
docker compose run --rm -e PHASE=phase-07 -e SCENARIO=points-cursor-sampling -e PRESET_NAME=cursor-sampling -e PRESET=presets/points-cursor-sampling.json k6 run --out experimental-prometheus-rw /scripts/points-cursor-sampling-test.js
docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/30-pg-stat-statements.sql
```

## 8. Phase 7 hot user cleanup

Cleanup은 Phase 7 evidence를 모두 캡처한 뒤에만 실행한다. 같은 Docker volume을 유지한 채 다른 Phase로 이동할 때 `user_id=707000`, Phase 7 전용 `point_history` 증폭 fixture, Phase 7 전용 pagination index만 제거하기 위한 절차다.

postgres container에서 repository root가 `/workspace`로 mount되어 있으면 아래 명령을 실행한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/06-hot-user-cleanup.sql
```

현재 `docker-compose.yml`처럼 `/workspace` mount가 없으면 script를 container로 복사한 뒤 실행한다.

```bash
rtk docker compose cp scripts/phase-07/06-hot-user-cleanup.sql postgres:/tmp/06-hot-user-cleanup.sql
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /tmp/06-hot-user-cleanup.sql
```

cleanup 후에는 Phase 7 fixture가 제거됐는지 확인한다.

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT COUNT(*) AS phase7_point_count FROM point_history WHERE user_id = 707000;"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT COUNT(*) AS phase7_user_count FROM users WHERE id = 707000;"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT COUNT(*) AS phase7_index_count FROM pg_indexes WHERE schemaname = 'public' AND indexname IN ('idx_point_history_created_id', 'idx_point_history_user_created_id');"
```

세 count가 모두 `0`이어야 한다.

Phase 간 상태 격리가 더 중요하면 cleanup SQL 대신 새 volume에서 다시 시작한다.

```bash
rtk docker compose down -v
rtk docker compose up -d
./scripts/seed.sh loadtest
```
