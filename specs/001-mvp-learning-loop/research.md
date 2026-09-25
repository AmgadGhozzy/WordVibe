# Research: MVP Learning Loop

All unknowns below were resolved against the existing codebase (fully audited); no open
questions remain. Each entry records what was chosen, why, and what was rejected.

## Decisions

### Test seam: repository + engine, hand fakes, no mocks

- **Decision**: Golden tests target the persistence seam (real repositories over hand-written
  DAO fakes) and the pure domain seam (scheduler, streak, states). No mocking framework.
- **Rationale**: DAOs are interfaces, so fakes are trivial and behavior-faithful; real
  collaborators (engine, providers) keep tests honest. Proven working: ghost-reject and
  idempotency tests green.
- **Alternatives considered**: mockk (rejected — version risk on this toolchain, fakes are
  simpler); Robolectric + Room in-memory for DAO tests (rejected for now — JUnit 5-only
  setup has no vintage engine; DAO semantics pinned via repository-level tests instead).

### Seeding: bundled JSON asset + first-launch seeder

- **Decision**: Single JSON asset in `assets/`, one idempotent seeder on first launch
  (store-empty check + persisted seed flag) writing through the existing insert interface.
- **Rationale**: Interface already exists with zero call sites; offline-first requires no
  network; idempotent re-runs are safe.
- **Alternatives considered**: Remote fetch on first launch (rejected — offline-first
  constraint); SQL prepackaged database (rejected — JSON is reviewable by curators).

### Session accounting: durable per-tap sends, finalize reconciles

- **Decision**: Each swipe sends its outcome to a buffered channel consumed into durable
  storage at tap time; finalization performs session bookkeeping only and never re-records.
- **Rationale**: Satisfies kill-survival AND exactly-once simultaneously; verified by green
  golden tests.
- **Alternatives considered**: RAM-buffer + finalize commit (rejected — loses taps on kill);
  finalize re-record (rejected — the double-count defect being fixed).

### Streak: chain semantics in engine, freeze in repository

- **Decision**: Engine computes unbroken chains (gap → zero); repository consumes one freeze
  only for exactly-one-missed-day gaps and fabricates no study.
- **Rationale**: Keeps dashboard = projection of actual events; tested red, fixed in 05 scope.
- **Alternatives considered**: ViewModel-assembled streaks (rejected — truth must live below UI).

### Scheduler: primitive + app-owned policy

- **Decision**: Keep the small scheduling primitive; put all judgement in the app-owned
  quality policy with frozen rows; exact numeric values freeze with their tests.
- **Rationale**: Matches the architectural decision to share mechanism, own policy.
- **Alternatives considered**: SM-2/FSRS adoption (rejected for MVP — unneeded machinery;
  current tiers + frozen rows suffice).

### Navigation: 3 destinations

- **Decision**: Home, flashcards, spelling reachable; placement/progress live inside the
  learning flow; all placeholder routes removed from the graph.
- **Rationale**: Smallest shell that teaches; every visible action must reach real content.
