# Milestone Tickets

## Status Snapshot (2026-03-20)
- Completed: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19.
- In progress: none.
- Newly completed in this slice: 1, 6, 8.
- Added hardening ticket: 19 (AGP 9 compatibility upgrade gate).

## Ticket Template

Title:
Scope:
Why:
Modules touched:
Out of scope:
Acceptance criteria:
Tests required:
Performance considerations:
Migration risk:
UX notes:

## Initial Backlog

### 1) Final Package Structure Alignment
Title: Establish final package structure and migrate to target module layout
Scope: Map `features/*` into target `feature/*` contracts and core/platform boundaries.
Why: Enforces long-term maintainability and ownership boundaries.
Modules touched: `bootstrap`, `navigation`, `core/*`, `features/*`, `platform/*`
Out of scope: Visual redesign of all screens.
Acceptance criteria: Target folders exist, docs updated, imports compile.
Tests required: Build + architecture boundary check.
Performance considerations: None.
Migration risk: Medium.
UX notes: No behavior regression.

### 2) Startup Runtime Orchestration
Title: Introduce `AppBootstrapCoordinator` and remove scattered startup logic
Scope: Centralize startup lifecycle decisions and route choice.
Why: Deterministic startup and easier diagnostics.
Modules touched: `bootstrap`, `MainActivity`, `LifeOSApplication`
Out of scope: Full onboarding redesign.
Acceptance criteria: Startup follows deterministic sequence and returns structured result.
Tests required: Bootstrap unit tests.
Performance considerations: Keep startup lightweight.
Migration risk: Medium.
UX notes: Smooth splash transition.

### 3) Room Source-of-Truth Enforcement
Title: Enforce Room as authoritative local state boundary
Scope: Keep UI backed by Room flows; reconcile network/platform writes into Room.
Why: Offline correctness and deterministic state.
Modules touched: `core/database`, repositories
Out of scope: Backend schema redesign.
Acceptance criteria: No UI reads directly from DTO/network result streams.
Tests required: Repository integration tests.
Performance considerations: Query/index tuning.
Migration risk: Medium.
UX notes: Stable offline behavior.

### 4) Sync Engine v1
Title: Implement sync queue schema and `SyncCoordinator` v1
Scope: Queue store, dispatcher, status tracking, retry/backoff, repair entrypoint.
Why: Idempotent and observable sync behavior.
Modules touched: `core/sync`, `core/database`, `core/work`
Out of scope: Full conflict UI.
Acceptance criteria: Mutable changes enqueue retry-safe jobs with observable status.
Tests required: Sync unit + integration tests.
Performance considerations: Respect battery/connectivity constraints.
Migration risk: Medium.
UX notes: Sync failures are visible.

### 5) Canonical Metadata Standardization
Title: Standardize entity `syncState` / `source` / lifecycle metadata
Scope: Add metadata to mutable entities and migration coverage.
Why: Consistent sync and audit semantics.
Modules touched: `core/database/entity`, migrations
Out of scope: Historical data backfills beyond schema defaults.
Acceptance criteria: Required metadata columns present and used.
Tests required: Migration tests.
Performance considerations: Index where needed.
Migration risk: High.
UX notes: Better sync status clarity.

### 6) Design Tokens and Core Scaffold
Title: Build design tokens and core `PageScaffold` / `AppCard`
Scope: Token system and reusable layout primitives.
Why: Consistent premium UI foundation.
Modules touched: `core/ui/designsystem`, `ui/theme`
Out of scope: Full screen redesign.
Acceptance criteria: Shared components available and adopted by key screens.
Tests required: Compose smoke tests.
Performance considerations: Avoid deep recomposition.
Migration risk: Low.
UX notes: Consistency uplift.

### 7) Navigation Shell Refactor
Title: Refactor main navigation into shell + guarded flows
Scope: Route guards and deterministic shell behavior.
Why: Reduces auth/state edge-case bugs.
Modules touched: `navigation`, `bootstrap`, `features/auth`
Out of scope: Deep-link expansion beyond current route set.
Acceptance criteria: Guarded auth/home routing works from cold start.
Tests required: Navigation UI tests.
Performance considerations: Lightweight nav state.
Migration risk: Medium.
UX notes: Cleaner route transitions.

### 8) Finance Domain Unification
Title: Unify finance models into ledger/budget/import/insights subdomains
Scope: Consolidate expenses/budget/income data contracts.
Why: Removes duplication and drift.
Modules touched: `features/expenses`, `features/budget`, `features/income`, `feature/finance` target
Out of scope: New charting library.
Acceptance criteria: Unified finance repository contracts and shared domain types.
Tests required: Finance calculation tests.
Performance considerations: Query optimization for ledger lists.
Migration risk: High.
UX notes: Cohesive finance experience.

### 9) SMS Subsystem Isolation
Title: Create `platform/sms` package and isolate all SMS import logic
Scope: Parser, ingestion, dedupe, audit, receiver, worker.
Why: Native reliability and clear ownership.
Modules touched: `platform/sms`, finance integration points
Out of scope: iOS parity.
Acceptance criteria: SMS import runtime code only lives in `platform/sms`.
Tests required: Parser + dedupe tests.
Performance considerations: Batch processing and memory safety.
Migration risk: Medium.
UX notes: Clear import status and recovery.

