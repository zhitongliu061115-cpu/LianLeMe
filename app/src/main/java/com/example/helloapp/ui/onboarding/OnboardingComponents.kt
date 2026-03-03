package com.example.helloapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ACCENT = Color(0xFF6DD5C3)
private val CARD_BG = Color.White.copy(alpha = 0.12f)

@Composable
fun StepBodyData(
    gender: String, age: String, weight: String, height: String,
    onGender: (String) -> Unit, onAge: (String) -> Unit,
    onWeight: (String) -> Unit, onHeight: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("告诉我们关于你", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("帮助我们为你定制专属计划", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))

        // Gender
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("male" to "♂ 男", "female" to "♀ 女").forEach { (key, label) ->
                val selected = gender == key
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (selected) ACCENT else CARD_BG)
                        .border(1.dp, if (selected) ACCENT else Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onGender(key) }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }

        OnboardingTextField("年龄", age, "岁", onAge)
        OnboardingTextField("体重", weight, "kg", onWeight)
        OnboardingTextField("身高", height, "cm", onHeight)

        NextButton(enabled = gender.isNotEmpty() && age.isNotEmpty(), onClick = onNext)
    }
}

@Composable
fun OnboardingTextField(label: String, value: String, unit: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue,
        label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
        suffix = { Text(unit, color = Color.White.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ACCENT, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
            focusedTextColor = Color.White, unfocusedTextColor = Color.White
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepChips(title: String, options: List<String>, selected: MutableList<String>, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("可多选", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { opt ->
                val isSelected = selected.contains(opt)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ACCENT else CARD_BG)
                        .border(1.dp, if (isSelected) ACCENT else Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { if (isSelected) selected.remove(opt) else selected.add(opt) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) { Text(opt, color = Color.White, fontSize = 14.sp) }
            }
        }

        NextButton(enabled = selected.isNotEmpty(), onClick = onNext)
    }
}

@Composable
fun NextButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ACCENT, disabledContainerColor = ACCENT.copy(alpha = 0.4f))
    ) { Text("下一步", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
}

