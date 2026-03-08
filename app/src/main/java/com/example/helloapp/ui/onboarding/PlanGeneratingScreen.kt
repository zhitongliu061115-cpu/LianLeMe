package com.example.helloapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlanGeneratingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF7B9DB8),
                        Color(0xFF9CB4C8)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "正在生成你的专属训练计划",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "已收到你的个人信息，AI 正在分析并安排训练内容，请稍候…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
