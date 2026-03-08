# Goodnight Tasks

Self-contained prompts for unattended Claude CLI sessions. Copy one prompt into a new
conversation, then walk away. Each task is designed to be thorough, methodical work
that benefits from running autonomously.

**Usage:** Open a new Claude CLI session, paste the prompt block, hit Enter, go to bed.

---

## 1. KDoc Audit

**Effort:** High (full codebase sweep)

```
Perform a thorough KDoc documentation audit of the entire RhythmWise codebase.

Read docs/architecture/DOCUMENTATION_GUIDELINES.md first — it defines exactly what must be
documented, what must not be documented, and the formatting rules.

Then systematically walk every file in shared/src/commonMain/ and composeApp/src/androidMain/
(excluding generated code, build directories, and test files):

1. For every public class, interface, object, and enum: verify a class-level KDoc exists that
   explains purpose, lifetime/scope, and key invariants. If missing, write one following the
   guidelines.

2. For every public function with non-trivial behavior: verify KDoc with @param, @return, side
   effects, and threading expectations. Add missing KDoc.

3. For complex private helpers (algorithms, magic numbers, multi-step logic): verify KDoc exists.
   Add it if missing.

4. For every EXISTING KDoc comment: verify it is accurate against the current code. Fix any stale
   or incorrect documentation. Remove KDoc that violates the "Never Document" rules (trivial
   getters, self-evident one-liners, Compose screen functions, code that restates what the code
   literally does).

5. Verify all KDoc follows the style rules: third person, present tense, [SquareBrackets] for type
   links, @property tags for data class fields, **bold** for invariants.

Do NOT document test files. Do NOT add KDoc to Compose @Composable screen functions.
Write a commit message when finished. Run the full test suite to verify nothing broke.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 2. DeveloperOnboarding.md Audit

**Effort:** High (deep cross-referencing)

```
Perform a comprehensive audit of docs/DeveloperOnboarding.md.

Read the entire file first, then systematically verify every claim against the actual codebase:

1. Every file path mentioned — verify the file exists at that exact path. Fix any stale paths.

2. Every class name, function name, or interface referenced — verify it exists and the description
   matches reality. Fix inaccuracies.

3. Every architectural claim (DI scopes, navigation routes, database setup, encryption flow) —
   trace the actual code path and verify the documentation matches. Fix any drift.

4. Every code snippet or example — verify it compiles conceptually and reflects current patterns.
   Update outdated examples.

5. Check for missing content: are there significant architectural patterns, modules, or workflows
   that a new developer would need to know but aren't documented? Add them.

6. Check section organization — is the flow logical for a new developer? Suggest restructuring if
   needed.

Do NOT rewrite the entire document from scratch. Make surgical, targeted fixes. Preserve the
existing voice and structure where it's correct. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 3. Test Coverage Gap Fill

**Effort:** High (write many tests)

```
Find and fill test coverage gaps across the RhythmWise codebase.

Read docs/testing/TESTING_STRATEGY.md first for conventions (naming, structure, test data builders).

Systematically audit every source file in shared/src/commonMain/ and composeApp/src/androidMain/:

1. For each public method on a class/interface, check if a corresponding test exists in the
   matching test source set. A "corresponding test" means at least one test that exercises that
   method's happy path.

2. For methods that have tests, check if edge cases are covered: null inputs, empty collections,
   boundary values, error paths.

3. Write missing tests following these conventions:
   - Test method naming: targetMethod_WHEN_condition_THEN_expected()
   - Structure: // ARRANGE, // ACT, // ASSERT comments
   - Use TestData.INSTANT and TestData.DATE for time values (never Clock.System)
   - Use factory functions from TestEntityBuilders.kt and TestDomainBuilders.kt
   - For ViewModel tests, use KoinTestRule and UnconfinedTestDispatcher
   - For DAO tests, use testDatabaseModule and Robolectric
   - Use Turbine for Flow testing
   - Use MockK for mocking

4. After writing tests, run the full test suite:
   ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
   Fix any failures.

Focus on domain logic (use cases, services, mappers, converters, ViewModels, insight generators)
first. Do not write tests for trivial getters/setters or simple data classes.
Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 4. Security Invariant Verification

**Effort:** Medium (focused cross-referencing)

```
Perform a security audit by cross-referencing docs/SECURITY_MODEL.md claims against actual code.

For each claim in SECURITY_MODEL.md, trace the implementation and verify:

