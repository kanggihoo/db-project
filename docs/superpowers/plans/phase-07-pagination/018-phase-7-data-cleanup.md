# 018 Phase 7 Data Cleanup

## Goal

Provide a Phase 7-owned cleanup path for long-lived Docker volume workflows. Phase 7 may add `user_id=707000` and 100,000 `point_history` rows for pagination evidence. When the same DB volume is reused for another Phase, this fixture must be removable without deleting base `loadtest` seed data.

## Files

- Create or verify: `scripts/phase-07/06-hot-user-cleanup.sql`
- Update if needed: `docs/phases/07-pagination/runbook.md`
- Update if needed: `docs/evidence/phase-07/README.md`

## Steps

- [ ] **Step 1: Create cleanup SQL**

Create `scripts/phase-07/06-hot-user-cleanup.sql` with a narrow delete scope:

```sql
\echo 'PHASE7_HOT_USER_CLEANUP'
\set phase7_user_id 707000
\set phase7_point_start_id 707000000
\set phase7_point_count 100000

BEGIN;

DELETE FROM point_history
WHERE user_id = :phase7_user_id
  AND id > :phase7_point_start_id
  AND id <= (:phase7_point_start_id + :phase7_point_count)
  AND description = 'phase7 hot user amplification';

DELETE FROM users
WHERE id = :phase7_user_id
  AND email = 'phase7-hot-user@example.com';

COMMIT;

SELECT COUNT(*) AS remaining_phase7_points
FROM point_history
WHERE user_id = :phase7_user_id;

SELECT COUNT(*) AS remaining_phase7_users
FROM users
WHERE id = :phase7_user_id;
```

- [ ] **Step 2: Document when to run cleanup**

Add the cleanup command to the Phase 7 runbook as an explicit post-evidence step:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/06-hot-user-cleanup.sql
```

Do not run cleanup before Phase 7 evidence is captured. Run it when moving to another Phase while keeping the same Docker volume.

- [ ] **Step 3: Verify cleanup result**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT COUNT(*) AS phase7_point_count FROM point_history WHERE user_id = 707000;"
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT COUNT(*) AS phase7_user_count FROM users WHERE id = 707000;"
```

Expected: both counts are `0`.

- [ ] **Step 4: Preserve stronger isolation option**

Keep `docker compose down -v && docker compose up -d && ./scripts/seed.sh loadtest` documented as the stronger isolation path when a Phase needs a freshly seeded DB.

## Done When

- [ ] `scripts/phase-07/06-hot-user-cleanup.sql` exists.
- [ ] Cleanup deletes only the Phase 7 amplified fixture.
- [ ] The runbook explains cleanup as a post-evidence step for long-lived Docker volume workflows.
- [ ] Verification queries confirm `user_id=707000` and its Phase 7 `point_history` rows are gone after cleanup.
