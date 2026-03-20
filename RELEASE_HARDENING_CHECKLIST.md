# RELEASE_HARDENING_CHECKLIST.md

## Scope

Use this checklist before cutting a production release of the Kotlin BELTECH app.

## 1) Build and Unit Checks

- Run unit tests:
  - `.\gradlew.bat testDebugUnitTest --no-daemon`
- Run static analysis (blocking; baseline-backed for legacy debt):
  - `.\gradlew.bat app:detekt app:ktlintCheck --no-daemon`

## 2) Database and Migration Checks

- Verify Room migrations:
  - App DB version reaches `6` without destructive fallback.
- Verify Supabase migrations applied:
  - v4: composite keys
  - v5: feature parity tables (`budgets`, `incomes`, `recurring_rules`)
  - v6: events status column/index (`status`, `idx_events_status`)
  - v7: final schema/policy hardening (`supabase_migration_v7_release_hardening.sql`)

## 3) Sync and Isolation Checks

- Run strict two-user smoke test:
  - `.\scripts\supabase_two_user_smoke_test.ps1 -RequireTwoUsers`
- Expected result:
  - `Result: FULL_PASS`
- Confirm all tables pass:
  - `transactions`, `tasks`, `events`, `merchant_categories`, `budgets`, `incomes`, `recurring_rules`

## 4) Background and Reminder Checks

- Confirm periodic workers are scheduled:
  - cloud sync worker
  - recurring execution worker
- Confirm notification channels exist:
  - event reminders
  - task reminders
- Toggle notifications off and confirm scheduled reminders are cancelled.

## 5) Manual Product Walkthrough

- Auth: sign in/out, session restore, profile update.
- Planner tabs:
  - Budget, Income, Recurring, Search, Export, Settings.
- Export:
  - JSON file is created and path is shown.
- Theme mode:
  - System/Light/Dark change takes effect app-wide.
- OTA updates:
  - Open `Planner > Settings > App Updates`.
  - Ensure `OTA_MANIFEST_URL` is configured in `local.properties`.
  - If using GitHub Releases OTA, ensure `ota/manifest.json` points to the latest release asset URL and checksum.
  - Tap `Check for updates`, then `Download & install`, and verify:
    - APK downloads successfully
    - installer opens
    - unknown-sources permission flow works when required

## Go / No-Go

- `GO`: all critical checks pass and strict smoke test is `FULL_PASS`.
- `NO-GO`: any migration, isolation, or recurring execution regression.
