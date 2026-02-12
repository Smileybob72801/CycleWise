# Documentation Guidelines

Standards for KDoc documentation across the RhythmWise codebase. All subsequent documentation PRs reference this guide.

---

## What Must Be Documented

### Always Document
- **Public classes and interfaces** — Class-level KDoc explaining purpose, lifetime, and key invariants
- **Public functions with non-trivial behavior** — `@param`, `@return`, side effects, threading expectations
- **Domain invariants** — Value ranges (e.g., `moodScore` 1-5), state transitions, sorting guarantees
- **Repository contracts** — Cold Flow vs suspend, nullability semantics, transactional expectations
- **Complex private logic** — Algorithms with magic numbers, multi-step state machines, normalization formulas
- **Security-critical code** — KDF parameters, encryption setup, key lifecycle, passphrase handling

### Never Document
- Trivial getters/setters where the name is self-explanatory
- Self-evident one-liner functions (e.g., `fun toEntity() = Entity(id, name)`)
- Test files (use descriptive test names instead)
- Compose `@Composable` screen functions (their behavior is defined by ViewModel contracts)
- Code that restates what the code literally does (e.g., `/** Returns the id. */ fun getId() = id`)

---

## KDoc Formatting Rules

### Class-Level Documentation
Every public class/interface gets a class-level KDoc block explaining:
1. What it represents or does
2. Its lifetime/scope (singleton, session-scoped, per-screen)
3. Key invariants or constraints

```kotlin
/**
 * Room entity representing a single menstrual period.
 *
 * Mapped to the `periods` table. Uses an auto-generated [id] for internal Room joins
 * and an exposed [uuid] (TEXT UNIQUE) for domain-layer identification.
 *
 * **Invariant:** [startDate] <= [endDate] when [endDate] is non-null.
 * A null [endDate] indicates an ongoing period.
 *
 * @property id      Auto-generated internal PK for Room (fast joins/indexing).
 * @property uuid    External UUID exposed to the domain and sync layers.
 * @property startDate The first day of this period.
 * @property endDate   The last day of this period, or null if ongoing.
 * @property createdAt Timestamp when this record was first persisted.
 * @property updatedAt Timestamp of the most recent modification.
 */
```

### Domain Models
Use `@property` tags for every property with non-obvious semantics:

```kotlin
/**
 * A day's complete health entry, linked to a parent period by date proximity.
 *
 * @property id        UUID primary key.
 * @property entryDate Calendar date this entry represents.
 * @property dayInCycle 1-based offset from the parent period's start date.
 * @property moodScore  User-rated mood on a 1 (lowest) to 5 (highest) scale, or null if unrecorded.
 * @property energyLevel User-rated energy on a 1-5 scale, or null if unrecorded.
 * @property customTags  Freeform user tags stored as a JSON-serialized list of strings.
 */
```

### Repository and Interface Methods
Document `@param`, `@return`, threading model, and side effects:

```kotlin
/**
 * Marks [date] as a period day, merging or extending adjacent periods as needed.
 *
 * Runs inside a Room transaction. Handles four scenarios:
 * 1. **Already inside a period** — ensures a PeriodLog exists for the date.
 * 2. **Bridges two periods** — merges the adjacent periods into one.
 * 3. **Extends a period** — adjusts the neighboring period's start or end date.
 * 4. **Island day** — creates a new single-day completed period.
 *
 * @param date the calendar date to mark as a period day.
 */
suspend fun logPeriodDay(date: LocalDate)
```

### Use Cases
Document preconditions, postconditions, and return semantics:

```kotlin
/**
 * Starts a new menstrual period beginning on [startDate].
 *
 * **Precondition:** No ongoing period exists (endDate == null).
 * **Postcondition:** A new [Period] with a null endDate is persisted.
 *
 * @return the newly created [Period], or null if an ongoing period already exists.
 */
```

### Complex Private Helpers
Document the algorithm, magic numbers, and edge cases:

```kotlin
/**
 * Groups a sorted list of day-offsets into runs of consecutive integers.
 *
 * For example, `[1, 2, 3, 7, 8]` becomes `[[1, 2, 3], [7, 8]]`.
 * Used to identify contiguous symptom clusters within a cycle phase.
 *
 * @param days sorted, distinct normalized day offsets.
 * @return a list of consecutive-day groups, each sorted ascending.
 */
private fun groupConsecutiveDays(days: List<Int>): List<List<Int>>
```

---

## Threading and Coroutine Rules

- **`suspend` functions** — May be called from any dispatcher unless documented otherwise. Repository `suspend` functions are safe to call from `Dispatchers.Main` (Room handles IO internally).
- **`Flow` return types** — Always cold Flows backed by Room's reactive queries. Collectors receive the latest snapshot on subscription and subsequent updates.
- **`SharedFlow` / `MutableSharedFlow`** — Document `replay` and buffer policy (e.g., "replay = 0, drops oldest on overflow").
- **`viewModelScope.launch`** — Runs on `Dispatchers.Main` by default. Document when `withContext(Dispatchers.IO)` is used for blocking work.

---

## Invariant Documentation

Always document:
- **Value ranges** — `moodScore: 1-5`, `severity: 1-5`, `cups: 0-99`
- **Nullability semantics** — `null endDate = ongoing period`, `null moodScore = unrecorded`
- **Sorting guarantees** — `getAllPeriods()` returns periods sorted by start date descending
- **State transitions** — the 4-scenario FSMs for `logPeriodDay` / `unLogPeriodDay`
- **One-row constraints** — `WaterIntake` uses date as natural key (one row per day)
- **FK relationships** — CASCADE (deleting parent deletes children) vs RESTRICT (cannot delete parent while children exist)

---

## Style Rules

- Write in third person ("Returns the period" not "Return the period")
- Use present tense ("Derives a key" not "Will derive a key")
- Reference related types with `[SquareBrackets]` KDoc links
- Keep the first sentence a concise summary (it appears in IDE quick-docs)
- Use `**bold**` for invariant keywords and scenario labels
- Prefer `@property` over inline `//` comments for data class fields
- Remove `//` comments that are superseded by KDoc
