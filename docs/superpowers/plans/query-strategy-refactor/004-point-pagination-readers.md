# 004 Point Pagination Readers

## Goal

Phase 7 Point pagination의 Offset/Page와 Cursor 알고리즘을 reader class로 분리하고 `PointService`는 기존 API를 유지한 채 위임만 담당하게 한다.

## Files

- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/point/OffsetPointPaginationReader.java`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/point/CursorPointPaginationReader.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java`
- Inspect: `ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java`
- Inspect: `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java`

## Steps

- [ ] **Step 1: Run current point cursor test**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*PointCursorApiTest"
```

Expected:

- command exits 0 before refactor

- [ ] **Step 2: Create `OffsetPointPaginationReader`**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/point/OffsetPointPaginationReader.java`:

```java
package com.dblab.ecommerce.service.point;

import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OffsetPointPaginationReader {

    private final PointHistoryRepository pointHistoryRepository;

    public Page<PointHistoryResponse> read(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        return pointHistoryRepository.findByUserId(userId, pageRequest)
                .map(PointHistoryResponse::from);
    }
}
```

- [ ] **Step 3: Create `CursorPointPaginationReader`**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/point/CursorPointPaginationReader.java`:

```java
package com.dblab.ecommerce.service.point;

import com.dblab.ecommerce.dto.PointHistoryCursor;
import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.entity.PointHistory;
import com.dblab.ecommerce.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CursorPointPaginationReader {

    private final PointHistoryRepository pointHistoryRepository;

    public PointHistoryCursorResponse read(
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

- [ ] **Step 4: Refactor `PointService` to delegate to readers**

Replace `PointService` with:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.PointHistoryCursorResponse;
import com.dblab.ecommerce.dto.PointHistoryResponse;
import com.dblab.ecommerce.service.point.CursorPointPaginationReader;
import com.dblab.ecommerce.service.point.OffsetPointPaginationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final OffsetPointPaginationReader offsetPointPaginationReader;
    private final CursorPointPaginationReader cursorPointPaginationReader;

    public Page<PointHistoryResponse> getPointHistory(Long userId, int page, int size) {
        return offsetPointPaginationReader.read(userId, page, size);
    }

    public PointHistoryCursorResponse getPointHistoryByCursor(
            Long userId,
            int size,
            LocalDateTime lastCreatedAt,
            Long lastId) {
        return cursorPointPaginationReader.read(userId, size, lastCreatedAt, lastId);
    }
}
```

- [ ] **Step 5: Verify `PointController` requires no behavior change**

Inspect:

```bash
rtk read ecommerce/src/main/java/com/dblab/ecommerce/controller/PointController.java
```

Expected:

- `GET /api/points` still calls `pointService.getPointHistory(userId, page, size)`
- `GET /api/points/cursor` still calls `pointService.getPointHistoryByCursor(userId, size, lastCreatedAt, lastId)`
- no endpoint path changes

- [ ] **Step 6: Run point focused tests**

Run:

```bash
cd ecommerce && rtk gradlew test --tests "*PointCursorApiTest" --tests "*PointHistoryRepositoryTest"
```

Expected:

- command exits 0
- Offset/Page and Cursor API contracts remain intact

- [ ] **Step 7: Verify `PointService` no longer contains pagination algorithm details**

Run:

```bash
rtk grep "PageRequest|findFirstCursorPage|findNextCursorPage|cursorFrom|size \\+ 1" ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java
```

Expected:

- no matches

- [ ] **Step 8: Commit**

```bash
git add ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java ecommerce/src/main/java/com/dblab/ecommerce/service/point
git commit -m "refactor(point): split pagination readers"
```
