package com.example.helloapp.model

data class Account(
    val username: String = "",
    val passwordHash: String = "",
    val displayName: String = "",
    val avatarPath: String = "",   // absolute path inside filesDir
    val gender: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val goal: String = "",
    val signature: String = ""
)

