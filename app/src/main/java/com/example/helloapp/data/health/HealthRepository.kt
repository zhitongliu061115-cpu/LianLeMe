package com.example.helloapp.data.health

import android.os.Build
import com.example.helloapp.data.health.model.HealthSummary
import kotlinx.coroutines.delay

class HealthRepository {

    private fun isOppoDevice(): Boolean {
        val brand = Build.BRAND ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        return brand.equals("OPPO", ignoreCase = true) ||
                manufacturer.equals("OPPO", ignoreCase = true)
    }

    suspend fun getHealthSummary(): HealthSummary {
        delay(500)

        if (!isOppoDevice()) {
            return HealthSummary(
                supported = false,
                authorized = false,
                hasWatch = false,
                message = "当前设备不是 OPPO，未启用 OPPO 健康能力"
            )
        }

        // 这里后续替换成真实 SDK 初始化 / 授权查询 / 设备连接状态查询
        return HealthSummary(
            supported = true,
            authorized = false,
            hasWatch = false,
            todaySteps = null,
            latestHeartRate = null,
            heartRateAvg = null,
            heartRateMin = null,
            heartRateMax = null,
            activityCalories = null,
            activityDistanceMeters = null,
            activityMinutes = null,
            workoutCount = null,
            pressureAvg = null,
            pressureMin = null,
            pressureMax = null,
            bloodOxygenAvg = null,
            bloodOxygenMin = null,
            bloodOxygenMax = null,
            sleepHours = null,
            sleepDeepHours = null,
            sleepLightHours = null,
            sleepAwakeHours = null,
            bloodPressureHigh = null,
            bloodPressureLow = null,
            message = "请先连接并授权 OPPO 健康"
        )
    }

    suspend fun connectHealth(): Result<Unit> {
        delay(500)

        if (!isOppoDevice()) {
            return Result.failure(IllegalStateException("当前设备不是 OPPO"))
        }

        // 这里后续替换成真实 SDK 授权逻辑
        return Result.success(Unit)
    }

    suspend fun refreshHealthData(): HealthSummary {
        delay(500)

        if (!isOppoDevice()) {
            return HealthSummary(
                supported = false,
                authorized = false,
                hasWatch = false,
                message = "当前设备不是 OPPO，无法同步 OPPO 健康数据"
            )
        }

        // 这里先用演示数据占位
        // 后续改成真实 SDK 查询结果
        return HealthSummary(
            supported = true,
            authorized = true,
            hasWatch = true,

            todaySteps = 5320,

            latestHeartRate = 78,
            heartRateAvg = 76.5f,
            heartRateMin = 62,
            heartRateMax = 118,

            activityCalories = 356.5f,
            activityDistanceMeters = 4210f,
            activityMinutes = 48,
            workoutCount = 1,

            pressureAvg = 42.0f,
            pressureMin = 28,
            pressureMax = 63,

            bloodOxygenAvg = 97.0f,
            bloodOxygenMin = 95,
            bloodOxygenMax = 99,

            sleepHours = 7.2f,
            sleepDeepHours = 2.1f,
            sleepLightHours = 4.4f,
            sleepAwakeHours = 0.7f,

            bloodPressureHigh = null,
            bloodPressureLow = null,

            message = "OPPO 健康数据已同步（当前为演示数据，后续替换为 SDK 实时数据）"
        )
    }
}
