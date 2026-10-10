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
kotomichi-kmp/
├── sharedLogic/            # KMP shared module (commonMain / androidMain)
│   └── src/commonMain/kotlin/com/kotomichi/shared/
│       ├── core/           # model, fsrs, bkt, furigana, repository (interfaces), di, util
│       └── feature/        # per-feature domain usecases (auth, belajar, gamifikasi)
├── androidApp/             # Android app (Jetpack Compose + Material 3)
│   └── src/main/kotlin/com/kotomichi/
│       ├── app/            # Application, Activity, di, navigation
│       ├── core/           # designsystem (theme/common/preference), sync
│       └── feature/        # per-feature UI (auth, belajar, review, dashboard, home, splash)
├── .github/workflows/      # CI: kmp-verify.yml (unit tests + debug assemble)
└── local.properties.example # Template for local secrets (git-ignored local.properties)
```

SQLDelight schema lives in `sharedLogic/src/commonMain/sqldelight/`.
iOS targets are declared in `sharedLogic` but there is no `iosApp` yet — repository implementations are currently Android-only.

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
- JDK 21 (project targets JVM 21)
- Kotlin 2.1.0 (see `gradle/libs.versions.toml`)

Copy `local.properties.example` to `local.properties` and fill in Supabase credentials (optional for debug builds — placeholders are used otherwise).

### Build

```bash
./gradlew :androidApp:assembleDebug
```

### Run Tests

```bash
./gradlew :sharedLogic:testDebugUnitTest
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

### Shared Module (`sharedLogic/`)

Pure Kotlin business logic with no platform dependencies:

- **Models**: Data classes for Vocabulary, Deck, SRS Progress, User, etc. (`core/model/`)
- **FSRS Engine**: `FsrsCalculator` - Pure functions for SRS calculations (`core/fsrs/`)
- **BKT Engine**: `BktCalculator` - Pure functions for grammar tracing (`core/bkt/`)
- **Repositories**: Interfaces for data access (Vendor-agnostic) (`core/repository/`)
- **Use Cases**: Business logic orchestration (`feature/<name>/domain/usecase/`)
- **Android data layer**: Repository implementations in `androidMain` (`feature/<name>/data/`)

### Android App (`androidApp/`)

Native Android UI with Jetpack Compose:

- **App shell**: `app/` (Application, Activity, DI, navigation)
- **Design system**: `core/designsystem/` (theme tokens, reusable composables, preferences)
- **Features**: `feature/<auth|belajar|review|dashboard|home|splash>/ui/` (per-feature screens)
- **Workers**: Background sync with WorkManager (`core/sync/`)

### SQLDelight (`sharedLogic/src/commonMain/sqldelight/`)

Type-safe database schema with generated Kotlin code (package `com.kotomichi.db`):

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