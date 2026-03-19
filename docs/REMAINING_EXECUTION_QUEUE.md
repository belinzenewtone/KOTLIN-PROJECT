# Remaining Execution Queue (2026-03-20)

Status legend: `PENDING`, `IN_PROGRESS`, `COMPLETED`

## Queue

1. Ticket 15: Migration test suite completion (`COMPLETED`)
- Scope: Add end-to-end migration chain validation from v4 through v8 with seeded data.
- Tests: `DatabaseMigrationV48EndToEndTest` + full `:app:check`.
- Exit criteria: Core records preserved; canonical metadata/table/index expectations verified through latest schema.

2. Ticket 14: Export framework hardening (`COMPLETED`)
- Scope: Strengthen export serialization validation and edge-case safety.
- Tests: `ExportPayloadSerializerTest`, `ExportViewModelTest`, `ExportShareHelperTest`, full `:app:check`.
- Exit criteria met: Reducer coverage added and share-flow regression path validated.

3. Ticket 3: Room source-of-truth enforcement (`COMPLETED`)
- Scope: Repository audit so UI never depends on remote-only state for persistent surfaces.
- Tests: Repository + integration checks for reconciled writes into Room.
- Exit criteria met: Added `ExpenseRepositoryRoomIntegrationTest`, `FinanceRoomSourceOfTruthIntegrationTest`, `SearchRepositoryRoomIntegrationTest`, `TaskRepositoryRoomIntegrationTest`, and `CalendarRepositoryRoomIntegrationTest` covering Room-observed writes/reads for expenses/budget/income/search/tasks/calendar, SMS import reconciliation, sync enqueue expectations, and strict user-scoped behavior.

4. Ticket 7: Navigation shell hardening (`COMPLETED`)
- Scope: Guard/deep-link/alias regression strengthening for 5-tab shell and secondary routes.
- Tests: navigation contract + route guard regression tests.
- Exit criteria met: Introduced pure guard resolver (`AppNavigationGuards`), canonical legacy-route normalization, and added route-guard + alias selection regression coverage.

5. Ticket 1: Final package structure alignment (`COMPLETED`)
- Scope: Finish migration from legacy `features/*` ownership into target `feature/*` boundaries and remove bridges.
- Tests: compile + architecture boundary checks + affected module tests.
- Exit criteria met: All primary and secondary navigation entrypoints now resolve through singular `feature/*` contracts, planner tabs consume singular feature bridges (`budget`, `income`, `recurring`, `search`, `export`, `settings`), and `review` now has explicit singular feature routing.

6. Ticket 8: Finance domain unification (`COMPLETED`)
- Scope: Converge expenses/budget/income seams into coherent finance domain boundaries.
- Tests: finance domain/use-case/repository tests + import dedupe regression checks.
- Exit criteria met: Finance now owns explicit transaction/filter/summary domain models (`FinanceTransaction`, `FinanceTransactionFilter`, `FinanceSpendingSummary`), repository mapping boundaries convert legacy expense entities into finance-domain models, and finance presentation no longer imports legacy expenses UI dialogs.

7. Ticket 6: Design system adoption completion (`COMPLETED`)
- Scope: Finish moving remaining screens/components onto shared design-system primitives.
- Tests: compose smoke tests + visual/manual parity checklist.
- Exit criteria met: Remaining large presentation files were decomposed (assistant input split, calendar month/event/dialog split), unused legacy task UI blocks were removed in favor of design-system `TaskRow` flow, and calendar card surfaces were aligned to design-system `AppCard`.

8. Ticket 5: Metadata standardization status closure (`COMPLETED`)
- Scope: Final verification pass for `syncState/source/lifecycle` fields and explicit tracker closure.
- Tests: schema + migration assertions for mutable entities.
- Exit criteria met: Added `CanonicalMetadataSchemaTest` to assert canonical metadata columns across mutable user-scoped tables.

## Completed in this queue cycle

- Added v4->v8 migration chain test:
  - `app/src/test/java/com/personal/lifeOS/core/database/DatabaseMigrationV48EndToEndTest.kt`
- Added parser fixture coverage:
  - `app/src/test/java/com/personal/lifeOS/platform/sms/parser/MpesaParserFixtures.kt`
  - `app/src/test/java/com/personal/lifeOS/platform/sms/parser/MpesaMessageParserFixturesTest.kt`
- Added OTA manifest interpretation tests:
  - `app/src/test/java/com/personal/lifeOS/core/update/OtaManifestInterpretationTest.kt`
- Added export serializer tests:
  - `app/src/test/java/com/personal/lifeOS/features/export/data/repository/ExportPayloadSerializerTest.kt`
- Added export share-flow regression tests:
  - `app/src/test/java/com/personal/lifeOS/platform/files/ExportShareHelperTest.kt`
- Added Room reconciliation integration tests:
  - `app/src/test/java/com/personal/lifeOS/features/expenses/data/repository/ExpenseRepositoryRoomIntegrationTest.kt`
- Added finance Room source-of-truth integration tests:
  - `app/src/test/java/com/personal/lifeOS/features/finance/data/repository/FinanceRoomSourceOfTruthIntegrationTest.kt`
- Added navigation guard regression tests:
  - `app/src/test/java/com/personal/lifeOS/navigation/AppNavigationGuardsTest.kt`
- Added canonical metadata schema verification:
  - `app/src/test/java/com/personal/lifeOS/core/database/CanonicalMetadataSchemaTest.kt`
- Added unified finance repository flow coverage:
  - `app/src/test/java/com/personal/lifeOS/feature/finance/data/repository/FinanceRepositoryImplTest.kt`
- Added search UI-state reducer coverage:
  - `app/src/test/java/com/personal/lifeOS/features/search/presentation/SearchViewModelTest.kt`
