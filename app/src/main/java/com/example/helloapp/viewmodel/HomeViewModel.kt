package com.example.helloapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.PlanRepository
import com.example.helloapp.model.TrainingItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    app: Application,
    private val username: String?
) : AndroidViewModel(app) {

    private val planRepo = PlanRepository(app)

    companion object {
        const val TOTAL_DAYS = 35
        val CENTER_INDEX = TOTAL_DAYS / 2

        fun greeting(): String {
            return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "早上好 ☀️"
                in 12..13 -> "中午好 🌤"
                in 14..17 -> "下午好 🌈"
                in 18..21 -> "晚上好 🌙"
                else -> "夜深了 🌟"
            }
        }
    }

    private val _selectedDay = MutableStateFlow(CENTER_INDEX)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _trainingItems = MutableStateFlow<List<TrainingItem>>(emptyList())
    val trainingItems: StateFlow<List<TrainingItem>> = _trainingItems.asStateFlow()

    private val _greeting = MutableStateFlow(greeting())
    val greeting: StateFlow<String> = _greeting.asStateFlow()

    init {
        loadPlan(_selectedDay.value)
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                _greeting.value = greeting()
            }
        }
    }

    fun selectDay(dayIndex: Int) {
        _selectedDay.value = dayIndex
        loadPlan(dayIndex)
    }

    fun refreshPlan() {
        loadPlan(_selectedDay.value)
    }

    private fun dateForIndex(index: Int): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, index - CENTER_INDEX)
        }
    }

    private fun loadPlan(rangeIndex: Int) {
        val targetDate = dateForIndex(rangeIndex)
        _trainingItems.value = planRepo.getPlanForDate(username, targetDate)
    }

    class Factory(
        private val app: Application,
        private val username: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(app, username) as T
        }
    }
}
