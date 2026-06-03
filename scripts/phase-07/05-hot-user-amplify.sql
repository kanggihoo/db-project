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
