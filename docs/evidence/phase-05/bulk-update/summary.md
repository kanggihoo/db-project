# Bulk Update Summary

## Result

| Strategy | Rows changed | Hibernate prepareStatementCount |
|---|---:|---:|
| Row-by-row dirty checking | 3 | 4 |
| JPQL bulk update | 3 | 1 |

## Interpretation

Row-by-row dirty checking first selects matching orders and then flushes updates for each changed entity. In this fixture/configuration, 3 orders changed, so Hibernate reported 3 entity updates and `prepareStatementCount=4`.

JPQL bulk update changed the same 3 rows with `prepareStatementCount=1` in this fixture/configuration. This evidence shows the bulk update path emits fewer prepared statements than per-entity dirty checking for this state transition test.

## Persistence Context Behavior

The repository method uses `@Modifying(clearAutomatically = true, flushAutomatically = true)` with a method-level transaction boundary. The focused persistence-context evidence verifies the clear/reload behavior: a previously loaded order is reloaded from the database after the bulk update and shows `PREPARING` instead of the stale `PENDING` value.
