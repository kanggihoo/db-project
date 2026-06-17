\echo 'PHASE7_PG_STAT_STATEMENTS_POINT_HISTORY'
SELECT calls,
       ROUND(mean_exec_time::numeric, 2) AS mean_ms,
       ROUND(total_exec_time::numeric, 2) AS total_ms,
       rows,
       query
FROM pg_stat_statements
WHERE query ILIKE '%point_history%'
ORDER BY total_exec_time DESC
LIMIT 20;
