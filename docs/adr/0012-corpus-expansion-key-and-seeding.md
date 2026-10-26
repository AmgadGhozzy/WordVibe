# ADR-0012 — Corpus expansion from WordsMaster.db: key, identifiers, seeding, and review policy

Date: 2026-10-07
Status: Accepted — merged to main; review policy is **option (ب) as the PM's
stated default** and is recorded as an explicit exception (see Decision 8).
Related: ADR-0005 (explicit machine-checkable production targets), ADR-0010
(placement anchor / rank semantics), ADR-0011 (units are the ten IELTS topics),
`.scratch/db-audit/REPORT.md` (full read-only audit this decision builds on).
Frozen surfaces untouched: SrsEngine, QualityPolicy, MasteryPolicy, the existing
schema, frozen contract tests, the new/due definitions. No code change in this
ADR: Phase 0 is files on disk only (ADR + conversion tool + design docs), no
commits, nothing under `app/src` modified.

## Context

`app/src/main/assets/WordsMaster.db` (7,300 rows, 11.07 MB, untracked, no VCS
history) is a user-owned asset the app never reads today, yet the APK ships it.
The audit (REPORT.md, 2026-10-07) established:

- It is **not** a Room database (`room_master_table` absent, `user_version=0`)
  and 9 contract columns are missing from its `wordsMaster` table, so
  `createFromAsset` is structurally impossible; 0 of 7,300 rows pass the frozen
  `CorpusSeeder.validateCorpus` contract as-is.
- 6,422 distinct lemmas but 7,300 rows — the file's real key is (lemma, pos);
  the corpus contract requires a unique lemma. 789 lemmas carry +878 extra
  multi-POS rows.
- Data defects: 719 example fields are arrays instead of CEFR-keyed maps; 7
  multi-word lemmas violate the lemma regex; literal `"null"` in `phoneticUk`
  (23 rows); ZWNJ in `phoneticAr` (11); Vertex AI Search citation remnants in
  rows 5683 and 6724; `wordFamily` empty in 296; `synonyms` empty in 82.
- Provenance (from the file's own audit tables): an **internal lexical-migration
  pipeline, LLM-assisted on Vertex AI** — `migration_audit_2026_001` (1,384
  edits, eval `eval_seeded348_v1-*`, 2026-09-08) and `migration_audit_2026_002`
  (564 rows, eval `eval_remed609_v1-*`, 2026-09-09), with `gen_job` /
  `judge_job` batch-prediction ids and per-file SHA-256 raw-output paths that
  no longer exist in the repo. **No license text exists anywhere in the file.**
- `fromOxford = 1` on **5,831 rows** suggests derivation from an Oxford word
  list (3000/5000) — unconfirmed and, crucially, **unlicensed**.

The PM chose selective import through the seeder (audit option 2) as the
destination, gated in phases. This ADR fixes the corpus-shape decisions Phase 0
implements mechanically, before any content generation.

## Decision (PM round-15, defaults recorded here)

### 1. Key: unique lemma, one row per lemma

The corpus contract's unique-lemma key wins over the file's (lemma, pos) key —
IELTS presentation is lemma-first, and every frozen surface (validator,
ordering, placement) already assumes it. For the 789 multi-POS lemmas exactly
one row is adopted and the rest are **left behind** (never merged into
franken-rows). Kept row = fewest data-rule failures after mechanical
normalization, then lower original `rank`, then lower `id` (deterministic;
implemented in `convert_wordsmaster.py`).

### 2. Identifiers: expansion ids start at 1001

Expansion words get ids **1001…** in expansion order. The pilot's 1–100 are
never renumbered and no source id is reused. Ingestion uses **INSERT OR
IGNORE** — no `OnConflictStrategy.REPLACE`, no `createFromAsset`, ever, for
expansion packages. This eliminates the audit's worst merge risk (silent
corruption of `userWordProgress` rows for ids 1–100).

### 3. Rank: `6000 + original rank`

Expansion `rank = 6000 + (source rank)`. Source ranks are a unique
frequency-ordered 1..7,300 scale, so expansion words occupy 6001+ with the same
relative commonness ordering, and the pilot's ranks (600–5800) always sort
first: pilot words are served before expansion words. The gap 5801–6000 is
reserved headroom. ADR-0010's placement-anchor semantics are unchanged; the
band-first anchor applies to the merged corpus exactly as it does today.

**Identifier spaces (verified, batch 1).**

