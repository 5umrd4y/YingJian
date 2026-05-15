# Git Safety Notes

This project has previously lost work because another tool reverted or reset the wrong code. Use this workflow for the photobook redesign.

## Before Claude Starts

Run:

```bash
git status --short
git branch --show-current
git rev-parse --short HEAD
```

If possible, create a dedicated branch:

```bash
git checkout -b codex/photobook-slot-layout-redesign
```

If work must stay on `main`, commit a checkpoint before broad edits.

## Before Risky Operations

Do not run these without a checkpoint:

```bash
git reset --hard
git checkout -- .
git restore .
git clean -fd
git clean -fdx
```

Before any reset/revert/clean operation, create a snapshot:

```bash
git status --short > /tmp/yingjian-status-before-risk.txt
git diff > /tmp/yingjian-worktree-before-risk.patch
git diff --staged > /tmp/yingjian-staged-before-risk.patch
git ls-files --others --exclude-standard > /tmp/yingjian-untracked-before-risk.txt
```

## During Implementation

- Commit after each stable phase when possible.
- Use focused commit messages.
- Stage explicit files, not broad paths.
- Never use `git add .` while this repository contains many unrelated untracked files.
- Do not stage generated build outputs unless explicitly requested.

## Suggested Phase Commits

```text
feat: add photobook slot layout model
feat: render photobook templates from shared layout engine
feat: support photobook slot photo picker and movement
feat: unify photobook preview and pdf layout
```

## Handoff Back To Codex

Ask Codex to review:

- `git status --short`
- `git diff --stat`
- changed files under `app/src/main/java/com/yingjian/feature/photobook`
- changed DB/schema files if any
- build/test output
- manual QA result using `validation.md`

