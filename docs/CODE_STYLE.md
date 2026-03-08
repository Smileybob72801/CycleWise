# RhythmWise Code Style Guide

## Kotlin
- Use explicit types except for obvious locals.
- Prefer `val` over `var`.
- Group imports by module (`com.veleda.cyclewise.*`).
- Use `sealed class` and `data class` for models.

## Compose
- One `@Composable` per UI element responsibility.
- Preview functions in `/preview` packages.
- All UI strings go through `stringResource()`.

## Naming
- Classes = PascalCase
- functions/vars = camelCase
- Constants = UPPER_SNAKE_CASE

## Testing
- File names end with `Test.kt`.
- Use `Given-When-Then` structure in test bodies.
- 100 % coverage required for public utilities.

## Commits
Follow [`GIT_COMMIT_GUIDELINES.md`](GIT_COMMIT_GUIDELINES.md).  
Each commit must pass tests and include DCO sign-off.
