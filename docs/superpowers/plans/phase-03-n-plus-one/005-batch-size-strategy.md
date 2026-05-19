# 005 BatchSize Strategy

## Goal

Implement `strategy=batch-size` through an isolated Spring profile so Hibernate groups lazy association loads into `IN (...)` queries without contaminating Lazy or Fetch Join evidence.

## Files

- Create: `ecommerce/src/main/resources/application-phase3-batch.yaml`
- Modify: `scripts/server.sh`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java`
- Modify: `docs/phases/03-n-plus-one/runbook.md`

## Steps

- [ ] **Step 1: Create batch-size profile**

Create `ecommerce/src/main/resources/application-phase3-batch.yaml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

Expected: profile keeps pool10 sizing and adds only Hibernate batch fetching.

- [ ] **Step 2: Allow server script to start the batch profile**

Modify `scripts/server.sh` profile validation:

```bash
case "$POOL" in
  pool5|pool10|pool20|phase3-batch) ;;
  *)
    echo "Usage: $0 [pool5|pool10|pool20|phase3-batch]" >&2
    exit 1
    ;;
esac
```

- [ ] **Step 3: Route `BATCH_SIZE` to the same lazy traversal service**

Modify `OrderService` switch:

```java
public List<OrderResponse> getOrdersByUserId(Long userId, OrderLoadingStrategy strategy) {
    return switch (strategy) {
        case LAZY -> getOrdersByUserIdLazy(userId);
        case FETCH_JOIN -> getOrdersByUserIdFetchJoin(userId);
        case BATCH_SIZE -> getOrdersByUserIdLazy(userId);
        case ENTITY_GRAPH -> getOrdersByUserIdLazy(userId);
    };
}
```

The SQL behavior difference comes from the `phase3-batch` profile, not from a separate code path.

- [ ] **Step 4: Update runbook with batch profile isolation**

In `docs/phases/03-n-plus-one/runbook.md`, add:

````markdown
## BatchSize profile

BatchSize evidence must run with the isolated profile:

```bash
./scripts/server.sh phase3-batch
PHASE=phase-03 POOL=pool10 STRATEGY=batch-size ./k6/run.sh orders baseline prometheus
```

Lazy, Fetch Join, and EntityGraph evidence must run without the `phase3-batch` profile so global batch fetching does not affect their SQL count.
````

- [ ] **Step 5: Verify configuration loads**

Run:

```bash
cd ecommerce && rtk gradlew test
```

Expected: all tests pass and Spring test context loads with existing default test profiles.

- [ ] **Step 6: Commit**

```bash
git add ecommerce/src/main/resources/application-phase3-batch.yaml scripts/server.sh ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java docs/phases/03-n-plus-one/runbook.md
git commit -m "feat: isolate phase 3 batch size strategy"
```
