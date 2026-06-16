\pset pager off

\echo 'PHASE_01_PG_STAT_STATEMENTS_TOP_20'
SELECT calls,
       ROUND(mean_exec_time::numeric, 2) AS mean_ms,
       ROUND(total_exec_time::numeric, 2) AS total_ms,
       rows,
       LEFT(query, 240) AS query
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
