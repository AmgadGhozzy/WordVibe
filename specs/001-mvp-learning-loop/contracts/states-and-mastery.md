# Contract: States and Mastery

- new: stored word with no progress row (left-join definition; never ID lists from UI).
- due: review timestamp present and past, nothing else.
- seen / learning: distinct intermediate states with documented transitions.
- MASTERED ⟺ cumulative recallSuccess ≥ 3 AND productionSuccess ≥ 3 AND stability ≥ 2.0.
  No windows; spelling-only or recall-only practice can never master.
- Any real failure demotes MASTERED → KNOWN deterministically; history is preserved and
  the word can advance again. Historical evidence ≠ current mastery state.
- No weakening without reopening the freeze (ADR + approval).
