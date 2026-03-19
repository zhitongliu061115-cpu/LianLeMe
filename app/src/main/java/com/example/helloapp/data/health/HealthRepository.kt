package com.example.helloapp.data.health

import com.example.helloapp.data.health.model.HealthSummary
import kotlinx.coroutines.delay

class  HealthRepository {

    suspend fun getHealthSummary(): HealthSummary {
        delay(500) // 模拟加载

        return HealthSummary(
            supported = true,
            authorized = false,
            hasWatch = false,
            todaySteps = null,
            latestHeartRate = null,
            sleepHours = null,
            message = "未连接 OPPO 健康数据"
        )
    }

    suspend fun connectHealth(): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }

    suspend fun refreshHealthData(): HealthSummary {
        delay(500)

        return HealthSummary(
            supported = true,
            authorized = true,
            hasWatch = false,
            todaySteps = 5320,
            latestHeartRate = null,
            sleepHours = null,
            message = "未检测到 OPPO 手表，部分数据不可用"
        )
    }
}
