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
- Key lifetime = session lifetime.
- On logout or app background timeout:
    - Key is zeroized in memory.
    - All repositories and DAOs in the scope are closed.

---

## No Recovery Policy
- No account linkage or backup service.
- Forgotten passphrases cannot be recovered.
- This design prevents forensic or remote access to private data.

---

## Network
- App has **no network permissions** in production.
- All functionality operates offline.

---

## Threat Model
- Protect against device theft (offline attacker).
- Mitigate shoulder-surfing via timeout lock.
- Assume compromised device ⇒ compromised data; user informed of risks.