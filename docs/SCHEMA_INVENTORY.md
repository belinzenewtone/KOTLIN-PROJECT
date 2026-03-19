# Schema Inventory (Milestone 0)

## Current Room Entities

- `UserEntity` (`users`)
- `TaskEntity` (`tasks`)
- `EventEntity` (`events`)
- `TransactionEntity` (`transactions`)
- `BudgetEntity` (`budgets`)
- `IncomeEntity` (`incomes`)
- `RecurringRuleEntity` (`recurring_rules`)
- `MerchantCategoryEntity` (`merchant_categories`)
- `SyncJobEntity` (`sync_jobs`)
- `ImportAuditEntity` (`import_audit`)
- `AssistantConversationEntity` (`assistant_conversations`)
- `AssistantMessageEntity` (`assistant_messages`)
- `InsightCardEntity` (`insight_cards`)
- `ReviewSnapshotEntity` (`review_snapshots`)
- `AppUpdateInfoEntity` (`app_update_info`)

## Canonical Metadata Coverage

The mutable domain tables include standardized metadata in v7:

- `updated_at`
- `sync_state`
- `record_source`
- `deleted_at`
- `revision`

Applied to:

- `tasks`
- `events`
- `transactions`
- `budgets`
- `incomes`
- `recurring_rules`
- `merchant_categories`

## Migration State

- Current database version: `7`
- Latest migration: `MIGRATION_6_7`
- Production safety rule: no destructive fallback migration in release path.
