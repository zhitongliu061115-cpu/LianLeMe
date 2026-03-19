package com.example.helloapp.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helloapp.data.AccountRepository
import kotlinx.coroutines.launch
import java.io.File

private val ACCENT = Color(0xFF6DD5C3)

@Composable
fun LoginForm(repo: AccountRepository, onSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // 1. 新增：加载状态和协程作用域
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthTextField("用户名", username) { username = it }
    AuthTextField("密码", password, isPassword = true) { password = it }
    if (error.isNotEmpty()) Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp)

    // 2. 根据加载状态改变按钮文字和点击状态
    AuthButton(label = if (isLoading) "登录中..." else "登录", enabled = !isLoading) {
        when {
            username.isBlank() -> error = "用户名不能为空"
            password.isBlank() -> error = "密码不能为空"
            else -> {
                isLoading = true
                error = ""
                // 3. 在协程中发起网络请求
                scope.launch {
                    val result = repo.login(username, password)
                    result.onSuccess {
                        onSuccess()
                    }.onFailure { e ->
                        error = e.message ?: "用户名或密码错误"
                    }
                    isLoading = false
                }
            }
        }
    }
}

@Composable
fun RegisterForm(repo: AccountRepository, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf("") }

    // 1. 新增：加载状态和协程作用域
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }

    // Avatar picker (防误触：加载时不可点击)
    Box(
        modifier = Modifier.size(80.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .border(2.dp, ACCENT, CircleShape)
            .clickable(enabled = !isLoading) { picker.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUri != null) {
            AsyncImage(model = avatarUri, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text("📷", fontSize = 28.sp)
        }
    }
    Text("点击上传头像", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))

    AuthTextField("用户名", username) { username = it }
    AuthTextField("密码", password, isPassword = true) { password = it }
    AuthTextField("昵称", displayName) { displayName = it }
    if (error.isNotEmpty()) Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp)

    AuthButton(label = if (isLoading) "注册中..." else "注册", enabled = !isLoading) {
        when {
            username.isBlank() -> error = "用户名不能为空"
            password.length < 6 -> error = "密码至少6位"
            else -> {
                isLoading = true
                error = ""
                // 2. 在协程中处理注册
                scope.launch {
                    val savedAvatar = avatarUri?.let { uri ->
                        val dest = File(context.filesDir, "avatar_${username}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }
                        dest.absolutePath
                    } ?: ""

                    val regResult = repo.register(username, password, displayName.ifEmpty { username }, savedAvatar)

                    regResult.onSuccess {
                        // 注册成功后，自动调用登录接口
                        repo.login(username, password)
                        onSuccess()
                    }.onFailure { e ->
                        error = e.message ?: "用户名已存在或网络异常"
                    }
                    isLoading = false
                }
            }
        }
    }
}

@Composable
fun AuthTextField(label: String, value: String, isPassword: Boolean = false, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ACCENT, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            focusedTextColor = Color.White, unfocusedTextColor = Color.White
        )
    )
}

// 修改了 AuthButton，增加了 enabled 参数来控制变灰不可点击
@Composable
fun AuthButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ACCENT,
            disabledContainerColor = ACCENT.copy(alpha = 0.5f)
        )
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
}