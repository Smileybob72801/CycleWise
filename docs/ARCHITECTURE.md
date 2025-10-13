# RhythmWise Architecture

## Overview
RhythmWise is organized around **Clean Architecture** principles with **Kotlin Multiplatform (KMP)**.  
Business logic lives in the shared module; platform code should remain as minimal as possible.

---

## Layers

| Layer | Purpose | Location |
|-------|----------|-----------|
| **Presentation** | Compose UI screens, state holders (ViewModels), user interactions | `androidApp/ui`, `shared/presentation` |
| **Domain** | Entities and use cases; pure logic with no dependencies | `shared/domain` |
| **Data** | Repository implementations, SQLCipher DAOs | `shared/data` |
| **DI** | Koin modules and scope management | `shared/di` |
| **Platform** | expect/actual platform bridges (logging, datetime, storage) | `shared/platform` |

---

## Dependency Injection
RhythmWise uses **Koin** for DI:
- `appModule` → ViewModels and controllers  
- `dataModule` → Repositories and DAOs  
- `sessionScope` → Scoped objects tied to an active unlock session

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
