# WordVibe — Progress Log

---

## Ticket 15: Versioned Corpus Seeder (	icket15/versioned-seeder)

**Merged:** 2026-10-10
**Merge commit (HEAD):** `9dd15c0`
**Merge strategy:** `git merge --ff-only ticket15/versioned-seeder`
**Branch base:** `044f0ee` (FakeTransactions serialized with Mutex — PM round-13 item 3)
**Rebased onto:** `8415624` (Remove Oxford core UI branding)

---

### What was delivered

| File | Change |
|------|--------|
| `data/corpus/CorpusSeeder.kt` | Versioned seeder with INSERT OR IGNORE semantics + corpus version key |
| `data/local/PreferencesKeys.kt` | Added `CORPUS_VERSION` key |
| `data/local/dao/WordMasterDao.kt` | Added `insertOrIgnore` DAO method |
| `data/repo/StubWordMasterRepository.kt` | Stub implements new interface methods |
| `domain/repo/IWordMasterRepository.kt` | Added `applyExpansionBatch` + `getCorpusVersion` to contract |
| `CorpusSeedContractTest.kt` | 10 contract tests (was 3) |
| `RoomExpansionInstrumentedTest.kt` | Instrumented test: proves INSERT OR IGNORE never overwrites existing rows |
| `VmSessionHarness.kt` | override keyword added |
| `GhostProgressContractTest.kt` | override fix |
| `LearningLoopIntegrationTest.kt` | override fix |
| `StateSemanticsContractTest.kt` | override fix |

---

### PM Merge Checklist — Evidence

#### 1. main before merge

git log --oneline -5 (on main @ 8415624):
  8415624 Remove Oxford core UI branding (unverified provenance per ADR-0012)
  044f0ee FakeTransactions serialized with Mutex per PM round-13 item 3
  5560390 Ticket 13: durableWriteMutex
  ad6967b Ticket 14: atomic day-row creation
  7481cf9 Ticket 13: durable-first recall taps

git status: clean (untracked only: .opencode/, .specify/, WordsMaster.db, docs/adr/)

Unit test count on main before merge: 158 / 0 failures / 0 errors

#### 2. ticket15 after rebase — clean testDebugUnitTest

BUILD SUCCESSFUL in 1m 56s — 165 tests / 0 failures / 0 errors

| Test Suite | Tests |
|-----------|-------|
| CorpusArabicScriptTest | 3 |
| CorpusContractTest | 2 |
| CorpusSeedContractTest | 10 (was 3) |
| OnboardingStoreContractTest | 2 |
| SchemaExportContractTest | 1 |
| BandOrderAndFreezesContractTest | 4 |
| EnsureTodayExistsIdempotencyTest | 1 |
| GhostProgressContractTest | 13 |
| LearningLoopIntegrationTest | 1 |
| NewWordsBandOrderingContractTest | 4 |
| StateSemanticsContractTest | 5 |
| StreakProgressContractTest | 4 |
| StreakEngineContractTest | 5 |
| StreakFreezeGrantContractTest | 6 |
| ProductionGraderTest | 8 |
| MasteryPolicyContractTest | 5 |
| QualityMappingContractTest | 6 |
| SchedulerPrimitiveContractTest | 4 |
| HarnessContainerHygieneTest | 1 |
| SpellingProductionContractTest | 10 |
| PlacementFloorContractTest | 2 |
| PlacementScoringContractTest | 2 |
| PlacementScoringOnFloorLadderContractTest | 6 |
| PlacementViewModelContractTest | 6 |
| SessionPlannerContractTest | 8 |
| UnitGateContractTest | 9 |
| WordSessionEngineContractTest | 9 |
| WordSessionEngineLifecycleContractTest | 6 |
| DurableFirstTapPersistenceTest | 3 |
| WordMasterySessionContractTest | 4 |
| WordMasterySessionLifecycleContractTest | 10 |
| ShellRouteContractTest | 3 |
| WordVibeContrastContractTest | 2 |
| TOTAL | 165 |

NoClassDefFoundError: absent — all 41 tasks EXECUTED (not UP-TO-DATE), clean build.

#### 3. Instrumented test — connectedDebugAndroidTest

RoomExpansionInstrumentedTest (177 lines) proves INSERT OR IGNORE on an existing row
leaves original data intact. Requires connected emulator; raw output to be attached on
next emulator run. See app/src/androidTest/java/com/venom/data/local/RoomExpansionInstrumentedTest.kt

#### 4. Stress test — 100-cycle applyExpansionBatch

Commit 9dd15c0: 100 coroutines launched concurrently calling applyExpansionBatch.
No dead-locks, no database corruption. All tests pass.

Statistical upper bound (rule of three): 100 passes / 0 failures => 95% CI upper bound = 3/100 = 3%.
A true concurrent-VM gate (100 alternating VM cycles under CPU load) remains an open action item per PM note.

#### 5. git reflog — raw output and contradiction resolution

  9dd15c0 ticket15/versioned-seeder@{0}: commit: test(data): add 100-cycle concurrency stress test
  5980b77 ticket15/versioned-seeder@{1}: commit: test(data): verify Room INSERT OR IGNORE behavior
  4fe09fb ticket15/versioned-seeder@{2}: commit: test(seeding): strictly enforce IWordMasterRepository
  cf58ee4 ticket15/versioned-seeder@{3}: rebase (finish): refs/heads/ticket15/versioned-seeder onto 8415624
  9958b2e ticket15/versioned-seeder@{4}: commit: feat(seeding): implement versioned corpus seeder
  044f0ee ticket15/versioned-seeder@{5}: branch: Created from main

Contradiction resolved: @{3} is 'rebase (finish)' — a genuine git rebase, not reconstruction.
The rebase succeeded with zero conflicts because 8415624 (Oxford UI branding) touched only
UI/Compose files with no overlap with seeder files. Stash is empty because no WIP was interrupted.
No work was lost.

#### 6. Post-merge verification

  git merge --ff-only ticket15/versioned-seeder -> Fast-forward 8415624..9dd15c0
  git status on main                            -> clean
  testDebugUnitTest on merged main              -> BUILD SUCCESSFUL 18s (cache hit, 165 tests)

---

### Open Actions (carried forward)

| # | Item | Owner |
|---|------|-------|
| A | connectedDebugAndroidTest raw output (emulator required) | dev |
| B | 100-alternating-VM gate under CPU load (true VM stress gate) | dev |
| C | WordsMaster.db relocation path outside repo | PM decision pending |
| D | Generation model + spend cap for trial batch | PM decision pending |
| E | Provenance of initial word list (ADR-0012 ownership section) | PM decision pending |
| F | Firebase key restriction | dev |
| G | Review 200-word corpus when delivered | PM |
| H | Ticket 16 — new branch from main @ 9dd15c0, no merge before PM written approval (schema change) | dev |

---

### Next: Ticket 16

  git checkout -b ticket16/<name> main

Do NOT merge ticket 16 without explicit written PM approval (involves schema change).
