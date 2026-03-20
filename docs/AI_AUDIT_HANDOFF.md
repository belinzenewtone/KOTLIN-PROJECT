# AI Audit Handoff (Personal OS Android Revamp)

## 1) Purpose

This document is the canonical handoff brief for another AI agent to audit the current Android codebase, identify risks/regressions/gaps, and implement fixes safely without breaking architecture boundaries.

## 2) Snapshot (As Of March 20, 2026)

- Repository: `https://github.com/belinzenewtone/DART-2.0`
- Active branch: `codex/full-revamp-android`
- Current head commit: `5a943dc`
- Local project root: `C:\Users\BELINZE NEWTONE\Documents\New project\DART-2.0`
- Canonical engineering rules: [`CODING_RULES.md`](../CODING_RULES.md)

## 3) What Has Been Implemented

- Major architecture revamp with staged migration from legacy `features/*` toward singular `feature/*`.
- 5-tab primary shell pattern (Home, Tasks, Finance, Calendar, Assistant) with guarded navigation contracts.
- Expanded design system and tokenized UI primitives.
- Finance-domain unification foundation and Room-first read/write behavior coverage.
- SMS platform isolation and import/health instrumentation.
- Sync/bootstrap/update/assistant hardening with additional tests and diagnostics.
- Supabase backend migrations and RLS hardening applied through v9.
- Edge functions deployed:
  - `assistant-proxy`
  - `ota-manifest`

## 4) Documentation Map

- Architecture baseline: [`ARCHITECTURE_LOCK.md`](./ARCHITECTURE_LOCK.md)
- Design references + token mapping: [`DESIGN_REFERENCE_PACK.md`](./DESIGN_REFERENCE_PACK.md)
- Feature parity tracker: [`PARITY_MATRIX.md`](./PARITY_MATRIX.md)
- Milestone ticket log: [`MILESTONE_TICKETS.md`](./MILESTONE_TICKETS.md)
- Queue/closure log: [`REMAINING_EXECUTION_QUEUE.md`](./REMAINING_EXECUTION_QUEUE.md)
- Release readiness: [`RELEASE_READINESS_REPORT_2026-03-20.md`](./RELEASE_READINESS_REPORT_2026-03-20.md)
- Signing/env setup: [`RELEASE_SIGNING_AND_ENV_SETUP.md`](./RELEASE_SIGNING_AND_ENV_SETUP.md)
- Schema/screen inventory: [`SCHEMA_INVENTORY.md`](./SCHEMA_INVENTORY.md), [`SCREEN_INVENTORY.md`](./SCREEN_INVENTORY.md)
- Setup/runtime/backend overview: [`README.md`](../README.md)

## 5) UI/UX Source Of Truth

- Primary UI reference: Stitch export pack (external local artifact)
  - Expected file: `C:\Users\BELINZE NEWTONE\Downloads\stitch_home_dashboard.zip`
- Secondary inspiration: Figma concept
  - `https://www.figma.com/make/CQlvcolXsicb4R58dnwnAu/Detailed-description-request?t=lHiVem2hLOQjcQ2y-20&fullscreen=1`
- Implementation policy: Stitch-first, function-first adaptation when visual intent conflicts with Android correctness/performance.

## 6) Backend/Supabase State

- Supabase project URL: `https://vjgtinoegpaaprcyqabe.supabase.co`
- Applied schema/migration set:
  - `supabase_schema.sql` (baseline)
  - `supabase_migration_v4_composite_keys.sql`
  - `supabase_migration_v5_feature_parity.sql`
  - `supabase_migration_v6_event_status.sql`
  - `supabase_migration_v7_release_hardening.sql`
  - `supabase_migration_v8_core_sync_bootstrap.sql`
  - `supabase_migration_v9_grant_hardening.sql`
- RLS posture:
  - RLS enabled on user data tables.
  - `anon` table grants revoked in v9.
  - `authenticated` has CRUD grants, still constrained by RLS policies.
- Edge functions:
  - `assistant-proxy` provider order:
    1. Gemini (`GEMINI_API_KEY`)
    2. deterministic fallback
  - `ota-manifest` serves snake_case manifest contract used by app update flow.

## 7) Current Verification Baseline

Latest local checks executed:

- `.\gradlew.bat :app:check` -> **PASS** (includes boundary check + secret scan + detekt + ktlint + tests + lint)
- `.\gradlew.bat assembleDebug` -> **PASS**
- Debug APK generated:
  - `app/build/outputs/apk/debug/app-debug.apk`

Release readiness caveats:

- Release signing config still required for production artifact signing.
- Connected device matrix QA not executed in this environment.

## 8) Known Risks / Audit Targets

1. Package migration completeness
- Both `feature/*` and legacy `features/*` currently coexist.
- Audit for duplicate ownership and dead bridge paths.

2. Navigation/route alias safety
- Verify no stale deep-link route breaks during phased migration.

3. UI consistency and states
- Confirm loading/empty/error/offline/sync states are consistently represented across all key screens.

4. Finance correctness
- Validate dedupe/import reconciliation and budget/ledger aggregates against real-world edge cases.

5. Assistant safety boundaries
- Confirm no silent mutation path bypasses proposal/preview/explicit commit.

6. Sync idempotency
- Re-check retry behavior and duplicate-prevention semantics on all mutable entities.

7. Release hardening
- Resolve lint warning backlog where material.
- Complete signed release workflow and device QA evidence.

## 9) Required Commands For The Auditor

From repo root:

```powershell
git checkout codex/full-revamp-android
git pull
.\gradlew.bat :app:check
.\gradlew.bat assembleDebug
.\gradlew.bat :app:assembleRelease :app:bundleRelease :app:lintRelease
```

Optional deeper checks:

```powershell
.\scripts\architecture_boundary_check.ps1
.\scripts\secret_scan.ps1
```

Supabase isolation smoke test (requires valid env credentials):

```powershell
$env:SUPABASE_PRIMARY_EMAIL="<confirmed-user-email>"
$env:SUPABASE_PRIMARY_PASSWORD="<confirmed-user-password>"
.\scripts\supabase_two_user_smoke_test.ps1
```

## 10) Non-Negotiable Rules For The Auditor

- Keep architecture chain: `UI -> ViewModel -> UseCase -> Repository -> Local/Network/Platform`.
- Room remains source of truth for user-visible persistent state.
- Do not place provider secrets in Android client source.
- Do not add direct network/database access inside composables.
- Preserve idempotency for sync/import/recurring flows.
- Keep assistant mutations preview-first and explicit-commit only.
- Avoid destructive DB/app migrations.

## 11) Expected Output From The Auditing AI

1. Ranked findings list (`critical`, `high`, `medium`, `low`) with file references.
2. Concrete fix plan grouped by ticket-sized changes.
3. Applied code fixes with tests.
4. Updated docs for any architectural deviations.
5. Final verification log with commands and results.

## 12) Security Hygiene Notes

- Never commit runtime keys, PATs, DB passwords, or model provider keys.
- Use Supabase secrets for function credentials.
- If any key was shared in chat, rotate/revoke it after setup.
