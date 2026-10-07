# ADR-0009 — Spelling input is the letter bank; free typing is withdrawn

Date: 2026-10-05
Status: Accepted
Supersedes: the free-typing decisions of ADR-0008 (counted attempt = typed input,
normalization/Levenshtein grading, near-miss floor). Keeps every other ADR-0008
rule. Related: ADR-0005, P-02, Phase 2 closure review.

## Context

ADR-0008 replaced the letter bank with free typing to stop scaffold farming.
The PM's closure review reversed that: on a real keyboard, **autocomplete,
suggestions and paste corrupt the very production measurement the change was
meant to protect**. The letter bank is restored and its farming holes are closed
by RULES, not by changing the input surface.

## Decision

1. **Input = letter bank only.** No text field, no system keyboard, no paste in
   the spelling path. The bank is built from `productionExpected` (falling back
   to `wordEn`); acceptance is exact match against `wordEn`, `productionExpected`
   or any `productionVariants` entry, case-insensitive.
2. **Kept from ADR-0008/P-02 unchanged**: keyed failure recording (3rd full
   error / reveal answer / exit-after-attempt → exactly one keyed production
   failure); reveal answer = failure; one counted success per word per session;
   the word is never re-served after success; no `recordView`/XP on
   re-initialization; hints can never reach a mastery claim.
3. **Configurable hint budget** (DataStore, surfaced in a bottom sheet on the
   spelling screen):
   - `hintsEnabled` (default true) — off hides hints entirely.
   - `maxHintsPerWord` (0..3, default 3).
   - `hintCreditPolicy`: `NO_CREDIT_IF_ANY_HINT` | `CREDIT_UP_TO_N`
     (default **CREDIT_UP_TO_N with N = 1**).
4. **Credit rules**: success with no hint → production success ×1. Success with
   hints within the policy limit → production success ×1. Success with hints
   beyond the limit → no production event (training). Hints never block
   failure recording.
5. **The 3-consecutive-successes dialog returns as celebration only**
   ("٣ نجاحات متتالية") — it never claims mastery and never reads mastery state.
6. `ProductionGrader` stays in domain with its tests but is **not called** from
   the spelling path.

## Consequences

- The measurement threat moves from the keyboard to the budget: the hint
  counter is always visible and the UI states whether the attempt will count.
- ADR-0008's grading machinery (normalization, Levenshtein) is retained as
  domain tooling for a future exercise type that can host it honestly.
