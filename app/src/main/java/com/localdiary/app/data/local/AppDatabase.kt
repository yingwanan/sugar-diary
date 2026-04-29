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
import com.localdiary.app.data.local.dao.UserPsychologyProfileDao
import com.localdiary.app.data.local.dao.PsychologyAgentProcessEventDao
import com.localdiary.app.data.local.dao.PsychologyAnalysisRunDao
import com.localdiary.app.data.local.dao.StylePresetDao
import com.localdiary.app.data.local.dao.VersionSnapshotDao
import com.localdiary.app.data.local.entity.EmotionAnalysisEntity
import com.localdiary.app.data.local.entity.EntryEntity
import com.localdiary.app.data.local.entity.MoodReportEntity
import com.localdiary.app.data.local.entity.PsychologyChatMessageEntity
import com.localdiary.app.data.local.entity.UserPsychologyProfileEntity
import com.localdiary.app.data.local.entity.PsychologyAgentProcessEventEntity
import com.localdiary.app.data.local.entity.PsychologyAnalysisRunEntity
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
        PsychologyAnalysisRunEntity::class,
        PsychologyAgentProcessEventEntity::class,
        UserPsychologyProfileEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun stylePresetDao(): StylePresetDao
    abstract fun emotionAnalysisDao(): EmotionAnalysisDao
    abstract fun moodReportDao(): MoodReportDao
    abstract fun versionSnapshotDao(): VersionSnapshotDao
    abstract fun psychologyChatMessageDao(): PsychologyChatMessageDao
    abstract fun psychologyAnalysisRunDao(): PsychologyAnalysisRunDao
    abstract fun psychologyAgentProcessEventDao(): PsychologyAgentProcessEventDao
    abstract fun userPsychologyProfileDao(): UserPsychologyProfileDao

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


        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN bodyStressSignalsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE emotion_analyses ADD COLUMN riskNotesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS psychology_analysis_runs (
                        id TEXT NOT NULL PRIMARY KEY,
                        entryId TEXT NOT NULL,
                        selectedAgentIdsJson TEXT NOT NULL,
                        status TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        finalAnalysisId TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS psychology_agent_process_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        runId TEXT NOT NULL,
                        entryId TEXT NOT NULL,
                        agentId TEXT NOT NULL,
                        agentName TEXT NOT NULL,
                        phase TEXT NOT NULL,
                        title TEXT NOT NULL,
                        contentMarkdown TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_psychology_profiles (
                        id TEXT NOT NULL PRIMARY KEY,
                        triggersJson TEXT NOT NULL DEFAULT '[]',
                        cognitivePatternsJson TEXT NOT NULL DEFAULT '[]',
                        needsJson TEXT NOT NULL DEFAULT '[]',
                        relationshipPatternsJson TEXT NOT NULL DEFAULT '[]',
                        defensePatternsJson TEXT NOT NULL DEFAULT '[]',
                        bodyStressSignalsJson TEXT NOT NULL DEFAULT '[]',
                        strengthsJson TEXT NOT NULL DEFAULT '[]',
                        riskNotesJson TEXT NOT NULL DEFAULT '[]',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        userEditedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "diary_app.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
