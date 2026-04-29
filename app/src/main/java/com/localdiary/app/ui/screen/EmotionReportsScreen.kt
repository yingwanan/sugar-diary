package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.MoodReport
import com.localdiary.app.ui.components.MarkdownText
import com.localdiary.app.model.ReportPeriod
import com.localdiary.app.model.label
import com.localdiary.app.ui.designsystem.organism.AppTopBar
import com.localdiary.app.ui.viewmodel.EmotionReportsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionReportsScreen(
    viewModel: EmotionReportsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        AppTopBar(
            title = "周期报告",
            onNavigateBack = onNavigateBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { error ->
                item("report-error") {
                    ReportStatusBanner(
                        title = "周期报告失败",
                        detail = error,
                        isError = true,
                    )
                }
            }

            item("report-generator") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("生成周期报告", style = MaterialTheme.typography.titleLarge)
                        Text("这里只保留周报和月报。你可以随时重新生成，并删除不需要的历史报告。")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.generateReport(ReportPeriod.WEEK) },
                                enabled = state.generatingPeriod == null,
                            ) {
                                Text(if (state.generatingPeriod == ReportPeriod.WEEK) "生成中..." else "生成周报")
                            }
                            Button(
                                onClick = { viewModel.generateReport(ReportPeriod.MONTH) },
                                enabled = state.generatingPeriod == null,
                            ) {
                                Text(if (state.generatingPeriod == ReportPeriod.MONTH) "生成中..." else "生成月报")
                            }
                        }
                    }
                }
            }

            item("report-list-title") {
                Text("历史报告", style = MaterialTheme.typography.titleMedium)
            }

            if (state.reports.isEmpty() && !state.loading) {
                item("report-empty") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("还没有生成任何周报或月报。", modifier = Modifier.padding(16.dp))
                    }
                }
            }

            items(state.reports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    deleting = state.deletingReportId == report.id,
                    onDelete = { viewModel.deleteReport(report.id) },
                )
            }
        }
    }
}

@Composable
private fun ReportStatusBanner(
    title: String,
    detail: String,
    isError: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                detail,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: MoodReport,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(report.period.label, style = MaterialTheme.typography.titleMedium)
            MarkdownText(report.summary)
            Text("常见心情: ${report.dominantMoods.joinToString()}")
            report.advice.forEach { advice ->
                MarkdownText("• $advice")
            }
            Text(
                "生成于 ${formatReportTimestamp(report.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = onDelete,
                enabled = !deleting,
            ) {
                Text(if (deleting) "删除中..." else "删除")
            }
        }
    }
}

private fun formatReportTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
