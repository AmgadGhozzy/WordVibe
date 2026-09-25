# Feature Specification: MVP Learning Loop

**Feature Branch**: `001-mvp-learning-loop`

**Created**: 2026-09-24

**Status**: Draft

**Input**: User description: "WordVibe MVP learning loop baseline from frozen v1.1 spec and ratified constitution — consume, do not renegotiate."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Start learning immediately (Priority: P1)

An Arabic-speaking IELTS learner opens the app for the first time and starts a real
vocabulary session within two taps, with no sign-in and no download. Every word shows its
Arabic meaning, English definition, pronunciation, and at least one example.

**Why this priority**: Without bundled content there is no product; this is the gate every
other story stands on.

**Independent Test**: Fresh install, airplane mode on, open app, start a session — real
IELTS words appear and can be swiped.

**Acceptance Scenarios**:

1. **Given** a fresh install, **When** the learner opens the app, **Then** a curated set of
   IELTS words is present and a session starts within two taps.
2. **Given** a restarted app, **When** the learner opens it again, **Then** no word is
   duplicated and no progress is lost.

---

### User Story 2 - Every tap counts exactly once (Priority: P1)

The learner swipes cards and practices spelling knowing each tap is recorded exactly once —
remembered, forgotten, killed mid-session, or retried — so all numbers shown are true.

**Why this priority**: Double-counted successes and vanished failures make every other
number a lie; trust is the product.

**Independent Test**: Perform one remembered tap and kill the app; relaunch shows exactly
one success for that word. Repeat the same tap twice; still exactly one stored effect.

**Acceptance Scenarios**:

1. **Given** a remembered card, **When** the learner swipes right, **Then** exactly one
   success, one attempt, and one reward are stored — never two.
2. **Given** a forgotten card, **When** the learner swipes left and kills the app,
   **Then** exactly one failure and one rescheduling are stored after relaunch.
3. **Given** an event for a word outside the store, **When** it is recorded, **Then** it is
   loudly rejected and no progress row is fabricated.

---

### User Story 3 - Right words at the right time (Priority: P2)

Sessions serve truly new words and truly due words; mastered words stay mastered until a
real failure demotes them. Never-seen words never repeat disguised as new.

**Why this priority**: Correct scheduling is the learning engine; wrong words waste study
time even when counting is honest.

**Independent Test**: Study a word once, close the app for a day past its review date —
the next session brings it back as due, not as new.

**Acceptance Scenarios**:

1. **Given** unseen words exist, **When** a session loads new material, **Then** only
   never-studied words are labelled new.
2. **Given** a word whose review date passed, **When** a session loads, **Then** it appears
   as due.
3. **Given** a mastered word, **When** the learner fails it, **Then** it demotes to known
   deterministically and can advance again.

---

### User Story 4 - Spelling counts (Priority: P2)

Spelling practice records successes and failures into the learning state, so production
weakness shortens scheduling and production strength advances it.

**Why this priority**: Without production signals the app tests recognition only, and
IELTS writing stays untrained.

**Independent Test**: Fail the same word's spelling twice — its next review comes sooner
than a word only ever answered correctly.

**Acceptance Scenarios**:

1. **Given** a failed spelling attempt, **When** it is stored, **Then** the word's next
   review is sooner (never a token minimum-shortening exception).
2. **Given** repeated production successes without recall successes, **When** mastery is
   evaluated, **Then** the word is not mastered.

---

### User Story 5 - Honest streak and dashboard (Priority: P3)

The streak and dashboard reconcile exactly with stored learning events: gaps reset to
zero, one freeze bridges exactly one missed day while fabricating nothing, and every
displayed total matches the event log.

**Why this priority**: Motivation numbers must be earned; forged continuity destroys trust
in all progress.

**Independent Test**: Miss a day with a freeze held, then check history — streak preserved,
but the missed day shows no study, no reward, no activity.

**Acceptance Scenarios**:

1. **Given** an unprotected missed day, **When** the learner returns, **Then** the streak
   reads zero.
2. **Given** a missed day with a freeze available, **When** the learner returns, **Then**
   continuity holds, one freeze is consumed, and the gap day stays absent everywhere.
