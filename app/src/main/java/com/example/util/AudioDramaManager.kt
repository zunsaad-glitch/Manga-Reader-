package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class AudioDramaManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isInitializing = false
    private var pendingSpeakRequest: Triple<String, Float, Float>? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSpokenText = MutableStateFlow<String?>(null)
    val currentSpokenText: StateFlow<String?> = _currentSpokenText

    private fun ensureTtsInitialized() {
        if (tts == null && !isInitializing) {
            isInitializing = true
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (_: Exception) {
                isInitializing = false
                isInitialized = false
            }
        }
    }

    override fun onInit(status: Int) {
        isInitializing = false
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.0f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                        _currentSpokenText.value = null
                    }

                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        _currentSpokenText.value = null
                    }
                })

                pendingSpeakRequest?.let { (text, rate, pitch) ->
                    pendingSpeakRequest = null
                    speak(text, rate, pitch)
                }
            } catch (_: Exception) {
                isInitialized = false
            }
        } else {
            isInitialized = false
            pendingSpeakRequest = null
        }
    }

    fun speak(text: String, speechRate: Float = 0.95f, pitch: Float = 1.0f) {
        if (!isInitialized) {
            pendingSpeakRequest = Triple(text, speechRate, pitch)
            ensureTtsInitialized()
            return
        }
        if (tts == null) return

        try {
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(pitch)
            _currentSpokenText.value = text
            _isPlaying.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AUDIO_DRAMA_UTTERANCE")
        } catch (_: Exception) {
            _isPlaying.value = false
            _currentSpokenText.value = null
        }
    }

    fun stop() {
        pendingSpeakRequest = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _currentSpokenText.value = null
    }

    fun release() {
        pendingSpeakRequest = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        isInitializing = false
        _isPlaying.value = false
        _currentSpokenText.value = null
    }
}
