# 005 Compatibility And Documentation Check

## Goal

API, k6, Grafana, Phase Evidence 계약이 리팩토링 후에도 유지되는지 확인하고 필요한 문서만 갱신한다.

## Files

- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java`
- Inspect: `k6/orders-test.js`
- Inspect: `k6/products-test.js`
- Inspect: `k6/points-test.js`
- Inspect: `k6/points-cursor-test.js`
- Inspect: `k6/points-offset-sampling-test.js`
- Inspect: `k6/points-cursor-sampling-test.js`
- Inspect: `docs/phases/03-n-plus-one/README.md`
- Inspect: `docs/phases/05-querydsl/README.md`
- Inspect: `docs/phases/07-pagination/README.md`

## Steps

- [ ] **Step 1: Verify controller endpoint paths**

Run:

```bash
rtk grep "@RequestMapping|@GetMapping|strategy|cursor" ecommerce/src/main/java/com/dblab/ecommerce/controller
```

Expected:

- `OrderController` still exposes `GET /api/orders`
- `ProductController` still exposes `GET /api/products` and `GET /api/products/review-summary`
- `PointController` still exposes `GET /api/points` and `GET /api/points/cursor`

- [ ] **Step 2: Verify k6 endpoint strings are unchanged**

Run:

```bash
rtk grep "/api/orders|/api/products|/api/points|/api/points/cursor" k6
```

Expected:

- order k6 still targets `/api/orders`
- product k6 still targets `/api/products`
- point offset k6 still targets `/api/points`
- point cursor k6 still targets `/api/points/cursor`

- [ ] **Step 3: Verify low-cardinality labels remain unchanged**

Run:

```bash
rtk grep "phase|scenario|preset|pool" k6/lib k6/*test.js
```

Expected:

- labels still use `phase`, `scenario`, `preset`, `pool`
- no strategy implementation class names are added as Prometheus labels

- [ ] **Step 4: Verify Phase docs do not claim removed behavior**

Run:

```bash
rtk grep "OrderLoadingStrategy|ProductSearchStrategy|PointService|strategy=baseline|strategy=querydsl|points/cursor" docs/phases/03-n-plus-one docs/phases/05-querydsl docs/phases/07-pagination docs/guides
```

Expected:

- docs may mention public strategy request values
- docs should not require service switch internals
- if docs mention Java class names that changed, update them to describe public API or new class names

- [ ] **Step 5: Update docs only if class-name references changed**

If docs reference old internal class names, prefer public-contract wording:

```markdown
Phase 3는 `GET /api/orders?strategy=...`의 public strategy 값으로 Lazy, Fetch Join, Batch Size, EntityGraph 측정 조건을 구분한다. 내부 구현은 strategy class로 분리되어 있으며, Phase Evidence 해석은 request parameter와 SQL shape를 기준으로 한다.
```

For Phase 5:

```markdown
Phase 5는 `GET /api/products?strategy=baseline|querydsl`의 public strategy 값으로 entity loading baseline과 QueryDSL DTO projection을 비교한다. 내부 구현은 product search strategy class로 분리되어 있다.
```

For Phase 7:

```markdown
Phase 7는 `GET /api/points` Offset/Page reader와 `GET /api/points/cursor` Cursor reader를 분리한다. 두 API는 응답 계약이 다르므로 하나의 strategy endpoint로 합치지 않는다.
```

- [ ] **Step 6: Run observability verifier**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected:

- command exits 0
- k6/Grafana contract remains valid

- [ ] **Step 7: Commit documentation compatibility changes**

If docs changed:

```bash
git add docs/phases docs/guides
git commit -m "docs(query): align strategy refactor terminology"
```

If docs did not change, do not commit.
