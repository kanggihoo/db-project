# Agentic Issue Workflow

이 문서는 `docs/roadmap/`의 장기 로드맵을 바탕으로 Superpowers spec/plan 문서를 만들고, `to-issues`로 GitHub Issues를 생성한 뒤, issue 단위로 구현과 리뷰를 진행하는 운영 규칙을 정리한다.

## Goal

- `docs/roadmap/`은 장기 방향과 Phase 목표를 관리한다.
- `docs/superpowers/specs/`는 무엇을 왜 만들지 결정한 spec을 관리한다.
- `docs/superpowers/plans/`는 구현자가 그대로 따라갈 수 있는 상세 plan을 관리한다.
- GitHub Issues는 작업 진행 상태, 담당 단위, 의존 관계, PR 연결을 관리한다.

문서가 진실의 원천이고, issue는 실행 현황판이다. issue 본문에 plan 전체를 복사하지 말고 source 문서 링크와 완료 조건을 둔다.

## Recommended Flow

```text
docs/roadmap/*
  -> superpowers:brainstorming
  -> docs/superpowers/specs/<feature>-design.md
  -> superpowers:writing-plans
  -> docs/superpowers/plans/<feature>/000-plan-index.md
  -> to-issues
  -> GitHub parent tracking issue + child implementation issues
  -> superpowers:subagent-driven-development
  -> PR review against spec, plan, and issue acceptance criteria
  -> issue update and close
```

## Source Documents

이슈를 만들 때는 다음 자료를 함께 제공한다.

| Document | Purpose |
|---|---|
| `docs/roadmap/*` | 작업이 어떤 Phase 또는 장기 목표에서 나왔는지 설명한다. |
| `docs/superpowers/specs/<feature>-design.md` | 목적, 범위, 비범위, 성공 기준을 제공한다. |
| `docs/superpowers/plans/<feature>/000-plan-index.md` | plan 디렉터리의 전체 순서와 의존 관계를 제공한다. |
| `docs/superpowers/plans/<feature>/*.md` | 구현 단계, 테스트, 검증 명령을 제공한다. |
| `CONTEXT.md` 또는 `CONTEXT-MAP.md` | 프로젝트 도메인 언어와 경계를 제공한다. |
| `docs/adr/*.md` | 이미 결정된 아키텍처 제약을 제공한다. |

현재 repo에 `CONTEXT.md`와 `docs/adr/`가 없다면 먼저 `setup-matt-pocock-skills`로 agent 설정을 만들고, 이후 Phase가 쌓이면서 필요한 ADR을 추가한다.

## Plan Directory Convention

큰 plan은 단일 파일보다 디렉터리 구조를 사용한다.

```text
docs/superpowers/plans/<feature>/
├── 000-plan-index.md
├── 001-first-vertical-slice.md
├── 002-second-vertical-slice.md
├── 003-third-vertical-slice.md
└── 999-integration-stabilization.md
```

`000-plan-index.md`는 전체 흐름을 보여주는 색인이다. 각 세부 plan 파일은 구현자가 직접 따라갈 수 있을 정도로 구체적이어야 한다. 파일 경로, 테스트 코드, 실행 명령, 기대 결과를 포함한다.

`*-implementation-notes.md` 같은 보조 문서가 있으면 issue 본문에는 링크만 둔다. 세부 구현 내용은 plan 문서에 남기고, issue에는 무엇을 만들고 어떻게 완료를 판단할지만 둔다.

## Issue Model

### Parent Tracking Issue

Parent tracking issue는 큰 spec/plan 묶음을 추적하는 상위 허브다. 직접 구현하는 티켓이라기보다 진행 상황을 한곳에서 보는 용도다.

Parent issue에 포함할 내용:

- source roadmap, spec, plan index 링크
- 전체 목표
- 범위와 비범위 요약
- child issue 체크리스트
- 완료 조건

예시:

```markdown
## Source documents

- Roadmap: docs/roadmap/03-phase-2-indexes.md
- Spec: docs/superpowers/specs/phase-2-index-optimization-design.md
- Plan index: docs/superpowers/plans/phase-2-index-optimization/000-plan-index.md

## Goal

상품 검색 경로에서 인덱스 적용 전후의 성능 차이를 테스트, SQL 분석, k6/Grafana 증빙으로 확인한다.

## Child issues

- [ ] #123
- [ ] #124
- [ ] #125

## Done when

- 모든 child issue가 닫혔다.
- spec의 성공 기준이 충족됐다.
- 최종 증빙 문서가 업데이트됐다.
```

### Child Implementation Issues

Child issue는 구현과 검증의 기본 단위다. `to-issues`는 plan 파일을 그대로 issue로 복사하지 않고, 독립적으로 검증 가능한 vertical slice로 나눈다.

좋은 child issue:

- 하나의 좁은 end-to-end 경로를 완성한다.
- 완료 후 독립적으로 테스트하거나 시연할 수 있다.
- 필요한 경우 schema, API, service, repository, test, docs를 함께 지난다.
- `AFK` 또는 `HITL` 여부가 분명하다.

좋지 않은 child issue:

- DB 수정만 하는 issue
- service 수정만 하는 issue
- 테스트 작성만 하는 issue
- plan 파일 전체를 그대로 복사한 issue

Child issue 본문 예시:

