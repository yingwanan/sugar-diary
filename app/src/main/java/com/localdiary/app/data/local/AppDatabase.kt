package com.localdiary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localdiary.app.data.local.dao.EmotionAnalysisDao
import com.localdiary.app.data.local.dao.EntryDao
import com.localdiary.app.data.local.dao.MoodReportDao
import com.localdiary.app.data.local.dao.PsychologyChatMessageDao
import com.localdiary.app.data.local.dao.StylePresetDao
import com.localdiary.app.data.local.dao.VersionSnapshotDao
import com.localdiary.app.data.local.entity.EmotionAnalysisEntity
import com.localdiary.app.data.local.entity.EntryEntity
import com.localdiary.app.data.local.entity.MoodReportEntity
import com.localdiary.app.data.local.entity.PsychologyChatMessageEntity
import com.localdiary.app.data.local.entity.StylePresetEntity
import com.localdiary.app.data.local.entity.VersionSnapshotEntity

@Database(
    entities = [
        EntryEntity::class,
        StylePresetEntity::class,
        EmotionAnalysisEntity::class,
        MoodReportEntity::class,
        VersionSnapshotEntity::class,
        PsychologyChatMessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun stylePresetDao(): StylePresetDao
    abstract fun emotionAnalysisDao(): EmotionAnalysisDao
    abstract fun moodReportDao(): MoodReportDao
    abstract fun versionSnapshotDao(): VersionSnapshotDao
    abstract fun psychologyChatMessageDao(): PsychologyChatMessageDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN triggersJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN cognitivePatternsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN needsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN relationshipSignalsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN defenseMechanismsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN strengthsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS psychology_chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        entryId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "diary_app.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}
