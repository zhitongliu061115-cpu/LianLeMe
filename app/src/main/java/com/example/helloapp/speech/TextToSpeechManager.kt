package com.example.helloapp.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(
    context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
    }

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            Log.d(TAG, "TTS 初始化完成，ready=$ready")
        } else {
            ready = false
            Log.e(TAG, "TTS 初始化失败，status=$status")
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!ready || text.isBlank()) return
        try {
            tts?.speak(
                text,
                if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "ai_reply_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak 异常：${e.message}", e)
        }
    }

    fun stop() {
        try {
            if (ready) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop 异常：${e.message}", e)
        }
    }

    fun release() {
        try {
            if (ready) {
                tts?.stop()
            }
        } catch (_: Exception) {
        }

        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }

        tts = null
        ready = false
    }
}
