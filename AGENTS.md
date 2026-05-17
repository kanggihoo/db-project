# Agent Instructions

정확하고 명확하게 응답

@/Users/kkh/.codex/RTK.md

## Agent skills

### Issue tracker

Issues and PRDs are tracked in GitHub Issues for `kanggihoo/db-project`; use `gh` CLI operations from this repository. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default five-label vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

This is a single-context repo: read root `CONTEXT.md` and `docs/adr/` when present, proceeding silently if they do not exist yet. See `docs/agents/domain.md`.

### Phase documentation

Learning Phase 작업은 `docs/agents/phase-documentation.md`를 따른다.

모든 Phase 디렉토리를 훑지 않는다. 먼저 대상 Phase를 확정한 뒤 아래만 읽는다.

- `docs/phases/README.md`
- `docs/phases/<target-phase>/README.md`
- 필요한 경우에만 해당 README가 연결한 추가 문서
