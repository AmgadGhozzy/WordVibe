# Implementation Plan: MVP Learning Loop

**Branch**: `001-mvp-learning-loop` | **Date**: 2026-09-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-mvp-learning-loop/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Rebuild the WordVibe learning loop on the existing Compose + Hilt + Room skeleton so every tap persists exactly once as a durable event, sessions serve truly new/due words from a bundled pilot corpus, scheduling follows the frozen quality table, and all progress reconciles with stored events. Approach: keep the current architecture and seams (repository + engine test seam, existing insert interface for seeding), fix by contract with golden tests first.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM 17, AGP 9.1.1

**Primary Dependencies**: Jetpack Compose (BOM 2026.08, Material3), Hilt + KSP, Room 2.8.4, DataStore Preferences, Navigation Compose, kotlinx.serialization, coroutines

**Storage**: Room database `wordvibe.db` v1 (8 entities); bundled JSON corpus asset seeded once on first launch; DataStore for onboarding/seed flags

**Testing**: JUnit 5 (jupiter) via `:app:testDebugUnitTest`; hand-written fakes over DAO interfaces (no mocking framework needed); real collaborators (engine, providers) in tests

**Target Platform**: Android, minSdk 24, target/compileSdk 37

**Project Type**: mobile-app (single module `:app`, package `com.venom`)

**Performance Goals**: Session load under 2s on mid-range devices; dashboard aggregates via SQL SUM (no full-table scans); first launch seeds 100–150 words in under 1s

**Constraints**: Offline-first (learning never needs network); pre-production (no migrations; clean installs define schema); Arabic-first RTL; zero reachable placeholders

**Scale/Scope**: Single learner on one device; 100–150 pilot words; 3 reachable destinations (home, flashcards, spelling)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] I. Event-first: plan persists every tap at tap time; finalization reconciles only.
- [x] II. Exactly-once: every event carries an idempotency key; re-apply is a no-op; unknown words rejected loudly.
- [x] III. Frozen contracts consumed unchanged: cumulative 3/3 + stability≥2.0 mastery with always-demote; quality table with real production-failure effect; freeze continuity-only; per-field corpus contract.
- [x] IV. Test-first: golden tests before fixes; red-to-green mandatory for contracted defects; infra/scaffolding exempt.
- [x] V. Minimal scope: no languages/OCR/sync/billing/ads/remote backends in this plan.
- Post-design re-check: see bottom of file.

## Project Structure

### Documentation (this feature)

```text
specs/001-mvp-learning-loop/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/venom/
├── stackcard/            # flashcard session (ViewModel = orchestration only)
├── lingospell/           # spelling production
├── quiz/                 # placement + quiz surfaces
├── data/
│   ├── local/            # Room: entities, DAOs, database, DataStore keys
│   ├── repo/             # repositories (persistence seam under test)
│   ├── mapper/           # entity/domain mappers
│   └── mock/             # demo fallback (removed from shipping path)
├── domain/
│   ├── srs/              # scheduler primitive + quality policy (pure, tested)
│   ├── model/            # WordMaster, progress, streak, mastery states
│   └── provider/         # config provider over defaults
├── wordvibe/
│   ├── navigation/       # 3 reachable destinations only
│   ├── ui/               # shell, dashboard
│   └── di/               # Hilt bindings (no stubs in shipping path)
└── analytics/            # local logging interfaces

app/src/test/java/com/venom/
├── domain/model/         # engine/state golden tests
└── data/repo/            # persistence-boundary golden tests (hand fakes)
```

**Structure Decision**: Single-module Android app; existing package layout is kept. Tests mirror the persistence seam (`data/repo` fakes over DAO interfaces) and the pure domain seam (`domain/srs`, `domain/model`). No new modules.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

None. No violations.

## Post-design Constitution Re-check

- [x] No new persistence path bypasses tap-time durable events.
- [x] No scheduler change alters frozen quality rows or mastery thresholds.
- [x] No scope additions beyond home/flashcards/spelling/progress.
- [x] Test architecture unchanged (hand fakes, real collaborators, JUnit 5).
