# Contract: Learning Events

Source of truth for all learning. Projections (progress, SRS, dashboard) derive from
these and are never edited directly.

- Shape: eventId, wordId, eventType ∈ {recall-success, recall-failure,
  production-success, production-failure}, occurredAt, idempotencyKey =
  `sessionId:wordId:outcome:tapOrdinal`, sessionId.
- Exactly-once: re-applying an idempotencyKey MUST produce zero additional effect
  (verified by golden test: same tap twice → one attempt, no further writes).
- Unknown words: events for wordIds outside the store are rejected loudly with zero
  stored side effects (verified by golden test: no ghost progress row).
- Durability: written at tap time; app kill loses nothing already tapped.
- Failure events remain stored LearningEvents; demotion of mastery state never erases them.
