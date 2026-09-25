# Tasks: MVP Learning Loop

**Input**: Design documents from `/specs/001-mvp-learning-loop/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: REQUIRED — constitution mandates golden contract tests before fixes (red-to-green for contracted defects; infra/scaffolding exempt).

**Organization**: Grouped by user story; each story independently testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallelizable (different files, no dependencies)
- **[Story]**: US1–US6 maps to spec.md user stories

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify harness green before any story work.

- [ ] T001 Verify unit harness runs: `./gradlew :app:testDebugUnitTest` goes red-or-green (not executor failure) — no files changed unless broken
- [ ] T002 [P] Record golden baseline: run full suite, log PASS/FAIL per contract in `specs/001-mvp-learning-loop/checklists/` notes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema and seams every story builds on. MUST complete before user stories.

- [ ] T003 Add `sessionId` column + UNIQUE index on `idempotencyKey` in `app/src/main/java/com/venom/data/local/entity/QuizAttemptEntity.kt` (clean v1 schema, pre-production) with lookup in `app/src/main/java/com/venom/data/local/dao/QuizAttemptDao.kt`
- [ ] T004 [P] Golden test: re-applied key is a DB-level no-op in `app/src/test/java/com/venom/data/repo/GhostProgressContractTest.kt` (write FIRST, fail, then implement T003)
- [ ] T005 Implement unapplied-event reconciliation (`replayUnappliedEvents`) in `app/src/main/java/com/venom/data/repo/UserWordProgressRepository.kt` with golden test: persisted event without projection → replayed exactly once, finalize never the sole rescue

**Checkpoint**: Foundation ready — event identity is DB-enforced, replay exists, harness green-or-contract-red.

---

## Phase 3: User Story 1 - Start learning immediately (Priority: P1) ✅ MVP

**Goal**: First launch teaches real IELTS words offline within two taps.

**Independent Test**: Fresh install + airplane mode → real session, swipable cards, restart loses nothing.

### Tests for US1 (write FIRST, fail before implement)

- [ ] T006 [P] [US1] Golden test: first launch seeds store exactly once, re-launch never duplicates in `app/src/test/java/com/venom/data/repo/` (new `CorpusSeedContractTest.kt`)
- [ ] T007 [P] [US1] Golden test: per-word field contract holds for every corpus entry (unique lemma, non-empty meaning/definition, example uses a valid form, collocations ≥ 1, synonyms ≥ 1, explicit expectedForm + acceptedVariants, reviewer sign-off)

### Implementation for US1

- [ ] T008 [P] [US1] Author pilot corpus JSON `app/src/main/assets/words_pilot.json` (100–150 curated words, all 11 contract fields + sign-off)
- [ ] T009 [US1] Implement idempotent first-launch seeder writing through the word-store insert interface (store-empty check + DataStore seed flag)
- [ ] T010 [US1] Route cold start and returns to home; remove demo-word shipping path (`app/src/main/java/com/venom/data/mock/MockWordData.kt` gated out of release sessions)
- [ ] T011 [US1] Evidence: quickstart scenarios 1 (first launch) PASS with observed assertions

**Checkpoint**: US1 independently demoable offline.

---

## Phase 4: User Story 2 - Every tap counts exactly once (Priority: P1)

**Goal**: Per-tap durable events, zero doubles, zero losses, loud unknown rejects.

**Independent Test**: One tap + kill → exactly one stored effect; same tap twice → one effect; unknown word → zero rows + loud log.

### Tests for US2 (mostly exist — verify green, extend where thin)

- [ ] T012 [P] [US2] Verify `GhostProgressContractTest` (ghost reject + same-key no-op) green; extend with unknown-word success-path reject case
- [ ] T013 [P] [US2] Golden test: left-swipe persists a failure without session finalization (kill-survival at tap granularity)

### Implementation for US2

- [ ] T014 [US2] Confirm per-tap durable sends + finalize bookkeeping-only (exists from prior work — verify, do not redesign)
- [ ] T015 [US2] Evidence: quickstart scenarios 2 (tap accounting), 3 (unknown words) PASS with observed assertions (event count = 1, duplicate effect = 0, kill survival, zero ghost rows)

**Checkpoint**: US1 + US2 jointly green; counting trusted.

---

## Phase 5: User Story 3 - Right words at the right time (Priority: P2)

**Goal**: DB-backed new/due/mastered; repeats-as-new gone; deterministic demotion.

**Independent Test**: Study once, return past review date → due, never new; mastered + fail → known.

### Tests for US3

- [ ] T016 [P] [US3] Golden test: new = word row with no progress row (left-join semantics) in `app/src/test/` (repo level with fakes)
- [ ] T017 [P] [US3] Golden tests: cumulative 3/3 + stability ≥ 2.0 masters; any real failure demotes to KNOWN preserving history; spelling-only/recall-only never master

### Implementation for US3

- [ ] T018 [US3] Replace seen-IDs loading with left-join new-word query in session loader; prefetch stops creating progress rows
- [ ] T019 [US3] Enforce frozen mastery policy in scheduler path; evidence: quickstart scenario 4 (scheduling) PASS

**Checkpoint**: US3 green; sessions serve true new/due.

---

## Phase 6: User Story 4 - Spelling counts (Priority: P2)

**Goal**: Production signals persist and move scheduling with real (never token) effect.

**Independent Test**: Two failed spellings → sooner review than an always-correct word.

### Tests for US4

- [ ] T020 [P] [US4] Golden tests: production failure shortens genuinely (negative quality); production success advances; both keyed and exactly-once

### Implementation for US4

- [ ] T021 [US4] Persist spelling failures into learning state through the keyed event path; wire quality rows (recall-failure lower, production-failure low — values frozen here with tests)
- [ ] T022 [US4] Evidence: quickstart spelling assertions PASS (earlier review, no-mastery-on-production-only)

**Checkpoint**: Recognition + production both train scheduling.

---

## Phase 7: User Story 5 - Honest streak and dashboard (Priority: P3)

**Goal**: Chain engine + explicit repository seam; freeze fabricates nothing; dashboard reconciles.

**Independent Test**: Missed day with freeze → continuity, −1 freeze, gap day absent from history/XP/dashboard.

### Tests for US5 (3 exist red — turn green)

- [ ] T023 [P] [US5] Turn the three `StreakEngineContractTest` reds green (gap → 0, single-miss → 0, unbridgeable freeze unconsumed)
- [ ] T024 [P] [US5] Golden test: dashboard totals equal stored-event sums one to one

### Implementation for US5

- [ ] T025 [US5] Split seam explicitly: pure deterministic `StreakEngine` (chain math) vs repository (freeze inventory, persisted streak state); freeze consumption never enters the event log
- [ ] T026 [US5] Replace in-memory dashboard sums with SQL aggregates; evidence: quickstart streak/dashboard assertions PASS

**Checkpoint**: Motivation numbers earned, never forged.

---

## Phase 8: User Story 6 - Coherent shell, no dead ends (Priority: P3)

**Goal**: Three reachable destinations, remembered onboarding, Arabic-first, zero placeholders.

**Independent Test**: Cold start → home; every visible action reaches real content.

### Tests for US6

- [ ] T027 [P] [US6] Golden test: onboarding flag persists across restarts (asked exactly once)

### Implementation for US6

- [ ] T028 [US6] Reduce navigation graph to home/flashcards/spelling; placement and progress live inside the learning flow
- [ ] T029 [P] [US6] Arabic strings + RTL coherence across the learning flow; permission requested in context; remove fixed launch delay

**Checkpoint**: Shell never strands the learner.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T030 Run all 7 quickstart scenarios end to end with observed PASS evidence in `specs/001-mvp-learning-loop/checklists/`
- [ ] T031 Full suite `./gradlew :app:testDebugUnitTest` green except contractually-red leftovers (none expected at this point)
- [ ] T032 Reconcile `.scratch/wordvibe-mvp` tickets (03 stays resolved; amend 04/05 only where this plan proved necessary)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Stories (Phase 3+)**: Depend on Foundational; then P1 → P2 → P3 in order (US2 verifies prior 01/03 work rather than rebuilding it).
- **Polish (Phase 9)**: Depends on all stories complete.

### Within Each Story

- Tests FIRST (fail before implement) for contracted defects; infra/scaffolding exempt.
- Contract tests before implementation; implementation before evidence run.
- Story checkpoint green before next story starts.

### Parallel Opportunities

- T002 with T001; T004 with T003-adjacent test; T006 with T007; T012 with T013; T016 with T017; T020 alone; T023 with T024; T027 with T028-work; T029 parallel to T028 (different files).
- Stories run sequentially (P1 → P2 → P3); tasks marked [P] parallelize within a phase.

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 Setup + Phase 2 Foundational (sessionId/UNIQUE/replay).
2. Phase 3 US1 (corpus + seeding + home landing) → STOP, validate offline demo.
3. Phase 4 US2 (verify exactly-once + evidence) → counting trusted. MVP demonstrable.

### Incremental Delivery

US3 states → US4 spelling weight → US5 streak/dashboard → US6 shell → Polish evidence sweep. Each phase independently green.

---

## Notes

- [P] = different files, no dependencies. Each task names exact file paths.
- Existing 01/03 work is verified and extended, never rebuilt (GPT order).
- Frozen contracts are inputs; any deviation needs ADR + approval, never silent edits.
