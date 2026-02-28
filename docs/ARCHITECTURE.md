# RhythmWise Architecture

## Overview
RhythmWise is organized around **Clean Architecture** principles with **Kotlin Multiplatform (KMP)**.
Business logic lives in the shared module; platform code should remain as minimal as possible.

---

## Layers

| Layer | Purpose | Location |
|-------|----------|-----------|
| **Presentation** | Compose UI screens, state holders (ViewModels), user interactions | `composeApp/src/androidMain/.../ui/` |
| **Domain** | Entities, use cases, repository interface, insight engine; pure logic with no platform dependencies | `shared/src/commonMain/.../domain/` |
| **Data** | Repository implementation, SQLCipher DAOs, Room entities, mappers | `composeApp/src/androidMain/.../androidData/` |
| **DI** | Koin module and scope management | `composeApp/src/androidMain/.../di/AppModule.kt` |
| **Platform** | `expect`/`actual` platform bridges | `shared/src/commonMain/.../Platform.kt` + per-platform actuals (`.android.kt`, `.ios.kt`) |

---

## Dependency Injection
RhythmWise uses **Koin** for DI, wired in a single file: `di/AppModule.kt`.

- **`appModule`** — The sole Koin module. Contains both singleton declarations and a
  session-scoped block.
- **Singleton scope** — ViewModels and services that live for the app process lifetime
  (e.g., `PassphraseViewModel`, `AppSettings`, `ReminderScheduler`).
- **Session scope** (`named("UnlockedSessionScope")`) — A named qualifier scope created
  after successful passphrase unlock. Contains the database, DAOs, repository, use
  cases, library providers, and session-bound ViewModels. Destroyed on logout or
  autolock to zeroize the encryption key.

The unlock session is created after successful key derivation and destroys all scoped instances on logout.

---

## Database Layer
- **Room + SQLCipher** for encrypted local storage
- Encryption key provided via `SupportFactory(passphrase)`
- All migrations versioned and unit-tested

---

## Cross-Platform Design
- **shared** module compiles to Android and iOS targets.
- Android uses Compose; iOS will later use SwiftUI via KMP shared logic.

---

## Testing
- **Unit tests** in `commonTest` and `androidUnitTest`.
- **Feature tests** simulate user flows (e.g., cycle logging, insight generation).
- CI enforces green test runs before merge.
