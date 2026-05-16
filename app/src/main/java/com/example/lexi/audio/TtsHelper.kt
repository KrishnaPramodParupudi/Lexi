package com.example.lexi.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    var onStatus: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (!isReady) {
                Log.e("TtsHelper", "TTS init failed: $status")
                onStatus?.invoke("TTS engine unavailable on this device")
            }
        }
    }

    fun speak(text: String) {
        if (!isReady || tts == null) {
            onStatus?.invoke("TTS not ready yet, try again in a moment")
            return
        }
        if (text.isBlank()) return

        val result = tts!!.setLanguage(Locale.ENGLISH)

        when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                onStatus?.invoke("English language data is not supported or missing on this device")
                return
            }
        }

        tts!!.setSpeechRate(0.85f) // slightly slower — easier for dyslexic kids to follow
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ttsUtterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}