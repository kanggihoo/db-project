EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id,
       p.name,
       ROUND(AVG(r.rating), 2) AS avg_rating,
       COUNT(r.id) AS review_count
FROM product p
LEFT JOIN review r ON r.product_id = p.id
GROUP BY p.id, p.name
HAVING COUNT(r.id) >= 10
ORDER BY AVG(r.rating) DESC, p.id ASC
LIMIT 100;