- Added phased package-alignment bridge for search:
  - `app/src/main/java/com/personal/lifeOS/feature/search/presentation/SearchScreen.kt`
- Added Room-backed search repository integration coverage:
  - `app/src/test/java/com/personal/lifeOS/features/search/data/repository/SearchRepositoryRoomIntegrationTest.kt`
- Added finance presentation-domain decoupling and mapper coverage:
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceViewModel.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceContracts.kt`
  - `app/src/test/java/com/personal/lifeOS/feature/finance/presentation/FinanceContractsTest.kt`
- Added legacy expenses compatibility bridge to unified finance UI:
  - `app/src/main/java/com/personal/lifeOS/features/expenses/presentation/ExpensesScreen.kt`
- Added analytics state reducer coverage:
  - `app/src/test/java/com/personal/lifeOS/features/analytics/presentation/AnalyticsViewModelTest.kt`
- Added Task and Calendar Room source-of-truth integration coverage:
  - `app/src/test/java/com/personal/lifeOS/features/tasks/data/repository/TaskRepositoryRoomIntegrationTest.kt`
  - `app/src/test/java/com/personal/lifeOS/features/calendar/data/repository/CalendarRepositoryRoomIntegrationTest.kt`
- Added 5-tab shell bridge entrypoints for phased package migration:
  - `app/src/main/java/com/personal/lifeOS/feature/tasks/presentation/TasksScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/calendar/presentation/CalendarScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/assistant/presentation/AssistantScreen.kt`
- Added analytics bridge entrypoint for phased package migration:
  - `app/src/main/java/com/personal/lifeOS/feature/analytics/presentation/AnalyticsScreen.kt`
- Moved finance summary/filter business logic into domain use cases:
  - `app/src/main/java/com/personal/lifeOS/feature/finance/domain/usecase/BuildFinanceSummaryUseCase.kt`
- Added budget use-case layer and reducer tests:
  - `app/src/main/java/com/personal/lifeOS/features/budget/domain/usecase/ObserveBudgetProgressUseCase.kt`
  - `app/src/main/java/com/personal/lifeOS/features/budget/domain/usecase/AddBudgetUseCase.kt`
  - `app/src/main/java/com/personal/lifeOS/features/budget/domain/usecase/DeleteBudgetUseCase.kt`
  - `app/src/test/java/com/personal/lifeOS/features/budget/domain/usecase/ObserveBudgetProgressUseCaseTest.kt`
  - `app/src/test/java/com/personal/lifeOS/features/budget/presentation/BudgetViewModelTest.kt`
- Added income use-case orchestration and design-system migration coverage:
  - `app/src/main/java/com/personal/lifeOS/features/income/domain/usecase/AddIncomeUseCase.kt`
  - `app/src/main/java/com/personal/lifeOS/features/income/domain/usecase/DeleteIncomeUseCase.kt`
  - `app/src/main/java/com/personal/lifeOS/features/income/presentation/IncomeViewModel.kt`
  - `app/src/main/java/com/personal/lifeOS/features/income/presentation/IncomeScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/features/income/presentation/IncomeComponents.kt`
  - `app/src/test/java/com/personal/lifeOS/features/income/domain/usecase/ObserveIncomeSnapshotUseCaseTest.kt`
  - `app/src/test/java/com/personal/lifeOS/features/income/presentation/IncomeViewModelTest.kt`
- Added secondary-route bridge entrypoints for phased package migration:
  - `app/src/main/java/com/personal/lifeOS/feature/profile/presentation/ProfileScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/settings/presentation/SettingsScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/export/presentation/ExportScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/planner/presentation/PlannerScreen.kt`
- Added auth and dashboard compatibility bridges for phased migration:
  - `app/src/main/java/com/personal/lifeOS/feature/auth/presentation/AuthScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/auth/presentation/OnboardingScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/features/dashboard/presentation/DashboardScreen.kt`
- Added additional singular bridges and planner contract migration:
  - `app/src/main/java/com/personal/lifeOS/feature/budget/presentation/BudgetScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/income/presentation/IncomeScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/recurring/presentation/RecurringScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/features/planner/presentation/PlannerScreen.kt`
- Added singular review bridge and route wiring:
  - `app/src/main/java/com/personal/lifeOS/feature/review/presentation/ReviewScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/navigation/LifeOSNavHost.kt`
- Added finance-owned transaction/filter/summary domain models and mapping boundaries:
  - `app/src/main/java/com/personal/lifeOS/feature/finance/domain/model/FinanceTransaction.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/domain/model/FinanceAnalyticsModels.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/domain/model/FinanceSnapshot.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/domain/usecase/BuildFinanceSummaryUseCase.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/data/repository/FinanceRepositoryImpl.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceContracts.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceViewModel.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceScreen.kt`
  - `app/src/main/java/com/personal/lifeOS/feature/finance/presentation/FinanceDialogs.kt`
- Reduced presentation complexity by splitting oversized files:
  - `app/src/main/java/com/personal/lifeOS/features/assistant/presentation/AssistantComponents.kt`
  - `app/src/main/java/com/personal/lifeOS/features/assistant/presentation/AssistantInputBar.kt`
  - `app/src/main/java/com/personal/lifeOS/features/tasks/presentation/TasksComponents.kt`
  - `app/src/main/java/com/personal/lifeOS/features/calendar/presentation/CalendarMonthComponents.kt`
  - `app/src/main/java/com/personal/lifeOS/features/calendar/presentation/CalendarEventComponents.kt`
  - `app/src/main/java/com/personal/lifeOS/features/calendar/presentation/CalendarEventDialog.kt`

## Verification Baseline

- Full gate currently passing: `:app:check`
