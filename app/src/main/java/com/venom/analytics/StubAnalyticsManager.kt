package com.venom.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * NOT STARTED: analytics backend. All calls are no-ops by design until a gated
 * post-MVP decision wires a real implementation behind [AnalyticsManager].
 */
class StubAnalyticsManager @Inject constructor() : AnalyticsManager {

    private val _isEnabled = MutableStateFlow(true)

    override val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    override fun initialize(enableCollection: Boolean) {}

    override fun logScreenView(screenName: String, screenClass: String?) {}

    override fun logEvent(eventName: String, params: Map<String, Any>?) {}

    override fun logButtonClick(buttonName: String, screenName: String) {}

    override fun logFeatureOpened(featureName: String, additionalParams: Map<String, Any>?) {}

    override fun logFeatureCompleted(featureName: String, result: String?, durationMs: Long?) {}

    override fun logError(errorType: String, errorMessage: String, screenName: String?) {}

    override fun logPerformanceMarker(markerName: String, durationMs: Long, additionalParams: Map<String, Any>?) {}

    override fun setUserProperty(name: String, value: String?) {}

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    override suspend fun resetAnalyticsData() {}
}
