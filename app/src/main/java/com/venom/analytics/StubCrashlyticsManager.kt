package com.venom.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * NOT STARTED: crash reporting backend. All calls are no-ops by design until a gated
 * post-MVP decision wires a real implementation behind [CrashlyticsManager].
 */
class StubCrashlyticsManager @Inject constructor() : CrashlyticsManager {

    override fun initialize(enableCollection: Boolean) {}

    override fun logNonFatalException(throwable: Throwable, message: String?) {}

    override fun logBreadcrumb(message: String) {}

    override fun setCustomKey(key: String, value: String) {}

    override fun setCustomKey(key: String, value: Boolean) {}

    override fun setCustomKey(key: String, value: Int) {}

    override fun setCustomKey(key: String, value: Long) {}

    override fun setCustomKey(key: String, value: Float) {}

    override fun setCustomKey(key: String, value: Double) {}

    override fun setCurrentScreen(screenName: String) {}

    override fun setCurrentFeature(featureName: String) {}

    override fun setUserId(userId: String?) {}

    override fun setCrashlyticsCollectionEnabled(enabled: Boolean) {}

    override suspend fun deleteUnsentReports() {}

    override suspend fun hasUnsentReports(): Boolean = false

    override suspend fun sendUnsentReports() {}
}
