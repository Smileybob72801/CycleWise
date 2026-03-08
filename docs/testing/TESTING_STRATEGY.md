# Testing Strategy

## Test Pyramid

```
        /   E2E   \         ← 1 smoke test (device required)
       /-----------\
      / Integration \       ← DAO + Repository tests (Robolectric + Room)
     /---------------\
    /    Unit Tests   \     ← Mappers, converters, use cases, VMs, services
   /-------------------\
```

## What Must Be Tested

### Unit Tests (fast, no Android framework)
- **Mappers & Converters** — Every entity↔domain mapping function
- **Use Cases** — All business logic paths (happy + error)
- **ViewModels** — State transitions, effect emissions, event handling
- **Services** — Key derivation, data transformations
- **Insight Generators** — Algorithm correctness with known inputs

### Integration Tests (Robolectric + in-memory Room)
- **DAOs** — All CRUD operations, query edge cases, conflict strategies
- **Repository** — End-to-end data flow through Room, reactive Flow emissions

### E2E Tests (device/emulator required)
- **Smoke path** — Unlock → create cycle → create daily log → verify calendar

## Conventions

### Naming
All test methods must follow the pattern:
```
targetMethod_WHEN_condition_THEN_expected()
```

Examples:
- `insert_WHEN_entryIsNew_THEN_addsToDatabase()`
- `deriveKey_WHEN_calledWithSamePassphraseAndSalt_THEN_producesSameKey()`

### Structure
Every test must use `// ARRANGE`, `// ACT`, `// ASSERT` comments:
```kotlin
@Test
fun targetMethod_WHEN_condition_THEN_expected() = runTest {
    // ARRANGE
    val input = buildEntity(...)

    // ACT
    val result = sut.doSomething(input)

    // ASSERT
    assertEquals(expected, result)
}
```

### Time & Dates
- **Never** use `Clock.System.now()` or `Clock.System.todayIn()` in test data construction
- Use `TestData.INSTANT` and `TestData.DATE` from the shared `testutil/` package
- Exception: E2E tests may use real dates when interacting with the live calendar UI

### Database Setup
- Use the shared `testDatabaseModule` from `testutil/TestDatabaseModule.kt`
- Use `KoinTestRule` to manage Koin lifecycle — never manually call `startKoin`/`stopKoin`
- Always close the database in `@After`

### Coroutines
- Use `= runTest { }` (assignment form) as the standard coroutine test wrapper
- For ViewModel tests: `UnconfinedTestDispatcher` with `Dispatchers.setMain`/`resetMain`

### Test Data Builders
- Use factory functions from `TestEntityBuilders.kt` and `TestDomainBuilders.kt`
- Only specify parameters that are relevant to the test scenario
- Let builders provide sensible defaults for everything else

## File Organization

| Source Set | Contents |
|-----------|----------|
| `composeApp/src/androidUnitTest/` | Unit + integration tests for Android-specific code |
| `shared/src/androidUnitTest/` | Unit tests for platform-agnostic domain logic |
| `composeApp/src/androidTest/` | Instrumented E2E tests (require device) |
