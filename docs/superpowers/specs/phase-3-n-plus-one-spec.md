# Phase 3 N+1 Loading Strategy Spec

> 주문 목록 화면의 상품 썸네일 조회를 기준으로 JPA 연관 로딩 전략별 SQL count, query shape, 부하 지표 차이를 증거로 남기기 위한 요구사항이다.

## Goal

Phase 3는 `GET /api/orders?userId=&strategy=` 주문 목록 API에서 `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` 조회 경로를 재현하고, Lazy naive, Fetch Join, BatchSize, EntityGraph 전략의 차이를 비교한다.

대상 화면은 주문 목록이다. 주문 당시 상품명, 옵션, 가격은 `OrderItem`의 snapshot 필드를 사용하지만, 화면 썸네일은 현재 상품의 대표 이미지를 보여준다는 요구사항을 둔다. 따라서 `order_item.product_name`으로 `Product`를 역조회하지 않고, `order_item.sku_id -> product_sku.product_id -> product_image.product_id` 경로를 사용한다.

이 spec은 [Phase 3 roadmap](../../roadmap/04-phase-3-n-plus-one.md), [CONTEXT.md](../../../CONTEXT.md), [ADR 0002](../../adr/0002-require-phase-evidence-under-docs-evidence.md), [ADR 0004](../../adr/0004-keep-naive-implementations-as-deliberate-baselines.md), [ADR 0005](../../adr/0005-use-one-observability-dashboard-for-comparable-phase-evidence.md)를 따른다.

## Non-Goals

- 주문 목록을 production ecommerce feature로 완성하지 않는다.
- 기본 엔티티 fetch type을 `EAGER`로 고정하지 않는다.
- `order_item.product_name` 같은 snapshot 값을 `Product` 조회 키로 사용하지 않는다.
- QueryDSL 기반 DTO projection 최적화는 Phase 5 주제로 남긴다.
- 동시 주문, 재고 차감, 격리 수준, 락 문제는 Phase 4 이후 주제로 남긴다.
- Phase 2처럼 인덱스 실행계획 전환을 핵심 목표로 삼지 않는다.

## Decisions

### Use One API With A Strategy Parameter

전략별 endpoint를 여러 개 만들지 않고 하나의 주문 목록 API에서 `strategy` 파라미터로 로딩 전략만 분리한다.

```text
GET /api/orders?userId=123&strategy=lazy
GET /api/orders?userId=123&strategy=fetch-join
GET /api/orders?userId=123&strategy=batch-size
GET /api/orders?userId=123&strategy=entity-graph
```

같은 response shape를 유지해야 전략별 SQL count, latency, failure rate를 비교할 수 있다.

### Keep Entity Associations Lazy By Default

엔티티에는 JPA 조회용 연관관계를 추가하되 기본 fetch type은 `LAZY`로 둔다.

Required association path:

```text
Orders.orderItems
OrderItem.productSku
ProductSku.product
Product.images
```

Lazy naive 전략은 이 연관 경로를 그대로 접근해 `1 + N + 3M` 형태의 쿼리 폭증을 재현한다.

### Treat Fetch Join And BatchSize As Required Comparisons

Phase 3의 최소 비교 대상은 Lazy naive, Fetch Join, BatchSize다. EntityGraph는 선택 비교 대상으로 둔다.

| Strategy | Required | Purpose |
|---|---|---|
| Lazy naive | Yes | N+1 baseline 재현 |
| Fetch Join | Yes | join 기반 eager loading 효과와 한계 확인 |
| BatchSize | Yes | `IN (...)` 기반 batch loading 효과 확인 |
| EntityGraph | Optional | annotation 기반 조회 시점 로딩 계획 비교 |

### Use Grafana For Pressure, Not SQL Text

Grafana는 strategy별 latency, failure, dropped iterations, Hikari pressure, active sessions를 비교한다. SQL 원문과 query shape별 calls는 Prometheus label로 올리지 않고 `pg_stat_statements` evidence로 저장한다.

`strategy=lazy|fetch-join|batch-size|entity-graph`는 낮은 cardinality label로 사용할 수 있다.

## Requirements

### API And Domain Shape

The implementation plan must produce one comparable order-list response across strategies.

