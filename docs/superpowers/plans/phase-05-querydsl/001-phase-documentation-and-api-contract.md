# 001 Phase Documentation And API Contract

## Goal

Create the Phase 5 documentation scaffold and introduce the product search strategy API contract without implementing the QueryDSL repository yet.

## Files

- Create: `docs/phases/05-querydsl/README.md`
- Create: `docs/phases/05-querydsl/scope.md`
- Create: `docs/phases/05-querydsl/runbook.md`
- Create: `docs/phases/05-querydsl/observability.md`
- Create: `docs/phases/05-querydsl/report.md`
- Create: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`
- Modify: `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`

## Steps

- [ ] **Step 1: Create the Phase 5 documentation directory**

Run:

```bash
rtk proxy powershell -NoProfile -Command "New-Item -ItemType Directory -Force 'docs/phases/05-querydsl' | Out-Null"
```

Expected: command exits 0.

- [ ] **Step 2: Add Phase 5 README**

Create `docs/phases/05-querydsl/README.md`:

```markdown
# Phase 5. QueryDSL 조회 최적화

Phase 5는 기존 Product entity 조회 baseline과 QueryDSL DTO projection 경로를 같은 상품 검색 API에서 비교하는 Learning Phase다.

## 현재 상태

Phase 5는 진행 전이다.

- 기존 상품 검색 API는 유지한다.
- `strategy=baseline|querydsl`로 같은 조건의 조회 경로를 비교한다.
- strategy 생략 시 QueryDSL 경로를 기본값으로 사용한다.
- k6/Grafana는 필수 evidence가 아니다.

## 문서

| 문서 | 용도 |
|---|---|
| [scope.md](./scope.md) | Phase 5 범위와 제외 범위 |
| [runbook.md](./runbook.md) | 반복 가능한 실행 절차 |
| [observability.md](./observability.md) | SQL/count 관측 기준 |
| [report.md](./report.md) | 결과 보고서와 Phase 6 handoff |

## Source Documents

- Roadmap: [docs/roadmap/06-phase-5-querydsl.md](../../roadmap/06-phase-5-querydsl.md)
- Design Spec: [docs/superpowers/specs/2026-05-26-phase-5-querydsl-design.md](../../superpowers/specs/2026-05-26-phase-5-querydsl-design.md)
- Phase 4 handoff: [docs/phases/04-transaction-isolation/report.md](../04-transaction-isolation/report.md)

## 관련 산출물

- Phase Evidence: [docs/evidence/phase-05/README.md](../../evidence/phase-05/README.md)

## 다음 Phase 연결

Phase 5 이후 단순 조회 최적화 경계를 정리하면, Phase 6에서는 Product/Review 집계 쿼리 병목과 GROUP BY 실행계획을 다룬다.
```

- [ ] **Step 3: Add Phase 5 scope**

Create `docs/phases/05-querydsl/scope.md`:

```markdown
# Phase 5 Scope

## 목표

기존 Product entity 조회 baseline과 QueryDSL DTO projection을 같은 상품 검색 조건에서 비교한다.

## 포함 범위

| Area | Scope |
|---|---|
| API | `GET /api/products` |
| Strategy | `baseline`, `querydsl` |
| Default | strategy 생략 시 `querydsl` |
| Search conditions | optional `categoryId`, optional `status` |
| Baseline | Spring Data JPA entity 조회 후 `ProductResponse.from(product)` |
| QueryDSL | 필요한 컬럼만 `ProductResponse`로 projection |
| Bulk update | `Orders` 상태 변경 row-by-row vs JPQL bulk update 비교 |
| Evidence | SQL 로그, SQL count, integration test output, summary |

## 제외 범위

- `minPrice`, `maxPrice`, `keyword` 검색 조건 추가
- k6/Grafana 필수 evidence
- stock/coupon/order concurrency control
- pessimistic lock, optimistic lock, retry, idempotency
- Product 검색 외 다른 사용자-facing API 변경

## 완료 조건

- [ ] `GET /api/products`가 `strategy=baseline|querydsl`을 받는다.
- [ ] strategy 생략 시 QueryDSL 경로를 사용한다.
- [ ] baseline과 querydsl이 같은 공유 조건에서 같은 `ProductResponse` 값을 반환한다.
- [ ] QueryDSL 경로가 null `categoryId`와 null `status` 조건을 predicate에서 제외한다.
- [ ] row-by-row update와 bulk update의 SQL count 차이를 테스트 또는 evidence로 남긴다.
- [ ] evidence가 `docs/evidence/phase-05/` 아래에 정리된다.
- [ ] Phase 6 집계 쿼리 handoff가 report에 기록된다.
```

- [ ] **Step 4: Add Phase 5 runbook**

Create `docs/phases/05-querydsl/runbook.md`:

```markdown
# Phase 5 Runbook

## Product search tests

