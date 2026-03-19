package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.EmotionAnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmotionAnalysisEntity)

    @Query("SELECT * FROM emotion_analyses WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun listForEntry(entryId: String): List<EmotionAnalysisEntity>

    @Query("SELECT * FROM emotion_analyses WHERE entryId = :entryId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestForEntry(entryId: String): EmotionAnalysisEntity?

    @Query("SELECT * FROM emotion_analyses WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    suspend fun listBetween(start: Long, end: Long): List<EmotionAnalysisEntity>

    @Query("SELECT * FROM emotion_analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EmotionAnalysisEntity>>

    @Query("DELETE FROM emotion_analyses WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM emotion_analyses")
    suspend fun getAll(): List<EmotionAnalysisEntity>
}