1. Key Derivation — Verify Argon2id parameters (memory 64MB, iterations 3, parallelism 1,
   128-bit salt, 256-bit output) match the actual KDF implementation code. Report any mismatches.

2. Encryption — Verify SQLCipher AES-256-GCM is actually configured. Check the database factory
   and cipher setup code.

3. Key Zeroization — Verify the derived key is actually zeroized (fill(0)) after database open
   in createDatabaseAndZeroizeKey(). Check there are no other code paths where the key could
   leak or persist.

4. Session Scope Destruction — Verify that logout and autolock actually destroy the session scope,
   close the database, and release all references. Trace the full teardown path.

5. No Network — Verify AndroidManifest.xml has no INTERNET permission. Check for any transitive
   dependency that might add network permissions. Verify the ManifestPermissionTest exists and
   tests this.

6. No Recovery — Verify there are no backup services, account linkage, or any mechanism that could
   expose data outside the device.

7. FLAG_SECURE — Verify it's actually set on the activity window.

8. Check for any security-relevant code that is NOT documented in SECURITY_MODEL.md and add it.

If you find any claim that doesn't match reality, fix the documentation OR the code (depending on
which is correct — use your judgment, but prefer fixing docs if the code is intentionally
different). Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 5. Accessibility Audit

**Effort:** High (every Compose screen)

```
Perform a comprehensive accessibility audit of all Compose UI in composeApp/src/androidMain/.

Walk every @Composable function that renders user-visible UI:

1. Content Descriptions:
   - Every Icon and Image must have a meaningful contentDescription (or null for decorative icons
     with semantics(contentDescription = null) or clearAndSetSemantics{}).
   - Check that contentDescription values are loaded via stringResource(), not hardcoded English.

2. Semantics:
   - Interactive elements (buttons, toggles, checkboxes) must have semantic roles.
   - Custom clickable elements must use Modifier.clickable or Modifier.selectable with proper
     role and onClickLabel.
   - Groups of related content should use Modifier.semantics(mergeDescendants = true) where
     appropriate.

3. Touch Targets:
   - All interactive elements must have a minimum 48dp touch target. Check for small icons or
     text-only buttons that might be too small.

4. Color Contrast:
   - Check that text colors have sufficient contrast against their background colors.
   - Verify that information is not conveyed by color alone (use icons, labels, or patterns too).

5. Screen Reader Navigation:
   - Verify logical focus order (top-to-bottom, left-to-right for LTR).
   - Check that decorative elements are hidden from screen readers.
   - Verify headings use proper semantics for TalkBack navigation.

6. State Announcements:
   - Loading states, errors, and success messages should be announced to screen readers.
   - Dynamic content changes should use LiveRegion semantics where appropriate.

Fix all issues you find directly in the code. Use stringResource() for all new content
description strings. Run the full test suite after changes. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 6. String Resource Audit

**Effort:** Medium (systematic search & replace)

```
Find all hardcoded user-visible strings in Compose UI and migrate them to stringResource().

Search all @Composable functions in composeApp/src/androidMain/ for:

1. Text() composables with hardcoded string arguments (not stringResource())
2. contentDescription parameters with hardcoded strings
3. placeholder/label text in TextFields with hardcoded strings
4. Toast messages or snackbar messages with hardcoded strings
5. Button text with hardcoded strings
6. Dialog title/text with hardcoded strings

For each hardcoded string found:
1. Check if an equivalent string already exists in composeApp/src/androidMain/res/values/strings.xml
2. If not, add a new entry with a descriptive resource name following existing naming conventions
3. Replace the hardcoded string with stringResource(R.string.resource_name)
4. If the string has format arguments, use stringResource(R.string.resource_name, arg1, arg2)

Do NOT migrate:
- Log messages (these are developer-facing, not user-facing)
- Test strings
- String constants used for internal logic (keys, identifiers)

Run the full test suite after all changes. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 7. Dimension Token Audit

**Effort:** Medium (systematic search & replace)

