# Visual System Overhaul Plan (2026-03-29)

## Implementation Progress (Wave 1 Complete)
- Shared surface depth system upgraded in `AppCard` (shadow + highlight layering).
- Global header consistency rollout completed through `PageScaffold` hero header variant.
- Home adapted to compact header mode to avoid duplicate hero stacking.
- Assistant flow modernized (hero header, modern prompt chips, upgraded input bar shell).
- Theme consistency upgrades applied (rounded shape scale + non-monospace labels).
- Shared controls updated (`SearchField`, `SegmentedControl`) for modern contrast and alignment.
- Auth/onboarding v1 polish applied (hero header, button radius consistency, copy casing fixes).
- Verification: `:app:compileDebugKotlin` passing after all changes.

## Implementation Progress (Wave 2 Complete)
- Screen-level header hierarchy normalized via `headerEyebrow` across:
  - Finance, Tasks, Calendar, Search, Settings, Planner, Profile, Insights
  - Budget, Income, Recurring, Review, Export, Learning
  - Analytics
- Deep/secondary pages now align with the same hero-header rhythm as main routes.
- Verification: `:app:compileDebugKotlin` passing after Wave 2.

## Goal
Deliver a modern, premium, and consistent UI across the Android app that feels more complete than the React reference while remaining highly user-friendly.

## Scope Coverage (Full App)
This overhaul is explicitly full-coverage and includes:
- Main pages (Home, Finance, Tasks, Calendar, Planner, Search, Settings, Profile, Insights).
- Secondary pages and deep feature pages.
- All dialogs, bottom sheets, empty/loading/error states, snackbars, banners, and inline status rows.
- Authentication flow pages:
  - Onboarding
  - Sign in / Auth
  - Sign up / registration paths
  - Password/account completion screens where present

Completion is blocked unless auth and deep pages are also visually aligned to the same design system.

## Non-Negotiables
- No clipped or squeezed UI on supported phone sizes.
- No hidden primary actions behind keyboard, FAB, or bottom nav.
- Consistent hero/header composition across all major screens.
- Consistent icon sizing, button behavior, spacing rhythm, and copy style.
- Readable contrast and scalable text at larger font settings.

## Baseline (Current Measured State)
Snapshot from code scan on 2026-03-29:
- Total `*Screen.kt` files: `38`
- Screens using `PageScaffold`: `16`
- Screens using `TopBanner`: `4`
- Screens using `SearchField`: `4`
- Screens using `SegmentedControl`: `3`
- Screens using `AppCard`: `9`
- Screens using `GlassCard`: `0`
- `Modifier.height(...)` occurrences: `25`
- `Modifier.width(...)` occurrences: `33`
- `maxLines = 1` occurrences: `12`

These numbers show strong progress but still significant visual-system fragmentation.

## UX Guardrails (Layout Integrity)
Every refactored screen must pass:
1. Small/medium/large handset checks.
2. Font scale checks at `1.0x`, `1.15x`, `1.30x`.
3. Orientation sanity check for top 6 core screens.
4. Zero clipped text in titles, subtitles, chips, tabs, and action bars.
5. Minimum touch target `48dp`.
6. Safe-area correctness (status bar, nav bar, keyboard).

## Design Direction
- Visual style: modern glass depth with restrained color.
- Depth tiers:
  - `flat`: utility rows, neutral background surfaces
  - `raised`: standard cards and grouped content
  - `hero`: top focal block with stronger edge and elevation
- Color allocation:
  - 75-80% neutral surfaces
  - 15-20% brand accent
  - 5% semantic signals only
- Geometry:
  - compact controls: `12dp`
  - standard cards: `16-20dp`
  - hero surfaces: `24-28dp`
  - pills/buttons: full radius

## Phase Plan

### Phase 0: Baseline Audit (In Progress)
Output:
- Screen inventory with risk labels (clipping, spacing drift, copy drift, icon drift).
- Hotspot list of files with fixed dimensions and one-line text risks.

### Phase 1: Design Tokens 2.0
Files:
- `app/src/main/java/com/personal/lifeOS/core/ui/designsystem/AppDesignTokens.kt`
- `app/src/main/java/com/personal/lifeOS/ui/theme/Theme.kt`
- `app/src/main/java/com/personal/lifeOS/ui/theme/Color.kt`

Deliverables:
- Complete token map for depth, border strength, shadow levels, hero variants.
- Unified type scale and content spacing ladder.

### Phase 2: Core Primitive Unification
Files:
- `core/ui/designsystem/PageScaffold.kt`
- `core/ui/designsystem/TopBanner.kt`
- `core/ui/designsystem/AppCard.kt`
- `ui/components/GlassCard.kt`
- `navigation/LifeOSNavHost.kt`

Deliverables:
- Single source of truth for card/surface variants.
- New reusable hero composable.
- Modern floating bottom nav shell with consistent active state.
- Unified button and input primitives.

### Phase 3: Hero and Header Consistency Rollout
Scope:
- Home, Finance, Tasks, Calendar, Planner, Search, Settings, Profile, Insights.

Deliverables:
- Shared hero templates:
  - Overview Hero
  - Status Hero
  - Action Hero
- Shared header contract:
  - eyebrow (optional), title, subtitle, action slot.

### Phase 4: Deep-Screen Standardization
Scope:
- Dialogs, bottom sheets, empty/loading/error states, snackbars, inline status rows.

Deliverables:
- Same spacing, corner radius, icon sizing, and semantic tone rules in deep flows.

### Phase 5: Motion and Accessibility Polish
Deliverables:
- Motion tokens and transitions applied consistently.
- Reduced-motion fallback behavior.
- Font-scale and contrast verification pass.

### Phase 6: Governance and Regression Prevention
Deliverables:
- Automated checks for hardcoded colors/shapes in feature files.
- PR checklist for visual consistency and layout integrity.
- Screenshot regression set for key screens/states.

## Priority Screen Order
1. Home
2. Finance
3. Tasks
4. Calendar
5. Auth + Onboarding
6. Planner
7. Search
8. Settings
9. Profile
10. Insights
11. Secondary/deep feature screens
12. Dialogs/sheets/supporting states

## File Hotspots To Address Early
- `features/dashboard/presentation/DashboardComponents.kt`
- `feature/finance/presentation/FinanceScreen.kt`
- `features/analytics/presentation/AnalyticsComponents.kt`
- `features/expenses/presentation/ExpensesComponents.kt`
- `features/calendar/presentation/CalendarMonthComponents.kt`

These currently contain higher concentrations of fixed size and one-line pressure points.

## Quality Gates Before Each Merge
1. Visual consistency check complete for touched screen(s).
2. No new hardcoded color/shape literals in feature modules.
3. No new clipping at larger font scale.
4. User flow remains one-handed and obvious (no hidden core actions).
5. Build passes: `:app:compileDebugKotlin`.

## Immediate Execution Queue (Starting Now)
1. Create screen-by-screen audit matrix (main + deep states).
2. Implement token additions for depth tiers and hero surfaces.
3. Build shared `HeroSurface` composable.
4. Migrate Home and Finance as reference implementations.
5. Migrate Auth + Onboarding immediately after Home/Finance.
6. Validate on device-size + font-scale matrix.

## Success Metrics
- >=90% major screens use unified shell/header/hero/card primitives.
- 100% auth flow screens use unified modern primitives.
- 100% deep pages/dialog/sheet states follow tokenized spacing/radius/color rules.
- 0 blocking clipping issues in QA matrix.
- <=5 ad-hoc visual exceptions across core flows.
- User feedback: "modern", "clear", "consistent", "easy to use".
