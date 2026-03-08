# BELTECH — Personal Management App

A modern Android personal management application built with **Kotlin + Jetpack Compose**, featuring MPESA SMS transaction parsing, glass morphism UI, and Clean Architecture.

## Architecture

```
com.personal.lifeOS/
├── core/
│   ├── database/        # Room DB, entities, DAOs, migrations
│   ├── di/              # Hilt dependency injection modules
│   ├── utils/           # Resource wrapper, date utils
│   ├── security/        # Biometric lock, encryption
│   └── preferences/     # DataStore preferences
├── navigation/          # Jetpack Navigation + bottom nav
├── ui/
│   ├── theme/           # Colors, typography, Material3 theme
│   └── components/      # GlassCard, reusable composables
└── features/
    ├── dashboard/       # Home screen with life metrics
    ├── calendar/        # Monthly/weekly calendar + events
    ├── expenses/        # MPESA parser, transaction tracking
    ├── tasks/           # Task manager with priorities
    ├── assistant/       # AI chat interface
    ├── analytics/       # Life analytics dashboard
    └── profile/         # User settings
```

Each feature follows **Clean Architecture**:
- `presentation/` — Screens, ViewModels
- `domain/model/` — Data models
- `domain/usecase/` — Business logic
- `domain/repository/` — Repository interfaces
- `data/repository/` — Repository implementations
- `data/datasource/` — Local/remote data sources

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room + SQLCipher (encrypted) |
| Async | Coroutines + Flow |
| Charts | Vico Charts |
| Navigation | Jetpack Navigation Compose |
| Preferences | DataStore |

## New Parity Modules

- Planner route now includes `Budget`, `Income`, `Recurring`, `Search`, `Export`, and `Settings` tabs.
- `Budget`, `Income`, and `Recurring` are Room-backed and included in cloud sync.
- `Search` provides cross-module lookup across transactions, tasks, events, budgets, incomes, and recurring rules.
- `Export` writes a JSON snapshot of all user data to the app documents folder.
- `Settings` now controls app theme mode (`system/light/dark`) and notification toggle behavior.
- Recurring rules are now executed by an hourly WorkManager job that generates due task/income/expense entries and advances `next_run_at`.

## Build Stages

- [x] **Stage 1** — Project architecture, Gradle, Hilt, Room, Navigation, Theme, GlassCard
- [x] **Stage 2** — Expense tracking module + MPESA SMS parser (full implementation)
- [x] **Stage 3** — Dashboard with live data widgets
- [x] **Stage 4** — Calendar & Task Manager
- [x] **Stage 5** — AI Assistant (local rule-based intelligence, ChatGPT-style UI)
- [x] **Stage 6** — Analytics dashboard with Vico charts
- [ ] **Stage 7** — Security (biometric lock) + polish

## Setup

1. Clone the repo
2. Open in Android Studio (Ladybug or newer)
3. Sync Gradle
4. Run on device/emulator (API 26+)

## Runtime Configuration

Set runtime values in `local.properties` (gitignored):

```properties
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
ASSISTANT_PROXY_URL=https://<project>.supabase.co/functions/v1/assistant-proxy
```

Assistant calls are proxy-based. Do not place model provider secret keys in the Android app.

## Supabase Sync Requirements

- Apply [`supabase_schema.sql`](supabase_schema.sql) (v3) so sync tables include `user_id` ownership and `events.importance`.
- For existing Supabase projects that were created with `PRIMARY KEY (id)`, run [`supabase_migration_v4_composite_keys.sql`](supabase_migration_v4_composite_keys.sql) to move all sync tables to `PRIMARY KEY (user_id, id)`.
- Run [`supabase_migration_v5_feature_parity.sql`](supabase_migration_v5_feature_parity.sql) to add `budgets`, `incomes`, and `recurring_rules` tables with RLS.
- Run [`supabase_migration_v6_event_status.sql`](supabase_migration_v6_event_status.sql) to add event completion `status` support.
- Android auth sessions are now stored in encrypted shared preferences (`AuthSessionStore`), with legacy plaintext token migration from DataStore.
- Cloud sync uses user-scoped conflict keys (`user_id,id` and `user_id,merchant`) with retry/backoff for transient HTTP/network failures.
- Event/task reminders are alarm-backed, and respect the profile `notifications_enabled` toggle at both scheduling and delivery time.
- Turning notifications off now cancels all currently scheduled event/task alarms immediately for the active user.

## Supabase Isolation Smoke Test

Run two-user RLS + collision checks against Supabase:

```powershell
$env:SUPABASE_PRIMARY_EMAIL="<confirmed-user-email>"
$env:SUPABASE_PRIMARY_PASSWORD="<confirmed-user-password>"
./scripts/supabase_two_user_smoke_test.ps1
```

- If `SUPABASE_SECONDARY_EMAIL` / `SUPABASE_SECONDARY_PASSWORD` are provided, the script uses them.
- Otherwise it attempts to create a temporary secondary user. If your project enforces email confirmation or hits email send limits, it falls back to a single-user RLS enforcement check.

## Release Hardening

- Checklist: [`RELEASE_HARDENING_CHECKLIST.md`](RELEASE_HARDENING_CHECKLIST.md)
- Automation script:
  - `.\scripts\release_hardening_check.ps1`

## MPESA SMS Permissions

The app requires `READ_SMS` and `RECEIVE_SMS` permissions to auto-detect MPESA transactions. Grant these permissions when prompted.
