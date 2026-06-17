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

DROP INDEX IF EXISTS idx_point_history_created_id;
DROP INDEX IF EXISTS idx_point_history_user_created_id;

COMMIT;

SELECT COUNT(*) AS remaining_phase7_points
FROM point_history
WHERE user_id = :phase7_user_id;

SELECT COUNT(*) AS remaining_phase7_users
FROM users
WHERE id = :phase7_user_id;

SELECT COUNT(*) AS remaining_phase7_indexes
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
      'idx_point_history_created_id',
      'idx_point_history_user_created_id'
  );
