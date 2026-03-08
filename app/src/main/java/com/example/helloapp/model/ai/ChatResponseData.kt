package com.example.helloapp.model.ai

data class ChatResponseData(
    val reply: String,
    val suggested_prompts: List<String> = emptyList(),
    val token_usage: Int = 0,
    val timing: Timing? = null
)
