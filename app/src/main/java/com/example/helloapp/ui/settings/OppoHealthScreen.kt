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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
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
                HealthStatusCard(summary = summary)

                if (!summary.authorized) {
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
private fun HealthStatusCard(summary: HealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "状态概览",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("设备支持：${if (summary.supported) "支持" else "不支持"}")
            Text("授权状态：${if (summary.authorized) "已授权" else "未授权"}")
            Text("手表状态：${if (summary.hasWatch) "已连接" else "未连接"}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("今日步数：${summary.todaySteps?.toString() ?: "暂无数据"}")
            Text("最近心率：${summary.latestHeartRate?.let { "$it bpm" } ?: "暂无数据"}")
            Text("睡眠时长：${summary.sleepHours?.let { "$it 小时" } ?: "暂无数据"}")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = summary.message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
