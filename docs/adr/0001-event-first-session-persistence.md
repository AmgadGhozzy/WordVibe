# Event-first session persistence

Every tap is persisted as a durable LearningEvent at tap time; session finalization only reconciles. We rejected session-transaction (RAM until finalize) because an app kill must not lose taps, and a learning app's retrieval events are themselves the valuable data.

## Considered Options

Session-transaction: simpler writes, but loses everything since the last finalize on kill — unacceptable for the kill-survival contract.
