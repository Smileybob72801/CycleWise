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
