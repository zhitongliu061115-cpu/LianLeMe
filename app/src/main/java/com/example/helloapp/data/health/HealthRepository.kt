package com.example.helloapp.data.health

import android.app.Activity
import android.os.Build
import com.example.helloapp.data.health.model.HealthSummary
import com.heytap.databaseengine.HeytapHealthApi
import com.heytap.databaseengine.apiv2.HResponse
import com.heytap.databaseengine.apiv2.auth.AuthResult
import com.heytap.databaseengine.apiv3.DataReadRequest
import com.heytap.databaseengine.apiv3.data.DataSet
import com.heytap.databaseengine.apiv3.data.DataType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import kotlin.coroutines.resume

class HealthRepository {

    suspend fun requestAuthorization(activity: Activity): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            try {
                HeytapHealthApi.getInstance()
                    .authorityApi()
                    .request(activity, object : HResponse<AuthResult> {
                        override fun onSuccess(result: AuthResult) {
                            cont.resume(Result.success(Unit))
                        }

                        override fun onFailure(code: Int) {
                            cont.resume(
                                Result.failure(
                                    IllegalStateException("OPPO 健康授权失败，code=$code")
                                )
                            )
                        }
                    })
            } catch (e: Exception) {
                cont.resume(Result.failure(e))
            }
        }

    suspend fun getHealthSummary(): HealthSummary {
        if (!isOppoDevice()) {
            return HealthSummary(
                supported = false,
                authorized = false,
                hasWatch = false,
                message = "当前设备不是 OPPO，未启用 OPPO 健康能力"
            )
        }

        val authorized = checkAuthorized()
        if (!authorized) {
            return HealthSummary(
                supported = true,
                authorized = false,
                hasWatch = false,
                message = "请先在 OPPO 健康中完成授权"
            )
        }

        return refreshHealthData()
    }

    suspend fun connectHealth(): Result<Unit> {
        if (!isOppoDevice()) {
            return Result.failure(IllegalStateException("当前设备不是 OPPO / 一加"))
        }

        return try {
            val authorized = checkAuthorized()
            if (authorized) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("当前未授权，请先发起授权"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshHealthData(): HealthSummary {
        if (!isOppoDevice()) {
            return HealthSummary(
                supported = false,
                authorized = false,
                hasWatch = false,
                message = "当前设备不是 OPPO，无法同步 OPPO 健康数据"
            )
        }

        val authorized = checkAuthorized()
        if (!authorized) {
            return HealthSummary(
                supported = true,
                authorized = false,
                hasWatch = false,
                message = "请先在 OPPO 健康中完成授权"
            )
        }

        val todaySteps = readTodaySteps()
        val heartRateStats = readHeartRateStats()

        return HealthSummary(
            supported = true,
            authorized = true,
            hasWatch = true,
            todaySteps = todaySteps,
            latestHeartRate = heartRateStats.latest,
            heartRateAvg = heartRateStats.avg,
            heartRateMin = heartRateStats.min,
            heartRateMax = heartRateStats.max,
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
            message = buildMessage(todaySteps, heartRateStats)
        )
    }

    private fun isOppoDevice(): Boolean {
        val brand = Build.BRAND ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        return brand.equals("OPPO", ignoreCase = true) ||
                manufacturer.equals("OPPO", ignoreCase = true) ||
                brand.equals("OnePlus", ignoreCase = true) ||
                manufacturer.equals("OnePlus", ignoreCase = true)
    }

    private fun buildMessage(
        todaySteps: Int?,
        heartRateStats: HeartRateStats
    ): String {
        return when {
            todaySteps == null && heartRateStats.latest == null ->
                "已完成授权，但暂时没有读取到今日步数和心率数据"
            todaySteps != null && heartRateStats.latest == null ->
                "已同步步数数据，暂未读取到心率数据"
            todaySteps == null && heartRateStats.latest != null ->
                "已同步心率数据，暂未读取到步数数据"
            else ->
                "OPPO 健康数据同步成功"
        }
    }

    private suspend fun checkAuthorized(): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                HeytapHealthApi.getInstance()
                    .authorityApi()
                    .valid(object : HResponse<List<String>> {
                        override fun onSuccess(result: List<String>) {
                            cont.resume(result.isNotEmpty())
                        }

                        override fun onFailure(code: Int) {
                            cont.resume(false)
                        }
                    })
            } catch (e: Exception) {
                cont.resume(false)
            }
        }

    private suspend fun readTodaySteps(): Int? =
        suspendCancellableCoroutine { cont ->
            try {
                val (start, end) = todayRange()

                val request = DataReadRequest.Builder()
                    .read(DataType.TYPE_DAILY_ACTIVITY)
                    .setTimeRange(start, end)
                    .build()

                HeytapHealthApi.getInstance()
                    .dataApi()
                    .read(request, object : HResponse<List<DataSet>> {
                        override fun onSuccess(result: List<DataSet>) {
                            val values = mutableListOf<Int>()
                            result.forEach { dataSet ->
                                values += extractIntCandidatesFromDataSet(dataSet)
                            }
                            cont.resume(values.filter { it in 0..100000 }.maxOrNull())
                        }

                        override fun onFailure(code: Int) {
                            cont.resume(null)
                        }
                    })
            } catch (e: Exception) {
                cont.resume(null)
            }
        }

    private suspend fun readHeartRateStats(): HeartRateStats =
        suspendCancellableCoroutine { cont ->
            try {
                val (start, end) = todayRange()

                val request = DataReadRequest.Builder()
                    .read(DataType.TYPE_HEART_RATE)
                    .setTimeRange(start, end)
                    .build()

                HeytapHealthApi.getInstance()
                    .dataApi()
                    .read(request, object : HResponse<List<DataSet>> {
                        override fun onSuccess(result: List<DataSet>) {
                            val values = mutableListOf<Int>()
                            result.forEach { dataSet ->
                                values += extractIntCandidatesFromDataSet(dataSet)
                            }

                            val heartRates = values.filter { it in 30..240 }
                            if (heartRates.isEmpty()) {
                                cont.resume(HeartRateStats())
                            } else {
                                cont.resume(
                                    HeartRateStats(
                                        latest = heartRates.last(),
                                        avg = heartRates.average().toFloat(),
                                        min = heartRates.minOrNull(),
                                        max = heartRates.maxOrNull()
                                    )
                                )
                            }
                        }

                        override fun onFailure(code: Int) {
                            cont.resume(HeartRateStats())
                        }
                    })
            } catch (e: Exception) {
                cont.resume(HeartRateStats())
            }
        }

    private fun todayRange(): Pair<Long, Long> {
        val end = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = end
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis to end
    }

    private fun extractIntCandidatesFromDataSet(dataSet: DataSet): List<Int> {
        val result = mutableListOf<Int>()

        result += collectNumericValuesDeep(dataSet)

        val pointContainers = listOf(
            invokeNoArg(dataSet, "getDataPoints"),
            invokeNoArg(dataSet, "getDataPointList"),
            invokeNoArg(dataSet, "getPointList"),
            invokeNoArg(dataSet, "getPoints"),
            readFieldSafely(dataSet, "dataPoints"),
            readFieldSafely(dataSet, "pointList"),
            readFieldSafely(dataSet, "points")
        )

        pointContainers.forEach { container ->
            when (container) {
                is Iterable<*> -> container.forEach { item ->
                    if (item != null) result += collectNumericValuesDeep(item)
                }
                is Array<*> -> container.forEach { item ->
                    if (item != null) result += collectNumericValuesDeep(item)
                }
            }
        }

        return result.distinct()
    }

    private fun collectNumericValuesDeep(target: Any?): List<Int> {
        if (target == null) return emptyList()

        val values = mutableListOf<Int>()

        try {
            target.javaClass.methods
                .filter { it.parameterTypes.isEmpty() && it.name != "getClass" }
                .forEach { method ->
                    try {
                        when (val value = method.invoke(target) ?: return@forEach) {
                            is Int -> values.add(value)
                            is Long -> if (value in Int.MIN_VALUE..Int.MAX_VALUE) values.add(value.toInt())
                            is Float -> values.add(value.toInt())
                            is Double -> values.add(value.toInt())
                        }
                    } catch (_: Exception) {
                    }
                }
        } catch (_: Exception) {
        }

        return values
    }

    private fun invokeNoArg(target: Any?, methodName: String): Any? {
        if (target == null) return null
        return try {
            val method = target.javaClass.methods.firstOrNull {
                it.name == methodName && it.parameterTypes.isEmpty()
            } ?: return null
            method.invoke(target)
        } catch (_: Exception) {
            null
        }
    }

    private fun readFieldSafely(target: Any?, fieldName: String): Any? {
        if (target == null) return null
        return try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(target)
        } catch (_: Exception) {
            null
        }
    }

    private data class HeartRateStats(
        val latest: Int? = null,
        val avg: Float? = null,
        val min: Int? = null,
        val max: Int? = null
    )
}
