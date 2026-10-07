# ADR-0008 — What counts as a counted production attempt

Date: 2026-10-04
Status: Accepted
Related: ADR-0005 (explicit production targets), ADR-0007, Phase 2 plan.
Freeze note: this ADR changes **what generates a production LearningEvent**, never
how QualityPolicy maps an event or how SrsEngine/mastery score it. Frozen numerics
(±2.0 / ±1.0 / 1.2 / 2.0 / 1.5, mastery 3 ∧ 3 ∧ 2.0) are untouched.

## Decision

1. **Counted attempt** = free-typed input in **typing mode**, with no hint used and
   no reveal used, graded against `productionExpected` + `productionVariants`.
2. **Grading**: exact match after normalization (case-insensitive, whitespace
   collapsed and trimmed). Variants count as correct. Levenshtein distance 1 is a
   **near-miss**: feedback "قريب جدًا" + one more attempt; **nothing is recorded**
   until the attempt's final outcome. Any larger distance is a full error.
2b. **Near-miss floor**: the distance-1 retry applies only to typed words of
   **5+ characters**. For short words (act/art) one edit is a different word and
   grades as a full error (PM closure decision, 2026-10-05).
3. **Training mode** (letter bank + hints) records **no productionSuccess and no
   productionFail** — help is neither punished nor rewarded. Switching to a hint
   mid-word converts the word to training mode. "Reveal answer" remains a recorded
   failure in **both** modes (P-02 contract — PM decision recorded 2026-10-05).
4. **Repetition guard**: at most **one counted production success per word per
   session** (per editor-session state); further successes are UI-only. The
   mastery requirement (production ≥ 3) therefore needs successes across
   **separate sessions** — reopening the editor quickly cannot farm credit.
5. **No `recordView`/XP on spelling re-initialization** — views are not attempts.
6. The spelling queue **prefers** words where `productionSuccess` (progress
   projection) is below their recorded PRODUCTION attempt count (read-only event
   aggregate; no schema change).

## Consequences

- Reaching MASTERED through hints alone is impossible: hints produce zero
  production events, and mastery requires productionSuccess ≥ 3.
- The failure signal keeps its P-02 shape (3rd full error, reveal, or
  exit-after-attempt → exactly one production-fail event, keyed, replay-safe).
- The grader is a pure domain function; the view model only decides whether an
  outcome is recordable.
