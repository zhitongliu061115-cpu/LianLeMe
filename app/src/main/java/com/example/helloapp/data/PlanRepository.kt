package com.example.helloapp.data

import android.content.Context
import com.example.helloapp.data.remote.AiApiService
import com.example.helloapp.data.remote.AiConfig
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.model.UserProfile
import com.example.helloapp.model.WeeklyPlan
import com.example.helloapp.model.ai.ApiResponse
import com.example.helloapp.model.ai.GeneratePlanRequest
import com.example.helloapp.model.ai.GeneratedPlan
import com.example.helloapp.model.ai.GeneratedPlanDay
import com.example.helloapp.model.ai.PlanProfilePayload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class PlanRepository(
    private val context: Context,
    private val api: AiApiService = AiApiService(AiConfig.BASE_URL),
    private val gson: Gson = Gson()
) {
    private val fallbackPlanFile get() = context.filesDir.resolve("training_plan.json")
    private val dayKeys = listOf(
        "monday", "tuesday", "wednesday",
        "thursday", "friday", "saturday", "sunday"
    )

    private fun ensureFallbackFile() {
        if (!fallbackPlanFile.exists()) {
            context.assets.open("training_plan.json").use { input ->
                fallbackPlanFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun loadWeeklyPlan(): WeeklyPlan {
        ensureFallbackFile()
        return runCatching {
            gson.fromJson(fallbackPlanFile.readText(), WeeklyPlan::class.java)
        }.getOrDefault(WeeklyPlan())
    }

    private fun sanitizeForFileName(username: String): String {
        val sanitized = username.trim().map { ch ->
            if (ch.isLetterOrDigit() || ch == '_' || ch == '-') ch else '_'
        }.joinToString("")
        return sanitized.ifBlank { "unknown" }
    }

    private fun userPlanFile(username: String) =
        context.filesDir.resolve("user_plan_${sanitizeForFileName(username)}.json")

    fun getFallbackPlanForDay(dayIndex: Int): List<TrainingItem> {
        val key = dayKeys.getOrNull(dayIndex) ?: return emptyList()
        return loadWeeklyPlan().days.find { it.day == key }?.exercises ?: emptyList()
    }

    fun saveUserGeneratedPlan(username: String, plan: GeneratedPlan) {
        userPlanFile(username).writeText(gson.toJson(plan))
    }

    fun loadUserGeneratedPlan(username: String): GeneratedPlan? = runCatching {
        val file = userPlanFile(username)
        if (file.exists()) gson.fromJson(file.readText(), GeneratedPlan::class.java) else null
    }.getOrNull()

    suspend fun generatePlan(username: String, profile: UserProfile): Result<GeneratedPlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 核心修改：直接将 UserProfile 里的列表转换为逗号分隔的字符串发送给后端
                val request = GeneratePlanRequest(
                    user_id = username,
                    profile = PlanProfilePayload(
                        gender = profile.gender.ifBlank { "unknown" },
                        height_cm = profile.heightCm.toDouble(),
                        weight_kg = profile.weightKg.toDouble(),
                        age = profile.age.takeIf { it > 0 } ?: 25,
                        goals = profile.goals.joinToString(","),
                        focus_areas = profile.focusAreas.joinToString(","),
                        workout_types = profile.workoutTypes.joinToString(",")
                    )
                )

                val raw = api.postJson("/generate_plan", gson.toJson(request))
                val type = object : TypeToken<ApiResponse<GeneratedPlan>>() {}.type
                val response: ApiResponse<GeneratedPlan> = gson.fromJson(raw, type)

                if (response.code != 200) {
                    throw IllegalStateException(response.msg)
                }
                response.data
            }
        }

    suspend fun generateAndSaveUserPlan(
        username: String,
        profile: UserProfile
    ): Result<GeneratedPlan> {
        return generatePlan(username, profile).mapCatching { plan ->
            saveUserGeneratedPlan(username, plan)
            plan
        }
    }

    fun getPlanForDate(username: String?, targetDate: Calendar): List<TrainingItem> {
        val userPlan = username?.let { loadUserGeneratedPlan(it) }
        val generatedDay = userPlan?.let { findGeneratedDay(it, targetDate) }

        if (generatedDay != null) {
            val mapped = generatedDay.toTrainingItems()
            if (mapped.isNotEmpty()) return mapped
        }

        return getFallbackPlanForDay(dayOfWeekIndex(targetDate))
    }

    private fun buildGoal(profile: UserProfile): String {
        val goals = profile.goals.takeIf { it.isNotEmpty() }?.joinToString("、") ?: "保持健康"
        val focus = profile.focusAreas.takeIf { it.isNotEmpty() }?.joinToString("、")
        val types = profile.workoutTypes.takeIf { it.isNotEmpty() }?.joinToString("、")

        return buildString {
            append(goals)
            if (!focus.isNullOrBlank()) append("；重点改善：$focus")
            if (!types.isNullOrBlank()) append("；偏好运动：$types")
        }
    }

    private fun findGeneratedDay(plan: GeneratedPlan, targetDate: Calendar): GeneratedPlanDay? {
        if (plan.start_date.isBlank()) return null

        val start = parseDate(plan.start_date) ?: return null
        val offset = daysBetween(start, targetDate) + 1
        if (offset <= 0) return null
        if (offset > plan.plan_days) return null

        return plan.days.firstOrNull { it.day_index == offset }
    }

    private fun parseDate(text: String): Calendar? {
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.isLenient = false
            val date = sdf.parse(text) ?: return null
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.getOrNull()
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val s = start.clone() as Calendar
        val e = end.clone() as Calendar
        normalizeDateOnly(s)
        normalizeDateOnly(e)
        val diff = e.timeInMillis - s.timeInMillis
        return (diff / (24 * 60 * 60 * 1000L)).toInt()
    }

    private fun normalizeDateOnly(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun dayOfWeekIndex(calendar: Calendar): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6
        }
    }

    private fun GeneratedPlanDay.toTrainingItems(): List<TrainingItem> {
        if (items.isEmpty()) {
            return listOf(
                TrainingItem(
                    title = if (title.isNotBlank()) title else "恢复日",
                    details = notes.ifBlank { "1组 | 20分钟" },
                    icon = "🧘"
                )
            )
        }

        return items.map { item ->
            TrainingItem(
                title = item.name,
                details = buildString {
                    append("${item.sets}组 | ${item.reps}")
                    if (item.rest_seconds > 0) {
                        append(" | 休息${item.rest_seconds}s")
                    }
                },
                icon = iconFor(item.name, title)
            )
        }
    }

    private fun iconFor(name: String, dayTitle: String): String {
        val text = "$name $dayTitle"
        return when {
            text.contains("跑") || text.contains("有氧") -> "🏃"
            text.contains("拉伸") || text.contains("瑜伽") || text.contains("恢复") -> "🧘"
            text.contains("核心") -> "🔥"
            text.contains("腿") || text.contains("下肢") -> "🦵"
            text.contains("上肢") || text.contains("肩") || text.contains("胸") -> "💪"
            else -> "🏋️"
        }
    }
}
