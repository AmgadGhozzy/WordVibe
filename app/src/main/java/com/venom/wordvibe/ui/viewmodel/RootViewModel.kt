package com.venom.wordvibe.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venom.data.local.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SubtitleOverrideState {
    data object Default : SubtitleOverrideState
    data class Resource(val resId: Int) : SubtitleOverrideState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val onboardingStore: OnboardingStore,
) : ViewModel() {

    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _subtitleOverride =
        MutableStateFlow<SubtitleOverrideState>(SubtitleOverrideState.Default)
    val subtitleOverride: StateFlow<SubtitleOverrideState> = _subtitleOverride.asStateFlow()

    private val _appTheme = MutableStateFlow(com.venom.domain.model.AppTheme.SYSTEM)
    val appTheme: StateFlow<com.venom.domain.model.AppTheme> = _appTheme.asStateFlow()

    private val _sharedUri = MutableStateFlow<String?>(null)
    val sharedUri: StateFlow<String?> = _sharedUri.asStateFlow()

    private val _paywallVisible = MutableStateFlow(false)
    val paywallVisible: StateFlow<Boolean> = _paywallVisible.asStateFlow()

    fun showPaywall() { _paywallVisible.value = true }
    fun hidePaywall() { _paywallVisible.value = false }

    fun setSharedUri(uri: String) {
        _sharedUri.value = uri
    }

    fun consumeSharedUri() {
        _sharedUri.value = null
    }

    init {
        viewModelScope.launch {
            _onboardingDone.value = onboardingStore.onboardingDone.first()
            isLoadingOnboardingState = false
        }
        viewModelScope.launch {
            onboardingStore.appTheme.collect { _appTheme.value = it }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingStore.complete()
            _onboardingDone.value = true
        }
    }

    fun setSubtitleResOverride(resId: Int) {
        _subtitleOverride.value = SubtitleOverrideState.Resource(resId)
    }

    fun clearSubtitleResOverride() {
        _subtitleOverride.value = SubtitleOverrideState.Default
    }

    /**
     * Resolved only after the persisted onboarding state is read. The shell must gate
     * NavHost composition on this, never on the transient `onboardingDone` value.
     */
    var isLoadingOnboardingState: Boolean by mutableStateOf(true)
        private set
}