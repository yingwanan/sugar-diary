package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.VersionSnapshotEntity

@Dao
interface VersionSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VersionSnapshotEntity)

    @Query("SELECT * FROM version_snapshots WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun listForEntry(entryId: String): List<VersionSnapshotEntity>

    @Query("SELECT * FROM version_snapshots")
    suspend fun getAll(): List<VersionSnapshotEntity>

    @Query("DELETE FROM version_snapshots WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)
}
