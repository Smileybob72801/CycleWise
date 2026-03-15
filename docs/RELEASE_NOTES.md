## v1.0.0-beta.2

**RhythmWise** is a privacy-first menstrual cycle tracker for Android. All of your data stays on your device — encrypted, offline, and fully under your control.

### What is RhythmWise?

A comprehensive cycle tracking app built with one guiding principle: **your data belongs to you**. There are no accounts, no cloud sync, no analytics, and no internet connection — ever. Your database is encrypted with AES-256-GCM (SQLCipher) behind a passphrase that only you know.

### What's New in beta.2

- **Heatmap redesign** — Replaced bubble rendering with merged color borders on calendar day cells for a cleaner visual.
- **Calendar layout** — Moved phase legend and heatmap metric selector below the calendar grid.
- **Period day safety** — Confirmation dialog when removing period days that have logged data.
- **Library management** — Edit and delete symptoms and medications from your personal library.
- **Small screen fix** — Phase legend text no longer wraps vertically and compresses the calendar on smaller devices.

### Features

- **Period tracking** — Log start/end dates, flow intensity, color, and consistency. View your history on an interactive calendar.
- **Daily wellness log** — Record mood, energy, libido, freeform notes, and custom tags.
- **Symptom tracking** — Curated symptom library organized by category with severity ratings and pattern tracking.
- **Medication log** — Track medications and supplements with dosage notes and a personal medication library.
- **Water intake** — Set a daily hydration goal and log with a single tap.
- **Cycle insights** — Cycle length trends, next period predictions, fertile window estimates, symptom recurrence patterns, mood analysis, and phase-based breakdowns — all calculated locally.
- **Educational content** — Articles on cycle basics, symptoms, wellness tips, and when to see a doctor, sourced from U.S. government public health agencies.
- **Reminders** — Customizable daily reminders for logging, period predictions, and hydration goals.

### Privacy & Security

- Zero internet permissions — the app **cannot** connect to the internet
- No analytics, no telemetry, no third-party data SDKs
- AES-256-GCM encryption via SQLCipher with Argon2id key derivation
- Encryption key exists only in memory while unlocked, then destroyed
- No passphrase recovery by design — only you can access your data
- Screen capture protection enabled by default

### Install

Download the APK below and sideload it on your Android device (Android 8.0+).

> **Note:** This is a beta release. If you encounter bugs or have feedback, please [open an issue](../../issues).

RhythmWise is free, open-source (Apache 2.0), and contains no ads or in-app purchases.
