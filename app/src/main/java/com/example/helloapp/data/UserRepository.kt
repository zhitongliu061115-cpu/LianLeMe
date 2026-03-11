package com.example.helloapp.data

import android.content.Context
import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.data.remote.AiConfig
import com.example.helloapp.model.UserProfile
import com.example.helloapp.model.ai.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val context: Context) {
    private val gson = Gson()
    private val api = AiApiService(AiConfig.BASE_URL)
    private val draftFile get() = context.filesDir.resolve("user_profile_draft.json")

    private fun sanitizeForFileName(username: String): String {
        val sanitized = username.trim().map { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '_'
        }.joinToString("")
        return sanitized.ifBlank { "unknown" }
    }

    private fun fileFor(username: String) =
        context.filesDir.resolve("user_profile_${sanitizeForFileName(username)}.json")

    suspend fun save(username: String, profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonBody = """
                {
                    "user_id": "$username",
                    "gender": "${profile.gender}",
                    "age": ${profile.age},
                    "height_cm": ${profile.heightCm},
                    "weight_kg": ${profile.weightKg},
                    "goals": "${profile.goals.joinToString(",")}",
                    "focus_areas": "${profile.focusAreas.joinToString(",")}",
                    "workout_types": "${profile.workoutTypes.joinToString(",")}"
                }
            """.trimIndent()

            val rawResponse = api.postJson("/save_profile", jsonBody)

            // 使用正规 Gson 解析
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val response: ApiResponse<Any> = gson.fromJson(rawResponse, type)

            if (response.code != 200) {
                throw Exception("同步云端档案失败: ${response.msg}")
            }

            fileFor(username).writeText(gson.toJson(profile))
        }
    }

    fun load(username: String): UserProfile? = runCatching {
        val file = fileFor(username)
        if (file.exists()) gson.fromJson(file.readText(), UserProfile::class.java) else null
    }.getOrNull()

    fun hasProfile(username: String): Boolean = fileFor(username).exists()

    fun saveDraft(profile: UserProfile) {
        draftFile.writeText(gson.toJson(profile))
    }

    fun loadDraft(): UserProfile? = runCatching {
        if (draftFile.exists()) gson.fromJson(draftFile.readText(), UserProfile::class.java) else null
    }.getOrNull()

    fun clearDraft() {
        if (draftFile.exists()) draftFile.delete()
    }
}