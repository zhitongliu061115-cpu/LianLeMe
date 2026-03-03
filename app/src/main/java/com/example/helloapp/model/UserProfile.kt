package com.example.helloapp.model

data class UserProfile(
    val gender: String = "",          // "male" | "female"
    val age: Int = 0,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val goals: List<String> = emptyList(),       // e.g. ["减脂","增肌"]
    val focusAreas: List<String> = emptyList(),  // e.g. ["腹部","手臂"]
    val workoutTypes: List<String> = emptyList() // e.g. ["力量","有氧"]
)

