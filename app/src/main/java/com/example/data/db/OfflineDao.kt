package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineDao {

    @Query("SELECT * FROM offline_mangas ORDER BY downloadTimestamp DESC")
    fun getAllOfflineMangas(): Flow<List<OfflineMangaEntity>>

    @Query("SELECT * FROM offline_mangas WHERE mangaId = :mangaId LIMIT 1")
    fun getOfflineManga(mangaId: String): Flow<OfflineMangaEntity?>

    @Query("SELECT * FROM offline_mangas WHERE mangaId = :mangaId LIMIT 1")
    suspend fun getOfflineMangaSync(mangaId: String): OfflineMangaEntity?

    @Query("SELECT * FROM offline_chapters WHERE mangaId = :mangaId ORDER BY downloadTimestamp ASC")
    fun getOfflineChaptersForManga(mangaId: String): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE mangaId = :mangaId")
    suspend fun getOfflineChaptersForMangaSync(mangaId: String): List<OfflineChapterEntity>

    @Query("SELECT * FROM offline_chapters WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getOfflineChapter(chapterId: String): OfflineChapterEntity?

    @Query("SELECT chapterId FROM offline_chapters")
    fun getAllDownloadedChapterIds(): Flow<List<String>>

    @Query("SELECT mangaId FROM offline_mangas")
    fun getAllDownloadedMangaIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineManga(manga: OfflineMangaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineChapter(chapter: OfflineChapterEntity)

    @Query("DELETE FROM offline_mangas WHERE mangaId = :mangaId")
    suspend fun deleteOfflineManga(mangaId: String)

    @Query("DELETE FROM offline_chapters WHERE chapterId = :chapterId")
    suspend fun deleteOfflineChapter(chapterId: String)

    @Query("DELETE FROM offline_chapters WHERE mangaId = :mangaId")
    suspend fun deleteOfflineChaptersForManga(mangaId: String)

    @Query("DELETE FROM offline_mangas")
    suspend fun clearAllOfflineMangas()

    @Query("DELETE FROM offline_chapters")
    suspend fun clearAllOfflineChapters()
}
