# Use One Observability Dashboard for Comparable Phase Evidence

Phase evidence should be comparable across learning phases, so this repo uses one shared `DB Lab Overview` dashboard with phase-focused rows instead of separate dashboard JSON files per phase. k6 metrics carry low-cardinality measurement-condition labels such as `phase`, `scenario`, `preset`, and `pool`; high-cardinality request values and SQL text remain out of Prometheus labels, and query-level detail stays in `pg_stat_statements` evidence snapshots.
