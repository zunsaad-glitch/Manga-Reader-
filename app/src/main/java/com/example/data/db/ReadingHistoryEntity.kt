package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey
    val mangaId: String,
    val title: String,
    val coverUrl: String?,
    val lastChapterId: String? = null,
    val lastChapterNumber: String? = null,
    val lastChapterTitle: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
