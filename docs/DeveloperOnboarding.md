# RhythmWise Developer Onboarding Guide

## Purpose

This guide brings new developers from zero to productive on the RhythmWise codebase.
It explains _why_ the architecture is shaped the way it is, walks through the critical
code paths, and provides step-by-step recipes for the most common development tasks.

**Target audience:** Developers of any experience level. This guide assumes basic Kotlin
familiarity but explains all architectural patterns (MVVM, MVI, Clean Architecture,
Repository Pattern, Dependency Injection) from first principles.

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

- [Phase 0 — From 0 to 60](#phase-0--from-0-to-60)
  - [0.1 What Is This Project?](#01-what-is-this-project)
  - [0.2 Where Is the Source Code?](#02-where-is-the-source-code)
  - [0.3 How Do I Build and Run It?](#03-how-do-i-build-and-run-it)
  - [0.4 Can I Just Get an APK?](#04-can-i-just-get-an-apk)
  - [0.5 What Is KMP and Why Should I Care?](#05-what-is-kmp-and-why-should-i-care)
  - [0.6 What Frameworks and Libraries Are Used?](#06-what-frameworks-and-libraries-are-used)
  - [0.7 What Architecture and Design Patterns Are Used?](#07-what-architecture-and-design-patterns-are-used)
  - [0.8 How Do I Contribute? (Git Flow)](#08-how-do-i-contribute-git-flow)
  - [0.9 Where Do I Go from Here?](#09-where-do-i-go-from-here)
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
  - [2.9 SettingsScreen and SettingsViewModel](#29-settingsscreen-and-settingsviewmodel)
  - [2.10 LockedWaterDraft — Pre-Authentication Water Tracking](#210-lockedwaterdraft--pre-authentication-water-tracking)
  - [2.11 Coach Mark / Tutorial System](#211-coach-mark--tutorial-system)
  - [2.12 Educational Content System](#212-educational-content-system)
  - [2.13 Reusable UI Components](#213-reusable-ui-components)
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
  - [4.10 Static Analysis (Lint and Detekt)](#410-static-analysis-lint-and-detekt)
- [Phase 5 — Future-Proofing](#phase-5--future-proofing)
  - [5.1 Planned iOS Module](#51-planned-ios-module)
  - [5.2 Insight Engine Overview](#52-insight-engine-overview)
  - [5.3 Encryption Versioning](#53-encryption-versioning)
  - [5.4 Migration Strategy](#54-migration-strategy)
  - [5.5 DI Scaling](#55-di-scaling)
- [Appendix: Dependency Layering Diagram](#appendix-dependency-layering-diagram)

---

# Phase 0 — From 0 to 60

This section answers the questions every developer asks within the first five minutes
of looking at the repository. If you are already familiar with the project, skip ahead
to [Phase 1](#phase-1--foundation).

## 0.1 What Is This Project?

RhythmWise is a **privacy-first menstrual cycle tracker** built with Kotlin
Multiplatform (KMP). It tracks periods, symptoms, medications, mood, and water intake,
then generates cycle-length predictions and pattern insights — all without ever leaving
the device.

| Detail | Value |
|--------|-------|
| Package namespace | `com.veleda.cyclewise` |
| Encryption | Argon2id key derivation + SQLCipher AES-256-GCM |
| Network posture | **Zero** — no internet permission, no telemetry, no cloud sync |

Everything is encrypted at rest behind a user-chosen passphrase. There is no recovery
mechanism by design — if the passphrase is lost, the data is gone. For the full threat
model, see [`docs/SECURITY_MODEL.md`](SECURITY_MODEL.md).

## 0.2 Where Is the Source Code?

The repository has two Gradle modules:

| Module | Role | Think of it as… |
|--------|------|-----------------|
| `shared/` | Platform-agnostic domain logic (models, use cases, insights, repository interface) | The **brains** — pure Kotlin, no Android imports |
| `composeApp/` | Android application (Compose UI, Room database, DI wiring, encryption service) | The **face** — everything the user sees and touches |

**"Where are the brains?"** — Domain logic lives in:

```
shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/
├── models/        # Period, DailyEntry, Symptom, enums
├── usecases/      # StartNewPeriodUseCase, EndPeriodUseCase, etc.
├── insights/      # InsightEngine + generators
├── repository/    # PeriodRepository interface
├── providers/     # SymptomLibraryProvider, MedicationLibraryProvider
└── services/      # PassphraseService (abstract)
```

**"Where does the app start?"**

| File | What it does |
|------|-------------|
| `composeApp/.../CycleWiseApp.kt` | `Application` subclass — bootstraps Koin, manages autolock lifecycle |
| `composeApp/.../MainActivity.kt` | Android `Activity` entry point — calls `setContent { CycleWiseAppUI() }` |
| `composeApp/.../ui/nav/NavRoutes.kt` | Defines the navigation routes and the four-tab bottom navigation bar |

For the module breakdown, see [1.2 Module Structure](#12-module-structure).

## 0.3 How Do I Build and Run It?

### Prerequisites

- **Android Studio** — latest stable release (Ladybug or newer)
- **JDK 11+** — bundled with Android Studio, no separate install needed
- **Android SDK 35** — install via Android Studio's SDK Manager

> Gradle 8.13 and Kotlin 2.2.0 ship via the Gradle wrapper and version catalog —
> you should **not** need to install them manually.

### Option A — Android Studio (recommended)

1. Clone the repository: `git clone <repo-url>`
2. Open the project root in Android Studio
3. Wait for Gradle sync to finish (first sync downloads dependencies)
4. Select the **composeApp** run configuration
5. Press **Run** (green play button) with an emulator or connected device

### Option B — Command line

```bash
# Build the debug APK
./gradlew clean assembleDebug

# Install on a connected device or running emulator
./gradlew installDebug
```

The APK is written to:

```
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### First launch

The app opens to a **passphrase screen** — create any passphrase you like. There is no
recovery, so remember it. Once unlocked, open **Settings** and tap **Seed Debug Data**
to populate the calendar with sample periods and symptoms.

### Running tests

```bash
./gradlew testDebugUnitTest
```

For full test documentation, see [`docs/testing/RUNNING_TESTS.md`](testing/RUNNING_TESTS.md).
For detailed run instructions, see [4.1 How to Run the Project](#41-how-to-run-the-project).

## 0.4 Can I Just Get an APK?

The project is preparing for its initial Play Store release. Until it is published,
you need to build from source. If you followed
[0.3](#03-how-do-i-build-and-run-it), you already have an APK sitting in
`composeApp/build/outputs/apk/debug/`.

## 0.5 What Is KMP and Why Should I Care?

**KMP (Kotlin Multiplatform)** lets you write business logic once in Kotlin and compile
it for Android, iOS, desktop, and more. The key concept is **source sets**:

| Source set | Compiles for | Can import |
|------------|-------------|------------|
| `commonMain` | All targets | Pure Kotlin + `kotlinx` libraries only |
| `androidMain` | Android | Android SDK, Jetpack, Room |
| `iosMain` | iOS | UIKit, Foundation, Apple frameworks |

**Day-to-day**, this means:

- Domain code (models, use cases, insights) goes in `shared/src/commonMain/`
- Android-specific code (Compose UI, Room DAOs, encryption) goes in `composeApp/`

**Why RhythmWise uses it:** The entire domain layer already compiles for iOS. When an
iOS app is eventually built, all business logic, use cases, and the insight engine are
ready — only the UI and platform services need to be written.

For the deep dive on KMP, `expect`/`actual` declarations, and source set rules, see
[1.1 What is KMP and Why We Use It](#11-what-is-kmp-and-why-we-use-it).

## 0.6 What Frameworks and Libraries Are Used?

All versions live in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) — you
will never see a hardcoded version string in a `build.gradle.kts` file.

| Library | What it does | Why it is here |
|---------|-------------|----------------|
| Jetpack Compose | Declarative UI toolkit | All screens are `@Composable` functions |
| Room | SQLite ORM with compile-time query verification | Type-safe database access |
| SQLCipher | Transparent AES-256 encryption for SQLite | Encrypts the entire database at rest |
| Koin | Lightweight dependency injection | Wires all layers together; supports session scoping |
| BouncyCastle + Argon2id | Cryptographic key derivation | Derives the database key from the user's passphrase |
| kotlinx-coroutines | Structured concurrency | All async work (DB queries, Flow streams) |
| kotlinx-datetime | Multiplatform date/time | Date math for cycle calculations and insights |
| WorkManager | Background task scheduling | Auto-close stale periods after midnight |
| MockK | Kotlin-first mocking library | Unit test doubles for repositories and use cases |
| Turbine | Flow testing library | Asserts emissions from `StateFlow` and `SharedFlow` |
| Robolectric | Android unit tests without emulator | Tests Room DAOs and Android components on JVM |

For Gradle configuration details, see
[1.3 Gradle Structure and Version Catalog](#13-gradle-structure-and-version-catalog).

## 0.7 What Architecture and Design Patterns Are Used?

RhythmWise follows **Clean Architecture** with the **MVVM** (Model-View-ViewModel)
presentation pattern. The dependency flow is a single direction:

```
Compose Screen → ViewModel → Use Case → Repository → DAO → SQLCipher DB
```

| Pattern | One-sentence summary |
|---------|---------------------|
| Clean Architecture | Business logic in `shared/` has zero knowledge of Android, Room, or Compose |
| MVVM | Each screen has a ViewModel that exposes a `StateFlow<UiState>` the screen collects |
| Repository pattern | `PeriodRepository` interface defines all data operations; `RoomPeriodRepository` implements them with Room |
| Session scope | The database, DAOs, repository, and use cases are created when the user unlocks and destroyed on logout — the key never persists |

> **The single most important concept to understand is the session scope.** It controls
> the lifetime of the encryption key and every object that depends on it. If you read
> nothing else before writing code, read
> [2.3 Session Scope Lifetime](#23-session-scope-lifetime).

For architecture deep dives, see [1.4 Dependency Injection with Koin](#14-dependency-injection-with-koin),
[1.6 MVVM and MVI Pattern](#16-mvvm-and-mvi-pattern),
[1.7 Repository Pattern and Clean Layering](#17-repository-pattern-and-clean-layering),
and [Phase 2 — Architectural Deep Dive](#phase-2--architectural-deep-dive).

## 0.8 How Do I Contribute? (Git Flow)

### Branches

| Branch | Purpose |
|--------|---------|
| `main` | Stable release branch — always builds, always passes tests |
| `dev` | Integration branch — features merge here first |
| `<issue#>-<type>-<description>` | One branch per feature or fix (branched from `dev`), e.g. `35-feature-cycle-phase-calculation` |

### Commits

All commits use **Conventional Commits 1.0.0** format with a **DCO sign-off**:

```
feat(tracker): add period-merge logic for adjacent periods

Implement automatic merging when a new period overlaps an existing one.
The merge preserves daily entries from both periods.

Signed-off-by: Your Name <your.email@example.com>
```

### PR checklist

Before opening a pull request, verify:

- [ ] Branch is based on `dev` (not `main`)
- [ ] All tests pass: `./gradlew testDebugUnitTest`
- [ ] Code follows project style ([`docs/CODE_STYLE.md`](CODE_STYLE.md))
- [ ] New public members have KDoc comments
- [ ] Commit messages include DCO sign-off

For full commit rules, see [`docs/GIT_COMMIT_GUIDELINES.md`](GIT_COMMIT_GUIDELINES.md).
For branching and PR expectations, see [4.5 Commit Conventions](#45-commit-conventions)
and [4.6 Branching and PR Expectations](#46-branching-and-pr-expectations).

## 0.9 Where Do I Go from Here?

Pick the path that matches what you need:

| I want to… | Start here |
|------------|-----------|
| Understand the full architecture | [Phase 1](#phase-1--foundation) then [Phase 2](#phase-2--architectural-deep-dive) |
| Add a new feature | [3.6 Where to Add New Features](#36-where-to-add-new-features) then [Phase 4](#phase-4--developer-workflows) |
| Work on the database or encryption | [1.5 SQLCipher and Passphrase-Derived Encryption](#15-sqlcipher-and-passphrase-derived-encryption) then [2.2 PassphraseScreen and Unlock Flow](#22-passphrasescreen-and-unlock-flow) |
| Write or run tests | [`docs/testing/RUNNING_TESTS.md`](testing/RUNNING_TESTS.md) |
| Check code style rules | [`docs/CODE_STYLE.md`](CODE_STYLE.md) |
| Understand the security model | [`docs/SECURITY_MODEL.md`](SECURITY_MODEL.md) |

**If you read nothing else, read [Phase 1](#phase-1--foundation).** It covers KMP,
module structure, Gradle, Koin, encryption, MVVM, and the repository pattern — the
seven pillars that everything else in this codebase is built on.

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

| Package | What lives here |
|---------|-----------------|
| `domain/models/` | Domain data classes (e.g., `Period`, `DailyEntry`, `FullDailyLog`) and enums (e.g., `FlowIntensity`, `SymptomCategory`) |
| `domain/repository/` | `PeriodRepository` interface — the single data access contract |
| `domain/usecases/` | One class per business operation (e.g., `StartNewPeriodUseCase`, `AutoCloseOngoingPeriodUseCase`, `TutorialSeederUseCase`) |
| `domain/insights/` | `InsightEngine` orchestrator, `Insight` sealed interface, and a `generators/` subdirectory with one generator per insight type |
| `domain/providers/` | Provider interfaces and reactive library providers (e.g., `SymptomLibraryProvider`, `EducationalContentProvider` interface) |
| `domain/services/` | `PassphraseService` interface (abstract KDF contract) |

### `composeApp/` — Android Application

Contains the Jetpack Compose UI and all Android-specific implementations.

| Package | What lives here |
|---------|-----------------|
| `androidData/local/dao/` | Room DAOs — one per database table (e.g., `PeriodDao`, `DailyEntryDao`) |
| `androidData/local/database/` | `PeriodDatabase` (Room + SQLCipher) and `migrations/` directory with one migration object per schema version step |
| `androidData/local/entities/` | Room `@Entity` classes, `Converters` (type converters), and `Mappers` (entity ↔ domain) |
| `androidData/local/draft/` | `LockedWaterDraft` — persists water intake while the DB is locked |
| `androidData/repository/` | `RoomPeriodRepository` — implements `PeriodRepository` with Room |
| `domain/usecases/` | Android-specific use cases that depend on platform APIs (e.g., `DeleteAllDataUseCase`) |
| `ui/auth/` | Passphrase/unlock screen, `WaterTrackerViewModel`, `WaterDraftSyncer` |
| `ui/tracker/` | Calendar screen — `TrackerScreen`, `TrackerViewModel`, `TrackerEvent`, and supporting composables |
| `ui/log/` | Daily log screen — `DailyLogScreen`, `DailyLogViewModel`, `DailyLogEvent` |
| `ui/insights/` | Insights screen — `InsightsScreen`, `InsightsViewModel` |
| `ui/settings/` | Settings screen — `SettingsScreen`, `SettingsViewModel`, `SettingsEvent`, and sub-page composables |
| `ui/coachmark/` | Post-login tutorial overlay system — `CoachMarkDef`, `CoachMarkOverlay`, `CoachMarkState`, `HintPreferences` |
| `ui/components/` | Reusable UI components (e.g., `EducationalBottomSheet`, `InfoButton`, `MarkdownText`) |
| `ui/nav/` | Navigation routes, bottom nav bar, and `NavHost` wiring |
| `ui/theme/` | Theme definitions — colors, shapes, typography, dimensions |
| `di/` | `AppModule.kt` — all Koin DI wiring |
| `services/` | `PassphraseServiceAndroid` (Argon2 impl), `SaltStorage` |
| `session/` | `SessionManager` (a `KoinComponent` for runtime scope management), `SessionBus` (logout event bus), `KeyFingerprintHolder` |
| `settings/` | `AppSettings` — DataStore preferences wrapper |
| `reminders/` | `ReminderScheduler`, `ReminderNotifier`, and WorkManager workers |

---

## 1.3 Gradle Structure and Version Catalog

### Version Catalog

All dependency versions are centralized in `gradle/libs.versions.toml` — you will
never see a hardcoded version string in a `build.gradle.kts` file. Key libraries:

| Library | Purpose |
|---------|---------|
| Kotlin | Language and compiler |
| Compose Multiplatform | UI framework |
| Room | Database ORM |
| SQLCipher | AES-256 database encryption |
| Koin | Dependency injection |
| BouncyCastle | Argon2id KDF implementation |
| kotlinx-datetime | Cross-platform date/time |
| kotlinx-coroutines | Asynchronous programming |
| MockK | Mocking in tests |
| Turbine | Flow testing |
| Robolectric | Android unit tests without device |

> All versions are defined in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml).

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
// composeApp/build.gradle.kts, inside the room { } block
extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}
```

---

## 1.4 Dependency Injection with Koin

### What is Dependency Injection?

**Dependency Injection (DI)** is a design pattern where an object receives its
dependencies from an external source rather than creating them itself. Think of it
as the "don't call us, we'll call you" principle for object construction.

**Without DI (tight coupling):**
```kotlin
class TrackerViewModel {
    // This ViewModel creates its own dependencies — tightly coupled
    private val database = PeriodDatabase.create(context, key)
    private val dao = database.periodDao()
    private val repository = RoomPeriodRepository(database, dao, ...)
}
```

Problems: `TrackerViewModel` knows how to build a database, what DAO implementation to
use, and what repository class to instantiate. If you change the database, every
ViewModel that constructs it must change too. Unit testing requires a real database
because you can't swap in a mock.

**With DI (loose coupling):**
```kotlin
class TrackerViewModel(
    private val periodRepository: PeriodRepository  // Injected — just an interface
)
```

Now `TrackerViewModel` only knows about the _interface_ `PeriodRepository`. The DI
framework handles constructing the real implementation and passing it in. During
tests, you pass a mock instead. If the database implementation changes, the ViewModel
code is untouched.

### How Koin Works

Koin is a lightweight DI framework for Kotlin. You define _modules_ that describe how
to create objects, then Koin resolves the dependency graph at runtime.

Key Koin primitives:
- **`single { ... }`** — Creates one instance for the entire app lifetime. Every
  `get()` call returns the same object.
- **`factory { ... }`** — Creates a _new_ instance every time `get()` is called.
  No caching.
- **`scoped { ... }`** — Creates one instance _per scope_. The instance lives as long
  as the scope lives, then is destroyed with it.
- **`viewModel { ... }`** — Like `factory`, but integrates with Android's ViewModel
  lifecycle (survives configuration changes like screen rotation).

### What is a Scope?

A _scope_ is a named container with a bounded lifetime. Objects registered inside a
scope are created when the scope opens and destroyed when it closes. Different objects
can live for different durations:

- A `SaltStorage` needs to live for the entire app process → **singleton scope**
- A `TrackerViewModel` needs to live only while the database is open → **session scope**

This matters for security: the database encryption key must not outlive the user's
authenticated session, so all database-related objects live in a scope that is destroyed
on logout.

### RhythmWise's Two DI Lifetimes

All DI wiring lives in a single file: `di/AppModule.kt`. Koin is bootstrapped in
`CycleWiseApp.onCreate()`:

```kotlin
startKoin {
    androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
    androidContext(this@CycleWiseApp)
    modules(appModule)
    allowOverride(false)
}
```

The critical architectural pattern is the **session scope**. The app has two DI
lifetimes with fundamentally different lifecycles:

#### 1. Singleton Scope (lives for the app process)

These objects are created once when the app starts and never destroyed until the
process dies. Representative examples:

| Component | Purpose |
|-----------|---------|
| `SaltStorage` | Persists 16-byte encryption salt in SharedPreferences |
| `AppSettings` | DataStore-backed user preferences |
| `SessionBus` | SharedFlow event bus for logout/lock signals |
| `PassphraseService` | Argon2id key derivation interface (bound to `PassphraseServiceAndroid`) |
| `InsightEngine` | Orchestrates insight generators (registered as `factory`, not `single` — a fresh instance is created each call, so no stale state accumulates) |

See `di/AppModule.kt` for the full list of singleton registrations.

#### 2. Session Scope (created on unlock, destroyed on logout/autolock)

These objects are created _after_ the user enters the correct passphrase and the
database is successfully opened. They are all destroyed together when the session ends.
Representative examples:

| Component | Purpose |
|-----------|---------|
| `PeriodDatabase` | SQLCipher-encrypted Room database |
| DAOs (one per table) | e.g., `PeriodDao`, `DailyEntryDao`, `SymptomDao` |
| `RoomPeriodRepository` | Implements `PeriodRepository` with Room |
| Use cases | e.g., `GetOrCreateDailyLogUseCase`, `AutoCloseOngoingPeriodUseCase` |
| Session-scoped ViewModels | e.g., `TrackerViewModel`, `DailyLogViewModel`, `InsightsViewModel` |

See `di/AppModule.kt`'s `scope(SESSION_SCOPE) { ... }` block for the full list.

### The SESSION_SCOPE Qualifier

```kotlin
// di/AppModule.kt, top-level constant
val SESSION_SCOPE: Qualifier = named("UnlockedSessionScope")
```

All session-scoped objects are registered inside `scope(SESSION_SCOPE) { ... }`:

```kotlin
// di/AppModule.kt, inside the scope(SESSION_SCOPE) block
scope(SESSION_SCOPE) {
    scoped { (passphrase: String) ->
        createDatabaseAndZeroizeKey(androidContext(), get(), passphrase, get())
    }

    scoped { get<PeriodDatabase>().periodDao() }
    // ... remaining DAOs ...

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
// PassphraseViewModel.unlock(), inside the IO dispatcher block
val db = sessionScope.get<PeriodDatabase> { parametersOf(passphrase) }
```

Inside the scope definition, the `(passphrase: String)` parameter is destructured and
passed to `createDatabaseAndZeroizeKey`, which derives the key, passes `key.copyOf()`
to the `SupportFactory` (so the factory has its own independent copy), and zeros the
original key immediately:

```kotlin
// di/AppModule.kt, top-level function
internal fun createDatabaseAndZeroizeKey(
    context: Context,
    passphraseService: PassphraseService,
    passphrase: String,
    keyFingerprintHolder: KeyFingerprintHolder,
): PeriodDatabase {
    val key = passphraseService.deriveKey(passphrase)
    return try {
        keyFingerprintHolder.store(key)
        migrateLegacyZeroKeyIfNeeded(context, key.copyOf())
        PeriodDatabase.create(context, key.copyOf())
    } finally {
        key.fill(0)   // zeroize on both success and failure
    }
}
```

**Why `key.copyOf()`?** `SupportFactory` stores the key array _by reference_, not by
copy. `Room.databaseBuilder().build()` returns _without_ opening the database — the
actual file open is deferred. If the original key were zeroed before the database was
actually opened, `SupportFactory` would read an all-zeros array and every passphrase
would appear valid. Passing `key.copyOf()` gives `SupportFactory` its own array so the
original can be zeroed immediately. The copy is zeroed by SQLCipher's built-in
`clearPassphrase` mechanism when the database is force-opened later.

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
   key.copyOf() → SupportFactory(keyCopy)
        │
        ▼
   Room.databaseBuilder(...)
       .openHelperFactory(factory)
       .build()
        │
        ▼
   db.openHelper.writableDatabase  ← forces SQLCipher to consume the real key
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
// services/SaltStorage.kt, inside getOrCreateSalt()
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

The passphrase and derived key **never persist to disk**. The derived key is
**actively zeroized** (`fill(0)`) immediately after the database builder returns
via `createDatabaseAndZeroizeKey()` in `AppModule.kt`. A `try/finally` block
guarantees zeroization even if `PeriodDatabase.create()` throws.

The `PassphraseService` interface documents this contract explicitly:

```kotlin
// shared/.../domain/services/PassPhraseService.kt
// Security contract:
// - The derived key must never be persisted to disk.
// - Callers must zeroize the returned ByteArray when the session scope closes.
// - The KDF must be computationally expensive to resist brute-force attacks.
```

---

## 1.6 MVVM and MVI Pattern

### What is MVVM?

**MVVM (Model-View-ViewModel)** is a UI architecture pattern that separates an
application into three interconnected layers:

- **Model** — The data and business logic. In RhythmWise, this includes domain models
  (`Period`, `DailyEntry`), the repository (`PeriodRepository`), and use cases
  (`StartNewPeriodUseCase`). The Model knows nothing about the UI.

- **View** — The UI layer that displays data and captures user input. In RhythmWise,
  this is the Jetpack Compose screens (`TrackerScreen`, `DailyLogScreen`, etc.).
  The View observes the ViewModel's state and renders it. It sends user actions to
  the ViewModel but never directly accesses the Model.

- **ViewModel** — The intermediary between View and Model. It holds UI state, processes
  user actions, and communicates with the Model layer. In Android, ViewModels survive
  configuration changes (like screen rotation), so UI state isn't lost.

**Why MVVM exists:**
1. **Separation of concerns** — UI rendering logic is isolated from business logic.
2. **Testability** — ViewModels can be unit-tested without a real UI or Android device.
3. **Lifecycle survival** — Android's `ViewModel` class survives configuration changes,
   preventing data loss on screen rotation.

**Data flow in MVVM:**
```
View ──observes──► ViewModel.state (StateFlow)
View ──calls────► ViewModel.doSomething()
                       │
                       ▼
                  Model (Repository, Use Cases)
```

### What is MVI?

**MVI (Model-View-Intent)** builds on MVVM by adding a strict _unidirectional data
flow_ constraint:

- **Intent** — Every user action is modeled as an explicit event object (in Kotlin,
  typically a `sealed interface`). Instead of the View calling arbitrary ViewModel
  methods, it dispatches a single typed event.

- **Reducer** — A function that takes the current state and an event, and produces a
  new state. This makes state transitions predictable and auditable.

- **Single state object** — All UI state lives in a single `data class` exposed as a
  `StateFlow`. The UI renders from this one source of truth.

**The MVI cycle:**
```
User Action → Event object → Reducer(currentState, event) → New State → UI renders
```

### What is a Sealed Interface?

A **sealed interface** (or **sealed class**) is a Kotlin feature that restricts which
classes can implement the interface. The compiler knows all possible subtypes at compile
time, which means `when` expressions over sealed types are _exhaustive_ — the compiler
warns you if you forget a case.

```kotlin
sealed interface TrackerEvent {
    data class DayTapped(val date: LocalDate) : TrackerEvent
    data class PeriodMarkDay(val date: LocalDate) : TrackerEvent
    data class PeriodRangeDragged(val anchorDate: LocalDate, val releaseDate: LocalDate) : TrackerEvent
    object ScreenEntered : TrackerEvent
    // ... more events
}
```

Sealed types are ideal for events (every possible user action is enumerated) and for
state hierarchies (every possible insight type is enumerated). They make the code
self-documenting: you can look at the sealed interface and immediately see every
possible event/state.

### How RhythmWise Uses a Hybrid Approach

RhythmWise ViewModels use a **MVI-inspired** unidirectional data flow pattern built on
top of MVVM components:

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
- **Reducer** (`reduce()`) — A pure function that takes current state + event and returns
  new state with no side effects. All ViewModels use a pure `reduce()` function.
  `SettingsViewModel` uses five reduce functions — `reduceSecurity()`, `reduceAppearance()`,
  `reduceColors()`, `reduceNotification()`, `reduceAbout()` — one per sub-state flow. Side effects
  (repository writes, scheduler calls, navigation) are launched in `onEvent()` after
  the state update.

### Concrete Example: TrackerViewModel

`TrackerViewModel` (`ui/tracker/TrackerViewModel.kt`) manages the calendar screen:

**State** (see `ui/tracker/TrackerViewModel.kt` for the full definition):
```kotlin
data class TrackerUiState(
    val periods: List<Period> = emptyList(),
    val dayDetails: Map<LocalDate, CalendarDayInfo> = emptyMap(),
    val showDeleteConfirmation: Boolean = false,
    // ... additional UI fields for sheets, libraries, etc.
) {
    val ongoingPeriod: Period? = periods.find { it.endDate == null }
}
```

**Events** (see `ui/tracker/TrackerEvent.kt` for the full sealed interface):
- `ScreenEntered` — Triggers auto-close of stale periods
- `DayTapped(date)` — Tap a day to view or create a log
- `PeriodMarkDay(date)` — Long-press to toggle period day
- `PeriodRangeDragged(anchorDate, releaseDate)` — Long-press-and-drag to mark/shrink a period range
- `DeletePeriodRequested(periodId)` / `DeletePeriodConfirmed` / `DeletePeriodDismissed` — Delete confirmation flow
- `UnmarkPeriodDayConfirmed(date)` / `UnmarkPeriodRangeConfirmed(dates)` / `UnmarkPeriodDismissed` — Unmark confirmation flow (shown when removing period days that have logged data)
- ... and additional events for sheet dismissal, educational content, etc.

**Dispatch:**
```kotlin
fun onEvent(event: TrackerEvent) {
    _uiState.update { currentState ->
        reduce(currentState, event)
    }
}
```

---

## 1.7 Repository Pattern and Clean Layering

### What is Clean Architecture?

**Clean Architecture** organizes code into concentric layers with a strict
_dependency rule_: **inner layers never know about outer layers**.

```
┌──────────────────────────────────────────────┐
│           UI Layer (outermost)               │
│  Screens, ViewModels, Compose components     │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │        Domain Layer (inner)            │  │
│  │  Use Cases, Repository interfaces,     │  │
│  │  Models, InsightEngine                 │  │
│  │                                        │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │        Data Layer (outer)              │  │
│  │  Repository implementations, DAOs,     │  │
│  │  Room entities, Mappers                │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │    Infrastructure Layer (outermost)    │  │
│  │  Encryption, Storage, Lifecycle        │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

The domain layer (models, use cases, repository _interfaces_) has zero dependencies on
Android, Room, or any framework. It only uses pure Kotlin. This means:
- Business logic can be tested without an emulator
- Business logic can be shared across platforms (Android, iOS)
- Changing the database from Room to something else doesn't touch domain code

### What is the Repository Pattern?

The **Repository Pattern** abstracts data access behind an interface. The rest of the
app only sees the interface — never the concrete implementation (Room, SQLite, an API,
a file, etc.).

**What problem does it solve?**
- **Abstraction** — ViewModels and use cases don't know whether data comes from Room,
  a network API, or an in-memory cache.
- **Testability** — Unit tests pass a mock repository. No real database needed.
- **Swappability** — Changing the storage mechanism (e.g., Room → Core Data for iOS)
  requires only a new implementation class.

**Interface vs Implementation:**
```kotlin
// In shared/ (domain layer) — the contract
interface PeriodRepository {
    fun getAllPeriods(): Flow<List<Period>>
    suspend fun logPeriodDay(date: LocalDate)
    // ... see PeriodRepository.kt for the full contract
}

// In composeApp/ (data layer) — one possible implementation
class RoomPeriodRepository(
    private val db: PeriodDatabase,
    private val periodDao: PeriodDao,
    // ... remaining DAOs
) : PeriodRepository {
    override fun getAllPeriods(): Flow<List<Period>> = // Room query
    override suspend fun logPeriodDay(date: LocalDate) = // Room transaction
}
```

### What is a Use Case?

A **use case** (also called an _interactor_) encapsulates a single business operation.
It coordinates repository calls and enforces business rules. Each use case has a single
public method using Kotlin's `operator fun invoke()` convention, which lets you call
the use case like a function:

```kotlin
class AutoCloseOngoingPeriodUseCase(
    private val repository: PeriodRepository
) {
    suspend operator fun invoke() {
        // Find stale ongoing periods and close them
    }
}

// Called like a function:
autoClosePeriodUseCase()  // equivalent to autoClosePeriodUseCase.invoke()
```

### Kotlin Flows — Reactive Streams

A **Flow** is Kotlin's way of representing a stream of values that arrive over time.
Think of it like a pipe: values flow through it, and you can observe (collect) them
as they arrive.

- **Cold Flow** — Doesn't produce values until someone collects it. Each collector
  gets its own independent stream. Room's `@Query` methods return cold Flows that
  re-emit whenever the underlying table changes.

- **Hot Flow** — Produces values regardless of collectors. `StateFlow` (used for UI
  state) and `SharedFlow` (used for one-shot effects) are hot Flows.

```kotlin
// Cold Flow from Room — emits current data, then re-emits on every DB change
fun getAllPeriods(): Flow<List<Period>>

// Hot Flow for UI state — always holds the latest state
val uiState: StateFlow<TrackerUiState>
```

**`Flow<T>` vs `suspend fun`:** In the repository:
- **`Flow<T>`** — For data that can change over time (live queries). Backed by Room's
  `@Query` annotations. Emits the current snapshot on subscription and re-emits
  whenever the underlying table changes.
- **`suspend fun`** — For one-shot operations (insert, update, delete). Safe to call
  from any dispatcher (Room handles IO internally).

### The Contract

`PeriodRepository` (`shared/.../domain/repository/PeriodRepository.kt`) is the single
data access contract for the entire app. Its methods are grouped by domain concern:

- **Period CRUD** — e.g., `getAllPeriods()`, `startNewPeriod()`, `deletePeriod()`
- **Period day marking** — `logPeriodDay()`, `unLogPeriodDay()` (4-scenario state machines)
- **Daily log access** — e.g., `getFullLogForDate()`, `saveFullLog()`
- **Calendar observation** — e.g., `observeAllPeriodDays()`, `observeDayDetails()`
- **Symptom library** — e.g., `getSymptomLibrary()`, `createOrGetSymptomInLibrary()`
- **Medication library** — e.g., `getMedicationLibrary()`, `createOrGetMedicationInLibrary()`
- **Water intake** — `upsertWaterIntake()`, `getWaterIntakeForDates()`
- **Tutorial / Debug** — `deleteSeedData()`, `seedDatabaseForDebug()`

See the interface source for the complete method list.

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
│          PeriodDao  DailyEntryDao  ... (one DAO per table)       │
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

1. **`CycleWiseApp.onCreate()`** (`CycleWiseApp.kt`) —
   - Starts Koin with `appModule` (and `allowOverride(false)` to catch DI errors early)
   - Calls `ReminderNotifier.ensureChannel(this)` to create the notification channel
     for medication, hydration, and prediction reminders
   - Initializes autolock SharedPreferences (`"autolock_prefs"`)
   - Begins collecting `autolockMinutes` from `AppSettings` into a `@Volatile` cache
   - Registers itself as a `ProcessLifecycleOwner` observer for autolock

2. **`MainActivity`** (`MainActivity.kt`) — Intentionally minimal:
   - Calls `installSplashScreen()` for the Android 12+ splash screen
   - Calls `enableEdgeToEdge()` for fullscreen content behind system bars
   - Sets `FLAG_SECURE` on the window — blocks screenshots, screen recording,
     and recent-apps thumbnails across all screens
   - Registers a global uncaught exception handler for crash logging
   - Calls `setContent { CycleWiseAppUI() }` — all Compose UI starts here

3. **`CycleWiseAppUI`** (`ui/CycleWiseAppUI.kt`) — The root composable:
   - Wraps the app in `RhythmWiseTheme` for Material 3 theming (light/dark)
   - Sets up the Compose `NavHost` with 6 routes and animated transitions
   - Start destination: `NavRoute.Passphrase`
   - **Bottom navigation is hidden on PassphraseScreen** — only shown after unlock:
     ```kotlin
     if (currentRoute != NavRoute.Passphrase.route) {
         BottomNavBar(navController)
     }
     ```
   - Sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)` on the outer Scaffold — each screen handles its own top insets via its own `Scaffold`/`TopAppBar`
   - On successful unlock, navigates to DailyLogHome and pops Passphrase from the backstack:
     ```kotlin
     PassphraseScreen {
         navController.navigate(NavRoute.DailyLogHome.route) {
             popUpTo(NavRoute.Passphrase.route) { inclusive = true }
         }
     }
     ```

### Navigation Routes

Defined in `ui/nav/NavRoutes.kt`:

```kotlin
sealed class NavRoute(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
) {
    object Passphrase : NavRoute("passphrase", "Pass Phrase")

    object DailyLogHome : NavRoute(
        route = "daily_log_home",
        label = "Daily Log",
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote,
    )

    object Tracker : NavRoute(
        route = "tracker",
        label = "Tracker",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
    )

    object Insights : NavRoute(
        route = "insights",
        label = "Insights",
        selectedIcon = Icons.Filled.Insights,
        unselectedIcon = Icons.Outlined.Insights,
    )

    object Settings : NavRoute(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )

    object DailyLog : NavRoute("log/{date}", "Daily Log") {
        fun createRoute(date: LocalDate) = "log/$date"
    }

    companion object {
        val all: List<NavRoute>
            get() = listOf(DailyLogHome, Tracker, Insights, Settings)
    }
}
```

Bottom navigation shows four tabs: **Daily Log** (home), **Tracker**, **Insights**,
**Settings**. Each tab route carries `selectedIcon` (filled variant, shown when active)
and `unselectedIcon` (outlined variant, shown when inactive) for visual feedback in the
navigation bar.

### DailyLog Navigation Parameters

The `DailyLog` route takes a single `date` path parameter:

```kotlin
composable(
    route = NavRoute.DailyLog.route,
    arguments = listOf(
        navArgument("date") { type = NavType.StringType },          // ISO-8601 date
    )
) { backStackEntry ->
    val dateString = backStackEntry.arguments?.getString("date")
    DailyLogScreen(
        date = LocalDate.parse(dateString),
        onSaveComplete = { navController.popBackStack() },
    )
}
```

`TrackerViewModel` constructs this route via `NavRoute.DailyLog.createRoute(date)`.
The `DailyLogViewModel` self-determines `isPeriodDay` from the repository during init
rather than receiving it as a navigation parameter.

---

## 2.2 PassphraseScreen and Unlock Flow

The passphrase screen is the app's gateway. Nothing database-related exists until the
user enters the correct passphrase and the session scope is created.

### Step-by-Step Walkthrough

Follow along in `ui/auth/PassphraseViewModel.kt`:

**1. User taps Unlock**
```
PassphraseScreen calls viewModel.onEvent(PassphraseEvent.UnlockClicked(passphrase))
→ PassphraseViewModel.unlock(passphrase)
```

**2. Re-entrancy guard**
```kotlin
if (_uiState.value.isUnlocking) return
```
Prevents double-tap from creating duplicate sessions.

**3. Set isUnlocking state**
```kotlin
_uiState.update { it.copy(isUnlocking = true) }
```
The UI shows a loading indicator.

**4. Check prepopulation status**
```kotlin
val needsPrepopulation = !appSettings.isPrepopulated.first()
```
Reads the DataStore flag _before_ entering the IO dispatcher to avoid blocking.

**5. Delegate to SessionManager.openSession(passphrase)**
```kotlin
sessionManager.openSession(passphrase)
```

`SessionManager.openSession()` runs entirely on `Dispatchers.IO` and performs these
steps internally:

> **Important behavioral detail:** The scope is _always_ closed first, then a fresh
> scope is _always_ created. This is **not** a "reuse if exists" pattern — it is a
> "destroy and recreate" pattern. This ensures that every unlock attempt performs full
> passphrase validation against a freshly constructed database, preventing a stale
> (already-open) database from being silently reused with a different passphrase.

**6. Close stale scope and create fresh scope** (inside `SessionManager.openSession`)
```kotlin
koin.getScopeOrNull("session")?.close()
val sessionScope = koin.createScope(scopeId = "session", qualifier = SESSION_SCOPE)
```

**7. Get PeriodDatabase (triggers key derivation)**
```kotlin
val db = sessionScope.get<PeriodDatabase> { parametersOf(passphrase) }
```
Inside the scope definition, this triggers:
- `createDatabaseAndZeroizeKey()` — calls `PassphraseServiceAndroid`
- `SaltStorage.getOrCreateSalt()` — returns or generates 16-byte salt
- Argon2id KDF — 64 MB memory, 3 iterations, parallelism 1 → 32-byte key
- `PeriodDatabase.create(context, key.copyOf())` — builds Room with `SupportFactory`
- `key.fill(0)` — immediately zeros the original key

**8. Force-open the database**
```kotlin
db.openHelper.writableDatabase
```
If the passphrase is wrong, SQLCipher throws here. The exception is caught below.

**9. Prepopulation**
```kotlin
if (needsPrepopulation) {
    repository.prepopulateSymptomLibrary()
    appSettings.setPrepopulated(true)
}
repository.prepopulateMedicationLibrary()
```
On first unlock, inserts 20 default symptoms (Cramps, Headache, Bloating, etc.).
Medication prepopulation runs unconditionally on every unlock — `INSERT IGNORE`
makes it idempotent, so existing users who predated the medication library still
get it populated.

**10. Sync water drafts**
```kotlin
val syncer = WaterDraftSyncer(lockedWaterDraft, repository)
syncer.sync()
```
Any water intake logged on the lock screen (via `LockedWaterDraft`) is persisted
into the database. `WaterDraftSyncer` (`ui/auth/WaterDraftSyncer.kt`) applies these
rules:
- **Today's draft is skipped** — the user may still be editing it
- **Higher-wins merge** — a draft is written to the DB only if its cup count exceeds
  the existing DB value for that date
- **Synced dates are cleared** from the draft store
- **Individual failures are logged** but don't abort the remaining sync

**11. Navigate to DailyLogHome** (back in `PassphraseViewModel`)
```kotlin
_effect.emit(PassphraseEffect.NavigateToTracker)
```
The UI observes this effect and navigates to `NavRoute.DailyLogHome`, popping Passphrase
from the backstack. (The effect name retains `Tracker` for historical reasons.)

### Error Path

```kotlin
} catch (e: Exception) {
    Log.e("PassphraseUnlock", "Unlock failed: ${e.message}")
    sessionManager.closeSession()
    _effect.emit(PassphraseEffect.ShowError("Failed to unlock. Wrong passphrase?"))
} finally {
    _uiState.update { it.copy(isUnlocking = false) }
}
```

On failure, the session scope is closed immediately via `sessionManager.closeSession()`
(destroying the partially-created database), and an error message is shown to the user.

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
│  │    PassphraseScreen     │◄───────────────────────┐           │
│  │   (no DB, no session)   │                        │           │
│  └───────────┬─────────────┘                        │           │
│              │ correct passphrase                   │           │
│              ▼                                      │           │
│  ┌─────────────────────────┐              session   │           │
│  │  Koin Session Scope     │              .close()  │           │
│  │  CREATED                │                  │     │           │
│  │  ├─ PeriodDatabase      │                  │     │           │
│  │  ├─ DAOs (one per table)│                  │     │           │
│  │  ├─ Repository          │                  │     │           │
│  │  ├─ Use Cases           │                  │     │           │
│  │  └─ ViewModels          │                  │     │           │
│  └───────────┬─────────────┘                  │     │           │
│              │                                │     │           │
│              ▼                                │     │           │
│  ┌─────────────────────────┐                  │     │           │
│  │    Normal Usage         │                  │     │           │
│  │  Tracker / Log /        │                  │     │           │
│  │  Insights / Settings    │──── manual ──────┘     │           │
│  └───────────┬─────────────┘     lock               │           │
│              │                                      │           │
│              │ app backgrounded                     │           │
│              ▼                                      │           │
│  ┌─────────────────────────┐                        │           │
│  │  Background Timer       │                        │           │
│  │  ON_STOP: record time   │                        │           │
│  │  ON_START: check        │── timeout exceeded ────┘           │
│  │    elapsed vs threshold │                                    │
│  └─────────────────────────┘                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Autolock Mechanism

`CycleWiseApp` (`CycleWiseApp.kt`) implements `LifecycleEventObserver` and watches
for process-level lifecycle events:

**ON_STOP (app backgrounds):**
```kotlin
Lifecycle.Event.ON_STOP -> {
    prefs.edit {
        putLong(KEY_LAST_BG_AT_ELAPSED, SystemClock.elapsedRealtime())
    }
}
```

**ON_START (app foregrounds):**
```kotlin
Lifecycle.Event.ON_START -> {
    val minutes = autolockMinutesCache
    val last = prefs.getLong(KEY_LAST_BG_AT_ELAPSED, -1L)

    if (shouldLockNow(minutes, last)) {
        sessionManager.closeSession()
        prefs.edit { remove(KEY_LAST_BG_AT_ELAPSED) }
        sessionBus.emitLogout()
    }
}
```

**Lock decision logic:**
```kotlin
private fun shouldLockNow(minutes: Int, lastBgAtElapsed: Long): Boolean {
    if (minutes == 0) return true                    // Always lock immediately
    if (lastBgAtElapsed <= 0L) return false          // No background timestamp
    val elapsedMs = SystemClock.elapsedRealtime() - lastBgAtElapsed
    val thresholdMs = minutes * 60_000L
    return elapsedMs >= thresholdMs
}
```

### Manual Lock

From `SettingsScreen`, the user can tap "Lock Now":

```kotlin
Button(
    enabled = isSessionActive,
    onClick = {
        sessionManager.closeSession()                       // Destroy all session-scoped objects
        navController.navigate(NavRoute.Passphrase.route) {
            popUpTo(0) { inclusive = true }                  // Clear entire backstack
        }
    }
) { Text(stringResource(R.string.settings_lock_now)) }
```

Both steps are required: `sessionManager.closeSession()` destroys the database and all
session-scoped objects (security), while `navController.navigate` with `popUpTo(0)` brings
the user back to the passphrase screen (UX). If you only close the scope without navigating,
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
component to observe logout signals by collecting `sessionBus.logout`.

---

## 2.4 TrackerScreen and TrackerViewModel

### Initialization

`TrackerViewModel` (`ui/tracker/TrackerViewModel.kt`) takes 6 constructor parameters:
`periodRepository`, `symptomLibraryProvider`, `medicationLibraryProvider`,
`autoClosePeriodUseCase`, `appSettings`, and `educationalContentProvider`.

It starts 4 Flow collectors in its `init` block:

```kotlin
init {
    // 1. Day details (calendar dots, period highlighting, cycle phases)
    viewModelScope.launch {
        periodRepository.observeDayDetails()
            .map { domainMap -> domainMap.mapValues { (_, d) ->
                CalendarDayInfo(
                    isPeriodDay = d.isPeriodDay,
                    hasSymptoms = d.hasLoggedSymptoms,
                    hasMedications = d.hasLoggedMedications,
                    hasNotes = d.hasNotes,
                    cyclePhase = d.cyclePhase
                )
            }}
            .collect { _uiState.update { it.copy(dayDetails = uiReadyDetailsMap) } }
    }

    // 2. All periods (also triggers prediction cache update)
    viewModelScope.launch {
        periodRepository.getAllPeriods().collect { periods ->
            _uiState.update { it.copy(periods = periods) }
            updatePredictionCache(periods)
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

`CalendarDayInfo` holds 5 fields: `isPeriodDay`, `hasSymptoms`, `hasMedications`,
`hasNotes`, and `cyclePhase`. It mirrors `DayDetails` from the domain layer (which
also has 5 matching fields) but lives in the UI layer.

### Prediction Cache

`updatePredictionCache()` is called every time the periods list changes. It computes the
predicted next period date (using the same average-cycle-length algorithm as the
`InsightEngine`) and caches it in `AppSettings` (plaintext DataStore). This allows the
`PeriodPredictionWorker` to access the prediction date for reminder notifications
_without_ unlocking the encrypted database.

### Event Handling

When the user interacts with the tracker:

- **`ScreenEntered`** — Calls `autoClosePeriodUseCase()` to close any stale ongoing
  period. Fired as a `LaunchedEffect` when the screen appears.
- **`DayTapped(date)`** — If a log exists for that date, shows a bottom sheet summary.
  If not, navigates to `DailyLogScreen` to create one.
- **`PeriodMarkDay(date)`** — Long-press toggle. If the date is inside a period,
  checks whether the day's `PeriodLog` has user-entered data (`hasData()`). If it
  does, shows a confirmation dialog (`UnmarkPeriodDayConfirmationDialog`). If not,
  calls `unLogPeriodDay()` silently. If the date is outside all periods, calls
  `logPeriodDay()`.
- **Unmark confirmation flow** — `UnmarkPeriodDayConfirmed(date)` calls
  `unLogPeriodDay()`. `UnmarkPeriodRangeConfirmed(dates)` calls `unLogPeriodDay()`
  for each date. `UnmarkPeriodDismissed` resets dialog state without side effects.
- **Delete flow** — `DeletePeriodRequested` shows a confirmation dialog.
  `DeletePeriodConfirmed` calls `periodRepository.deletePeriod()`.

### Long-Press-and-Drag Period Selection

The calendar grid supports a long-press-and-drag gesture for marking or shrinking
period ranges in a single interaction.

**DayBoundsRegistry:** A lightweight coordinate registry (`ui/tracker/DayBoundsRegistry.kt`)
that maps each calendar cell's screen bounds (`Rect`) to its `LocalDate`. Each cell
registers its bounds via `register(date, rect)` during layout and unregisters on disposal.
The gesture detector calls `dateAt(offset)` to convert pointer positions to dates.

**Gesture detection:** The calendar grid wraps in a `pointerInput` modifier that detects
long-press, then tracks drag position. On release:
- If the drag stayed on the anchor date → dispatches `PeriodMarkDay` (toggle single day)
- If the drag moved to a different date → dispatches `PeriodRangeDragged(anchor, release)`

**Shrink vs Expand logic in TrackerViewModel:** When processing `PeriodRangeDragged`:
- **Shrink from start:** When anchor equals the start date of an existing period and
  release is inside the period → collects the days from anchor up to (but not
  including) release. If any of those days have `PeriodLog` data (`hasData()`),
  shows `UnmarkPeriodRangeConfirmationDialog` with counts. If none have data,
  calls `unLogPeriodDay()` for each day silently.
- **Shrink from end:** When anchor equals the end date of an existing period and
  release is inside the period → same data-check logic as shrink-from-start,
  applied to the days from anchor down to (but not including) release.
- **Default (expand/mark):** Otherwise → calls `logPeriodDay()` for every day in the
  anchor-to-release range (inclusive).

**CyclePhaseColors:** Object (`ui/tracker/CyclePhaseColors.kt`) holding default hex color
constants for the four cycle phases (menstruation, follicular, ovulation, luteal). Used
by `TrackerScreen` for calendar cell coloring and by `SettingsViewModel` for default
values and reset-to-defaults functionality.

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
transaction. After the scenario branch, it ensures a `PeriodLog` exists for the
date with null `flowIntensity` if no log was already present:

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
│  │  FullDailyLog, CyclePhase                                │   │
│  │  DayDetails, etc.                                        │   │
│  │                                                          │   │
│  │  domain/usecases/        domain/services/                │   │
│  │  StartNewPeriodUseCase   PassphraseService (interface)   │   │
│  │  EndPeriodUseCase                                        │   │
│  │  GetOrCreateDaily...     domain/insights/                │   │
│  │  AutoCloseOngoing...     InsightEngine                   │   │
│  │  DebugSeederUseCase      generators/                     │   │
│  │  TutorialSeeder/Cleanup                                  │   │
│  │                                                          │   │
│  │  domain/providers/       Platform.kt                     │   │
│  │  SymptomLibraryProvider  expect fun getPlatform()        │   │
│  │  MedicationLibProvider   expect val num                  │   │
│  │  EducationalContent...                                   │   │
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
│  │  Room DAOs (one per table)    SettingsScreen             │   │
│  │                               PassphraseScreen           │   │
│  │  androidData/local/database/                             │   │
│  │  PeriodDatabase               di/AppModule.kt            │   │
│  │  migrations/                                              │   │
│  │                               services/                  │   │
│  │  androidData/local/entities/  PassphraseServiceAndroid    │   │
│  │  Room entities (one per table) SaltStorage               │   │
│  │  Converters, Mappers                                     │   │
│  │                               reminders/                 │   │
│  │  ui/theme/                    ReminderScheduler           │   │
│  │  Color, Theme, Dimensions     ReminderNotifier            │   │
│  │  Shape, Type                  3 Workers                   │   │
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
in the codebase. See `DailyLogEvent.kt` for the full sealed interface.

### Navigation Routes

The daily log has two navigation entry points:
- **DailyLogHome** — Navigated to from the passphrase screen after unlock, shows today's date.
- **DailyLog(date)** — Navigated from the tracker with a specific date parameter.

### Swipeable Pager Layout

`DailyLogScreen` uses a 5-page `HorizontalPager` with `ScrollableTabRow` navigation:
1. **Wellness** — Mood (star rating), energy (star rating), libido (star rating), water intake
2. **Period** — Period toggle switch, flow intensity, period color, period consistency
3. **Symptoms** — Symptom library chips with create-and-add, long-press to rename/delete
4. **Medications** — Medication library chips with create-and-add, long-press to rename/delete
5. **Notes/Tags** — Custom tags with add/remove, free-text note editor

### Two-Phase Initialization

```kotlin
init {
    // Phase 1: Load initial data, then dispatch a single comprehensive event
    viewModelScope.launch {
        val initialSymptoms = symptomLibraryProvider.symptoms.first()
        val initialMedications = medicationLibraryProvider.medications.first()
        val result = getOrCreateDailyLog(entryDate)
        val waterIntake = periodRepository.getWaterIntakeForDates(listOf(entryDate)).firstOrNull()

        // Self-determine isPeriodDay from existing periods
        val periods = periodRepository.getAllPeriods().first()
        val isPeriodDay = periods.any { period ->
            val end = period.endDate
            entryDate >= period.startDate && (end == null || entryDate <= end)
        }

        onEvent(DailyLogEvent.LogLoaded(result, initialSymptoms, initialMedications))
        _uiState.update { it.copy(waterCups = waterIntake?.cups ?: 0, isPeriodDay = isPeriodDay) }
    }

    // Phase 2: Subscribe to library changes as events (not direct state mutations)
    symptomLibraryProvider.symptoms
        .onEach { symptoms -> onEvent(DailyLogEvent.LibraryUpdated(symptoms, ...)) }
        .launchIn(viewModelScope)
}
```

This pattern ensures all state changes flow through the reducer, even for
initialization. Library updates arrive as events, making the data flow auditable.
The ViewModel self-determines `isPeriodDay` by querying the repository during init,
rather than relying on a navigation parameter.

### Event Types

All events are defined in `ui/log/DailyLogEvent.kt` as a sealed interface. Each event
corresponds to a single user action in the daily log — field changes (e.g.,
`FlowIntensityChanged`, `MoodScoreChanged`), library interactions (e.g.,
`SymptomToggled`, `CreateAndAddSymptom`), library editing (e.g.,
`SymptomLongPressed`, `RenameSymptomConfirmed`, `DeleteSymptomConfirmed` and
medication equivalents), period toggling (`PeriodToggled`), water tracking
(`WaterIncrement` / `WaterDecrement`), and initialization events (`LogLoaded`,
`LibraryUpdated`). See `DailyLogEvent.kt` for the complete sealed interface.

> **PeriodLog lifecycle:** The `PeriodLog` is created (with null `flowIntensity`) by
> `PeriodToggled` and deleted when the user toggles the period OFF. If the existing
> `PeriodLog` has user-entered data (`hasData()` — flow, color, or consistency),
> toggling OFF shows `UnmarkPeriodDayDialog` for confirmation instead of deleting
> immediately. `FlowIntensityChanged` only updates the flow field on an existing
> `PeriodLog` — it no longer creates or deletes one. This allows users to log period
> attributes (color, consistency) without first selecting a flow level.

### The Pure Reducer

All `reduce()` branches are pure — they return updated state with no side effects.
Side effects (auto-save, repository writes, library creation, water persistence) are
launched in `onEvent()` after the state update.

| Event | Pure state return? | Async side effect? |
|-------|-------------------|-------------------|
| `FlowIntensityChanged` | Yes — updates flow field on existing PeriodLog | No |
| `MoodScoreChanged` | Yes | No |
| `SymptomToggled` | Yes | No |
| `CreateAndAddSymptom` | Returns `currentState` | Yes — creates symptom in library, then updates state |
| `PeriodToggled` | Yes — creates PeriodLog (null flow), deletes it, or shows unmark confirmation if it has data | Yes — calls `logPeriodDay` or `unLogPeriodDay` (unless data check triggers dialog) |
| `UnmarkPeriodConfirmed` | Yes — nulls periodLog, clears confirmation flag | Yes — calls `unLogPeriodDay`, auto-saves |
| `UnmarkPeriodDismissed` | Yes — clears confirmation flag | No |
| `WaterIncrement` | Yes (optimistic) | Yes — upserts water intake to DB |
| `SymptomLongPressed` | Yes — sets `symptomForContextMenu` | No |
| `RenameSymptomConfirmed` | Returns `currentState` | Yes — calls `RenameSymptomUseCase`, updates state on result |
| `DeleteSymptomConfirmed` | Returns `currentState` | Yes — calls `DeleteSymptomUseCase`, filters in-memory logs |
| `LibraryUpdated` | Yes — updates `symptomLibrary` / `medicationLibrary` | No |

### Auto-Save

The explicit `SaveLog` event has been replaced by automatic persistence. Every
state-changing user event triggers `autoSave()`, which persists the current log if
it contains any user data. The `NoteChanged` event uses `debouncedAutoSave()` with
a 500ms delay to avoid excessive repository writes during typing.

### Empty Log Detection

The `autoSave()` method checks if the log contains any user-entered data before
persisting. If it's empty (no mood, no symptoms, no medications, no notes, no tags),
the save is skipped:

```kotlin
private fun isLogEmpty(log: FullDailyLog): Boolean {
    val entry = log.entry
    return log.periodLog == null &&
            log.symptomLogs.isEmpty() &&
            log.medicationLogs.isEmpty() &&
            entry.moodScore == null &&
            entry.energyLevel == null &&
            entry.libidoScore == null &&
            entry.customTags.isEmpty() &&
            entry.note.isNullOrBlank()
}
```

---

## 2.8 InsightsScreen and InsightsViewModel

`InsightsViewModel` (`ui/insights/InsightsViewModel.kt`) generates insights on
initialization and supports pull-to-refresh for regeneration.

### Constructor

The ViewModel receives its dependencies via Koin injection. See `InsightsViewModel.kt`
for the current constructor parameters. Key dependency: `insightEngine` is injected
by Koin (registered as a `factory` in `AppModule.kt`), not instantiated inline.

### Generation Pipeline

On init and on pull-to-refresh, `loadInsights()` runs this pipeline:

1. **Fetch** — Collects all data in one shot (`Flow.first()` for periods, logs,
   symptom library, water intakes, medication library, top-symptoms count).
2. **Generate** — Calls `insightEngine.generateInsights(...)` which returns
   `List<ScoredInsight>` (insights wrapped with relevance scores).
3. **Format** — Applies platform-specific date formatting via
   `toLocalizedDateString()` on insights that contain dates (e.g.,
   `NextPeriodPrediction`, `SymptomPhasePattern`). This keeps generators in `shared/`
   free of Android locale APIs.
4. **Emit** — Updates `_uiState` with the formatted, scored insights.

See `InsightsViewModel.kt` for the full implementation.

### Pull-to-Refresh

The `InsightsUiState` has two loading flags:
- `isLoading` — Set during the initial load; the UI shows a full-screen spinner.
- `isRefreshing` — Set during pull-to-refresh; existing content stays visible while
  new data loads in the background.

The `refresh()` method is called from the UI's `PullToRefreshBox` composable.

### Why Post-Process Formatting?

Insight generators in `shared/` are pure Kotlin — they have no access to Android's
`DateFormat` or locale APIs. The `InsightsViewModel` applies platform-specific
formatting (via `toLocalizedDateString()`) _after_ generation. This keeps generators
portable to iOS without modification.

---

## 2.9 SettingsScreen and SettingsViewModel

`SettingsViewModel` (`ui/settings/SettingsViewModel.kt`) manages all user preferences
using the MVI pattern. It is registered at the **module root** (outside the session
scope) as a `viewModel` (no database dependency). See `SettingsViewModel.kt` for its
current constructor parameters.

### Architecture

- **State:** Five focused sub-state data classes — one per pager page — each exposed
  as a separate `StateFlow` to limit recomposition scope:
  - `SecuritySettingsState` — autolock, passphrase change dialog, and
    delete-all-data confirmation flow.
  - `AppearanceSettingsState` — theme mode, summary toggles, phase visibility,
    and top symptoms count.
  - `ColorsSettingsState` — phase colors, heatmap colors, and educational
    bottom sheet.
  - `NotificationSettingsState` — period, medication, and hydration reminder
    configuration plus permission rationale and privacy dialog.
  - `AboutSettingsState` — about dialog, legal dialogs, and tutorial hint reset.
- **Events:** `SettingsEvent` — a sealed interface covering every toggle, slider,
  color input, and dialog interaction.
- **Event routing:** `onEvent()` applies a pure state update via the appropriate
  reduce function — `reduceSecurity()`, `reduceAppearance()`, `reduceColors()`,
  `reduceNotification()`, or `reduceAbout()` — then launches side effects (DataStore
  writes, `ReminderScheduler` calls) in `viewModelScope`.

### Initialization Pattern

The init block uses individual `Flow.onEach { }.launchIn(viewModelScope)` collectors
— one per `AppSettings` preference flow — routing each to the appropriate sub-state
flow. This intentionally avoids `combine()` because Kotlin's `combine()` supports a
maximum of 5 parameters without custom extensions, and there are far more settings
flows to observe.

### 5-Page Swipeable Pager

`SettingsScreen` uses a `HorizontalPager` with 5 pages:
1. **Security** — Autolock timeout, change passphrase, lock now, delete all data
2. **Appearance** — Theme mode, summary visibility toggles (mood, energy, libido),
   phase visibility toggles (`PhaseVisibilitySettings`), top symptoms count
3. **Colors** — Phase color customization (`PhaseColorSettings`) with hex input and
   preset swatch grid, heatmap color customization (`HeatmapColorSettings`),
   educational bottom sheet
4. **Notifications** — Period prediction, medication, and hydration reminder
   configuration (`ReminderSettings`) with POST_NOTIFICATIONS permission handling
5. **About** — App version, about dialog, legal documents (privacy policy, terms of
   service), reset tutorial hints, developer tools (debug only)

### Phase Color Customization

`PhaseColorSettings` (`ui/settings/PhaseColorSettings.kt`) provides:
- Four color rows (Menstruation, Follicular, Ovulation, Luteal)
- Hex string input for each phase color
- Preset color swatch grid with 10 colors
- "Reset to Defaults" button using constants from `CyclePhaseColors`
- Colors stored in `AppSettings` as 6-character hex strings

### Reminder Configuration

`ReminderSettings` (`ui/settings/ReminderSettings.kt`) handles:
- **Period prediction reminders:** Toggle + days-before selection (1–3)
- **Daily medication reminders:** Toggle + hour/minute configuration
- **Hydration reminders:** Toggle + goal cups, frequency (2h/3h/4h), active window
  (start/end hour)
- POST_NOTIFICATIONS permission request at the composable level (requires
  `@Composable` context for `rememberPermissionState`)
- Privacy dialog (`PeriodPrivacyDialog`) for period reminder consent

### Lock Now

The "Lock Now" action is handled at the screen level via `SessionManager.closeSession()`.
The `SettingsScreen` composable obtains `SessionManager` from Koin and calls
`sessionManager.closeSession()` followed by navigation to the passphrase screen.

---

## 2.10 LockedWaterDraft — Pre-Authentication Water Tracking

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

## 2.11 Coach Mark / Tutorial System

RhythmWise includes a post-login guided walkthrough that highlights UI elements with
spotlight overlays and tooltip-style hints. The system lives in `ui/coachmark/` and
coordinates with two domain use cases.

### Architecture

```
HintKey (enum)
    │
    ▼
CoachMarkDef (data class)
├── key: HintKey
├── titleRes / bodyRes (string resources)
├── nextKey: HintKey?          ← links to the next step in the chain
├── skipTargetKey: HintKey?    ← optional jump-ahead target
└── skipToastRes: Int?
    │
    ▼
CoachMarkState (composition-scoped state holder)
├── registerTarget(key, bounds)  ← target composables report screen position
├── showHint(def)                ← activate a hint (immediate or pending)
├── advanceOrDismiss(allDefs)    ← mark seen, resolve nextKey, show next or clear
├── skipToKey(targetKey, allDefs)← walk chain marking intermediates seen
└── skipAll(allDefs)             ← mark everything seen and clear overlay
    │
    ▼
HintPreferences (DataStore)
├── isHintSeen(key): Flow<Boolean>
├── markHintSeen(key)
└── resetAll()
```

### Hint Chain

Each `CoachMarkDef` carries a `nextKey: HintKey?` that points to the next step.
When the user taps "Next", `CoachMarkState.advanceOrDismiss()` resolves `nextKey`
against the full definition map, marks the current hint as seen via
`HintPreferences.markHintSeen()`, and activates the next hint.

The hints are split across two screens — a Daily Log walkthrough and a Tracker
walkthrough. Each chain starts with a `_WELCOME` hint and proceeds through the
screen's key features. See the `HintKey` enum in `CoachMarkDef.kt` for the
full list of hints and their chain order.

### Tutorial Seed Data

Two domain use cases support the walkthrough:

- **`TutorialSeederUseCase`** — seeds demo periods, daily entries, symptoms, and water
  intake so the walkthrough has data to highlight. Returns a `SeedManifest` tracking
  exact IDs of all seeded records.
- **`TutorialCleanupUseCase`** — deletes all seeded data by exact IDs from the
  `SeedManifest` after the walkthrough completes, so the user starts with a clean slate.

Both use cases live in `shared/src/commonMain/.../domain/usecases/` and operate through
`PeriodRepository.deleteSeedData()`.

### Integration Points

- `DailyLogScreen` and `TrackerScreen` each define their walkthrough hints as local
  `CoachMarkDef` lists and register target composables with `CoachMarkState`.
- `CoachMarkOverlay` renders the full-screen dimmed overlay with a spotlight cutout
  around the target bounds and a tooltip with title, body, and action buttons.
- `HintPreferences` is a singleton (separate DataStore from `AppSettings`) so hint
  state persists across sessions.

---

## 2.12 Educational Content System

RhythmWise provides in-app educational articles about cycle health, symptoms, and
wellness. The content is bundled as a JSON asset and displayed via bottom sheets.

### Data Flow

```
res/raw/educational_content.json
        │
        ▼
EducationalContentLoader (composeApp — Android object)
├── load(context): List<EducationalArticle>
└── parseJson(jsonText): List<EducationalArticle>
        │
        ▼
EducationalContentProvider (shared — interface)
├── getByCategory(category: ArticleCategory)
├── getByTag(tag: String)
└── getById(id: String)
        │
        ▼
StaticEducationalContentProvider (composeApp — class)
├── implements EducationalContentProvider
└── sorts articles by sortOrder at construction
        │
        ▼
TrackerViewModel.educationalContentProvider
        │
        ▼
TrackerUiState.educationalArticles
        │
        ▼
InfoButton → ShowEducationalSheet(contentTag) → EducationalBottomSheet
```

### Domain Model

`EducationalArticle` (`shared/.../domain/models/EducationalArticle.kt`) is a
`@Serializable` data class:

```kotlin
data class EducationalArticle(
    val id: String,
    val title: String,
    val body: String,
    val category: ArticleCategory,
    val contentTags: List<String>,
    val sourceName: String,
    val sourceUrl: String,
    val sortOrder: Int,
)
```

`ArticleCategory` has four values: `CYCLE_BASICS`, `SYMPTOMS`, `WELLNESS`,
`WHEN_TO_SEE_A_DOCTOR`.

### UI Components

- **`InfoButton`** — A tappable info icon that dispatches `ShowEducationalSheet(contentTag)`.
- **`EducationalBottomSheet`** — Renders the article body using `MarkdownText`,
  with `SourceAttribution` and `MedicalDisclaimer` at the bottom.

---

## 2.13 Reusable UI Components

The `ui/components/` package contains shared composables used across multiple screens:

| Component | Location | Purpose |
|-----------|----------|---------|
| `EducationalBottomSheet` | `ui/components/` | Modal bottom sheet for displaying educational articles |
| `InfoButton` | `ui/components/` | Small info icon button that triggers educational content display |
| `MarkdownText` | `ui/components/` | Renders markdown-formatted text in Compose (used for article bodies) |
| `MedicalDisclaimer` | `ui/components/` | Standard disclaimer banner: "This is not medical advice" |
| `SourceAttribution` | `ui/components/` | Displays source name and URL for educational content |
| `LibraryChip` | `ui/log/pages/SymptomsPage.kt` | Chip with tap + long-press support for library items (see below) |
| `RenameDialog` | `ui/log/pages/SymptomsPage.kt` | AlertDialog with pre-filled OutlinedTextField for renaming library items |
| `DeleteLibraryItemDialog` | `ui/log/pages/SymptomsPage.kt` | Delete confirmation dialog with log count warning |
| `UnmarkPeriodDayConfirmationDialog` | `ui/tracker/UnmarkPeriodConfirmationDialog.kt` | Confirmation dialog for removing a single period day with logged data |
| `UnmarkPeriodRangeConfirmationDialog` | `ui/tracker/UnmarkPeriodConfirmationDialog.kt` | Confirmation dialog for removing multiple period days via drag-shrink |
| `UnmarkPeriodDayDialog` | `ui/log/UnmarkPeriodDayDialog.kt` | Confirmation dialog for DailyLog period toggle-off with logged data |

These components follow the project convention of one `@Composable` per UI
responsibility and use `stringResource()` for all user-visible text.

### LibraryChip — Long-Press-Aware Chip

`LibraryChip` replaces `FilterChip` for library items that need both tap (toggle)
and long-press (context menu) gestures. It uses `Surface` + `combinedClickable`
directly because **Material3's `FilterChip` internally wraps itself in a `Surface`
with its own clickable modifier, which consumes all pointer events and prevents
`combinedClickable` from ever detecting a long-press** — whether placed on the
chip's modifier or on a parent `Box`.

`LibraryChip` is styled to match `FilterChip` appearance (pill shape, checkmark
when selected, `secondaryContainer` fill, outline border) and sets proper semantics
(`selected`, `role = Checkbox`, `stateDescription`, `customActions`) for TalkBack.

### RenameDialog and DeleteLibraryItemDialog

Both dialogs are defined in `SymptomsPage.kt` and reused by `MedicationsPage.kt`
via direct function call (they are `internal` composables in the same package).

- `RenameDialog`: Pre-fills the current name, validates blank/unchanged input by
  disabling the Save button, and displays inline errors from the ViewModel's
  `renameError` state (e.g., "A symptom with this name already exists").
- `DeleteLibraryItemDialog`: Shows a log count warning ("appears in N daily logs")
  or a no-logs message, with an error-colored confirm button.

---

# Phase 3 — Code Organization and Design Principles

## 3.1 Why Interfaces Live in Shared Module

`PeriodRepository`, `PassphraseService`, and `EducationalContentProvider` are defined
as interfaces in `shared/src/commonMain/`. This is intentional:

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

`RoomPeriodRepository`, `PassphraseServiceAndroid`, and `StaticEducationalContentProvider`
import Android SDK classes (`Context`, Room annotations, BouncyCastle) or are
platform-specific implementations. They cannot exist in `commonMain`.

Koin binds the interface to the implementation inside the session scope:

```kotlin
// di/AppModule.kt, inside the scope(SESSION_SCOPE) block
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
// di/AppModule.kt, singleton declarations
single<PassphraseService> { PassphraseServiceAndroid(get()) }
```

And for the educational content provider at singleton scope:

```kotlin
// di/AppModule.kt, singleton declarations
single<EducationalContentProvider> { StaticEducationalContentProvider(EducationalContentLoader.load(androidContext())) }
```

This pattern means the domain layer only ever sees `PeriodRepository`,
`PassphraseService`, and `EducationalContentProvider`. It never knows about Room,
SQLCipher, Argon2, or the static article loading mechanism.

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
PeriodDatabase.create(context, key.copyOf()) → SupportFactory wraps the copy
        │
        ▼
key.fill(0) — original key zeroed immediately via try/finally
        │
        ▼
db.openHelper.writableDatabase — SupportFactory consumes its copy
        │
        ▼
SupportFactory zeros its own copy (clearPassphrase = true by default)
        │
        ▼
Session scope closes → All references to the database are released.
The database file on disk is unreadable without re-deriving the key.
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

The shared module already compiles for iOS. Check `shared/build.gradle.kts`:

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
- All domain models and enums
- `PeriodRepository` and `PassphraseService` interfaces
- All use cases
- `InsightEngine` and all generators
- All library providers

### What an iOS App Would Need to Build

1. **`PeriodRepository` implementation** — Using Core Data, SQLite, or Realm instead
   of Room.
2. **`PassphraseService` implementation** — Using Apple CryptoKit or a Swift Argon2
   library.
3. **SwiftUI screens** — Passphrase, Tracker (calendar), DailyLog, Insights, Settings.
4. **DI wiring** — Koin works on iOS via `koin-core`, or use a native Swift DI solution.
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
5. Inject into the ViewModel that needs it by adding it as a constructor parameter

### New Screen

Every new screen must use a `Scaffold` with a `TopAppBar`. This is the canonical
pattern — Material3's `TopAppBar` automatically handles status bar insets, so you
never need to think about `statusBarsPadding()` or `WindowInsets` manually.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(viewModel: NewScreenViewModel = koinViewModel()) {
    val dims = LocalDimensions.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_screen_title)) },
            )
        },
    ) { padding ->
        // padding includes status bar (top) automatically
        ContentContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // Screen content here
            }
        }
    }
}
```

**Steps:**

1. Create the Composable in `composeApp/.../ui/newscreen/NewScreen.kt` following the
   template above
2. Create a ViewModel: `NewScreenViewModel.kt` (follow MVI pattern — see §3.8.3)
3. Add a `NavRoute` to `NavRoutes.kt` (with `selectedIcon`/`unselectedIcon` if it
   should appear in bottom navigation)
4. Add the composable to the `NavHost` in `CycleWiseAppUI`
5. Register the ViewModel in `AppModule.kt` (session-scoped if it needs DB access)
6. If it should appear in bottom navigation, add it to `NavRoute.all`

> **Why always `Scaffold` + `TopAppBar`?** The outer `Scaffold` in `CycleWiseAppUI`
> sets `contentWindowInsets = WindowInsets(0, 0, 0, 0)` — it does NOT handle status
> bar insets. Each screen is responsible for its own top insets. Using `Scaffold` +
> `TopAppBar` handles this automatically with no extra code. Some older screens
> (PassphraseScreen, SetupScreen, DailyLogScreen) use `statusBarsPadding()` instead
> because they predate this convention — do not copy that pattern for new screens.

### New Insight Generator

1. Create a class implementing `InsightGenerator` in
   `shared/.../domain/insights/generators/`
2. Define a new `Insight` sealed interface subtype in `Insight.kt` (must include
   `category: InsightCategory`)
3. Add a `when` branch in `InsightCardDispatcher` in `InsightsScreen.kt`
4. Add the generator to the list in `AppModule.kt`'s `InsightEngine` factory registration

### Library Item Edit/Delete Workflow

The symptom and medication libraries support rename and delete via long-press context
menus. This pattern is reusable for any future library-style feature. The data flow:

```
UI (long-press chip) → ViewModel event → reduce() shows context menu
UI (tap Rename/Delete) → ViewModel event → reduce() shows dialog
UI (confirm) → ViewModel event → onEvent() side effect → UseCase → Repository → DAO
                                                              ↓
                                              Room Flow auto-emits updated library
                                                              ↓
                                              LibraryUpdated event → reduce() updates state
                                                              ↓
                                              UI recomposes with new library
```

**Key implementation details:**

- **Validation via sealed result types:** `RenameSymptomUseCase` and
  `RenameMedicationUseCase` return `RenameResult` (a shared sealed interface with
  `Success`, `BlankName`, `NameAlreadyExists` variants) rather than throwing
  exceptions. The ViewModel maps these to UI state (`renameError` for inline dialog
  errors, or clearing the dialog on success).

- **Delete with cascade:** `Migration_11_12` changed the FK on `symptom_logs` and
  `medication_logs` from `RESTRICT` to `CASCADE`, so deleting a library item
  automatically removes associated log entries at the database level. The ViewModel
  also filters the in-memory `FullDailyLog.symptomLogs` / `medicationLogs` to keep
  the UI state consistent without a full re-fetch.

- **Library reactivity:** The DAO's `getAllSymptoms()` / `getAllMedications()` return
  `Flow`, so Room automatically emits updated lists after `updateName()` or
  `deleteById()`. The ViewModel subscribes to these Flows and dispatches
  `LibraryUpdated` events through `reduce()` to update the chip list.

  > **Gotcha:** The `LibraryUpdated` event must be handled in `reduce()` in **both**
  > the early-return path (before the log is loaded) **and** the normal path (after
  > the log is loaded). A bug where `LibraryUpdated` was grouped with the no-op
  > catch-all in the normal path caused rename/delete to appear non-functional
  > because the updated library was silently discarded.

- **Delete confirmation with log count:** `DeleteSymptomUseCase.getLogCount()` queries
  the database for the number of log entries referencing the item, which is displayed
  in the confirmation dialog to warn users about data loss.

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

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/di/AppModule.kt`

This file is the single source of truth for all dependency injection. Reading order:

- **Top-level:** `SESSION_SCOPE` qualifier definition
- **`createDatabaseAndZeroizeKey()` function:** Derives the AES key, passes
  `key.copyOf()` to `PeriodDatabase.create()`, and zeros the original via `try/finally`
- **Singleton declarations:** SaltStorage, AppSettings, SessionBus,
  PassphraseService, InsightEngine (factory), LockedWaterDraft, ReminderScheduler,
  SessionManager, WaterTrackerViewModel, PassphraseViewModel
- **`InsightEngine`** is a `factory` — a new instance is created each time
  it's requested, not cached. This is because insight generation is stateless.
- **Session scope block** (`scope(SESSION_SCOPE) { ... }`):
  - Database factory — key derivation + Room builder via `createDatabaseAndZeroizeKey`
  - DAO providers (one per table), each delegating to `PeriodDatabase`
  - Repository binding (`PeriodRepository` → `RoomPeriodRepository`)
  - Library providers and use cases
  - Session-scoped ViewModels (Tracker, DailyLog, Insights)

### 2. `ui/auth/PassphraseViewModel.kt` — Unlock Flow

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/auth/PassphraseViewModel.kt`

Key architectural details:
- **Constructor takes** `appSettings: AppSettings` and `sessionManager: SessionManager`
- **Delegates to `SessionManager`** — All Koin scope operations (open, close, passphrase
  change) are centralized in `SessionManager`, a `KoinComponent` for runtime scope management.
  This keeps ViewModels decoupled from the DI framework.
- **Re-entrancy guard** prevents duplicate session creation on double-tap
- **Scope lifecycle:** `SessionManager.openSession()` always closes the existing scope
  first, then creates a fresh one (see [section 2.2](#22-passphrasescreen-and-unlock-flow)
  for the full walkthrough)
- On error, the scope is explicitly closed via `sessionManager.closeSession()` to
  prevent zombie sessions

Note: `SettingsViewModel` also receives `SessionManager` via constructor injection for
session scope closure (passphrase change acknowledgment, data deletion).

### 3. `ui/tracker/TrackerViewModel.kt` — MVI Pattern

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/tracker/TrackerViewModel.kt`

Key architectural details:
- **Constructor** receives its dependencies via Koin injection (repository, library
  providers, use cases, settings, and educational content provider)
- **`init` block:** Multiple collectors demonstrate the reactive data flow pattern. Each
  collector transforms domain data into UI models. The periods collector also triggers
  `updatePredictionCache()` for reminder worker support.
- **`onEvent` → `reduce` dispatch** is the MVI core. `_uiState.update` ensures atomic
  state transitions.
- **`reduce()`** is organized as a `when` expression over all event types. Side effects
  are launched via `viewModelScope.launch` in `onEvent()` after the state update.
- Uses `Clock.System.todayIn(TimeZone.currentSystemDefault())` — uses `kotlin.time.Clock`,
  never `kotlinx.datetime.Clock`.

### 4. `androidData/repository/RoomPeriodRepository.kt` — State Machines

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/repository/RoomPeriodRepository.kt`

One of the largest files in the codebase. Key sections:

- **Constructor:** Takes `db` + one DAO per table (all injected by Koin session scope)
- **`saveFullLog()`:** Uses delete-then-insert transaction semantics
- **`logPeriodDay()`:** The 4-scenario state machine (see [section 2.5](#25-use-cases-to-repository-to-dao))
- **`unLogPeriodDay()`:** The inverse 4-scenario state machine
- **`getPeriodLogForDate()`:** Fetches a single `PeriodLog` for a date (used by unmark data check)
- **`getPeriodLogsForDateRange()`:** Batch fetches `PeriodLog`s for a date range (used by drag-shrink data check)
- **`observeDayDetails()`:** Combines `getAllPeriods()` and `getAllLogs()` into a
  `Map<LocalDate, DayDetails>` — the single source of truth for the calendar UI
- **`seedDatabaseForDebug()`:** Generates 6 cycles of realistic data (varied cycle
  lengths and symptoms)

### 5. `androidData/local/database/PeriodDatabase.kt` — SQLCipher Setup

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/database/PeriodDatabase.kt`

Key architectural details:
- **`version`** in the `@Database` annotation — see `PeriodDatabase.kt` for the current schema version
- **Entity classes** registered in `@Database(entities = [...])` — one per table
- **Abstract DAO accessors** — one per entity (e.g., `periodDao()`, `dailyEntryDao()`)
- **The `create()` companion function:**
  - Takes `Context`, `passphrase: ByteArray`, and optional `dbName`
  - Creates `SupportFactory(passphrase)` — SQLCipher's bridge to Room
  - Registers all migrations from the `migrations/` directory
  - Returns the built database (note: not yet opened — caller must force-open)
  - **Security note:** The caller should pass `key.copyOf()` so the original can be
    zeroed independently (see `createDatabaseAndZeroizeKey`)

### 6. `androidData/local/entities/Converters.kt` — Room Type Converters

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/entities/Converters.kt`

Room requires type converters for non-primitive column types:

| Kotlin Type | SQLite Type | Conversion |
|-------------|-------------|------------|
| `LocalDate` | TEXT | ISO-8601 string (e.g., `"2025-03-15"`) via `toString()` / `parse()` |
| `Instant` | INTEGER | Epoch milliseconds via `toEpochMilliseconds()` / `fromEpochMilliseconds()` |
| `FlowIntensity?` | TEXT (nullable) | Enum `name` string (e.g., `"HEAVY"`) via `valueOf()` |
| `PeriodColor?` | TEXT (nullable) | Enum `name` string via `valueOf()` |
| `PeriodConsistency?` | TEXT (nullable) | Enum `name` string via `valueOf()` |
| `SymptomCategory?` | TEXT (nullable) | Enum `name` string via `valueOf()` |

Important: Nullable enums return `null` if the stored string is `null`. If an enum
value is ever renamed, a migration is required (existing data would break `valueOf()`).

> **Note:** The project uses `kotlin.time.Instant`, not `kotlinx.datetime.Instant`.

### 7. `androidData/local/entities/Mappers.kt` — Entity-Domain Conversion

**File:** `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/entities/Mappers.kt`

Bidirectional extension functions convert between Room entities and domain models.

**Why two representations exist:**
Room entities are annotated with `@Entity`, `@PrimaryKey`, and other Room annotations
that are Android-specific. Domain models in `shared/` are pure Kotlin data classes with
no framework dependencies. The mapper layer translates between these two worlds.

**The Dual ID System:**
```kotlin
// Entity → Domain: uuid becomes the domain "id"
fun PeriodEntity.toDomain(): Period =
    Period(id = uuid, startDate = startDate, endDate = endDate, ...)

// Domain → Entity: id set to 0 so Room auto-generates the internal PK
fun Period.toEntity(): PeriodEntity =
    PeriodEntity(id = 0, uuid = id, startDate = startDate, ...)
```

`PeriodEntity` has an internal auto-increment `id` (for fast Room joins) and a `uuid`
field (exposed to the domain layer). When converting domain → entity, `id` is set to 0
so Room auto-generates it on insert.

**Key patterns:**
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
cycles of synthetic data with varied cycle lengths, symptoms, medications, and mood
scores.

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

The project uses a `CustomTestRunner` for instrumented tests. The custom runner:
- Replaces the default `Application` with `TestCycleWiseApp`
- Stops any prior Koin state (`stopKoin()`)
- Restarts Koin with `allowOverride(true)` and a `testOverridesModule` that can
  replace production singletons

Unit tests that need Android resources (themes, strings) use **Robolectric**:
```kotlin
// composeApp/build.gradle.kts
testOptions {
    unitTests {
        isIncludeAndroidResources = true  // Required for Robolectric
    }
}
```

### CI Pipeline

GitHub Actions runs on every push/PR to `main` or `dev`
(`.github/workflows/test.yml`):

1. **Setup:** Ubuntu, Temurin JDK 17, Gradle
2. **Shared module tests:** `./gradlew :shared:testDebugUnitTest`
3. **composeApp tests:** `./gradlew :composeApp:testDebugUnitTest`
4. **Artifacts:** Test reports uploaded regardless of pass/fail

---

## 4.3 Where to Place New Use Cases

Use cases live in `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/usecases/`.

### Template

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

### Validation Result Types

When a use case can fail with user-correctable errors (e.g., blank input, duplicate
name), prefer returning a sealed interface over throwing exceptions. This keeps the
ViewModel's `when` handling exhaustive and avoids try/catch boilerplate:

```kotlin
// RenameResult.kt — shared by RenameSymptomUseCase and RenameMedicationUseCase
sealed interface RenameResult {
    data object Success : RenameResult
    data object BlankName : RenameResult
    data object NameAlreadyExists : RenameResult
}
```

The ViewModel maps each variant to a state update (e.g., setting `renameError` for
inline dialog feedback or clearing the dialog on `Success`).

### Registration

Register the use case in `AppModule.kt` inside the session scope:

```kotlin
scope(SESSION_SCOPE) {
    // ... existing registrations ...
    scoped { NewUseCase(get()) }
}
```

---

## 4.4 Where to Place New Repository Methods

1. Add the method signature to `PeriodRepository` in `shared/`
2. Implement it in `RoomPeriodRepository` in `composeApp/`
3. If it needs a new query, add the `@Query` to the appropriate DAO
4. If it modifies the schema, write a migration (see [4.9](#49-database-migrations))

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

**DCO sign-off** is required on every commit.

---

## 4.6 Branching and PR Expectations

### Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable release branch. All code here passes tests. |
| `dev` | Integration branch for feature work. |
| `<issue#>-<type>-<description>` | Individual feature branches, branched from `dev` (e.g. `35-feature-cycle-phase-calculation`). |

### PR Requirements

1. Branch from `dev` (or `main` for hotfixes)
2. Write descriptive PR title using Conventional Commits format
3. All tests must pass
4. Code follows the style guide (`docs/CODE_STYLE.md`)
5. New public API has KDoc documentation
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

### Never Log Stack Traces in Production

Pass only the message, never the full `Throwable`:

```kotlin
// BAD — full stack trace visible in logcat
Log.e("Tag", "Operation failed", exception)

// GOOD — message only, no internal structure exposed
Log.e("Tag", "Operation failed: ${exception.message}")
```

The global crash handler in `MainActivity` is the **only** place that logs a full
`Throwable`.

### No Network Access

The app declares **no INTERNET permission** in the manifest. This is enforced by a
Robolectric unit test (`ManifestPermissionTest`). Never add network calls, analytics
SDKs, crash reporters, or any dependency that requires `android.permission.INTERNET`.

### Screenshot Protection

`MainActivity` sets `FLAG_SECURE` on the window before `setContent`. This blocks
screenshots, screen recording, and recent-apps thumbnails. Do not remove this flag.

---

## 4.8 Common Architectural Mistakes

### 1. Injecting a session-scoped object into a singleton

```kotlin
// WRONG — SomeService is singleton, but PeriodRepository is session-scoped
single { SomeService(get<PeriodRepository>()) }  // Crash: no session scope active
```

### 2. Importing Android SDK in commonMain

```kotlin
// WRONG — in shared/src/commonMain/
import android.content.Context  // Won't compile for iOS
```

### 3. Missing database migration

Every schema change requires an explicit `Migration` object and a version bump.

### 4. Accessing DAOs directly from ViewModel

```kotlin
// WRONG
class TrackerViewModel(private val periodDao: PeriodDao) { ... }

// RIGHT
class TrackerViewModel(private val periodRepository: PeriodRepository) { ... }
```

### 5. Calling db.withTransaction from a ViewModel

```kotlin
// WRONG — ViewModel should not know about db transactions
viewModelScope.launch {
    db.withTransaction { periodDao.insert(...) }
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

### 7. Using kotlinx.datetime.Clock or kotlinx.datetime.Instant

```kotlin
// WRONG — deprecated in this project
import kotlinx.datetime.Clock
val now = Clock.System.now()

// RIGHT
import kotlin.time.Clock
val now = Clock.System.now()
```

### 8. Forgetting to register in session scope

```kotlin
// WRONG — registered as singleton, but needs DB
single { NewService(get<PeriodRepository>()) }

// RIGHT — registered in session scope
scope(SESSION_SCOPE) {
    scoped { NewService(get()) }
}
```

### 9. Double status bar insets from nested Scaffolds

```kotlin
// WRONG — outer Scaffold manually applies system bars, inner TopAppBar adds them again
Scaffold(
    modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues())
) {
    // Inner screen:
    Scaffold(topBar = { TopAppBar(...) }) { ... }  // Double status bar gap!
}

// RIGHT — outer Scaffold delegates insets; each screen handles its own
Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
    // Inner screen with TopAppBar handles status bar automatically
    Scaffold(topBar = { TopAppBar(...) }) { ... }
}
```

### 10. Using `FilterChip` with long-press gestures

Material3's `FilterChip` internally wraps itself in a `Surface` with its own
`clickable` modifier, which consumes all pointer events. `combinedClickable` on the
chip's modifier or on a parent `Box` will never detect a long-press. Use `LibraryChip`
(in `SymptomsPage.kt`) instead — it uses `Surface` + `combinedClickable` directly.

### 11. Grouping `LibraryUpdated` with no-op events in `reduce()`

The `reduce()` function must handle `LibraryUpdated` in both the early-return path
(before the log loads) and the normal `when` block. Grouping it with no-op events
like `LogLoaded` in the normal path silently discards library updates, making rename
and delete appear non-functional.

```kotlin
// WRONG — LibraryUpdated is a no-op after the log loads
is DailyLogEvent.LogLoaded, is DailyLogEvent.LibraryUpdated -> currentState

// RIGHT — always apply library updates
is DailyLogEvent.LibraryUpdated -> currentState.copy(
    symptomLibrary = event.symptoms,
    medicationLibrary = event.medications,
)
is DailyLogEvent.LogLoaded -> currentState
```

---

## 4.9 Database Migrations

### Migration File Location

All migrations live in:
```
composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/local/database/migrations/
```

### Current Migrations

Each schema version step has a corresponding `Migration_X_Y.kt` file in the migrations
directory. See `PeriodDatabase.kt`'s `@Database` annotation for the current schema
version and `.addMigrations(...)` call for the full list of registered migrations.

### Writing a New Migration

1. **Create the migration object** (replace `N` and `N+1` with the actual versions):

```kotlin
// Migration_N_N1.kt
package com.veleda.cyclewise.androidData.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_N_N1 : Migration(N, N + 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_entries ADD COLUMN new_field TEXT DEFAULT ''")
    }
}
```

2. **Register in `PeriodDatabase.create()`** — add the new migration to the
   `.addMigrations(...)` chain.

3. **Bump the version** in the `@Database` annotation to match the migration's target version.

4. **Update the entity class** to match the new schema.

5. **Write a migration test** to verify the migration runs correctly.

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

### Changing Foreign Key Constraints (Table Rebuild)

SQLite has no `ALTER CONSTRAINT` or `DROP FOREIGN KEY`. To change a foreign key's
`ON DELETE` behavior (e.g., from `RESTRICT` to `CASCADE`), you must rebuild the
table. This is the pattern used in `Migration_11_12` to enable library item deletion:

```kotlin
object Migration_11_12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create temp table with the new FK constraint
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `table_new` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `item_id` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`entry_id`) REFERENCES `daily_entries`(`id`) ON DELETE CASCADE,
                FOREIGN KEY(`item_id`) REFERENCES `item_library`(`id`) ON DELETE CASCADE
            )
        """)
        // 2. Copy all data
        db.execSQL("INSERT INTO `table_new` SELECT * FROM `table`")
        // 3. Drop old table
        db.execSQL("DROP TABLE `table`")
        // 4. Rename temp to original name
        db.execSQL("ALTER TABLE `table_new` RENAME TO `table`")
        // 5. Recreate all indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_entry_id` ON `table`(`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_item_id` ON `table`(`item_id`)")
    }
}
```

**Important:** After the migration, update the entity class's `@ForeignKey`
annotation to match the new constraint (e.g., `onDelete = ForeignKey.CASCADE`).
The annotation and the actual schema must stay in sync.

---

## 4.10 Static Analysis (Lint and Detekt)

RhythmWise uses two static analysis tools to catch issues before they reach code review.

### Android Lint

**What it does:** Checks Android-specific concerns — missing permissions, unused resources, accessibility issues, API compatibility, and security anti-patterns.

**Scope:** `composeApp` module only (Android-specific code).

```bash
./gradlew :composeApp:lintDebug
```

**Configuration:** The `lint` block in `composeApp/build.gradle.kts` sets:
- `abortOnError = true` — the build fails on new errors
- `checkDependencies = true` — also checks library code
- `baseline = file("lint-baseline.xml")` — filters out pre-existing findings

**Baseline:** `composeApp/lint-baseline.xml`. To regenerate, delete the file and re-run lint.

### Detekt

**What it does:** Kotlin-specific static analysis — complexity metrics, naming conventions, code style, coroutine anti-patterns, and unused code.

**Scope:** All Kotlin source sets in both `shared` and `composeApp` modules.

```bash
# Run analysis
./gradlew detekt

# Regenerate baseline
./gradlew detektBaseline
```

**Configuration files:**
| File | Location | Purpose |
|------|----------|---------|
| `detekt.yml` | Project root | Rule configuration |
| `detekt-baseline.xml` | Project root | Pre-existing findings |
| `.editorconfig` | Project root | Formatting baseline (max line length, indent) |

### Key Conventions Enforced

- **Compose functions:** `FunctionNaming` ignores `@Composable` (PascalCase is allowed)
- **Long parameter lists:** Relaxed for `@Composable` functions and data classes
- **Forbidden imports:** `kotlinx.datetime.Clock` — use `kotlin.time.Clock` instead
- **Max line length:** 120 characters
- **Magic numbers:** Excluded from test code
- **Coroutines:** `GlobalCoroutineUsage`, `SleepInsteadOfDelay`, and `SuspendFunSwallowedCancellation` are active

### Kotlin Version Compatibility

Detekt 1.23.8 was built against Kotlin 2.0.21. This project uses Kotlin 2.2.0. The basic `detekt` task (PSI parsing, no type resolution) works correctly. The type-resolution tasks (`detektMain`, `detektTest`) may produce false positives — only the plain `detekt` task should be used until Detekt 2.0 stable ships with Kotlin 2.2+ support.

---

# Phase 5 — Future-Proofing

## 5.1 Planned iOS Module

### Current State

The iOS targets are already declared in `shared/build.gradle.kts`. The `iosApp/`
directory contains a skeleton Xcode project with placeholder Swift files.

### What Needs to Be Built

1. **Repository implementation** — `PeriodRepository` using Core Data, SQLite, or
   another iOS-compatible persistence framework.
2. **Passphrase service** — `PassphraseService` using Apple CryptoKit or a Swift
   Argon2 library.
3. **SwiftUI screens** — Passphrase, Tracker (calendar), DailyLog, Insights, Settings.
4. **DI wiring** — Koin works on iOS via `koin-core`, or use a native Swift solution.
5. **Session scope management** — Equivalent autolock and key lifecycle logic.

### What's Already Shared

All domain logic compiles for iOS out of the box — every model, interface, use case,
insight generator, and library provider in `shared/src/commonMain/`.

---

## 5.2 Insight Engine Overview

### Two-Phase Pipeline

`InsightEngine` (`shared/.../domain/insights/InsightEngine.kt`) runs insight
generators in dependency order:

1. **Phase 1:** `NextPeriodPredictionGenerator` runs first. Its output (predicted
   next period date) is needed by other generators.
2. **Phase 2:** All remaining generators run with the prediction available in
   `InsightData.generatedInsights`.

Results are deduplicated by `Insight.id`, scored by `InsightScorer`, and returned
as `ScoredInsight`s sorted by relevance descending.

### InsightData — The Generator Input

Every generator receives an immutable `InsightData` snapshot:

```kotlin
data class InsightData(
    val allPeriods: List<Period>,
    val allLogs: List<FullDailyLog>,
    val symptomLibrary: List<Symptom>,
    val averageCycleLength: Double?,
    val generatedInsights: List<Insight> = emptyList(),
    val topSymptomsCount: Int,
    val waterIntakes: List<WaterIntake> = emptyList(),
    val medicationLibrary: List<Medication> = emptyList(),
)
```

### Current Generators

Each generator lives in `shared/.../domain/insights/generators/` and produces one
`Insight` sealed interface subtype. After generation, `InsightScorer` wraps each
insight in a `ScoredInsight` with a multi-factor relevance score (significance,
temporal relevance, novelty, category diversity). `CycleSummary` is always pinned
to score 1.0. For the current list of generators, see the source files in the
`generators/` directory and `InsightEngine.kt`.

### Adding a New Generator

1. Create a class implementing `InsightGenerator` in
   `shared/.../domain/insights/generators/`
2. Define a new `Insight` sealed interface subtype in `Insight.kt`
3. Add the generator to the list in `AppModule.kt`'s `InsightEngine` factory

The `InsightEngine` is registered as a `factory` in Koin (not singleton), meaning a
fresh instance is created each time insights are generated.

---

## 5.3 Encryption Versioning

### Current State

The app uses a single encryption scheme:
- Argon2id with fixed parameters (64 MB, 3 iterations, parallelism 1)
- SQLCipher AES-256-GCM

There is no version field on the database or salt storage yet.

### Future Approach

If KDF parameters need to change:

1. Add a `kdf_version` field to `SaltStorage`
2. On unlock, check the version and use the matching KDF parameters
3. After successful unlock, optionally re-derive the key with new parameters and
   use SQLCipher's `PRAGMA rekey` to re-encrypt the database
4. Update the version field

---

## 5.4 Migration Strategy

### Fresh Install vs Upgrade

- **Fresh install:** Room creates the database at the latest version (see
  `PeriodDatabase.kt`). No migrations run.
- **Upgrade:** Room runs each migration sequentially from the installed version to the
  latest. For example, upgrading across several versions runs each `Migration_X_Y`
  object in order.

### Testing Migrations

Each migration should be tested to verify:
1. The SQL executes without errors
2. Existing data is preserved
3. New columns have correct defaults
4. Indexes are created properly

---

## 5.5 DI Scaling

### Current State

All DI wiring lives in a single `appModule` in `di/AppModule.kt`. This is manageable
for the current codebase size.

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
        viewModel { TrackerViewModel(get(), get(), get(), get(), get()) }
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

The session scope qualifier (`SESSION_SCOPE`) works across modules — Koin resolves
dependencies globally within the same scope ID.

---

# Appendix: Dependency Layering Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      UI LAYER                              │  │
│  │  Compose screens (Tracker, DailyLog, Insights, Settings,   │  │
│  │  Passphrase), navigation, theme                            │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ observes state, sends events       │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   VIEWMODEL LAYER                          │  │
│  │  One ViewModel per screen (e.g., TrackerViewModel,         │  │
│  │  DailyLogViewModel, InsightsViewModel)                     │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ calls use cases, repository        │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                    DOMAIN LAYER                            │  │
│  │  Use cases (one per business operation)                    │  │
│  │  InsightEngine + generators (one per insight type)         │  │
│  │  Library providers (symptom, medication, educational)      │  │
│  │  Domain models and enums                                   │  │
│  │  Interfaces: PeriodRepository, PassphraseService           │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ implements interfaces              │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      DATA LAYER                            │  │
│  │  RoomPeriodRepository                                      │  │
│  │  Room DAOs (one per table)                                 │  │
│  │  Room entities + Converters + Mappers                      │  │
│  │  PeriodDatabase (Room + SQLCipher)                         │  │
│  │  Migrations (see migrations/ directory)                    │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │ platform services                  │
│                             ▼                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                  INFRASTRUCTURE LAYER                      │  │
│  │  PassphraseServiceAndroid (Argon2id KDF)                   │  │
│  │  SaltStorage, AppSettings, SessionBus                      │  │
│  │  LockedWaterDraft (water intake while locked)              │  │
│  │  ReminderScheduler + ReminderNotifier (WorkManager)        │  │
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
