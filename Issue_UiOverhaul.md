# [Epic] UI Polish: Library Adoption, Lottie Animations, Shimmer Loading & Calendar Upgrade

## Is your feature request related to a problem?

RhythmWise's UI is functional but lacks the visual polish expected of a production app. Several areas use static placeholders or minimal loading indicators where richer experiences are possible:

1. **Markdown rendering** — The custom `MarkdownText.kt` (88 lines) uses regex-based bold parsing. It works but is fragile, hard to extend (no support for lists, links, headers, code blocks), and represents maintenance burden for a solved problem.

2. **Empty states & loading** — Screens like Insights show a static icon when empty and a plain `CircularProgressIndicator` when loading. These feel utilitarian rather than polished.

3. **Loading transitions** — Multiple screens (`InsightsScreen`, `DailyLogScreen`, `PassPhraseScreen`, `SetupScreen`) show spinner-style loading indicators with no content preview, causing layout shift when data arrives.

4. **Calendar library** — Kizitonwose Calendar is pinned at 2.8.0 (current: 2.10.0), missing bug fixes and the `HeatMapCalendar` composable that could enable a "year at a glance" view.

## Describe the solution you'd like

A phased approach across three work streams, each independently shippable:

- **Phase 1** — Replace the custom markdown renderer with `compose-markdown` by jeziellago
- **Phase 2** — Add Lottie animations for empty states, loading, success confirmations, and onboarding; add a custom shimmer modifier for skeleton loading screens
- **Phase 3** — Bump Kizitonwose Calendar to 2.10.0 and explore `HeatMapCalendar` for a year-at-a-glance view

---

## Acceptance Criteria

### Phase 1: Markdown Library Adoption

**Goal:** Replace the 88-line custom regex markdown renderer with `compose-markdown`, reducing maintenance surface and gaining full Markdown support.

- [ ] Add JitPack repository to `settings.gradle.kts` (not currently present)
  ```kotlin
  maven("https://jitpack.io")
  ```
- [ ] Add `compose-markdown` dependency to `gradle/libs.versions.toml` and `composeApp/build.gradle.kts`
- [ ] Replace `composeApp/.../ui/components/MarkdownText.kt` with a thin wrapper around the library's `Markdown` composable
    - Preserve the existing public API signature so call sites require zero changes
    - Call sites: `LearnArticleCard`, `EducationalBottomSheet`, `SetupScreen.InfoPage`
- [ ] Delete the `parseMarkdown()` function and `boldRegex` constant (no longer needed)
- [ ] Replace `composeApp/.../ui/components/MarkdownTextKtTest.kt` (174 lines testing `parseMarkdown()` internals) with Robolectric UI tests that verify rendered output
- [ ] **Security audit:** Verify `compose-markdown` makes no network calls and does not attempt to load remote images — this is a hard constraint (no network permissions in the app)
- [ ] All existing tests pass; lint and detekt pass

### Phase 2: Lottie Animations & Custom Shimmer

This is the largest phase with the highest visual impact. It has two sub-tracks.

#### Phase 2a: Lottie Integration

**Goal:** Add Lottie animations for empty states, loading overlays, success confirmations, and onboarding illustrations.

**Setup:**
- [ ] Add `lottie-compose` dependency (v6.6.7+, available on Maven Central — no JitPack needed)
- [ ] Add version entry to `gradle/libs.versions.toml`

**Reusable components:**
- [ ] Create `composeApp/.../ui/components/LottieAnimationBox.kt` — reusable wrapper composable
    - Accepts `@RawRes animationResId`, optional `iterations` (default infinite), `modifier`, `contentDescription`
    - Handles `LottieComposition` loading and `LottieAnimation` display
    - Respects reduced motion accessibility setting (skip to final frame or show static fallback)
- [ ] Create `composeApp/.../ui/components/SuccessAnimation.kt` — overlay component for transient success confirmations
    - Plays once, then auto-dismisses via callback
    - Semi-transparent scrim background
    - Centered Lottie animation

