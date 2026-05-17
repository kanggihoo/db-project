# Phase 1 Baseline Evidence

## Environment

- Date: 2026-05-17
- Seed preset: `loadtest`
- Spring profile: `pool10`
- k6 labels: `phase=phase-01`, `pool=pool10`

## Data Scale

| Table | Rows |
|---|---:|
| `users` | 10,000 |
| `product` | 100,000 |
| `orders` | 500,000 |
| `order_item` | 1,000,000 |
| `point_history` | 2,000,000 |
| `delivery_tracking` | 1,000,000 |

## k6 Summary

| Scenario | Preset | Requests | RPS | Failed | p95 | Dropped |
|---|---|---:|---:|---:|---:|---:|
| `orders` | `baseline` | 14,844 | 48.67/s | 100.00% | 5s | 157 |
| `products` | `baseline` | 15,001 | 50.00/s | 0.00% | 17.24ms | 0 |
| `points` | `points-page0` | 14,835 | 48.64/s | 60.44% | 5s | 166 |
| `points` | `points-page500` | 14,766 | 48.41/s | 99.16% | 5s | 235 |

## SQL Evidence

### Orders

`orders baseline` reproduced an N+1 shaped query pattern.

| Query | Calls | Mean | Total |
|---|---:|---:|---:|
| `order_item where order_id = ?` | 36,631 | 87.72ms | 3,213,308.15ms |
| `orders where user_id = ?` | 110 | 78.42ms | 8,626.39ms |

Evidence files:

- `orders/pool10-baseline/k6-summary.txt`
- `orders/pool10-baseline/pg-stat-statements.txt`
- `grafana-screenshots/orders-baseline.png`

### Products

`products baseline` stayed fast at this load, but the unindexed filter query dominated SQL work.

| Query | Calls | Mean | Total | Rows |
|---|---:|---:|---:|---:|
| `product where category_id = ? and status = ?` | 15,001 | 7.90ms | 118,489.64ms | 2,508,501 |

Evidence files:

- `products/pool10-baseline/k6-summary.txt`
- `products/pool10-baseline/pg-stat-statements.txt`
- `products/pool10-baseline/product-seq-scan.json`
- `grafana-screenshots/products-baseline.png`

### Points

`points-page500` was materially worse than `points-page0`.

| Preset | Main Query | Calls | Mean | Total | Rows |
|---|---|---:|---:|---:|---:|
| `points-page0` | `point_history where user_id = ? fetch first ? rows only` | 13,283 | 3.12ms | 41,384.08ms | 265,660 |
| `points-page500` | `point_history where user_id = ? offset ? rows fetch first ? rows only` | 5,711 | 274.68ms | 1,568,719.47ms | 0 |

Both point scenarios also paid a count-query cost:

| Preset | Count Query Calls | Mean | Total |
|---|---:|---:|---:|
| `points-page0` | 13,273 | 186.30ms | 2,472,750.61ms |
| `points-page500` | 5,704 | 269.47ms | 1,537,065.92ms |

Evidence files:

- `points/pool10-page0/k6-summary.txt`
- `points/pool10-page0/pg-stat-statements.txt`
- `points/pool10-page500/k6-summary.txt`
- `points/pool10-page500/pg-stat-statements.txt`
- `grafana-screenshots/points-page0.png`
- `grafana-screenshots/points-page500.png`

## Interpretation

- `orders baseline` clearly reproduces excessive repeated `order_item` lookups and Hikari pool pressure.
- `products baseline` has acceptable latency at 50 rps, but still gives a Phase 2 comparison point for index work.
- `points-page500` shows deep offset cost through much higher mean SQL time and near-total request failure at this load.
- `points-page0` is not clean at 50 rps because the count query is already expensive on the loadtest dataset; it is still substantially less expensive than page500.
