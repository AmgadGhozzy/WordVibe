package com.venom.ui.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TtsState(
    val isSpeaking: Boolean = false,
    val currentText: String? = null
)

@HiltViewModel
class TTSViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = true)
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = false, currentText = null)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = false, currentText = null)
                }
            })
        }
    }

    fun toggle(text: String, languageTag: String?) {
        if (!isTtsReady) return
        if (_state.value.isSpeaking && _state.value.currentText == text) {
            tts?.stop()
            _state.value = _state.value.copy(isSpeaking = false, currentText = null)
        } else {
            speak(text, languageTag)
        }
    }

    fun speakSlow(text: String, languageTag: String?) {
        if (!isTtsReady) return
        languageTag?.let { tag ->
            val locale = Locale.forLanguageTag(tag)
            tts?.language = locale
        }
        tts?.setSpeechRate(0.6f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_slow")
        _state.value = _state.value.copy(isSpeaking = true, currentText = text)
    }

    private fun speak(text: String, languageTag: String?) {
        if (!isTtsReady) return
        languageTag?.let { tag ->
            val locale = Locale.forLanguageTag(tag)
            tts?.language = locale
        }
        tts?.setSpeechRate(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak")
        _state.value = _state.value.copy(isSpeaking = true, currentText = text)
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onCleared()
    }
}
