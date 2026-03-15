# Play Store Publishing Guide

Step-by-step guide for publishing RhythmWise to the Google Play Store as an
individual developer. This document references existing project docs rather than
duplicating their content.

---

## Section 0: Prerequisites Checklist

### Already Done

| Item | Location |
|------|----------|
| Release build config with R8 minification | `composeApp/build.gradle.kts` lines 117-125 |
| ProGuard / R8 keep rules | `composeApp/proguard-rules.pro` |
| Adaptive app icon | `composeApp/src/androidMain/res/mipmap-anydpi-v26/` |
| Store listing copy | `docs/PLAY_STORE_LISTING.md` |
| Data Safety form answers | `docs/PLAY_STORE_DATA_SAFETY.md` |
| Health Declaration answers | `docs/PLAY_STORE_HEALTH_DECLARATION.md` |
| Privacy policy text | `docs/PRIVACY_POLICY.md` |
| Terms of Service text | `docs/TERMS_OF_SERVICE.md` |
| In-app privacy policy and ToS (Settings > About) | `composeApp/.../res/values/strings.xml` |
| Screenshot plan | `docs/SCREENSHOT_PLAN.md` |
| Backup disabled (cloud + device transfer) | `composeApp/src/androidMain/res/xml/backup_rules.xml` |
| Only declared permission: `POST_NOTIFICATIONS` | `composeApp/src/androidMain/AndroidManifest.xml` line 4 |
| `.gitignore` excludes `*.jks`, `*.keystore`, `*.p12`, `*.pem` | `.gitignore` lines 25-29 |
| Medical disclaimer in 5 locations | `docs/PLAY_STORE_HEALTH_DECLARATION.md` § In-App Medical Disclaimer Locations |
| `FLAG_SECURE` on activity window | `MainActivity.kt` lines 31-34 |

### Still Needed

- [ ] Google Play Developer account ($25 one-time fee, individual)
- [ ] Upload signing keystore generated and backed up securely
- [ ] Release build tested on a physical Android device
- [ ] 8 screenshots captured per `docs/SCREENSHOT_PLAN.md`
- [ ] Feature graphic (1024x500 PNG) created per `docs/SCREENSHOT_PLAN.md`
- [ ] 512x512 high-res icon PNG exported for Play Console
- [ ] Privacy policy hosted at a public URL (GitHub Pages)
- [ ] Version numbers finalized for first production release
- [ ] IARC content rating questionnaire completed in Play Console
- [ ] 20 testers recruited for the closed testing phase (14-day requirement)

---

## Section 1: Developer Account Setup (Individual)

### Steps

