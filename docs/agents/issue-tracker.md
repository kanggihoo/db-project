# Issue tracker: GitHub

Issues and PRDs for this repo live as GitHub Issues. Use the `gh` CLI for all operations.

## Repository

- GitHub repo: `kanggihoo/db-project`
- Remote: `https://github.com/kanggihoo/db-project.git`

Infer the repo from `git remote -v`. `gh` does this automatically when run inside this clone.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, fetching labels and comments.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

For plan-driven work, first draft the breakdown and get approval. After approval, publish issues in dependency order so later issues can reference actual blocker issue numbers.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## Project issue workflow

This repo uses roadmap, spec, plan, and issue documents together:

```text
docs/roadmap/*
  -> docs/superpowers/specs/*
  -> docs/superpowers/plans/<feature>/
  -> GitHub parent tracking issue
  -> GitHub child implementation issues
```

Use `docs/guides/agentic-issue-workflow.md` for the detailed workflow.

When converting a spec or plan to issues:

- Create one parent tracking issue for the larger spec/plan bundle.
- Create child implementation issues as vertical slices, not layer-by-layer tasks.
- Include links to the source roadmap, spec, plan index, and relevant plan files.
- Keep detailed implementation instructions in plan files; keep issue bodies focused on behavior, acceptance criteria, and blockers.
