# Architecture Lock (Milestone 0)

## Target Structure

The codebase is migrating toward:

- `bootstrap/`
- `navigation/`
- `core/common`
- `core/database`
- `core/datastore`
- `core/network`
- `core/sync`
- `core/security`
- `core/notifications`
- `core/work`
- `core/telemetry`
- `core/update`
- `core/ui`
- `core/testing`
- `feature/*`
- `platform/sms`
- `platform/files`
- `platform/alarms`
- `platform/biometrics`
- `platform/packageinstaller`

## Non-negotiables

- UI -> ViewModel -> UseCase -> Repository -> Local/Network/Platform.
- Room remains local source of truth.
- Sync jobs must be idempotent and retry-safe.
- AI mutations require explicit proposal and commit.
- SMS import logic remains isolated in platform package.

## Current Milestone-0/1 Deliverables Added

- Bootstrap coordinator and startup model.
- Sync queue table + coordinator + dispatcher + backoff + status tracker.
- Canonical metadata columns for mutable entities.
- New foundation tables:
  - `sync_jobs`
  - `import_audit`
  - `assistant_conversations`
  - `assistant_messages`
  - `insight_cards`
  - `review_snapshots`
  - `app_update_info`
- Runtime feature flag baseline:
  - `core/datastore/FeatureFlagStore`
  - `core/network/FeatureFlagRemoteDataSource`
  - startup refresh in `AppBootstrapCoordinator`
- Sync queue hardening:
  - active-job dedupe in `RoomSyncQueueStore`
  - mutation enqueuer wired from mutable repositories
- Assistant safety baseline:
  - proposal builder use case
  - explicit confirm/cancel commit UX
  - executor wired for `CREATE_TASK` and `LOG_EXPENSE`
- SMS isolation hardening:
  - legacy feature-local SMS receiver/worker removed
  - runtime receiver/worker retained only in `platform/sms/*`
- Deterministic insights baseline:
  - new `features/insights` domain/data/di module
  - deterministic rules for overdue tasks, spending acceleration, and category pressure
  - Room-backed cache/observation integrated into dashboard cards
- Verification gates:
  - `scripts/architecture_boundary_check.ps1`
  - `scripts/secret_scan.ps1`
