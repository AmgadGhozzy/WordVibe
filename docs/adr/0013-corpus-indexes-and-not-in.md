# ADR-0013 — Corpus indexes (migration #2) and the end of the NOT IN binder risk

Date: 2026-10-06
Status: Proposed — tracked on branch ticket16/indexes-and-migration per
PM round-16; pending final merge approval.
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
   EXISTS index_wordsMaster_cefrLevel_rank ON wordsMaster(cefrLevel, rank)`
   (composite index replacing single level index) and `CREATE INDEX IF NOT
   EXISTS index_wordsMaster_rank ON wordsMaster(rank)` (serving rank order);
   `@Entity` indices mirrored so Room emits them itself. No table
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
   LEARNING, KNOWN, and MASTERED) and user isolation.

## Consequences

- `app/schemas/…/2.json` exported and tracked with the migration; schema
  `1.json` untouched. `SchemaExportContractTest` updated and strictly
  validates v2 schema structure against v1 baseline.
- The migration test (1→2 on a DB carrying userWordProgress rows) verifies
  both indexes exist in `sqlite_master`, sample rows survive intact from each
  table, and the identity hash is valid for v2 — instrumented (Room
  MigrationTestHelper), device-gated.
- Placement rungs (`getWordsByLevel`) use the composite index `(cefrLevel, rank)`
  eliminating `USE TEMP B-TREE FOR ORDER BY`; rank-ordered new-word serving
  (`getNewWordsForUser`) uses `index_wordsMaster_rank` avoiding whole-corpus sort;
  `getRandomNotMasteredWordsForUser` scans `wordsMaster` and orders with `RANDOM()`
  by definition, while searching `userWordProgress` via its primary index without
  full progress table scans; the latent 999-binder crash class is eliminated by construction.
- **EXPLAIN QUERY PLAN evidence captured on**: SQLite 3.32.2 / Android API 33 (SM-A715F),
  with a realistic 4,800-row `wordsMaster` corpus. API 24 (SQLite 3.9.x) verification is
  deferred to the pre-launch gate (Phase 7). The migration is additive indexes only; the
  worst-case outcome on an older query planner is a slower-but-correct plan, not incorrect
  results or a crash.

