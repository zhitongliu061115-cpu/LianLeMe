package com.example.helloapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.helloapp.ui.components.DayItem
import java.util.Calendar

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * Generates a list of (dayName, dayOfMonth) pairs for a range of days
 * centered around today. totalDays should be odd for symmetry.
 */
private fun generateDateRange(totalDays: Int = 35): List<Triple<String, String, Int>> {
    val cal = Calendar.getInstance()
    val todayOffset = totalDays / 2
    cal.add(Calendar.DAY_OF_MONTH, -todayOffset)

    return List(totalDays) {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val dayNameIndex = when (dow) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            else -> 6
        }
        val dayName = DAY_NAMES[dayNameIndex]
        val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
        val globalIndex = it
        cal.add(Calendar.DAY_OF_MONTH, 1)
        Triple(dayName, dayNumber, globalIndex)
    }
}

@Composable
fun WeekCalendar(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    val totalDays = 35 // 5 weeks: 2.5 weeks before today, 2.5 weeks after
    val centerIndex = totalDays / 2 // today's position in the list

    val dateRange = remember { generateDateRange(totalDays) }
    val listState = rememberLazyListState()

    // Scroll to center (today) on first composition
    LaunchedEffect(Unit) {
        // Position today roughly in the middle of the visible area
        // Assuming ~7 items visible, offset by 3 to center
        listState.scrollToItem((centerIndex - 3).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(totalDays) { index ->
            val (dayName, dayNumber, _) = dateRange[index]
            DayItem(
                dayName = dayName,
                dayNumber = dayNumber,
                isSelected = index == selectedDay,
                isToday = index == centerIndex,
                onClick = { onDaySelected(index) }
            )
        }
    }
}
