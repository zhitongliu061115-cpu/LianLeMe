package com.example.helloapp.data.health.model

data class HealthSummary(
    val supported: Boolean = false,
    val authorized: Boolean = false,
    val hasWatch: Boolean = false,

    // 基础概览
    val todaySteps: Int? = null,
    val message: String = "",

    // 心率详情 / 统计
    val latestHeartRate: Int? = null,
    val heartRateAvg: Float? = null,
    val heartRateMin: Int? = null,
    val heartRateMax: Int? = null,

    // 活动统计
    val activityCalories: Float? = null,
    val activityDistanceMeters: Float? = null,
    val activityMinutes: Int? = null,
    val workoutCount: Int? = null,

    // 压力统计
    val pressureAvg: Float? = null,
    val pressureMin: Int? = null,
    val pressureMax: Int? = null,

    // 血氧统计
    val bloodOxygenAvg: Float? = null,
    val bloodOxygenMin: Int? = null,
    val bloodOxygenMax: Int? = null,

    // 睡眠统计
    val sleepHours: Float? = null,
    val sleepDeepHours: Float? = null,
    val sleepLightHours: Float? = null,
    val sleepAwakeHours: Float? = null,

    // 血压详情（先留字段，后续接 SDK 时再填）
    val bloodPressureHigh: Int? = null,
    val bloodPressureLow: Int? = null
)
