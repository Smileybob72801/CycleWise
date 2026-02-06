# RhythmWise Project Onboarding and Architectural Directive

This document provides the foundational context, coding standards, and a phased refactoring roadmap for the **RhythmWise** Kotlin Multiplatform (KMP) project.

Your role is to act as a **Senior Software Engineer specializing in Architecture and Code Standards**. All changes you implement must strictly adhere to the standards detailed below.

## I. Core Project Context

### 1. Application Mission & Stack

| Feature | Detail |
| :--- | :--- |
| **Application Name** | RhythmWise (A menstrual cycle tracker). |
| **Core Value** | Absolute data privacy. All data is stored **locally** using SQLCipher encryption. No cloud, no telemetry, no accounts. |
| **Technology Stack** | Kotlin Multiplatform (KMP), Android (Jetpack Compose), Persistence (Room + SQLCipher). |
| **Architecture** | Clean Architecture (Domain-Data-Presentation). Business logic resides exclusively in the `shared` module. |

### 2. Architectural Structure

The project uses scoped dependency injection (Koin). The most critical concept is the **Session Scope**:

*   **Session Scope (`SESSION_SCOPE`)**: This scope is created *only* after a successful passphrase unlock. It contains the derived encryption key, the `PeriodDatabase` instance, the repositories, and all scoped ViewModels (`TrackerViewModel`, `DailyLogViewModel`, etc.).
*   **On Logout/Timeout**: The entire Session Scope is closed, destroying all sensitive objects (including the database connection and the key in memory).

## II. Coding & Style Standards

The following directives supersede common practices and must be strictly followed to ensure a clean, maintainable KMP codebase.

### 1. State Management (MVI-Like)

Every ViewModel must follow this pattern:

| Component | Purpose | Type |
| :--- | :--- | :--- |
| **State** | The entire current UI state. | `StateFlow<UiState>` |
| **Event** | Inbound user actions (e.g., `ButtonTapped`). | `fun onEvent(event: Event)` |
| **Effect** | Outbound, one-time side effects (e.g., navigation, toasts, showing dialogs). | `SharedFlow<Effect>` |

### 2. Clean Architecture Boundary Enforcement

This is a non-negotiable rule to prevent mixing UI and Data layers:

*   **Rule**: **Composable functions and their lambda arguments (UI Callbacks) MUST NOT be suspending.**
*   **Action**: All asynchronous calls (Repository, Use Case, `delay()`, etc.) must be triggered **only** within the ViewModel's `viewModelScope.launch` blocks, initiated by a non-suspending `onEvent` call.

### 3. Kotlin & Code Hygiene

*   **Visibility**: Private backing properties must use the standard `_` prefix (e.g., `private val _uiState = MutableStateFlow(...)`).
*   **Constants**: All standalone constants must be defined in `companion object` and named `UPPER_SNAKE_CASE`.
*   **Comment Removal**: **Remove ALL conversational or temporary comments** (`// TODO:`, `// FIX:`, `// CRITICAL`, boilerplate comments, or commented-out code blocks) from the *production* codebase (this includes `composeApp/src/androidMain` and `shared/src/commonMain`).
*   **KDoc**: All major classes (`ViewModel`, `Repository`, `Use Case`) and their public methods must retain clear, concise KDoc documentation.

## III. Architectural Refactoring Roadmap

The following four phases outline the necessary architectural cleanup. We will tackle them sequentially.

### **Phase 1: Groundwork & Domain Modeling (Current Focus)**

| Step | File to Refactor | Directive & Focus |
| :--- | :--- | :--- |
| **1.1** | `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/models/DailyEntry.kt` | **Code Hygiene:** Remove all `TODO:`s and non-essential commentary. Standardize KDoc. |
| **1.2** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/di/AppModule.kt` | **DI Hygiene:** Remove boilerplate and unnecessary comments. Standardize constants. |
| **1.3** | `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/repository/PeriodRepository.kt` | **KDoc & Naming:** Ensure all public methods have clear KDoc. Enforce naming conventions. |

### **Phase 2: Data Access and Use Cases**

| Step | File to Refactor | Directive & Focus |
| :--- | :--- | :--- |
| **2.1** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/androidData/repository/RoomPeriodRepository.kt` | **Code Hygiene:** Remove all internal comments, especially `// ---`, `// NOTE:`, and specific generator names in `seedDatabaseForDebug`. Enforce method KDoc. |
| **2.2** | `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/usecases/GetOrCreateDailyLogUseCase.kt` | **Code Hygiene & Logic:** Remove all internal logic/commentary (e.g., `// TODO: Remove day in cycle, this no longer makes sense...`). |
| **2.3** | `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/usecases/AutoCloseOngoingPeriodUseCase.kt` | **Code Hygiene:** Remove all internal comments and enhance KDoc for clarity. |

### **Phase 3: State Management Standardization (Isolated ViewModels)**

| Step | File to Refactor | Directive & Focus |
| :--- | :--- | :--- |
| **3.1** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/auth/PassphraseEvent.kt` & `PassphraseScreen.kt` | **MVI/Effect Pattern:** Rename `unlockSuccess` to `PassphraseEffect` (sealed interface). Update the UI screen to consume the new `Effect` flow. |
| **3.2** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/auth/PassphraseViewModel.kt` | **MVI/Effect Pattern:** Implement the `_effect` flow. Remove the internal `error` state from `UiState` and emit it as an `Effect` (Toast). |
| **3.3** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/log/DailyLogEvent.kt` & `DailyLogScreen.kt` | **MVI/Effect Pattern:** Rename `saveCompleteEvent` to `DailyLogEffect`. Update UI to consume the new `Effect` flow. |
| **3.4** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/log/DailyLogViewModel.kt` | **Logic & MVI:** Implement the `_effect` flow. Encapsulate the `isLogEmpty(log)` check inside the `viewModelScope.launch` block (it was synchronous before an async call). |

### **Phase 4: Main Screen and Obsolete Code Removal**

| Step | File to Refactor | Directive & Focus |
| :--- | :--- | :--- |
| **4.1** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/tracker/TrackerEvent.kt` | **Obsolete Removal:** Delete the obsolete `DateTapped` event. Add new events for `EditLogClicked` and `DeletePeriodRequest`/`Confirmed`. Define `TrackerEffect`. |
| **4.2** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/tracker/TrackerViewModel.kt` | **MVI & Boundary:** Implement `_effect` flow. Refactor `handleDateTap` logic to emit `TrackerEffect` (either `ShowLogSummarySheet` or `NavigateToDailyLog`). |
| **4.3** | `composeApp/src/androidMain/kotlin/com/veleda/cyclewise/ui/tracker/TrackerScreen.kt` | **Boundary Enforcement:** Update all UI callbacks (`onDateTap`, `onEditClick`, `onDeleteRequest`) to be non-suspending and call the ViewModel's `onEvent` handlers directly. Update `LaunchedEffect` to handle new `TrackerEffect` flow. |

## IV. Immediate Action

**Your immediate task is to perform the refactoring for Phase 1, Step 1.1:**

1.  **File**: `shared/src/commonMain/kotlin/com/veleda/cyclewise/domain/models/DailyEntry.kt`
2.  **Action**: Remove the three conversational `// TODO:` comments and ensure the KDoc is concise.

Please provide the content of the modified file in response.