package com.example.helloapp.model.ai

data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T
)
