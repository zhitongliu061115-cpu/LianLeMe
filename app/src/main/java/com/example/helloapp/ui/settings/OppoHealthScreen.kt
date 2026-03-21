package com.example.helloapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.data.health.model.HealthSummary
import com.example.helloapp.viewmodel.OppoHealthUiState
import com.example.helloapp.viewmodel.OppoHealthViewModel
import androidx.compose.material3.ExperimentalMaterial3Api


private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OppoHealthScreen(
    onBack: () -> Unit,
    viewModel: OppoHealthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(Unit) {
        viewModel.loadHealthData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OPPO 健康") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        OppoHealthContent(
            uiState = uiState,
            innerPadding = innerPadding,
            onAuthorizeClick = {
                if (activity != null) {
                    viewModel.requestAuthorization(activity)
                }
            },
            onConnectClick = {
                viewModel.connectHealth()
            },
            onRefreshClick = {
                viewModel.refresh()
            }
        )
    }
}

@Composable
private fun OppoHealthContent(
    uiState: OppoHealthUiState,
    innerPadding: PaddingValues,
    onAuthorizeClick: () -> Unit,
    onConnectClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val summary = uiState.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "OPPO 健康同步",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "你可以在这里连接 OPPO 健康、完成授权，并读取步数和心率数据。",
            style = MaterialTheme.typography.bodyMedium
        )

        if (uiState.loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "正在处理中...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "操作失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        ActionCard(
            summary = summary,
            onAuthorizeClick = onAuthorizeClick,
            onConnectClick = onConnectClick,
            onRefreshClick = onRefreshClick
        )

        if (summary != null) {
            StatusCard(summary = summary)
            StepsCard(summary = summary)
            HeartRateCard(summary = summary)
            ExtraTipsCard()
        }
    }
}

@Composable
private fun ActionCard(
    summary: HealthSummary?,
    onAuthorizeClick: () -> Unit,
    onConnectClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "操作",
                style = MaterialTheme.typography.titleMedium
            )

            Button(
                onClick = onAuthorizeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("授权连接 OPPO 健康")
            }

            OutlinedButton(
                onClick = onConnectClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("检查连接状态")
            }

            OutlinedButton(
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("刷新健康数据")
            }

            if (summary != null) {
                Text(
                    text = summary.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatusCard(summary: HealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "当前状态",
                style = MaterialTheme.typography.titleMedium
            )

            StatusRow(label = "设备支持", value = if (summary.supported) "是" else "否")
            StatusRow(label = "已授权", value = if (summary.authorized) "是" else "否")
            StatusRow(label = "检测到手表/健康设备", value = if (summary.hasWatch) "是" else "否")
            StatusRow(label = "提示信息", value = summary.message)
        }
    }
}

@Composable
private fun StepsCard(summary: HealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "步数数据",
                style = MaterialTheme.typography.titleMedium
            )

            MetricRow(
                label = "今日步数",
                value = summary.todaySteps?.toString() ?: "--"
            )

            MetricRow(
                label = "活动卡路里",
                value = summary.activityCalories?.toString() ?: "--"
            )

            MetricRow(
                label = "活动距离(米)",
                value = summary.activityDistanceMeters?.toString() ?: "--"
            )

            MetricRow(
                label = "活动时长(分钟)",
                value = summary.activityMinutes?.toString() ?: "--"
            )
        }
    }
}

@Composable
private fun HeartRateCard(summary: HealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "心率数据",
                style = MaterialTheme.typography.titleMedium
            )

            MetricRow(
                label = "最新心率",
                value = summary.latestHeartRate?.toString() ?: "--"
            )

            MetricRow(
                label = "平均心率",
                value = summary.heartRateAvg?.toString() ?: "--"
            )

            MetricRow(
                label = "最低心率",
                value = summary.heartRateMin?.toString() ?: "--"
            )

            MetricRow(
                label = "最高心率",
                value = summary.heartRateMax?.toString() ?: "--"
            )
        }
    }
}

@Composable
private fun ExtraTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "测试建议",
                style = MaterialTheme.typography.titleMedium
            )

            Text("1. 请确认 OPPO 健康已安装并已登录账号。")
            Text("2. 请确认 OPPO 健康里已经产生了步数或心率数据。")
            Text("3. 请先点击“授权连接 OPPO 健康”，再点击“刷新健康数据”。")
            Text("4. 如果仍然没有数据，先检查 OPPO 健康中的应用授权是否已开启。")
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
