package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_mangas")
data class OfflineMangaEntity(
    @PrimaryKey
    val mangaId: String,
    val title: String,
    val coverUrl: String?,
    val author: String? = null,
    val description: String? = null,
    val contentRating: String? = "safe",
    val downloadedChaptersCount: Int = 0,
    val totalChaptersCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val downloadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_chapters")
data class OfflineChapterEntity(
    @PrimaryKey
    val chapterId: String,
    val mangaId: String,
    val chapterNumber: String,
    val title: String,
    val pagesJson: String, // JSON array of local file paths or URLs
    val pageCount: Int,
    val downloadTimestamp: Long = System.currentTimeMillis()
)
