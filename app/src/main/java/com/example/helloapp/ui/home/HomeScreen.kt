package com.example.helloapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.ui.components.BottomNavigation
import com.example.helloapp.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    selectedNavItem: Int,
    onNavItemSelected: (Int) -> Unit,
    onStartTraining: (List<TrainingItem>, Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val selectedDay by viewModel.selectedDay.collectAsState()
    val trainingItems by viewModel.trainingItems.collectAsState()
    val greeting by viewModel.greeting.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF7B9DB8), Color(0xFF9CB4C8))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp)
        ) {
            GreetingHeader(greeting = greeting)
            WeekCalendar(
                selectedDay = selectedDay,
                onDaySelected = { viewModel.selectDay(it) }
            )
            TrainingList(
                items = trainingItems
            )
            ActionButtons(
                exercises = trainingItems,
                onStartTraining = onStartTraining
            )
        }

        BottomNavigation(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


