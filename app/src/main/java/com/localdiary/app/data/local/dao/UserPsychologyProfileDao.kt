package com.localdiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.localdiary.app.data.local.entity.UserPsychologyProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPsychologyProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UserPsychologyProfileEntity)

    @Query("SELECT * FROM user_psychology_profiles WHERE id = :id LIMIT 1")
    suspend fun get(id: String = UserPsychologyProfileEntity.LOCAL_PROFILE_ID): UserPsychologyProfileEntity?

    @Query("SELECT * FROM user_psychology_profiles WHERE id = :id LIMIT 1")
    fun observe(id: String = UserPsychologyProfileEntity.LOCAL_PROFILE_ID): Flow<UserPsychologyProfileEntity?>

    @Query("SELECT * FROM user_psychology_profiles")
    suspend fun getAll(): List<UserPsychologyProfileEntity>
}
