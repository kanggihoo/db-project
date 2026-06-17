# 011 Hot User Amplification And Preconditions

## Goal

`user_id=707000`에 100,000건의 `point_history`를 생성하고, Phase 7 재측정의 공통 측정 조건을 고정한다.

## Files

- Verify: `scripts/phase-07/05-hot-user-amplify.sql`
- Verify: `scripts/phase-07/06-hot-user-cleanup.sql`
- Use: `docs/superpowers/plans/phase-07-pagination/006-hot-user-amplification.md`
- Update later: `docs/evidence/phase-07/data-profile/retest-hot-user-profile.txt`

## Steps

- [ ] **Step 1: Verify hot user SQL content**

`scripts/phase-07/05-hot-user-amplify.sql`은 아래 동작을 포함해야 한다.

```sql
\echo 'PHASE7_HOT_USER_AMPLIFY'
\set phase7_user_id 707000
\set phase7_point_start_id 707000000
\set phase7_point_count 100000

INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES (:phase7_user_id, 'phase7-hot-user@example.com', 'pw', 'Phase7 Hot User', 'MALE', 'VIP', 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET email = EXCLUDED.email,
    name = EXCLUDED.name,
    grade = EXCLUDED.grade,
    updated_at = NOW();

INSERT INTO point_history (id, user_id, type, amount, balance_after, description, created_at)
SELECT :phase7_point_start_id + gs,
       :phase7_user_id,
       CASE
           WHEN gs % 3 = 0 THEN 'USE'
           WHEN gs % 3 = 1 THEN 'EARN'
           ELSE 'EXPIRE'
       END,
       100,
       gs * 100,
       'phase7 hot user amplification',
       TIMESTAMP '2026-05-27 00:00:00' - (gs * INTERVAL '1 second')
FROM generate_series(1, :phase7_point_count) AS gs
ON CONFLICT (id) DO NOTHING;

SELECT user_id, COUNT(*) AS point_count
FROM point_history
WHERE user_id = :phase7_user_id
GROUP BY user_id;
```

- [ ] **Step 2: Apply hot user amplification**

Run from repository root:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/05-hot-user-amplify.sql
```

Expected output includes:

```text
 user_id | point_count
---------+-------------
  707000 |      100000
```

- [ ] **Step 3: Verify index for hot user pagination**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -f /workspace/scripts/phase-07/20-user-index-prepare.sql
```

Expected: `idx_point_history_user_created_id` exists on `(user_id, created_at DESC, id DESC)`.

- [ ] **Step 4: Capture retest data profile**

Run:

```bash
rtk docker compose exec -T postgres psql -U app -d ecommerce -c "SELECT user_id, COUNT(*) AS point_count, floor((COUNT(*) - 1) / 20) AS max_page FROM point_history WHERE user_id = 707000 GROUP BY user_id;"
```

Save output to:

```text
docs/evidence/phase-07/data-profile/retest-hot-user-profile.txt
```

Expected values:

```text
user_id=707000
point_count=100000
max_page=4999
```

- [ ] **Step 5: Record measurement constants**

Use these values in all later retest documents and presets:

```text
userId=707000
size=20
pages=[0,10,50,100,500,1000,2000,3000,4000,4999]
warm-cache repeated load
```

- [ ] **Step 6: Verify cleanup contract**

`scripts/phase-07/06-hot-user-cleanup.sql` must remove only the Phase 7 amplified fixture and Phase 7-owned pagination indexes. It must use `user_id=707000`, point id range `707000001..707100000`, and `description='phase7 hot user amplification'` when deleting `point_history`.

Cleanup is not part of evidence capture. Run it only when the same Docker volume will be reused by another Phase.

## Done When

- [ ] `707000` exists in `users`.
- [ ] `point_history` has exactly `100000` rows for `user_id=707000`.
- [ ] `idx_point_history_user_created_id` exists.
- [ ] `scripts/phase-07/06-hot-user-cleanup.sql` exists and is scoped to the Phase 7 fixture and Phase 7 pagination indexes only.
- [ ] `retest-hot-user-profile.txt` is saved under `docs/evidence/phase-07/data-profile/`.
