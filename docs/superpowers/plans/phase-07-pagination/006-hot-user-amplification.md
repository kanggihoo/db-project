# 006 Hot User Amplification

## Goal

Phase 7 Cursor/Offset 차이를 더 명확히 보기 위해 기존 `loadtest` seed를 유지한 채 실험용 hot user와 100,000건의 `point_history`를 추가한다.

## Files

- Create: `scripts/phase-07/05-hot-user-amplify.sql`
- Modify: `k6/presets/points-page0.json`
- Modify: `k6/presets/points-mid.json`
- Modify: `k6/presets/points-deep.json`
- Modify: `k6/presets/points-cursor.json`
- Modify: `docs/phases/07-pagination/report.md`

## Steps

- [ ] **Step 1: Create hot user amplification SQL**

Create `scripts/phase-07/05-hot-user-amplify.sql`:

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

- [ ] **Step 2: Apply amplification SQL**

Run:

```bash
docker compose exec -T postgres psql -U app -d ecommerce < scripts/phase-07/05-hot-user-amplify.sql
```

Expected: `user_id=707000` has `point_count=100000`.

- [ ] **Step 3: Update k6 presets**

Set Offset presets to `userStart=707000`, `userEnd=707000`, and use `page=0`, `page=2499`, `page=3999`.

Set Cursor preset to `userId=707000`.

- [ ] **Step 4: Re-capture focused evidence**

Run SQL-only hot user explain with `deep_offset=79980`, then run k6 Offset page0/mid/deep and Cursor.

- [ ] **Step 5: Update report**

Add an amplified hot user section to `docs/phases/07-pagination/report.md` that records:

- selected user `707000`
- point count `100000`
- maxPage `4999`
- midPage `2499`
- deepPage `3999`
- why this evidence supersedes the earlier natural hot user run for pagination contrast

- [ ] **Step 6: Commit**

```bash
git add scripts/phase-07/05-hot-user-amplify.sql k6/presets/points-page0.json k6/presets/points-mid.json k6/presets/points-deep.json k6/presets/points-cursor.json docs/phases/07-pagination/report.md docs/superpowers/plans/phase-07-pagination/006-hot-user-amplification.md
git commit -m "docs(phase7): add hot user amplification plan"
```
