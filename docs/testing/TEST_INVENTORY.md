# Test Inventory

Comprehensive listing of every test file in the RhythmWise test suite.

## composeApp — Unit Tests (`androidUnitTest`)

### DAO Integration Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `dao/DailyEntryDaoTest.kt` | `DailyEntryDao` | Room (in-memory), Robolectric, Koin |
| `dao/MedicationDaoTest.kt` | `MedicationDao` | Room (in-memory), Robolectric, Koin |
| `dao/MedicationLogDaoTest.kt` | `MedicationLogDao` | Room (in-memory), Robolectric, Koin |
| `dao/PeriodDaoTest.kt` | `PeriodDao` | Room (in-memory), Robolectric, Koin |
| `dao/SymptomDaoTest.kt` | `SymptomDao` | Room (in-memory), Robolectric, Koin |
| `dao/SymptomLogDaoTest.kt` | `SymptomLogDao` | Room (in-memory), Robolectric, Koin |
| `dao/WaterIntakeDaoTest.kt` | `WaterIntakeDao` | Room (in-memory), Robolectric, Koin |

### Repository Integration Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `repository/RoomCycleRepositoryTest.kt` | `RoomPeriodRepository` | Room (in-memory), Robolectric, Koin, Turbine |

### Mapper & Converter Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `entities/ConvertersTest.kt` | `Converters` (Room TypeConverters) | None (pure unit) |
| `entities/MappersTest.kt` | Entity↔Domain mapper extensions | None (pure unit) |

### DataStore Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `draft/LockedWaterDraftTest.kt` | `LockedWaterDraft` | Robolectric, DataStore |

### Service Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `services/PassphraseServiceAndroidTest.kt` | `PassphraseServiceAndroid` | MockK |

### ViewModel Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `ui/tracker/CycleViewModelTest.kt` | `TrackerViewModel` | MockK, Turbine, InstantTaskExecutorRule |
| `ui/insights/InsightsViewModelTest.kt` | `InsightsViewModel` | MockK, Turbine, InstantTaskExecutorRule |
| `ui/auth/WaterSyncTest.kt` | `WaterDraftSyncer` | MockK |

### Test Infrastructure

| File | Purpose |
|------|---------|
| `KoinTestRule.kt` | JUnit Rule for Koin lifecycle management |
| `RobolectricTestApp.kt` | Custom Application class for Robolectric |
| `testutil/TestData.kt` | Deterministic time/date constants |
| `testutil/TestDatabaseModule.kt` | Shared Koin module for in-memory Room DB |
| `testutil/TestEntityBuilders.kt` | Factory functions for Room entity construction |
| `testutil/TestDomainBuilders.kt` | Factory functions for domain model construction |

## shared — Unit Tests (`androidUnitTest`)

### Use Case Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `usecases/StartNewCycleUseCaseTest.kt` | `StartNewPeriodUseCase` | MockK |
| `usecases/EndPeriodUseCaseTest.kt` | `EndPeriodUseCase` | MockK |
| `usecases/GetOrCreateDailyEntryUseCaseTest.kt` | `GetOrCreateDailyLogUseCase` | MockK |

### Domain Logic Unit Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `insights/InsightEngineTest.kt` | `InsightEngine` + 6 generators | None (pure unit) |

### Test Infrastructure

| File | Purpose |
|------|---------|
| `testutil/TestData.kt` | Deterministic time/date constants |
| `testutil/TestDomainBuilders.kt` | Factory functions for domain model construction |

## composeApp — Instrumented Tests (`androidTest`)

### E2E Tests

| File | Target Under Test | Dependencies |
|------|------------------|--------------|
| `e2e/UnlockCreateLogE2ETest.kt` | Full unlock → log → verify flow | Compose Test, AndroidJUnit4 |

### E2E Infrastructure

| File | Purpose |
|------|---------|
| `CustomTestRunner.kt` | Custom AndroidJUnitRunner |
| `TestCycleWiseApp.kt` | Test Application class |
| `di/TestOverrides.kt` | DI overrides for E2E environment |

## Summary

| Category | Count |
|----------|-------|
| DAO integration tests | 7 |
| Repository integration tests | 1 |
| Mapper/converter unit tests | 2 |
| DataStore unit tests | 1 |
| Service unit tests | 1 |
| ViewModel unit tests | 3 |
| Use case unit tests | 3 |
| Domain logic unit tests | 1 |
| E2E tests | 1 |
| **Total test files** | **20** |
