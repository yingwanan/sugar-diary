package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdiary.app.ui.components.ArticlePreview
import com.localdiary.app.ui.designsystem.molecule.AppLoadingState
import com.localdiary.app.ui.designsystem.organism.AppTopBar
import com.localdiary.app.ui.viewmodel.ViewerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel,
    onNavigateBack: () -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenEmotionCenter: () -> Unit,
) {
    val state = viewModel.uiState
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这篇文章？") },
            text = { Text("正文、版本快照和心理分析记录都会一并删除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry(onNavigateBack)
                }) {
                    Text("彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
        )
    }

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
            title = state.document?.meta?.title ?: "查看文章",
            onNavigateBack = onNavigateBack,
            actions = {
                state.document?.let { document ->
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text("删除")
                    }
                    TextButton(onClick = { onEditEntry(document.meta.id) }) {
                        Text("编辑")
                    }
                }
            },
        )

        when {
            state.loading -> {
                AppLoadingState(message = "正在加载文章...", modifier = Modifier.fillMaxSize())
            }

            state.error != null -> {
                Card(modifier = Modifier.padding(16.dp)) {
                    Text(state.error, modifier = Modifier.padding(16.dp))
                }
            }

            state.document != null -> {
                val document = state.document
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
                            Text("创建于 ${formatTimestamp(document.meta.createdAt)}")
                            Text("更新于 ${formatTimestamp(document.meta.updatedAt)}")
                            if (document.meta.tags.isNotEmpty()) {
                                Text("标签: ${document.meta.tags.joinToString()}")
                            }
                            state.latestAnalysis?.let { analysis ->
                                Text(
                                    "最近状态: ${analysis.labels.joinToString()} · ${analysis.summary}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onEditEntry(document.meta.id) }) {
                                    Text("编辑此文章")
                                }
                                TextButton(onClick = onOpenEmotionCenter) {
                                    Text("心理洞察")
                                }
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("正文预览", style = MaterialTheme.typography.titleMedium)
                            ArticlePreview(
                                content = document.content,
                                format = document.meta.format,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
