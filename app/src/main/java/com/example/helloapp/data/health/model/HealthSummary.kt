package com.example.helloapp.data.health.model

data class HealthSummary(
    val supported: Boolean = false,
    val authorized: Boolean = false,
    val hasWatch: Boolean = false,
    val todaySteps: Int? = null,
    val latestHeartRate: Int? = null,
    val sleepHours: Float? = null,
    val message: String = ""
)