**Lottie JSON assets (add to `composeApp/src/androidMain/res/raw/`):**
- [ ] `anim_empty_insights.json` — empty state illustration for Insights
- [ ] `anim_loading_general.json` — general loading animation
- [ ] `anim_success_checkmark.json` — success confirmation (checkmark/celebration)
- [ ] `anim_onboarding_welcome.json` — onboarding page 0 illustration
- [ ] `anim_onboarding_privacy.json` — onboarding page 1 illustration
- [ ] `anim_onboarding_tracking.json` — onboarding page 2 illustration
- [ ] All JSON files must be < 100KB each for performance
- [ ] Source animations from LottieFiles or create custom — ensure license compatibility (MIT/Apache 2.0/CC0)

**Screen integrations — Empty states:**
- [ ] `InsightsEmptyState.kt` (53 lines) — Replace static `Icon` with `LottieAnimationBox` using `anim_empty_insights.json`

**Screen integrations — Loading overlays:**
- [ ] `InsightsScreen.kt` (line ~99) — Replace `CircularProgressIndicator` with shimmer skeleton (see Phase 2b)
- [ ] `DailyLogScreen.kt` (line ~279) — Replace `CircularProgressIndicator` with shimmer skeleton (see Phase 2b)
- [ ] `PassPhraseScreen.kt` (line ~286) — Replace `CircularProgressIndicator` with `LottieAnimationBox` using `anim_loading_general.json`
- [ ] `SetupScreen.kt` (line ~195) — Replace `CircularProgressIndicator` with `LottieAnimationBox` using `anim_loading_general.json`

**Screen integrations — Onboarding illustrations:**
- [ ] `SetupScreen.kt` InfoPage pages 0-2 (lines ~120-136, composable at line ~251) — Add Lottie illustrations above text content for each onboarding page

**Screen integrations — Success confirmations:**
- [ ] Period toggle action (marking a day as period day) — Show brief `SuccessAnimation` overlay
- [ ] Onboarding completion — Show `SuccessAnimation` before navigating to main app
- [ ] Period marking on Tracker screen — Show brief `SuccessAnimation` overlay
- [ ] **Important:** Daily log auto-saves per keystroke — do NOT add success animation to daily log saves (would be disruptive)
- [ ] **Important:** Inline button spinners in `GeneralPage.kt` should remain as `CircularProgressIndicator` — that is the correct pattern for inline button loading states

**Non-goals for Lottie:**
- Coach marks system stays as-is (custom implementation, ~1000 LOC, already polished with unique features)

#### Phase 2b: Custom Shimmer Loading

**Goal:** Add skeleton loading screens with a shimmer effect to replace spinner-style loading indicators, providing content preview and eliminating layout shift.

**Shimmer modifier:**
- [ ] Create `composeApp/.../ui/components/ShimmerModifier.kt` (~40 LOC)
    - `Modifier.shimmer()` extension using `rememberInfiniteTransition` + `Brush.linearGradient`
    - Animates a highlight sweep across the composable
    - Uses `composed {}` modifier factory (acceptable perf for 3-4 cards)
    - Respects reduced motion accessibility setting

**Skeleton loaders:**
- [ ] Create `composeApp/.../ui/insights/InsightsSkeletonLoader.kt`
    - 3-4 placeholder cards mimicking `InsightCard` layout
    - Rounded rectangles with shimmer for title, body text, and icon areas
    - Matches actual card dimensions to prevent layout shift on load
- [ ] Create `composeApp/.../ui/log/DailyLogSkeletonLoader.kt`
    - Tab bar placeholder + form field skeletons
    - Matches actual daily log layout structure

**Integration:**
- [ ] `InsightsScreen.kt` (line ~99) — Replace `CircularProgressIndicator` loading branch with `InsightsSkeletonLoader`
- [ ] `DailyLogScreen.kt` (line ~279) — Replace `CircularProgressIndicator` loading branch with `DailyLogSkeletonLoader`

### Phase 3: Calendar Version Bump & HeatMap Exploration

**Goal:** Update Kizitonwose Calendar to latest and explore `HeatMapCalendar` for a year-at-a-glance feature.

