# Play Store Health Apps Declaration

Reference answers for the Google Play Console Health Apps Declaration form.
Update this document whenever the declaration is revised.

---

## App Category

**Menstrual Cycle Tracking**

---

## Declaration Questions

### Is your app a regulated medical device?

**No.**

RhythmWise is a personal wellness tracker. It does not diagnose, treat, cure,
or prevent any medical condition and is not classified or marketed as a medical
device under any jurisdiction.

### Does your app connect to Health Connect (Android Health)?

**No.**

RhythmWise does not read from or write to Health Connect. All data is stored
exclusively in the app's local encrypted database.

### Does your app use age-restricted signals for health profiling?

**No.**

RhythmWise does not collect age, date of birth, or any demographic information.
It does not perform health profiling based on age-restricted signals.

### How is user health data stored and protected?

All health data is stored **locally on the user's device only**.

- Database encryption: AES-256-GCM via SQLCipher.
- Key derivation: Argon2id (64 MB memory cost, 3 iterations) from a
  user-provided passphrase.
- The encryption key exists only in memory during an active session and is
  zeroized on logout or app lock.
- The passphrase is never stored in plain text and there is no recovery
  mechanism.

### Is health data transmitted off the device?

**No.**

The app declares zero internet permissions (`android.permission.INTERNET` is not
present in the manifest). There is no cloud sync, no analytics, no telemetry,
and no third-party SDKs that transmit data.

---

## In-App Medical Disclaimer Locations

The following disclaimer is displayed to users:

> "This app is not a medical device and does not diagnose, treat, cure, or
> prevent any medical condition. Always consult a qualified healthcare provider
> for medical advice, diagnosis, or treatment."

Displayed in:

1. **First-use onboarding** — Setup screen, page 1 (before passphrase creation)
2. **Insights tab** — Learn section header
3. **Educational article bottom sheet** — footer banner
4. **Settings > About** — Health Content section
5. **Play Store listing** — first paragraph of the full description

---

## Supporting Materials

| Document                       | Path                              |
|--------------------------------|-----------------------------------|
| Play Store listing text        | `docs/PLAY_STORE_LISTING.md`      |
| Data Safety form answers       | `docs/PLAY_STORE_DATA_SAFETY.md`  |
| Privacy Policy (full)          | `docs/PRIVACY_POLICY.md`          |
| Terms of Service (full)        | `docs/TERMS_OF_SERVICE.md`        |
| Security architecture          | `docs/SECURITY_MODEL.md`          |
