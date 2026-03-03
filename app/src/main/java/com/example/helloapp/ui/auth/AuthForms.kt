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
import java.io.File

private val ACCENT = Color(0xFF6DD5C3)

@Composable
fun LoginForm(repo: AccountRepository, onSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AuthTextField("用户名", username) { username = it }
    AuthTextField("密码", password, isPassword = true) { password = it }
    if (error.isNotEmpty()) Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp)

    AuthButton("登录") {
        when {
            username.isBlank() -> error = "用户名不能为空"
            password.isBlank() -> error = "密码不能为空"
            repo.login(username, password) != null -> onSuccess()
            else -> error = "用户名或密码错误"
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

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }

    // Avatar picker
    Box(
        modifier = Modifier.size(80.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .border(2.dp, ACCENT, CircleShape)
            .clickable { picker.launch("image/*") },
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

    AuthButton("注册") {
        when {
            username.isBlank() -> error = "用户名不能为空"
            password.length < 6 -> error = "密码至少6位"
            else -> {
                val savedAvatar = avatarUri?.let { uri ->
                    val dest = File(context.filesDir, "avatar_${username}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }
                    dest.absolutePath
                } ?: ""
                if (repo.register(username, password, displayName.ifEmpty { username }, savedAvatar)) {
                    repo.login(username, password)
                    onSuccess()
                } else error = "用户名已存在"
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

@Composable
fun AuthButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
    ) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
}

