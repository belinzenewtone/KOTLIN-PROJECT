# OVERHAUL_IMPLEMENTATION_PLAN.md

## Objective

Upgrade the Kotlin BELTECH app to match or exceed the Dart 2.0 engineering standard.

## Phase Plan

1. Guardrails and Baseline
- Add coding rules, quality tooling (`detekt`, `ktlint`), and test scaffolding.
- Establish measurable acceptance criteria for parity and security.

2. Security Refactor
- Remove direct OpenAI key usage from client.
- Route assistant calls through proxy endpoint.
- Migrate sensitive local auth/password handling to encrypted storage.

3. Data and Sync Correctness
- Align local entity ownership fields with Supabase RLS model.
- Replace destructive migration paths with explicit Room migrations.
- Implement robust sync conflict and retry strategy.

4. Feature Parity
- Implement missing modules present in Dart project:
  - budget
  - income
  - recurring
  - search
  - export
  - settings

5. Notifications and Background Work
- Implement reminder scheduling and cancellation for tasks/events.
- Introduce WorkManager-backed background processing for SMS and sync jobs.

6. UI System Refactor
- Split oversized screens into reusable components.
- Improve glass implementation consistency and accessibility.
- Add theme mode support (dark/light/system).

7. Test and Release Hardening
- Add repository/use-case/integration coverage for critical workflows.
- Run migration and sync verification against real schema scenarios.
- Prepare release checklist and rollout plan.

## Current Execution Slice

- [x] Guardrail docs created.
- [x] Static analysis plugins added to Gradle.
- [x] Unit test dependencies added.
- [x] Proxy-only assistant flow wired.
- [x] Initial parser tests added.
- [x] Supabase auth/session and user-scoped sync wired.
- [x] Room migration to user-scoped composite keys (v4) added.
- [x] Sync retry/backoff + conflict target hardening implemented.
- [x] Event reminder scheduling and notification receiver foundation added.
- [x] Task deadline reminders and WorkManager SMS ingestion implemented.
- [x] Global notifications preference now enforced by reminder scheduling and receivers.
- [x] Notifications toggle now performs immediate cleanup of already-scheduled reminders.
- [x] Feature parity scaffold added for budget, income, and recurring modules with navigation entry.
- [x] Budget, income, and recurring modules migrated to Room + cloud sync table wiring.
- [x] Search, export, and settings modules added with planner tab integration.
- [x] Recurring execution engine added (hourly worker materializes due rules and advances cadence).
- [x] Strict two-user Supabase isolation smoke test passed for all synced tables.
- [x] Recurring execution idempotency hardened with atomic `advanceIfDue` guard.
- [x] Release hardening checklist and runnable verification script added.
- [x] UI complexity pass (option 2) advanced: `Profile`, `Expenses`, `Dashboard`, and `Tasks` screens decomposed into reusable component modules.
- [x] UI consistency hardening: shared spacing tokens (`AppSpacing`) and bottom-safe/FAB-safe screen insets applied across major screens.
- [x] UI complexity pass extended: `Calendar`, `Analytics`, `Assistant`, and `Auth` screens decomposed into reusable component modules.
- [x] UI complexity pass completed for remaining planner modules: `Recurring`, `Budget`, and `Income` screens decomposed into reusable component modules.
- [x] Static quality gates are now blocking (`detekt` + `ktlintCheck`) with baseline files for legacy debt tracking.
- [x] Additional automated coverage added for recurring execution idempotency/materialization, secure auth session persistence, and DB migration v4->v5 table/index creation.
- [x] Global padding/inset consistency pass applied across planner tabs and primary list screens to reduce overlap risk and improve layout uniformity.
- [x] Release verification evidence captured: unit tests + blocking static checks + strict two-user Supabase smoke test (`FULL_PASS`).
- [x] Calendar parity hardening: priority chip, swipe-to-complete/delete, edit flow, and event/due date-time selection implemented.
- [x] OTA updates implemented for APK distribution (non-Play): remote manifest check, in-app APK download, and installer handoff from Settings.