```markdown
## Parent

#122

## Source documents

- Spec: docs/superpowers/specs/phase-2-index-optimization-design.md
- Plan: docs/superpowers/plans/phase-2-index-optimization/002-product-search-index.md
- Notes: docs/superpowers/plans/phase-2-index-optimization/002-product-search-index-implementation-notes.md

## What to build

상품 검색 API에서 category, status, soft delete 조건을 포함한 조회 경로를 구현하고, 인덱스 적용 전후 실행계획과 응답시간 차이를 검증한다.

## Acceptance criteria

- [ ] 검색 조건별 통합 테스트가 통과한다.
- [ ] 인덱스 적용 전 `EXPLAIN ANALYZE` 결과가 증빙 문서에 기록된다.
- [ ] 인덱스 적용 후 실행계획과 응답시간 개선이 기록된다.
- [ ] k6 또는 지정된 검증 명령 결과가 parent issue에 연결된다.

## Blocked by

- #123
```

## Vertical Slice Rules

`to-issues`를 사용할 때는 horizontal layer가 아니라 tracer bullet vertical slice로 나눈다.

Horizontal split 예시:

```text
- Entity 작성
- Repository 작성
- Service 작성
- Controller 작성
- 테스트 작성
```

Vertical slice 예시:

```text
- 상품 검색 조건의 baseline 쿼리와 응답시간을 기록한다.
- 상품 검색에 복합 인덱스를 적용하고 실행계획 차이를 증명한다.
- soft delete 조건이 인덱스 선택에 미치는 영향을 테스트와 문서로 남긴다.
```

각 slice는 작아야 하지만, 완료되면 의미 있는 결과가 남아야 한다.

## AFK And HITL

`to-issues`는 각 issue를 `AFK` 또는 `HITL`로 구분한다.

| Type | Meaning | Example |
|---|---|---|
| `AFK` | agent가 추가 human context 없이 구현 가능 | 기존 plan에 맞춘 테스트, 구현, 문서 증빙 |
| `HITL` | human decision이 필요한 작업 | 인덱스 전략 선택, 도메인 범위 변경, 성능 기준 조정 |

가능하면 `AFK` issue를 선호한다. 단, 아키텍처 결정이나 실험 기준이 불명확하면 `HITL`로 분리한다.

## GitHub Issue Publishing

권장 issue tracker는 GitHub Issues다. 승인된 issue breakdown은 `gh` CLI로 dependency 순서대로 생성한다.

운영 순서:

1. Parent tracking issue를 먼저 생성한다.
2. Blocker가 없는 child issue부터 생성한다.
3. 뒤 issue의 `Blocked by`에는 앞에서 생성된 실제 issue 번호를 넣는다.
4. 각 child issue에 `ready-for-agent` 또는 적절한 triage label을 붙인다.
5. GitHub Projects를 사용한다면 parent와 child issue를 같은 project에 넣는다.

권장 label:

| Role | Label |
|---|---|
| maintainer evaluation | `needs-triage` |
| waiting on reporter | `needs-info` |
| agent-ready work | `ready-for-agent` |
| human implementation | `ready-for-human` |
| not planned | `wontfix` |

## Implementation Flow

Child issue 하나를 구현할 때의 기본 흐름:

```text
1. issue 본문을 읽는다.
2. source spec과 plan 파일을 읽는다.
3. 필요하면 CONTEXT.md와 관련 ADR을 읽는다.
4. superpowers:using-git-worktrees로 격리된 worktree를 만든다.
5. superpowers:subagent-driven-development로 plan task를 실행한다.
6. 테스트, k6, SQL 분석, Grafana 증빙 등 plan의 검증 명령을 수행한다.
7. PR을 만들고 issue를 연결한다.
8. superpowers:requesting-code-review로 spec, plan, diff 기준 리뷰를 수행한다.
9. issue에 결과와 증빙 링크를 남긴다.
10. acceptance criteria가 모두 충족되면 issue를 닫는다.
```

## Issue Update Rules

구현 중 issue에는 진행 상태만 짧게 남긴다. 세부 구현 설명은 PR과 plan 체크박스에 남긴다.

좋은 update:

```markdown
Progress update:

- Implemented baseline product search integration test.
- Captured pre-index EXPLAIN ANALYZE in docs/evidence/phase-2/product-search-baseline.md.
- Next: add composite index and compare execution plan.
```

완료 update:

```markdown
Completed:

- PR: #140
- Evidence: docs/evidence/phase-2/product-search-index-comparison.md
- Verification: `./gradlew test`, k6 product search scenario

All acceptance criteria are met.
```

## Prompt Template For `to-issues`

다음 프롬프트를 기준으로 사용한다.

```text
다음 문서들을 기반으로 GitHub Issues를 만들어줘.

- Roadmap: docs/roadmap/<source>.md
- Spec: docs/superpowers/specs/<feature>-design.md
- Plan index: docs/superpowers/plans/<feature>/000-plan-index.md
- Plan directory: docs/superpowers/plans/<feature>/
- Context: CONTEXT.md
- ADRs: docs/adr/

요구사항:
- 먼저 parent tracking issue를 하나 만들고 child issue들은 parent를 참조하게 해줘.
- child issue는 plan 파일을 그대로 복사하지 말고 vertical slice 단위로 나눠줘.
- 각 issue는 AFK/HITL로 표시해줘.
- 각 issue에는 source spec, plan file, 관련 notes 링크를 넣어줘.
- 파일 경로나 세부 코드는 issue 본문에 과하게 넣지 말고 plan 링크를 보게 해줘.
- Acceptance criteria와 Blocked by 관계를 명확히 작성해줘.
- 먼저 breakdown을 보여주고 승인받은 뒤 gh CLI로 GitHub Issues에 업로드해줘.
```

## Practical Rule

기본 순서는 항상 다음으로 둔다.

```text
spec first, plan second, issues third, implementation fourth
```

spec 없이 issue를 만들면 방향이 흔들린다. plan 없이 issue를 만들면 작업 단위가 추상적이다. issue 없이 구현하면 진행 상태와 의존 관계가 흩어진다.
