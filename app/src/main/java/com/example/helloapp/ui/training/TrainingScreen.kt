package com.example.helloapp.ui.training

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.viewmodel.TrainingViewModel

@Composable
fun TrainingScreen(
    exercises: List<TrainingItem>,
    startIndex: Int = 0,
    onBack: () -> Unit,
    viewModel: TrainingViewModel = viewModel()
) {
    // 初始化 ViewModel
    LaunchedEffect(exercises, startIndex) {
        viewModel.init(exercises, startIndex)
    }

    val isPaused by viewModel.isPaused.collectAsState()
    val currentRep by viewModel.currentRep.collectAsState()
    val repsPerSet by viewModel.repsPerSet.collectAsState()
    val currentSet by viewModel.currentSet.collectAsState()
    val totalSets by viewModel.totalSets.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val exerciseName by viewModel.currentExerciseName.collectAsState()
    val exerciseIcon by viewModel.currentExerciseIcon.collectAsState()
    val exerciseIndex by viewModel.currentExerciseIndex.collectAsState()
    val totalExercises by viewModel.totalExercises.collectAsState()
    val isTimeBased by viewModel.isTimeBased.collectAsState()
    val setTimeRemaining by viewModel.setTimeRemaining.collectAsState()
    val isTransitioning by viewModel.isTransitioning.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF3D4C5C))
    ) {
        // ─── 背景：摄像头 ───
        if (hasCameraPermission && !isFinished) {
            CameraPreview(modifier = Modifier.fillMaxSize())
        } else if (!isFinished) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF2d3748)),
                contentAlignment = Alignment.Center
            ) {
                Text("需要摄像头权限", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        // ─── 训练完成界面 ───
        if (isFinished) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2d3748)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("训练完成！", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    val minutes = elapsedTime / 60
                    val seconds = elapsedTime % 60
                    Text(
                        text = "总时长 ${String.format("%02d:%02d", minutes, seconds)}  |  完成 $totalExercises 个动作",
                        fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .width(180.dp).height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF5DD4C4))
                    ) {
                        Text("返回首页", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
            return
        }

        // ─── 顶部信息栏 ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp).padding(top = 40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                // 动作名 + 动作索引
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = exerciseName,
                        fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White
                    )
                    Text(
                        text = "动作 ${exerciseIndex + 1}/$totalExercises",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // 组信息
                Text(
                    text = "第 ${currentSet} 组 / 共 $totalSets 组",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF6DD5C3),
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 次数 或 剩余时间
                Text(
                    text = if (isTimeBased) {
                        val m = setTimeRemaining / 60
                        val s = setTimeRemaining % 60
                        "剩余 ${String.format("%02d:%02d", m, s)}"
                    } else {
                        "$currentRep/$repsPerSet"
                    },
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // ─── 动作切换过渡 ───
        AnimatedVisibility(
            visible = isTransitioning,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC2d3748)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "动作完成！",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "即将进入下一个动作…",
                        fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ─── 暂停遮罩 ───
        AnimatedVisibility(
            visible = isPaused && !isTransitioning,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC2d3748)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = exerciseIcon,
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = exerciseName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击开始",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ─── 底部控制栏 ───
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF2d3748).copy(alpha = 0.8f))
                    )
                )
                .padding(bottom = 40.dp, top = 60.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (isTimeBased) {
                    val m = setTimeRemaining / 60
                    val s = setTimeRemaining % 60
                    StatBox(label = "剩余", value = String.format("%02d:%02d", m, s), color = Color(0xFF4DD0C0))
                } else {
                    StatBox(label = "次数", value = "$currentRep/$repsPerSet", color = Color(0xFF4DD0C0))
                }
                StatBox(label = "组数", value = "$currentSet/$totalSets", color = Color(0xFF6B8DAD))
                val minutes = elapsedTime / 60
                val seconds = elapsedTime % 60
                StatBox(label = "时间", value = String.format("%02d:%02d", minutes, seconds), color = Color(0xFF5C6B7C))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE85D5D))
                ) {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                }
                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF5DD4C4))
                ) {
                    Text(text = if (isPaused) "▶" else "⏸", fontSize = 32.sp, color = Color.White)
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0xFF8A98A8))
                ) {
                    Text("✨", fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 12.sp, color = Color.White)
        }
    }
}
