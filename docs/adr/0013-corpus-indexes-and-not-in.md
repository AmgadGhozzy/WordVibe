# ADR-0013 — Corpus indexes (migration #2) and the end of the NOT IN binder risk

Date: 2026-10-06
Status: Proposed — written to disk per PM round-16; tracked in a commit only
after PM approval (docs/adr/ is now repo-tracked, so this file sits untracked
until then).
Related: ADR-0012 (corpus expansion to 4,777 candidates), db-audit REPORT.md
§8, tickets 15/16, the frozen new/due definitions and P-01 state semantics.

## Context

- `getWordsByLevel` (WHERE cefrLevel ORDER BY rank LIMIT) runs for every
  placement rung and is a full table scan + top-N today; the rank-ordered
  new-word windows scan the whole table. With the pilot's 100 rows this is
  invisible; with the 4,777-candidate expansion (~4.8×) it is the hot path.
- `getRandomNotMasteredWordsForUser` binds `w.id NOT IN (:excludeIds)` — the
  exclusion list grows with learner progress, and SQLite's parameter limit on
  API < 30 is 999: the query shape creates a latent runtime crash risk for any
  learner past ~999 seen words once the corpus is large (no production caller
  exists today, but the risk is latent in the interface contract).

## Decision

1. **Migration #2 (version 1 → 2), additive only**: `CREATE INDEX IF NOT
   EXISTS index_wordsMaster_cefrLevel ON wordsMaster(cefrLevel)` and the same
   for `rank`; `@Entity` indices mirrored so Room emits them itself. No table
   rebuild, no column change, **no destructive fallback** — the migration is
   idempotent CREATE INDEX statements and nothing else.
2. **NOT EXISTS query**: Added to `WordMasterDao` as `getRandomNotMasteredWordsForUser`:
   ```sql
   SELECT w.* FROM wordsMaster w
   WHERE NOT EXISTS (
     SELECT 1 FROM userWordProgress p
     WHERE p.wordId = w.id AND p.userId = :userId AND p.knownState = 'MASTERED'
   )
   ORDER BY RANDOM()
   LIMIT :limit
   ```
   Note: This query is a new definition with no prior production caller. The
   previous signature excluded caller-provided `excludeIds`, whereas this new
   definition excludes `MASTERED` words directly from the stored user progress
   table. This establishes a safe, binder-free pattern eliminating any SQLite
   999-parameter binder risk on `userWordProgress` (satisfying P-01 state semantics).
3. **Query Semantics and Coverage**: Because this is a newly defined query
   rather than an in-place replacement of an existing production call,
   correctness is verified via dedicated Room instrumented tests on an actual
   database fixture covering all progress states (unseen/no-progress, NEW,
   LEARNING, KNOWN, and MASTERED).

## Consequences

- `app/schemas/…/2.json` exported and committed with the migration; schema
  `1.json` untouched. `SchemaExportContractTest` updated and green.
- The migration test (1→2 on a DB carrying userWordProgress rows) verifies
  both indexes exist in `sqlite_master`, all rows intact (count + checksum),
  and the identity hash valid for v2 — instrumented (Room MigrationTestHelper),
  device-gated.
- Placement rungs and rank-ordered serving stop scanning; the latent 999-binder
  crash class is eliminated by construction.
- B's files untouched (CorpusSeeder, Application); the seeder remains
  schema-version-1-compatible until ticket 15's versioned packages land.