```
Find all hardcoded dp values in Compose UI and migrate them to LocalDimensions tokens.

Read the Dimensions class definition first to understand available tokens:
- Search for the Dimensions data class (likely in a theme or dimensions file)
- Understand the naming conventions and available spacing/size tokens

Then search all @Composable functions in composeApp/src/androidMain/ for:

1. Hardcoded .dp values in padding, spacing, size, offset, etc.
2. Hardcoded .dp values in Modifier chains
3. Hardcoded .dp values in Arrangement.spacedBy()
4. Hardcoded .dp values in height(), width(), size()
5. Hardcoded .dp values in RoundedCornerShape()

For each hardcoded dp value:
1. Find the closest matching LocalDimensions token
2. If no suitable token exists, check if the value is truly unique or if it should be added as a
   new token to the Dimensions class
3. Replace with LocalDimensions.current.tokenName

Access dimensions via: val dimensions = LocalDimensions.current

Do NOT change dp values in:
- Preview functions
- Test files
- Non-Compose code (XML layouts if any exist)

Run the full test suite after all changes. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 8. Static Analysis Clean Sweep

**Effort:** Medium-High (fix all warnings)

```
Run all static analysis tools and fix every fixable warning.

Step 1: Run detekt
  ./gradlew detekt
  Read the full report. For each warning:
  - If it's a real code quality issue, fix it.
  - If it's a false positive that should be suppressed, add a targeted @Suppress annotation with
    a comment explaining why.
  - Do NOT regenerate the baseline to hide warnings. Fix them.

Step 2: Run Android Lint
  ./gradlew :composeApp:lintDebug
  Read the full report. For each warning:
  - Fix real issues (unused resources, deprecated API usage, missing translations, etc.)
  - For false positives, add targeted suppressions.

Step 3: Compiler warnings
  Build with: ./gradlew :composeApp:assembleDebug 2>&1 | grep -i "warning"
  Fix deprecation warnings, unchecked cast warnings, and other compiler warnings.

After all fixes:
1. Run the full test suite: ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest
2. Run detekt again to confirm zero new warnings
3. Run lint again to confirm zero new warnings

Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 9. CLAUDE.md Accuracy Audit

**Effort:** Medium (cross-referencing)

```
Verify every factual claim in CLAUDE.md against the actual codebase.

Read CLAUDE.md completely, then for each section verify:

1. Project Overview:
   - Package namespace is com.veleda.cyclewise
   - Two Gradle modules: shared/ and composeApp/
   - Encryption uses Argon2id + SQLCipher AES-256-GCM
   - Verify no network access / telemetry claims

2. Architecture:
   - Clean Architecture with MVI pattern — verify ViewModels actually use onEvent/reduce
   - StateFlow for UI state, SharedFlow(replay=0) for effects — verify in actual ViewModels
   - DI scopes match description (singleton vs session scope)
   - SESSION_SCOPE named qualifier exists
   - SessionManager is the only KoinComponent

3. Navigation:
   - NavRoute is a sealed class in ui/nav/NavRoutes.kt
   - Bottom nav has 4 tabs: DailyLogHome, Tracker, Insights, Settings — verify

4. Build & Test Commands:
   - Run each command and verify it works
   - Verify SDK targets: minSdk 26, targetSdk 35, compileSdk 35, JVM 11

5. Key Conventions:
   - Verify commit format, branching strategy
   - Verify Compose conventions (stringResource, LocalDimensions)

6. Common Gotchas:
   - InsightEngine is factory scoped — verify
   - PeriodDatabase.create() behavior — verify
   - DailyLogViewModel self-determines isPeriodDay — verify

7. Quick Recipes:
   - Trace each recipe step against actual file locations and patterns

Fix any inaccuracies you find. Add missing information that would be important for an AI
assistant working on this codebase. Remove stale information. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 10. ProGuard Rules Completeness Audit

**Effort:** Medium (systematic cross-check)

```
Verify that every reflection-dependent class has a corresponding keep rule in
composeApp/proguard-rules.pro.

Read proguard-rules.pro first to understand the current rules.

Then systematically check:

1. Room Entities:
   - Find all classes annotated with @Entity in the codebase
   - Verify each has a -keep class rule in proguard-rules.pro
   - Check the fully qualified class name matches exactly

2. Room DAOs:
   - Find all interfaces annotated with @Dao
   - Verify each has a -keep interface rule

3. Room Type Converters:
   - Find all classes annotated with @TypeConverter or referenced in @TypeConverters
   - Verify each has a -keep class rule

4. Enums used by TypeConverters or serialization:
   - Find all enum classes used in type converters or JSON serialization
   - Verify each has a -keepclassmembers enum rule

5. Room Database class:
   - Verify the @Database class has a keep rule

6. WorkManager Workers:
   - Find all classes extending Worker or CoroutineWorker
   - Verify each has a keep rule preserving the (Context, WorkerParameters) constructor

