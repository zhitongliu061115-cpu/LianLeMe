package com.example.helloapp.ui.settings

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helloapp.data.AccountRepository
import com.example.helloapp.model.Account
import com.example.helloapp.ui.components.BottomNavigation
import kotlinx.coroutines.launch
import java.io.File

private val ACCENT = Color(0xFF6DD5C3)

@Composable
fun SettingsScreen(
    selectedNavItem: Int,
    onNavItemSelected: (Int) -> Unit,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit = {},
    refreshKey: Int = 0          // incremented by caller after auth returns
) {
    val context = LocalContext.current
    val repo = remember { AccountRepository(context) }
    var account by remember { mutableStateOf<Account?>(repo.currentAccount()) }
    var isEditing by remember { mutableStateOf(false) }

    // 修复点 1：将协程作用域和加载状态移入 Composable 函数内部
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var editGender by remember { mutableStateOf("保密") }
    var editAge by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editGoal by remember { mutableStateOf("") }
    var editSignature by remember { mutableStateOf("") }

    val ageValid = editAge.isBlank() || editAge.toIntOrNull()?.let { it in 1..120 } == true
    val heightValid = editHeight.isBlank() || editHeight.toIntOrNull()?.let { it in 80..260 } == true
    val weightValid = editWeight.isBlank() || editWeight.toIntOrNull()?.let { it in 20..300 } == true

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        editAvatarUri = uri
    }

    // Re-read whenever refreshKey changes (i.e. after returning from AuthScreen)
    LaunchedEffect(refreshKey) {
        account = repo.currentAccount()
        isEditing = false
        editName = account?.displayName.orEmpty()
        editAvatarUri = null
        editGender = account?.gender?.ifBlank { "保密" } ?: "保密"
        editAge = account?.age.orEmpty()
        editHeight = account?.heightCm.orEmpty()
        editWeight = account?.weightKg.orEmpty()
        editGoal = account?.goal.orEmpty()
        editSignature = account?.signature.orEmpty()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF7B9DB8), Color(0xFF9CB4C8)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 70.dp).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .border(3.dp, ACCENT, CircleShape)
                    .clickable(enabled = account != null && isEditing) { avatarPicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    editAvatarUri != null -> {
                        AsyncImage(
                            model = editAvatarUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    account?.avatarPath?.let { File(it).exists() } == true -> {
                        AsyncImage(
                            model = File(account!!.avatarPath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> Text("👤", fontSize = 40.sp)
                }
            }

            if (account != null && isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("点击头像可更换", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (account != null && isEditing) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it.take(16) },
                    singleLine = true,
                    label = { Text("昵称", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ACCENT,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("男", "女", "保密").forEach { g ->
                        FilterChip(
                            selected = editGender == g,
                            onClick = { editGender = g },
                            label = { Text(g) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ACCENT,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editAge,
                        onValueChange = { editAge = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("年龄") },
                        isError = !ageValid,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ACCENT,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("身高(cm)") },
                        isError = !heightValid,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ACCENT,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        label = { Text("体重(kg)") },
                        isError = !weightValid,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ACCENT,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editGoal,
                    onValueChange = { editGoal = it.take(40) },
                    singleLine = true,
                    label = { Text("训练目标（如：减脂、增肌）") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ACCENT,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editSignature,
                    onValueChange = { editSignature = it.take(60) },
                    maxLines = 2,
                    label = { Text("个性签名") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ACCENT,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("@${account!!.username}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            } else {
                Text(
                    text = account?.displayName ?: "未登录",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (account != null) {
                    Text("@${account!!.username}", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(14.dp))

                    val bmi = runCatching {
                        val h = account!!.heightCm.toFloat() / 100f
                        val w = account!!.weightKg.toFloat()
                        if (h > 0f) w / (h * h) else null
                    }.getOrNull()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileInfoRow("性别", account!!.gender.ifBlank { "未设置" }, "年龄", account!!.age.ifBlank { "未设置" })
                        ProfileInfoRow("身高", account!!.heightCm.ifBlank { "未设置" }.let { if (it == "未设置") it else "${it} cm" }, "体重", account!!.weightKg.ifBlank { "未设置" }.let { if (it == "未设置") it else "${it} kg" })
                        ProfileInfoRow("目标", account!!.goal.ifBlank { "未设置" }, "BMI", bmi?.let { String.format("%.1f", it) } ?: "--")
                        Text("签名：${account!!.signature.ifBlank { "这个人很懒，还没写签名" }}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (account == null) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                ) { Text("登录 / 注册", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            } else if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val old = account ?: return@OutlinedButton
                            isEditing = false
                            editName = old.displayName
                            editGender = old.gender.ifBlank { "保密" }
                            editAge = old.age
                            editHeight = old.heightCm
                            editWeight = old.weightKg
                            editGoal = old.goal
                            editSignature = old.signature
                            editAvatarUri = null
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("取消") }

                    // 修复点 2：将原来单纯的 repo.updateAccount 替换为包含网络请求的协程逻辑
                    Button(
                        onClick = {
                            val old = account ?: return@Button
                            if (!ageValid || !heightValid || !weightValid) {
                                Toast.makeText(context, "年龄/身高/体重格式不正确", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val savedAvatarPath = runCatching {
                                editAvatarUri?.let { uri ->
                                    val dest = File(context.filesDir, "avatar_${old.username}.jpg")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        dest.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    dest.absolutePath
                                }
                            }.getOrNull() ?: old.avatarPath

                            val updated = old.copy(
                                displayName = editName.ifBlank { old.username },
                                avatarPath = savedAvatarPath,
                                gender = editGender,
                                age = editAge,
                                heightCm = editHeight,
                                weightKg = editWeight,
                                goal = editGoal.trim(),
                                signature = editSignature.trim()
                            )

                            // 开启加载状态并向服务器提交
                            isSaving = true
                            scope.launch {
                                val result = repo.updateSettingsToServer(updated)
                                result.onSuccess {
                                    repo.updateAccount(updated) // 云端成功后，更新本地文件
                                    account = updated           // 刷新 UI
                                    isEditing = false
                                    editAvatarUri = null
                                    Toast.makeText(context, "资料已成功同步到云端", Toast.LENGTH_SHORT).show()
                                }.onFailure { e ->
                                    Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving, // 保存时按钮变灰不可点
                        colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
                    ) { Text(if (isSaving) "保存中..." else "保存") }
                }
            } else {
                Button(
                    onClick = {
                        val old = account ?: return@Button
                        isEditing = true
                        editName = old.displayName
                        editGender = old.gender.ifBlank { "保密" }
                        editAge = old.age
                        editHeight = old.heightCm
                        editWeight = old.weightKg
                        editGoal = old.goal
                        editSignature = old.signature
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) { Text("编辑资料", fontSize = 16.sp, color = Color.White) }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        repo.logout()
                        account = null
                        isEditing = false
                        editAvatarUri = null
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) { Text("退出登录", fontSize = 16.sp, color = Color.White) }
            }
        }

        BottomNavigation(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileInfoRow(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(leftLabel, fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
            Text(leftValue, fontSize = 14.sp, color = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(rightLabel, fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f))
            Text(rightValue, fontSize = 14.sp, color = Color.White)
        }
    }
}