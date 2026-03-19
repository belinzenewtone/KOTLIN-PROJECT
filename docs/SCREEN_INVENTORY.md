# Screen Inventory (Milestone 0)

## Primary Navigation

- `auth` -> `features/auth/presentation/AuthScreen.kt`
- `dashboard` (target: `feature/home`) -> `features/dashboard/presentation/DashboardScreen.kt`
- `calendar` -> `features/calendar/presentation/CalendarScreen.kt`
- `expenses` (target: `feature/finance/ledger`) -> `features/expenses/presentation/ExpensesScreen.kt`
- `tasks` -> `features/tasks/presentation/TasksScreen.kt`
- `planner` (hub for modular tools) -> `features/planner/presentation/PlannerScreen.kt`
- `assistant` -> `features/assistant/presentation/AssistantScreen.kt`
- `analytics` -> `features/analytics/presentation/AnalyticsScreen.kt`
- `profile` -> `features/profile/presentation/ProfileScreen.kt`

## Planner Sub-Screens

- `budget` -> `features/budget/presentation/BudgetScreen.kt`
- `income` -> `features/income/presentation/IncomeScreen.kt`
- `recurring` -> `features/recurring/presentation/RecurringScreen.kt`
- `search` -> `features/search/presentation/SearchScreen.kt`
- `export` -> `features/export/presentation/ExportScreen.kt`
- `settings` -> `features/settings/presentation/SettingsScreen.kt`

## Target Mapping Notes

- `dashboard` will be renamed into `feature/home` with richer decision-surface state.
- `expenses`, `budget`, and `income` will consolidate into `feature/finance` subdomains.
- `planner` remains a shell route while sub-features are migrated to explicit navigation contracts.
