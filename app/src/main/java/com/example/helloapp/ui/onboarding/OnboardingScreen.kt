package com.example.helloapp.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.helloapp.model.UserProfile

private val BG = Brush.verticalGradient(listOf(Color(0xFF3D5A80), Color(0xFF98C1D9)))
private val ACCENT = Color(0xFF6DD5C3)

@Composable
fun OnboardingScreen(
    onFinish: (UserProfile) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var gender by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    val goals = remember { mutableStateListOf<String>() }
    val focusAreas = remember { mutableStateListOf<String>() }
    val workoutTypes = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier.fillMaxSize().background(BG),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = step, label = "onboarding") { s ->
            when (s) {
                0 -> StepBodyData(gender, age, weight, height,
                    onGender = { gender = it }, onAge = { age = it },
                    onWeight = { weight = it }, onHeight = { height = it },
                    onNext = { step = 1 })
                1 -> StepChips("你的健身目标", listOf("减脂","增肌","提升耐力","塑形","保持健康"), goals,
                    onNext = { step = 2 })
                2 -> StepChips("重点改善部位", listOf("腹部","手臂","背部","腿部","胸部","臀部"), focusAreas,
                    onNext = { step = 3 })
                3 -> StepChips("偏好运动类型", listOf("力量训练","有氧运动","瑜伽","HIIT","游泳","跑步"), workoutTypes,
                    onNext = {
                        onFinish(UserProfile(
                            gender = gender,
                            age = age.toIntOrNull() ?: 0,
                            weightKg = weight.toFloatOrNull() ?: 0f,
                            heightCm = height.toFloatOrNull() ?: 0f,
                            goals = goals.toList(),
                            focusAreas = focusAreas.toList(),
                            workoutTypes = workoutTypes.toList()
                        ))
                    })
            }
        }
        // Step dots
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { i ->
                Box(modifier = Modifier.size(if (i == step) 24.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == step) ACCENT else Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}

