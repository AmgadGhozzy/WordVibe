package com.venom.domain.repo

import kotlinx.coroutines.flow.StateFlow

/**
 * Identity seam for presentation: the local identity store is Android-bound
 * (Settings.Secure), so view models depend on this interface to stay testable
 * on the JVM. Mirrors the existing IWordMasterRepository / IQuizRepository pattern.
 */
interface IUserIdentityRepository {
    data class AuthState(
        val isSignedIn: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
        val photoUrl: String? = null
    )

    val authState: StateFlow<AuthState>
    fun isSignedIn(): Boolean
    suspend fun getCurrentUserId(): String
}
