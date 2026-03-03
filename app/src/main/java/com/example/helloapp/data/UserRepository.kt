package com.example.helloapp.data

import android.content.Context
import com.example.helloapp.model.UserProfile
import com.google.gson.Gson

class UserRepository(private val context: Context) {
    private val gson = Gson()
    private val draftFile get() = context.filesDir.resolve("user_profile_draft.json")

    private fun sanitizeForFileName(username: String): String {
        val sanitized = username.trim().map { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '_'
        }.joinToString("")
        return sanitized.ifBlank { "unknown" }
    }

    private fun fileFor(username: String) =
        context.filesDir.resolve("user_profile_${sanitizeForFileName(username)}.json")

    fun save(username: String, profile: UserProfile) {
        fileFor(username).writeText(gson.toJson(profile))
    }

    fun load(username: String): UserProfile? = runCatching {
        val file = fileFor(username)
        if (file.exists()) gson.fromJson(file.readText(), UserProfile::class.java) else null
    }.getOrNull()

    fun hasProfile(username: String): Boolean = fileFor(username).exists()

    // 未登录用户在引导里填的数据，先暂存，登录/注册成功后再归档到对应账号
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

