package com.example.helloapp.ui.training

import VoiceCoach
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
import androidx.camera.core.ImageAnalysis
import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions


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
    val detectedAction by viewModel.currentDetectedAction.collectAsState()
    var missingPersonFrames by remember { mutableIntStateOf(0) }
    val formFeedback by viewModel.formFeedback.collectAsState()
    var currentLandmarks by remember { mutableStateOf<List<NormalizedLandmark>?>(null) }

    val context = LocalContext.current

    val isPausedState = rememberUpdatedState(isPaused)

    //初始化你的 LSTM 动作分析器
    val poseActionAnalyzer = remember { PoseActionAnalyzer(context) }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            poseActionAnalyzer.clearBuffer()
        }
    }

    val voiceCoach = remember { VoiceCoach(context) }
    DisposableEffect(Unit) {
        onDispose { voiceCoach.shutdown() }
    }

    val poseLandmarker = remember {
        val baseOptions = BaseOptions.builder()
            // 确保你把 pose_landmarker_lite.task 放到了 assets 文件夹中
            .setModelAssetPath("pose_landmarker.task")
            .build()

        val options = PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            // MediaPipe 识别出结果后的回调
            .setResultListener { result, _ ->
                val landmarks = result.landmarks().getOrNull(0)
                currentLandmarks = landmarks
                if (landmarks != null) {
                    val actionIndex = poseActionAnalyzer.processLandmarks(landmarks)

                    android.util.Log.d("AI_Action_Test", "当前帧识别到的 actionIndex: $actionIndex")

                    // 处理异常状态 UI (看不见人等)
                    if (actionIndex == -1) {
                        missingPersonFrames++
                        if (missingPersonFrames > 30) {
                            viewModel.updateDetectedStatus("请确保大部分身体进入镜头...")
                        }
                    }else{
                        missingPersonFrames = 0
                    }

                    // 统一把坐标和 LSTM 的判定结果喂给 ViewModel 组合处理
                    viewModel.processFrameWithPose(actionIndex, landmarks)
                }else{
                    missingPersonFrames++
                    if (missingPersonFrames > 30) {
                        viewModel.updateDetectedStatus("未检测到人体...")
                    }
                    // 🚨 极其重要：告诉 ViewModel 画面里没人了！
                    // 传入 -1 让它清空滑动窗口投票池，防止幽灵计数
                    viewModel.processFrameWithPose(-1, emptyList())
                }
            }
            .build()

        PoseLandmarker.createFromOptions(context, options)
    }

    // 👇 3. 创建 ImageAnalyzer 桥接 CameraX 和 MediaPipe
    val imageAnalyzer = remember {
        ImageAnalysis.Analyzer { imageProxy ->
            if (isPausedState.value) {
                imageProxy.close()
                return@Analyzer
            }
            val bitmap = imageProxy.toBitmap()
            // 解决前置摄像头的旋转问题 (根据实际情况调整旋转角度)
            val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // 转换成 MediaPipe 需要的格式
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()

            // 喂给 MediaPipe (传入当前时间戳)
            val frameTime = System.currentTimeMillis()
            poseLandmarker.detectAsync(mpImage, frameTime)

            // 必须 close，否则会阻塞后面的帧
            imageProxy.close()
        }
    }

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

    DisposableEffect(Unit) {
        onDispose {
            poseActionAnalyzer.close()
            poseLandmarker.close()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF3D4C5C))
    ) {
        // ─── 背景：摄像头 ───
        if (hasCameraPermission && !isFinished) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageAnalyzer = imageAnalyzer
            )

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：AI 实时状态
                    Text(
                        text = "动作: $detectedAction",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700) // 金黄色，醒目一点
                    )

                    // 右侧：原有的次数/时间逻辑
                    Text(
                        text = if (isTimeBased) {
                            val m = setTimeRemaining / 60
                            val s = setTimeRemaining % 60
                            "剩余 ${String.format("%02d:%02d", m, s)}"
                        } else {
                            "$currentRep/$repsPerSet"
                        },
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

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

        // ─── 纠错信息悬浮提示 ───
        AnimatedVisibility(
            visible = formFeedback != null && !isFinished && !isPaused && !isTransitioning,
            enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp) // 根据你的顶部栏高度调整
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF4C4C).copy(alpha = 0.9f)) // 醒目的红色底
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = formFeedback ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
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
