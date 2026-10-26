# ADR-0011 — Units are the corpus IELTS topics, gated by a known-ratio

Date: 2026-10-06
Status: **Accepted** (PM ruling, 2026-10-06 — the units feature is the explicit
MVP-scope extension). Implementation order is gated: UnitGate (pure, JVM) may
land now; new-word gating wiring and the UI lock-reason may start only after
track A's flake-fix merge closes and the 20 consecutive suite runs are done.
Related: ADR-0010 (placement anchor for new-word ordering), P-01/P-02 (frozen
new/due definitions), frozen numerics (mastery thresholds).
Frozen surfaces untouched: SrsEngine, QualityPolicy, MasteryPolicy, the existing
schema, frozen contract tests, and the new/due definitions (new = no progress
row for the user; due = nextReview <= now).

## Context

Units are the content container on top of the corpus:

- `wordsMaster.ieltsTopic: String?` exists on the entity, the domain model, and
  the remote DTO; the corpus seeder validates it against exactly ten canonical
  values: Education, Work, Environment, Health, Technology, Society, Culture,
  Economy, Science, Travel.
- Serving today is due-first-then-new (`rank ASC` with the ADR-0010
  `placementCefr` anchor). A `getKnownCount` read (KNOWN-state count per user)
  already exists as a repository surface.

## Decision (accepted, PM 2026-10-06)

1. **"Known" = KNOWN or MASTERED**, read through the existing `getKnownCount`
   surface. SEEN/LEARNING never count — the gate cannot be satisfied by views.
2. **Opening rule (60%, rounded up):** unit T+1 opens when
   `ceil(60% × |usable words of unit T|)` of them are known. Usable words are
   the corpus-valid ones (non-blank `definitionEn` and `arabicAr`). The first
   unit is always open, and placement completion never gates anything: an
   unplaced user simply starts at unit 1.
3. **The lock applies to new-word entry only.** Due review is never locked —
   the frozen due definition keeps serving unconditionally. Units steer, they
   never block the scheduler.
4. **Fixed sequential chain over the ten units, in this editorial order:**
   Education → Work → Health → Travel → Technology → Environment → Culture →
   Society → Economy → Science. The order is an editorial decision (closest to
   a new learner's daily life first, most abstract last), deliberately not
   derived from corpus statistics; it can be re-ordered editorially only with a
   PM ruling.
5. **Placement does not choose the unit.** Inside a unit, new words follow the
   existing rules unchanged: the ADR-0010 placement band-first anchor, then
   `rank ASC`.
6. **Words without an `ieltsTopic` are rejected at ingestion** — already
   enforced by the existing corpus validator (`stringField` yields `""`, which
   is outside `IELTS_TOPICS`), so no code change is required; this ADR records
   the guarantee.
7. **The UI shows the lock reason** ("اعرف N كلمات أخرى لتفتح وحدة X") so a
   locked unit never looks like a defect.

### Frozen-scope extension

Persisting the currently-open unit is resolved in favor of a DataStore
preference (`PreferencesKeys.OPEN_UNIT`) rather than a `userProfile` column,
preserving the frozen Room schema (`2.json`) without a database migration.
The 60% gate (`UnitGate`) and `EDITORIAL_CHAIN` drive monotonic unit
progression on new-word entry in `loadSrsWords`.

## Resolved questions (were open in the Proposed draft)

1. Definition of "known" → KNOWN/MASTERED via `getKnownCount` (Decision 1).
2. Sequential vs. set-based unlocks → fixed sequential chain (Decision 4).
3. Placement interaction → placement never chooses the unit; inside-unit
   ordering keeps the anchor (Decision 5).
4. NULL-topic words → rejected at ingestion; no "General" bucket (Decision 6).
5. Measure over usable words, minimum-size floor → measure over usable words;
   rounding up makes tiny units demand relatively more (ceil of 60%), no
   separate floor (Decision 2).
6. Chain ordering → editorial order pinned in Decision 4.

## Consequences

- No scheduler, grading, or frozen-numerics change: zero risk to the frozen
  surface; the feature is additive read/projection logic plus one persisted
  preference.
- Unit progress display becomes possible from the same read surfaces without
  new writes.
- The gate is honest against the mastery pipeline by construction (Decision 1).
