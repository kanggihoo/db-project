# 004 Help And Command Docs

## Goal

`make help`와 command guide가 새 Makefile 구조와 preset 변수 계약을 정확히 설명하게 한다.

## Files

- Modify: `makefiles/help.mk`
- Modify: `docs/guides/commands.md`
- Modify: `docs/guides/project-format-standard.md`

## Steps

- [ ] **Step 1: Review current help output**

Run:

```bash
rtk proxy make help
```

Expected:

- public target examples are readable
- `PRESET`, `K6_PRESET`, `GRAFANA_PRESET` explanation is present
- no Phase 3/4 compatibility target is listed

- [ ] **Step 2: Update `docs/guides/commands.md` common variables**

Make the variables table include:

```markdown
| Variable | Example | Meaning |
|---|---|---|
| `PHASE` | `phase-06` | Phase id |
| `SCENARIO` | `review-aggregate` | SQL, k6, or Grafana scenario |
| `CONDITION` | `query-shaped-index` | Evidence Measurement Condition |
| `ACTION` | `prepare` | SQL runner action |
| `PRESET` | `review-summary-baseline` | Public preset value used by k6 and Grafana by default |
| `K6_PRESET` | `review-summary-baseline` | k6 preset override, defaults to `PRESET` |
| `GRAFANA_PRESET` | `baseline` | Grafana preset variable override, defaults to `PRESET` |
| `POOL` | `pool10` | Connection pool label |
| `TABLE` | `review` | Grafana table variable |
| `OUTPUT` | `docs/evidence/phase-06/.../explain.txt` | Output file path |
| `WINDOW_FILE` | `docs/evidence/phase-06/.../run-window.json` | Fixed Grafana capture window file |
```

- [ ] **Step 3: Update `docs/guides/commands.md` examples**

Keep the existing Phase 6 examples and add one preset split example:

````markdown
```bash
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=prepare
make phase-sql PHASE=phase-06 SCENARIO=review-aggregate CONDITION=baseline ACTION=explain OUTPUT=docs/evidence/phase-06/review-aggregate/baseline/explain.txt
make k6-evidence PHASE=phase-06 SCENARIO=review-summary CONDITION=naive-index
make grafana-capture PHASE=phase-06 SCENARIO=review-summary CONDITION=query-shaped-index TABLE=review
make grafana-capture PHASE=phase-06 SCENARIO=review-summary GRAFANA_PRESET=baseline CONDITION=query-shaped-index TABLE=review
```
````

- [ ] **Step 4: Update `docs/guides/project-format-standard.md` Makefile contract**

In the standard variable table, keep `PRESET` as the public variable and add:

```markdown
| `K6_PRESET` | k6 preset override when different from `PRESET` | `review-summary-baseline` |
| `GRAFANA_PRESET` | Grafana preset variable override when different from `PRESET` | `baseline` |
```

- [ ] **Step 5: Clarify monolithic Makefile example**

If the example block still shows a monolithic Makefile, add this sentence before the example:

```markdown
작은 프로젝트는 단일 Makefile을 사용할 수 있다. target이 늘어나면 루트 Makefile은 include 중심으로 두고 `makefiles/*.mk`로 책임을 나눈다.
```

- [ ] **Step 6: Verify docs mention only supported targets**

Run:

```bash
rtk grep "phase3-grafana\\|phase4-sql\\|phase-compat" docs/guides Makefile makefiles
```

Expected:

- no matches in `docs/guides`, `Makefile`, or `makefiles`
- matches in historical spec/plan docs are acceptable if the search scope is broader than this command

- [ ] **Step 7: Verify help after docs change**

Run:

```bash
rtk proxy make help
```

Expected: command exits 0.

- [ ] **Step 8: Commit**

```bash
git add makefiles/help.mk docs/guides/commands.md docs/guides/project-format-standard.md
git commit -m "docs(make): document command interface split"
```
