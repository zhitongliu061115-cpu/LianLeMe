package com.example.helloapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GreetingHeader(greeting: String) {
    var timeStr by remember { mutableStateOf(currentTime()) }
    var dateStr by remember { mutableStateOf(currentDate()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            timeStr = currentTime()
            dateStr = currentDate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 4.dp)
    ) {
        Text(
            text = greeting,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = dateStr, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            Text(text = timeStr, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

private fun currentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
private fun currentDate(): String = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date())