- Expansion ids 1001–1198 are newly assigned and do not match base-source ids; same numeric id means a different word.
- Expansion rank = source rank + 6000 (verified 81/81).
- The 7,300-row source base must never be loaded into the same wordsMaster table.
- Verified against words_pilot.json: word, id and rank collisions are 0/0/0.

**Merge condition:** main's untracked copy of this ADR must be removed or moved aside, with written approval from Amgad, before merging this branch into main.

### 4. Exclusions (batch 1)

Excluded, classified by first-match precedence:

1. **Leaked-citation rows 5683 (`homemade`) and 6724 (`migrate`)** — Vertex AI
   Search citation remnants inside text; hard exclusion, never repaired
   silently.
2. **The 7 multi-word lemmas** (`all right`, `any more`, `ice cream`, `next to`,
   `no one`, `owing to`, `per cent`) — they violate the frozen lemma regex
   (`^[A-Za-z'\-]+$`); un-fixable without a contract change, which is out of
   scope.
3. **CEFR A1 and A2** — WordVibe's MVP audience is B1–C2 exam prep; the pilot
   itself contains no A1.
4. **The 78 lemmas shared with the reviewed pilot** — pilot priority (Decision
   5); all their DB rows are excluded.
5. **Multi-POS losing rows** (Decision 1).

Result: 7,300 → **4,777 candidate words** (full numbers:
`tools/corpus-expansion/out/EXCLUSION_REPORT.md`).

### 5. Pilot priority: the reviewed pilot is authoritative

The 78 shared lemmas keep the **pilot's reviewed versions** — definitions,
translations, examples, rank, CEFR, everything. The DB's variants are reference
material only and are never substituted, merged, or diffed back into the pilot.
No field of any of the 100 pilot words changes because of this expansion.

### 6. Mechanical normalization only (no editorial content in Phase 0)

- `pos` `adj` → `adjective` (pilot vocabulary). Other POS values pass through.
- Examples array → CEFR map, re-keyed **only** from the file's own audit
  pre-images (`migration_audit_2026_001/002.value_before`), by exact sentence
  text. Keys are never inferred or invented; unmatched sentences are dropped
  with a full transform-log entry; fully unkeyable rows stay flagged
  (`examples_unkeyable`, fail R14) and fall to the generation stage, where an
  example must exist anyway. (Measured: 455 rows re-keyed — 346 full + 109
  partial; 56 rows unkeyable in candidates.)
- Literal `"null"` in `phoneticUk` → absent (20 rows).
- ZWNJ (U+200C) removed from `phoneticAr` (7 rows).
- Whitespace trim on text fields (1 row).
- `unitId` dropped: ADR-0011 makes units the ten IELTS topics; the DB's 136
  CEFR modules are a different, incompatible unit model.
- `fromOxford` is **not imported** from the source (flag absent → parses as 0).
- Left as-is, deliberately: tatweel in `mnemonicAr` (stylistic), digits inside
  Arabic text (69 ASCII + 8 Arabic-Indic — optional normalization, open
  question below), `translit` and `category` values.

Every change lands in `transform_log.json` (field, before, after) so the review
workflow can audit each mechanical edit; nothing is rewritten silently.

### 7. Versioned seeding (no `createFromAsset`, no REPLACE)

Corpus arrives as **sequential signed JSON packages**; the applied package
sequence is tracked as `corpusVersion` in DataStore. Each seeding step runs
INSERT OR IGNORE into the existing Room store, so replays and re-orders are
no-ops and existing rows (pilot or earlier expansion) are never overwritten.
Contract tests cover: order independence, replay idempotence, pilot-row
immutability, and `corpusVersion` monotonicity. Implementation lives in
`.scratch/wordvibe-mvp/issues/15-seeder-versioned.md` (ticket 15); supporting
indexes in ticket 16.

### 8. Review policy: option (ب) — recorded as an explicit exception

**PM default, adopted:** full human review of batch 1 (the 200-word pilot
batch), then 100% mechanical audit (the 28-rule validator plus the additional
quality rules of `NINE_COLUMN_PIPELINE.md`) with a **15% human sample** for
subsequent batches. This is weaker than the per-word human sign-off the pilot
corpus received, so it is recorded as an **explicit, bounded exception**, not a
silent relaxation:

- every word carries `reviewMode: full | sampled` (see column note below), so
  the audit trail shows which words a human actually reviewed;
- the exception is batch-scoped: reverting to `full` for any future batch is a
  PM decision, not an implementation default;
- if the sampled audit finds a rule violation in the machine audit that human
  review of batch 1 would have caught (i.e. a class of defect the mechanical
  rules cannot see), the batch falls back to `full` review.

