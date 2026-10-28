# ADR-0014 — Lexical Entry Identity, Word-Family Semantics, and Multiword Expression Validation

Date: 2026-10-11
Status: **Accepted** — read-only to app runtime; governs corpus contract, expansion export, and seeder validation.
Related: ADR-0012 (corpus expansion key and seeding), ADR-0013 (corpus indexes and NOT IN replacement), `.scratch/db-audit/REPORT.md`.

## Context

The initial corpus validator (`CorpusSeeder.validateCorpus`) enforced a unique lemma policy (`wordEn.lowercase()`) across the entire corpus document. When evaluating expansion candidates from `WordsMaster.db`, this policy forced the exclusion of 416 rows representing distinct parts of speech (e.g. `state` as verb vs adjective, `live` as verb vs adjective, `set` as verb vs noun).

Investigation of the data layer confirmed:
1. `wordsMaster` primary key is `id: Int` (no database-level unique constraint on `wordEn`).
2. `userWordProgress` references progress via `(userId, wordId: Int)`.
3. `WordMasterDao` queries and joins exclusively on `wordId: Int`.
4. Compose UI lists currently reference `key(word.wordEn)`, which requires update to `key(word.id)`.

Furthermore:
- The universal `wordFamily` requirement (R19) penalized closed-class or non-derivational words (e.g. conjunctions `now`, adverbs `by`) where no morphological family is linguistically applicable.
- The `usesWordForm` tokenization rule split text on `[^a-z']+`, causing hyphenated expressions (such as `so-called` and `set-up`) to fail example validation (R16) despite literally appearing in their example sentences.
- Multiword expressions with spaces (such as `no one`, `ice cream`, `owing to`) were excluded purely due to single-token lemma regex constraints.

## Decision 1: Lexical Entry Identity (Lemma + POS)

1. **Identity**: Each lexical entry is identified by its unique integer `id`.
2. **Duplicate Detection**: The validator detects duplicates using the normalized composite key `(lemma, normalized_pos)`:
   - `lemma`: trimmed, lowercase.
   - `pos`: mapped through an explicit normalization table (`adj` -> `adjective`, etc.). Synonymous POS tags must never bypass uniqueness checks.
3. **Data Integrity**: Distinct POS entries for the same lemma maintain their own distinct definitions, examples, CEFR levels, and source provenance. They are never conflated or merged into franken-rows.
4. **Ordering & Curriculum**: Source frequency `rank` represents lexical frequency evidence and must not be treated as a pedagogical curriculum order.
5. **Downstream Stability**: Ingestion continues to use `id >= 1001` and `INSERT OR IGNORE` to guarantee zero corruption of existing user progress.

## Decision 2: Word-Family Semantics (Conditional Applicability)

1. `wordFamily` is **not universally mandatory** across all grammatical categories.
2. The data contract distinguishes three semantic states:
   - `populated`: morphological family populated with at least one non-empty member (`noun`, `verb`, `adj`, `adv`).
   - `none_applicable`: the entry belongs to a category where derivational families are not linguistically applicable (or documented as having none). An empty map `{}` or explicit absent flag is accepted without failing R19.
   - `unknown`: unreviewed or unverified state, flagged for editorial review.
3. R19 validator rule is refined to permit empty/null `wordFamily` when marked or when POS indicates a closed-class category, avoiding artificial derivations.

## Decision 3: Multiword and Hyphenated Expressions

1. **Lemma Shape**:
   - Single-word entries: `^[A-Za-z][A-Za-z'\-]*[A-Za-z]$` (or single letters where valid).
   - Multiword expressions: letters, hyphens, apostrophes, with single spaces between tokens (`^[A-Za-z'\-]+( [A-Za-z'\-]+)+$`). Leading, trailing, or consecutive spaces are strictly rejected.
2. **Tokenization Consistency**:
   - `usesWordForm` must treat hyphens consistently with lemma validation, ensuring hyphenated terms (`so-called`, `set-up`) are matched as unified tokens or matching prefixes rather than fractured fragments.
3. **Multiword Matching**:
   - Multiword expressions must match as complete phrases or inflections in example sentences, not as accidental substring slices.

## Consequences

- **Positive**: Rich lexical distinctions in the upstream database are preserved. Legitimate hyphenated and multiword expressions pass validation without artificial exclusions.
- **Negative / Risks**: Increased complexity in token matching. Compose lists and analytics referencing `wordEn` instead of `id` must be audited and updated.
- **Scope Restriction**: This ADR authorizes contract definition and test validation. It does **not** authorize direct writes to `WordsMaster.db`, production migrations, or bulk corpus seeding until staging verification is complete.
