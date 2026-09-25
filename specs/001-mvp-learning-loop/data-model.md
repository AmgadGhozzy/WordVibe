# Data Model: MVP Learning Loop

Derived from spec entities; storage is Room v1 (clean installs define the schema).

## Word (corpus entry)

Stored word with teaching material plus retrieval targets plus tags plus review sign-off.
Fields: id, wordEn, pos, cefrLevel, phoneticUs/Uk/Ar, translit, syllabify, definitionEn,
definitionAr, usageNote, category, primarySense, semanticTags, register, mnemonicAr,
wordFamily, synonyms, antonyms, examples (per CEFR), collocations, relatedWords,
translations, unitId, recallTarget (gapped sentence), productionTarget (expectedForm +
acceptedVariants), source, ieltsTopic, ieltsRelevance, reviewer, reviewedAt.
Validation: per-field corpus contract (unique lemma, non-empty meaning/definition,
example uses a valid form, collocations ≥ 1, synonyms ≥ 1, reviewer sign-off present).

## LearningEvent (source of truth)

One stored signal for one word at one time. Fields: eventId, wordId, eventType (one of
recall-success / recall-failure / production-success / production-failure), occurredAt,
idempotencyKey (session + word + outcome + tap ordinal), sessionId. Re-applying a key
produces zero additional effect. Never edited, never deleted by the app.

## WordProgress (projection)

Per-word derived state: counters (recallSuccess, productionSuccess, totalAttempts,
lapsesCount), stability, difficulty, knownState (NEW / SEEN / LEARNING / KNOWN /
MASTERED), lastReview, nextReview, bookmarked, firstSeen. Computed from events only.
Transitions: NEW → SEEN/LEARNING on first events → KNOWN on first successes → MASTERED on
cumulative 3/3 + stability ≥ 2.0; any real failure demotes MASTERED → KNOWN.

## Session (bounded run)

In-memory accumulation over already-durable events plus session-level bookkeeping
(duration, counts, completion). Creates no learning state of its own.

## Streak (counter + inventory)

currentStreak, bestStreak, lastActiveDate, streakFreezes. Unbroken chain ending today or
yesterday; unprotected gap → zero; one freeze bridges exactly one missed day while
recording nothing; multi-day gaps never consume freezes.
