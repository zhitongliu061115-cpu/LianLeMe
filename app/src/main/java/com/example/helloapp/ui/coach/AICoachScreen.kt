package com.example.helloapp.ui.coach

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.speech.SpeechRecognizerManager
import com.example.helloapp.speech.TextToSpeechManager
import com.example.helloapp.ui.components.BottomNavigation
import com.example.helloapp.viewmodel.AICoachViewModel

@Composable
fun AICoachScreen(
    selectedNavItem: Int,
    onNavItemSelected: (Int) -> Unit,
    viewModel: AICoachViewModel = viewModel()
) {
    val context = LocalContext.current

    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val suggestedPrompts by viewModel.suggestedPrompts.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val speechError by viewModel.speechError.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()

    val messageScrollState = rememberScrollState()
    val promptScrollState = rememberScrollState()

    val ttsManager = remember(context) {
        TextToSpeechManager(context)
    }

    val speechRecognizerManager = remember(context) {
        SpeechRecognizerManager(
            context = context,
            onChunk = { text ->
                viewModel.appendVoiceChunk(text)
            },
            onFinished = {
                viewModel.finishVoiceInputAndSend()
            },
            onError = { msg ->
                viewModel.onSpeechError(msg)
                viewModel.stopRecordingUi()
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ttsManager.stop()
            viewModel.clearSpeechError()
            viewModel.startVoiceInput()

            val started = speechRecognizerManager.startListening()
            if (!started) {
                viewModel.stopRecordingUi()
            }
        } else {
            viewModel.onSpeechError("未授予录音权限")
        }
    }

    LaunchedEffect(messages.size, isSending, suggestedPrompts.size, speechError) {
        messageScrollState.animateScrollTo(messageScrollState.maxValue)
    }

    LaunchedEffect(Unit) {
        viewModel.speakText.collect { text ->
            if (ttsEnabled) {
                ttsManager.speak(text)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizerManager.release()
            ttsManager.release()
        }
    }

    fun onMicClick() {
        if (isSending) return

        if (isRecording) {
            speechRecognizerManager.stopListening(false)
            viewModel.stopRecordingUi()
            return
        }

        viewModel.clearSpeechError()

        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            ttsManager.stop()
            viewModel.startVoiceInput()

            val started = speechRecognizerManager.startListening()
            if (!started) {
                viewModel.stopRecordingUi()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8FA8BE))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    color = Color(0xFF2D3748),
                    modifier = Modifier.clickable { onNavItemSelected(0) }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "AI教练",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFD6E2EB))
                        .clickable {
                            val next = !ttsEnabled
                            viewModel.setTtsEnabled(next)
                            if (!next) {
                                ttsManager.stop()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (ttsEnabled) "🔊" else "🔇",
                        color = Color(0xFF2D3748),
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(messageScrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                messages.forEach { ChatMessageItem(it) }

                if (isSending) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFD6E2EB))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "AI 正在思考中...",
                                color = Color(0xFF2D3748),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (isRecording) {
                    Text(
                        text = "正在录音中... 再点一次麦克风可停止",
                        color = Color(0xFFB00020),
                        fontSize = 13.sp
                    )
                }

                errorMessage?.let { msg ->
                    Text(
                        text = "错误：$msg",
                        color = Color(0xFFB00020),
                        fontSize = 13.sp
                    )
                }

                speechError?.let { msg ->
                    Text(
                        text = "语音：$msg",
                        color = Color(0xFFB00020),
                        fontSize = 13.sp
                    )
                }

                if (suggestedPrompts.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "推荐提问",
                            color = Color(0xFF2D3748),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(promptScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestedPrompts.forEach { prompt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFD6E2EB))
                                        .clickable(enabled = !isSending && !isRecording) {
                                            viewModel.sendSuggestedPrompt(prompt)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = prompt,
                                        color = Color(0xFF2D3748),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isRecording -> Color(0xFFE88D8D)
                                isSending -> Color(0xFF9AAAB6)
                                else -> Color(0xFFB8C9D6)
                            }
                        )
                        .clickable(enabled = !isSending) {
                            onMicClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRecording) "■" else "🎤",
                        fontSize = 22.sp
                    )
                }

                TextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = when {
                                isRecording -> "正在听你说话..."
                                isSending -> "AI 回复中..."
                                else -> "输入消息..."
                            },
                            color = Color(0xFF6B7F92)
                        )
                    },
                    enabled = !isSending,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFB8C9D6),
                        unfocusedContainerColor = Color(0xFFB8C9D6),
                        disabledContainerColor = Color(0xFFAFC0CD),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSending) Color(0xFF9AAAB6) else Color(0xFFB8C9D6))
                        .clickable(enabled = !isSending && !isRecording) {
                            viewModel.sendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSending) "…" else "✈️",
                        fontSize = 24.sp
                    )
                }
            }
        }

        BottomNavigation(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
