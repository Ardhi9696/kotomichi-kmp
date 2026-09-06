# Kotomichi (言道) - Japanese Vocabulary & Grammar Learning App

A Kotlin Multiplatform (KMP) mobile application for learning Japanese vocabulary and grammar using Spaced Repetition System (FSRS) and Knowledge Tracing (BKT).

## Features

- **FSRS-based Spaced Repetition**: Advanced scheduling algorithm for optimal vocabulary retention
- **6-Direction Learning**: Kanji→Meaning, Kanji→Hiragana, Hiragana→Meaning, Meaning→Hiragana, Hiragana→Kanji, Meaning→Kanji
- **Offline-First**: Full offline capability with background sync via WorkManager
- **Gamification**: EXP system, levels, streaks, and achievement tracking
- **Deduplication**: Vocabulary shared across decks without duplicate learning
- **Role-Based Access**: Super Admin, Admin, User roles with proper authorization

## Architecture

```
kotomichi/
├── shared/                 # KMP shared module
│   ├── commonMain/         # Shared business logic
│   │   ├── model/          # Data models
│   │   ├── fsrs/           # FSRS engine (pure Kotlin)
│   │   ├── bkt/            # BKT engine (pure Kotlin)
│   │   ├── repository/     # Repository interfaces
│   │   ├── usecase/        # Business logic use cases
│   │   └── di/             # Koin dependency injection
│   ├── androidMain/        # Android implementations
│   └── iosMain/            # iOS implementations (future)
├── androidApp/             # Android app (Jetpack Compose + Material 3)
├── iosApp/                 # iOS app (SwiftUI) - future
└── sqldelight/             # SQLDelight database schema
```

## Tech Stack

- **Kotlin Multiplatform** - Shared business logic
- **Android** - Jetpack Compose + Material 3
- **SQLDelight** - Local database (offline-first)
- **Ktor** - HTTP client for Supabase REST API
- **Koin** - Dependency injection
- **WorkManager** - Background sync
- **Supabase** - Auth + PostgreSQL backend
- **Cloudflare R2** - Audio storage

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Kotlin 2.0.0+

### Build

```bash
./gradlew assembleDebug
```

### Run Tests

```bash
./gradlew shared:testDebugUnitTest
```

## Database Schema

The app uses SQLDelight for type-safe database access. Key tables:

- `vocabulary` - Master vocabulary data
- `deck` - Learning decks/bab
- `deck_vocabulary` - Many-to-many relationship
- `srs_progress` - Per-user, per-direction FSRS progress
- `review_log` - Complete review history
- `user_profile` - User data and gamification stats

## FSRS Algorithm

The FSRS (Free Spaced Repetition Scheduler) implementation includes:

- 4 card states: NEW, LEARNING, REVIEW, RELEARNING
- 4 ratings: AGAIN, HARD, GOOD, EASY
- Automatic rating based on response time
- Per-direction stability tracking
- Direction unlock thresholds

## BKT Algorithm (Future)

Bayesian Knowledge Tracing for grammar patterns:

- P(L) - Probability of learning
- P(G) - Probability of guessing
- P(S) - Probability of slipping
- P(T) - Probability of transit

## Project Structure Details

### Shared Module (`shared/`)

Pure Kotlin business logic with no platform dependencies:

- **Models**: Data classes for Vocabulary, Deck, SRS Progress, User, etc.
- **FSRS Engine**: `FsrsCalculator` - Pure functions for SRS calculations
- **BKT Engine**: `BktCalculator` - Pure functions for grammar tracing
- **Repositories**: Interfaces for data access (Vendor-agnostic)
- **Use Cases**: Business logic orchestration

### Android App (`androidApp/`)

Native Android UI with Jetpack Compose:

- **Screens**: Dashboard, Learn, Review, Auth
- **Navigation**: Navigation Compose
- **DI**: Koin modules for Android implementations
- **Workers**: Background sync with WorkManager

### SQLDelight (`sqldelight/`)

Type-safe database schema with generated Kotlin code:

- Tables for all entities
- Custom queries for complex operations
- Type converters for enums

## Configuration

Key configuration values (stored in `SystemConfig` table):

- `exp_base` = 100
- `exp_exponent` = 1.5
- `mastery_threshold` = 0.90
- `direction_thresholds_*` - Per-direction response time thresholds
- `sync_interval_hours` = 6

## Direction Thresholds (Response Time → Rating)

| Direction | Easy (<) | Good (<) | Hard |
|-----------|----------|----------|------|
| All (default) | 8s | 15s | >15s |

Configurable per-direction via database.

## License

Proprietary - Kotomichi Project