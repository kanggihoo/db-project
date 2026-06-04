# Separate Phase Query Strategies by Target Contract

Learning phases that compare multiple query implementations should separate strategy code by **Optimization Target** while preserving the public **Measurement Condition** and **Phase Evidence** contracts. The query strategy refactor plan in `docs/superpowers/plans/query-strategy-refactor/000-plan-index.md` establishes the reference pattern for Phase 3, Phase 5, and Phase 7.

Phase 3 Order loading and Phase 5 Product search compare strategies under the same input and output contract, so they should use Spring strategy beans plus a registry keyed by the public request strategy value. `OrderService` and `ProductService` should delegate to those registries and should not contain central strategy execution switches. Existing enum-style types may remain only as `*StrategyName` compatibility types for request parameter parsing and naming, not as the place where execution branching lives.

Phase 7 Point pagination compares Offset/Page and Cursor pagination, but those APIs have different response contracts. It should use dedicated Offset and Cursor reader classes instead of forcing both paths into one generic strategy registry or one combined endpoint. `PointService` should preserve the existing service API and delegate pagination algorithm details to those readers.

Every phase following this pattern must keep endpoint paths, request parameter values, k6 scenario entrypoints, preset files, Grafana low-cardinality labels, and `docs/evidence/` paths stable unless a separate ADR explicitly changes those contracts. Strategy names are part of the measurement vocabulary, so they should stay aligned with k6/Grafana labels and phase documentation, and implementation class names should not become observability labels.

Future phases, including Phase 11 Concurrency Control, should reuse the same rule: introduce target-specific strategy contracts when competing implementations share a contract, split readers or use cases when response or workflow contracts differ, and avoid turning one service into a central registry of phase experiments.
