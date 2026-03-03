package com.example.helloapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloapp.model.TrainingItem

@Composable
fun ActionButtons(
    exercises: List<TrainingItem>,
    onStartTraining: (List<TrainingItem>, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 35.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = {
                // 自由训练：创建一个默认动作
                val freeExercise = listOf(TrainingItem("自由训练", "1组 | 30分钟", "🏋️"))
                onStartTraining(freeExercise, 0)
            },
            modifier = Modifier.weight(1f).height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xB3FFFFFF)),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
        ) {
            Text("自由训练", color = Color(0xFF2d3748), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Button(
            onClick = {
                // 开始训练：传递当天全部动作，从第一个开始
                if (exercises.isNotEmpty()) {
                    onStartTraining(exercises, 0)
                }
            },
            modifier = Modifier.weight(1f).height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC6DD5C3)),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
        ) {
            Text("开始训练", color = Color(0xFF2d3748), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = {
                // 模拟测试：用一个少量次数的测试动作
                val testExercises = listOf(
                    TrainingItem("测试动作 A", "2组 | 3次/组", "🧘"),
                    TrainingItem("测试动作 B", "1组 | 2次/组", "💪")
                )
                onStartTraining(testExercises, 0)
            },
            modifier = Modifier.weight(1f).height(70.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xB3FFFFFF)),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
        ) {
            Text("模拟测试", color = Color(0xFF2d3748), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

