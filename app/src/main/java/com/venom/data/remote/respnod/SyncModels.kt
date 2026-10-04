package com.venom.data.remote.respnod

// Sync Events

// Sync Events
 
data class SyncRequest(
    val events: List<ClientEventDto>,
    val wordProgress: List<WordProgressDto>? = null
 )
 
data class ClientEventDto(
    val actionType: String,
    val relatedWordId: Int? = null,
    val timeSpentMs: Int? = 0,
    val isCorrect: Boolean? = null,
    val xpAwarded: Int? = 0,
    val idempotencyKey: String,
    val createdAt: String
 )

data class WordProgressDto(
    val wordId: Int,
    val knownState: String,
    val difficulty: Int,
    val stability: Float,
    val lapsesCount: Int,
    val totalAttempts: Int,
    val recallSuccess: Int,
    val productionSuccess: Int,
    val bookmarked: Boolean,
    val firstSeen: String,
    val lastReview: String? = null,
    val nextReview: String? = null,
    val updatedAt: String? = null
)
 
data class SyncResponse(
    val success: Boolean = true,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val error: String? = null
 )

data class ProfileDto(
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDate: String? = null,
    val dailyGoalXp: Int = 50
)

data class StreakDto(
    val current: Int = 0,
    val best: Int = 0
)

// Check Usage

data class CheckUsageRequest(
    val usageType: String,
    val count: Int = 1
)

data class CheckUsageResponse(
    val allowed: Boolean,
    val plan: String,
    val usageType: String,
    val current: Int,
    val limit: Int,
    val remaining: Int,
    val message: String? = null,
    val error: String? = null
)

// Leaderboard

/**
 * p_date is intentionally omitted — the server function defaults to CURRENT_DATE
 * (with a fallback to the most recent date that has data).
 * Sending p_date=null explicitly can cause a PostgREST type-cast rejection.
 */
data class LeaderboardRequest(
    val p_limit: Int = 50
)

data class LeaderboardEntry(
    val rank: Long,
    val userId: String,
    val displayName: String?,
    val avatarUrl: String?,
    val xp: Int
)

// Supabase Auth

/** POST /auth/v1/token?grant_type=id_token */
data class SupabaseIdTokenRequest(
    val provider: String = "google",
    val idToken: String,
    val nonce: String? = null
)

/** POST /auth/v1/signup  (anonymous) */
data class SupabaseAnonSignUpRequest(
    // empty body — the API key header is enough
    val data: Map<String, String>? = null
)

/** Shared response from /auth/v1/token and /auth/v1/signup */
data class SupabaseAuthResponse(
    val accessToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long = 3600,
    val expiresAt: Long? = null,
    val refreshToken: String,
    val user: SupabaseUser? = null
)

/** POST /auth/v1/token?grant_type=refresh_token */
data class SupabaseRefreshRequest(
    val refreshToken: String
)

/** Supabase user object returned inside auth responses */
data class SupabaseUser(
    val id: String,
    val aud: String? = null,
    val role: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val userMetadata: SupabaseUserMetadata? = null,
    val appMetadata: Map<String, Any>? = null
)

data class SupabaseUserMetadata(
    val name: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val picture: String? = null,
    val providerId: String? = null
)

// The request now sends the old anonymous UID (not the Google token)
data class LinkIdentityRequest(
    val anonymousUid: String
)

// Response no longer carries session tokens (client already has them)
data class LinkIdentityResponse(
    val success: Boolean,
    val migrated: Boolean? = null,
    val oldUserId: String? = null,
    val userId: String? = null,
    val error: String? = null,
    val detail: String? = null,
    val reason: String? = null
)
