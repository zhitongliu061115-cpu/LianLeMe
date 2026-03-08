package com.example.helloapp.data.repository

import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.model.ChatMessage
import com.example.helloapp.model.ai.ApiResponse
import com.example.helloapp.model.ai.ChatHistoryMessage
import com.example.helloapp.model.ai.ChatRequest
import com.example.helloapp.model.ai.ChatResponseData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoachRepository(
    private val api: AiApiService,
    private val gson: Gson = Gson()
) {
    suspend fun ping(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.get("/ping")
            Unit
        }
    }

    suspend fun sendMessage(
        userId: String,
        message: String,
        currentMessages: List<ChatMessage>
    ): Result<ChatResponseData> = withContext(Dispatchers.IO) {
        runCatching {
            val history = currentMessages.map {
                ChatHistoryMessage(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }

            val request = ChatRequest(
                user_id = userId,
                message = message,
                history = history
            )

            val json = gson.toJson(request)
            val raw = api.postJson("/chat", json)

            val type = object : TypeToken<ApiResponse<ChatResponseData>>() {}.type
            val response: ApiResponse<ChatResponseData> = gson.fromJson(raw, type)

            if (response.code != 200) {
                throw IllegalStateException(response.msg)
            }

            response.data
        }
    }
}
