<!-- Sync Impact Report (temporary review scratch; remove before commit):
- Version change: none → 1.0.0 (initial ratification)
- Principles: all five new, derived from WordVibe MVP Spec v1.1 FROZEN
- Added sections: Additional Constraints, Development Workflow
- Follow-up TODOs: none
-->

# WordVibe Constitution

## Core Principles

### I. Event-First Persistence (NON-NEGOTIABLE)
Every tap persists as a durable LearningEvent at tap time: tap → durable LearningEvent → idempotency key → projection update → Session UI state. Session finalization reconciles only and is never the sole save point. A kill or crash MUST lose nothing already tapped.

### II. Exactly-Once via Idempotency Keys (NON-NEGOTIABLE)
Every LearningEvent carries an idempotencyKey (session + word + outcome + tap ordinal). The store applies each key at most once: re-application MUST produce zero additional learning effect. Unknown-word events are rejected loudly and MUST never fabricate progress rows.

### III. Frozen Learning Contracts
Mastery, Quality mapping, Streak, and Corpus rules are frozen product decisions: Mastery is cumulative recall≥3 AND production≥3 AND stability≥2.0 with always-demote on any real failure; QualityPolicy owns judgement while the Scheduler stays a dumb primitive; a Freeze preserves continuity only and fabricates nothing; every Corpus word passes the per-field acceptance contract with reviewer sign-off. Changes require a new ADR and explicit freeze reopen — never silent edits during implementation.

### IV. Test-First with Golden Contracts (NON-NEGOTIABLE)
Contracts are encoded as behavior-only tests BEFORE fixes: each contract has a test, current code is classified PASS/FAIL per contract, and no fix of a contracted defect lands without a contract/regression test that fails before the fix and passes after it. Infrastructure and scaffolding changes are exempt; contracted behavior is red-to-green mandatory.

### V. Minimal Scope
The MVP is IELTS vocabulary for Arabic speakers: home, flashcards, spelling, SRS, honest progress. Ten languages, OCR, translation, conversation, leaderboard, sync, billing, ads, and remote backends are out. Fewest files, shortest diff, no speculative generality; pre-production means no database migrations.

## Additional Constraints

Kotlin + Jetpack Compose + Hilt + Room, offline-first (bundled pilot Corpus, no network for learning), Arabic-first RTL experience, JUnit 5 unit tests run via `:app:testDebugUnitTest`. Agent working files (`.scratch/`, `docs/`, `CONTEXT.md`, `AGENTS.md`) live on disk only and MUST never be committed.

## Development Workflow

Work flows Gates G0→G7 through tracer-bullet tickets with declared blocking edges, frontier-first. Each implementation runs red-green at the agreed seams, then a two-axis review (Standards + Spec) before commit. Commits hold code and tests only.

## Governance

This constitution supersedes all other practices. Amendments require a documenting ADR, user approval, and a migration plan for in-flight tickets. Versioning is semantic: MAJOR for removed/redefined principles, MINOR for new principles, PATCH for clarifications. Every review MUST verify compliance against Principles I–IV.

**Version**: 1.0.0 | **Ratified**: 2026-09-24 | **Last Amended**: 2026-09-24
