# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This is a single-context repo.

Expected domain documentation locations:

```text
/
├── CONTEXT.md
├── docs/adr/
└── docs/
    ├── roadmap/
    └── phases/
```

## Before exploring, read these

- **`CONTEXT.md`** at the repo root if it exists.
- **`docs/adr/`** for decisions that touch the area about to be changed.
- **Relevant `docs/roadmap/<phase>.md` files only** when work is tied to a specific learning phase or long-term milestone. Do not read the whole roadmap directory by default.

If `CONTEXT.md` or `docs/adr/` do not exist yet, proceed silently. Do not block the task just because they have not been created. Producer skills can create them later when terms or architectural decisions need to be recorded.

## Use the glossary's vocabulary

When output names a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term as defined in `CONTEXT.md`.

If the concept is not in the glossary yet, either avoid inventing new language or note the gap for a future domain-doc pass.

## Flag ADR conflicts

If output contradicts an existing ADR, surface it explicitly rather than silently overriding it:

> Contradicts ADR-0007, but worth reopening because...

## Current project context

This repository is an ecommerce DB optimization learning project. Roadmap phases use tests, SQL analysis, k6, Prometheus, and Grafana evidence to show the effect of each database optimization or operational pattern.
