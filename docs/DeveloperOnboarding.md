# RhythmWise Developer Onboarding Guide

## Purpose

This guide brings new developers from zero to productive on the RhythmWise codebase.
It explains _why_ the architecture is shaped the way it is, walks through the critical
code paths, and provides step-by-step recipes for the most common development tasks.

**Target audience:** Developers with 1-2 years of programming experience, some Kotlin
familiarity, and a basic understanding of MVVM.

---

## Cross-References

This guide is the canonical onboarding reference. It deliberately does _not_ duplicate
content from these companion documents:

| Document | What it covers |
|----------|---------------|
| [`docs/architecture/DOCUMENTATION_GUIDELINES.md`](architecture/DOCUMENTATION_GUIDELINES.md) | KDoc standards, what/how to document |
| [`docs/GIT_COMMIT_GUIDELINES.md`](GIT_COMMIT_GUIDELINES.md) | Conventional Commits format, DCO sign-off |
| [`docs/CODE_STYLE.md`](CODE_STYLE.md) | Kotlin & Compose naming, formatting, test structure |
| [`docs/SECURITY_MODEL.md`](SECURITY_MODEL.md) | Threat model, no-recovery policy, network posture |

---

## Table of Contents

- [Phase 1 — Foundation](#phase-1--foundation)
  - [1.1 What is KMP and Why We Use It](#11-what-is-kmp-and-why-we-use-it)
  - [1.2 Module Structure](#12-module-structure)
  - [1.3 Gradle Structure and Version Catalog](#13-gradle-structure-and-version-catalog)
  - [1.4 Dependency Injection with Koin](#14-dependency-injection-with-koin)
  - [1.5 SQLCipher and Passphrase-Derived Encryption](#15-sqlcipher-and-passphrase-derived-encryption)
  - [1.6 MVVM and MVI Pattern](#16-mvvm-and-mvi-pattern)
  - [1.7 Repository Pattern and Clean Layering](#17-repository-pattern-and-clean-layering)
- [Phase 2 — Architectural Deep Dive](#phase-2--architectural-deep-dive)
  - [2.1 App Launch](#21-app-launch)
  - [2.2 PassphraseScreen and Unlock Flow](#22-passphrasescreen-and-unlock-flow)
  - [2.3 Session Scope Lifetime](#23-session-scope-lifetime)
  - [2.4 TrackerScreen and TrackerViewModel](#24-trackerscreen-and-trackerviewmodel)
  - [2.5 Use Cases to Repository to DAO](#25-use-cases-to-repository-to-dao)
  - [2.6 Platform vs Shared Separation](#26-platform-vs-shared-separation)
  - [2.7 DailyLogScreen and DailyLogViewModel](#27-dailylogscreen-and-dailylogviewmodel)
  - [2.8 InsightsScreen and InsightsViewModel](#28-insightsscreen-and-insightsviewmodel)
  - [2.9 LockedWaterDraft — Pre-Authentication Water Tracking](#29-lockedwaterdraft--pre-authentication-water-tracking)
- [Phase 3 — Code Organization and Design Principles](#phase-3--code-organization-and-design-principles)
  - [3.1 Why Interfaces Live in Shared Module](#31-why-interfaces-live-in-shared-module)
  - [3.2 Why Platform Implementations Live in composeApp](#32-why-platform-implementations-live-in-composeapp)
  - [3.3 Why No Passphrase Storage](#33-why-no-passphrase-storage)
  - [3.4 Why Session Scope is Ephemeral](#34-why-session-scope-is-ephemeral)
  - [3.5 How Design Enables iOS Porting](#35-how-design-enables-ios-porting)
  - [3.6 Where to Add New Features](#36-where-to-add-new-features)
  - [3.7 How to Avoid Breaking Layering Rules](#37-how-to-avoid-breaking-layering-rules)
  - [3.8 Key File Walkthroughs](#38-key-file-walkthroughs)
- [Phase 4 — Developer Workflows](#phase-4--developer-workflows)
  - [4.1 How to Run the Project](#41-how-to-run-the-project)
  - [4.2 Unlocking Session During Testing](#42-unlocking-session-during-testing)
  - [4.3 Where to Place New Use Cases](#43-where-to-place-new-use-cases)
  - [4.4 Where to Place New Repository Methods](#44-where-to-place-new-repository-methods)
  - [4.5 Commit Conventions](#45-commit-conventions)
  - [4.6 Branching and PR Expectations](#46-branching-and-pr-expectations)
  - [4.7 How to Avoid Leaking Secrets](#47-how-to-avoid-leaking-secrets)
  - [4.8 Common Architectural Mistakes](#48-common-architectural-mistakes)
  - [4.9 Database Migrations](#49-database-migrations)
- [Phase 5 — Future-Proofing](#phase-5--future-proofing)
  - [5.1 Planned iOS Module](#51-planned-ios-module)
  - [5.2 Insight Engine Overview](#52-insight-engine-overview)
  - [5.3 Encryption Versioning](#53-encryption-versioning)
  - [5.4 Migration Strategy](#54-migration-strategy)
  - [5.5 DI Scaling](#55-di-scaling)
- [Appendix: Dependency Layering Diagram](#appendix-dependency-layering-diagram)

---

# Phase 1 — Foundation

## 1.1 What is KMP and Why We Use It

### Kotlin Multiplatform in 60 Seconds

Kotlin Multiplatform (KMP) lets you write business logic once in Kotlin and compile it
for multiple platforms — Android, iOS, desktop, and more. The key idea is source set
separation:

- **`commonMain`** — Pure Kotlin code that compiles for every target. No platform SDK
  imports allowed. This is where domain models, use cases, repository interfaces, and
  the insight engine live.
- **`androidMain`** — Kotlin code that can import Android SDK classes. Room DAOs,
  Compose UI, and Android services live here.
- **`iosMain`** — Kotlin code that can import Apple frameworks (`UIKit`,
  `Foundation`). The iOS app will eventually implement platform-specific services here.

### Why RhythmWise Uses KMP

1. **Testability** — The entire domain layer (`shared/src/commonMain/`) compiles and
   runs on the JVM without an emulator or device. Unit tests are fast and reliable.
2. **Portability** — When an iOS app is built, all domain models, use cases,
   `InsightEngine`, and providers are already compiled for iOS. Only platform
   integrations (UI, database, encryption) need to be written.
3. **Single source of truth** — Business rules like "how to merge two adjacent
   periods" exist in exactly one place (`PeriodRepository` interface), not duplicated
   across platforms.

### The `expect`/`actual` Pattern

KMP uses `expect`/`actual` declarations to define a contract in `commonMain` and
fulfill it per platform. RhythmWise demonstrates this with `Platform.kt`:

**`shared/src/commonMain/kotlin/com/veleda/cyclewise/Platform.kt`:**
```kotlin
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect val num: Int
```

**`shared/src/androidMain/kotlin/com/veleda/cyclewise/Platform.android.kt`:**
```kotlin
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val num: Int = 3
```

**`shared/src/iosMain/kotlin/com/veleda/cyclewise/Platform.ios.kt`:**
```kotlin
class IOSPlatform: Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
```

> **Note:** The iOS actual is currently missing `actual val num: Int`. This means the
> iOS target will not compile until this is added. The Android actual provides
> `actual val num: Int = 3`. This is a known gap in the iOS placeholder.

The compiler enforces that every `expect` declaration has a matching `actual` for each
target. If you add a new platform target but forget an `actual`, the build fails.

---

## 1.2 Module Structure

The project has two Gradle modules, declared in `settings.gradle.kts`:

```kotlin
include(":composeApp")
include(":shared")
```

### `shared/` — KMP Library

Contains platform-agnostic domain logic. Zero Android dependencies in `commonMain`.

| Package | Contents |
|---------|----------|
| `domain/models/` | `Period`, `DailyEntry`, `FullDailyLog`, `Symptom`, `Medication`, `PeriodLog`, `SymptomLog`, `MedicationLog`, `WaterIntake`, `DayDetails`, enums (`FlowIntensity`, `LibidoLevel`, `SymptomCategory`) |
| `domain/repository/` | `PeriodRepository` interface — the single data access contract |
| `domain/usecases/` | `StartNewPeriodUseCase`, `EndPeriodUseCase`, `GetOrCreateDailyLogUseCase`, `AutoCloseOngoingPeriodUseCase`, `DebugSeederUseCase` |
| `domain/insights/` | `InsightEngine`, `Insight` sealed interface, `InsightGenerator` interface |
| `domain/insights/generators/` | 6 generators: `CycleLengthAverageGenerator`, `NextPeriodPredictionGenerator`, `SymptomRecurrenceGenerator`, `MoodPhasePatternGenerator`, `CycleLengthTrendGenerator`, `SymptomPhasePatternGenerator` |
| `domain/providers/` | `SymptomLibraryProvider`, `MedicationLibraryProvider` |
| `domain/services/` | `PassphraseService` interface |

### `composeApp/` — Android Application

Contains the Jetpack Compose UI and all Android-specific implementations.

| Package | Contents |
|---------|----------|
| `androidData/local/dao/` | 8 Room DAOs: `PeriodDao`, `DailyEntryDao`, `SymptomDao`, `MedicationDao`, `MedicationLogDao`, `SymptomLogDao`, `PeriodLogDao`, `WaterIntakeDao` |
| `androidData/local/database/` | `PeriodDatabase` (Room + SQLCipher), `migrations/` (8 migration objects, v1 through v9) |
| `androidData/local/entities/` | 8 Room entities + `Converters` + `Mappers` |
| `androidData/local/draft/` | `LockedWaterDraft` — persists water intake while locked |
| `androidData/repository/` | `RoomPeriodRepository` — implements `PeriodRepository` |
| `ui/auth/` | `PassphraseScreen`, `PassphraseViewModel`, `WaterTrackerViewModel`, `WaterDraftSyncer` |
| `ui/tracker/` | `TrackerScreen`, `TrackerViewModel`, `TrackerEvent`, `CalendarDayInfo` |
| `ui/log/` | `DailyLogScreen`, `DailyLogViewModel` |
| `ui/insights/` | `InsightsScreen`, `InsightsViewModel` |
| `ui/settings/` | `SettingsScreen` |
| `ui/nav/` | `NavRoutes`, `CycleWiseAppUI` (NavHost) |
| `di/` | `AppModule.kt` — all Koin DI wiring |
| `services/` | `PassphraseServiceAndroid`, `SaltStorage` |
| `session/` | `SessionBus` |
| `settings/` | `AppSettings` |

### Full Directory Tree

```
PeriodTracker/
├── settings.gradle.kts              # Module declarations
├── gradle/
│   └── libs.versions.toml           # Version catalog
├── docs/                            # All documentation
│   ├── DeveloperOnboarding.md       # This file
│   ├── ARCHITECTURE.md
│   ├── CODE_STYLE.md
│   ├── GIT_COMMIT_GUIDELINES.md
│   ├── SECURITY_MODEL.md
│   ├── ISSUE_WRITING_GUIDE.md
│   ├── architecture/
│   │   └── DOCUMENTATION_GUIDELINES.md
│   └── testing/
│       ├── RUNNING_TESTS.md
│       ├── TESTING_STRATEGY.md
│       └── TEST_INVENTORY.md
│
├── shared/                          # KMP library module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/veleda/cyclewise/
│       │   ├── Platform.kt                       # expect declarations
│       │   └── domain/
│       │       ├── models/
│       │       │   ├── Period.kt
│       │       │   ├── DailyEntry.kt
│       │       │   ├── FullDailyLog.kt
│       │       │   ├── Symptom.kt
│       │       │   ├── Medication.kt
│       │       │   ├── PeriodLog.kt
│       │       │   ├── SymptomLog.kt
│       │       │   ├── MedicationLog.kt
│       │       │   ├── WaterIntake.kt
│       │       │   ├── DayDetails.kt
│       │       │   └── Enums.kt                  # FlowIntensity, LibidoLevel, SymptomCategory
│       │       ├── repository/
│       │       │   └── PeriodRepository.kt       # The single data access contract
│       │       ├── usecases/
│       │       │   ├── StartNewPeriodUseCase.kt
│       │       │   ├── EndPeriodUseCase.kt
│       │       │   ├── GetOrCreateDailyLogUseCase.kt
│       │       │   ├── AutoCloseOngoingPeriodUseCase.kt
│       │       │   └── DebugSeederUseCase.kt
│       │       ├── insights/
│       │       │   ├── InsightEngine.kt
│       │       │   ├── Insight.kt
│       │       │   └── generators/               # 6 insight generators
│       │       ├── providers/
│       │       │   ├── SymptomLibraryProvider.kt
│       │       │   └── MedicationLibraryProvider.kt
│       │       └── services/
│       │           └── PassPhraseService.kt      # Interface
│       ├── androidMain/kotlin/com/veleda/cyclewise/
│       │   └── Platform.android.kt               # actual for Android
│       └── iosMain/kotlin/com/veleda/cyclewise/
│           └── Platform.ios.kt                   # actual for iOS
│
├── composeApp/                      # Android application module
│   ├── build.gradle.kts
│   ├── schemas/                     # Room schema exports (JSON)
│   └── src/androidMain/kotlin/com/veleda/cyclewise/
│       ├── CycleWiseApp.kt                       # Application class + lifecycle observer
│       ├── MainActivity.kt
│       ├── di/
│       │   └── AppModule.kt                      # All Koin DI wiring
│       ├── androidData/
│       │   ├── local/
│       │   │   ├── dao/                          # 8 Room DAOs
│       │   │   ├── database/
│       │   │   │   ├── PeriodDatabase.kt
│       │   │   │   └── migrations/               # 8 migration objects (v1→v9)
│       │   │   ├── entities/                     # 8 Room entities + Converters + Mappers
│       │   │   └── draft/
│       │   │       └── LockedWaterDraft.kt
│       │   └── repository/
│       │       └── RoomPeriodRepository.kt
│       ├── ui/
│       │   ├── auth/
│       │   │   ├── PassphraseScreen.kt
│       │   │   ├── PassphraseViewModel.kt
│       │   │   ├── WaterTrackerViewModel.kt
│       │   │   └── WaterDraftSyncer.kt
│       │   ├── tracker/
│       │   │   ├── TrackerScreen.kt
│       │   │   ├── TrackerViewModel.kt
│       │   │   ├── TrackerEvent.kt
│       │   │   └── CalendarDayInfo.kt
│       │   ├── log/
│       │   │   ├── DailyLogScreen.kt
│       │   │   └── DailyLogViewModel.kt
│       │   ├── insights/
│       │   │   ├── InsightsScreen.kt
│       │   │   └── InsightsViewModel.kt
│       │   ├── settings/
│       │   │   └── SettingsScreen.kt
│       │   └── nav/
│       │       ├── NavRoutes.kt
│       │       └── CycleWiseAppUI.kt             # NavHost + bottom navigation
│       ├── services/
│       │   ├── PassphraseServiceAndroid.kt
│       │   └── SaltStorage.kt
│       ├── session/
│       │   └── SessionBus.kt
│       └── settings/
│           └── AppSettings.kt
│
└── iosApp/                          # Xcode project (placeholder)
    ├── iosApp.xcodeproj/
    └── iosApp/
        ├── ContentView.swift
        └── iOSApp.swift
```

---

## 1.3 Gradle Structure and Version Catalog

### Version Catalog

All dependency versions are centralized in `gradle/libs.versions.toml`. Key versions:

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Kotlin | 2.2.0 | Language and compiler |
| Compose Multiplatform | 1.8.2 | UI framework |
| Room | 2.7.2 | Database ORM |
| SQLCipher | 4.5.4 | AES-256 database encryption |
| Koin | 4.1.0 | Dependency injection |
| BouncyCastle | 1.81 | Argon2id KDF implementation |
| kotlinx-datetime | 0.7.1 | Cross-platform date/time |
| kotlinx-coroutines | 1.10.2 | Asynchronous programming |
| MockK | 1.14.5 | Mocking in tests |
| Turbine | 1.2.1 | Flow testing |
| Robolectric | 4.16 | Android unit tests without device |

### Build Plugins

`composeApp/build.gradle.kts` applies these plugins:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)                  // KSP for Room annotation processing
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)                 // Room schema export
}
```

### SDK Targets

```
minSdk     = 26     (Android 8.0 Oreo)
targetSdk  = 35     (Android 15)
compileSdk = 35
JVM target = 11
```

### Room Schema Export

Room schemas are exported to `composeApp/schemas/` for migration testing:

```kotlin
// composeApp/build.gradle.kts:134
extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}
```

---

## 1.4 Dependency Injection with Koin

All DI wiring lives in a single file: `di/AppModule.kt`. Koin is bootstrapped in
`CycleWiseApp.onCreate()`:

```kotlin
// CycleWiseApp.kt:43-48
startKoin {
    printLogger()
    androidContext(this@CycleWiseApp)
    modules(appModule)
    allowOverride(false)
}
```

### Two DI Lifetimes

The critical architectural pattern is the **session scope**. The app has two DI
lifetimes with fundamentally different lifecycles:

#### 1. Singleton Scope (lives for the app process)

These objects are created once when the app starts and never destroyed until the
process dies:

| Component | Purpose |
|-----------|---------|
| `SaltStorage` | Persists 16-byte encryption salt in SharedPreferences |
| `AppSettings` | DataStore-backed user preferences (autolock, top symptoms count) |
| `SessionBus` | SharedFlow event bus for logout/lock signals |
| `PassphraseService` | Argon2id key derivation interface (bound to `PassphraseServiceAndroid`) |
| `LockedWaterDraft` | Persists water intake edits while the DB is locked |
| `InsightEngine` | Orchestrates insight generators (registered as `factory`, not `single` — a fresh instance is created each call, so no stale state accumulates) |
| `PassphraseViewModel` | Manages unlock flow (survives screen rotation) |
| `WaterTrackerViewModel` | Manages water tracker on lock screen |

#### 2. Session Scope (created on unlock, destroyed on logout/autolock)

These objects are created _after_ the user enters the correct passphrase and the
database is successfully opened. They are all destroyed together when the session ends:

| Component | Purpose |
|-----------|---------|
| `PeriodDatabase` | SQLCipher-encrypted Room database |
| 8 DAOs | `PeriodDao`, `DailyEntryDao`, `SymptomDao`, `MedicationDao`, `MedicationLogDao`, `SymptomLogDao`, `PeriodLogDao`, `WaterIntakeDao` |
| `RoomPeriodRepository` | Implements `PeriodRepository` with Room |
| `SymptomLibraryProvider` | Reactive stream of symptom library |
| `MedicationLibraryProvider` | Reactive stream of medication library |
| `GetOrCreateDailyLogUseCase` | Retrieves or creates blank daily log |
| `DebugSeederUseCase` | Seeds database with test data |
| `AutoCloseOngoingPeriodUseCase` | Auto-closes stale ongoing periods |
| `TrackerViewModel` | Calendar/tracker screen state |
| `DailyLogViewModel` | Daily log editing state |
| `InsightsViewModel` | Insights screen state |

### The SESSION_SCOPE Qualifier

```kotlin
// di/AppModule.kt:43
val SESSION_SCOPE: Qualifier = named("UnlockedSessionScope")
```

All session-scoped objects are registered inside `scope(SESSION_SCOPE) { ... }`:

```kotlin
// di/AppModule.kt:84-153
scope(SESSION_SCOPE) {
    scoped { (passphrase: String) ->
        val key = get<PassphraseService>().deriveKey(passphrase)
        PeriodDatabase.create(androidContext(), key)
    }

    scoped { get<PeriodDatabase>().periodDao() }
    // ... 7 more DAOs ...

    scoped<PeriodRepository> {
        RoomPeriodRepository(
            db = get(), periodDao = get(), dailyEntryDao = get(),
            symptomDao = get(), medicationDao = get(),
            medicationLogDao = get(), symptomLogDao = get(),
            periodLogDao = get(), waterIntakeDao = get(),
        )
    }

    // Use cases, providers, ViewModels...
}
```

### How the Passphrase Reaches the Database

The database factory uses `parametersOf` to pass the passphrase at runtime:

```kotlin
// PassphraseViewModel.kt:70
val db = sessionScope.get<PeriodDatabase> { parametersOf(passphrase) }
```

Inside the scope definition, the `(passphrase: String)` parameter is destructured:

```kotlin
// di/AppModule.kt:86-89
scoped { (passphrase: String) ->
    val key = get<PassphraseService>().deriveKey(passphrase)
    PeriodDatabase.create(androidContext(), key)
}
```

---

## 1.5 SQLCipher and Passphrase-Derived Encryption

### Key Derivation Chain

The full chain from user input to encrypted database:

```
User passphrase (String)
        │
        ▼
   Argon2id KDF
   ├── Salt: 16 bytes from SaltStorage (SecureRandom, stored in SharedPreferences)
   ├── Memory: 64 MB (64 * 1024 KB)
   ├── Iterations: 3
   ├── Parallelism: 1
   └── Output: 32 bytes (256-bit AES key)
        │
        ▼
   SupportFactory(key)
        │
        ▼
   Room.databaseBuilder(...)
       .openHelperFactory(factory)
       .build()
        │
        ▼
   SQLCipher AES-256-GCM encrypted database (cyclewise.db)
```

### Salt Management

`SaltStorage` (`services/SaltStorage.kt`) stores a 16-byte salt in plain
SharedPreferences. The salt is **not secret** — its purpose is to ensure that the same
passphrase on different devices produces different derived keys. It is generated once
via `SecureRandom` and persisted:

```kotlin
// services/SaltStorage.kt:31-39
fun getOrCreateSalt(): ByteArray {
    prefs.getString(SALT_KEY, null)?.let { base64 ->
        return Base64.decode(base64, Base64.DEFAULT)
    }
    val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
    val encoded = Base64.encodeToString(salt, Base64.NO_WRAP)
    prefs.edit { putString(SALT_KEY, encoded) }
    return salt
}
```

### Security Invariant

The passphrase and derived key **never persist to disk**. They exist only in memory
during the session scope's lifetime. When the session scope is destroyed (logout,
autolock, or app termination), the key becomes eligible for garbage collection.

The `PassphraseService` interface documents this contract explicitly:

```kotlin
// shared/.../domain/services/PassPhraseService.kt:10-13
// Security contract:
// - The derived key must never be persisted to disk.
// - Callers must zeroize the returned ByteArray when the session scope closes.
// - The KDF must be computationally expensive to resist brute-force attacks.
```

---

## 1.6 MVVM and MVI Pattern

RhythmWise ViewModels use a **MVI-inspired** (Model-View-Intent) unidirectional data
flow pattern built on top of MVVM components.

### The Pattern

```
┌─────────────────────────────────────────────────────────────┐
│  User Action                                                │
│      │                                                      │
│      ▼                                                      │
│  TrackerEvent (sealed interface)                            │
│      │                                                      │
│      ▼                                                      │
│  onEvent(event) ──► reduce(currentState, event)             │
│                          │                                  │
│                ┌─────────┴──────────┐                       │
│                │                    │                        │
│                ▼                    ▼                        │
│      New TrackerUiState    Side effect launched              │
│      (emitted via          (viewModelScope.launch)          │
│       StateFlow)                   │                        │
│                                    ▼                        │
│                           TrackerEffect emitted             │
│                           (via SharedFlow,                  │
│                            replay = 0)                      │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **State** (`TrackerUiState`) — A `data class` holding all UI state. Exposed as
  `StateFlow<TrackerUiState>`. The UI recomposes whenever this changes.
- **Events** (`TrackerEvent`) — A `sealed interface` defining every user action.
  The UI calls `viewModel.onEvent(TrackerEvent.DayTapped(date))`.
- **Effects** (`TrackerEffect`) — One-shot side effects (navigation, toasts) that
  should be consumed exactly once. Exposed as `SharedFlow` with `replay = 0`.
- **Reducer** (`reduce()`) — Takes current state + event and returns new state.
  In `TrackerViewModel`, the reducer also launches side effects (repository calls,
  navigation) inside `viewModelScope.launch` — making it a _hybrid_ reducer rather
  than a pure function. `DailyLogViewModel` keeps most of its reducer branches pure,
  but still launches async work for save, library creation, and water persistence.

### Concrete Example: TrackerViewModel

`TrackerViewModel` (`ui/tracker/TrackerViewModel.kt`) manages the calendar screen:

**State:**
```kotlin
data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val logForSheet: FullDailyLog? = null,
    val periodIdForSheet: String? = null,
    val symptomLibrary: List<Symptom> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap(),
    val showDeleteConfirmation: Boolean = false,
    val periodIdToDelete: String? = null,
    val waterCupsForSheet: Int? = null
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
}
```

**Events** (8 types defined in `ui/tracker/TrackerEvent.kt`):
1. `ScreenEntered` — Triggers auto-close of stale periods
2. `DayTapped(date)` — Tap a day to view or create a log
3. `PeriodMarkDay(date)` — Long-press to toggle period day
4. `DismissLogSheet` — Close the log summary bottom sheet
5. `EditLogClicked(date)` — Navigate to edit the daily log
6. `DeletePeriodRequested(periodId)` — Show delete confirmation
7. `DeletePeriodConfirmed(periodId)` — Execute deletion
8. `DeletePeriodDismissed` — Cancel deletion

**Dispatch:**
```kotlin
// TrackerViewModel.kt:98-102
fun onEvent(event: TrackerEvent) {
    _uiState.update { currentState ->
        reduce(currentState, event)
    }
}
```

---

## 1.7 Repository Pattern and Clean Layering

### The Contract

`PeriodRepository` (`shared/.../domain/repository/PeriodRepository.kt`) is the single
data access contract for the entire app. It defines 27 methods grouped into:

- **Period CRUD** (9 methods) — `getAllPeriods()`, `getPeriodById()`, `startNewPeriod()`,
  `updatePeriodEndDate()`, `endPeriod()`, `getCurrentlyOngoingPeriod()`,
  `createCompletedPeriod()`, `isDateRangeAvailable()`, `deletePeriod()`
- **Period day marking** (2 methods) — `logPeriodDay()`, `unLogPeriodDay()` (4-scenario state machines)
- **Daily log access** (4 methods) — `getFullLogForDate()`, `saveFullLog()`, `getLogsForMonth()`, `getAllLogs()`
- **Calendar observation** (2 methods) — `observeAllPeriodDays()`, `observeDayDetails()`
- **Symptom library** (4 methods) — `getSymptomLibrary()`, `createOrGetSymptomInLibrary()`,
  `prepopulateSymptomLibrary()`, `getAllSymptomLogs()`
- **Medication library** (3 methods) — `getMedicationLibrary()`, `createOrGetMedicationInLibrary()`,
  `getAllMedicationLogs()`
- **Water intake** (2 methods) — `upsertWaterIntake()`, `getWaterIntakeForDates()`
- **Debug** (1 method) — `seedDatabaseForDebug()`

### Flow vs Suspend

The repository uses two return styles:
- **`Flow<T>`** — Cold reactive streams backed by Room's `@Query` annotations. Emits
  the current snapshot on subscription and re-emits whenever the underlying table
  changes.
- **`suspend fun`** — One-shot operations safe to call from any dispatcher (Room
  handles IO internally).

### Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  TrackerScreen ──► TrackerViewModel                              │
│                         │                                        │
│                    onEvent(TrackerEvent)                          │
│                         │                                        │
├─────────────────────────┼────────────────────────────────────────┤
│                    Domain Layer                                   │
│                         │                                        │
│               AutoCloseOngoingPeriodUseCase                       │
│               GetOrCreateDailyLogUseCase                          │
│                         │                                        │
│                    PeriodRepository (interface)                   │
│                         │                                        │
├─────────────────────────┼────────────────────────────────────────┤
│                     Data Layer                                    │
│                         │                                        │
│               RoomPeriodRepository (implementation)               │
│                         │                                        │
│              ┌──────────┼──────────┐                             │
│              │          │          │                              │
│          PeriodDao  DailyEntryDao  ... (8 DAOs total)            │
│              │          │          │                              │
├──────────────┼──────────┼──────────┼─────────────────────────────┤
│                    Infrastructure                                 │
│                         │                                        │
│              PeriodDatabase (Room + SQLCipher)                    │
│                         │                                        │
│                    cyclewise.db                                   │
│              (AES-256-GCM encrypted)                             │
└──────────────────────────────────────────────────────────────────┘
```

**Rule:** Dependencies flow downward only. The UI layer never imports DAO classes.
Use cases never import Room annotations. The `shared/` module never imports
`composeApp/` classes.

---

# Phase 2 — Architectural Deep Dive

## 2.1 App Launch

RhythmWise is a **single-activity** Compose application. The launch sequence:

1. **`CycleWiseApp.onCreate()`** (`CycleWiseApp.kt:40-60`) —
   - Starts Koin with `appModule` (and `allowOverride(false)` to catch DI errors early)
   - Initializes autolock SharedPreferences (`"autolock_prefs"`)
   - Begins collecting `autolockMinutes` from `AppSettings` into a `@Volatile` cache
   - Registers itself as a `ProcessLifecycleOwner` observer for autolock

2. **`MainActivity`** (`MainActivity.kt`) — Intentionally minimal:
   - Calls `enableEdgeToEdge()` for fullscreen content behind system bars
   - Registers a global uncaught exception handler for crash logging
   - Calls `setContent { CycleWiseAppUI() }` — all Compose UI starts here

3. **`CycleWiseAppUI`** (`ui/CycleWiseAppUI.kt`) — The root composable:
   - Sets up the Compose `NavHost` with 5 routes
   - Start destination: `NavRoute.Passphrase`
   - **Bottom navigation is hidden on PassphraseScreen** — only shown after unlock:
     ```kotlin
     if (currentRoute != NavRoute.Passphrase.route) {
         BottomNavBar(navController)
     }
     ```
   - Handles `WindowInsets.systemBars` padding so content doesn't overlap system UI
   - On successful unlock, navigates to Tracker and pops Passphrase from the backstack:
     ```kotlin
     PassphraseScreen {
         navController.navigate(NavRoute.Tracker.route) {
             popUpTo(NavRoute.Passphrase.route) { inclusive = true }
         }
     }
     ```

### Navigation Routes

Defined in `ui/nav/NavRoutes.kt`:

```kotlin
sealed class NavRoute(val route: String, val label: String) {
    object Passphrase : NavRoute("passphrase", "Pass Phrase")
    object Tracker    : NavRoute("tracker", "Tracker")
    object Insights   : NavRoute("insights", "Insights")
    object Settings   : NavRoute("settings", "Settings")
    object DailyLog   : NavRoute("log/{date}?isPeriodDay={isPeriodDay}", "Daily Log") {
        fun createRoute(date: LocalDate, isPeriodDay: Boolean = false) =
            "log/$date?isPeriodDay=$isPeriodDay"
    }

    companion object {
        val all = listOf(Tracker, Insights, Settings)  // Bottom nav items (excludes Passphrase)
    }
}
```

Bottom navigation shows three tabs: **Tracker**, **Insights**, **Settings**.

### DailyLog Navigation Parameters

The `DailyLog` route takes two parameters parsed from the URL:

```kotlin
composable(
    route = NavRoute.DailyLog.route,
    arguments = listOf(
        navArgument("date") { type = NavType.StringType },          // ISO-8601 date
        navArgument("isPeriodDay") { type = NavType.BoolType; defaultValue = false }
    )
) { backStackEntry ->
    val dateString = backStackEntry.arguments?.getString("date")
    val isPeriodDay = backStackEntry.arguments?.getBoolean("isPeriodDay") ?: false
    DailyLogScreen(
        date = LocalDate.parse(dateString),
        onSaveComplete = { navController.popBackStack() },
        isPeriodDay = isPeriodDay
    )
}
```

`TrackerViewModel` constructs this route via `NavRoute.DailyLog.createRoute(date, isPeriodDay)`.

---

## 2.2 PassphraseScreen and Unlock Flow

The passphrase screen is the app's gateway. Nothing database-related exists until the
user enters the correct passphrase and the session scope is created.

### Step-by-Step Walkthrough

Follow along in `ui/auth/PassphraseViewModel.kt`:

**1. User taps Unlock**
```
PassphraseScreen calls viewModel.onEvent(PassphraseEvent.UnlockClicked(passphrase))
→ PassphraseViewModel.unlock(passphrase)                          // line 54
```

**2. Re-entrancy guard**
```kotlin
if (_uiState.value.isUnlocking) return                            // line 55
```
Prevents double-tap from creating duplicate sessions.

**3. Set isUnlocking state**
```kotlin
_uiState.update { it.copy(isUnlocking = true) }                  // line 58
```
The UI shows a loading indicator.

**4. Check prepopulation status**
```kotlin
val needsPrepopulation = !appSettings.isPrepopulated.first()      // line 60
```
Reads the DataStore flag _before_ entering the IO dispatcher to avoid blocking.

**5. Switch to IO dispatcher and create session scope**
```kotlin
withContext(Dispatchers.IO) {                                      // line 62
    val koin = getKoin()
    val sessionScope = koin.getScopeOrNull("session")             // line 64
        ?: koin.createScope(                                      // line 65
            scopeId = "session",
            qualifier = SESSION_SCOPE                              // line 67
        )
```
If the scope already exists (e.g., after a rotation), it is reused.

**6. Get PeriodDatabase (triggers key derivation)**
```kotlin
    val db = sessionScope.get<PeriodDatabase> {
        parametersOf(passphrase)                                  // line 70
    }
```
Inside the scope definition (`AppModule.kt:86-89`), this triggers:
- `PassphraseService.deriveKey(passphrase)` — calls `PassphraseServiceAndroid`
- `SaltStorage.getOrCreateSalt()` — returns or generates 16-byte salt
- Argon2id KDF — 64 MB memory, 3 iterations, parallelism 1 → 32-byte key
- `PeriodDatabase.create(context, key)` — builds Room with `SupportFactory(key)`

**7. Force-open the database**
```kotlin
    db.openHelper.writableDatabase                                // line 71
```
If the passphrase is wrong, SQLCipher throws here. The exception is caught below.

**8. First-unlock prepopulation**
```kotlin
    if (needsPrepopulation) {                                     // line 73
        val repository = sessionScope.get<PeriodRepository>()
        repository.prepopulateSymptomLibrary()                    // line 75
        appSettings.setPrepopulated(true)                         // line 76
    }
```
Inserts 20 default symptoms (Cramps, Headache, Bloating, etc.) on first unlock.

**9. Sync water drafts**
```kotlin
    syncWaterDrafts(sessionScope)                                 // line 79
```
Any water intake logged on the lock screen (via `LockedWaterDraft`) is persisted
into the database. `WaterDraftSyncer` (`ui/auth/WaterDraftSyncer.kt`) applies these
rules:
- **Today's draft is skipped** — the user may still be editing it
- **Higher-wins merge** — a draft is written to the DB only if its cup count exceeds
  the existing DB value for that date
- **Synced dates are cleared** from the draft store
- **Individual failures are logged** but don't abort the remaining sync

**10. Navigate to Tracker**
```kotlin
_effect.emit(PassphraseEffect.NavigateToTracker)                  // line 82
```
The UI observes this effect and navigates, popping Passphrase from the backstack.

### Error Path

```kotlin
} catch (e: Exception) {                                          // line 84
    Log.e("PassphraseUnlock", "Unlock failed with exception", e)
    getKoin().getScopeOrNull("session")?.close()                  // line 86
    _effect.emit(PassphraseEffect.ShowError(
        "Failed to unlock. Wrong passphrase?"                     // line 87
    ))
} finally {
    _uiState.update { it.copy(isUnlocking = false) }             // line 89
}
```

On failure, the session scope is closed immediately (destroying the partially-created
database), and an error message is shown to the user.

---

## 2.3 Session Scope Lifetime

The session scope is the security boundary of the app. Understanding when it is
created and destroyed is essential.

### Session Lifecycle Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  App Launch                                                     │
│      │                                                          │
│      ▼                                                          │
│  ┌─────────────────────────┐                                    │
│  │    PassphraseScreen     │◄──────────────────────┐            │
│  │   (no DB, no session)   │                       │            │
│  └───────────┬─────────────┘                       │            │
│              │ correct passphrase                   │            │
│              ▼                                      │            │
│  ┌─────────────────────────┐              session   │            │
│  │  Koin Session Scope     │              .close()  │            │
│  │  CREATED                │                  │     │            │
│  │  ├─ PeriodDatabase      │                  │     │            │
│  │  ├─ 8 DAOs              │                  │     │            │
│  │  ├─ Repository          │                  │     │            │
│  │  ├─ Use Cases           │                  │     │            │
│  │  └─ ViewModels          │                  │     │            │
│  └───────────┬─────────────┘                  │     │            │
│              │                                │     │            │
│              ▼                                │     │            │
│  ┌─────────────────────────┐                  │     │            │
│  │    Normal Usage         │                  │     │            │
│  │  Tracker / Log /        │                  │     │            │
│  │  Insights / Settings    │──── manual ──────┘     │            │
│  └───────────┬─────────────┘     lock               │            │
│              │                                      │            │
│              │ app backgrounded                      │            │
│              ▼                                      │            │
│  ┌─────────────────────────┐                        │            │
│  │  Background Timer       │                        │            │
│  │  ON_STOP: record time   │                        │            │
│  │  ON_START: check        │── timeout exceeded ────┘            │
│  │    elapsed vs threshold │                                     │
│  └─────────────────────────┘                                     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Autolock Mechanism

`CycleWiseApp` (`CycleWiseApp.kt`) implements `LifecycleEventObserver` and watches
for process-level lifecycle events:

**ON_STOP (app backgrounds):**
```kotlin
// CycleWiseApp.kt:64-68
Lifecycle.Event.ON_STOP -> {
    prefs.edit {
        putLong(KEY_LAST_BG_AT_ELAPSED, SystemClock.elapsedRealtime())
    }
}
```

**ON_START (app foregrounds):**
```kotlin
// CycleWiseApp.kt:70-84
Lifecycle.Event.ON_START -> {
    val minutes = autolockMinutesCache
    val last = prefs.getLong(KEY_LAST_BG_AT_ELAPSED, -1L)

    if (shouldLockNow(minutes, last)) {
        getKoin().getScopeOrNull(SESSION_SCOPE_ID)?.close()
        prefs.edit { remove(KEY_LAST_BG_AT_ELAPSED) }
        sessionBus.emitLogout()
    }
}
```

**Lock decision logic:**
```kotlin
// CycleWiseApp.kt:96-101
private fun shouldLockNow(minutes: Int, lastBgAtElapsed: Long): Boolean {
    if (minutes == 0) return true                    // Always lock immediately
    if (lastBgAtElapsed <= 0L) return false          // No background timestamp
    val elapsedMs = SystemClock.elapsedRealtime() - lastBgAtElapsed
    val thresholdMs = minutes * 60_000L
    return elapsedMs >= thresholdMs
}
```

### Manual Lock

From `SettingsScreen` (`ui/settings/SettingsScreen.kt`), the user can tap "Lock Now":

```kotlin
// SettingsScreen.kt:32
val session = getKoin().getScopeOrNull("session")

// SettingsScreen.kt:64-71
Button(
    enabled = session != null,
    onClick = {
        session?.close()                                    // Destroy all session-scoped objects
        navController.navigate(NavRoute.Passphrase.route) {
            popUpTo(0) { inclusive = true }                  // Clear entire backstack
        }
    }
) { Text("Lock Now") }
```

Both steps are required: `scope.close()` destroys the database and all session-scoped
objects (security), while `navController.navigate` with `popUpTo(0)` brings the user
back to the passphrase screen (UX). If you only close the scope without navigating,
the user would see a broken screen with no backing ViewModel.

### SessionBus

`SessionBus` (`session/SessionBus.kt`) is a singleton `SharedFlow` that decouples the
UI from lifecycle internals:

```kotlin
class SessionBus {
    private val _logout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val logout: SharedFlow<Unit> = _logout

    fun emitLogout() {
        _logout.tryEmit(Unit)
    }
}
```

Currently, `SessionBus.emitLogout()` is called by `CycleWiseApp` during autolock
(after closing the session scope). The bus provides infrastructure for any UI
component to observe logout signals by collecting `sessionBus.logout`. In the current
codebase, the primary lock mechanism is scope closure: when the session scope closes,
all session-scoped ViewModels are destroyed. For manual lock, `SettingsScreen`
closes the scope _and_ navigates to Passphrase directly. For autolock,
`CycleWiseApp` closes the scope and emits to `SessionBus`, allowing future screens
to react if needed.

---

## 2.4 TrackerScreen and TrackerViewModel

### Initialization

`TrackerViewModel` (`ui/tracker/TrackerViewModel.kt:62-96`) starts 4 Flow collectors
in its `init` block:

```kotlin
init {
    // 1. Day details (calendar dots and period highlighting)
    viewModelScope.launch {
        periodRepository.observeDayDetails()
            .map { domainMap -> domainMap.mapValues { (_, d) ->
                CalendarDayInfo(
                    isPeriodDay = d.isPeriodDay,
                    hasSymptoms = d.hasLoggedSymptoms,
                    hasMedications = d.hasLoggedMedications
                )
            }}
            .collect { _uiState.update { it.copy(dayDetails = uiReadyDetailsMap) } }
    }

    // 2. All periods
    viewModelScope.launch {
        periodRepository.getAllPeriods().collect { periods ->
            _uiState.update { it.copy(periods = periods) }
        }
    }

    // 3. Symptom library
    viewModelScope.launch {
        symptomLibraryProvider.symptoms.collect { symptoms ->
            _uiState.update { it.copy(symptomLibrary = symptoms) }
        }
    }

    // 4. Medication library
    viewModelScope.launch {
        medicationLibraryProvider.medications.collect { medications ->
            _uiState.update { it.copy(medicationLibrary = medications) }
        }
    }
}
```

These are all cold Flows from Room. The ViewModel survives configuration changes but
is destroyed when the session scope closes (logout/autolock).

### Event Handling

When the user interacts with the tracker:

- **`ScreenEntered`** — Calls `autoClosePeriodUseCase()` to close any stale ongoing
  period. Fired as a `LaunchedEffect` when the screen appears. The auto-close logic
  (`AutoCloseOngoingPeriodUseCase`): finds the last logged day within the ongoing
  period, checks if `lastLoggedDay + 1 day <= today`, and if so, closes the period
  with `endDate = lastLoggedDay`. This handles the case where the user forgot to
  manually end a period.
- **`DayTapped(date)`** — If a log exists for that date, shows a bottom sheet summary.
  If not, navigates to `DailyLogScreen` to create one.
- **`PeriodMarkDay(date)`** — Long-press toggle. If the date is inside a period,
  calls `unLogPeriodDay()`. If not, calls `logPeriodDay()`.
- **Delete flow** — `DeletePeriodRequested` shows a confirmation dialog.
  `DeletePeriodConfirmed` calls `periodRepository.deletePeriod()`.

---

## 2.5 Use Cases to Repository to DAO

Let's trace a concrete event end-to-end: **the user long-presses a day to mark it
as a period day**.

### The Event Chain

```
TrackerScreen
    │ long press on March 15
    ▼
TrackerViewModel.onEvent(TrackerEvent.PeriodMarkDay(date = March 15))
    │
    ▼
reduce() checks: is March 15 inside any existing period?
    │
    ├─── YES → periodRepository.unLogPeriodDay(March 15)
    │
    └─── NO  → periodRepository.logPeriodDay(March 15)
```

### The `logPeriodDay()` State Machine

`RoomPeriodRepository.logPeriodDay()` handles 4 scenarios inside a database
transaction:

**Scenario 1: Already inside a period**
```
Existing:    ├── Mar 13 ──── Mar 17 ──┤
Mark:                    Mar 15
Result:      ├── Mar 13 ──── Mar 17 ──┤  (no change, ensure PeriodLog exists)
```

**Scenario 2: Bridges two adjacent periods → Merge**
```
Existing:    ├── Mar 10 ── Mar 14 ──┤   ├── Mar 16 ── Mar 20 ──┤
Mark:                          Mar 15
Result:      ├── Mar 10 ──────────────────── Mar 20 ──┤  (merged into one)
```

**Scenario 3: Extends an adjacent period**
```
Existing:    ├── Mar 10 ── Mar 14 ──┤
Mark:                          Mar 15
Result:      ├── Mar 10 ──── Mar 15 ──┤  (end date extended)
```

**Scenario 4: Island day — creates a new single-day period**
```
Existing:    ├── Mar 10 ── Mar 12 ──┤              ├── Mar 20 ── Mar 25 ──┤
Mark:                                    Mar 15
Result:      ├── Mar 10 ── Mar 12 ──┤  ├Mar 15┤   ├── Mar 20 ── Mar 25 ──┤
```

### The `unLogPeriodDay()` State Machine

The inverse operation also handles 4 scenarios:

**Scenario 1: Single-day period → Delete entirely**
```
Existing:    ├Mar 15┤
Unmark:       Mar 15
Result:      (period deleted)
```

**Scenario 2: Start date → Advance start by one day**
```
Existing:    ├── Mar 15 ──── Mar 18 ──┤
Unmark:           Mar 15
Result:          ├── Mar 16 ── Mar 18 ──┤
```

**Scenario 3: End date → Retract end by one day**
```
Existing:    ├── Mar 15 ──── Mar 18 ──┤
Unmark:                        Mar 18
Result:      ├── Mar 15 ── Mar 17 ──┤
```

**Scenario 4: Middle day → Split into two periods**
```
Existing:    ├── Mar 13 ──────── Mar 18 ──┤
Unmark:                    Mar 15
Result:      ├── Mar 13 ── Mar 14 ──┤  ├── Mar 16 ── Mar 18 ──┤
```

### Transaction Wrapping

All mutations in `logPeriodDay()` and `unLogPeriodDay()` are wrapped in
`db.withTransaction { ... }` to ensure atomicity. If any step fails, the entire
operation rolls back.

---

## 2.6 Platform vs Shared Separation

### Platform Boundary Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   shared/src/commonMain/                         │
│                  (Pure Kotlin — no platform SDK)                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  domain/models/          domain/repository/              │   │
│  │  Period, DailyEntry,     PeriodRepository (interface)    │   │
│  │  FullDailyLog, etc.                                      │   │
│  │                                                          │   │
│  │  domain/usecases/        domain/services/                │   │
│  │  StartNewPeriodUseCase   PassphraseService (interface)   │   │
│  │  EndPeriodUseCase                                        │   │
│  │  GetOrCreateDaily...     domain/insights/                │   │
│  │  AutoCloseOngoing...     InsightEngine                   │   │
│  │  DebugSeederUseCase      6 generators                    │   │
│  │                                                          │   │
│  │  domain/providers/       Platform.kt                     │   │
│  │  SymptomLibraryProvider  expect fun getPlatform()        │   │
│  │  MedicationLibProvider   expect val num                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│ ═══════════════════ PLATFORM BOUNDARY ═════════════════════════ │
│                                                                 │
│                   composeApp/src/androidMain/                    │
│                  (Android SDK allowed)                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  androidData/repository/      ui/                        │   │
│  │  RoomPeriodRepository         TrackerScreen              │   │
│  │                               DailyLogScreen             │   │
│  │  androidData/local/dao/       InsightsScreen             │   │
│  │  8 Room DAOs                  SettingsScreen             │   │
│  │                               PassphraseScreen           │   │
│  │  androidData/local/database/                             │   │
│  │  PeriodDatabase               di/AppModule.kt            │   │
│  │  8 migrations                                            │   │
│  │                               services/                  │   │
│  │  androidData/local/entities/  PassphraseServiceAndroid    │   │
│  │  8 Room entities              SaltStorage                │   │
│  │  Converters, Mappers                                     │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Facts

- `shared/` has **zero** Android dependencies in `commonMain`. Its `commonMain`
  dependencies are: `kotlinx-datetime`, `kotlinx-coroutines-core`, `koin-core`,
  `uuid`, and `kotlinx-serialization-json`.
- `composeApp/` depends on `shared/` via `implementation(projects.shared)`.
- The reverse dependency (`shared/` importing `composeApp/`) is **never allowed**.
- All platform-specific code crosses the boundary through interfaces defined in
  `shared/` (`PeriodRepository`, `PassphraseService`).

---

## 2.7 DailyLogScreen and DailyLogViewModel

`DailyLogViewModel` (`ui/log/DailyLogViewModel.kt`) is the most event-rich ViewModel
in the codebase, handling 14 distinct event types for editing a single day's log.

### Two-Phase Initialization

```kotlin
init {
    // Phase 1: Load initial data, then dispatch a single comprehensive event
    viewModelScope.launch {
        val initialSymptoms = symptomLibraryProvider.symptoms.first()
        val initialMedications = medicationLibraryProvider.medications.first()
        val result = getOrCreateDailyLog(entryDate)
        val waterIntake = periodRepository.getWaterIntakeForDates(listOf(entryDate)).firstOrNull()

        onEvent(DailyLogEvent.LogLoaded(result, initialSymptoms, initialMedications))
        _uiState.update { it.copy(waterCups = waterIntake?.cups ?: 0) }
    }

    // Phase 2: Subscribe to library changes as events (not direct state mutations)
    symptomLibraryProvider.symptoms
        .onEach { symptoms -> onEvent(DailyLogEvent.LibraryUpdated(symptoms, ...)) }
        .launchIn(viewModelScope)
}
```

This pattern ensures all state changes flow through the reducer, even for
initialization. Library updates arrive as events, making the data flow auditable.

### The Hybrid Reducer

Most `DailyLogEvent` branches return updated state synchronously (mood changes, tag
additions, symptom toggles). A few launch async work:

| Event | Pure state return? | Async side effect? |
|-------|-------------------|-------------------|
| `FlowIntensityChanged` | Yes | No |
| `MoodScoreChanged` | Yes | No |
| `SymptomToggled` | Yes | No |
| `CreateAndAddSymptom` | Returns `currentState` | Yes — creates symptom in library, then updates state |
| `SaveLog` | Returns `currentState` | Yes — persists to repository, emits `NavigateBack` |
| `WaterIncrement` | Yes (optimistic) | Yes — upserts water intake to DB |

### Empty Log Detection

When the user taps Save, the ViewModel checks if the log contains any user-entered
data. If it's empty (no mood, no symptoms, no medications, no notes, no tags), it
skips the save and just navigates back:

```kotlin
private fun isLogEmpty(log: FullDailyLog): Boolean {
    val entry = log.entry
    return log.periodLog == null &&
            log.symptomLogs.isEmpty() &&
            log.medicationLogs.isEmpty() &&
            entry.moodScore == null &&
            entry.energyLevel == null &&
            entry.libidoLevel == null &&
            entry.customTags.isEmpty() &&
            entry.note.isNullOrBlank()
}
```

---

## 2.8 InsightsScreen and InsightsViewModel

`InsightsViewModel` (`ui/insights/InsightsViewModel.kt`) is simpler than the other
ViewModels — it generates insights once on initialization, with no ongoing events.

### Generation Pipeline

```kotlin
init { generateInsights() }

private fun generateInsights() {
    viewModelScope.launch {
        // 1. Fetch all data in one shot (Flow.first() for each)
        val allCycles = periodRepository.getAllPeriods().first()
        val allLogs = periodRepository.getAllLogs().first()
        val symptomLibrary = periodRepository.getSymptomLibrary().first()
        val topSymptomsCount = appSettings.topSymptomsCount.first()

        // 2. Run the InsightEngine
        val rawInsights = insightEngine.generateInsights(
            allPeriods = allCycles, allLogs = allLogs,
            symptomLibrary = symptomLibrary, topSymptomsCount = topSymptomsCount
        )

        // 3. Apply platform-specific date formatting
        val formattedInsights = rawInsights.map { insight ->
            when (insight) {
                is NextPeriodPrediction ->
                    insight.copy(formattedDateString = insight.predictedDate.toLocalizedDateString())
                is SymptomPhasePattern ->
                    insight.predictedDate?.toLocalizedDateString()?.let {
                        insight.copy(formattedPredictedDateString = it)
                    } ?: insight
                else -> insight
            }
        }

        _uiState.update { it.copy(isLoading = false, insights = formattedInsights) }
    }
}
```

### Why Post-Process Formatting?

Insight generators in `shared/` are pure Kotlin — they have no access to Android's
`DateFormat` or locale APIs. The `InsightsViewModel` applies platform-specific
formatting (via `toLocalizedDateString()`) _after_ generation. This keeps generators
portable to iOS without modification.

---

## 2.9 LockedWaterDraft — Pre-Authentication Water Tracking

A unique pattern in RhythmWise: the user can log water intake _before_ unlocking the
encrypted database.

### The Problem

The encrypted database is inaccessible until the user enters the passphrase. But water
tracking benefits from quick, frictionless logging throughout the day — requiring
unlock each time defeats the purpose.

### The Solution

`LockedWaterDraft` (`androidData/local/draft/LockedWaterDraft.kt`) stores water
intake in a **plaintext** DataStore (no encryption). This is acceptable because water
intake is low-sensitivity data.

```
Lock Screen                                     After Unlock
┌──────────────┐                                ┌──────────────────┐
│ User taps +  │                                │ WaterDraftSyncer │
│ water cup    │                                │ reads all drafts │
│      │       │                                │      │           │
│      ▼       │                                │      ▼           │
│ LockedWater  │     passphrase entered         │ Skips today      │
│ Draft (plain │ ──────────────────────────►    │ Merges others    │
│ DataStore)   │                                │ (higher wins)    │
│              │                                │ Clears synced    │
└──────────────┘                                └──────────────────┘
```

Key details:
- **Storage format:** JSON-serialized `WaterDraftPayload` with a version field for
  future migrations
- **Auto-prune:** Entries older than 29 days are dropped on any write
- **Max cups:** Clamped to 99 (`MAX_DRAFT_CUPS`)
- **Day rollover:** `ensureRolledOver(today)` detects day changes and resets

---

# Phase 3 — Code Organization and Design Principles

## 3.1 Why Interfaces Live in Shared Module

`PeriodRepository` and `PassphraseService` are defined as interfaces in
`shared/src/commonMain/`. This is intentional:

1. **Platform neutrality** — The interfaces have zero Android imports. They use only
   `kotlinx.datetime.LocalDate`, `kotlinx.coroutines.flow.Flow`, and domain model
   types from `shared/`.
2. **Testability** — Unit tests in `shared/` can mock these interfaces without
   needing an Android framework.
3. **iOS portability** — When the iOS app is built, it will provide its own
   implementations of these interfaces using Core Data or SQLite + CommonCrypto.

The `PassphraseService` interface is minimal by design:

```kotlin
// shared/.../domain/services/PassPhraseService.kt
interface PassphraseService {
    fun deriveKey(passphrase: String): ByteArray
}
```

Any platform can implement this contract with its preferred KDF library. Android uses
BouncyCastle's Argon2id. iOS might use Apple's CryptoKit or a ported Argon2 library.

---

## 3.2 Why Platform Implementations Live in composeApp

`RoomPeriodRepository` and `PassphraseServiceAndroid` import Android SDK classes
(`Context`, Room annotations, BouncyCastle). They cannot exist in `commonMain`.

Koin binds the interface to the implementation inside the session scope:

```kotlin
// di/AppModule.kt:102-114
scoped<PeriodRepository> {
    RoomPeriodRepository(
        db = get(), periodDao = get(), dailyEntryDao = get(),
        symptomDao = get(), medicationDao = get(),
        medicationLogDao = get(), symptomLogDao = get(),
        periodLogDao = get(), waterIntakeDao = get(),
    )
}
```

And for the passphrase service at singleton scope:

```kotlin
// di/AppModule.kt:63
single<PassphraseService> { PassphraseServiceAndroid(get()) }
```

This pattern means the domain layer only ever sees `PeriodRepository` and
`PassphraseService`. It never knows about Room, SQLCipher, or Argon2.

---

## 3.3 Why No Passphrase Storage

The app deliberately provides **no way** to recover a forgotten passphrase. This is
a security design choice, not a missing feature.

### The Lifecycle of a Passphrase

```
User types passphrase → String in memory
        │
        ▼
PassphraseService.deriveKey(passphrase) → ByteArray (32 bytes)
        │
        ▼
PeriodDatabase.create(context, key) → SupportFactory wraps the key
        │
        ▼
Database is open. The original passphrase String and derived key ByteArray
are now eligible for garbage collection.
        │
        ▼
Session scope closes → All references to the database are released.
The key ByteArray is GC'd. The database file on disk is unreadable
without re-deriving the key from the passphrase.
```

### Why This Matters

- **No passphrase on disk** — If the device is seized, the attacker must brute-force
  Argon2id (64 MB memory per attempt, 3 iterations) to read the database.
- **No recovery mechanism** — There is no "forgot password" flow. No backup key, no
  seed phrase, no email reset. The passphrase IS the only key.
- **Salt is not secret** — The 16-byte salt stored in SharedPreferences only prevents
  rainbow table attacks. It cannot be used to derive the key without the passphrase.

---

## 3.4 Why Session Scope is Ephemeral

The session scope's bounded lifetime serves multiple security purposes:

1. **Limited attack window** — The derived key exists in memory only while the user
   is actively using the app. An attacker who gains memory access after session close
   finds no key.
2. **Background timeout** — If the user switches apps, the autolock timer starts.
   After the configured timeout (default: 10 minutes, configurable to 0 for immediate
   lock), the session scope is destroyed and the user must re-enter the passphrase.
3. **Clean lifecycle** — All database-dependent objects (DAOs, repository, ViewModels)
   are created and destroyed together. There's no risk of a ViewModel holding a stale
   database reference after a lock.
4. **Deterministic teardown** — `scope.close()` immediately invalidates all scoped
   objects. There's no "grace period" where components might accidentally access a
   closed database.

---

## 3.5 How Design Enables iOS Porting

The shared module already compiles for iOS. Check `shared/build.gradle.kts:24-33`:

```kotlin
listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "Shared"
        isStatic = true
    }
}
```

The `iosApp/` directory already contains a skeleton Xcode project with
`ContentView.swift` and `iOSApp.swift`.

### What Already Compiles for iOS

Everything in `shared/src/commonMain/`:
- All domain models (`Period`, `DailyEntry`, `FullDailyLog`, enums, etc.)
- `PeriodRepository` interface
- `PassphraseService` interface
- All 5 use cases
- `InsightEngine` and all 6 generators
- `SymptomLibraryProvider` and `MedicationLibraryProvider`

### What an iOS App Would Need to Build

1. **`PeriodRepository` implementation** — Using Core Data, SQLite, or Realm instead
   of Room.
2. **`PassphraseService` implementation** — Using Apple CryptoKit or a Swift Argon2
   library.
3. **SwiftUI screens** — Passphrase, Tracker, DailyLog, Insights, Settings.
4. **DI wiring** — Koin works on iOS, or use a Swift DI framework.
5. **Session scope equivalent** — Manage the database key lifecycle.

The domain logic, insight calculations, and use case orchestration would be shared
without modification.

---

## 3.6 Where to Add New Features

### New Domain Model

1. Create the data class in `shared/src/commonMain/.../domain/models/NewModel.kt`
2. Create a Room entity in `composeApp/.../androidData/local/entities/NewModelEntity.kt`
3. Add mapper functions in `Mappers.kt` (domain ↔ entity)
4. Create a DAO in `composeApp/.../androidData/local/dao/NewModelDao.kt`
5. Register the entity in `PeriodDatabase` `@Database(entities = [...])`
6. Write a migration in `androidData/local/database/migrations/Migration_X_Y.kt`
7. Register the migration in `PeriodDatabase.create()`
8. Add abstract DAO accessor in `PeriodDatabase`
9. Register the DAO in `AppModule.kt` session scope: `scoped { get<PeriodDatabase>().newModelDao() }`
10. Add repository methods to `PeriodRepository` interface + `RoomPeriodRepository`
11. Bump the schema version in `PeriodDatabase`

### New Use Case

1. Create the class in `shared/src/commonMain/.../domain/usecases/NewUseCase.kt`
2. Inject `PeriodRepository` (and any other dependencies) via constructor
3. Implement the operator function `suspend operator fun invoke(...): Result`
4. Register in `AppModule.kt` session scope:
   ```kotlin
   scoped { NewUseCase(get()) }
   ```
5. Inject into the ViewModel that needs it:
   ```kotlin
   viewModel {
       SomeViewModel(newUseCase = get(), /* ... */)
   }
   ```

### New Repository Method

1. Add the method signature to `PeriodRepository` interface in `shared/`
2. Implement it in `RoomPeriodRepository` in `composeApp/`
3. If it needs a new query, add the `@Query` to the appropriate DAO
4. If it modifies the schema, write a migration (see [4.9](#49-database-migrations))

### New Screen

1. Create the Composable in `composeApp/.../ui/newscreen/NewScreen.kt`
2. Create a ViewModel: `NewScreenViewModel.kt`
3. Add a `NavRoute` to `NavRoutes.kt`:
   ```kotlin
   object NewScreen : NavRoute("new_screen", "New Screen")
   ```
4. Add the composable to the `NavHost` in `CycleWiseAppUI`
5. Register the ViewModel in `AppModule.kt` (session-scoped if it needs DB access)
6. If it should appear in bottom navigation, add it to `NavRoute.all`

### New Insight Generator

1. Create a class implementing `InsightGenerator` in
   `shared/.../domain/insights/generators/`
2. Define a new `Insight` subtype in `Insight.kt` with a unique `id` and `priority`
3. Add the generator to the list in `AppModule.kt`:
   ```kotlin
   factory {
       InsightEngine(listOf(
           // ... existing generators ...
           NewInsightGenerator()
       ))
   }
   ```

---

## 3.7 How to Avoid Breaking Layering Rules

### Anti-Pattern List

| Rule | Why |
|------|-----|
| **Never import `composeApp` from `shared`** | The shared module must remain platform-agnostic. Adding Android imports breaks iOS compilation. |
| **Never import Android SDK in `commonMain`** | `commonMain` compiles for all targets. `android.content.Context` doesn't exist on iOS. |
| **Never access DAOs from ViewModels directly** | ViewModels should only know about `PeriodRepository` (the interface). Direct DAO access bypasses the repository's transaction and mapping logic. |
| **Never store passphrase/key outside session scope** | The key must be GC-eligible when the session closes. Storing it in a singleton leaks it. |
| **Never call `db.withTransaction` from a ViewModel** | Transaction management is the repository's responsibility. ViewModels call use cases, which call repository methods. |
| **Always register session-scoped objects in `scope(SESSION_SCOPE)`** | Objects that need database access must live inside the session scope. Registering them as singletons means they'll outlive the database connection. |
| **Never use `kotlinx.datetime.Clock`** | The project uses `kotlin.time.Clock` exclusively. Using `kotlinx.datetime.Clock` introduces a deprecated API. |
| **Never skip database migrations** | Room requires an explicit migration for every schema version change. Use `fallbackToDestructiveMigration` only in tests, never in production. |

---

## 3.8 Key File Walkthroughs

### 1. `di/AppModule.kt` — DI Wiring

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/di/AppModule.kt` (155 lines)

This file is the single source of truth for all dependency injection. Reading order:

- **Line 43:** `SESSION_SCOPE` qualifier definition
- **Lines 56-82:** Singleton declarations (SaltStorage, AppSettings, SessionBus,
  PassphraseService, InsightEngine, LockedWaterDraft, ViewModels)
- **Line 65:** `InsightEngine` is a `factory` — a new instance is created each time
  it's requested, not cached. This is because insight generation is stateless.
- **Lines 84-153:** Session scope block
  - **Lines 86-89:** Database factory — key derivation + Room builder. This is where
    `parametersOf(passphrase)` is consumed.
  - **Lines 92-99:** 8 DAO providers, each delegating to `PeriodDatabase`
  - **Lines 102-114:** Repository binding (`PeriodRepository` → `RoomPeriodRepository`)
  - **Lines 117-123:** Providers and use cases
  - **Lines 126-152:** Session-scoped ViewModels (Tracker, DailyLog, Insights)

### 2. `ui/auth/PassphraseViewModel.kt` — Unlock Flow

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/auth/PassphraseViewModel.kt` (99 lines)

Key architectural details:
- **Implements `KoinComponent`** (line 40) — This is unusual for a ViewModel. It's
  necessary because `PassphraseViewModel` needs to create/access Koin scopes
  programmatically (not just receive injected dependencies).
- **Line 55:** Re-entrancy guard prevents duplicate session creation on double-tap
- **Lines 64-68:** Scope reuse pattern — `getScopeOrNull` before `createScope`
- **Line 70:** `parametersOf(passphrase)` is the bridge between user input and the
  session scope's database factory
- **Line 86:** On error, the scope is explicitly closed to prevent zombie sessions

### 3. `ui/tracker/TrackerViewModel.kt` — MVI Pattern

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/tracker/TrackerViewModel.kt` (190 lines)

Key architectural details:
- **Lines 62-96:** 4 init collectors demonstrate the reactive data flow pattern.
  Each collector transforms domain data into UI models.
- **Lines 98-102:** The `onEvent` → `reduce` dispatch is the MVI core. `_uiState.update`
  ensures atomic state transitions.
- **Lines 104-188:** `reduce()` is organized as a `when` expression over all event
  types. Side effects are launched via `viewModelScope.launch` inside the reducer.
- **Line 113:** `Clock.System.todayIn(TimeZone.currentSystemDefault())` — uses
  `kotlin.time.Clock`, never `kotlinx.datetime.Clock`.

### 4. `androidData/repository/RoomPeriodRepository.kt` — State Machines

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/repository/RoomPeriodRepository.kt` (614 lines)

This is the largest file in the codebase. Key sections:

- **Constructor:** Takes `db` + 8 DAOs (all injected by Koin session scope)
- **`saveFullLog()`:** Uses delete-then-insert transaction semantics — existing child
  records (period logs, symptom logs, medication logs) for a date are deleted before
  the new set is inserted. This avoids complex upsert logic.
- **`logPeriodDay()`:** Implements the 4-scenario state machine described in
  [section 2.5](#25-use-cases-to-repository-to-dao). Each scenario is a branch in a
  `when` expression inside `db.withTransaction`.
- **`unLogPeriodDay()`:** The inverse state machine — single-day deletion, start
  advance, end retract, or middle split.
- **`observeDayDetails()`:** Combines period Flows, symptom log Flows, and medication
  log Flows into a single `Map<LocalDate, DayDetails>`. This is the single source of
  truth for the calendar UI.
- **`seedDatabaseForDebug()`:** Generates 6 months of realistic cycle data with
  symptoms, medications, and mood scores.

### 5. `androidData/local/database/PeriodDatabase.kt` — SQLCipher Setup

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/database/PeriodDatabase.kt` (106 lines)

Key architectural details:
- **Line 57:** `version = 9` — the current schema version
- **Lines 47-55:** 8 entity classes registered in `@Database`
- **Lines 62-69:** 8 abstract DAO accessors
- **Lines 81-104:** The `create()` factory method:
  - Takes `Context`, `passphrase: ByteArray`, and optional `dbName`
  - Creates `SupportFactory(passphrase)` (line 86) — SQLCipher's bridge to Room
  - Registers all 8 migrations (lines 93-101)
  - Returns the built database

### 6. `androidData/local/entities/Converters.kt` — Room Type Converters

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/entities/Converters.kt` (59 lines)

Room requires type converters for non-primitive column types. These define how domain
types are serialized into SQLite columns:

| Kotlin Type | SQLite Type | Conversion |
|-------------|-------------|------------|
| `LocalDate` | TEXT | ISO-8601 string (e.g., `"2025-03-15"`) via `toString()` / `parse()` |
| `Instant` | INTEGER | Epoch milliseconds via `toEpochMilliseconds()` / `fromEpochMilliseconds()` |
| `FlowIntensity?` | TEXT (nullable) | Enum `name` string (e.g., `"HEAVY"`) via `valueOf()` |
| `LibidoLevel?` | TEXT (nullable) | Enum `name` string via `valueOf()` |
| `SymptomCategory?` | TEXT (nullable) | Enum `name` string via `valueOf()` |

Important: Nullable enums return `null` if the stored string is `null`. If an enum
value is ever renamed, a migration is required (existing data would break `valueOf()`).

> **Note:** The project uses `kotlin.time.Instant`, not `kotlinx.datetime.Instant`.

### 7. `androidData/local/entities/Mappers.kt` — Entity-Domain Conversion

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/entities/Mappers.kt` (108 lines)

Bidirectional extension functions convert between Room entities and domain models:

```kotlin
// Entity → Domain: uuid becomes the domain "id"
fun PeriodEntity.toDomain(): Period =
    Period(id = uuid, startDate = startDate, endDate = endDate, ...)

// Domain → Entity: id set to 0 so Room auto-generates the internal PK
fun Period.toEntity(): PeriodEntity =
    PeriodEntity(id = 0, uuid = id, startDate = startDate, ...)
```

**Key patterns:**
- **Dual ID system:** `PeriodEntity` has an internal auto-increment `id` (for fast
  Room joins) and a `uuid` field (exposed to the domain layer). When converting
  domain → entity, `id` is set to 0 so Room auto-generates it on insert.
- **JSON serialization:** `DailyEntry.customTags` (a `List<String>`) is serialized
  via `Json.encodeToString()` for storage and `Json.decodeFromString()` on retrieval.
- **Date string conversion:** `WaterIntakeEntity.date` is stored as ISO-8601 string,
  converted to/from `LocalDate` in the mapper.

---

# Phase 4 — Developer Workflows

## 4.1 How to Run the Project

### Prerequisites

- **Android Studio** (latest stable recommended)
- **JDK 11+** (Android Studio bundles one)
- **Android SDK 35** (install via SDK Manager)
- **An Android device or emulator** running API 26+

### Clone and Build

```bash
git clone <repository-url>
cd PeriodTracker
```

Open in Android Studio. Gradle sync will download all dependencies from the version
catalog.

### Run

1. Select the `composeApp` run configuration
2. Choose a device/emulator (API 26+)
3. Click Run

### First Launch

On first launch, you'll see the passphrase screen. Enter any passphrase to create a
new encrypted database. **Remember this passphrase** — there is no recovery.

### Debug Seeder

To populate the database with test data:

1. Unlock the app with your passphrase
2. Navigate to Settings
3. Tap "Seed Debug Data"

This runs `DebugSeederUseCase`, which **deletes all existing data** and generates 6
months of synthetic cycles with symptoms, medications, and mood scores. Useful for
testing the calendar, insights, and daily log screens with realistic data.

---

## 4.2 Unlocking Session During Testing

### Unit Tests

Unit tests mock the database layer entirely. They don't need a real passphrase or
SQLCipher:

```kotlin
@Test
fun `auto-close sets end date when period is stale`() {
    // Given
    val mockRepo = mockk<PeriodRepository>()
    coEvery { mockRepo.getCurrentlyOngoingPeriod() } returns stalePeriod
    coEvery { mockRepo.endPeriod(any(), any()) } returns closedPeriod

    val useCase = AutoCloseOngoingPeriodUseCase(mockRepo)

    // When
    runTest { useCase() }

    // Then
    coVerify { mockRepo.endPeriod(stalePeriod.id, expectedEndDate) }
}
```

### Instrumented Tests

The project uses a `CustomTestRunner` (configured in `composeApp/build.gradle.kts:95`)
for instrumented tests. The custom runner:
- Replaces the default `Application` with `TestCycleWiseApp`
- Stops any prior Koin state (`stopKoin()`)
- Restarts Koin with `allowOverride(true)` and a `testOverridesModule` that can
  replace production singletons (e.g., mock `PassphraseService`, in-memory database)

Unit tests that need Android resources (themes, strings) use **Robolectric**:
```kotlin
// composeApp/build.gradle.kts:126-129
testOptions {
    unitTests {
        isIncludeAndroidResources = true  // Required for Robolectric
    }
}
```

### CI Pipeline

GitHub Actions runs on every push/PR to `main` or `develop`
(`.github/workflows/test.yml`):

1. **Setup:** Ubuntu, Temurin JDK 17, Gradle
2. **Shared module tests:** `./gradlew :shared:testDebugUnitTest`
3. **composeApp tests:** `./gradlew :composeApp:testDebugUnitTest`
4. **Artifacts:** Test reports uploaded regardless of pass/fail

CI runs only unit tests (not instrumented/emulator tests). Test reports are available
as downloadable artifacts.

### Manual Testing

For manual testing, use the debug seeder (Settings → "Seed Debug Data") to quickly
populate the database with realistic data. The seeder button only appears in debug
builds (`BuildConfig.DEBUG`) and requires an active session (DB unlocked). It fetches
`DebugSeederUseCase` from the session scope:

```kotlin
// SettingsScreen.kt:114-121
session?.let {
    val seeder: DebugSeederUseCase = it.get()
    scope.launch {
        seeder()  // Deletes all data, then seeds 6 months of cycles
    }
}
```

---

## 4.3 Where to Place New Use Cases

Use cases live in `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/usecases/`.

### Template

Follow the pattern established by `GetOrCreateDailyLogUseCase`:

```kotlin
package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * [Description of what this use case does].
 *
 * **Precondition:** [state required before invocation]
 * **Postcondition:** [state after successful invocation]
 *
 * @return [description of return value and null semantics]
 */
class NewUseCase(
    private val repository: PeriodRepository
) {
    suspend operator fun invoke(/* parameters */): ReturnType {
        // Implementation
    }
}
```

### Registration

Register the use case in `AppModule.kt` inside the session scope:

```kotlin
scope(SESSION_SCOPE) {
    // ... existing registrations ...
    scoped { NewUseCase(get()) }
}
```

Then inject it into the ViewModel that needs it by adding it as a constructor
parameter and updating the ViewModel's registration in `AppModule.kt`.

---

## 4.4 Where to Place New Repository Methods

### Step 1: Add to the Interface

Add the method signature to `PeriodRepository` in
`shared/src/commonMain/.../domain/repository/PeriodRepository.kt`:

```kotlin
/**
 * [KDoc describing behavior, parameters, return semantics]
 */
suspend fun newMethod(param: Type): ReturnType
```

### Step 2: Implement in RoomPeriodRepository

Add the implementation in
`composeApp/.../androidData/repository/RoomPeriodRepository.kt`:

```kotlin
override suspend fun newMethod(param: Type): ReturnType {
    // Use DAOs, wrap multi-step mutations in db.withTransaction { ... }
}
```

### Step 3: Add DAO Queries if Needed

If the method requires new database queries, add `@Query` methods to the appropriate
DAO in `composeApp/.../androidData/local/dao/`.

### Step 4: Write a Migration if Schema Changes

If you added new columns or tables, you must write a migration. See
[section 4.9](#49-database-migrations).

---

## 4.5 Commit Conventions

RhythmWise follows **Conventional Commits 1.0.0**. See
[`docs/GIT_COMMIT_GUIDELINES.md`](GIT_COMMIT_GUIDELINES.md) for the full specification.

### Quick Reference

```
<type>(scope): <short description>

[optional body]

[optional footer]
Signed-off-by: Your Name <your@email.com>
```

**Types:** `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `style`, `perf`

**Scopes:** `shared`, `ui`, `data`, `deps`, `di`, `security`, `insights`, etc.

**Examples:**
```
feat(shared): add hydration tracking use case
fix(ui): correct layout for period summary bottom sheet
chore(deps): update Kotlin to 2.2.0
docs(onboarding): add developer onboarding guide
```

**DCO sign-off** is required on every commit.

---

## 4.6 Branching and PR Expectations

### Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable release branch. All code here passes tests. |
| `develop` | Integration branch for feature work. |
| `feature/*` | Individual feature branches, branched from `develop`. |
| `docs/*` | Documentation-only branches. |

### PR Requirements

1. Branch from `develop` (or `main` for hotfixes)
2. Write descriptive PR title using Conventional Commits format
3. All tests must pass
4. Code follows the style guide (`docs/CODE_STYLE.md`)
5. New public API has KDoc documentation (`docs/architecture/DOCUMENTATION_GUIDELINES.md`)
6. Commits include DCO sign-off

---

## 4.7 How to Avoid Leaking Secrets

### Never Log the Passphrase or Key

```kotlin
// BAD
Log.d("Debug", "Passphrase: $passphrase")
Log.d("Debug", "Key: ${key.toHex()}")

// GOOD
Log.d("Debug", "Key derivation complete, length=${key.size}")
```

### Never Persist the Passphrase or Key

The passphrase and derived key must only exist in local variables during the unlock
flow. They must never be:
- Written to SharedPreferences
- Stored in a ViewModel property
- Passed to analytics or crash reporting
- Included in exception messages

### Never Include Secrets in Exceptions

```kotlin
// BAD
throw IllegalStateException("Failed to open DB with key: ${key.contentToString()}")

// GOOD
throw IllegalStateException("Failed to open encrypted database")
```

### Debug-Only Features

The debug seeder (`DebugSeederUseCase`) is gated behind the Settings screen and should
only be available in debug builds. Use `BuildConfig.DEBUG` to guard development-only
features:

```kotlin
if (BuildConfig.DEBUG) {
    // Show debug seeder button
}
```

---

## 4.8 Common Architectural Mistakes

### 1. Injecting a session-scoped object into a singleton

```kotlin
// WRONG — SomeService is singleton, but PeriodRepository is session-scoped
single { SomeService(get<PeriodRepository>()) }  // Crash: no session scope active
```

Session-scoped objects don't exist until the user unlocks. Singletons are created at
app start. This will crash.

### 2. Importing Android SDK in commonMain

```kotlin
// WRONG — in shared/src/commonMain/
import android.content.Context  // Won't compile for iOS
```

Use `expect`/`actual` if you need platform-specific functionality in the shared module.

### 3. Missing database migration

```kotlin
// WRONG — Added a column but no migration
@Entity(tableName = "periods")
data class PeriodEntity(
    // ... existing columns ...
    val newColumn: String = ""  // Room will crash on upgrade
)
```

Every schema change requires an explicit `Migration` object and a version bump.

### 4. Accessing DAOs directly from ViewModel

```kotlin
// WRONG
class TrackerViewModel(private val periodDao: PeriodDao) { ... }

// RIGHT
class TrackerViewModel(private val periodRepository: PeriodRepository) { ... }
```

DAOs are implementation details of the data layer. ViewModels should only depend on
the repository interface.

### 5. Calling db.withTransaction from a ViewModel

```kotlin
// WRONG — ViewModel should not know about db transactions
viewModelScope.launch {
    db.withTransaction {
        periodDao.insert(...)
        dailyEntryDao.insert(...)
    }
}

// RIGHT — Repository handles transactions internally
viewModelScope.launch {
    periodRepository.saveFullLog(log)
}
```

### 6. Storing the key in a singleton

```kotlin
// WRONG — Key outlives the session
object KeyHolder {
    var currentKey: ByteArray? = null  // Security violation
}
```

The key must only exist within the session scope's lifetime.

### 7. Using kotlinx.datetime.Clock or kotlinx.datetime.Instant

```kotlin
// WRONG — deprecated in this project
import kotlinx.datetime.Clock
val now = Clock.System.now()

// RIGHT
import kotlin.time.Clock
val now = Clock.System.now()
```

The same applies to `Instant` — use `kotlin.time.Instant`, not
`kotlinx.datetime.Instant`. The type converters in `Converters.kt` import from
`kotlin.time.Instant`.

### 8. Forgetting to register in session scope

```kotlin
// WRONG — registered as singleton, but needs DB
single { NewService(get<PeriodRepository>()) }

// RIGHT — registered in session scope
scope(SESSION_SCOPE) {
    scoped { NewService(get()) }
}
```

---

## 4.9 Database Migrations

### Migration File Location

All migrations live in:
```
composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/database/migrations/
```

### Current Migrations

The database is currently at **version 9** with 8 migration files:

| Migration | File |
|-----------|------|
| v1 → v2 | `Migration_1_2.kt` |
| v2 → v3 | `Migration_2_3.kt` |
| v3 → v4 | `Migration_3_4.kt` |
| v4 → v5 | `Migration_4_5.kt` |
| v5 → v6 | `Migration_5_6.kt` |
| v6 → v7 | `Migration_6_7.kt` |
| v7 → v8 | `Migration_7_8.kt` |
| v8 → v9 | `Migration_8_9.kt` |

### Writing a New Migration

1. **Create the migration object:**

```kotlin
// Migration_9_10.kt
package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_9_10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_entries ADD COLUMN new_field TEXT DEFAULT ''")
    }
}
```

2. **Register in PeriodDatabase.create():**

```kotlin
// PeriodDatabase.kt
.addMigrations(
    Migration_1_2,
    // ... existing migrations ...
    Migration_8_9,
    Migration_9_10  // Add new migration
)
```

3. **Bump the version:**

```kotlin
@Database(
    entities = [...],
    version = 10,  // Was 9
    exportSchema = true
)
```

4. **Update the entity class** to match the new schema.

5. **Write a migration test** to verify the migration runs correctly on an existing
   database.

### Common ALTER TABLE Patterns

```sql
-- Add a nullable column
ALTER TABLE table_name ADD COLUMN column_name TEXT

-- Add a column with a default value
ALTER TABLE table_name ADD COLUMN column_name INTEGER NOT NULL DEFAULT 0

-- Create a new table
CREATE TABLE IF NOT EXISTS new_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL
)

-- Create an index
CREATE INDEX IF NOT EXISTS index_name ON table_name (column_name)
```

**Note:** SQLite does not support `DROP COLUMN`, `RENAME COLUMN` (before SQLite 3.25),
or `ALTER COLUMN`. For these operations, you must create a new table, copy data, drop
the old table, and rename the new one.

---

# Phase 5 — Future-Proofing

## 5.1 Planned iOS Module

### Current State

The iOS targets are already declared in `shared/build.gradle.kts:24-33`:

```kotlin
listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
).forEach { iosTarget ->
    iosTarget.binaries.framework {
        baseName = "Shared"
        isStatic = true
    }
}
```

The `iosApp/` directory contains a skeleton Xcode project with placeholder Swift files
(`ContentView.swift`, `iOSApp.swift`).

### What Needs to Be Built

1. **Repository implementation** — A new class implementing `PeriodRepository` using
   Core Data, SQLite, or another iOS-compatible persistence framework.
2. **Passphrase service** — A new class implementing `PassphraseService` using Apple
   CryptoKit or a Swift Argon2 library for key derivation.
3. **SwiftUI screens** — Passphrase, Tracker (calendar), DailyLog, Insights, Settings.
4. **DI wiring** — Koin works on iOS via `koin-core`, or use a native Swift DI solution.
5. **Session scope management** — Equivalent autolock and key lifecycle logic.

### What's Already Shared

All domain logic compiles for iOS out of the box:
- All models in `domain/models/`
- `PeriodRepository` interface
- `PassphraseService` interface
- All 5 use cases
- `InsightEngine` and all 6 generators
- `SymptomLibraryProvider` and `MedicationLibraryProvider`

---

## 5.2 Insight Engine Overview

### Two-Phase Pipeline

`InsightEngine` (`shared/.../domain/insights/InsightEngine.kt`) runs insight
generators in dependency order:

1. **Phase 1:** `NextPeriodPredictionGenerator` runs first. Its output (predicted
   next period date) is needed by other generators.
2. **Phase 2:** All remaining generators run with the prediction available in
   `InsightData.generatedInsights`.

Results are deduplicated by `Insight.id` and sorted by `priority` descending
(highest priority = shown first).

### InsightData — The Generator Input

Every generator receives an immutable `InsightData` snapshot:

```kotlin
data class InsightData(
    val allPeriods: List<Period>,                    // Sorted by start date descending
    val allLogs: List<FullDailyLog>,
    val symptomLibrary: List<Symptom>,               // For ID → name resolution
    val averageCycleLength: Double?,                 // null if < 2 completed periods
    val generatedInsights: List<Insight> = emptyList(),  // From earlier pipeline phases
    val topSymptomsCount: Int                        // User-configurable (default: 3)
)
```

The `averageCycleLength` is pre-calculated by `InsightEngine` from completed periods
(start-to-start intervals). It requires at least 2 completed periods to produce a
value. Generators that need it check for `null` and return an empty list if
insufficient data exists.

### InsightGenerator Contract

```kotlin
interface InsightGenerator {
    fun generate(data: InsightData): List<Insight>
}
```

Generators must be **pure** — no side effects, no mutations to `data`. Each is
responsible for its own minimum-data guards (returning an empty list when there's
insufficient data).

### Current Generators

| Generator | Priority | What It Produces |
|-----------|----------|-----------------|
| `NextPeriodPredictionGenerator` | 110 | Predicted start date of next period (highest priority — shown first) |
| `MoodPhasePatternGenerator` | 106 | Mood patterns across cycle phases with self-care advice |
| `CycleLengthAverageGenerator` | 100 | Average cycle length in days |
| `CycleLengthTrendGenerator` | 95 | Whether cycles are getting longer/shorter/stable (3-month trend) |
| `SymptomPhasePatternGenerator` | Variable | Symptom + phase correlations with recurrence rates |
| `SymptomRecurrenceGenerator` | 90 | Most frequently logged symptoms (lowest priority) |

Priority determines display order (110 first, 90 last). The current range is 90-110.
When adding a new generator, choose a priority that reflects its relative importance
to the user.

### Adding a New Generator

1. Create a class implementing `InsightGenerator` in
   `shared/.../domain/insights/generators/`
2. Define a new `Insight` sealed interface subtype in `Insight.kt` with a unique `id`
   and `priority`
3. Add the generator instance to the list in `AppModule.kt` (line 67-74)

The `InsightEngine` is registered as a `factory` in Koin (not singleton), meaning a
fresh instance is created each time insights are generated. This keeps it stateless.

If your generator depends on the prediction date, access it via
`data.generatedInsights.filterIsInstance<NextPeriodPrediction>().firstOrNull()`.
This works because the prediction generator always runs in Phase 1.

---

## 5.3 Encryption Versioning

### Current State

The app uses a single encryption scheme:
- Argon2id with fixed parameters (64 MB, 3 iterations, parallelism 1)
- SQLCipher AES-256-GCM

There is no version field on the database or salt storage yet.

### Future Approach

If KDF parameters need to change (e.g., increasing memory for stronger security):

1. Add a `kdf_version` field to `SaltStorage`
2. On unlock, check the version and use the matching KDF parameters
3. After successful unlock, optionally re-derive the key with new parameters and
   use SQLCipher's `PRAGMA rekey` to re-encrypt the database
4. Update the version field

This allows transparent migration without requiring users to re-enter their passphrase
or lose data.

---

## 5.4 Migration Strategy

### Schema Exports

Room exports the JSON schema to `composeApp/schemas/` on every build. These schemas
are version-controlled and used for migration testing.

### Fresh Install vs Upgrade

- **Fresh install:** Room creates the database at the latest version (currently v9).
  No migrations run.
- **Upgrade:** Room runs each migration sequentially from the installed version to the
  latest. For example, upgrading from v5 to v9 runs: `Migration_5_6` → `Migration_6_7`
  → `Migration_7_8` → `Migration_8_9`.

### Testing Migrations

Each migration should be tested to verify:
1. The SQL executes without errors
2. Existing data is preserved
3. New columns have correct defaults
4. Indexes are created properly

---

## 5.5 DI Scaling

### Current State

All DI wiring lives in a single `appModule` in `di/AppModule.kt` (155 lines). This
is manageable for the current codebase size.

### Future Module Split

As the codebase grows, `appModule` can be split into focused modules:

```kotlin
// di/DataModule.kt
val dataModule = module {
    scope(SESSION_SCOPE) {
        scoped { (passphrase: String) -> /* database */ }
        scoped { get<PeriodDatabase>().periodDao() }
        // ... all DAOs and repository ...
    }
}

// di/DomainModule.kt
val domainModule = module {
    factory { InsightEngine(/* generators */) }
    scope(SESSION_SCOPE) {
        scoped { GetOrCreateDailyLogUseCase(get()) }
        // ... all use cases and providers ...
    }
}

// di/UiModule.kt
val uiModule = module {
    viewModel { PassphraseViewModel(get(), get()) }
    scope(SESSION_SCOPE) {
        viewModel { TrackerViewModel(get(), get(), get(), get()) }
        // ... all session-scoped ViewModels ...
    }
}
```

Then in `CycleWiseApp.onCreate()`:
```kotlin
startKoin {
    modules(dataModule, domainModule, uiModule)
}
```

This split improves readability and allows teams to own specific modules. The session
scope qualifier (`SESSION_SCOPE`) works across modules — Koin resolves dependencies
globally within the same scope ID.

---

# Appendix: Dependency Layering Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      UI LAYER                              │  │
│  │  TrackerScreen, DailyLogScreen, InsightsScreen,            │  │
│  │  SettingsScreen, PassphraseScreen                          │  │
│  │  NavRoutes, CycleWiseAppUI                                 │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ observes state, sends events       │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   VIEWMODEL LAYER                          │  │
│  │  PassphraseViewModel, TrackerViewModel,                    │  │
│  │  DailyLogViewModel, InsightsViewModel,                     │  │
│  │  WaterTrackerViewModel                                     │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ calls use cases, repository        │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                    DOMAIN LAYER                             │  │
│  │  Use Cases: StartNewPeriod, EndPeriod, GetOrCreateDailyLog,│  │
│  │    AutoCloseOngoingPeriod, DebugSeeder                     │  │
│  │  InsightEngine + 6 generators                              │  │
│  │  Providers: SymptomLibraryProvider, MedicationLibProvider  │  │
│  │  Models: Period, DailyEntry, FullDailyLog, Symptom, etc.   │  │
│  │  Interfaces: PeriodRepository, PassphraseService           │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ implements interfaces              │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      DATA LAYER                            │  │
│  │  RoomPeriodRepository                                      │  │
│  │  8 DAOs: PeriodDao, DailyEntryDao, SymptomDao,             │  │
│  │    MedicationDao, MedicationLogDao, SymptomLogDao,         │  │
│  │    PeriodLogDao, WaterIntakeDao                             │  │
│  │  8 Entities + Converters + Mappers                         │  │
│  │  PeriodDatabase (Room + SQLCipher)                         │  │
│  │  8 Migrations (v1 → v9)                                    │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ platform services                  │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                  INFRASTRUCTURE LAYER                       │  │
│  │  PassphraseServiceAndroid (Argon2id KDF)                   │  │
│  │  SaltStorage (SharedPreferences, 16-byte salt)             │  │
│  │  AppSettings (DataStore preferences)                       │  │
│  │  SessionBus (logout event bus)                             │  │
│  │  LockedWaterDraft (water intake while locked)              │  │
│  │  CycleWiseApp (lifecycle observer, autolock)               │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

Dependencies flow DOWNWARD only.
Each layer depends only on the layer directly below it (and the domain layer's interfaces).
```

---

_This guide is maintained alongside the codebase. If you find inaccuracies, open a PR
against `docs/DeveloperOnboarding.md`._
