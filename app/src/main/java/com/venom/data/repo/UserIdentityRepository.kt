package com.venom.data.repo

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.venom.data.local.dao.QuizAttemptDao
import com.venom.data.local.dao.UsageDailyDao
import com.venom.data.local.dao.UserIdentityDao
import com.venom.data.local.dao.XpEventDao
import com.venom.data.local.entity.UserIdentityEntity
import com.venom.domain.model.UserIdentity
import com.venom.domain.repo.IUserIdentityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SignInResult(
    val identity: UserIdentity,
    val previousAnonymousUid: String? = null
)

@Singleton
class UserIdentityRepository @Inject constructor(
    private val dao: UserIdentityDao,
    @ApplicationContext private val context: Context,
    private val activityRepository: UserActivityRepository,
    private val wordProgressRepository: UserWordProgressRepository,
    private val xpEventDao: XpEventDao,
    private val quizAttemptDao: QuizAttemptDao,
    private val usageDailyDao: UsageDailyDao,
) : IUserIdentityRepository {
    private val _authState = MutableStateFlow(IUserIdentityRepository.AuthState())
    override val authState: StateFlow<IUserIdentityRepository.AuthState> = _authState.asStateFlow()

    private var currentUserId: String? = null

    init {
        refreshAuthState()
    }

    private fun refreshAuthState() {
        val userId = currentUserId
        _authState.value = IUserIdentityRepository.AuthState(
            isSignedIn = userId != null,
            userName = null,
            userEmail = null,
            photoUrl = null
        )
    }

    override fun isSignedIn(): Boolean = currentUserId != null

    /**
     * Phase 2 closure: a new identity is minted ONLY when the store has no row
     * (true absence). A failed read propagates loudly instead of silently
     * minting a replacement identity, which would orphan all learner data.
     */
    suspend fun getOrCreateIdentity(): UserIdentity = withContext(Dispatchers.IO) {
        val existing = try {
            dao.getIdentity()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw IllegalStateException("identity read failed; refusing to mint a replacement", e)
        }
        if (existing != null) {
            currentUserId = existing.userId
            dao.updateLastActive(System.currentTimeMillis())
            refreshAuthState()
            return@withContext existing.toDomain()
        }

        val userId = UUID.randomUUID().toString()
        val identity = createLocalIdentity(userId, isAnonymous = true)
        dao.insertOrUpdate(identity.toEntity())
        currentUserId = userId
        refreshAuthState()
        identity
    }

    override suspend fun getCurrentUserId(): String = getOrCreateIdentity().userId

    suspend fun signInWithGoogle(idToken: String): SignInResult = withContext(Dispatchers.IO) {
        val previousUid = currentUserId
        val userId = UUID.randomUUID().toString()
        val identity = createLocalIdentity(userId, isAnonymous = false)
        dao.insertOrUpdate(identity.toEntity())
        currentUserId = userId
        refreshAuthState()
        SignInResult(identity = identity, previousAnonymousUid = previousUid)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        currentUserId = null
        refreshAuthState()
        Log.d(TAG, "User signed out")
    }

    private fun createLocalIdentity(userId: String, isAnonymous: Boolean): UserIdentity {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()

        return UserIdentity(
            userId = userId,
            deviceId = deviceId,
            isAnonymous = isAnonymous,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )
    }

    private fun UserIdentityEntity.toDomain() = UserIdentity(
        userId = userId,
        deviceId = deviceId,
        isAnonymous = isAnonymous,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt
    )

    private fun UserIdentity.toEntity() = UserIdentityEntity(
        userId = userId,
        deviceId = deviceId,
        isAnonymous = isAnonymous,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt
    )

    companion object {
        private const val TAG = "UserIdentity"
    }
}