`reviewMode` is carried as batch/word metadata alongside the nine columns and
is stamped into `reviewer`/`reviewedAt`/`checklist` at sign-off (`checklist`
gains a `review-mode` attestation entry); it does not alter the frozen 28-rule
contract, which stays exactly as written.

### 9. Source attribution and ownership (blocking placeholder)

The source is, per the file's own audit tables, an **internal lexical-migration
pipeline, LLM-assisted on Vertex AI** (`migration_audit_2026_001/002`, eval ids
`eval_seeded348_v1-*` / `eval_remed609_v1-*`, 2026-09-08/09). The **original
word list behind the 7,300-row base and its owner cannot be documented from
inside the file** — that provenance must come from the user:

> **أصل القائمة: Academic Word List وOxford 3000/5000. لم يُوثَّق أي ترخيص، ولم يُحدَّد المصدر الأصلي لكل كلمة. مخاطرة ترخيص مفتوحة قبل أي إطلاق تجاري.**

Supporting evidence recorded here: `fromOxford = 1` on 5,831 of 7,300 rows
suggests the base list derives from an Oxford 3000/5000-style list. That claim
is unverified, **no license can be assumed from it**, and the flag itself is
**not imported** into candidates (Decision 6) and expansion words will not
carry `fromOxford = 1`. Precision note: the flag is not entirely inert in the
app — `OverviewTab.kt` (stack-card insights) shows an "Oxford core" indicator
derived from `word.fromOxford == 1`; today only pilot words carry the flag, and
expansion words (flag 0) will simply never show that badge. The audit's
broader statement that the source DB file itself is never imported nor rendered
by the app remains true (`Application.onCreate` reads `words_pilot.json` only;
`Room.databaseBuilder` has no `createFromAsset`).

## Consequences

- 4,777 mechanically clean-or-flagged candidates exist on disk without any
  model call; 4,604 of them (96.4%) already pass every data rule that can run
  before the nine columns exist.
- The APK keeps shipping the 11.07 MB unused asset until the PM separately
  decides its fate (audit option 3); this ADR neither moves nor deletes it.
- Batch 2+ can reuse the same tool unchanged (`convert_wordsmaster.py` is
  deterministic and offline).
- Remaining license question is now a single blocking placeholder (Decision 9)
  instead of an undocumented risk.
- What this ADR does **not** decide: the fate of the asset in `assets/`, the
  optional Arabic-digit normalization, whether `adv` should normalize to
  `adverb` (pilot has no adverb precedent), and the final topic of any
  individual word (generation + review decide those).

## Open questions

1. **Original source list of the 7,300-word base and its owner/license**
   (Decision 9 placeholder) — open on the user; blocks sign-off/shipping, not
   Phase-0 work.
2. Should `adv` also normalize (`adverb`) for vocabulary symmetry, and what is
   the canonical form for the remaining POS values (`pron`, `prep`, `det`,
   `num`, `conj`, `excl`, `modal`)?
3. Normalize digits inside Arabic definitions (69 ASCII + 8 Arabic-Indic) or
   leave as-is?
4. PM confirmation that `unitId` stays dropped for expansion words until
   topic-units (ADR-0011) define the mapping.
5. Does the future `corpusVersion` scheme (sequential integers in DataStore,
   ticket 15) need a user-visible "content version" surface, or is it
   diagnostics-only?

## Addendum: PM / Amgad Decisions (Draft — 2026-10-11)

1. **Zero generative model spend**: No model calls or generation pipeline costs. Words are taken directly from `WordsMaster.db` and the nine columns are derived mechanically/programmatically.
2. **CEFR authority**: The reviewed pilot (1–100) is the ground truth in case of conflict. Expansion words inherit the CEFR level from `WordsMaster.db`.
3. **Numbering & stability**: Pilot words retain IDs 1–100. Expansion words receive `id >= 1001` matching `CorpusSeeder`. No modifications to the 28 contract rules or to `CorpusSeeder.kt`.
4. **Environment isolation**: Only the primary session writes to the main workspace directory. All corpus expansion preparation occurs in a dedicated external branch and worktree.
5. **Human sign-off requirement**: Every commit and merge requires explicit written approval from Amgad. Strict engineering invariants remain in force (TDD, no git reset/amend, no unvetted changes, raw diagnostic outputs).
6. **Source provenance**: Recorded in §9 — the original list is the Academic Word List plus Oxford 3000/5000, with no license documented (open licensing risk before commercial launch). This resolves the former placeholder.

