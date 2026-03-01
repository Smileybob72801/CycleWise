# Running Tests

## Unit Tests

### All unit tests (both modules)
```bash
./gradlew testDebugUnitTest
```

### Shared module only
```bash
./gradlew :shared:testDebugUnitTest
```

### ComposeApp module only
```bash
./gradlew :composeApp:testDebugUnitTest
```

### Single test class
```bash
./gradlew testDebugUnitTest --tests "*.DailyEntryDaoTest"
./gradlew testDebugUnitTest --tests "*.ConvertersTest"
./gradlew :shared:testDebugUnitTest --tests "*.InsightEngineTest"
```

### Single test method
```bash
./gradlew testDebugUnitTest --tests "*.PeriodDaoTest.getAllCycles_WHEN_databaseIsEmpty_THEN_returnsEmptyFlow"
```

## Instrumented / E2E Tests

Requires a connected device or running emulator.

```bash
./gradlew connectedDebugAndroidTest
```

## Build Verification (no test execution)

```bash
./gradlew assembleDebug
```

## Test Reports

After running tests, HTML reports are generated at:
- `composeApp/build/reports/tests/testDebugUnitTest/index.html`
- `shared/build/reports/tests/testDebugUnitTest/index.html`

## CI

Unit tests run automatically on push and PR to `main`/`develop` via GitHub Actions (`.github/workflows/test.yml`). Instrumented/E2E tests require a device and must be run locally.

## Static Analysis

### Android Lint

Runs Android-specific checks (missing permissions, unused resources, accessibility, etc.) on the `composeApp` module.

```bash
./gradlew :composeApp:lintDebug
```

**Reports:**
- HTML: `composeApp/build/reports/lint-results-debug.html`
- Text: `composeApp/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`

**Baseline:** `composeApp/lint-baseline.xml` — captures pre-existing findings so new regressions are caught. To regenerate the baseline, delete the file and re-run lint.

### Detekt (Kotlin Static Analysis)

Runs Kotlin-specific checks (complexity, naming, style, coroutines) across both `shared` and `composeApp` modules.

```bash
# Run analysis (fails on new issues)
./gradlew detekt

# Regenerate baseline (captures all current findings)
./gradlew detektBaseline
```

**Reports:**
- HTML: `build/reports/detekt/detekt.html`
- Plain text: `build/reports/detekt/detekt.txt`
- SARIF: `build/reports/detekt/detekt.sarif`

**Configuration:** `detekt.yml` (project root) — rules are tailored for KMP + Compose patterns.

**Baseline:** `detekt-baseline.xml` (project root) — captures pre-existing findings.

> **Note:** Detekt 1.23.8 was built against Kotlin 2.0.21; this project uses Kotlin 2.2.0. Only the plain `detekt` task (PSI parsing) should be used. Type-resolution tasks (`detektMain`, `detektTest`) may produce false positives until Detekt 2.0 ships with Kotlin 2.2+ support.
