# Play Store Data Safety Form

Reference answers for the Google Play Console Data Safety section.
Update this document whenever the form is revised.

---

## Overview

RhythmWise collects health and app activity data that the user explicitly
enters. All data is stored locally on the device and encrypted at rest. No data
is shared with third parties or transferred off the device.

---

## Data Collection

### Does your app collect or share any of the required user data types?

**Yes** — the app collects data that stays on the device.

### Is all of the user data collected by your app encrypted in transit?

**Not applicable.** The app has no internet permissions and never transmits data.

### Do you provide a way for users to request that their data is deleted?

**Yes.** Users can delete their data via **Settings > Delete All Data**, which
performs a two-step confirmation and then permanently erases the encrypted
database and all associated records.

---

## Data Types Collected

### Health Info

| Field                 | Value                            |
|-----------------------|----------------------------------|
| **Data type**         | Health info — Menstrual health   |
| **Collected**         | Yes                              |
| **Shared**            | No                               |
| **Ephemeral**         | No (persisted in local database) |
| **Required**          | Yes (core app functionality)     |
| **Purpose**           | App functionality                |
| **User-initiated**    | Yes                              |

Data includes: menstrual cycle dates, flow intensity, color, consistency,
symptoms, symptom severity, mood, energy, libido, medications, dosage notes,
water intake, custom tags, and freeform notes.

### App Activity

| Field                 | Value                            |
|-----------------------|----------------------------------|
| **Data type**         | App activity — App interactions  |
| **Collected**         | Yes                              |
| **Shared**            | No                               |
| **Ephemeral**         | Yes (in-memory only)             |
| **Required**          | No                               |
| **Purpose**           | App functionality                |
| **User-initiated**    | Yes                              |

This covers transient UI state such as the currently viewed page and selected
date. None of this data is persisted beyond the active session.

---

## Security Practices

| Practice                           | Answer |
|------------------------------------|--------|
| Data encrypted at rest             | Yes — AES-256-GCM via SQLCipher |
| Data encrypted in transit          | N/A — no network access          |
| Data shared with third parties     | No                               |
| Data transferred off device        | No                               |
| Deletion mechanism provided        | Yes — Settings > Delete All Data |
| App requests internet permission   | No                               |
| Third-party analytics / SDKs       | None                             |

---

## Supporting Materials

| Document                       | Path                                      |
|--------------------------------|-------------------------------------------|
| Play Store listing text        | `docs/PLAY_STORE_LISTING.md`              |
| Health Declaration answers     | `docs/PLAY_STORE_HEALTH_DECLARATION.md`   |
| Privacy Policy (full)          | `docs/PRIVACY_POLICY.md`                  |
| Terms of Service (full)        | `docs/TERMS_OF_SERVICE.md`                |
| Security architecture          | `docs/SECURITY_MODEL.md`                  |
