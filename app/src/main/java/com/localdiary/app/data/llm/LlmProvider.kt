package com.localdiary.app.data.llm

import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.PeriodicReportResult
import com.localdiary.app.model.PolishCandidate
import com.localdiary.app.model.PsychologyAnalysisResult
import com.localdiary.app.model.ReportPeriod
import com.localdiary.app.model.ReviewResult
import com.localdiary.app.model.StylePreset

interface LlmProvider {
    suspend fun testConnection(config: AiEndpointConfig): String

    suspend fun testImageConnection(config: AiEndpointConfig): String

    suspend fun review(
        config: AiEndpointConfig,
        content: String,
        targetFormat: EntryFormat,
        embeddedImagePlaceholders: List<String>,
    ): ReviewResult

    suspend fun polish(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
        preset: StylePreset,
        embeddedImagePlaceholders: List<String>,
    ): PolishCandidate

    suspend fun analyzePsychology(
        config: AiEndpointConfig,
        content: String,
        format: EntryFormat,
        imageDataUrls: List<String>,
        embeddedImagePlaceholders: List<String>,
    ): PsychologyAnalysisResult

    suspend fun summarizePeriod(
        config: AiEndpointConfig,
        period: ReportPeriod,
        summaries: List<String>,
    ): PeriodicReportResult
}
