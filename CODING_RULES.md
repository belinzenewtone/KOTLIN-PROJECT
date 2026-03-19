# Kotlin Personal OS Engineering Charter

This document is the canonical engineering charter for the native Android Kotlin Personal OS codebase.
All changes must follow these rules unless a stronger architectural reason is documented in an architecture note.

## 1) Core Principles

- Prioritize clarity over cleverness.
- Keep one architecture path: `UI -> ViewModel -> UseCase -> Repository -> Local DB / Network / Platform`.
- Keep Room as local source of truth for user-visible state.
- Prefer Android-native correctness for SMS, reminders, biometrics, OTA/install, files, and background work.

## 2) Required Structure

- Feature-first organization with `data/domain/presentation/navigation/di` per feature where practical.
- Shared foundational code belongs in `core/*`, platform integrations in `platform/*`.
- Avoid dumping code into vague `utils/helpers/managers` buckets.

## 3) Compose / UI Rules

- Use unidirectional flow: state in, events out.
- Keep composables thin: no DB/network/sync/parser/business logic.
- One screen contract should expose explicit `UiState` and `UiEvent` (`UiEffect` only when required).
- Prefer stateless reusable components and useful previews.
- Split oversized screen files; avoid giant composable files.

## 4) ViewModel Rules

- ViewModels orchestrate screen logic and expose immutable `StateFlow`.
- Do not place low-level DB/parser/platform logic inside ViewModels.
- Keep constructor dependencies focused; large constructor count is a design smell.

## 5) Domain / Use Cases

- Business logic belongs in use cases/domain services with single, clear responsibility.
- Domain models must represent real app concepts (Task, Transaction, Budget, ImportAudit, InsightCard, etc.).
- Prefer pure functions for deterministic and testable logic.

## 6) Repository Rules

- Repositories coordinate data sources, mapping, and sync enqueueing.
- Repositories should hide data source details from UI/ViewModels.
- Keep repositories scoped to one domain area; avoid god repositories.

## 7) Room / Database Rules

- Room is authoritative for persistent, user-visible state.
- Do not use destructive migration fallback in production paths.
- Use explicit migrations and migration tests.
- Keep entities explicit with ownership and lifecycle metadata where relevant:
  `id, userId, createdAt, updatedAt, syncState, source, deletedAt`.

## 8) Sync Rules

- Sync behavior must be explicit, retry-safe, idempotent, and observable.
- Mutable synced records must carry sync metadata statuses:
  `LOCAL_ONLY, QUEUED, SYNCING, SYNCED, FAILED, CONFLICT, TOMBSTONED`.
- Never silently duplicate financial imports/transactions in retry or conflict paths.

## 9) Platform / SMS Rules

- All SMS import runtime logic must stay in `platform/sms`.
- Split SMS responsibilities: receiver/parser/ingestion/dedupe/audit/permissions/background.
- Parser + dedupe must be heavily test-covered with fixtures and regression cases.

## 10) Network / Backend Rules

- UI must not call Supabase/HTTP directly.
- Normalize network payloads via DTOs/mappers and explicit error models.
- No secrets in app source.
- Assistant/AI calls must be backend-mediated, and mutations must be previewable before commit.

## 11) Error Handling Rules

- No silent failures.
- Use typed result/error models where practical.
- Never swallow exceptions in critical paths: sync/import/migrations/auth/updates/export/reminders.

## 12) File Size and Complexity

- New files should remain easy to scan; ~300+ lines triggers decomposition.
- One file should have one main responsibility.
- Use intention-revealing filenames; avoid generic `Utils/Helper/Manager` naming.

## 13) Naming Rules

- Use clear names that reveal behavior and ownership.
- Booleans should read as booleans (`isLoading`, `hasPendingImports`, `canRetry`).

## 14) Modern Kotlin Rules

- Use immutable data models by default.
- Use sealed classes/interfaces for event/result/state models when useful.
- Prefer coroutines + Flow (`suspend` for one-shot work, `Flow` for streams).
- Favor composition over inheritance.

## 15) Dependency Injection

- Use Hilt with constructor injection by default.
- Keep modules organized by feature/core boundary.
- Avoid service locator patterns.

## 16) Testing Rules

- Prioritize business-critical tests: finance calculations, parser/dedupe, recurring, search, sync, migrations, assistant action parsing.
- Add migration tests for Room changes.
- Add repository integration tests for Room + sync behavior.

## 17) Performance Rules

- Optimize for low-end through flagship devices.
- Avoid heavy recomposition and rendering overhead.
- Keep startup coordinated and lightweight.
- Keep background tasks battery/constraint aware.

## 18) Documentation Rules

- Major feature areas must have concise architecture notes:
  purpose, entry points, data flow, constraints, and test notes.
- Document tricky flows (SMS, sync, OTA, assistant actions, recurring, biometrics, export).

## 19) Code Review Checklist

- Does this preserve the architecture chain?
- Is UI free from direct network/DB operations?
- Is Room still source of truth?
- Are sync/idempotency and financial dedupe guarantees preserved?
- Are error handling and tests adequate for changed critical logic?
- Is the change still easy to read and debug in 6+ months?

## 20) Anti-Patterns to Avoid

- God classes/repositories.
- Hidden side effects and implicit sync behavior.
- Business logic in composables.
- Scattered SMS logic.
- Assistant mutations without preview/confirmation.
- Destructive migrations and silent catch blocks.

## 21) Success Standard

The codebase should remain:

- easy to scan, debug, and extend,
- robust in offline + sync-aware behavior,
- safe for finance-related workflows,
- strong in Android-native platform features,
- maintainable for long-term ownership,
- aligned with modern Kotlin/Android practice.
