package com.venom.analytics

import android.util.Log
import com.venom.wordvibe.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NOT STARTED: crash reporting backend. All calls are no-ops toward any backend
 * by design until a gated post-MVP decision wires a real implementation.
 *
 * P-07 observability guarantee: in DEBUG builds every call is mirrored to logcat
 * (tag `WordVibeCrash`) so "loudly rejected" events are actually loud while
 * developing. Release builds stay fully silent (no collection, no logs).
 */
@Singleton
class StubCrashlyticsManager @Inject constructor() : CrashlyticsManager {

    private fun log(message: String, throwable: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        try {
            if (throwable != null) Log.w(TAG, message, throwable)
            else Log.w(TAG, message)
        } catch (_: Throwable) {
            // JVM unit tests: android.util.Log is stubbed out — keep tests alive.
            println("$TAG: $message${throwable?.let { " — ${it.message}" } ?: ""}")
        }
    }

    override fun initialize(enableCollection: Boolean) {
        log("initialize(enableCollection=$enableCollection)")
    }

    override fun logNonFatalException(throwable: Throwable, message: String?) {
        log("non-fatal: ${message ?: ""}", throwable)
    }

    override fun logBreadcrumb(message: String) {
        log("breadcrumb: $message")
    }

    override fun setCustomKey(key: String, value: String) {
        log("key: $key=$value")
    }

    override fun setCustomKey(key: String, value: Boolean) {
        log("key: $key=$value")
    }

    override fun setCustomKey(key: String, value: Int) {
        log("key: $key=$value")
    }

    override fun setCustomKey(key: String, value: Long) {
        log("key: $key=$value")
    }

    override fun setCustomKey(key: String, value: Float) {
        log("key: $key=$value")
    }

    override fun setCustomKey(key: String, value: Double) {
        log("key: $key=$value")
    }

    override fun setCurrentScreen(screenName: String) {
        log("screen: $screenName")
    }

    override fun setCurrentFeature(featureName: String) {
        log("feature: $featureName")
    }

    override fun setUserId(userId: String?) {
        log("userId: $userId")
    }

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        log("collectionEnabled=$enabled")
    }

    override suspend fun deleteUnsentReports() {
        log("deleteUnsentReports")
    }

    override suspend fun hasUnsentReports(): Boolean {
        log("hasUnsentReports -> false")
        return false
    }

    override suspend fun sendUnsentReports() {
        log("sendUnsentReports")
    }

    private companion object {
        const val TAG = "WordVibeCrash"
    }
}