7. Any other reflection-dependent code:
   - Koin module references
   - Serialization classes
   - Any Class.forName() or similar reflection calls

For each missing rule, add it to proguard-rules.pro under the correct section header, following
the existing formatting conventions. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 11. Compose Best Practices Audit

**Effort:** High (deep analysis of every Composable)

```
Audit all Compose UI code for performance and best practices.

Walk every @Composable function in composeApp/src/androidMain/:

1. Unnecessary Recomposition:
   - Look for unstable parameters being passed to composables (List, Map, mutable objects).
     Suggest wrapping in remember {} or using toImmutableList().
   - Check for lambda allocations in composition that could be hoisted or remembered.
   - Look for expensive computations happening during composition that should use
     remember {} or derivedStateOf {}.

2. State Hoisting:
   - Verify state is hoisted to the appropriate level (not too high, not too low).
   - Check that stateless composables don't create their own state when they should receive it.

3. derivedStateOf Opportunities:
   - Find computed values that depend on state and are recalculated on every recomposition.
   - Wrap them in remember { derivedStateOf { ... } } where appropriate.

4. remember {} Usage:
   - Check for objects created during composition that should be remembered.
   - Verify remember keys are correct (not missing dependencies, not over-specifying).

5. LaunchedEffect / DisposableEffect:
   - Verify effect keys are correct.
   - Check for effects that should restart when certain values change but don't.
   - Look for effects with missing cleanup in DisposableEffect.

6. Modifier Best Practices:
   - Check modifier ordering (background before padding vs after padding).
   - Look for duplicate modifiers or conflicting modifiers.
   - Verify Modifier parameter is the first optional parameter and defaults to Modifier.

7. Lazy List Performance:
   - If LazyColumn/LazyRow is used, verify items have stable keys.
   - Check for expensive operations inside item {} blocks.

Fix real issues. Do not over-engineer or add premature optimizations. Focus on patterns that
genuinely cause performance problems. Run the full test suite after changes.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 12. Error Handling & Edge Case Audit

**Effort:** High (deep analysis)

```
Audit the entire codebase for error handling gaps and edge cases.

Systematically check:

1. Swallowed Exceptions:
   - Search for empty catch blocks or catch blocks that only log without re-throwing or handling.
   - Search for runCatching {} usages that ignore the failure case.
   - Verify each catch block has appropriate handling (recovery, user notification, or rethrow).

2. Null Safety:
   - Look for force unwraps (!!) that could be replaced with safe alternatives.
   - Check for nullable values used without null checks in business logic.
   - Verify ?.let {} chains handle the null case when it matters.

3. Collection Edge Cases:
   - Look for .first(), .last(), .single() on potentially empty collections.
   - Check for index-based access without bounds checking.
   - Verify empty collection handling in algorithms and UI.

4. Date/Time Edge Cases:
   - Leap year handling in cycle calculations.
   - Month boundary crossing.
   - Year boundary crossing.
   - Single-day periods vs multi-day periods.

5. Database Edge Cases:
   - Concurrent access patterns.
   - Transaction failure handling.
   - Migration edge cases with existing data.

6. UI State Edge Cases:
   - Loading states — is there always a loading indicator?
   - Empty states — what happens when there's no data?
   - Error states — are errors shown to the user?
   - Rapid user input — are there debounce mechanisms where needed?

Fix real issues. Do not add defensive programming for impossible cases. Focus on scenarios
that could actually occur in production. Run the full test suite after changes.
Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 13. Thread Safety & Coroutine Audit

**Effort:** Medium-High (focused analysis)