1. Navigate to [Google Play Console](https://play.google.com/console/).
2. Select **Create developer account** > **Individual** > **Get started**.
3. Pay the one-time **$25 registration fee**.
4. Complete **identity verification** — Google will request a government-issued
   ID and may take **1-2 business days** to approve.
5. Choose a **developer name** — this is displayed publicly on the Play Store.
6. Set a **developer email** — this is permanent and cannot be changed later.

### Important Notes

- The email you register with is **permanent**. Choose carefully.
- An individual account **cannot be upgraded** to an organization account later.
  If you ever need an organization account, you would have to create a separate
  one from scratch (requires a D-U-N-S number, business registration, etc.).
- Individual accounts are subject to the **20-tester / 14-day closed testing
  requirement** before production access is granted. This is covered in detail
  in [Section 13](#section-13-closed-testing-required-for-individual-accounts).
  Plan for this early — it is the single biggest time bottleneck.
- If the tester requirement proves too difficult, an organization account is an
  alternative path (no closed testing gate), but it requires a registered
  business and D-U-N-S number.

---

## Section 2: Release Build Verification

Before signing and uploading, verify the release build works correctly.

### Build the Release APK for Local Testing

The release build type does not have a signing config yet. To test locally on a
physical device, temporarily sign the release variant with the debug keystore:

1. In Android Studio: **Build > Select Build Variant** > set to **release**.
2. Go to **File > Project Structure > Modules > Default Config**.
3. Under **Signing Config**, select `$signingConfigs.debug`.
4. Apply, OK, then run on a physical device.

Alternatively, build from the command line:

```bash
./gradlew :composeApp:assembleRelease
```

> **Note:** Revert the debug signing config before creating the real upload
> build. The temporary debug signing is only for local verification.

### Physical Device Testing Checklist

Test these areas specifically — R8 minification can break reflection-dependent
paths that work fine in debug builds:

- [ ] Passphrase creation (onboarding flow)
- [ ] Passphrase unlock (existing database)
- [ ] SQLCipher encryption / Argon2id key derivation (database opens correctly)
- [ ] All Room DAOs (create, read, update, delete operations)
- [ ] WorkManager reminders (hydration, medication, period prediction)
- [ ] Insights charts (Vico chart rendering)
- [ ] Autolock / session timeout (key zeroized, session scope destroyed)
- [ ] Coach mark walkthrough (first-run tooltips)
- [ ] Dark mode / light mode switching
- [ ] Edge-to-edge rendering and system bar insets

### Run the Full Test Suite

```bash
./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
```

### Run Static Analysis

```bash
./gradlew :composeApp:lintDebug
./gradlew detekt
```

Fix any issues before proceeding. All checks must pass cleanly.

---

## Section 3: Signing Configuration

### Generate the Upload Keystore

```bash
keytool -genkeypair -v \
  -keystore rhythmwise-upload.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias rhythmwise-upload
```

You will be prompted for:
- Keystore password
- Key password
- Certificate information (name, organization, etc.)

> **Windows note:** `keytool` ships with Android Studio's bundled JDK at
> `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`. If `keytool`
> is not on your PATH, use the full path or run from that directory.

### Keystore Security

- **Store the keystore outside the repository.** The `.gitignore` already
  excludes `*.jks` and `*.keystore`, but keep it in a separate secure location.
- **Back up the keystore** to at least one other secure location (encrypted USB
  drive, password manager attachment, etc.). If you lose the upload key, you can
  request a reset from Google, but it takes several days.
- **Record the passwords** in a password manager. You need both the keystore
  password and the key password.

### Configure Gradle Signing

Add the keystore credentials to `local.properties` (this file is already
gitignored):

```properties
RELEASE_STORE_FILE=/absolute/path/to/rhythmwise-upload.jks
RELEASE_STORE_PASSWORD=your-keystore-password
RELEASE_KEY_ALIAS=rhythmwise-upload
RELEASE_KEY_PASSWORD=your-key-password
```

Then add a `signingConfigs` block to `composeApp/build.gradle.kts` inside the
`android { }` block, before `buildTypes`:

```kotlin
signingConfigs {
    create("release") {
        val props = rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.let { java.util.Properties().apply { load(it.inputStream()) } }

        storeFile = props?.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
        storePassword = props?.getProperty("RELEASE_STORE_PASSWORD")
        keyAlias = props?.getProperty("RELEASE_KEY_ALIAS")
        keyPassword = props?.getProperty("RELEASE_KEY_PASSWORD")
    }
}
```

Then update the `release` build type to use it:

```kotlin
buildTypes {
    getByName("release") {
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Build the Signed AAB

Google Play requires an Android App Bundle (AAB), not an APK:

```bash
./gradlew :composeApp:bundleRelease
```

The signed AAB will be at:
`composeApp/build/outputs/bundle/release/composeApp-release.aab`

### Play App Signing

When you upload the first AAB to Play Console, **enroll in Play App Signing**
(recommended). This means:

- Your keystore is the **upload key** — used to sign uploads to Play Console.
- Google generates a separate **app signing key** — used to sign the APK
  delivered to users.
- If you lose the upload key, you can request a reset. If you did not enroll in
  Play App Signing, losing the key means you can never update the app.

---

## Section 4: Version Numbering

### Current State

In `composeApp/build.gradle.kts` lines 100-101:

```kotlin
versionCode = 1
versionName = "1.0"
```

### Recommendations

- Bump `versionName` to `"1.0.0"` for the production release (semantic
  versioning).
- Keep `versionCode = 1` for the first upload. This integer must **strictly
  increase** with every upload to Play Console — it can never go backwards or
  stay the same.
- Do **not** use suffixes like `-beta` in `versionName` on the Play Store. Use
  Play Console's testing tracks (internal, closed, open, production) to
  distinguish release stages instead.

### Version Bump Process for Updates

For each subsequent upload:
1. Increment `versionCode` by at least 1.
2. Update `versionName` to reflect the semantic version (e.g., `"1.0.1"`,
   `"1.1.0"`).

---

## Section 5: Play Console — Create the App

1. Open [Google Play Console](https://play.google.com/console/).
2. Click **Create app**.
3. Fill out:
   - **App name:** `RhythmWise`
   - **Default language:** English (United States)
   - **App or Game:** App
   - **Free or Paid:** Free
4. Accept the **Developer Program Policies** and **US export laws**
   declarations.
5. Click **Create app**.

This creates the app listing shell. The left sidebar will show a "Set up your
app" checklist of tasks to complete before you can publish.

---

## Section 6: Store Listing

All listing copy is in **`docs/PLAY_STORE_LISTING.md`**. Copy each field into
Play Console:

| Play Console Field | Source |
|--------------------|--------|
| App name | `PLAY_STORE_LISTING.md` § App Title — "RhythmWise" |
| Short description | `PLAY_STORE_LISTING.md` § Short Description |
| Full description | `PLAY_STORE_LISTING.md` § Full Description |
| Category | Health & Fitness |

### High-Res Icon (512x512)

Play Console requires a 512x512 PNG app icon (separate from the in-app adaptive
icon). To export:

1. Open the adaptive icon foreground drawable in Android Studio.
2. Export a 512x512 PNG render of the icon against its background layer.
3. Alternatively, create a 512x512 PNG from the same source artwork used for
   the adaptive icon layers in
   `composeApp/src/androidMain/res/mipmap-anydpi-v26/`.

Upload this PNG in the **Store listing > Graphics** section of Play Console.

---

## Section 7: Screenshots and Feature Graphic

All specifications are in **`docs/SCREENSHOT_PLAN.md`**.

### Critical: FLAG_SECURE Blocks Screenshots

`MainActivity.kt` lines 31-34 set `FLAG_SECURE` on the activity window, which
blocks all screenshot capture (system screenshots, `adb`, and screen recording).

**Before capturing screenshots, temporarily comment out the flag:**

```kotlin
// window.setFlags(
//     WindowManager.LayoutParams.FLAG_SECURE,
//     WindowManager.LayoutParams.FLAG_SECURE
// )
```

Build and run a debug variant for the screenshot session. **Re-enable
FLAG_SECURE immediately afterward.** Do not commit the change or ship a build
without it.

### Screenshots (8 Total)

Follow the plan in `docs/SCREENSHOT_PLAN.md`:

- Use a **Pixel 8 emulator** (1080x2400) in portrait orientation.
- Load **tutorial seed data** (`TutorialSeederUseCase`) — do not use real health
  data.
- Capture all 8 screens listed in the plan.
- Add caption overlays as specified.
- Apply device frames (Android Studio device frames or screenshots.pro).

Upload screenshots under **Store listing > Graphics > Phone screenshots** in
Play Console.

### Feature Graphic (1024x500)

Create per the design spec in `docs/SCREENSHOT_PLAN.md` § Feature Graphic:

- 1024x500 PNG, no alpha channel.
- Purple-to-rose gradient background (`#8E6C88` → `#B5808E`).
- App icon on the left, "RhythmWise" title and tagline on the right.

Upload under **Store listing > Graphics > Feature graphic**.

---

## Section 8: Content Rating (IARC)

Navigate to **Policy and programs > Content rating** in Play Console and
complete the IARC questionnaire.

Reference **`docs/PLAY_STORE_LISTING.md`** § Content Rating Notes for all
answers:

| Question | Answer |
|----------|--------|
| Health information | Yes — menstrual health info and educational articles |
| Violence | None |
| Sexual content | None |
| Gambling | None |
| Substance use | None (medication tracking is general medications only) |
| User-generated content | None (all data is local, no sharing/social features) |
| Location data | Not collected |
| Personal data shared with third parties | None (zero internet permissions) |
| Ads | None |
| In-app purchases | None |

**Expected rating:** Everyone

---

## Section 9: Data Safety Form

Navigate to **Policy and programs > App content > Data safety** in Play Console.

Walk through the form using **`docs/PLAY_STORE_DATA_SAFETY.md`** verbatim:

1. **Does your app collect or share any required user data types?** → Yes
2. **Is all collected data encrypted in transit?** → Not applicable (no internet
   permissions, data never transmitted)
3. **Can users request data deletion?** → Yes (Settings > Delete All Data)
4. **Data types collected:**
   - **Health info — Menstrual health:** Collected, not shared, not ephemeral,
     required, purpose is app functionality, user-initiated.
   - **App activity — App interactions:** Collected, not shared, ephemeral
     (in-memory only), not required, purpose is app functionality,
     user-initiated.
5. **Security practices:** Data encrypted at rest (AES-256-GCM via SQLCipher),
   no third-party sharing, no data transfer off device, deletion mechanism
   provided.

---

## Section 10: Health Apps Declaration

Navigate to **Policy and programs > App content > Health apps** in Play Console.

Walk through the form using **`docs/PLAY_STORE_HEALTH_DECLARATION.md`**
verbatim:

| Question | Answer |
|----------|--------|
| App category | Menstrual Cycle Tracking |
| Regulated medical device? | No |
| Connects to Health Connect? | No |
| Age-restricted signals for health profiling? | No |
| How is health data stored and protected? | Locally on device, AES-256-GCM via SQLCipher, Argon2id key derivation, key zeroized on lock |
| Health data transmitted off device? | No (zero internet permissions) |

---

## Section 11: Privacy Policy Hosting (GitHub Pages)

Play Console requires a **publicly accessible URL** for your privacy policy. Use
GitHub Pages on your personal repository.

### Steps

1. In the GitHub repository, go to **Settings > Pages**.
2. Under **Source**, select **Deploy from a branch**.
3. Select the branch (e.g., `main`) and root folder (`/` or `/docs`).
4. Click **Save**.
5. GitHub Pages will build and deploy. The site URL will be displayed (e.g.,
   `https://smileybob72801.github.io/CycleWise/`).

### Privacy Policy Page

**Option A — Jekyll Markdown rendering (simplest):**

If deploying from `/docs`, GitHub Pages with Jekyll will automatically render
`PRIVACY_POLICY.md` as HTML. The URL would be something like:

```
https://smileybob72801.github.io/CycleWise/PRIVACY_POLICY
```

You can add a front matter block to control the page title:

```markdown
---
title: Privacy Policy
---
```

**Option B — Dedicated HTML page:**

Create a standalone `privacy-policy.html` in the Pages source directory with
the content from `docs/PRIVACY_POLICY.md` rendered as HTML. This gives full
control over styling.

### Enter the URL in Play Console

The privacy policy URL must be entered in **two** locations:

1. **Store settings** (under **Policy and programs > App content > Privacy
   policy**)
2. **Main store listing** (under **Grow > Store presence > Main store listing >
   Store settings**)

### Verify Before Submitting

- Open the URL in an incognito/private browser window to confirm it is publicly
  accessible.
- Check that the content matches the current version of
  `docs/PRIVACY_POLICY.md`.

---

## Section 12: Internal Testing

Internal testing lets you verify the upload and install flow without Google
review. It is available immediately after upload.

### Steps

1. In Play Console, go to **Test and release > Testing > Internal testing**.
2. Click **Create new release**.
3. Upload the signed AAB from [Section 3](#section-3-signing-configuration).
4. Fill out the release name and release notes.
5. Click **Next**, then **Save and publish**.
6. Go to the **Testers** tab.
7. Create an email list (add your own email).
8. **Copy the opt-in link** at the bottom of the Testers tab.
9. Open the link on your Android device, accept the invitation, and install
   from the Play Store.

### What to Verify

- App installs and opens from the Play Store.
- Passphrase creation works (onboarding).
- All core features function with R8 minification active.
- The correct version name and code appear in Settings > About.

> **Note about the deobfuscation warning:** Play Console may warn about a
> missing deobfuscation file. Upload `mapping.txt` for readable crash reports
> (see [Section 15](#section-15-post-launch)), but the warning does not block
> testing.

---

## Section 13: Closed Testing (Required for Individual Accounts)

This is the **biggest hurdle** for individual developer accounts. Google
requires:

- **20 real people** opted in as testers on Android devices.
- The app must be **run across 14 different days** (not 14 consecutive days —
  14 distinct calendar days with at least one tester active).
- Google monitors for gaming — testers must be **genuine users**, not fake
  accounts or bots.

Until this requirement is met, you **cannot publish to production**.

### Steps

1. In Play Console, go to **Test and release > Testing > Closed testing**.
2. Click **Create track** (or use the default "Closed testing" track).
3. Click **Create new release**, upload the signed AAB.
4. Fill out release name and notes, click **Next**, then **Save and publish**.
5. Google will **review the AAB** before testers can access it — this takes
   **1-3 business days**.
6. Once approved, go to the **Testers** tab.
7. Create an email list with your testers' Gmail addresses.
8. Copy the **opt-in link** and distribute it to all testers.
9. Testers open the link, accept the invitation, and install from the Play
   Store.

### Recruiting 20 Testers

This requires real people with Android devices who will actually install and
open the app. Strategies:

- **Friends and family** — the most reliable source, but you may not have 20
  Android users in your circle.
- **Developer communities** — Reddit (r/androiddev, r/playmyapp), Discord
  servers, open-source communities.
- **Open-source community** — since RhythmWise is Apache 2.0, you can post in
  open-source forums and ask for testers.
- **Social media** — Twitter/X, Mastodon, relevant subreddits.
- **Reciprocal testing** — offer to test other developers' apps in exchange.

> **Do not use fake accounts or testing services that "sell" testers.** Google
> actively monitors for this and may suspend your developer account.

### Timeline

- AAB review: 1-3 days
- Recruiting 20 testers: varies (potentially the longest part)
- 14-day testing window: 14 days minimum
- **Total: 14-21+ days** after the AAB is approved

Once Google confirms the 20-tester / 14-day threshold is met, production access
unlocks in Play Console.

---

## Section 14: Production Release

### Pre-Flight Checks

1. Verify all tasks under **"Set up your app"** in Play Console are marked
   green / complete.
2. Double-check:
   - Store listing, screenshots, and feature graphic are uploaded.
   - Content rating is set.
   - Data safety form is submitted.
   - Health declaration is submitted.
   - Privacy policy URL is live and accessible.
   - Target audience is configured.

### App Access

Play Console may ask about app access for reviewers. RhythmWise uses a
passphrase — reviewers **create their own passphrase** during onboarding. There
are no external credentials or server accounts to provide. Select the option
indicating the app does not require special access instructions, or add a brief
note:

> "The app requires a user-created passphrase during first launch. Reviewers
> can create any passphrase to access the app. There are no external accounts
> or login credentials."

### Target Audience

Set the target audience to **18+**. The app handles health data and is not
designed for children. **Do not** select any age group that includes children
under 13 — this would trigger additional Children's Online Privacy Protection
requirements.

### Submit for Review

1. Go to **Test and release > Production**.
2. Either **create a new release** and upload the AAB, or **promote** an
   existing release from closed testing.
3. Select the **countries and regions** where you want to distribute.
4. Click **Next**, review the summary, then **Save and publish**.
5. Click **Send changes for review** on the overview page.

### Review Timeline

- Standard apps: **3-7 business days**.
- Health apps may receive additional scrutiny and take **longer** — potentially
  up to 2 weeks for the first submission.
- Google will email you if they need additional information or if the review is
  complete.

---

## Section 15: Post-Launch

### Upload mapping.txt

Upload the R8/ProGuard mapping file so crash reports in Play Console are
readable:

1. Find the mapping file at:
   `composeApp/build/outputs/mapping/release/mapping.txt`
2. In Play Console, go to the release and upload it under **Deobfuscation
   files**.

Upload a new `mapping.txt` with every release — each build produces a different
mapping.

### Monitor Android Vitals

Check **Quality > Android vitals** in Play Console regularly for:
- ANR (Application Not Responding) rates
- Crash rates
- Excessive wakeups or stuck wake locks

### Respond to Reviews

Monitor and respond to user reviews in **Ratings and reviews**. Prompt,
respectful responses improve your store listing visibility.

### Update Process

For each app update:

1. Increment `versionCode` (must be strictly higher than the previous upload).
2. Update `versionName` as appropriate.
3. Build the signed AAB: `./gradlew :composeApp:bundleRelease`
4. Upload to an appropriate track (internal → closed → production, or directly
   to production).
5. Upload the new `mapping.txt`.
6. Write release notes describing the changes.

### Maintain targetSdk

Google requires apps to target a recent API level. Currently:

- `targetSdk = 35` (set in `composeApp/build.gradle.kts` line 99)
- Google typically requires updates to the latest targetSdk within one year of
  its release. Monitor the
  [target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)
  page.

---

## Section 16: Common Pitfalls

### 1. R8 Breaks Native Crypto Paths

SQLCipher and BouncyCastle (Argon2) use reflection and JNI. The ProGuard rules
in `composeApp/proguard-rules.pro` already have keep rules for these, but if you
add new reflection-dependent code, verify it works in release builds. Always test
the release build on a physical device before uploading.

### 2. FLAG_SECURE Blocks Screenshot Capture

`MainActivity.kt` lines 31-34 set `FLAG_SECURE`, which blocks all screenshot
and screen recording tools. You must temporarily disable it for the screenshot
capture session. Do not forget to re-enable it before building the upload AAB.

### 3. Upload Key vs App Signing Key Confusion

If you enroll in Play App Signing (recommended), there are two keys:

- **Upload key** — your `rhythmwise-upload.jks`. Used to sign AABs for upload
  to Play Console.
- **App signing key** — managed by Google. Used to sign the final APK delivered
  to users.

If you lose the upload key, request a reset in Play Console (takes a few days).
The app signing key is managed by Google and cannot be lost.

### 4. versionCode Not Incrementing

Play Console rejects uploads where `versionCode` is less than or equal to the
previously uploaded value. Always increment before uploading.

### 5. Privacy Policy URL Inaccessible

The privacy policy URL must be reachable by anyone on the public internet. Test
in an incognito window. Common issues:

- GitHub Pages not enabled or not deployed yet.
- Repository is private (GitHub Pages requires a public repo on the free plan).
- Incorrect URL path.

### 6. Health App Review Delays

Health-related apps may receive additional scrutiny. Ensure:

- The medical disclaimer is prominently displayed (it is — in 5 locations).
- The app is clearly positioned as a wellness tracker, not a medical device.
- The Data Safety and Health Declaration forms are filled out accurately.

### 7. 20-Tester Recruitment Difficulty

The closed testing requirement needs **genuine users**, not just email
addresses. Each tester must:

- Have a Google account.
- Opt in via the link.
- Actually install and open the app on an Android device.

Start recruiting early — this is the most time-consuming part of the process.

### 8. Individual Account Cannot Be Upgraded

If you later want an organization account (e.g., for a business), you must
create a completely new developer account ($25 again, plus D-U-N-S number,
business registration, etc.). The existing app would remain under the individual
account.

### 9. Missing mapping.txt for Obfuscated Crash Reports

Without the mapping file, crash reports in Play Console show obfuscated class
and method names. Upload `mapping.txt` with every release.

### 10. App Access Questions from Reviewers

Google reviewers may be confused by the passphrase requirement. They do not need
existing credentials — they create a new passphrase during onboarding, just like
any new user. Add a note in the app access section explaining this (see
[Section 14](#section-14-production-release)).

### 11. Target Audience: Do NOT Mark as Designed for Children

RhythmWise handles health data. Marking the app as targeting children would
trigger Families Policy requirements (COPPA compliance, teacher-approved
program, etc.) that do not apply and would likely cause the app to be rejected.
Set the target audience to 18+.

---

## Appendix A: Command Reference

### Build

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:assembleDebug` | Build debug APK |
| `./gradlew :composeApp:assembleRelease` | Build release APK (needs signing config) |
| `./gradlew :composeApp:bundleRelease` | Build signed release AAB for Play Store upload |

### Test

| Command | Purpose |
|---------|---------|
| `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest` | Run all unit tests |
| `./gradlew :shared:testDebugUnitTest` | Run shared module tests only |
| `./gradlew :composeApp:testDebugUnitTest` | Run composeApp tests only |

### Static Analysis

| Command | Purpose |
|---------|---------|
| `./gradlew :composeApp:lintDebug` | Run Android Lint |
| `./gradlew detekt` | Run Detekt analysis |

### Signing

| Command | Purpose |
|---------|---------|
| `keytool -genkeypair -v -keystore rhythmwise-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias rhythmwise-upload` | Generate upload keystore |

### Verification

| Command | Purpose |
|---------|---------|
| `jarsigner -verify -verbose -certs composeApp/build/outputs/bundle/release/composeApp-release.aab` | Verify AAB signature |
| `bundletool build-apks --bundle=composeApp/build/outputs/bundle/release/composeApp-release.aab --output=out.apks --mode=universal` | Extract universal APK from AAB for local testing |

---

## Appendix B: Document Cross-Reference Table

| Project Document | Play Console Section |
|-----------------|---------------------|
| `docs/PLAY_STORE_LISTING.md` | Grow > Store presence > Main store listing |
| `docs/PLAY_STORE_DATA_SAFETY.md` | Policy and programs > App content > Data safety |
| `docs/PLAY_STORE_HEALTH_DECLARATION.md` | Policy and programs > App content > Health apps |
| `docs/PRIVACY_POLICY.md` | Policy and programs > App content > Privacy policy; also Main store listing |
| `docs/TERMS_OF_SERVICE.md` | Not required by Play Console, but referenced in-app |
| `docs/SCREENSHOT_PLAN.md` | Grow > Store presence > Main store listing > Graphics |
| `docs/SECURITY_MODEL.md` | Supporting reference for Data Safety and Health Declaration answers |
| `composeApp/proguard-rules.pro` | N/A (build artifact, not uploaded) |
| `composeApp/build.gradle.kts` | N/A (version numbers, signing config) |

---

## Appendix C: Timeline Estimator

Estimated timeline from start to production listing, for an individual developer
account:

| Phase | Duration | Notes |
|-------|----------|-------|
| Account setup and identity verification | 1-2 days | $25 fee, government ID verification |
| Release build verification and signing | 1-2 days | Physical device testing, keystore generation |
| Screenshot and feature graphic creation | 1-2 days | 8 screenshots + feature graphic per SCREENSHOT_PLAN.md |
| Play Console setup (listing, forms, rating) | 1-2 days | Copy from existing docs, upload graphics |
| Privacy policy hosting (GitHub Pages) | < 1 day | Enable Pages, verify URL |
| Internal testing | 1 day | Upload AAB, verify install flow |
| Closed testing (20 testers, 14 days) | **14-21+ days** | **The bottleneck.** Depends on tester recruitment speed |
| Production review | 3-7 days | May be longer for health apps |
| **Estimated total** | **3-5 weeks** | **Heavily depends on tester recruitment speed** |

The closed testing phase dominates the timeline. Begin recruiting testers as
early as possible — ideally while working through the earlier steps.
