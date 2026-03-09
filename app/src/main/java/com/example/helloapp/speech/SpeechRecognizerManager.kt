package com.example.helloapp.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpeechRecognizerManager(
    context: Context,
    private val onChunk: (String) -> Unit,
    private val onFinished: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "SpeechRecognizer"
        private const val FINAL_RESULT_TIMEOUT_MS = 6000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var asr: ASR? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isStopping = false
    private var finalResultReceived = false
    private var stopTimeoutJob: Job? = null

    private val sampleRateInHz = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes =
        AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (isRecording) {
            Log.d(TAG, "当前已经在录音中，忽略重复启动")
            return true
        }

        if (bufferSizeInBytes <= 0) {
            Log.e(TAG, "AudioRecord bufferSize 无效: $bufferSizeInBytes")
            onError("录音参数初始化失败")
            return false
        }

        return try {
            Log.d(TAG, "开始录音，准备创建新的 ASR 会话，bufferSizeInBytes=$bufferSizeInBytes")

            finalResultReceived = false
            isStopping = false
            isRecording = true
            stopTimeoutJob?.cancel()

            recreateAsrSession()

            if (asr == null) {
                isRecording = false
                onError("语音引擎初始化失败")
                return false
            }

            asr?.start(null)
            Log.d(TAG, "ASR 会话已启动")

            scope.launch(Dispatchers.IO) {
                try {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRateInHz,
                        channelConfig,
                        audioFormat,
                        bufferSizeInBytes
                    )

                    if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                        Log.e(TAG, "AudioRecord 初始化失败，state=${audioRecord?.state}")
                        isRecording = false
                        releaseRecorderOnly()
                        scope.launch {
                            onError("麦克风初始化失败")
                        }
                        return@launch
                    }

                    audioRecord?.startRecording()

                    if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        Log.e(TAG, "AudioRecord 未真正开始录音，recordingState=${audioRecord?.recordingState}")
                        isRecording = false
                        releaseRecorderOnly()
                        scope.launch {
                            onError("麦克风启动失败")
                        }
                        return@launch
                    }

                    Log.d(TAG, "AudioRecord 已开始录音")

                    val buffer = ByteArray(1280)
                    var successLogCount = 0

                    while (isRecording) {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readSize > 0) {
                            successLogCount++
                            if (successLogCount <= 3) {
                                Log.d(TAG, "已采集到音频数据，字节数=$readSize")
                            }
                            val data = buffer.copyOfRange(0, readSize)
                            asr?.write(data)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "录音异常：${e.message}", e)
                    scope.launch {
                        onError(e.message ?: "录音异常")
                    }
                } finally {
                    releaseRecorderOnly()
                    Log.d(TAG, "录音线程结束，准备调用 asr.stop(force=false)")
                    try {
                        asr?.stop(false)
                        Log.d(TAG, "asr.stop(false) 已调用")
                    } catch (e: Exception) {
                        Log.e(TAG, "调用 asr.stop(false) 异常：${e.message}", e)
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败：${e.message}", e)
            isRecording = false
            isStopping = false
            releaseRecorderOnly()
            onError(e.message ?: "启动录音失败")
            false
        }
    }

    fun stopListening(force: Boolean = false) {
        if (!isRecording && !isStopping) {
            Log.d(TAG, "当前没有进行中的录音，会忽略 stopListening")
            return
        }

        if (force) {
            Log.d(TAG, "强制停止录音")
            isRecording = false
            isStopping = false
            stopTimeoutJob?.cancel()
            try {
                asr?.stop(true)
            } catch (e: Exception) {
                Log.e(TAG, "强制停止 ASR 异常：${e.message}", e)
            }
            releaseRecorderOnly()
            return
        }

        Log.d(TAG, "用户结束录音，等待最终识别结果")
        isRecording = false
        isStopping = true

        stopTimeoutJob?.cancel()
        stopTimeoutJob = scope.launch {
            val startWait = SystemClock.elapsedRealtime()
            delay(FINAL_RESULT_TIMEOUT_MS)
            if (!finalResultReceived && isStopping) {
                val waited = SystemClock.elapsedRealtime() - startWait
                Log.e(TAG, "等待最终识别结果超时：${waited}ms")
                isStopping = false
                try {
                    asr?.stop(true)
                } catch (_: Exception) {
                }
                onError("语音识别超时，未收到最终结果")
            }
        }
    }

    private fun recreateAsrSession() {
        try {
            asr?.stop(true)
        } catch (_: Exception) {
        }

        asr = null

        try {
            asr = ASR("zh_cn", "iat", "mandarin")
            asr?.registerCallbacks(callbacks)
            Log.d(TAG, "ASR 会话对象创建成功并已注册回调")
        } catch (e: Exception) {
            Log.e(TAG, "ASR 会话对象创建失败：${e.message}", e)
            asr = null
        }
    }

    private fun releaseRecorderOnly() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }

        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }

        audioRecord = null
    }

    fun release() {
        Log.d(TAG, "释放语音识别资源")
        isRecording = false
        isStopping = false
        finalResultReceived = false
        stopTimeoutJob?.cancel()
        releaseRecorderOnly()

        try {
            asr?.stop(true)
        } catch (_: Exception) {
        }

        asr = null
        scope.cancel()
    }

    private val callbacks = object : AsrCallbacks {
        override fun onResult(result: ASR.ASRResult?, usrTag: Any?) {
            result?.let { asrResult ->
                val text = asrResult.bestMatchText ?: ""
                val status = asrResult.status

                Log.d(TAG, "收到 ASR 回调：status=$status，text=$text")

                scope.launch {
                    if (text.isNotEmpty()) {
                        onChunk(text)
                    }

                    if (status == 2) {
                        Log.d(TAG, "收到最终识别结果")
                        finalResultReceived = true
                        isRecording = false
                        isStopping = false
                        stopTimeoutJob?.cancel()
                        onFinished()
                    }
                }
            }
        }

        override fun onError(error: ASR.ASRError?, usrTag: Any?) {
            val errMsg = error?.errMsg ?: "未知错误"
            Log.e(TAG, "ASR 回调报错：msg=$errMsg")
            scope.launch {
                isRecording = false
                isStopping = false
                stopTimeoutJob?.cancel()
                onError(errMsg)
            }
        }
    }
}
