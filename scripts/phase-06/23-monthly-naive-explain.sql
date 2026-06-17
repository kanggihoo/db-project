EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
