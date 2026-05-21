SELECT calls,
       round(mean_exec_time::numeric, 2) AS mean_ms,
       round(total_exec_time::numeric, 2) AS total_ms,
       rows,
       left(query, 220) AS query
FROM pg_stat_statements
WHERE query ILIKE '%orders%'
   OR query ILIKE '%order_item%'
   OR query ILIKE '%product_sku%'
   OR query ILIKE '%product_image%'
   OR query ILIKE '% from product %'
ORDER BY total_exec_time DESC
LIMIT 30;
