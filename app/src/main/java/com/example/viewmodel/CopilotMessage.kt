package com.example.viewmodel

data class CopilotMessage(
    val role: String, // "user" or "model"
    val text: String
)
