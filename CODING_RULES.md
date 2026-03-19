# CODING_RULES.md

## Global Engineering Rules

This document defines mandatory rules for the Kotlin BELTECH project.
All contributors and agents must follow these constraints.

## KR-01: Architecture Boundaries

- Required flow: `presentation -> domain -> data`.
- `presentation` must not call Room/HTTP clients directly.
- Business logic belongs in `domain`.
- Data source and mapping logic belongs in `data`.

## KR-02: Feature Modularity

- Each feature must include:
  - `data/`
  - `domain/`
  - `presentation/`
- Cross-feature dependencies must go through domain contracts or shared `core` abstractions.

## KR-03: File Size and Composition

- Target max file length is 300 lines.
- Files over 300 lines must be split by responsibility.
- Screens should orchestrate composables, not contain all UI internals.

## KR-04: UI and Theme

- Shared styles/tokens live in `ui/theme` and `ui/components`.
- Avoid one-off hardcoded colors in feature screens.
- Reuse `GlassCard` and shared surface abstractions.

## KR-05: State and Async Handling

- ViewModels must expose explicit loading/success/error states.
- Silent failure paths are forbidden.
- Exceptions must map to actionable UI states.

## KR-06: Security

- Plain-text passwords are forbidden.
- Sensitive credentials/tokens must use encrypted storage.
- Client apps must not embed provider secrets.
- AI calls must go through a backend proxy endpoint.

## KR-07: Data and Migrations

- Destructive migration must not be default behavior for production paths.
- Schema upgrades must use explicit Room migrations.
- Sync models must include ownership fields compatible with Supabase RLS.

## KR-08: Testing

- Required tests for repositories, use cases, parsers, and security logic.
- New business logic must ship with tests.
- Priority tests:
  - SMS parsing and dedupe
  - Auth/session handling
  - Sync/merge logic

## KR-09: Tooling and Quality Gates

- `detekt` and `ktlint` are required and must run in CI.
- No new warnings in touched files without explicit justification.

## KR-10: Dependency Governance

- Add dependencies only when existing platform/project tools are insufficient.
- Prefer maintained and widely adopted libraries.

## Compliance Checklist

- Architecture boundaries preserved.
- No sensitive data stored in plaintext.
- Async error handling present.
- Test coverage added/updated for changed business logic.
- New code follows module and style rules.
