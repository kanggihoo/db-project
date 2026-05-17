# Require Phase Evidence Under docs/evidence

A learning phase is complete only when the relevant behavior is measured and recorded under `docs/evidence/`, not merely when code is implemented. Evidence can include test output, query counts, `EXPLAIN ANALYZE` output, `pg_stat_statements` snapshots, k6 summaries, and Grafana screenshots; optimization phases should compare pre-change and post-change behavior under comparable conditions, while Phase 1 serves as the baseline for later phases.
