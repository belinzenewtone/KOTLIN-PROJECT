# Visual Audit Matrix (2026-03-29)

Legend: `Y` = yes, `N` = no, `TBD` = not reviewed manually yet.

| Screen File | PageScaffold | TopBanner | SearchField | SegmentedControl | Priority | Clipping Risk | Notes |
|---|---|---|---|---|---|---|---|
| `app\src\main\java\com\personal\lifeOS\feature\analytics\presentation\AnalyticsScreen.kt` | N | N | N | N | P2 | MED |  |
| `app\src\main\java\com\personal\lifeOS\feature\assistant\presentation\AssistantScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\auth\presentation\AuthScreen.kt` | N | N | N | N | P1 | MED | Auth-flow included in full overhaul |
| `app\src\main\java\com\personal\lifeOS\feature\auth\presentation\OnboardingScreen.kt` | N | N | N | N | P1 | MED | Auth-flow included in full overhaul |
| `app\src\main\java\com\personal\lifeOS\feature\budget\presentation\BudgetScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\calendar\presentation\CalendarScreen.kt` | N | N | N | N | P1 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\export\presentation\ExportScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\finance\presentation\FinanceScreen.kt` | Y | Y | Y | Y | P1 | LOW | Hero header eyebrow aligned (`Money OS`) |
| `app\src\main\java\com\personal\lifeOS\feature\home\presentation\HomeScreen.kt` | Y | Y | N | N | P1 | LOW | Compact header + HeroSurface v1 applied |
| `app\src\main\java\com\personal\lifeOS\feature\income\presentation\IncomeScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\planner\presentation\PlannerScreen.kt` | N | N | N | N | P1 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\profile\presentation\ProfileScreen.kt` | N | N | N | N | P1 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\recurring\presentation\RecurringScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\review\presentation\ReviewScreen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\search\presentation\SearchScreen.kt` | N | N | N | N | P1 | MED |  |
| `app\src\main\java\com\personal\lifeOS\feature\settings\presentation\SettingsScreen.kt` | N | N | N | N | P1 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\feature\tasks\presentation\TasksScreen.kt` | N | N | N | N | P1 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\features\analytics\presentation\AnalyticsScreen.kt` | Y | N | N | Y | P2 | LOW | Hero header eyebrow aligned (`Overview`) |
| `app\src\main\java\com\personal\lifeOS\features\assistant\presentation\AssistantScreen.kt` | N | N | N | N | P2 | LOW | Hero header + modern prompt chips + glass input shell v1 |
| `app\src\main\java\com\personal\lifeOS\features\auth\presentation\AuthScreen.kt` | N | N | N | N | P1 | MED | Auth hero + button/input polish v1 applied |
| `app\src\main\java\com\personal\lifeOS\features\auth\presentation\OnboardingScreen.kt` | N | N | N | N | P1 | MED | Scroll-first onboarding + hero shell v1 applied |
| `app\src\main\java\com\personal\lifeOS\features\budget\presentation\BudgetScreen.kt` | Y | Y | N | N | P2 | LOW | Hero header eyebrow aligned (`Spending Guardrails`) |
| `app\src\main\java\com\personal\lifeOS\features\calendar\presentation\CalendarScreen.kt` | Y | N | Y | N | P1 | LOW | Hero header eyebrow aligned (`Schedule`) |
| `app\src\main\java\com\personal\lifeOS\features\dashboard\presentation\DashboardScreen.kt` | N | N | N | N | P2 | MED |  |
| `app\src\main\java\com\personal\lifeOS\features\expenses\presentation\ExpensesScreen.kt` | N | N | N | N | P2 | MED |  |
| `app\src\main\java\com\personal\lifeOS\features\export\presentation\ExportScreen.kt` | Y | N | N | N | P2 | LOW | Hero header eyebrow aligned (`Data Portability`) |
| `app\src\main\java\com\personal\lifeOS\features\income\presentation\IncomeScreen.kt` | Y | N | N | N | P2 | LOW | Hero header eyebrow aligned (`Cash Flow`) |
| `app\src\main\java\com\personal\lifeOS\features\insights\presentation\InsightsScreen.kt` | Y | N | N | N | P1 | LOW | Hero header eyebrow aligned (`Trends`) |
| `app\src\main\java\com\personal\lifeOS\features\learning\presentation\LearningScreen.kt` | Y | N | N | N | P2 | LOW | Hero header eyebrow aligned (`Growth`) |
| `app\src\main\java\com\personal\lifeOS\features\planner\presentation\PlannerScreen.kt` | Y | N | N | N | P1 | LOW | Hero header eyebrow aligned (`Finance Tools`) |
| `app\src\main\java\com\personal\lifeOS\features\profile\presentation\ProfileScreen.kt` | Y | N | N | N | P1 | LOW | Hero header eyebrow aligned (`Personal Space`) |
| `app\src\main\java\com\personal\lifeOS\features\recurring\presentation\RecurringScreen.kt` | Y | N | N | N | P2 | LOW | Hero header eyebrow aligned (`Automation`) |
| `app\src\main\java\com\personal\lifeOS\features\review\presentation\ReviewScreen.kt` | Y | N | N | N | P2 | LOW | Hero header eyebrow aligned (`Weekly Ritual`) |
| `app\src\main\java\com\personal\lifeOS\features\search\presentation\SearchScreen.kt` | Y | N | Y | Y | P1 | LOW | Shared search + segmented control modernized via core primitives |
| `app\src\main\java\com\personal\lifeOS\features\settings\presentation\SettingsScreen.kt` | Y | Y | N | N | P1 | LOW | Hero header eyebrow aligned (`Preferences`) |
| `app\src\main\java\com\personal\lifeOS\features\tasks\presentation\TasksScreen.kt` | Y | N | Y | N | P1 | LOW | Hero header eyebrow aligned (`Execution`) |
| `app\src\main\java\com\personal\lifeOS\navigation\Screen.kt` | N | N | N | N | P2 | TBD |  |
| `app\src\main\java\com\personal\lifeOS\ui\splash\PersonalOsSplashScreen.kt` | N | N | N | N | P2 | TBD |  |
