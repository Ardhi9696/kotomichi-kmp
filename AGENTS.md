# AGENTS.md — Kotomichi KMP

Kotlin Multiplatform app (Japanese vocab learning, FSRS + BKT). Modules: `androidApp`, `sharedLogic`. iOS targets declared in `sharedLogic` but no `iosApp` yet.

## Package layout (ikuti ini untuk file baru)

- `androidApp/src/main/kotlin/com/kotomichi/`
  - `app/` — Application, Activity, `app/di/`, `app/navigation/`
  - `core/designsystem/` — `theme/` (Color/Shape/Type/Dimens), `common/` (reusable composables), `preference/` (DataStore prefs)
  - `core/sync/` — WorkManager sync plumbing (Android-only)
  - `feature/<auth|belajar|review|dashboard|home|splash>/ui/` — per-feature screens
- `sharedLogic/src/commonMain/kotlin/com/kotomichi/shared/`
  - `core/` — `model/`, `fsrs/`, `bkt/`, `furigana/`, `repository/` (interfaces), `di/`, `util/`, `database/`
  - `feature/<name>/domain/usecase/` — use cases
- `sharedLogic/src/androidMain/...` — `core/database/driver/`, `feature/<name>/data/` (repository impls — Android-only for now)
- SQLDelight schema: `sharedLogic/src/commonMain/sqldelight/` (packageName `com.kotomichi.db` — jangan diubah)

## Rules

- 1 file = 1 tanggung jawab. Feature baru → package `feature/<nama>/` sendiri, jangan taruh di `feature/home/`.
- Domain language: pakai "belajar" untuk fitur belajar/quiz (konsisten dengan `BelajarQuizUseCase`), bukan "learn".
- Jangan tambah duplikasi belajar/learn — `feature/belajar/ui/` adalah satu-satunya rumah komponen belajar & quiz.
- local.properties (SUPABASE_URL, SUPABASE_ANON_KEY, keystore) tidak boleh di-commit; contoh ada di `local.properties.example`.
- google-services.json ter-commit mengikuti praktik repo ini (client-side key, low risk).
- Tanpa Gradle/Xcode build lokal di VM ini — verifikasi via GitHub Actions (`kmp-verify.yml`).