### 10) Import Audit + Review Queue
Title: Add `ImportAudit` persistence and review queue model
Scope: Persist outcomes and unresolved candidates.
Why: Traceability and safer financial imports.
Modules touched: `core/database`, finance import flow
Out of scope: Final review UX polish.
Acceptance criteria: Outcomes recorded with timestamps and queryable.
Tests required: DAO and flow tests.
Performance considerations: Index on outcome/time.
Migration risk: Medium.
UX notes: Better trust in imports.

### 11) Deterministic Insight Engine
Title: Build deterministic insight engine baseline
Scope: Local rules for insight cards and caching.
Why: Offline insight parity.
Modules touched: `features/insights` target, `core/database`
Out of scope: Full AI ranking.
Acceptance criteria: Deterministic cards generated from local data.
Tests required: Insight rule unit tests.
Performance considerations: Incremental generation.
Migration risk: Low.
UX notes: Insights work offline.

### 12) Assistant Proposal Safety
Title: Introduce assistant action proposal model with preview UX
Scope: Proposal + explicit commit path.
Why: Prevent silent mutation from assistant actions.
Modules touched: `features/assistant`
Out of scope: Model-provider changes.
Acceptance criteria: Mutations require explicit user confirmation.
Tests required: Assistant action parser/commit tests.
Performance considerations: Keep conversation caching local-first.
Migration risk: Medium.
UX notes: Clear trust boundaries.

### 13) Remote Config + Feature Flags
Title: Add remote config / feature flags support
Scope: Local flag store, remote fetch, bootstrap refresh.
Why: Controlled rollout and kill-switch support.
Modules touched: `core/datastore`, `core/network`, `bootstrap`
Out of scope: Complex targeting UI.
Acceptance criteria: Flags are fetched, cached, and consumed in runtime decisions.
Tests required: Flag store and fetch tests.
Performance considerations: Lightweight startup fetch.
Migration risk: Low.
UX notes: Stable staged rollouts.

### 14) Export Framework
Title: Build export framework with JSON/CSV/date range support
Scope: Domain export service and share flow.
Why: Reliable user data portability.
Modules touched: `features/export`, `platform/files`
Out of scope: Cloud backup.
Acceptance criteria: JSON + CSV export for selected ranges works.
Tests required: Export format tests.
Performance considerations: Stream large exports.
Migration risk: Low.
UX notes: Clear export summary.

### 15) Room Migration Test Suite
Title: Add Room migration tests for all production upgrades
Scope: Verify schema upgrades and preserved data.
Why: Prevent release-time data loss.
Modules touched: `core/database`, `core/testing`, `src/test`
Out of scope: Downgrade migrations.
Acceptance criteria: Migration suite runs in CI and passes.
Tests required: Migration tests.
Performance considerations: Keep fixtures lean.
Migration risk: High.
UX notes: Safe app upgrades.

### 16) M-Pesa Parser Fixture Suite
Title: Add parser fixture regression suite for M-Pesa messages
Scope: Real-world variants and edge cases.
Why: Import reliability.
Modules touched: `platform/sms/parser`, `src/test`
Out of scope: OCR import.
Acceptance criteria: Fixture suite covers key format variants and regressions.
Tests required: Parser fixture tests.
Performance considerations: Fast deterministic parsing.
Migration risk: Low.
UX notes: Fewer missed transactions.

### 17) Update Manifest Hardening
Title: Add update manifest model and robust check flow
Scope: Version policy, diagnostics persistence, install handoff safety.
Why: Safer OTA lifecycle.
Modules touched: `core/update`, `features/settings`
Out of scope: Play Store in-app updates.
Acceptance criteria: Required/optional logic and diagnostics are surfaced.
Tests required: Manifest parsing tests.
Performance considerations: Efficient download checks.
Migration risk: Medium.
UX notes: Clear update prompts.

### 18) Diagnostics Surface
Title: Create debug/diagnostic surfaces for sync/import/update health
Scope: Developer-facing diagnostics panel and runtime summaries.
Why: Faster issue triage and operational visibility.
Modules touched: `core/telemetry`, `features/settings`
Out of scope: External observability backend integration.
Acceptance criteria: In-app diagnostics show queue/import/update health.
Tests required: ViewModel tests.
Performance considerations: Use Flow aggregation, avoid heavy polling.
Migration risk: Low.
UX notes: Expose health without cluttering core user flows.

### 19) AGP 9 Upgrade Gate
Title: Upgrade toolchain to AGP 9 with compatible Kotlin/KSP/Hilt stack
Scope: Move to AGP 9 and required Gradle/Kotlin/KSP/Hilt versions only after all processors are compatible.
Why: Future-proof build stack and keep platform support current.
Modules touched: `gradle/libs.versions.toml`, `gradle/wrapper`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`
Out of scope: Feature implementation changes.
Acceptance criteria: Project builds and passes `check` on AGP 9 without experimental suppressions for source-set wiring.
Tests required: Full `check` and smoke install build.
Performance considerations: Keep configuration/build times within current baseline bounds.
Migration risk: Medium.
UX notes: No direct UX changes; reliability/maintainability improvement.
