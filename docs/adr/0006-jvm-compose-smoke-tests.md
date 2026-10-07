# ADR-0006 — JVM testing for the Compose shell: contract tests now, UI smoke on device

Date: 2026-10-04
Status: Accepted
Supersedes: none. Related: Constitution Article IV (test-first), Phase 1 plan.

## Decision

1. The Phase 1 shell closes with **JVM-only unit tests**: the updated
   `ShellRouteContractTest` (route/label/icon uniqueness for the four tabs) plus the
   `countNewWordsForUser` definition contract. Compose **smoke tests for the four tab
   surfaces live in `androidTest`** (`ShellTabSmokeTest`) and run on a real device or
   emulator together with the screenshot pass that already gates Phase 1 closure.
2. Robolectric was **attempted and withdrawn**. A temporary dependency
   (robolectric 4.16 + vintage engine + test-manifest) was added to run the smoke tests
   on the JVM; in this build environment the Robolectric/Compose test JVM hangs and then
   crashes the Gradle daemon (reproduced twice, including a daemon death). The
   dependency was removed to protect dependency hygiene; the test files moved intact to
   `androidTest`.
3. `countNewWordsForUser` (WordMasterDao) is accepted as a read-only companion to the
   frozen new-word definition; a contract test pins it to the same LEFT-JOIN semantics
   (`new word count matches the frozen no-progress-row definition`).

## Consequences

- Every tab-surface UI assertion (compose-without-crash, Arabic content visible, four
  distinct surfaces) is executed by `connectedDebugAndroidTest` on the PM's device,
  together with the light/dark screenshots required for Phase 1 closure.
- The planned scope-expansion ADR for Phase 3/4 (units) and the production-credit ADR
  for Phase 2 keep their reserved numbering from the PM plan (0006 was consumed here;
  renumber those to 0007/0008 or dictate otherwise).
- Re-attempting Robolectric is allowed only with a RAM budget and `--max-workers`
  tuning; not before Phase 2.