Run from `ecommerce/`:

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest"
```

Expected:

- baseline/querydsl equivalent response test passes.
- omitted strategy default test passes.
- QueryDSL null condition tests pass.
- SQL count assertions pass.

## Bulk update tests

Run from `ecommerce/`:

```bash
rtk gradlew test --tests "*OrderBulkUpdateTest"
```

Expected:

- row-by-row update emits more update statements than bulk update.
- bulk update changes the expected rows.
- persistence context clear behavior is tested.

## Evidence capture

Save focused test output:

```bash
rtk gradlew test --tests "*ProductSearchStrategyTest" > ..\\docs\\evidence\\phase-05\\product-search\\strategy-test-output.txt
rtk gradlew test --tests "*OrderBulkUpdateTest" > ..\\docs\\evidence\\phase-05\\bulk-update\\persistence-context-test-output.txt
```

Representative SQL should be copied from Hibernate SQL logs into:

- `docs/evidence/phase-05/product-search/baseline-sql.txt`
- `docs/evidence/phase-05/product-search/querydsl-sql.txt`
- `docs/evidence/phase-05/bulk-update/loop-update-sql-count.txt`
- `docs/evidence/phase-05/bulk-update/bulk-update-sql-count.txt`
```

- [ ] **Step 5: Add Phase 5 observability doc**

Create `docs/phases/05-querydsl/observability.md`:

```markdown
# Phase 5 Observability

## Primary evidence

Phase 5 uses test and SQL evidence by default.

| Evidence | Purpose |
|---|---|
| Hibernate representative SQL | Compare selected columns and where predicates |
| Hibernate statistics `prepareStatementCount` | Compare SQL count |
| Integration test output | Prove strategy behavior and bulk update behavior |
| Summary documents | Explain result and handoff |

## Product search interpretation

- baseline SQL is expected to select Product entity columns.
- querydsl SQL is expected to select only `ProductResponse` fields.
- omitted `categoryId` must remove the category predicate.
- omitted `status` must remove the status predicate.

## Bulk update interpretation

- row-by-row update should issue one select plus one update per changed row.
- JPQL bulk update should issue one update statement.
- persistence context state after bulk update must be cleared or explicitly documented.

## Optional evidence

`EXPLAIN ANALYZE`, `pg_stat_statements`, k6, and Grafana may be added later, but they are not required for Phase 5 completion.
```

- [ ] **Step 6: Add initial Phase 5 report**

Create `docs/phases/05-querydsl/report.md`:

```markdown
# Phase 5 결과 보고서

## 결론

Phase 5는 진행 전이다. 구현 후 baseline entity 조회와 QueryDSL DTO projection의 SQL shape, SQL count, 테스트 결과를 이 문서에 정리한다.

## Product Search 비교

| Strategy | Result | Evidence |
|---|---|---|
| baseline | 미측정 | `docs/evidence/phase-05/product-search/` |
| querydsl | 미측정 | `docs/evidence/phase-05/product-search/` |

## Bulk Update 비교

| Strategy | Result | Evidence |
|---|---|---|
| row-by-row | 미측정 | `docs/evidence/phase-05/bulk-update/` |
| bulk update | 미측정 | `docs/evidence/phase-05/bulk-update/` |

## Phase 6 Handoff

Phase 5 완료 후 단순 조회 최적화 이후 남는 Product/Review 집계 쿼리 병목을 Phase 6으로 넘긴다.
```

- [ ] **Step 7: Add `ProductSearchStrategy` enum**

Create `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java`:

```java
package com.dblab.ecommerce.service;

public enum ProductSearchStrategy {
    BASELINE,
    QUERYDSL;

    public static ProductSearchStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return QUERYDSL;
        }

        return switch (value.trim().toLowerCase()) {
            case "baseline" -> BASELINE;
            case "querydsl" -> QUERYDSL;
            default -> throw new IllegalArgumentException("Unsupported product search strategy: " + value);
        };
    }
}
```

- [ ] **Step 8: Update `ProductController` to accept optional strategy**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java`:

```java
package com.dblab.ecommerce.controller;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.service.ProductSearchStrategy;
import com.dblab.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Product.Status status,
            @RequestParam(defaultValue = "querydsl") String strategy) {
        return productService.searchProducts(categoryId, status, resolveStrategy(strategy));
    }

    private ProductSearchStrategy resolveStrategy(String strategy) {
        try {
            return ProductSearchStrategy.from(strategy);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
```

- [ ] **Step 9: Update `ProductService` with strategy routing placeholder**

Modify `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java`:

```java
package com.dblab.ecommerce.service;

import com.dblab.ecommerce.dto.ProductResponse;
import com.dblab.ecommerce.entity.Product;
import com.dblab.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> searchProducts(Long categoryId, Product.Status status) {
        return searchProducts(categoryId, status, ProductSearchStrategy.QUERYDSL);
    }

    public List<ProductResponse> searchProducts(
            Long categoryId,
            Product.Status status,
            ProductSearchStrategy strategy) {
        if (strategy == ProductSearchStrategy.BASELINE) {
            return searchProductsBaseline(categoryId, status);
        }

        return searchProductsBaseline(categoryId, status);
    }

    private List<ProductResponse> searchProductsBaseline(Long categoryId, Product.Status status) {
        return productRepository.findByCategoryIdAndStatus(categoryId, status)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
```

This temporary routing keeps behavior compiling before the QueryDSL repository is added in slice 002.

- [ ] **Step 10: Run compile check**

Run:

```bash
cd ecommerce && rtk gradlew compileJava
```

Expected: Gradle exits 0.

- [ ] **Step 11: Commit**

```bash
git add docs/phases/05-querydsl ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategy.java
git commit -m "feat(phase5): add product search strategy contract"
```
