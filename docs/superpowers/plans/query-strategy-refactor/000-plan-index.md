# Query Strategy Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phase 3/5/7 조회 전략 분기를 target별 strategy/reader class로 분리하면서 기존 API와 Evidence 계약을 유지한다.

**Architecture:** Order loading과 Product search는 같은 입력/출력 계약에서 전략만 바뀌므로 Spring strategy bean + registry 구조로 전환한다. Point pagination은 Offset/Page와 Cursor 응답 타입이 다르므로 공통 registry로 묶지 않고 offset reader와 cursor reader로 분리한다. 기존 enum은 실행 분기 switch가 아니라 request parameter 이름 변환용 `*StrategyName` compatibility 타입으로 남긴다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Data JPA, QueryDSL, JUnit 6, Testcontainers, AssertJ.

---

## Spec

구현 기준 문서:

- [Query Strategy Refactor Design](../../specs/2026-06-02-query-strategy-refactor-design.md)

## Plan Decisions

| 질문 | 결정 |
|---|---|
| 전체 범위 | Phase 3 Order loading, Phase 5 Product search, Phase 7 Point pagination |
| Order/Product 방식 | Strategy interface + registry |
| Point 방식 | Offset/Cursor reader class 분리 |
| 기존 enum 처리 | `OrderLoadingStrategyName`, `ProductSearchStrategyName`으로 rename |
| API endpoint | 변경하지 않음 |
| request parameter 값 | `lazy`, `fetch-join`, `batch-size`, `entity-graph`, `baseline`, `querydsl` 유지 |
| k6/Grafana/evidence | 변경하지 않음 |
| Phase 11 구현 | 제외, 확장 방향만 유지 |

## Implementation Slices

순서대로 진행한다. 각 slice는 리뷰 가능한 독립 변경 단위다.

| 순서 | 문서 | 결과 |
|---:|---|---|
| 001 | [Baseline And Naming Contract](./001-baseline-and-naming-contract.md) | 현재 테스트 기준선과 enum rename 기준 고정 |
| 002 | [Order Loading Strategy Registry](./002-order-loading-strategy-registry.md) | Phase 3 Order loading switch 제거 |
| 003 | [Product Search Strategy Registry](./003-product-search-strategy-registry.md) | Phase 5 Product search switch 제거 |
| 004 | [Point Pagination Readers](./004-point-pagination-readers.md) | Phase 7 Offset/Cursor reader 분리 |
| 005 | [Compatibility And Documentation Check](./005-compatibility-and-documentation-check.md) | API, k6, docs 계약 영향 점검 |
| 999 | [Integration Stabilization](./999-integration-stabilization.md) | focused tests, 전체 tests, diff 최종 점검 |

## File Ownership

| Path | Responsibility |
|---|---|
| `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderService.java` | Order loading registry 위임 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/OrderLoadingStrategyName.java` | 기존 order strategy request value compatibility enum |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategy.java` | Order loading strategy contract |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/order/OrderLoadingStrategyRegistry.java` | order strategy name lookup |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/order/*OrderLoadingStrategy.java` | lazy/fetch-join/batch-size/entity-graph 구현 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductService.java` | Product search registry 위임 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/ProductSearchStrategyName.java` | 기존 product strategy request value compatibility enum |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategy.java` | Product search strategy contract |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/product/ProductSearchStrategyRegistry.java` | product strategy name lookup |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/product/*ProductSearchStrategy.java` | baseline/querydsl 구현 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/PointService.java` | Point pagination reader 위임 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/point/OffsetPointPaginationReader.java` | offset/page pagination 구현 |
| `ecommerce/src/main/java/com/dblab/ecommerce/service/point/CursorPointPaginationReader.java` | cursor pagination 구현 |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/OrderController.java` | order strategy string 전달 |
| `ecommerce/src/main/java/com/dblab/ecommerce/controller/ProductController.java` | product strategy string 전달과 400 mapping 유지 |
| `ecommerce/src/test/java/com/dblab/ecommerce/service/*` | strategy name/registry tests |
| `ecommerce/src/test/java/com/dblab/ecommerce/product/ProductSearchStrategyTest.java` | product service compatibility tests |
| `ecommerce/src/test/java/com/dblab/ecommerce/point/PointCursorApiTest.java` | point cursor compatibility tests |

## Cross-Slice Invariants

- API endpoint path를 바꾸지 않는다.
- request parameter 값을 바꾸지 않는다.
- k6 script와 preset JSON을 바꾸지 않는다.
- Grafana dashboard YAML과 label 계약을 바꾸지 않는다.
- Phase Evidence 경로를 이동하지 않는다.
- `OrderService`와 `ProductService`에 전략 실행 switch를 남기지 않는다.
- `PointService`는 Offset/Cursor 알고리즘을 직접 구현하지 않고 reader에 위임한다.
- Phase 11 동시성 전략은 이번 구현 범위에 넣지 않는다.

## Verification Policy

각 slice 후 가능한 범위에서 focused test를 실행한다.

```bash
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyNameTest"
cd ecommerce && rtk gradlew test --tests "*OrderLoadingStrategyRegistryTest"
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyNameTest"
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyRegistryTest"
cd ecommerce && rtk gradlew test --tests "*ProductSearchStrategyTest"
cd ecommerce && rtk gradlew test --tests "*PointCursorApiTest"
```

최종 slice에서는 전체 테스트를 실행한다.

```bash
cd ecommerce && rtk gradlew test
rtk git diff --check
```
