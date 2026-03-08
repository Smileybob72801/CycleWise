# RhythmWise Issue Writing Guide

A well-written issue is one of the most valuable contributions you can make to the project. It makes it easier to understand, prioritize, and implement fixes and features. This guide provides a set of best practices for writing high-quality issues for RhythmWise.

Before creating an issue, please **search the existing issues** to see if a similar one has already been reported.

## The Issue Title

A good title gives a high-level summary of the issue at a glance. We use a `[Type] <Subject>` format.

#### **[Type]**

The type tag helps categorize the issue. Please choose one of the following:

-   `[Feature]`: For a new feature or enhancement that adds user-facing value.
-   `[Bug]`: For a defect or unexpected behavior in the application.
-   `[Refactor]`: For improving the internal structure or implementation of existing code without changing its external behavior.
-   `[Chore]`: Tasks for project health: updating dependencies, builds, tooling, and tests.
-   `[Docs]`: For writing or updating documentation.

#### **<Subject>**

The subject should be a concise, imperative summary of the goal.
-   **Good:** `[Feature] Implement Medical Report PDF Export`
-   **Bad:** `[Feature] Medical report exporting`

## Anatomy of a Feature Request

Our feature request template is designed to capture the "Why," "What," and "How" of an idea.

#### ### Is your feature request related to a problem? Please describe.

**Purpose:** To explain the motivation behind the feature. Every feature should solve a real problem for a user or a developer. This section justifies the work.

-   **Good:** "Users can track detailed health data but have no way to share it with their doctor in a readable format. The current tools are only for developer backups."
-   **Bad:** "I think we should have a PDF export."

#### ### Describe the solution you'd like

**Purpose:** To describe the proposed feature from a user's perspective. How will it work? What will the user see and do? This helps everyone on the team understand the desired outcome.

-   **Good:** "I'd like a new 'Export Report' button in Settings. It will ask for a date range and then generate a multi-page PDF with a summary and daily logs, which can be shared."
-   **Bad:** "Make a PDF."

#### ### Acceptance Criteria

**Purpose:** This is the most critical section for implementation. It is a technical checklist that defines what "done" means. It forces the author to think through the implementation details and provides a clear roadmap for the developer.

-   **Good:** A detailed, bulleted list broken down by module and layer (e.g., "In `RoomPeriodRepository.kt`, add a new function `getLogsForDateRange(...)`").
-   **Bad:** "Code the feature." or "The report should be generated."

## Anatomy of a Bug Report

A good bug report is reproducible. The template helps ensure that anyone on the team can replicate the bug and verify the fix.

#### ### Steps to Reproduce

**Purpose:** To provide a precise, step-by-step recipe for triggering the bug. If we can't reproduce it, we can't fix it.

-   **Good:** "1. Start a new cycle on today's date. 2. Tap the 'End Cycle Today' button. 3. Re-open the app. 4. The cycle that was just ended is still showing as ongoing."
-   **Bad:** "The end cycle button is broken."

#### ### Expected vs. Actual Behavior

**Purpose:** To clearly state the discrepancy between what you thought would happen and what actually did. This removes ambiguity.

-   **Good:** "**Expected:** The cycle should be marked with an end date. **Actual:** The cycle's end date remains null in the UI."
-   **Bad:** "It didn't work right."

---

Thank you for taking the time to write a detailed issue. It is a massive help in making RhythmWise a better, more stable application!