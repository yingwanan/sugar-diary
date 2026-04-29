package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.PsychologyChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PsychologyChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PsychologyChatMessageEntity)

    @Query("SELECT * FROM psychology_chat_messages WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeForEntry(entryId: String): Flow<List<PsychologyChatMessageEntity>>

    @Query("SELECT * FROM psychology_chat_messages WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun listForEntry(entryId: String): List<PsychologyChatMessageEntity>

    @Query("DELETE FROM psychology_chat_messages WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM psychology_chat_messages")
    suspend fun getAll(): List<PsychologyChatMessageEntity>
}
