package com.example.helloapp.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.ui.components.BottomNavigation
import com.example.helloapp.viewmodel.AICoachViewModel

@Composable
fun AICoachScreen(
    selectedNavItem: Int,
    onNavItemSelected: (Int) -> Unit,
    viewModel: AICoachViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8FA8BE))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    color = Color(0xFF2d3748),
                    modifier = Modifier.clickable { onNavItemSelected(0) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "AI教练",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2d3748),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            // 消息列表
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                messages.forEach { ChatMessageItem(it) }
            }

            // 输入栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFB8C9D6)),
                    contentAlignment = Alignment.Center
                ) { Text("🎤", fontSize = 24.sp) }

                TextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...", color = Color(0xFF6B7F92)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFB8C9D6),
                        unfocusedContainerColor = Color(0xFFB8C9D6),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp).clip(CircleShape).background(Color(0xFFB8C9D6))
                        .clickable { viewModel.sendMessage() },
                    contentAlignment = Alignment.Center
                ) { Text("✈️", fontSize = 24.sp) }
            }
        }

        BottomNavigation(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

