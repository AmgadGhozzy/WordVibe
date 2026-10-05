package com.venom.stackcard.session

import com.venom.domain.model.WordMaster

/**
 * Pure session planning (Step 3b): the deck serves due words first, then
 * enough new words to fill the session limit. Mirrors the loadSrsWords
 * composition verbatim. The frozen new/due definitions (no-progress-row /
 * next-review-due) live in the repository layer and are untouched here — the
 * planner only composes what the repositories return.
 */
object SessionPlanner {

    /** Words served per session (migrated verbatim from loadSrsWords). */
    const val SESSION_LIMIT = 15

    fun plan(
        due: List<WordMaster>,
        newWords: List<WordMaster>,
        limit: Int,
    ): List<WordMaster> {
        val serving = due.take(limit)
        val newNeeded = limit - serving.size
        val fresh = if (newNeeded > 0) newWords.take(newNeeded) else emptyList()
        return serving + fresh
    }
}
