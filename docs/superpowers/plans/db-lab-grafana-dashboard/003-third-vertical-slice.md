# 003 Third Vertical Slice: Documentation Workflow

## Goal

Document how to run labeled k6 scenarios and use `DB Lab Overview` for Phase Evidence.

## Files

- Modify: `docs/guides/grafana-observability.md`
- Modify: `docs/guides/k6-load-testing.md`
- Modify: `docs/guides/environment.md`
- Test: `scripts/verify-observability.mjs`

## Steps

- [ ] **Step 1: Update `docs/guides/grafana-observability.md`**

Ensure the guide links to the spec at:

```markdown
[DB Lab Grafana Dashboard Spec](../../specs/db-lab-grafana-dashboard-spec.md)
```

Add or keep a `DB Lab Overview` section that says:

```markdown
공통 대시보드는 `DB Lab Overview` 하나를 기준으로 한다. Phase별로 dashboard JSON을 나누지 않고, 공통 row와 Phase focus row를 함께 둔다.
```

Add or keep Measurement Condition label guidance:

```markdown
| Label | Example | Meaning |
|---|---|---|
| `phase` | `phase-01` | Learning Phase |
| `scenario` | `orders` | k6 scenario |
| `preset` | `baseline` | workload preset |
| `pool` | `pool10` | Spring Hikari profile |
```

The guide must state that `userId`, `categoryId`, `page`, order IDs, and SQL text are not Prometheus labels.

- [ ] **Step 2: Update `docs/guides/k6-load-testing.md`**

Document labeled Prometheus mode with this example:

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
```

The guide must explain:

```markdown
`scenario`와 `preset`은 `k6/run.sh` 인자에서 결정한다. `phase`와 `pool`은 환경변수로 전달한다. `userId`, `categoryId`, `page` 같은 요청별 값은 label로 남기지 않는다.
```

The Prometheus examples must use `PHASE=phase-01 POOL=pool10`.

- [ ] **Step 3: Update `docs/guides/environment.md`**

Add or keep this dashboard location note:

```markdown
공통 대시보드는 `DB Lab / DB Lab Overview`를 기준으로 한다. 대시보드는 Spring Boot 서버가 실행되고 k6가 `prometheus` 모드로 한 번 이상 실행된 뒤에 의미 있는 값을 보여준다.
```

The k6 with Prometheus examples must use:

```bash
PHASE=phase-01 POOL=pool10 ./k6/run.sh orders baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh products baseline prometheus
PHASE=phase-01 POOL=pool10 ./k6/run.sh points points-page500 prometheus
```

- [ ] **Step 4: Verify slice**

Run:

```bash
rtk proxy node scripts/verify-observability.mjs
```

Expected: PASS with `Observability configuration verified.`

- [ ] **Step 5: Commit**

```bash
git add docs/guides/grafana-observability.md docs/guides/k6-load-testing.md docs/guides/environment.md
git commit -m "docs: document db lab grafana workflow"
```