3. **Given** any dashboard total, **When** compared against stored events, **Then** they
   match one to one.

---

### User Story 6 - Coherent shell, no dead ends (Priority: P3)

The learner reaches home, flashcards, and spelling directly; onboarding is asked once and
remembered; the whole flow reads as Arabic-first; no tap lands on an empty screen.

**Why this priority**: A correct engine behind unreachable or empty screens still teaches
nothing.

**Independent Test**: Cold start lands on home; every visible button leads somewhere real.

**Acceptance Scenarios**:

1. **Given** a returning learner, **When** the app opens, **Then** it lands on home, not
   an empty screen.
2. **Given** any navigation action, **When** taken, **Then** no placeholder or empty
   destination is reachable.

---

### Edge Cases

- What happens when the app is killed between two taps? Nothing tapped is lost; nothing
  untapped appears.
- How does the system handle an event for an unknown word? Loud rejection, zero stored
  side effects.
- What happens on a two-or-more-day gap, with or without freezes? Streak resets to zero;
  no freeze is consumed for the unbridgeable.
- What happens when the learner views without studying? Displayed streak holds without
  incrementing; nothing new is stored.
- What happens when the corpus store is empty at launch? First launch always seeds it;
  demo fallback words never ship as content.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST ship a bundled pilot corpus of 100–150 curated IELTS words that
  seeds the local store exactly once on first launch, offline.
- **FR-002**: Every corpus word MUST carry source/reference, CEFR level, IELTS topic,
  relevance rationale, Arabic meaning, English definition, example(s), collocations, word
  family, one recall retrieval target, one production target, and reviewer sign-off.
- **FR-003**: System MUST persist every learning tap as a durable event at tap time with an
  idempotency key; re-applying a key MUST produce zero additional effect.
- **FR-004**: System MUST reject events for unknown words loudly without storing progress.
- **FR-005**: System MUST define new as stored word with no progress, due as past review
  date, mastered as cumulative recall≥3 AND production≥3 AND stability≥2.0.
- **FR-006**: Any real failure MUST demote a mastered word to known deterministically,
  preserving history.
- **FR-007**: System MUST map the four fixed signals (recall success/failure, production
  success/failure) through an app-owned quality policy into deterministic scheduling.
- **FR-008**: Production failure MUST genuinely shorten scheduling (negative quality);
  recall failure MUST shorten sharply (strong negative, lower quality).
- **FR-009**: System MUST reset the streak to zero on unprotected gaps, preserve continuity
  on exactly one freeze per single missed day, and fabricate no study for bridged days.
- **FR-010**: Dashboard totals MUST reconcile exactly with stored learning events.
- **FR-011**: System MUST keep only home, flashcards, and spelling reachable; onboarding is
  asked once and remembered; the experience is Arabic-first with no reachable placeholders.

### Key Entities *(include if feature involves data)*

- **Word**: A corpus entry with teaching material, retrieval targets, CEFR/topic tags, and
  review sign-off.
- **LearningEvent**: The source of truth — one stored signal for one word at one time,
  carrying session, type, timestamp, and idempotency key.
- **WordProgress**: A projection of events per word (counters, stability, state, review
  date); never edited directly.
- **Session**: A bounded learning run reconciling already-durable events.
- **Streak**: A counter of consecutive active days plus a consumable freeze inventory.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time learner starts a real session within two taps of opening the app.
- **SC-002**: 100% of performed taps survive an app kill; zero taps are stored twice.
- **SC-003**: Zero unreachable-tap destinations: every visible action leads to real content.
- **SC-004**: Every dashboard total matches its underlying stored events with zero mismatch.
- **SC-005**: A returning learner after a missed day sees a truthful streak (zero unprotected,
  preserved with exactly one freeze consumed and no forged study).
- **SC-006**: 100% of pilot corpus words pass the per-field acceptance contract on review.

## Assumptions

- Pilot corpus of 100–150 words is human-curated and reviewed; no external word-list
  dependency for v1.
- Single learner on one device; cloud sync is out of scope.
- Offline-first: learning never requires network.
- Pre-production: no upgrade migrations; clean installs define the schema.
- Existing flashcard, spelling, and dashboard surfaces are reused, not rebuilt.
