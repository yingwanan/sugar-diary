package com.localdiary.app.data

import android.content.ContentResolver
import android.net.Uri
import com.localdiary.app.data.file.LocalEntryFileStore
import com.localdiary.app.data.image.EmbeddedImageInsertResult
import com.localdiary.app.data.image.EmbeddedImageService
import com.localdiary.app.data.llm.LlmProvider
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
import com.localdiary.app.data.settings.AiSettingsRepository
import com.localdiary.app.data.transfer.TransferManager
import com.localdiary.app.domain.emotion.EmotionCenterProjector
import com.localdiary.app.domain.browser.EntryPreviewFormatter
import com.localdiary.app.domain.psychology.PsychologyAgentContextBuilder
import com.localdiary.app.domain.report.MoodReportGenerator
import com.localdiary.app.model.AppStorageSettings
import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.EmbeddedImageParser
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EmotionCenterItem
import com.localdiary.app.model.EntryEmotionSummary
import com.localdiary.app.model.EntryDocument
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.EntryMeta
import com.localdiary.app.model.MoodReport
import com.localdiary.app.model.PeriodicReportResult
import com.localdiary.app.model.PolishCandidate
import com.localdiary.app.model.PsychologyAnalysisResult
import com.localdiary.app.model.PsychologyChatMessage
import com.localdiary.app.model.PsychologyChatRole
import com.localdiary.app.model.ReportPeriod
import com.localdiary.app.model.ReviewResult
import com.localdiary.app.model.StorageMode
import com.localdiary.app.model.StylePreset
import com.localdiary.app.model.VersionSnapshot
import com.localdiary.app.data.local.dao.PsychologyAnalysisRunDao
import com.localdiary.app.data.local.dao.PsychologyAgentProcessEventDao
import com.localdiary.app.data.local.dao.UserPsychologyProfileDao
import com.localdiary.app.data.local.entity.PsychologyAnalysisRunEntity
import com.localdiary.app.data.local.entity.PsychologyAgentProcessEventEntity
import com.localdiary.app.data.local.entity.UserPsychologyProfileEntity
import com.localdiary.app.domain.psychology.PsychologyAgentCatalog
import com.localdiary.app.domain.psychology.PsychologyAgentOrchestrator
import com.localdiary.app.domain.psychology.UserPsychologyProfileUpdate
import com.localdiary.app.model.PsychologyAgentPhase
import com.localdiary.app.model.PsychologyAgentProcessEvent
import com.localdiary.app.model.PsychologyAgentRuntimeUpdate
import com.localdiary.app.model.PsychologyAnalysisRunStatus
import com.localdiary.app.model.UserPsychologyProfile
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DiaryRepository(
    private val entryDao: EntryDao,
    private val styleDao: StylePresetDao,
    private val emotionDao: EmotionAnalysisDao,
    private val moodReportDao: MoodReportDao,
    private val versionDao: VersionSnapshotDao,
    private val chatDao: PsychologyChatMessageDao,
    private val runDao: PsychologyAnalysisRunDao,
    private val eventDao: PsychologyAgentProcessEventDao,
    private val profileDao: UserPsychologyProfileDao,
    private val fileStore: LocalEntryFileStore,
    private val aiSettingsRepository: AiSettingsRepository,
    private val llmProvider: LlmProvider,
    private val transferManager: TransferManager,
    private val moodReportGenerator: MoodReportGenerator,
    private val embeddedImageService: EmbeddedImageService,
    private val orchestrator: PsychologyAgentOrchestrator = PsychologyAgentOrchestrator(llmProvider),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun observeEntries(): Flow<List<EntryMeta>> = entryDao.observeAll().map { entries ->
        entries.map(::entryToModel)
    }

    fun observeBrowserItems(): Flow<List<EntryBrowserItem>> = combine(
        entryDao.observeAll(),
        emotionDao.observeAll(),
    ) { entries, analyses ->
        val latestByEntry = analyses
            .groupBy { it.entryId }
            .mapValues { (_, items) -> items.maxByOrNull { it.createdAt } }
        entries.map { entry ->
            val meta = entryToModel(entry)
            EntryBrowserItem(
                meta = meta,
                previewText = runCatching {
                    EntryPreviewFormatter.buildPreview(fileStore.readContent(meta.filePath))
                }.getOrDefault("暂无正文摘要"),
                latestEmotion = latestByEntry[entry.id]?.let { analysis ->
                    EntryEmotionSummary(
                        labels = json.decodeFromString(analysis.labelsJson),
                        summary = analysis.summary,
                        createdAt = analysis.createdAt,
                    )
                },
            )
        }
    }

    fun observeReports(): Flow<List<MoodReport>> = moodReportDao.observeAll().map { reports ->
        reports.mapNotNull(::reportToModel)
    }

    fun observeEmotionCenterItems(): Flow<List<EmotionCenterItem>> = combine(
        entryDao.observeAll(),
        emotionDao.observeAll(),
    ) { entries, analyses ->
        EmotionCenterProjector.project(
            entries = entries.map(::entryToModel),
            analyses = analyses.map(::analysisToModel),
        )
    }

    fun observeStyles(): Flow<List<StylePreset>> = styleDao.observeAll().map { styles ->
        styles.map(::styleToModel)
    }

    fun observePsychologyChat(entryId: String): Flow<List<PsychologyChatMessage>> =
        chatDao.observeForEntry(entryId).map { messages ->
            messages.map(::chatMessageToModel)
        }

    fun observeUserProfile(): Flow<UserPsychologyProfile> =
        profileDao.observe().map { it?.let(::profileToModel) ?: UserPsychologyProfile() }

    suspend fun loadUserProfile(): UserPsychologyProfile =
        profileDao.get()?.let(::profileToModel) ?: UserPsychologyProfile()

    suspend fun saveUserProfile(profile: UserPsychologyProfile) {
        val now = System.currentTimeMillis()
        profileDao.insert(profile.copy(updatedAt = now, userEditedAt = now).toEntity())
    }

    suspend fun latestAgentEvents(entryId: String): List<PsychologyAgentProcessEvent> {
        val run = runDao.latestForEntry(entryId) ?: return emptyList()
        return eventDao.listForRun(run.id).map(::processEventToModel)
    }

    fun runPsychologyAnalysis(
        entryId: String,
        selectedAgentId: String? = null,
    ): Flow<PsychologyAgentRuntimeUpdate> = flow {
        val config = aiSettingsRepository.load()
        require(config.isConfigured) { "请先在设置中配置主模型 API。" }
        val document = loadDocument(entryId)
        val prepared = EmbeddedImageParser.sanitizeForLlm(document.content)
        val latest = latestAnalysis(entryId)
        val history = emotionDao.getAll().map(::analysisToModel)
        val profile = loadUserProfile()
        val runId = UUID.randomUUID().toString()
        val selectedAgents = PsychologyAgentCatalog.resolveSelection(selectedAgentId).map { it.id }
        runDao.insert(
            PsychologyAnalysisRunEntity(
                id = runId,
                entryId = entryId,
                selectedAgentIdsJson = json.encodeToString(selectedAgents),
                status = PsychologyAnalysisRunStatus.RUNNING.name,
                startedAt = System.currentTimeMillis(),
                completedAt = null,
                finalAnalysisId = null,
            ),
        )
        try {
            orchestrator.run(
                config = config,
                document = document,
                sanitizedContent = prepared.content,
                format = document.meta.format,
                latestAnalysis = latest,
                historicalAnalyses = history,
                currentProfile = profile,
                selectedAgentId = selectedAgentId,
                runId = runId,
            ).collect { update ->
                when (update) {
                    is PsychologyAgentRuntimeUpdate.Event -> {
                        val event = update.event
                        if (event.title == "__RESULT__") {
                            val parts = event.contentMarkdown.split("\n---PROFILE---\n", limit = 2)
                            val result = json.decodeFromString<PsychologyAnalysisResult>(parts.first())
                            val profileUpdate = json.decodeFromString<UserPsychologyProfileUpdate>(parts.getOrElse(1) { "{}" })
                            val analysisEntity = result.toEntity(entryId)
                            val mergedProfile = com.localdiary.app.domain.psychology.UserPsychologyProfileMerger.merge(
                                loadUserProfile(),
                                profileUpdate,
                                System.currentTimeMillis(),
                            )
                            emotionDao.insert(analysisEntity)
                            profileDao.insert(mergedProfile.toEntity())
                            runDao.insert(
                                PsychologyAnalysisRunEntity(
                                    id = runId,
                                    entryId = entryId,
                                    selectedAgentIdsJson = json.encodeToString(selectedAgents),
                                    status = PsychologyAnalysisRunStatus.COMPLETED.name,
                                    startedAt = event.createdAt,
                                    completedAt = System.currentTimeMillis(),
                                    finalAnalysisId = analysisEntity.id,
                                ),
                            )
                            emit(PsychologyAgentRuntimeUpdate.Completed(analysisToModel(analysisEntity), mergedProfile, runId))
                        } else {
                            if (!event.isPartial) eventDao.insert(event.toEntity())
                            emit(update)
                        }
                    }
                    is PsychologyAgentRuntimeUpdate.Completed -> emit(update)
                }
            }
        } catch (error: Throwable) {
            runDao.insert(
                PsychologyAnalysisRunEntity(
                    id = runId,
                    entryId = entryId,
                    selectedAgentIdsJson = json.encodeToString(selectedAgents),
                    status = PsychologyAnalysisRunStatus.FAILED.name,
                    startedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis(),
                    finalAnalysisId = null,
                ),
            )
            throw error
        }
    }

    suspend fun sendPsychologyChatMessage(entryId: String, message: String): PsychologyChatMessage {
        val trimmed = message.trim()
        require(trimmed.isNotBlank()) { "请输入想和心理 Agent 讨论的内容。" }
        val config = aiSettingsRepository.load()
        val document = loadDocument(entryId)
        val latest = latestAnalysis(entryId)
        val history = emotionDao.getAll()
            .asSequence()
            .filterNot { it.entryId == entryId && latest != null && it.id == latest.id }
            .sortedByDescending { it.createdAt }
            .map(::analysisToModel)
            .toList()
        val context = PsychologyAgentContextBuilder.build(
            currentTitle = document.meta.title,
            currentContent = EmbeddedImageParser.sanitizeForLlm(document.content).content,
            latestAnalysis = latest,
            historicalAnalyses = history,
        )
        val systemPrompt = PsychologyAgentContextBuilder.chatSystemPrompt(context)
        val previousMessages = chatDao.listForEntry(entryId).map(::chatMessageToModel)
        val now = System.currentTimeMillis()
        val userEntity = PsychologyChatMessageEntity(
            id = UUID.randomUUID().toString(),
            entryId = entryId,
            role = PsychologyChatRole.USER.name,
            content = trimmed,
            createdAt = now,
        )
        chatDao.insert(userEntity)
        val reply = llmProvider.chatPsychology(
            config = config,
            systemPrompt = systemPrompt,
            messages = previousMessages,
            userMessage = trimmed,
        ).reply.trim().ifBlank { "我暂时没有生成有效回复，请稍后重试。" }
        val assistantEntity = PsychologyChatMessageEntity(
            id = UUID.randomUUID().toString(),
            entryId = entryId,
            role = PsychologyChatRole.ASSISTANT.name,
            content = reply,
            createdAt = now + 1,
        )
        chatDao.insert(assistantEntity)
        return chatMessageToModel(assistantEntity)
    }

    suspend fun seedBuiltinStyles() {
        if (styleDao.countBuiltins() > 0) return
        styleDao.insertAll(
            listOf(
                StylePresetEntity("builtin-plain", "克制纪实", "保持克制、准确、少修饰，但增强画面感。", true),
                StylePresetEntity("builtin-warm", "温柔散文", "以温柔、细腻、富有情感流动的方式润色。", true),
                StylePresetEntity("builtin-clean", "结构清晰", "优化逻辑层次与段落衔接，保留原意。", true),
                StylePresetEntity("builtin-poetic", "诗性表达", "在不脱离原意的前提下提升意象与节奏。", true),
            ),
        )
    }

    suspend fun listStyles(): List<StylePreset> = styleDao.getAll().map(::styleToModel)

    suspend fun saveCustomStyle(name: String, prompt: String) {
        styleDao.insert(
            StylePresetEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                prompt = prompt,
                isBuiltin = false,
            ),
        )
    }

    suspend fun createEntry(
        title: String,
        format: EntryFormat,
        initialContent: String = "",
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val filePath = fileStore.createEntry(id, format, initialContent, aiSettingsRepository.loadStorageSettings())
        entryDao.insert(
            EntryEntity(
                id = id,
                title = title.ifBlank { "未命名记录" },
                format = format.name,
                filePath = filePath,
                tagsJson = "[]",
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun loadDocument(entryId: String): EntryDocument {
        val meta = entryDao.getById(entryId)?.let(::entryToModel) ?: error("Entry not found.")
        val content = fileStore.readContent(meta.filePath)
        return EntryDocument(meta = meta, content = content)
    }

    suspend fun saveEntry(
        entryId: String,
        title: String,
        content: String,
        tags: List<String>,
    ) {
        val current = entryDao.getById(entryId) ?: error("Entry not found.")
        fileStore.overwrite(current.filePath, content)
        entryDao.update(
            current.copy(
                title = title,
                tagsJson = json.encodeToString(tags),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun listVersions(entryId: String): List<VersionSnapshot> = versionDao.listForEntry(entryId).map(::versionToModel)

    suspend fun latestAnalysis(entryId: String): EmotionAnalysis? = emotionDao.latestForEntry(entryId)?.let(::analysisToModel)

    suspend fun listAnalyses(entryId: String): List<EmotionAnalysis> = emotionDao.listForEntry(entryId).map(::analysisToModel)

    suspend fun getAiConfig() = aiSettingsRepository.load()

    suspend fun getStorageSettings() = aiSettingsRepository.loadStorageSettings()

    suspend fun saveAiConfig(config: AiEndpointConfig) {
        aiSettingsRepository.save(config)
    }

    suspend fun saveStorageSettings(mode: StorageMode, treeUri: String?) {
        aiSettingsRepository.saveStorageSettings(
            AppStorageSettings(
                mode = mode,
                treeUri = treeUri,
            ),
        )
    }

    suspend fun saveSystemFolder(treeUri: String) {
        aiSettingsRepository.saveStorageSettings(
            getStorageSettings().copy(
                mode = StorageMode.SYSTEM_FOLDER,
                treeUri = treeUri,
            ),
        )
    }

    suspend fun testAiConnection(): String {
        val config = aiSettingsRepository.load()
        return llmProvider.testConnection(config)
    }

    suspend fun testImageConnection(): String {
        val config = aiSettingsRepository.load()
        return llmProvider.testImageConnection(config)
    }

    suspend fun createEmbeddedImageSnippet(uri: Uri, format: EntryFormat): EmbeddedImageInsertResult =
        embeddedImageService.createSnippet(uri, format)

    suspend fun deleteEntry(entryId: String) {
        val entry = entryDao.getById(entryId) ?: error("Entry not found.")
        val storageSettings = aiSettingsRepository.loadStorageSettings()
        fileStore.deleteEntryFiles(
            entryId = entryId,
            filePath = entry.filePath,
            storageSettings = storageSettings,
        )
        versionDao.deleteByEntryId(entryId)
        emotionDao.deleteByEntryId(entryId)
        chatDao.deleteByEntryId(entryId)
        eventDao.deleteByEntryId(entryId)
        runDao.deleteByEntryId(entryId)
        entryDao.deleteById(entryId)
    }

    suspend fun reviewEntry(entryId: String): ReviewResult {
        val config = aiSettingsRepository.load()
        val document = loadDocument(entryId)
        val prepared = EmbeddedImageParser.sanitizeForLlm(document.content)
        val result = llmProvider.review(
            config = config,
            content = prepared.content,
            targetFormat = document.meta.format,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        return result.copy(
            candidateContent = EmbeddedImageParser.restorePlaceholders(result.candidateContent, prepared),
        )
    }

    suspend fun reviewContent(
        content: String,
        targetFormat: EntryFormat,
    ): ReviewResult {
        val config = aiSettingsRepository.load()
        val prepared = EmbeddedImageParser.sanitizeForLlm(content)
        val result = llmProvider.review(
            config = config,
            content = prepared.content,
            targetFormat = targetFormat,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        return result.copy(
            candidateContent = EmbeddedImageParser.restorePlaceholders(result.candidateContent, prepared),
        )
    }

    suspend fun polishEntry(entryId: String, preset: StylePreset): PolishCandidate {
        val config = aiSettingsRepository.load()
        val document = loadDocument(entryId)
        val prepared = EmbeddedImageParser.sanitizeForLlm(document.content)
        val result = llmProvider.polish(
            config = config,
            content = prepared.content,
            format = document.meta.format,
            preset = preset,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        return result.copy(
            content = EmbeddedImageParser.restorePlaceholders(result.content, prepared),
        )
    }

    suspend fun polishContent(
        content: String,
        format: EntryFormat,
        preset: StylePreset,
    ): PolishCandidate {
        val config = aiSettingsRepository.load()
        val prepared = EmbeddedImageParser.sanitizeForLlm(content)
        val result = llmProvider.polish(
            config = config,
            content = prepared.content,
            format = format,
            preset = preset,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        return result.copy(
            content = EmbeddedImageParser.restorePlaceholders(result.content, prepared),
        )
    }

    suspend fun analyzeEntry(entryId: String): EmotionAnalysis {
        val config = aiSettingsRepository.load()
        val document = loadDocument(entryId)
        val prepared = EmbeddedImageParser.sanitizeForLlm(document.content)
        val result = llmProvider.analyzePsychology(
            config = config,
            content = prepared.content,
            format = document.meta.format,
            imageDataUrls = prepared.imageDataUrls,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        val entity = result.toEntity(entryId)
        emotionDao.insert(entity)
        return analysisToModel(entity)
    }

    suspend fun analyzeContent(
        entryId: String,
        content: String,
        format: EntryFormat,
    ): EmotionAnalysis {
        val config = aiSettingsRepository.load()
        val prepared = EmbeddedImageParser.sanitizeForLlm(content)
        val result = llmProvider.analyzePsychology(
            config = config,
            content = prepared.content,
            format = format,
            imageDataUrls = prepared.imageDataUrls,
            embeddedImagePlaceholders = prepared.placeholders,
        )
        val entity = result.toEntity(entryId)
        emotionDao.insert(entity)
        return analysisToModel(entity)
    }

    suspend fun applyCandidate(
        entryId: String,
        content: String,
        source: String,
        targetFormat: EntryFormat? = null,
    ) {
        val entry = entryDao.getById(entryId) ?: error("Entry not found.")
        val format = EntryFormat.valueOf(entry.format)
        val resolvedTargetFormat = targetFormat ?: format
        val previousContent = fileStore.readContent(entry.filePath)
        val versionPath = fileStore.saveVersion(entryId, format, previousContent, source)
        versionDao.insert(
            VersionSnapshotEntity(
                versionId = UUID.randomUUID().toString(),
                entryId = entryId,
                source = source,
                format = format.name,
                filePath = versionPath,
                createdAt = System.currentTimeMillis(),
            ),
        )
        val updatedPath = fileStore.overwriteEntry(
            entryId = entryId,
            currentPath = entry.filePath,
            currentFormat = format,
            targetFormat = resolvedTargetFormat,
            content = content,
            storageSettings = aiSettingsRepository.loadStorageSettings(),
        )
        entryDao.update(
            entry.copy(
                format = resolvedTargetFormat.name,
                filePath = updatedPath,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun generateReport(period: ReportPeriod): MoodReport {
        val now = LocalDate.now()
        val (start, end) = period.range(now)
        val analyses = emotionDao.listBetween(start, end).map(::analysisToModel)
        val baseReport = moodReportGenerator.generate(period, start, end, analyses)
        val config = aiSettingsRepository.load()
        val summaries = analyses.map { "${it.labels.joinToString()} | ${it.summary}" }
        val enhanced: PeriodicReportResult? = if (config.isConfigured && summaries.isNotEmpty()) {
            runCatching { llmProvider.summarizePeriod(config, period, summaries) }.getOrNull()
        } else {
            null
        }

        val report = baseReport.copy(
            dominantMoods = enhanced?.dominantMoods ?: baseReport.dominantMoods,
            summary = enhanced?.summary ?: baseReport.summary,
            advice = enhanced?.advice ?: baseReport.advice,
        )
        moodReportDao.insert(report.toEntity())
        return report
    }

    suspend fun deleteReport(reportId: String) {
        moodReportDao.deleteById(reportId)
    }

    suspend fun deleteLegacyDailyReports() {
        moodReportDao.deleteByPeriod("DAY")
    }

    suspend fun exportBundle(resolver: ContentResolver, uri: Uri) {
        transferManager.exportBundle(
            resolver = resolver,
            uri = uri,
            entries = entryDao.getAll(),
            styles = styleDao.getAll(),
            analyses = emotionDao.getAll(),
            reports = moodReportDao.getAll(),
            versions = versionDao.getAll(),
            psychologyChats = chatDao.getAll(),
            psychologyRuns = runDao.getAll(),
            psychologyEvents = eventDao.getAll(),
            userProfiles = profileDao.getAll(),
        )
    }

    suspend fun importBundle(resolver: ContentResolver, uri: Uri) {
        val imported = transferManager.importBundle(resolver, uri)
        val storageSettings = aiSettingsRepository.loadStorageSettings()
        val idMapping = mutableMapOf<String, String>()
        imported.manifest.entries.forEach { bundled ->
            val bytes = imported.fileBytes[bundled.relativePath] ?: return@forEach
            val entryId = UUID.randomUUID().toString()
            idMapping[bundled.id] = entryId
            val (format, filePath) = fileStore.importContent(entryId, bundled.relativePath, bytes, storageSettings)
            entryDao.insert(
                EntryEntity(
                    id = entryId,
                    title = bundled.title,
                    format = format.name,
                    filePath = filePath,
                    tagsJson = bundled.tagsJson,
                    createdAt = bundled.createdAt,
                    updatedAt = bundled.updatedAt,
                ),
            )
        }
        styleDao.insertAll(imported.manifest.styles)
        imported.manifest.analyses.forEach { entity ->
            emotionDao.insert(
                entity.copy(
                    id = UUID.randomUUID().toString(),
                    entryId = idMapping[entity.entryId] ?: entity.entryId,
                ),
            )
        }
        imported.manifest.versions.forEach { version ->
            val remappedEntryId = idMapping[version.entryId] ?: return@forEach
            val bytes = imported.fileBytes[version.relativePath] ?: return@forEach
            val filePath = fileStore.importVersion(remappedEntryId, version.relativePath, bytes)
            versionDao.insert(
                VersionSnapshotEntity(
                    versionId = UUID.randomUUID().toString(),
                    entryId = remappedEntryId,
                    source = version.source,
                    format = version.format,
                    filePath = filePath,
                    createdAt = version.createdAt,
                ),
            )
        }
        imported.manifest.reports
            .filterNot { it.period == "DAY" }
            .forEach { report ->
                moodReportDao.insert(report)
            }
        imported.manifest.psychologyChats.forEach { chat ->
            chatDao.insert(
                chat.copy(
                    id = UUID.randomUUID().toString(),
                    entryId = idMapping[chat.entryId] ?: chat.entryId,
                ),
            )
        }
        val runIdMapping = mutableMapOf<String, String>()
        imported.manifest.psychologyRuns.forEach { run ->
            val newRunId = UUID.randomUUID().toString()
            runIdMapping[run.id] = newRunId
            runDao.insert(
                run.copy(
                    id = newRunId,
                    entryId = idMapping[run.entryId] ?: run.entryId,
                ),
            )
        }
        imported.manifest.psychologyEvents.forEach { event ->
            eventDao.insert(
                event.copy(
                    id = UUID.randomUUID().toString(),
                    runId = runIdMapping[event.runId] ?: event.runId,
                    entryId = idMapping[event.entryId] ?: event.entryId,
                ),
            )
        }
        imported.manifest.userProfiles.forEach { profile ->
            profileDao.insert(profile)
        }
    }

    suspend fun exportRaw(resolver: ContentResolver, folderUri: Uri) {
        transferManager.exportRawEntries(resolver, folderUri, entryDao.getAll())
    }

    suspend fun importRaw(resolver: ContentResolver, folderUri: Uri) {
        val rawFiles = transferManager.importRawEntries(resolver, folderUri)
        val storageSettings = aiSettingsRepository.loadStorageSettings()
        rawFiles.forEach { file ->
            val entryId = UUID.randomUUID().toString()
            val (format, filePath) = fileStore.importContent(entryId, file.name, file.bytes, storageSettings)
            val now = System.currentTimeMillis()
            entryDao.insert(
                EntryEntity(
                    id = entryId,
                    title = file.name.substringBeforeLast("."),
                    format = format.name,
                    filePath = filePath,
                    tagsJson = "[]",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    private fun entryToModel(entity: EntryEntity): EntryMeta = EntryMeta(
        id = entity.id,
        title = entity.title,
        format = EntryFormat.valueOf(entity.format),
        filePath = entity.filePath,
        tags = json.decodeFromString(entity.tagsJson),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    private fun styleToModel(entity: StylePresetEntity): StylePreset = StylePreset(
        id = entity.id,
        name = entity.name,
        prompt = entity.prompt,
        isBuiltin = entity.isBuiltin,
    )

    private fun analysisToModel(entity: EmotionAnalysisEntity): EmotionAnalysis = EmotionAnalysis(
        id = entity.id,
        entryId = entity.entryId,
        labels = json.decodeFromString(entity.labelsJson),
        intensity = entity.intensity,
        summary = entity.summary,
        suggestions = json.decodeFromString(entity.suggestionsJson),
        safetyFlag = entity.safetyFlag,
        createdAt = entity.createdAt,
        triggers = json.decodeFromString(entity.triggersJson),
        cognitivePatterns = json.decodeFromString(entity.cognitivePatternsJson),
        needs = json.decodeFromString(entity.needsJson),
        relationshipSignals = json.decodeFromString(entity.relationshipSignalsJson),
        defenseMechanisms = json.decodeFromString(entity.defenseMechanismsJson),
        strengths = json.decodeFromString(entity.strengthsJson),
        bodyStressSignals = json.decodeFromString(entity.bodyStressSignalsJson),
        riskNotes = json.decodeFromString(entity.riskNotesJson),
    )

    private fun chatMessageToModel(entity: PsychologyChatMessageEntity): PsychologyChatMessage = PsychologyChatMessage(
        id = entity.id,
        entryId = entity.entryId,
        role = runCatching { PsychologyChatRole.valueOf(entity.role) }.getOrDefault(PsychologyChatRole.ASSISTANT),
        content = entity.content,
        createdAt = entity.createdAt,
    )

    private fun processEventToModel(entity: PsychologyAgentProcessEventEntity): PsychologyAgentProcessEvent = PsychologyAgentProcessEvent(
        id = entity.id,
        runId = entity.runId,
        entryId = entity.entryId,
        agentId = entity.agentId,
        agentName = entity.agentName,
        phase = runCatching { PsychologyAgentPhase.valueOf(entity.phase) }.getOrDefault(PsychologyAgentPhase.ROUND_ONE),
        title = entity.title,
        contentMarkdown = entity.contentMarkdown,
        sequence = entity.sequence,
        createdAt = entity.createdAt,
    )

    private fun PsychologyAgentProcessEvent.toEntity(): PsychologyAgentProcessEventEntity = PsychologyAgentProcessEventEntity(
        id = id,
        runId = runId,
        entryId = entryId,
        agentId = agentId,
        agentName = agentName,
        phase = phase.name,
        title = title,
        contentMarkdown = contentMarkdown,
        sequence = sequence,
        createdAt = createdAt,
    )

    private fun profileToModel(entity: UserPsychologyProfileEntity): UserPsychologyProfile = UserPsychologyProfile(
        triggers = json.decodeFromString(entity.triggersJson),
        cognitivePatterns = json.decodeFromString(entity.cognitivePatternsJson),
        needs = json.decodeFromString(entity.needsJson),
        relationshipPatterns = json.decodeFromString(entity.relationshipPatternsJson),
        defensePatterns = json.decodeFromString(entity.defensePatternsJson),
        bodyStressSignals = json.decodeFromString(entity.bodyStressSignalsJson),
        strengths = json.decodeFromString(entity.strengthsJson),
        riskNotes = json.decodeFromString(entity.riskNotesJson),
        updatedAt = entity.updatedAt,
        userEditedAt = entity.userEditedAt,
    )

    private fun UserPsychologyProfile.toEntity(): UserPsychologyProfileEntity = UserPsychologyProfileEntity(
        triggersJson = json.encodeToString(triggers),
        cognitivePatternsJson = json.encodeToString(cognitivePatterns),
        needsJson = json.encodeToString(needs),
        relationshipPatternsJson = json.encodeToString(relationshipPatterns),
        defensePatternsJson = json.encodeToString(defensePatterns),
        bodyStressSignalsJson = json.encodeToString(bodyStressSignals),
        strengthsJson = json.encodeToString(strengths),
        riskNotesJson = json.encodeToString(riskNotes),
        updatedAt = updatedAt,
        userEditedAt = userEditedAt,
    )

    private fun reportToModel(entity: MoodReportEntity): MoodReport? {
        val period = entity.period.toReportPeriodOrNull() ?: return null
        return MoodReport(
            id = entity.id,
            period = period,
            rangeStart = entity.rangeStart,
            rangeEnd = entity.rangeEnd,
            dominantMoods = json.decodeFromString(entity.dominantMoodsJson),
            averageIntensity = entity.averageIntensity,
            summary = entity.summary,
            advice = json.decodeFromString(entity.adviceJson),
            createdAt = entity.createdAt,
        )
    }

    private fun versionToModel(entity: VersionSnapshotEntity): VersionSnapshot = VersionSnapshot(
        versionId = entity.versionId,
        entryId = entity.entryId,
        source = entity.source,
        format = EntryFormat.valueOf(entity.format),
        filePath = entity.filePath,
        createdAt = entity.createdAt,
    )

    private fun PsychologyAnalysisResult.toEntity(entryId: String): EmotionAnalysisEntity = EmotionAnalysisEntity(
        id = UUID.randomUUID().toString(),
        entryId = entryId,
        labelsJson = json.encodeToString(labels),
        intensity = intensity,
        summary = summary,
        suggestionsJson = json.encodeToString(suggestions),
        safetyFlag = safetyFlag,
        createdAt = System.currentTimeMillis(),
        triggersJson = json.encodeToString(triggers),
        cognitivePatternsJson = json.encodeToString(cognitivePatterns),
        needsJson = json.encodeToString(needs),
        relationshipSignalsJson = json.encodeToString(relationshipSignals),
        defenseMechanismsJson = json.encodeToString(defenseMechanisms),
        strengthsJson = json.encodeToString(strengths),
        bodyStressSignalsJson = json.encodeToString(bodyStressSignals),
        riskNotesJson = json.encodeToString(riskNotes),
    )

    private fun MoodReport.toEntity(): MoodReportEntity = MoodReportEntity(
        id = id,
        period = period.name,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        dominantMoodsJson = json.encodeToString(dominantMoods),
        averageIntensity = averageIntensity,
        summary = summary,
        adviceJson = json.encodeToString(advice),
        createdAt = createdAt,
    )
}

private fun ReportPeriod.range(now: LocalDate): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val startDate = when (this) {
        ReportPeriod.DAY -> now
        ReportPeriod.WEEK -> now.with(java.time.DayOfWeek.MONDAY)
        ReportPeriod.MONTH -> now.with(TemporalAdjusters.firstDayOfMonth())
    }
    val endDate = when (this) {
        ReportPeriod.DAY -> startDate.plusDays(1)
        ReportPeriod.WEEK -> startDate.plusWeeks(1)
        ReportPeriod.MONTH -> startDate.plusMonths(1)
    }
    val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = endDate.atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}

private fun String.toReportPeriodOrNull(): ReportPeriod? = when (this) {
    ReportPeriod.DAY.name -> ReportPeriod.DAY
    ReportPeriod.WEEK.name -> ReportPeriod.WEEK
    ReportPeriod.MONTH.name -> ReportPeriod.MONTH
    else -> null
}
