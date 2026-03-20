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
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.ui.viewmodel.EmotionDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionDetailScreen(
    viewModel: EmotionDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
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
        TopAppBar(
            title = { Text(state.document?.meta?.title ?: "情绪分析") },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
            },
        )

        when {
            state.loading -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("正在加载情绪分析...")
                }
            }

            state.error != null -> {
                Card(modifier = Modifier.padding(16.dp)) {
                    Text(state.error, modifier = Modifier.padding(16.dp))
                }
            }

            state.document != null -> {
                val document = state.document
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item("emotion-detail-header") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(document.meta.title, style = MaterialTheme.typography.headlineSmall)
                                Text("更新于 ${formatEmotionTimestamp(document.meta.updatedAt)}")
                                state.latestAnalysis?.let { analysis ->
                                    Text(
                                        "最近分析于 ${formatEmotionTimestamp(analysis.createdAt)}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = viewModel::analyzeEntry,
                                        enabled = !state.working,
                                    ) {
                                        Text(if (state.working) "分析中..." else "重新分析")
                                    }
                                    TextButton(onClick = { onOpenEntry(document.meta.id) }) {
                                        Text("查看文章")
                                    }
                                    TextButton(onClick = { onEditEntry(document.meta.id) }) {
                                        Text("编辑")
                                    }
                                }
                            }
                        }
                    }

                    item("emotion-detail-latest-title") {
                        Text("最新分析", style = MaterialTheme.typography.titleMedium)
                    }

                    item("emotion-detail-latest-card") {
                        state.latestAnalysis?.let { analysis ->
                            EmotionAnalysisCard(
                                analysis = analysis,
                                title = "当前解读",
                            )
                        } ?: Card(modifier = Modifier.fillMaxWidth()) {
                            Text("这篇文章还没有情绪分析记录。", modifier = Modifier.padding(16.dp))
                        }
                    }

                    if (state.history.isNotEmpty()) {
                        item("emotion-detail-history-title") {
                            Text("历史分析", style = MaterialTheme.typography.titleMedium)
                        }
                        items(state.history.drop(1), key = { it.id }) { analysis ->
                            EmotionAnalysisCard(
                                analysis = analysis,
                                title = formatEmotionTimestamp(analysis.createdAt),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionAnalysisCard(
    analysis: EmotionAnalysis,
    title: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("情绪标签: ${analysis.labels.joinToString()}")
            Text("强度: ${analysis.intensity}/100")
            Text(analysis.summary)
            analysis.suggestions.forEach { suggestion ->
                Text("• $suggestion")
            }
            if (analysis.safetyFlag) {
                Text("检测到高风险内容，请优先联系现实中的可信支持或专业帮助。")
            }
            Text(
                "分析于 ${formatEmotionTimestamp(analysis.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatEmotionTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
