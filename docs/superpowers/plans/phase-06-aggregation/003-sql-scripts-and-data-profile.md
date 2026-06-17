# 003 SQL Scripts And Data Profile

## Goal

Create numbered SQL scripts that reproduce Phase 6 data profile and SQL-only EXPLAIN evidence conditions.

## Files

- Create: `scripts/phase-06/00-data-profile.sql`
- Create: `scripts/phase-06/10-review-baseline-prepare.sql`
- Create: `scripts/phase-06/11-review-baseline-explain.sql`
- Create: `scripts/phase-06/12-review-naive-prepare.sql`
- Create: `scripts/phase-06/13-review-naive-explain.sql`
- Create: `scripts/phase-06/14-review-query-shaped-prepare.sql`
- Create: `scripts/phase-06/15-review-query-shaped-explain.sql`
- Create: `scripts/phase-06/20-monthly-baseline-prepare.sql`
- Create: `scripts/phase-06/21-monthly-baseline-explain.sql`
- Create: `scripts/phase-06/22-monthly-naive-prepare.sql`
- Create: `scripts/phase-06/23-monthly-naive-explain.sql`
- Create: `scripts/phase-06/24-monthly-query-shaped-prepare.sql`
- Create: `scripts/phase-06/25-monthly-query-shaped-explain.sql`

## Steps

- [ ] **Step 1: Create script directory**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'scripts/phase-06' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 2: Add data profile SQL**

Create `scripts/phase-06/00-data-profile.sql`:

```sql
\echo 'PHASE6_ROW_COUNTS'
SELECT 'product' AS table_name, COUNT(*) AS row_count FROM product
UNION ALL SELECT 'review', COUNT(*) FROM review
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'users', COUNT(*) FROM users
ORDER BY table_name;

\echo 'PHASE6_REVIEWED_PRODUCT_COUNT'
SELECT COUNT(DISTINCT product_id) AS reviewed_product_count
FROM review;

\echo 'PHASE6_REVIEW_COUNT_DISTRIBUTION'
WITH product_review_counts AS (
  SELECT product_id, COUNT(*) AS review_count
  FROM review
  GROUP BY product_id
)
SELECT MIN(review_count) AS min_review_count,
       MAX(review_count) AS max_review_count,
       ROUND(AVG(review_count), 2) AS avg_review_count
FROM product_review_counts;

\echo 'PHASE6_REVIEW_TOP_20_PRODUCTS'
SELECT product_id, COUNT(*) AS review_count
FROM review
GROUP BY product_id
ORDER BY review_count DESC, product_id ASC
LIMIT 20;

\echo 'PHASE6_MONTHLY_ORDER_DISTRIBUTION'
SELECT DATE_TRUNC('month', created_at) AS order_month,
       COUNT(*) AS order_count,
       SUM(final_price) AS total_revenue
FROM orders
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY order_month DESC;

\echo 'PHASE6_INDEX_STATE_BEFORE'
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('product', 'review', 'orders', 'users')
ORDER BY tablename, indexname;
```

- [ ] **Step 3: Add review baseline prepare SQL**

Create `scripts/phase-06/10-review-baseline-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_review_product_id;
DROP INDEX IF EXISTS idx_review_product_rating;

VACUUM (ANALYZE) product;
VACUUM (ANALYZE) review;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 4: Add review baseline explain SQL**

Create `scripts/phase-06/11-review-baseline-explain.sql`:

```sql
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
```

- [ ] **Step 5: Add review naive prepare SQL**

Create `scripts/phase-06/12-review-naive-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_review_product_id;
DROP INDEX IF EXISTS idx_review_product_rating;

CREATE INDEX idx_review_product_id ON review(product_id);

VACUUM (ANALYZE) product;
VACUUM (ANALYZE) review;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 6: Add review naive explain SQL**

Create `scripts/phase-06/13-review-naive-explain.sql` with the same query as `11-review-baseline-explain.sql`:

```sql
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
```

- [ ] **Step 7: Add review query-shaped prepare SQL**

Create `scripts/phase-06/14-review-query-shaped-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_review_product_id;
DROP INDEX IF EXISTS idx_review_product_rating;

CREATE INDEX idx_review_product_rating ON review(product_id, rating);

VACUUM (ANALYZE) product;
VACUUM (ANALYZE) review;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 8: Add review query-shaped explain SQL**

Create `scripts/phase-06/15-review-query-shaped-explain.sql` with the same query as `11-review-baseline-explain.sql`:

```sql
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
```

- [ ] **Step 9: Add monthly baseline prepare SQL**

Create `scripts/phase-06/20-monthly-baseline-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_orders_created_at;
DROP INDEX IF EXISTS idx_orders_month_user;

VACUUM (ANALYZE) orders;
VACUUM (ANALYZE) users;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 10: Add monthly baseline explain SQL**

Create `scripts/phase-06/21-monthly-baseline-explain.sql`:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

- [ ] **Step 11: Add monthly naive prepare SQL**

Create `scripts/phase-06/22-monthly-naive-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_orders_created_at;
DROP INDEX IF EXISTS idx_orders_month_user;

CREATE INDEX idx_orders_created_at ON orders(created_at);

VACUUM (ANALYZE) orders;
VACUUM (ANALYZE) users;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 12: Add monthly naive explain SQL**

Create `scripts/phase-06/23-monthly-naive-explain.sql` with the same query as `21-monthly-baseline-explain.sql`:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

- [ ] **Step 13: Add monthly query-shaped prepare SQL**

Create `scripts/phase-06/24-monthly-query-shaped-prepare.sql`:

```sql
DROP INDEX IF EXISTS idx_orders_created_at;
DROP INDEX IF EXISTS idx_orders_month_user;

CREATE INDEX idx_orders_month_user
ON orders ((DATE_TRUNC('month', created_at)), user_id);

VACUUM (ANALYZE) orders;
VACUUM (ANALYZE) users;

SELECT pg_stat_statements_reset();
```

- [ ] **Step 14: Add monthly query-shaped explain SQL**

Create `scripts/phase-06/25-monthly-query-shaped-explain.sql` with the same query as `21-monthly-baseline-explain.sql`:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT u.grade,
       DATE_TRUNC('month', o.created_at) AS order_month,
       COUNT(o.id) AS order_count,
       SUM(o.final_price) AS total_revenue
FROM orders o
JOIN users u ON u.id = o.user_id
GROUP BY u.grade, DATE_TRUNC('month', o.created_at)
ORDER BY order_month DESC;
```

- [ ] **Step 15: Verify script files exist and contain reset markers**

Run:

```bash
rtk rg -n "VACUUM \\(ANALYZE\\)|pg_stat_statements_reset|EXPLAIN \\(ANALYZE, BUFFERS\\)|idx_review_product|idx_orders_" scripts/phase-06
```

Expected: output includes all prepare and explain scripts.

- [ ] **Step 16: Commit**

```bash
git add scripts/phase-06
git commit -m "test(phase6): add aggregation explain scripts"
```

