import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceCoach(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var lastSpeakTime = 0L

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 设置为中文语音
            val result = tts.setLanguage(Locale.CHINESE)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    /**
     * 播报纠错语音，自带防频繁打扰的冷却机制
     * @param text 播报内容
     * @param cooldownMs 冷却时间，默认 3000 毫秒（3秒内不会重复同一句话）
     */
    fun speakCorrection(text: String, cooldownMs: Long = 3000L) {
        if (!isReady) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSpeakTime > cooldownMs) {
            // QUEUE_FLUSH 表示打断当前正在说的话，立刻说这句重要的纠错
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            lastSpeakTime = currentTime
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}