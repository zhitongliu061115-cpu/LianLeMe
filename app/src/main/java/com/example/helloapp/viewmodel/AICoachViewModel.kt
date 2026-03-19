package com.example.helloapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.data.remote.AiConfig
import com.example.helloapp.data.repository.CoachRepository
import com.example.helloapp.model.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider // 新增

class AICoachViewModel(private val username: String) : ViewModel() {

    private val repository = CoachRepository(
        api = AiApiService(AiConfig.BASE_URL)
    )



    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage("你好，我是你的 AI 健身教练。今天想练什么？", false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _suggestedPrompts = MutableStateFlow<List<String>>(emptyList())
    val suggestedPrompts: StateFlow<List<String>> = _suggestedPrompts.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private val _speakText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val speakText = _speakText.asSharedFlow()

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    init {
        pingServer()
    }

    fun onInputChanged(newText: String) {
        _inputText.value = newText
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _isSending.value) return

        val currentList = _messages.value.filterNot {
            !it.isUser && it.text == "你好，我是你的 AI 健身教练。今天想练什么？"
        }
        val newUserMessage = ChatMessage(text = text, isUser = true)

        _messages.value = currentList + newUserMessage
        _inputText.value = ""
        _isSending.value = true
        _errorMessage.value = null
        _suggestedPrompts.value = emptyList()

        viewModelScope.launch {
            val result = repository.sendMessage(
                userId = username,
                message = text,
                currentMessages = currentList
            )

            result.onSuccess { data ->
                _messages.value = _messages.value + ChatMessage(
                    text = data.reply,
                    isUser = false
                )
                _suggestedPrompts.value = data.suggested_prompts

                if (_ttsEnabled.value) {
                    _speakText.tryEmit(data.reply)
                }
            }.onFailure { e ->
                Log.e("AI_CHAT", "聊天失败", e)
                _errorMessage.value = e.message ?: "请求失败"
            }

            _isSending.value = false
        }
    }

    fun sendSuggestedPrompt(prompt: String) {
        _inputText.value = prompt
        sendMessage()
    }

    fun startRecordingUi() {
        if (_isSending.value) return
        _speechError.value = null
        _isRecording.value = true
    }

    fun stopRecordingUi() {
        _isRecording.value = false
    }

    fun startVoiceInput() {
        _speechError.value = null
        _inputText.value = ""
        _isRecording.value = true
    }

    fun appendVoiceChunk(text: String) {
        if (text.isBlank()) return
        _inputText.value = text
    }

    fun finishVoiceInputAndSend() {
        _isRecording.value = false
        if (_inputText.value.trim().isNotBlank()) {
            sendMessage()
        }
    }

    fun onSpeechError(message: String) {
        _isRecording.value = false
        _speechError.value = message
    }

    fun clearSpeechError() {
        _speechError.value = null
    }

    private fun pingServer() {
        viewModelScope.launch {
            val result = repository.ping()
            result.onSuccess {
                Log.d("AI_CHAT", "服务器连接成功")
            }.onFailure { e ->
                Log.e("AI_CHAT", "服务器连接失败", e)
            }
        }
    }

    // 4. 新增 Factory 用于注入参数
    class Factory(private val username: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AICoachViewModel(username) as T
        }
    }
}
