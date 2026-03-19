package com.localdiary.app.model

import kotlinx.serialization.Serializable

data class ReviewResult(
    val issues: List<String>,
    val suggestedTitle: String?,
    val candidateContent: String,
    val format: EntryFormat,
)

data class PolishCandidate(
    val styleName: String,
    val rationale: String,
    val content: String,
    val format: EntryFormat,
)

@Serializable
data class PsychologyAnalysisResult(
    val labels: List<String>,
    val intensity: Int,
    val summary: String,
    val suggestions: List<String>,
    val safetyFlag: Boolean,
)

@Serializable
data class PeriodicReportResult(
    val dominantMoods: List<String>,
    val summary: String,
    val advice: List<String>,
)
