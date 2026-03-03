package com.example.helloapp.data

import android.content.Context
import com.example.helloapp.model.TrainingItem
import com.example.helloapp.model.WeeklyPlan
import com.google.gson.Gson

class PlanRepository(private val context: Context) {
    private val gson = Gson()
    private val planFile get() = context.filesDir.resolve("training_plan.json")

    private fun ensureFile() {
        if (!planFile.exists()) {
            context.assets.open("training_plan.json").use { input ->
                planFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun loadWeeklyPlan(): WeeklyPlan {
        ensureFile()
        return runCatching {
            gson.fromJson(planFile.readText(), WeeklyPlan::class.java)
        }.getOrDefault(WeeklyPlan())
    }

    /** dayIndex: 0=周一 … 6=周日 */
    fun getPlanForDay(dayIndex: Int): List<TrainingItem> {
        val dayKeys = listOf("monday","tuesday","wednesday","thursday","friday","saturday","sunday")
        val key = dayKeys.getOrNull(dayIndex) ?: return emptyList()
        return loadWeeklyPlan().days.find { it.day == key }?.exercises ?: emptyList()
    }
}

