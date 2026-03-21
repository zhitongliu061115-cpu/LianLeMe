package com.example.helloapp.data

import android.content.Context
import android.util.Log
import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.data.remote.AiConfig
import com.example.helloapp.model.Account
import com.example.helloapp.model.UserProfile
import com.example.helloapp.model.ai.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AccountRepository(private val context: Context) {
    private val gson = Gson()
    private val api = AiApiService(AiConfig.BASE_URL)
    private val currentFile get() = context.filesDir.resolve("current_account.json")

    private fun sanitizeForFileName(username: String): String {
        val sanitized = username.trim().map { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '_'
        }.joinToString("")
        return sanitized.ifBlank { "unknown" }
    }

    // 恢复密码加密，保证存入数据库的是哈希值
    private fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(username: String, passwordRaw: String, displayName: String, avatarPath: String): Result<Account> = withContext(Dispatchers.IO) {
        runCatching {
            val passwordHash = hash(passwordRaw) // 将密码加密后再发送
            val jsonBody = """
                {
                    "username": "$username",
                    "password_hash": "$passwordHash",
                    "display_name": "$displayName",
                    "avatar_path": "$avatarPath"
                }
            """.trimIndent()

            val rawResponse = api.postJson("/register", jsonBody)

            // 使用正规的 Gson 解析，不再受空格影响
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val response: ApiResponse<Any> = gson.fromJson(rawResponse, type)

            if (response.code != 200) {
                throw Exception(response.msg)
            }

            Account(username, passwordHash, displayName, avatarPath)
        }
    }

    suspend fun login(username: String, passwordRaw: String): Result<Account> = withContext(Dispatchers.IO) {
        runCatching {
            val passwordHash = hash(passwordRaw)
            val jsonBody = """{"username": "$username", "password_hash": "$passwordHash"}"""
            val rawResponse = api.postJson("/login", jsonBody)

            val type = object : TypeToken<ApiResponse<Map<String, Any>>>() {}.type
            val response: ApiResponse<Map<String, Any>> = gson.fromJson(rawResponse, type)

            if (response.code != 200) throw Exception(response.msg)

            val data = response.data ?: emptyMap()

            // 安全地将 Gson 解析的数字转化为字符串
            val ageVal = (data["age"] as? Number)?.toInt()?.takeIf { it > 0 }?.toString() ?: ""
            val heightVal = (data["height_cm"] as? Number)?.toFloat()?.takeIf { it > 0f }?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: ""
            val weightVal = (data["weight_kg"] as? Number)?.toFloat()?.takeIf { it > 0f }?.let { if (it % 1 == 0f) it.toInt().toString() else it.toString() } ?: ""

            val account = Account(
                username = username,
                passwordHash = passwordHash,
                displayName = data["display_name"]?.toString() ?: username,
                avatarPath = data["avatar_path"]?.toString() ?: "",
                signature = data["signature"]?.toString() ?: "",
                gender = data["gender"]?.toString() ?: "",
                age = ageVal,
                heightCm = heightVal,
                weightKg = weightVal,
                goal = data["goals"]?.toString() ?: ""
            )

            currentFile.writeText(gson.toJson(account))

            // 如果服务器返回了完整的用户档案信息，同步创建 UserProfile 文件
            if (account.gender.isNotBlank() && account.age.isNotBlank()) {
                val userRepo = UserRepository(context)
                val profile = UserProfile(
                    gender = account.gender,
                    age = account.age.toIntOrNull() ?: 0,
                    weightKg = account.weightKg.toFloatOrNull() ?: 0f,
                    heightCm = account.heightCm.toFloatOrNull() ?: 0f,
                    goals = if (account.goal.isNotBlank()) account.goal.split("、").filter { it.isNotBlank() } else emptyList(),
                    focusAreas = emptyList(),
                    workoutTypes = emptyList()
                )
                // 直接保存本地 UserProfile 文件，避免重复调用服务器
                val profileFile = context.filesDir.resolve("user_profile_${sanitizeForFileName(username)}.json")
                profileFile.writeText(gson.toJson(profile))
            }

            account
        }
    }

    // 2. 新增一个方法，专门用于把设置页的数据同步到云端
    suspend fun updateSettingsToServer(account: Account): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonBody = """
                {
                    "username": "${account.username}",
                    "display_name": "${account.displayName}",
                    "avatar_path": "${account.avatarPath.replace("\\", "\\\\")}",
                    "signature": "${account.signature}",
                    "gender": "${account.gender}",
                    "age": ${account.age.ifBlank { "0" }},
                    "height_cm": ${account.heightCm.ifBlank { "0" }},
                    "weight_kg": ${account.weightKg.ifBlank { "0" }},
                    "goals": "${account.goal}"
                }
            """.trimIndent()

            val rawResponse = api.postJson("/update_settings", jsonBody)
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val response: ApiResponse<Any> = gson.fromJson(rawResponse, type)

            if (response.code != 200) {
                throw Exception(response.msg)
            }
        }
    }

    fun logout() {
        if (currentFile.exists()) currentFile.delete()
    }

    fun currentAccount(): Account? = runCatching {
        if (currentFile.exists()) gson.fromJson(currentFile.readText(), Account::class.java) else null
    }.getOrNull()

    fun updateAccount(account: Account) {
        currentFile.writeText(gson.toJson(account))
    }
}