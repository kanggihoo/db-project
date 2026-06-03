# 003 Cursor Point API

## Goal

기존 `/api/points` Offset/Page API를 유지하고, count 없는 `/api/points/cursor` API를 추가한다.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursor.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursorResponse.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/repository/PointHistoryRepository.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java`
- Create: `ecommerce/src/test/resources/test-data/point-history-cursor-setup.sql`
- Modify: `ecommerce/src/test/java/com/dblab/ecommerce/repository/PointHistoryRepositoryTest.java`
- Create: `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java`

## Steps

- [ ] **Step 1: Add deterministic cursor fixture**

Create `ecommerce/src/test/resources/test-data/point-history-cursor-setup.sql`:

```sql
INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES (7300, 'phase7-user@example.com', 'pw', 'Phase7 User', 'MALE', 'GOLD', 0, NOW(), NOW());

INSERT INTO point_history (id, user_id, type, amount, balance_after, description, created_at)
VALUES
  (7301, 7300, 'EARN', 100, 100, 'p1', TIMESTAMP '2026-05-27 10:00:00'),
  (7302, 7300, 'USE',  100,   0, 'p2', TIMESTAMP '2026-05-27 10:00:00'),
  (7303, 7300, 'EARN', 200, 200, 'p3', TIMESTAMP '2026-05-27 09:00:00'),
  (7304, 7300, 'USE',  100, 100, 'p4', TIMESTAMP '2026-05-27 08:00:00'),
  (7305, 7300, 'EXPIRE', 50, 50, 'p5', TIMESTAMP '2026-05-27 07:00:00');
```

- [ ] **Step 2: Add cursor DTO**

Create `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursor.java`:

```java
package com.dblab.ecommerce.dto;

import java.time.LocalDateTime;

public record PointHistoryCursor(
        LocalDateTime lastCreatedAt,
        Long lastId) {
}
```

- [ ] **Step 3: Add cursor response DTO**

Create `ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursorResponse.java`:

```java
package com.dblab.ecommerce.dto;

import java.util.List;

public record PointHistoryCursorResponse(
        List<PointHistoryResponse> items,
        PointHistoryCursor nextCursor,
        boolean hasNext) {
}
```

- [ ] **Step 4: Add repository cursor queries**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/repository/PointHistoryRepository.java`:

```java
package com.dblab.ecommerce.repository;

import com.dblab.ecommerce.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT ph
            FROM PointHistory ph
            WHERE ph.userId = :userId
            ORDER BY ph.createdAt DESC, ph.id DESC
            """)
    List<PointHistory> findFirstCursorPage(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT ph
            FROM PointHistory ph
            WHERE ph.userId = :userId
              AND (ph.createdAt < :lastCreatedAt
                   OR (ph.createdAt = :lastCreatedAt AND ph.id < :lastId))
            ORDER BY ph.createdAt DESC, ph.id DESC
            """)
    List<PointHistory> findNextCursorPage(
            @Param("userId") Long userId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);
}
```

- [ ] **Step 5: Add cursor service method**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java`:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.PointHistoryCursor;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.entity.PointHistory;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;

    public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        return pointHistoryRepository.findByUserId(userId, PageRequest.of(page, size))
                .map(PointHistoryResponse::from);
    }

    public PointHistoryCursorResponse getPointHistoryByCursor(
            Long userId,
            int size,
            LocalDateTime lastCreatedAt,
            Long lastId) {
        int limit = size + 1;
        List<PointHistory> rows = lastCreatedAt == null || lastId == null
                ? pointHistoryRepository.findFirstCursorPage(userId, PageRequest.of(0, limit))
                : pointHistoryRepository.findNextCursorPage(userId, lastCreatedAt, lastId, PageRequest.of(0, limit));

        boolean hasNext = rows.size() > size;
        List<PointHistory> pageRows = hasNext ? rows.subList(0, size) : rows;
        List<PointHistoryResponse> items = pageRows.stream()
                .map(PointHistoryResponse::from)
                .toList();
        PointHistoryCursor nextCursor = hasNext ? cursorFrom(pageRows.get(pageRows.size() - 1)) : null;
        return new PointHistoryCursorResponse(items, nextCursor, hasNext);
    }

    private PointHistoryCursor cursorFrom(PointHistory pointHistory) {
        return new PointHistoryCursor(pointHistory.getCreatedAt(), pointHistory.getId());
    }
}
```

- [ ] **Step 6: Add cursor controller endpoint**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java`:

```java
package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public Page<PointHistoryResponse> getPointHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return pointService.getPointHistory(userId, page, size);
    }

    @GetMapping("/cursor")
    public PointHistoryCursorResponse getPointHistoryByCursor(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreatedAt,
            @RequestParam(required = false) Long lastId
    ) {
        return pointService.getPointHistoryByCursor(userId, size, lastCreatedAt, lastId);
    }
}
```

- [ ] **Step 7: Add repository cursor tests**

Append to `ecommerce/src/test/java/com/dblab/ecommerce/repository/PointHistoryRepositoryTest.java`:

```java
@Test
void cursor_첫_페이지는_createdAt_id_내림차순으로_size_plus_one건_조회한다() {
    List<PointHistory> rows = pointHistoryRepository.findFirstCursorPage(
            300L, PageRequest.of(0, 11));

    assertThat(rows).hasSize(11);
    assertThat(rows)
            .isSortedAccordingTo((left, right) -> {
                int createdCompare = right.getCreatedAt().compareTo(left.getCreatedAt());
                if (createdCompare != 0) {
                    return createdCompare;
                }
                return right.getId().compareTo(left.getId());
            });
}
```

Add imports:

```java
import java.util.List;
```

- [ ] **Step 8: Add cursor API integration test**

Create `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java`:

```java
package com.dblab.ecommerce.point;

import com.dblab.ecommerce.TestcontainersConfiguration;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Sql("/test-data/point-history-cursor-setup.sql")
class PointCursorApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("cursor API returns first page and next cursor")
    void cursorApiReturnsFirstPageAndNextCursor() {
        PointHistoryCursorResponse response = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2",
                PointHistoryCursorResponse.class);

        assertThat(response.items()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.items())
                .extracting("id")
                .containsExactly(7302L, 7301L);
    }

    @Test
    @DisplayName("cursor API returns rows after supplied cursor")
    void cursorApiReturnsRowsAfterSuppliedCursor() {
        PointHistoryCursorResponse first = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2",
                PointHistoryCursorResponse.class);

        PointHistoryCursorResponse second = restTemplate.getForObject(
                "/api/points/cursor?userId=7300&size=2&lastCreatedAt={lastCreatedAt}&lastId={lastId}",
                PointHistoryCursorResponse.class,
                first.nextCursor().lastCreatedAt(),
                first.nextCursor().lastId());

        assertThat(second.items())
                .extracting("id")
                .containsExactly(7303L, 7304L);
        assertThat(second.hasNext()).isTrue();
    }
}
```

- [ ] **Step 9: Run focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*PointHistoryRepositoryTest" --tests "*PointCursorApiTest"
```

Expected: Gradle exits 0.

- [ ] **Step 10: Compile**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0.

- [ ] **Step 11: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursor.java ecommerce/src/main/java/com/dblab/ecommerce/dto/PointHistoryCursorResponse.java ecommerce/src/main/java/com/dblab/ecommerce/repository/PointHistoryRepository.java ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java ecommerce/src/test/resources/test-data/point-history-cursor-setup.sql ecommerce/src/test/java/com/dblab/ecommerce/repository/PointHistoryRepositoryTest.java ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java
git commit -m "feat(phase7): add cursor point history API"
```

