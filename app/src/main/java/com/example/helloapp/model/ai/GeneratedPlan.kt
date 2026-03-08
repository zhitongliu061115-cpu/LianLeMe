package com.example.helloapp.model.ai

data class PlanProfilePayload(
    val gender: String,
    val height_cm: Double,
    val weight_kg: Double,
    val age: Int,
    val goal: String,
    val strength_level: String
)

data class GeneratePlanRequest(
    val user_id: String,
    val profile: PlanProfilePayload
)

data class GeneratedPlan(
    val plan_days: Int = 14,
    val start_date: String = "",
    val days: List<GeneratedPlanDay> = emptyList(),
    val overall_advice: String = "",
    val timing: Timing? = null
)

data class GeneratedPlanDay(
    val day_index: Int = 0,
    val title: String = "",
    val items: List<GeneratedPlanItem> = emptyList(),
    val notes: String = ""
)

data class GeneratedPlanItem(
    val name: String = "",
    val sets: Int = 1,
    val reps: String = "",
    val rest_seconds: Int = 60,
    val alternatives: List<String> = emptyList()
)
