package com.example.helloapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.helloapp.data.PlanRepository
import com.example.helloapp.model.TrainingItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val planRepo = PlanRepository(app)

    companion object {
        const val TOTAL_DAYS = 35
        val CENTER_INDEX = TOTAL_DAYS / 2 // today's index in the range

        fun greeting(): String {
            return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "早上好 ☀️"
                in 12..13 -> "中午好 🌤"
                in 14..17 -> "下午好 🌈"
                in 18..21 -> "晚上好 🌙"
                else -> "夜深了 🌟"
            }
        }

        /**
         * Given an index in the 35-day range (0..34, center=17 is today),
         * returns the day-of-week index 0=Mon..6=Sun for that date.
         */
        fun dayOfWeekForIndex(index: Int): Int {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, index - CENTER_INDEX)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            return when (dow) {
                Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
                else -> 6
            }
        }
    }

    // selectedDay is now an index in the 35-day range; default to today (center)
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

    private fun loadPlan(rangeIndex: Int) {
        // Convert range index to day-of-week (0=Mon..6=Sun) for the plan lookup
        val dayOfWeek = dayOfWeekForIndex(rangeIndex)
        _trainingItems.value = planRepo.getPlanForDay(dayOfWeek)
    }
}
