package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.PsychologyAnalysisRunEntity

@Dao
interface PsychologyAnalysisRunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PsychologyAnalysisRunEntity)

    @Query("SELECT * FROM psychology_analysis_runs WHERE entryId = :entryId ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestForEntry(entryId: String): PsychologyAnalysisRunEntity?

    @Query("SELECT * FROM psychology_analysis_runs")
    suspend fun getAll(): List<PsychologyAnalysisRunEntity>

    @Query("DELETE FROM psychology_analysis_runs WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)
}
