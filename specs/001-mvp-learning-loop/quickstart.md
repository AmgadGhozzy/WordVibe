# Quickstart: MVP Learning Loop Validation

Runnable scenarios proving the feature end to end. See contracts/ and data-model.md for
details; no implementation code lives here.

## Prerequisites

- Clean checkout builds: `./gradlew :app:assembleDebug`
- Unit suite: `./gradlew :app:testDebugUnitTest`
- Targeted goldens: `--tests "com.venom.domain.model.StreakEngineContractTest" --tests "com.venom.data.repo.GhostProgressContractTest"`

## Scenarios

1. **First launch teaches**: fresh install → open app → real IELTS session within two
   taps, airplane mode on. Expect: curated words, swipable cards, zero demo fallbacks.
2. **Tap accounting**: one right swipe → exactly one success/attempt/reward in storage;
   one left swipe + force-stop → exactly one failure + rescheduling after relaunch;
   same tap twice → single stored effect.
3. **Unknown words**: event for an unknown word id → loud rejection, zero progress rows.
4. **Scheduling honesty**: study once, return past the review date → word returns as due,
   never as new; mastered + failed → known, advancable again.
5. **Spelling weight**: two failed spellings → sooner review than an always-correct word;
   production-only successes never master.
6. **Streak truth**: unprotected missed day → zero; freeze held → continuity with one
   freeze consumed and the gap day absent from history, XP, and dashboard.
7. **Shell coherence**: cold start lands on home; every visible action reaches real
   content; onboarding appears exactly once.

## Expected outcomes

All scenarios pass; golden suite green except streak-trio red until its ticket lands;
dashboard totals match stored events one to one.
