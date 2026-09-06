package com.example.model

data class PanelBookmark(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mangaId: String,
    val mangaTitle: String,
    val chapterTitle: String,
    val pageNumber: Int,
    val imageUrl: String? = null,
    val userNote: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ReadingQuest(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val currentProgress: Int,
    val targetProgress: Int,
    val expReward: Int,
    val isCompleted: Boolean = false
)
