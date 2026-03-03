package com.example.helloapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigation(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xF2DCE6EE),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(icon = "🏠", label = "首页",   isSelected = selectedItem == 0, onClick = { onItemSelected(0) })
            NavItem(icon = "🎧", label = "AI教练", isSelected = selectedItem == 1, onClick = { onItemSelected(1) })
            NavItem(icon = "⚙️", label = "设置",   isSelected = selectedItem == 2, onClick = { onItemSelected(2) })
        }
    }
}

@Composable
fun NavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color(0xFF2d3748) else Color(0xFF4a5568)
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF6DD5C3))
            )
        }
    }
}

