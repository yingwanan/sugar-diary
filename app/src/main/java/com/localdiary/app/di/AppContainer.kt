package com.localdiary.app.di

import android.content.Context
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.data.file.LocalEntryFileStore
import com.localdiary.app.data.image.EmbeddedImageService
import com.localdiary.app.data.llm.OpenAiCompatibleLlmProvider
import com.localdiary.app.data.local.AppDatabase
import com.localdiary.app.data.settings.AiSettingsRepository
import com.localdiary.app.data.transfer.TransferManager
import com.localdiary.app.domain.report.MoodReportGenerator
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy { AppDatabase.build(applicationContext) }
    private val fileStore by lazy { LocalEntryFileStore(applicationContext) }
    private val embeddedImageService by lazy { EmbeddedImageService(applicationContext.contentResolver) }
    private val aiSettingsRepository by lazy { AiSettingsRepository(applicationContext) }
    private val llmProvider by lazy { OpenAiCompatibleLlmProvider() }
    private val transferManager by lazy { TransferManager(applicationContext, fileStore) }
    val uiMessageManager by lazy { UiMessageManager() }

    val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(
            entryDao = database.entryDao(),
            styleDao = database.stylePresetDao(),
            emotionDao = database.emotionAnalysisDao(),
            moodReportDao = database.moodReportDao(),
            versionDao = database.versionSnapshotDao(),
            chatDao = database.psychologyChatMessageDao(),
            fileStore = fileStore,
            aiSettingsRepository = aiSettingsRepository,
            llmProvider = llmProvider,
            transferManager = transferManager,
            moodReportGenerator = MoodReportGenerator(),
            embeddedImageService = embeddedImageService,
        )
    }

    init {
        appScope.launch {
            diaryRepository.seedBuiltinStyles()
            diaryRepository.deleteLegacyDailyReports()
        }
    }
}
