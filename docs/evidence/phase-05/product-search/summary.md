# Product Search Summary

## Result

The baseline and QueryDSL strategies returned equivalent `ProductResponse` values for shared `categoryId` and `status` conditions.

## SQL Shape

- Baseline selected Product entity columns before mapping to `ProductResponse`.
- QueryDSL selected only the fields needed by `ProductResponse`.

## SQL Count

Both strategies used one SQL statement for the shared condition set. The Phase 5 difference is selected column shape and DTO projection, not load-test throughput.

## Null Conditions

QueryDSL omitted null `categoryId` and null `status` predicates in focused tests.
