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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.EmotionCenterItem
import com.localdiary.app.ui.components.OverviewHeroCard
import com.localdiary.app.ui.components.OverviewHeroChip
import com.localdiary.app.ui.viewmodel.EmotionCenterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EmotionCenterScreen(
    viewModel: EmotionCenterViewModel,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenEmotionDetail: (String) -> Unit,
    onOpenPsychologyChat: (String) -> Unit,
    onOpenReports: () -> Unit,
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
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewHeroCard(
            title = "心理洞察",
            subtitle = "这里统一查看单篇心理分析，并进入周报或月报。",
            actions = {
                TextButton(onClick = onOpenReports) {
                    Text("周期报告")
                }
                IconButton(onClick = viewModel::toggleSearch) {
                    Text(if (state.isSearchExpanded) "✕" else "⌕", style = MaterialTheme.typography.titleLarge)
                }
            },
            stats = {
                OverviewHeroChip("文章 ${state.items.size}")
            },
            expandedContent = if (state.isSearchExpanded) {
                {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::updateQuery,
                        label = { Text("搜索标题或日期") },
                        placeholder = { Text("例如 2026-03-21 / 春天") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            } else {
                null
            },
        )

        state.error?.let {
            EmotionStatusBanner(
                title = "心理分析失败",
                detail = it,
                isError = true,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("entry-section-title") {
                Text("单篇心理分析", style = MaterialTheme.typography.titleMedium)
            }

            if (state.items.isEmpty() && !state.loading) {
                item("empty-entry-state") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text("没有符合条件的心理分析。", modifier = Modifier.padding(16.dp))
                    }
                }
            }

            items(state.items, key = { it.meta.id }) { item ->
                EmotionEntryCard(
                    item = item,
                    working = state.workingEntryId == item.meta.id,
                    onAnalyze = { viewModel.analyzeEntry(item.meta.id) },
                    onOpenEntry = { onOpenEntry(item.meta.id) },
                    onEditEntry = { onEditEntry(item.meta.id) },
                    onOpenEmotionDetail = { onOpenEmotionDetail(item.meta.id) },
                    onOpenPsychologyChat = { onOpenPsychologyChat(item.meta.id) },
                )
            }
        }
    }
}

@Composable
private fun EmotionEntryCard(
    item: EmotionCenterItem,
    working: Boolean,
    onAnalyze: () -> Unit,
    onOpenEntry: () -> Unit,
    onEditEntry: () -> Unit,
    onOpenEmotionDetail: () -> Unit,
    onOpenPsychologyChat: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.meta.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "更新于 ${formatTimestamp(item.meta.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
            val analysis = item.latestAnalysis
            if (analysis == null) {
                Text("还没有心理分析记录。")
            } else {
                Text(
                    "最近状态: ${analysis.labels.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    analysis.summary,
                    maxLines = 2,
                )
                Text(
                    "分析于 ${formatTimestamp(analysis.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAnalyze, enabled = !working) {
                    Text(if (working) "分析中..." else "重新分析")
                }
                TextButton(onClick = onOpenEmotionDetail) {
                    Text("查看分析")
                }
                TextButton(onClick = onOpenPsychologyChat) {
                    Text("对话")
                }
                TextButton(onClick = onOpenEntry) {
                    Text("查看文章")
                }
                TextButton(onClick = onEditEntry) {
                    Text("编辑")
                }
            }
        }
    }
}

@Composable
private fun EmotionStatusBanner(
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

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
