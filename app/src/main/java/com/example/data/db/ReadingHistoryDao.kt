package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {
    @Query("SELECT * FROM reading_history ORDER BY lastReadTimestamp DESC")
    fun getAllHistory(): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE mangaId = :mangaId LIMIT 1")
    suspend fun getHistoryForManga(mangaId: String): ReadingHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: ReadingHistoryEntity)

    @Query("DELETE FROM reading_history WHERE mangaId = :mangaId")
    suspend fun deleteHistory(mangaId: String)

    @Query("DELETE FROM reading_history")
    suspend fun clearAllHistory()
}
