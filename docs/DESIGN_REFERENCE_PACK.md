# Design Reference Pack (Stitch-First)

## Source Priority

1. Primary reference: `stitch_home_dashboard.zip` (`stitch_home_dashboard/*`).
2. Secondary reference: linked Figma exploration.
3. Conflict policy: function-first adaptation (keep architecture/behavior correctness on Android).

## Included Stitch Artifacts

The Stitch pack includes `code.html` + `screen.png` for:

- `home_dashboard_final`
- `tasks_view_final`
- `finance_view_final`
- `ai_assistant_final`
- `calendar_view_final`
- `finance_insights_final`
- `onboarding_welcome_final`
- `onboarding_pillars_final`
- `onboarding_profile_setup_final`
- `onboarding_final_step_final`
- `sign_in_final`
- `sign_up_final`
- `user_profile_final`
- `settings_final`

The pack also includes `lumina_personal_os/DESIGN.md` as the visual-system narrative baseline.

## Token Mapping (Stitch -> App Tokens)

| Stitch token | Kotlin token target | Notes |
|---|---|---|
| `primary #006a6a` | `AppColorRoles.primary` | Primary action + key highlights |
| `primary_container #00a8a8` | `AppColorRoles.primaryContainer` | CTA gradients and accents |
| `background/surface #f7f9fc` | `AppColorRoles.surface/background` | Calm light base |
| `surface_container_low #f2f4f7` | `AppColorRoles.surfaceContainerLow` | Section grouping |
| `surface_container_lowest #ffffff` | `AppColorRoles.surfaceContainerLowest` | Elevated cards |
| `on_surface #191c1e` | `AppColorRoles.onSurface` | Primary text |
| `on_surface_variant #3c4949` | `AppColorRoles.onSurfaceVariant` | Secondary text |
| `outline_variant #bbc9c8` | `AppColorRoles.outlineVariant` | Ghost border only |
| Inter | `AppTypographyScale` headline/body | Editorial hierarchy |
| JetBrains Mono | `AppTypographyScale.mono` | Financial/time data |

## Screen-to-Feature Mapping

| Stitch screen | Kotlin target |
|---|---|
| Home dashboard | `feature/home` + `features/dashboard` bridge |
| Tasks view | `features/tasks` |
| Finance view + insights | `feature/finance` + `features/expenses/budget/income` bridges |
| AI assistant | `features/assistant` |
| Calendar view | `features/calendar` |
| Sign in / Sign up | `features/auth` |
| Onboarding steps | `features/auth` + profile setup flow |
| User profile | `features/profile` |
| Settings | `features/settings` |

## Implementation Constraints

- Do not sacrifice architecture boundaries for visual parity.
- Avoid expensive blur/overdraw on large scrolling surfaces.
- Keep touch targets and text legibility Android-first.
- Preserve explicit loading/empty/error/offline/sync-aware states.
- Keep assistant actions preview-first before commit.
- Keep finance import states transparent (pending review, duplicate, parse issues).

## Navigation IA Baseline

Primary bottom tabs are fixed to:

- `home`
- `tasks`
- `finance`
- `calendar`
- `assistant`

Secondary flows:

- profile
- settings
- export
- analytics
- planner/review utilities

Legacy route aliases must remain during migration to avoid deep-link breakage.
