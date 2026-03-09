# RhythmWise Issue Writing Guide

A well-written issue is one of the most valuable contributions you can make. It determines how quickly a feature gets prioritized, how smoothly it gets implemented, and whether the result actually solves the right problem. Poor issues create back-and-forth; great issues create momentum.

Before creating an issue, **search existing issues** to avoid duplicates. If a related issue exists, add your context there instead of opening a new one.

## Choosing the Right Template

When you create a new issue on GitHub, you will be presented with two structured templates:

- **Feature Request** — for new functionality or enhancements
- **Bug Report** — for defects or unexpected behavior

Select the one that matches your intent. The templates provide labeled fields with placeholders — this guide explains how to fill them out *well*.

## Title Conventions

Both templates pre-fill a `[Type]` prefix. Keep it, and write the rest as an **imperative summary** — a command that describes the goal.

| Prefix | Use for |
|---|---|
| `[Feature]` | New functionality or enhancements |
| `[Bug]` | Defects or unexpected behavior |
| `[Refactor]` | Internal improvements with no behavior change |
| `[Chore]` | Dependency updates, tooling, build config |
| `[Docs]` | Documentation additions or corrections |

**Good:** `[Feature] Add cycle-length trend chart to Insights tab`
**Bad:** `[Feature] chart stuff` — too vague to triage without opening the issue

The title should be specific enough that someone scanning the issue list can understand the scope without clicking in.

---

## Feature Request Fields

### Is your feature request related to a problem? Please describe.

*Required*

This field asks for the **why** — the motivation behind the feature.

**Why it matters:** Every feature has an implementation cost, a maintenance cost, and a complexity cost. This section is how the team decides whether the work is justified. If you can't articulate a real problem, the feature probably doesn't need to exist. It also helps prioritizers gauge urgency: "users have no way to share data with their doctor" is more urgent than "it would be cool if..."

**Write it well:**
- Name who is affected and how (users, developers, both)
- Describe the current limitation and its real-world impact
- Avoid jumping to solutions here — stay in problem space

**Good:** "Users can track detailed symptom data daily but have no way to see trends over time. They have to manually scroll through weeks of logs to notice patterns, which defeats the purpose of consistent tracking."

**Bad:** "We need a chart." — This describes a solution, not a problem. It gives the team nothing to prioritize against.

### Describe the solution you'd like

*Required*

This field asks for the **what** — the desired outcome from the user's perspective.

**Why it matters:** A clear solution description keeps scope focused and prevents features from expanding during implementation. It gives the developer a concrete user story to build against and the reviewer a scenario to test. Without it, "add insights" could mean anything from a single stat to an entire analytics dashboard.

**Write it well:**
- Describe what the user will see and do, step by step
- Stay at the user-experience level — save implementation details for Acceptance Criteria
- If there are multiple possible approaches, state which one you're proposing and briefly note the alternatives

**Good:** "A new 'Trends' card on the Insights tab that shows a line chart of cycle length over the last 6 cycles. Tapping the card expands it to full-screen with axis labels and the ability to select different metrics."

**Bad:** "Add a chart component." — This is an implementation task, not a user outcome.

### Acceptance Criteria

*Required*

This is the **implementation contract**. It defines what "done" means.

**Why it matters:** This is the section the developer builds against, the reviewer tests against, and the issue closer checks off. Vague criteria lead to incomplete implementations, missed edge cases, and issues that get reopened. The template's module breakdown (Data Model → Persistence → Logic → UI → Testing) mirrors the project's Clean Architecture layers — following it ensures nothing gets missed across the stack.

**Write it well:**
- Use the template's layer-by-layer structure — it exists to prevent gaps
- Each bullet should be specific and verifiable: "add a `getCycleLengths(limit: Int): List<Int>` function to `PeriodRepository`" not "add repo method"
- Include edge cases: what happens with zero data? With one cycle? With 100?
- The Testing section is not optional in practice — specify what kinds of tests are needed (unit, integration, UI)
- If a bullet requires a database change, note the migration

