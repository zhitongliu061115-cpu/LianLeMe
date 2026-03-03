package com.example.helloapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.helloapp.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AICoachViewModel : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage("你好！我是你的AI教练，\n今天想练什么？", isUser = false),
            ChatMessage("我想练胸肌，帮我安排一\n个计划。", isUser = true),
            ChatMessage("计划已经置入", isUser = false)
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        _messages.value = _messages.value + ChatMessage(text, isUser = true)
        _inputText.value = ""
    }
}

