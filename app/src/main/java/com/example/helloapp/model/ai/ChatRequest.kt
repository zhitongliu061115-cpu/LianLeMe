package com.example.helloapp.model.ai

data class ChatRequest(
    val user_id: String,
    val message: String,
    val history: List<ChatHistoryMessage>
)
