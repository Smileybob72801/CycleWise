# RythmWise

**RythmWise** is a privacy-first menstrual-cycle tracker built with **Kotlin Multiplatform (KMP)**.  
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

See [`SECURITY_MODEL.md`](SECURITY_MODEL.md) for detailed implementation.

---

## 🏗️ Architecture Overview
RythmWise follows a **Clean Architecture** layout:
```
shared/
├── domain/ # Use-cases, entities, pure logic
├── data/ # Repositories, SQLCipher Room DAO, local storage
├── di/ # Koin dependency-injection modules
├── platform/ # expect/actual platform utilities
└── presentation/ # UI-facing state & mappers
androidApp/
├── ui/ # Jetpack Compose screens
├── navigation/
└── main/ # App entry, Koin bootstrap
```


Dependency graph and DI wiring explained in [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## 🚀 Build & Run

### Prerequisites
- Android Studio Koala (2025) +
- Kotlin 1.9 +
- Gradle 8 +
- JDK 17

### Build
```bash
./gradlew clean assembleDebug
```

### Launch
Open the project in Android Studio and run on an emulator or device.

## 🤝 Contributing

Contributions are welcome!
Please read CONTRIBUTING.md **before** opening an issue or PR.

All commits must include a DCO sign-off:
Signed-off-by: Your Name <you@example.com>

## ⚖️ License
Released under the Apache License 2.0.
See LICENSE.md

© 2025 Veleda - RythmWise™
Veleda and RythmWise are registered trademarks of Veleda.
See TRADEMARK_POLICY.md for details.
