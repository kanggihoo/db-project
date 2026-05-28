# 016 Report And Evidence Docs

## Goal

Phase 7 report와 evidence index를 재측정 spec의 A/B/C 구조로 갱신한다.

## Files

- Modify: `docs/phases/07-pagination/report.md`
- Modify: `docs/evidence/phase-07/README.md`
- Optional modify: `docs/phases/07-pagination/runbook.md`
- Optional modify: `docs/phases/07-pagination/observability.md`

## Steps

- [ ] **Step 1: Update evidence index**

Add retest files to `docs/evidence/phase-07/README.md` in a separate section named `Retest Sampling Result Summary`.

Include these rows:

```text
retest hot user profile
retest cursor samples
retest offset sampling EXPLAIN
retest cursor page10/page1000/page4999 EXPLAIN
retest count-only EXPLAIN
retest offset sampling k6
retest cursor sampling k6
retest offset pg_stat_statements
retest cursor pg_stat_statements
retest Grafana screenshot
```

- [ ] **Step 2: Rewrite report structure**

Use this structure in `docs/phases/07-pagination/report.md`.

```text
1. Summary
2. Measurement Condition
3. Why Page and Cursor Are Not the Same UX
4. A. Offset/Page Depth Result
5. B. Cursor Next-Slice Result
6. C. Count Query Result
7. Interpretation
8. Limitations
9. Phase 8 Handoff
```

- [ ] **Step 3: Add Summary wording**

Use this summary framing:

```text
Phase 7 재측정은 Offset/Page와 Cursor를 단순 속도 경쟁으로 비교하지 않는다.
Offset/Page는 임의 page 접근과 total count 제공을 위해 skip/count 비용을 부담한다.
Cursor는 임의 page 점프를 제공하지 않지만, 이미 cursor를 가진 순차 탐색에서는 다음 slice를 count 없이 조회한다.
이번 재측정은 이 trade-off를 point_history hot user 707000 조건에서 A/B/C evidence로 분리해 확인했다.
```

- [ ] **Step 4: Add A/B/C result sections**

Each result section must include:

```text
측정 조건
핵심 수치 표
원본 evidence 링크
해석
한계
```

For A, state that k6 sampling is warm-cache repeated load and not cold read evidence.

For B, state that cursor values were precomputed and cursor source lookup was not included in API latency.

For C, state that count query cost is separate from page depth skip cost.

- [ ] **Step 5: Add Grafana interpretation**

Add this wording:

```text
Grafana screenshot은 기존 shared DB Lab Overview를 캡처한 보조 evidence다.
Phase 7 재측정을 위해 별도 dashboard나 panel은 추가하지 않았다.
Offset/Page의 skip 비용은 EXPLAIN으로, count query 비용은 pg_stat_statements로 판정한다.
Grafana는 k6 run 중 request rate, failure rate, Hikari 상태, table access 상태를 함께 확인하기 위한 보조 자료로 사용한다.
```

- [ ] **Step 6: Update runbook and observability only if commands changed**

If the new sampling commands are expected to be reused, add them to:

```text
docs/phases/07-pagination/runbook.md
docs/phases/07-pagination/observability.md
```

Keep Phase 7 directory limited to the standard five files.

## Done When

- [ ] `report.md` no longer frames Cursor as a direct replacement for numbered page jump.
- [ ] A/B/C evidence links are present.
- [ ] Grafana is described as auxiliary evidence only.
- [ ] `docs/evidence/phase-07/README.md` links every new retest artifact.