**Version bump:**
- [ ] Update `gradle/libs.versions.toml` line 27: `compose-calendar = "2.8.0"` → `"2.10.0"`
- [ ] Review [Kizitonwose Calendar changelog](https://github.com/kizitonwose/Calendar/blob/main/CHANGELOG.md) for breaking API changes affecting:
    - `HorizontalCalendar` composable
    - `CalendarState` / `rememberCalendarState`
    - `DayPosition` enum
    - Day/month content lambdas
- [ ] Fix any breaking changes in:
    - `CalendarGrid.kt`
    - `CalendarDay.kt`
    - `TrackerScreen.kt`
- [ ] Regression test calendar navigation, day selection, period marking, and month transitions

**HeatMap exploration (spike):**
- [ ] Investigate `HeatMapCalendar` composable from the library
- [ ] Prototype `YearAtAGlanceCard.kt` showing 12 months in a compact grid with color-coded period days
- [ ] Document findings: API surface, customization options, data requirements
- [ ] **Note:** This is distinct from the Insights Engine Overhaul (Issue: `Issue_InsightsEngineOverhaul.md`) Phase 5 heatmap overlays. That work adds metric-specific color overlays to the existing `HorizontalCalendar`. This work explores the dedicated `HeatMapCalendar` composable for a standalone year view.

---

## Cross-Cutting Concerns

### Accessibility
- All Lottie animations must have meaningful `contentDescription` strings via `stringResource()`
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` — when reduced motion is enabled:
    - Lottie: show static final frame or fallback image
    - Shimmer: show static gray placeholders without animation
- Skeleton loaders should announce "Loading" to screen readers

### Security
- No network permissions may be added — this is a hard constraint
- `compose-markdown` must be audited to confirm it does not fetch remote resources (images, links)
- Lottie JSON files are bundled locally in `res/raw/` — no network loading
- If `compose-markdown` has a remote image loading capability, it must be disabled or the image handler overridden to no-op

### Performance
- Lottie JSON files: target < 100KB each (ideally < 50KB)
- Shimmer uses `composed {}` modifier factory — acceptable for the 3-4 skeleton cards in use; do not apply to lists with 50+ items
- `LottieAnimationBox` should use `LottieConstants.IterateForever` for looping and properly cancel on disposal

### ProGuard / R8
- Lottie does not require keep rules for JSON resources in `res/raw/`
- `compose-markdown` — verify no reflection-based parsing; add keep rules if needed
- Run `./gradlew :composeApp:assembleRelease` to verify R8 doesn't strip required classes

### Testing
- Lottie animations may not fully render under Robolectric — test composition and `contentDescription` presence, not visual output
- Shimmer modifier can be tested by verifying the modifier chain applies without crashing
- Skeleton loaders: Robolectric tests verifying composable renders and placeholder elements exist
- `compose-markdown` wrapper: Robolectric tests verifying markdown text renders (bold, lists, headers)
- Calendar: existing tracker tests should catch regressions after version bump

---

## Verification Checklist

- [ ] `./gradlew :composeApp:assembleDebug` succeeds
- [ ] `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest` passes
- [ ] `./gradlew :composeApp:lintDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew :composeApp:assembleRelease` succeeds (R8 doesn't strip Lottie resources or markdown library classes)
- [ ] Manual verification: all screens with animations, shimmer, and markdown render correctly
- [ ] Manual verification: animations respect reduced motion setting
- [ ] Manual verification: calendar navigation and period marking work after version bump

---

## Dependencies & Libraries

| Library | Version | Source | Purpose |
|---------|---------|--------|---------|
| `compose-markdown` (jeziellago) | Latest | JitPack | Markdown rendering |
| `lottie-compose` (Airbnb) | 6.6.7+ | Maven Central | Lottie animations |
| `kizitonwose/Calendar` | 2.10.0 | Maven Central | Calendar (bump from 2.8.0) |

**No new library for shimmer** — custom `Modifier.shimmer()` (~40 LOC) using `InfiniteTransition`.

**No change to coach marks** — custom implementation retained (already polished, unique features).

---

## Cross-Reference: Insights Engine Overhaul (`Issue_InsightsEngineOverhaul.md`)

Several areas of this issue overlap with the Insights Engine Overhaul epic. **If both issues are implemented, the order matters.** The recommended sequence is UI Polish Phase 1 (Markdown) and Phase 3 (Calendar bump) first, then Insights Engine Overhaul, then UI Polish Phase 2 (Lottie/Shimmer) — because the Overhaul fundamentally changes the Insights screen layout that Phase 2 targets.

### Overlap 1: `InsightsScreen.kt` — Shimmer skeleton vs. progressive disclosure redesign

- **UI Polish Phase 2b** creates `InsightsSkeletonLoader.kt` with 3-4 placeholder cards mimicking the current `InsightCard` layout and replaces the `CircularProgressIndicator` at line ~99.
- **Overhaul Phase 3** completely redesigns `InsightsScreen.kt` with a new layout: cycle summary card, data readiness countdowns, top-N insights, categorized accordion sections, and a charts section.
- **Impact:** The skeleton loader created in UI Polish Phase 2b would need to be redesigned after the Overhaul to match the new layout (summary card skeleton, category section skeletons, chart placeholders). If the Overhaul is done first, build the skeleton loader to match the new layout from the start.

### Overlap 2: `InsightsEmptyState.kt` — Lottie animation vs. data-readiness countdowns

- **UI Polish Phase 2a** replaces the static `Icon` in `InsightsEmptyState.kt` with a Lottie animation (`anim_empty_insights.json`).
- **Overhaul Phase 2.5 + Phase 3** introduces data-readiness countdown cards that replace the "no insights" empty state with actionable feedback ("12 more days of logging needed to unlock Cross-Variable Correlations").
- **Impact:** After the Overhaul, the empty state composable is rarely (if ever) shown — users see countdown cards instead. The Lottie empty state animation may only appear for brand-new users with zero data. Consider whether the Lottie animation should go on the countdown cards instead, or if `InsightsEmptyState.kt` should be kept as a true zero-data fallback.

### Overlap 3: `InsightsViewModel.kt` — Loading state changes

- **UI Polish Phase 2b** changes how loading state is consumed in the UI (shimmer instead of spinner).
- **Overhaul Phases 2-3** restructure `InsightsUiState` entirely (adding `cycleSummary`, `topInsights`, `categorizedInsights`, `dataReadiness`, `charts`, `expandedCategories`, `expandedCardIds`).
- **Impact:** Whichever ships second will need to reconcile the loading/shimmer approach with the new state structure. Minor conflict — shimmer is a UI-layer concern that can be adapted to any state shape.

### Overlap 4: Calendar — Version bump is a prerequisite for heatmap overlays

- **UI Polish Phase 3** bumps Kizitonwose Calendar from 2.8.0 to 2.10.0 and explores `HeatMapCalendar` for a year-at-a-glance view.
- **Overhaul Phase 5** adds metric-specific heatmap overlays on the existing `HorizontalCalendar` via `CalendarDayCell` modifications, `HeatmapSelector`, and `HeatmapMetric` sealed interface.
- **Impact:** These are complementary, not conflicting. UI Polish Phase 3's version bump should be done first — it's a prerequisite for the Overhaul's heatmap work. The two heatmap features are distinct: UI Polish explores the dedicated `HeatMapCalendar` composable (year view), while the Overhaul adds color overlays to the existing monthly `HorizontalCalendar`.

### Recommendation

If both epics are planned, consider this sequencing:
1. **UI Polish Phase 1** (Markdown) — no overlap, ship independently
2. **UI Polish Phase 3** (Calendar bump) — no overlap, enables Overhaul Phase 5
3. **Insights Engine Overhaul Phases 1-5** — the full Insights redesign
4. **UI Polish Phase 2** (Lottie/Shimmer) — build against the post-Overhaul Insights layout so skeletons and animations match the final UI

Alternatively, if UI Polish ships entirely first, expect to revisit `InsightsSkeletonLoader.kt` and `InsightsEmptyState.kt` during the Overhaul.

---

## Estimated Scope

- **Phase 1:** Small — 1-2 files changed, 1 dependency added
- **Phase 2:** Large — 6-8 new files, 6-7 asset files, 8-10 files modified
- **Phase 3:** Medium — 1 version bump, 2-4 files checked/modified, 1 spike prototype
