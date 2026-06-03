# 012 Offset Ordering And Tests

## Goal

Offset/Page API를 Cursor API와 같은 `created_at DESC, id DESC` 정렬로 맞춰 A/B 비교의 logical order를 통일한다.

## Files

- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java`
- Optional inspect: `ecommerce/src/test/resources/test-data/point-history-cursor-setup.sql`

## Steps

- [ ] **Step 1: Add failing API test for Offset ordering**

Add a test to `PointCursorApiTest` that calls `/api/points?userId=7300&page=0&size=2` and verifies the same descending order used by Cursor.

```java
@Test
@DisplayName("offset API uses createdAt and id descending order")
void offsetApiUsesCreatedAtAndIdDescendingOrder() {
    String body = restTemplate.getForObject(
            "/api/points?userId=7300&page=0&size=2",
            String.class);

    assertThat(body).contains("\"id\":7302");
    assertThat(body).contains("\"id\":7301");
    assertThat(body.indexOf("\"id\":7302"))
            .isLessThan(body.indexOf("\"id\":7301"));
}
```

- [ ] **Step 2: Run the focused test**

Run from `ecommerce/`:

```bash
rtk ./gradlew test --tests "*PointCursorApiTest"
```

Expected before implementation: the new test exposes the current Offset ordering gap if repository default ordering does not match `created_at DESC, id DESC`.

- [ ] **Step 3: Apply Offset ordering in service**

Update `PointService#getPointHistory` to pass an explicit sort.

```java
import org.springframework.data.domain.Sort;
```

```java
public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
    PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id")));
    return pointHistoryRepository.findByUserId(userId, pageRequest)
            .map(PointHistoryResponse::from);
}
```

- [ ] **Step 4: Re-run tests**

Run:

```bash
rtk ./gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
```

Expected: all selected tests pass.

- [ ] **Step 5: Confirm generated SQL shape during retest**

During later k6 runs, verify `pg_stat_statements` Offset data query contains:

```text
order by ph1_0.created_at desc, ph1_0.id desc
```

## Done When

- [ ] Offset API uses explicit `createdAt DESC, id DESC` ordering.
- [ ] Cursor API behavior still passes existing tests.
- [ ] Later `pg_stat_statements` evidence can show Offset and Cursor share the same logical order.