```
Audit all coroutine and threading code for correctness.

Systematically check:

1. Dispatcher Usage:
   - Verify CPU-bound work runs on Dispatchers.Default.
   - Verify IO-bound work (file access, encryption) runs on Dispatchers.IO.
   - Verify UI updates happen on Dispatchers.Main.
   - Check for blocking calls on Main dispatcher.

2. SharedFlow / StateFlow:
   - Verify MutableStateFlow updates use update {} (atomic) instead of .value = (non-atomic).
   - Check SharedFlow replay and buffer overflow policies are correct.
   - Verify no StateFlow is used for one-shot events (should be SharedFlow with replay=0).

3. Race Conditions:
   - Look for shared mutable state accessed from multiple coroutines without synchronization.
   - Check for check-then-act patterns that could race.
   - Verify atomic operations where needed.

4. Cancellation Handling:
   - Verify long-running operations check for cancellation (isActive or ensureActive()).
   - Check that resources are cleaned up on cancellation (use try/finally or invokeOnCompletion).
   - Verify withContext blocks handle CancellationException correctly.

5. Structured Concurrency:
   - Verify coroutines are launched in appropriate scopes (viewModelScope, not GlobalScope).
   - Check for coroutine leaks (launched coroutines that outlive their scope).
   - Verify SupervisorJob usage where child failure shouldn't cancel siblings.

6. Room Threading:
   - Verify Room queries are called from suspend functions or Flow collectors.
   - Check that @Transaction methods are used for multi-query operations.

Fix real issues. Run the full test suite after changes. Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 14. ARCHITECTURE.md & CODE_STYLE.md Audit

**Effort:** Medium (cross-referencing)

```
Audit docs/ARCHITECTURE.md and docs/CODE_STYLE.md for accuracy and completeness.

Part 1: ARCHITECTURE.md
Read the entire file, then verify every claim:

1. Module structure — verify shared/ and composeApp/ descriptions match reality.
2. Layer descriptions — verify each architectural layer (UI, ViewModel, UseCase, Repository, DAO)
   matches the actual code structure.
3. Dependency flow — trace actual imports to verify the described dependency direction.
4. Package structure — verify listed packages exist and contain what's described.
5. Diagram accuracy — verify any ASCII or textual diagrams match the code.
6. Technology choices — verify listed libraries, frameworks, and versions are current.
7. Missing patterns — check if significant architectural patterns exist in code but aren't
   documented (e.g., provider interfaces, insight engine, session management).

Part 2: CODE_STYLE.md
Read the entire file, then verify:

1. Kotlin style rules — check that the codebase actually follows these rules consistently.
   Note any widespread violations.
2. Compose conventions — verify against actual Compose code.
3. Naming conventions — spot-check across the codebase.
4. Testing conventions — verify against actual test files.
5. Missing conventions — check if there are de facto conventions in the code that aren't
   documented (e.g., import ordering, file organization within packages).

Fix inaccuracies in the documentation. Add missing content that would help developers maintain
consistency. Do not change the codebase to match docs — fix the docs to match the codebase
(unless the code is clearly wrong). Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```

---

## 15. Room Query & Migration Audit

**Effort:** Medium (focused analysis)

```
Audit all Room database queries and migrations for correctness and efficiency.

Part 1: Query Audit
Find all @Dao interfaces and review every @Query:

1. SQL Correctness:
   - Verify JOIN conditions are correct (no accidental cross joins).
   - Check WHERE clauses for logic errors.
   - Verify ORDER BY clauses match documented sorting guarantees.
   - Check for missing NULL handling in queries.

2. Index Usage:
   - Check if frequently queried columns have indices.
   - Verify composite indices match common query patterns.
   - Look for queries that would benefit from new indices.

3. Performance:
   - Look for N+1 query patterns (querying in a loop).
   - Check for SELECT * when only specific columns are needed.
   - Verify LIMIT/OFFSET usage for large result sets.

4. Conflict Strategies:
   - Verify @Insert(onConflict = ...) strategies are appropriate.
   - Check @Update and @Delete behavior with missing records.

5. Flow Queries:
   - Verify Flow-returning queries are used for reactive UI updates.
   - Check that suspend queries are used for one-shot operations.

Part 2: Migration Audit
Find all migration files and review:

1. Migration Completeness:
   - Verify each migration handles all schema changes (new columns, changed types, new tables).
   - Check that default values are provided for new NOT NULL columns.
   - Verify existing data is preserved during migrations.

2. Migration Chain:
   - Verify migrations form a continuous chain from version 1 to current.
   - Check that no migration version numbers are skipped.
   - Verify the @Database version annotation matches the latest migration target.

3. Migration Tests:
   - Check that each migration has a corresponding test.
   - Verify tests cover edge cases (empty tables, tables with data).

4. Schema Export:
   - Verify schema JSON files in composeApp/schemas/ are up to date.

Fix real issues in queries and migrations. Be very careful with migration changes — incorrect
migrations can corrupt user data. Run the full test suite after changes.
Write a commit message when finished.

This task is meant to run with no supervision. Don't use commands that require user confirmation — you will hang all night if you have to ask. Precheck your local settings (settings.local.json) to see what you can use — DO NOT USE ANY other confirmable commands not on this list.
```
