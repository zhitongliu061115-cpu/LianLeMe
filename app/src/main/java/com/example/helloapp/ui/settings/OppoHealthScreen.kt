package com.example.helloapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.data.health.model.HealthSummary
import com.example.helloapp.viewmodel.OppoHealthViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OppoHealthScreen(
    onBack: () -> Unit,
    viewModel: OppoHealthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHealthData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("OPPO 健康数据") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.summary?.let { summary ->
                HealthOverviewCard(summary)
                HeartRateCard(summary)
                ActivityCard(summary)
                PressureCard(summary)
                BloodOxygenCard(summary)
                SleepCard(summary)
                BloodPressureCard(summary)

                if (!summary.supported) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("返回")
                    }
                } else if (!summary.authorized) {
                    Button(
                        onClick = { viewModel.connectHealth() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("连接 OPPO 健康")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重新同步")
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthOverviewCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("状态概览", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("设备支持：${if (summary.supported) "支持" else "不支持"}")
            Text("授权状态：${if (summary.authorized) "已授权" else "未授权"}")
            Text("手表状态：${if (summary.hasWatch) "已连接" else "未连接"}")
            Text("说明：${summary.message}")
        }
    }
}

@Composable
private fun HeartRateCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("心率详情", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("最近心率：${summary.latestHeartRate?.let { "$it bpm" } ?: "暂无数据"}")
            Text("平均心率：${summary.heartRateAvg?.let { "$it bpm" } ?: "暂无数据"}")
            Text("最低心率：${summary.heartRateMin?.let { "$it bpm" } ?: "暂无数据"}")
            Text("最高心率：${summary.heartRateMax?.let { "$it bpm" } ?: "暂无数据"}")
        }
    }
}

@Composable
private fun ActivityCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("活动统计", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("今日步数：${summary.todaySteps ?: "暂无数据"}")
            Text("消耗卡路里：${summary.activityCalories?.let { "$it kcal" } ?: "暂无数据"}")
            Text("活动距离：${summary.activityDistanceMeters?.let { "$it m" } ?: "暂无数据"}")
            Text("活动时长：${summary.activityMinutes?.let { "$it 分钟" } ?: "暂无数据"}")
            Text("运动次数：${summary.workoutCount ?: "暂无数据"}")
        }
    }
}

@Composable
private fun PressureCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("压力统计", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("平均压力：${summary.pressureAvg ?: "暂无数据"}")
            Text("最低压力：${summary.pressureMin ?: "暂无数据"}")
            Text("最高压力：${summary.pressureMax ?: "暂无数据"}")
        }
    }
}

@Composable
private fun BloodOxygenCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("血氧统计", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("平均血氧：${summary.bloodOxygenAvg?.let { "$it%" } ?: "暂无数据"}")
            Text("最低血氧：${summary.bloodOxygenMin?.let { "$it%" } ?: "暂无数据"}")
            Text("最高血氧：${summary.bloodOxygenMax?.let { "$it%" } ?: "暂无数据"}")
        }
    }
}

@Composable
private fun SleepCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("睡眠统计", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("总睡眠：${summary.sleepHours?.let { "$it 小时" } ?: "暂无数据"}")
            Text("深睡：${summary.sleepDeepHours?.let { "$it 小时" } ?: "暂无数据"}")
            Text("浅睡：${summary.sleepLightHours?.let { "$it 小时" } ?: "暂无数据"}")
            Text("清醒：${summary.sleepAwakeHours?.let { "$it 小时" } ?: "暂无数据"}")
        }
    }
}

@Composable
private fun BloodPressureCard(summary: HealthSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("血压详情", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("高压：${summary.bloodPressureHigh ?: "暂未接入"}")
            Text("低压：${summary.bloodPressureLow ?: "暂未接入"}")
        }
    }
}
