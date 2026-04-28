package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "user_psychology_profiles")
data class UserPsychologyProfileEntity(
    @PrimaryKey val id: String = LOCAL_PROFILE_ID,
    val triggersJson: String = "[]",
    val cognitivePatternsJson: String = "[]",
    val needsJson: String = "[]",
    val relationshipPatternsJson: String = "[]",
    val defensePatternsJson: String = "[]",
    val bodyStressSignalsJson: String = "[]",
    val strengthsJson: String = "[]",
    val riskNotesJson: String = "[]",
    val updatedAt: Long = 0L,
    val userEditedAt: Long = 0L,
) {
    companion object {
        const val LOCAL_PROFILE_ID = "local-user"
    }
}
