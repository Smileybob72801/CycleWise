# RhythmWise

**RhythmWise** is a privacy-first menstrual-cycle tracker built with **Kotlin Multiplatform (KMP)**.  
It stores all data **locally**, encrypted with a **passphrase-derived key**, ensuring total user autonomy and zero data harvesting.

---

## 🌟 Vision & Core Values
- **Absolute Privacy** - All data stays on the device. No cloud, telemetry, or third-party analytics.
- **User Autonomy** - Encryption keys never leave user control.
- **Transparency** - Open source for verifiable security.
- **Accessibility** - Lightweight, offline-first design.
- **Empathy** - Built to respect the personal nature of menstrual health data.

---

## 🔐 Security Philosophy
- Client-side encryption using **Argon2id** (key derivation) and **AES-GCM** (storage encryption).
- Data encrypted at rest via **SQLCipher + Room**.
- Key material lives only during an **active unlock session**.
- No recovery mechanisms, accounts, or telemetry.

See [`SECURITY_MODEL.md`](docs/SECURITY_MODEL.md) for detailed implementation.

---

## 🏗️ Architecture Overview
RhythmWise follows a **Clean Architecture** layout:
```
shared/
└── domain/              # Use cases, entities, repository interface, insight engine, providers
    ├── models/          # Period, DailyEntry, FullDailyLog, CyclePhase, enums, etc.
    ├── usecases/        # StartNewPeriodUseCase, EndPeriodUseCase, etc.
    ├── insights/        # InsightEngine + 6 generators
    ├── repository/      # PeriodRepository interface
    ├── providers/       # SymptomLibrary, MedicationLibrary, EducationalContent
    └── services/        # PassphraseService interface
composeApp/
├── ui/                  # Jetpack Compose screens (auth, tracker, log, insights, settings, nav, coachmark, components)
├── androidData/         # Room DAOs, entities, mappers, repository implementation, migrations
├── di/                  # AppModule.kt — Koin DI wiring (singleton + session scope)
├── services/            # PassphraseServiceAndroid, SaltStorage
├── settings/            # AppSettings (DataStore preferences)
├── session/             # SessionBus (logout event bus)
└── reminders/           # ReminderScheduler, ReminderNotifier, WorkManager workers
```


Dependency graph and DI wiring explained in [`ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## 🚀 Build & Run

### Quick Option — Download the Latest Release

If you just want to install the app, download the latest APK from the
[Releases](../../releases) page and sideload it onto your Android device.

### Build from Source

#### Prerequisites

- **Android Studio** — latest stable release (Ladybug or newer)
- **JDK 11+** — bundled with Android Studio, no separate install needed
- **Android SDK 35** — install via Android Studio's SDK Manager

> Gradle 8.13 and Kotlin 2.2.0 ship via the Gradle wrapper and version catalog —
> you do **not** need to install them manually.

#### Option A — Android Studio (recommended)

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/RhythmWise.git
   ```
2. Open the project root in Android Studio
3. Wait for Gradle sync to finish (the first sync downloads all dependencies)
4. Select the **composeApp** run configuration from the toolbar
5. Press **Run** (green play button) with an emulator or connected device

#### Option B — Command line

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

#### First Launch

The app opens to a **passphrase screen** — create any passphrase you like. There is
no recovery mechanism, so remember it. Once unlocked, open **Settings** and tap
**Seed Debug Data** to populate the calendar with sample periods and symptoms.

#### Running Tests

```bash
./gradlew testDebugUnitTest
```

#### Static Analysis

```bash
# Android Lint (composeApp module)
./gradlew :composeApp:lintDebug

# Detekt (Kotlin static analysis, all sources)
./gradlew detekt
```

Lint and Detekt baselines capture existing findings so new regressions are caught.
See [`docs/testing/RUNNING_TESTS.md`](docs/testing/RUNNING_TESTS.md) for report paths and baseline info.

For full onboarding instructions, see [`docs/DeveloperOnboarding.md`](docs/DeveloperOnboarding.md).

## 🤝 Contributing

Contributions are welcome!
Please read CONTRIBUTING.md **before** opening an issue or PR.

RhythmWise follows the Conventional Commit Specification for commit messages:
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

All commits must include a DCO sign-off:
Signed-off-by: Your Name <you@example.com>

## 🧭 Maintainer Rights & Governance

RhythmWise is an open-source project created and maintained by **Daniel Simmons**.

Daniel Simmons retains long-term stewardship and decision-making authority over:
- The project’s **vision, roadmap, and release cadence**
- The official **RhythmWise™** name, logos, and visual identity
- Approval of pull requests and code merges to protected branches

Community contributions are warmly encouraged and reviewed on their technical
and ethical merit, but inclusion in the official repository and releases
is at Daniel Simmons’s discretion.

This structure ensures that RhythmWise continues to uphold its core values of
privacy, transparency, and user autonomy while maintaining a consistent and
trustworthy experience for everyone.

## 📚 Documentation Map

RhythmWise maintains clear, structured documentation to make it easy for new
contributors, auditors, and users to understand how the project works.

| Area                                      | Description                                                        | Location                                                     |
|-------------------------------------------|--------------------------------------------------------------------|--------------------------------------------------------------|
| **Architecture**                          | System layout, KMP structure, DI, and module design                | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)               |
| **Security Model**                        | Key derivation, encryption, session scopes, and privacy guarantees | [`docs/SECURITY_MODEL.md`](docs/SECURITY_MODEL.md)           |
| **Code Style Guide**                      | Kotlin, Compose, and testing conventions                           | [`docs/CODE_STYLE.md`](docs/CODE_STYLE.md)                   |
| **Issue Writing Guide**                   | Guide for writing high-quality feature requests and bug reports    | [`docs/ISSUE_WRITING_GUIDE.md`](docs/ISSUE_WRITING_GUIDE.md) |
| **Governance**                            | Maintainer responsibilities and decision-making process            | [`GOVERNANCE.md`](GOVERNANCE.md)                             |
| **Contributing Guide**                    | Branching model, DCO, CI/CD rules, and review policy               | [`CONTRIBUTING.md`](CONTRIBUTING.md)                         |
| **Git Commit Guide**                      | Conventional Commits Specification                                 | [`GIT_COMMIT_GUIDELINES.md`](docs/GIT_COMMIT_GUIDELINES.md)  |
| **Code of Conduct**                       | Contributor behavior and community standards                       | [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)                   |
| **Trademark Policy**                      | Rules for using the RhythmWise™ and Daniel Simmons names or logos  | [`TRADEMARK_POLICY.md`](TRADEMARK_POLICY.md)                 |
| **Developer Certificate of Origin (DCO)** | Legal attestation for all contributions                            | [`DCO.md`](DCO.md)                                           |
| **License**                               | Project license (Apache 2.0)                                       | [`LICENSE`](LICENSE)                                         |

Additional technical or meta documentation may be found in the [`docs/`](docs)
directory. Each document is self-contained and linked from the others for easy
navigation.

## ⚖️ License
Released under the Apache License 2.0.
See [`LICENSE`](LICENSE)

© 2025 Veleda - RhythmWise™
RhythmWise is a registered trademarks.
See TRADEMARK_POLICY.md for details.