**Good:**
```
- **Data Model (`shared`):**
  - Add `CycleLengthTrend` data class with `cycleNumber: Int`, `lengthDays: Int`, `startDate: LocalDate`
- **Logic (`shared`):**
  - Add `GetCycleLengthTrendsUseCase` that returns the last N completed cycles with their lengths
  - Handle edge case: fewer than 2 completed cycles returns empty list
- **UI (`composeApp`/Compose):**
  - Add `CycleTrendCard` composable to InsightsScreen
  - Use Vico `CartesianChartModelProducer` for the line chart
- **Testing:**
  - Unit test `GetCycleLengthTrendsUseCase` with 0, 1, and 6+ cycles
  - Robolectric integration test for `CycleTrendCard` rendering
```

**Bad:** "Implement the feature and add tests." — This tells the developer nothing they didn't already know.

### Technical Implementation Notes *(optional)*

**Why it matters:** RhythmWise has specific architectural patterns (Koin session scoping, provider interfaces, MVI ViewModels) and security invariants (no network, SQLCipher encryption, passphrase keys never persisted). This field prevents a contributor from reinventing an existing utility, violating a constraint they didn't know about, or choosing an approach that conflicts with the architecture.

**Write it well:**
- Reference existing code that should be reused or extended (e.g., "reuse `InsightGenerator` interface pattern from `shared/.../domain/insights/`")
- Call out relevant constraints from CLAUDE.md (e.g., "must remain in session scope — no singleton injection")
- Mention performance considerations if the feature touches large datasets
- Suggest libraries only if there's a strong reason to prefer one

**Good:** "The chart should use the existing Vico 2.1.2 dependency — see `ui/insights/charts/` for the established pattern. The use case must be registered in session scope in `AppModule.kt` since it needs DB access via `PeriodRepository`."

**Bad:** "Use a good library." — Not actionable.

### Additional Context *(optional)*

Use this for anything that doesn't fit the other fields: mockups, screenshots, links to related issues, future directions, or design rationale. Keep it supplementary — if information is critical to implementation, it belongs in Acceptance Criteria or Technical Notes.

---

## Bug Report Fields

### Bug Description

*Required*

A clear and concise summary of the defect. What is broken?

**Why it matters:** This is the first thing a triager reads. A good summary lets someone decide priority and assignment in seconds. It also helps duplicate detection — if someone searches for a similar bug, a clear description is more likely to surface your report.

**Write it well:**
- State what is broken, not just that something is wrong
- Name the affected feature or screen
- Keep it to 2–3 sentences — save details for the fields below

**Good:** "Symptoms logged on a day are not persisted. After navigating away from the daily log and returning, the symptom list is empty even though the save confirmation appeared."

**Bad:** "Something is broken in the app."

### Steps to Reproduce

*Required*

A precise, step-by-step recipe for triggering the bug.

**Why it matters:** RhythmWise has no network access, no telemetry, and no crash reporting — by design. The bug report is the **only signal** the team has. If we can't reproduce it, we can't diagnose it, and we can't verify the fix. Every skipped step is a potential dead end.

**Write it well:**
- Start from a known state (e.g., "with a fresh install" or "with at least 3 logged cycles")
- Number each step as a discrete action
- Include exact values you entered, not just "fill in the form"

**Good:**
```
1. Open the app with at least one completed cycle
2. Navigate to Tracker tab
3. Tap on a day in the current cycle
4. Log a symptom (e.g., headache, severity 3)
5. Press back to return to Tracker
6. Tap the same day again
7. The symptom logged in step 4 is not shown
```

**Bad:** "Symptoms disappear sometimes." — This could take hours to reproduce, if it's even possible.

### Expected Behavior

*Required*

What you believed should have happened.

**Why it matters:** Not all unexpected behavior is a bug — sometimes it's an intentional design decision the user disagrees with, or a misunderstanding of the feature. Stating your expectation explicitly removes ambiguity and helps the team distinguish between "broken" and "works differently than expected."

**Good:** "After logging a symptom and returning to the same day, the symptom should still appear in the daily log with the severity I set."

**Bad:** "It should work." — Work how?

### Actual Behavior

*Required*

What actually happened instead.

**Why it matters:** Combined with Expected Behavior, this creates a clear before/after that pinpoints the defect. It also helps the team assess severity — "data is silently lost" is critical; "icon color is slightly off" is cosmetic.

