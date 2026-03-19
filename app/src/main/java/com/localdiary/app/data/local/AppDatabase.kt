package com.localdiary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.localdiary.app.data.local.dao.EmotionAnalysisDao
import com.localdiary.app.data.local.dao.EntryDao
import com.localdiary.app.data.local.dao.MoodReportDao
import com.localdiary.app.data.local.dao.StylePresetDao
import com.localdiary.app.data.local.dao.VersionSnapshotDao
import com.localdiary.app.data.local.entity.EmotionAnalysisEntity
import com.localdiary.app.data.local.entity.EntryEntity
import com.localdiary.app.data.local.entity.MoodReportEntity
import com.localdiary.app.data.local.entity.StylePresetEntity
import com.localdiary.app.data.local.entity.VersionSnapshotEntity

@Database(
    entities = [
        EntryEntity::class,
        StylePresetEntity::class,
        EmotionAnalysisEntity::class,
        MoodReportEntity::class,
        VersionSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun stylePresetDao(): StylePresetDao
    abstract fun emotionAnalysisDao(): EmotionAnalysisDao
    abstract fun moodReportDao(): MoodReportDao
    abstract fun versionSnapshotDao(): VersionSnapshotDao

    companion object {
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "diary_app.db",
        ).build()
    }
}
