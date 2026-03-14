# AGENTS.md

## Purpose

This file defines how AI coding agents must execute work in this repository.
It is complementary to `CODING_RULES.md`.

## Precedence

- Read `AGENTS.md` first for workflow and execution behavior.
- Read `CODING_RULES.md` next for architecture and quality constraints.
- Follow both files on every task.
- If guidance conflicts, apply the stricter engineering constraint unless the task includes a written exemption.

## Required Workflow for Every Task

1. Confirm the user goal and success criteria.
2. Inspect relevant files before editing.
3. Propose the minimal set of file changes.
4. Implement only task-relevant edits.
5. Run validation checks (tests/lint/analyze where available).
6. Summarize what changed, what was verified, and any remaining risks.

## Architecture and Scope Discipline

- Preserve clean architecture boundaries and feature isolation defined in `CODING_RULES.md`.
- Avoid unrelated refactors.
- Do not rename/move files unless required by the task.
- Keep pull requests/task changes small and reviewable.

## UI Direction (Screenshot Reference)

Use the attached app screenshots as visual product reference:

- Dark theme baseline.
- Glassmorphism surfaces (blurred/frosted cards, rounded corners).
- Bottom tab navigation for: Home, Calendar, Expenses, Tasks, AI, Profile.
- Blue accent highlights for active controls and key actions.

When implementing UI, preserve this design language unless explicitly changed by the user.

## Planning Requirement for Large Changes

Before large changes, provide:

- Intended approach.
- Files expected to change.
- Risks and validation strategy.

## Safety and Data Handling

- Never include secrets, API keys, or credentials in code or docs.
- Prefer environment-based configuration.
- Do not log sensitive personal data.

## Done Criteria

A task is done only when:

- Changes satisfy the requested outcome.
- Relevant checks pass or failures are clearly reported.
- Architecture/style rules were respected.
- Final response includes concise change and verification summary.
