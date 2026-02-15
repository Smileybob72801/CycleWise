# RhythmWise Security Model

## Core Principle
> **User data belongs solely to the user.**  
> No remote storage, no telemetry, no third parties.

---

## Key Derivation
- User passphrase → **Argon2id** key derivation
    - Salt: random 128-bit per-install
    - Parameters: Memory 64 MB, Iterations 3, Parallelism 2
    - Output: 256-bit AES key

---

## Encryption
- Database encrypted using **SQLCipher** (AES-256-GCM).
- Each record stored via Room with the cipher factory.
- Keys never persist to disk.

---

## Session Scope
- After unlock, a Koin `sessionScope` is created and injected with the derived key.
- The derived key is **actively zeroized** (`fill(0)`) immediately after the database
  is opened, via `try/finally` in `createDatabaseAndZeroizeKey()`.
- On logout or app background timeout:
    - All repositories and DAOs in the scope are closed.
    - The session scope is destroyed.

---

## No Recovery Policy
- No account linkage or backup service.
- Forgotten passphrases cannot be recovered.
- This design prevents forensic or remote access to private data.

---

## Network
- App has **no network permissions** in production.
- `android.permission.INTERNET` is explicitly absent from the manifest.
- A Robolectric unit test (`ManifestPermissionTest`) enforces this invariant.
- All functionality operates offline.

---

## Screen Protection
- `FLAG_SECURE` is set on the activity window at launch.
- Blocks screenshots, screen recording, and recent-apps thumbnails.

---

## Logging Hygiene
- Catch blocks log **message only** (`e.message`), never full stack traces.
- The global crash handler in `MainActivity` is the sole exception (unrecoverable crashes).
- No `println()` in Gradle build scripts.
- Passphrase, derived key, and internal structure are never logged.

---

## Threat Model
- Protect against device theft (offline attacker).
- Mitigate shoulder-surfing via timeout lock and `FLAG_SECURE`.
- Assume compromised device ⇒ compromised data; user informed of risks.