1. Add JPA associations for the required path while keeping default loading lazy.
2. Extend the order-list response to include a product thumbnail URL or equivalent thumbnail field.
3. Keep order name, option, price fields sourced from `OrderItem` snapshot values.
4. Route the request by `strategy` while keeping the same endpoint and response contract.
5. Reject or default unknown strategy values consistently.

### Loading Strategies

The implementation plan must provide these loading strategies.

| Strategy | Required Behavior |
|---|---|
| `lazy` | Access lazy associations naturally and reproduce repeated select queries. |
| `fetch-join` | Use JPQL `join fetch` for the main association path where safe. Document row duplication or collection fetch limits. |
| `batch-size` | Use Hibernate batch loading through `default_batch_fetch_size` profile/config or targeted `@BatchSize`. Show `IN (...)` query shape. |
| `entity-graph` | If implemented, use `@EntityGraph` on repository methods and document generated SQL shape. |

BatchSize configuration must be isolated enough that it does not contaminate the Lazy or Fetch Join evidence. A separate Spring profile is acceptable.

### Evidence Layout

Raw evidence must be stored under `docs/evidence/phase-03/orders/`.

Expected directories:

```text
docs/evidence/phase-03/orders/lazy/
docs/evidence/phase-03/orders/fetch-join/
docs/evidence/phase-03/orders/batch-size/
docs/evidence/phase-03/orders/entity-graph/
```

Each strategy directory should contain:

- `k6-summary.txt`
- `pg-stat-statements.txt`
- `sql-count.txt`
- `representative-sql.txt`
- `explain.txt`
- `grafana-screenshot.png`

`pg_stat_statements_reset()` must run before each strategy measurement so calls and total time do not mix across strategies.

### Measurement Workflow

The implementation plan must use smoke runs for SQL shape verification and baseline runs for final comparison.

Example commands:

```bash
PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders smoke prometheus

PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders baseline prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders baseline prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders baseline prometheus
```

The k6 order script must pass `strategy` as both a query parameter and a low-cardinality metric label.

### Phase Documentation

The implementation plan must create the standard Phase 3 documentation set under `docs/phases/03-n-plus-one/`.

| File | Required Content |
|---|---|
| `README.md` | Phase status, source documents, strategy list, evidence links |
| `scope.md` | 목표, 대상 API/table/entity path, non-goals, completion criteria |
| `runbook.md` | server profile, strategy runs, SQL count, k6, evidence capture commands |
| `observability.md` | SQL count, `pg_stat_statements`, k6, Grafana, EXPLAIN 해석 기준 |
| `report.md` | strategy별 결과 비교, Fetch Join/BatchSize trade-off, Phase 4 handoff |

### Observability

Phase 3 must compare these metrics by strategy:

- Request-level SQL count
- `pg_stat_statements.calls`
- `pg_stat_statements.mean_exec_time`
- `pg_stat_statements.total_exec_time`
- k6 p95/p99 latency
- k6 failure rate
- dropped iterations
- Hikari active connections
- Hikari pending threads
- Hikari acquire time
- PostgreSQL active sessions

`EXPLAIN (ANALYZE, BUFFERS)` is required only for representative SQL shapes:

- Lazy repeated select query
- Fetch Join query
- BatchSize `IN (...)` query

## Acceptance Criteria

- Phase 3 roadmap and spec agree on the `Orders -> OrderItems -> ProductSku -> Product -> ProductImages` target path.
- The API uses `GET /api/orders?userId=&strategy=` for strategy separation.
- The same response fields are available across Lazy, Fetch Join, and BatchSize strategies.
- Lazy evidence shows repeated query growth consistent with `1 + N + 3M`.
- Fetch Join and BatchSize evidence each include SQL count, `pg_stat_statements`, k6 summary, Grafana screenshot, and representative EXPLAIN output.
- k6 metrics include a low-cardinality `strategy` label.
- Grafana Phase 3 evidence focuses on latency, failure, dropped iterations, Hikari pressure, and active sessions rather than table scan conversion.
- Phase 3 report states which strategy is safest for order-list style pagination and why.
- Phase 3 report hands off the remaining consistency/concurrency question to Phase 4.

## Open Questions

- Should `entity-graph` be implemented in Phase 3 or left as a documented optional comparison?
- Should `batch-size` use a separate Spring profile or targeted `@BatchSize` annotations?
- Should product image loading fetch all images or only the representative main image?
- Should the Phase 3 Grafana dashboard generator add an explicit `$strategy` variable before the implementation work starts?
