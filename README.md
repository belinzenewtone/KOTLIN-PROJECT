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

## MPESA SMS Permissions

The app requires `READ_SMS` and `RECEIVE_SMS` permissions to auto-detect MPESA transactions. Grant these permissions when prompted.
