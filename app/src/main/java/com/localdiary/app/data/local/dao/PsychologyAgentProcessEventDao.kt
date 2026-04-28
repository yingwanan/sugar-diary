package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.PsychologyAgentProcessEventEntity

@Dao
interface PsychologyAgentProcessEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PsychologyAgentProcessEventEntity)

    @Query("SELECT * FROM psychology_agent_process_events WHERE runId = :runId ORDER BY sequence ASC")
    suspend fun listForRun(runId: String): List<PsychologyAgentProcessEventEntity>

    @Query("SELECT * FROM psychology_agent_process_events")
    suspend fun getAll(): List<PsychologyAgentProcessEventEntity>

    @Query("DELETE FROM psychology_agent_process_events WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)
}
