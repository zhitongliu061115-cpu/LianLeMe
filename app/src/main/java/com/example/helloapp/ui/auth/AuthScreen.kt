package com.example.helloapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloapp.data.AccountRepository

private val BG = Brush.verticalGradient(listOf(Color(0xFF3D5A80), Color(0xFF98C1D9)))
private val ACCENT = Color(0xFF6DD5C3)

@Composable
fun AuthScreen(onSuccess: () -> Unit, onBack: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val repo = remember { AccountRepository(context) }

    Box(modifier = Modifier.fillMaxSize().background(BG)) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (isLogin) "欢迎回来" else "创建账号", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // Tab switch
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)),
            ) {
                listOf("登录" to true, "注册" to false).forEach { (label, isL) ->
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (isLogin == isL) ACCENT else Color.Transparent)
                            .clickable { isLogin = isL }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = Color.White, fontWeight = FontWeight.Medium) }
                }
            }

            if (isLogin) LoginForm(repo, onSuccess) else RegisterForm(repo, onSuccess)

            TextButton(onClick = onBack) {
                Text("返回", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

