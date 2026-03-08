package com.example.helloapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.data.remote.AiConfig
import com.example.helloapp.data.repository.CoachRepository
import com.example.helloapp.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class AICoachViewModel : ViewModel() {

    private val repository = CoachRepository(
        api = AiApiService(AiConfig.BASE_URL)
    )

    private val userId = "user_001"

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

    fun onInputChanged(newText: String) {
        _inputText.value = newText
    }

    fun clearError() {
        _errorMessage.value = null
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
                userId = userId,
                message = text,
                currentMessages = currentList
            )

            result.onSuccess { data ->
                _messages.value = _messages.value + ChatMessage(
                    text = data.reply,
                    isUser = false
                )
                _suggestedPrompts.value = data.suggested_prompts
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
    init {
        pingServer()
    }

    private fun pingServer() {
        viewModelScope.launch {
            val result = repository.ping()
            result.onSuccess {
                android.util.Log.d("AI_CHAT", "服务器连接成功")
            }.onFailure { e ->
                android.util.Log.e("AI_CHAT", "服务器连接失败", e)
            }
        }
    }
}