**Write it well:**
- Be specific: include error messages verbatim, UI states, or data inconsistencies
- If applicable, note whether the data is actually lost or just not displayed (check by navigating away and back)

**Good:** "The daily log for that day shows no symptoms. Navigating away and back to the day still shows an empty symptom list. The symptom appears to not have been persisted."

**Bad:** "It broke."

### Reproducibility

*Required*

A dropdown indicating how often the bug occurs: Always (100%), Often (~75%), Sometimes (~50%), Rarely (~25%), or Unable to reproduce (happened once).

**Why it matters:** Deterministic bugs (Always) and intermittent bugs (Sometimes/Rarely) require fundamentally different debugging approaches. A 100% reproducible bug can be debugged with a breakpoint; a rare bug may need logging, stress testing, or race-condition analysis. This field lets the team set realistic expectations for turnaround time.

### Severity

*Required*

A dropdown self-assessment of impact: Crash / Data Loss, Major (feature broken, no workaround), Moderate (feature broken, workaround exists), Minor (cosmetic / UI glitch), or Trivial.

**Why it matters:** Combined with Reproducibility, severity drives prioritization. A crash that always happens is a release blocker; a cosmetic glitch that happens rarely goes to the backlog. The structured dropdown also enables filtering the issue list by severity.

**Choose honestly:**
- **Crash / Data Loss** — The app crashes, force-closes, or user data is lost or corrupted
- **Major** — A feature is completely broken with no way around it
- **Moderate** — A feature is broken but there's a workaround
- **Minor** — Cosmetic issue, misalignment, wrong color, typo
- **Trivial** — Negligible impact, polish-level issue

### Workaround *(optional)*

Any temporary workaround you've discovered.

**Why it matters:** This field serves double duty. For triage, it signals that a bug may be less urgent if users can work around it. For other users hitting the same bug before a fix ships, it provides immediate relief. Even "None found" is useful — it confirms you looked.

### App Version

*Required*

Found in Settings (e.g., `1.0.0-beta.1`).

**Why it matters:** For a pre-1.0 app with active development, the version is essential context. A bug in `beta.1` may already be fixed in `beta.2`. This field is required because version-less bug reports are nearly impossible to act on during rapid iteration.

### Device and Android Version *(optional but strongly recommended)*

**Why it matters:** SQLCipher encryption, Room database behavior, and Jetpack Compose rendering can vary across Android versions and OEM skins. A bug on a Samsung device running Android 12 may not reproduce on a Pixel running Android 14. These fields help the team determine whether the issue is device-specific, version-specific, or universal — which fundamentally changes the debugging approach and fix strategy.

- **Device:** Manufacturer and model (e.g., `Pixel 8`, `Samsung Galaxy S23`)
- **Android Version:** OS version (e.g., `Android 14`)

### Screenshots or Recordings *(optional)*

Drag and drop images or screen recordings that show the bug. Especially helpful for UI issues where describing the visual problem in words is less effective than showing it.

### Logs or Stack Traces *(optional)*

If you captured a stack trace or logcat output, paste it in this field. The template auto-formats it as a code block, so you don't need to add markdown fencing.

**Why it matters:** A stack trace can cut debugging time from hours to minutes. If you see a crash dialog or can capture logcat output, include it — even partial traces are valuable.

### Additional Context *(optional)*

Anything that doesn't fit the other fields: related issues, when the bug first appeared, whether it started after an update, or configuration details. Keep it supplementary — if information is critical to reproduction, it belongs in Steps to Reproduce.

### Checklist

Two checkbox items at the bottom of the template:

- **"I have searched existing issues and confirmed this is not a duplicate"** *(required)* — Duplicate bugs are the most common source of issue noise. This prompt ensures the reporter checks first.
- **"This bug involves data loss or corruption"** *(optional)* — A quick triage signal. Data loss bugs get escalated regardless of the severity dropdown selection, since RhythmWise has no cloud backup or recovery mechanism by design.

---

Thank you for taking the time to write detailed issues. Each well-crafted report directly improves RhythmWise for everyone who uses it.
