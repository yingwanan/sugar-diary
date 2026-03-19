package com.localdiary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "mood_reports")
data class MoodReportEntity(
    @PrimaryKey val id: String,
    val period: String,
    val rangeStart: Long,
    val rangeEnd: Long,
    val dominantMoodsJson: String,
    val averageIntensity: Int,
    val summary: String,
    val adviceJson: String,
    val createdAt: Long,
)
