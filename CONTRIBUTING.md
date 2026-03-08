# Contributing to RhythmWise

Thank you for helping to keep RhythmWise a privacy-first and trustworthy application.

RhythmWise follows the Conventional Commit Specification for commit messages:
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org)

All commits must include a DCO sign-off:
Signed-off-by: Your Name <you@example.com>

---

## 🧭 Branch Strategy
- `main` – stable releases
- `develop` – active integration
- `feature/*` – individual work
- `docs/*` – documentation updates

---

## 🧪 Testing
All PRs must include unit or feature tests and pass CI.

Run locally:
```bash
./gradlew test
```

## 🔍 Maintainer Review Policy

All pull requests to protected branches (such as `main` or `develop`) require
approval from the project maintainer **Daniel Simmons** before merging.

The `CODEOWNERS` file automatically requests a review from Daniel on every PR.
A merge cannot proceed until at least one maintainer approval is granted.

This policy ensures that:
- Code changes align with RhythmWise’s privacy and architectural standards  
- The app remains consistent with its design philosophy  
- All contributions are properly reviewed for security and stability  

Contributors are encouraged to open drafts early to get feedback before formal
review. Constructive collaboration is welcome — final merge decisions simply
rest with the maintainer to preserve project integrity.
