# ADR-0010 — Placement starts where the corpus starts; freezes are earned

Date: 2026-10-05
Status: Accepted — consolidated. This file merges the two earlier 0010 drafts that
existed on disk (`0010-placement-floor-and-freeze-grant.md` and
`0010-placement-ladder-and-freeze-grants.md`) and states the shape that was
actually implemented and merged (main @ 93c7fd4). Disk-only per the constitution:
docs/ and ADR files live on this machine and are never committed or migrated.

Related: ADR-0004 (freeze continuity-only — this is an addendum, not a change),
ADR-0007, spec v1.1 (placement scoring), audit gaps P-04/P-05, Step-2 ticket
(Placement + Freezes + Firebase key restriction).
Frozen surfaces untouched: SrsEngine, QualityPolicy, MasteryPolicy, frozen numerics,
schema, and the new/due definitions (no progress row / nextReview <= now).

## Context

- **P-04**: the placement ladder always began at A1 while the shipped corpus has no
  A1 words, so the test finalized on question zero and its (meaningless) result was
  persisted. The dialog's only escape hatches were "take the test" or "start from
  the beginning"; abandoning the test itself saved a bogus placement.
- **P-05**: `streakFreezes` existed and was consumable, but nothing ever granted a
  freeze, so the ADR-0004 machinery could never fire.
- Pilot corpus at decision time: 100 words (A2:2, B1:25, B2:35, C1:32, C2:6) — no
  A1 and a thin A2.

## Decision

### Placement

1. **Floor**: the ladder is the canonical CEFR list with every level before the
   first level that has at least one word row dropped — per-level
   `COUNT(*) FROM wordsMaster WHERE cefrLevel = :level` (`countWordsByCefr > 0`),
   evaluated at runtime. If no level has words, nothing is dropped (the full
   ladder is the degenerate-corpus fallback). On the pilot corpus the floor is A2.
2. **Thin levels borrow**: a ladder level whose pool cannot serve one target plus
   three distractors (`MIN_POOL = 4`) borrows from the nearest adjacent ladder
   level (distance 1, then 2, …); the target stays own-level, only distractors
   are borrowed. The test never ends on zero questions for any corpus that has
   words at all.
3. **Scoring skips unattempted levels**: `calculateAssignedLevel` no longer breaks
   on levels below the ladder start — passing the floor start no longer assigns
   Beginner. Break-on-failure for attempted levels (the tested adaptive cutoff) is
   unchanged. Failing the lowest present level still assigns the level below it.
4. **Consumption — serving order only, caller-passed anchor** (the approved
   shape): the result is stored in `userProfile.placementCefr` (existing column)
   and is consumed **only** as the anchor for new-word serving. The serving layer
   (`WordMasteryViewModel`) passes the stored anchor as `preferredCefr` to
   `getNewWordsForUser`; the repository never reads the profile for it.
   - No anchor (null/blank): pure rank order with the exact-limit passthrough —
     the frozen contract, byte-identical.
   - With an anchor: the candidate window doubles (`limit * 2`) over the SAME
     frozen LEFT-JOIN new-word query, then a stable partition reorders — band
     words first (rank-ordered), then the rest (rank-ordered) — and `take(limit)`.
   - Ordering never filters: the new-word definition and its counter are
     untouched. An anchor whose band has no words in the window (e.g. an anchor
     below the corpus floor) degrades to pure rank order.
5. **Honest skip at both surfaces**: the choice dialog gains an explicit "تخطّي"
   that only sets the existing `PLACEMENT_COMPLETED` DataStore flag (no result,
   no re-appearance), and abandoning the test mid-way (`skip`) persists nothing.

### Freeze grants (addendum to ADR-0004)

1. `StreakEngine.freezeGrant(currentStreak, freezes, lastGrantedAtStreak)` is a
   pure function: one freeze at each multiple of 7 of the honest streak, capped
   at 2 (`MAX_STREAK_FREEZES`), never twice for the same multiple
   (`currentStreak <= lastGrantedAtStreak` grants nothing), and a stale higher
   marker blocks the grant conservatively. All of it is pinned by the
   `StreakFreezeGrantContractTest` contracts.
2. The "last multiple granted" marker is **derived, not stored**: the repository
   (`UserActivityRepository`) passes the profile's pre-update `currentStreak` as
   `lastGrantedAtStreak`. Because a real study day advances the streak by exactly
   one, a multiple is reached (and granted) at most once per climb — no schema
   change, no extra DataStore key. If a bridged day jumps over a multiple, that
   multiple is simply not granted this run; the next multiple is.
3. Grants are evaluated only on real study days: a bridged (freeze-consumed) day
   grants nothing — mirroring the existing rule that bridged milestones mint no
   XP. Granting and consuming fabricate nothing: no LearningEvent, no activity
   row, no XP, no dashboard effect beyond the freeze balance itself.
4. The Home tab shows the freeze balance (icon + count beside the streak) and one
   line explaining how freezes are earned.

## Consequences

- Placement works on any corpus distribution and completes with ≥ 1 question on
  today's corpus; its result improves new-word serving order without touching any
  frozen definition.
- A user studying 14 consecutive real days holds 2 freezes; consumption follows
  ADR-0004 unchanged (bridges exactly one missed day, continuity only).
- The placement-question sizing helper still maps legacy level ids
  ("beginner"…) that no longer match the CEFR-string ids, so every level serves
  one question; accepted behavior, out of this ADR's scope.
- Device evidence (screenshots of Home with a freeze balance and a completed
  placement, `connectedDebugAndroidTest`) remains the user's closure gate, as in
  ADR-0007.
- Success criteria: non-degenerate placement results on the pilot corpus; the
  freeze balance rises at 7-day marks and never above 2.
