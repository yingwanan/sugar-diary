package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.MoodReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MoodReportEntity)

    @Query("SELECT * FROM mood_reports ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MoodReportEntity>>

    @Query("SELECT * FROM mood_reports")
    suspend fun getAll(): List<MoodReportEntity>
}
