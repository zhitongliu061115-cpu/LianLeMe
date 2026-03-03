package com.example.helloapp.model

data class TrainingItem(
    val title: String,
    val details: String,
    val icon: String
) {
    /**
     * 解析 details 字段，提取组数和每组次数/时长。
     * 支持格式：
     *   "3组 | 15次/组"  → sets=3, repsPerSet=15, isTimeBased=false
     *   "1组 | 30分钟"   → sets=1, durationMinutes=30, isTimeBased=true
     *   "6组 | 200米/组" → sets=6, repsPerSet=200, isTimeBased=false (距离当作次数模拟)
     *   "5组 | 3分钟/组" → sets=5, durationMinutes=3, isTimeBased=true
     */
    fun toSpec(): ExerciseSpec {
        val setsMatch = Regex("(\\d+)组").find(details)
        val sets = setsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

        // 时间型："30分钟" 或 "3分钟/组"
        val minutesMatch = Regex("(\\d+)分钟").find(details)
        if (minutesMatch != null) {
            val minutes = minutesMatch.groupValues[1].toInt()
            return ExerciseSpec(sets = sets, repsPerSet = 0, isTimeBased = true, durationMinutes = minutes)
        }

        // 次数型："15次/组" 或距离型 "200米/组"
        val repsMatch = Regex("(\\d+)[次米]").find(details)
        val reps = repsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10

        return ExerciseSpec(sets = sets, repsPerSet = reps, isTimeBased = false, durationMinutes = 0)
    }
}

/**
 * 训练动作的结构化参数。
 */
data class ExerciseSpec(
    val sets: Int,           // 总组数
    val repsPerSet: Int,     // 每组次数（仅次数型有效）
    val isTimeBased: Boolean,// true=按时间计, false=按次数计
    val durationMinutes: Int // 每组时长（仅时间型有效）
)

