package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.StylePresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StylePresetDao {
    @Query("SELECT * FROM style_presets ORDER BY isBuiltin DESC, name ASC")
    fun observeAll(): Flow<List<StylePresetEntity>>

    @Query("SELECT * FROM style_presets ORDER BY isBuiltin DESC, name ASC")
    suspend fun getAll(): List<StylePresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StylePresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StylePresetEntity>)

    @Query("SELECT COUNT(*) FROM style_presets WHERE isBuiltin = 1")
    suspend fun countBuiltins(): Int
}
