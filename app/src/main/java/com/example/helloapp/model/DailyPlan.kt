package com.example.helloapp.model

data class DailyPlan(
    val day: String = "",              // "monday" … "sunday"
    val exercises: List<TrainingItem> = emptyList()
)

data class WeeklyPlan(
    val days: List<DailyPlan> = emptyList()
)

