package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.localdiary.app.model.BrowserCategory
import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.ui.components.OverviewHeroCard
import com.localdiary.app.ui.components.OverviewHeroChip
import com.localdiary.app.ui.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenEntry: (String) -> Unit,
) {
    val state = viewModel.uiState
    var pendingDelete by remember { mutableStateOf<EntryBrowserItem?>(null) }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这篇文章？") },
            text = { Text("《${item.meta.title}》的正文、版本快照和心理分析记录都会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(item.meta.id)
                    pendingDelete = null
                }) {
                    Text("彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
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
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewHeroCard(
            title = "文章浏览",
            subtitle = "按标签、时间线、心情筛选，或直接搜索标题和日期。",
            actions = {
                IconButton(onClick = viewModel::toggleSearch) {
                    Text(if (state.isSearchExpanded) "✕" else "⌕", style = MaterialTheme.typography.titleLarge)
                }
            },
            stats = {
                OverviewHeroChip("结果 ${state.items.size}")
                OverviewHeroChip("标签 ${state.availableTags.size}")
                OverviewHeroChip("心情 ${state.availableMoods.size}")
            },
            expandedContent = if (state.isSearchExpanded) {
                {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::updateQuery,
                        label = { Text("搜索标题或日期") },
                        placeholder = { Text("例如 春天 / 2026-03-21 / 2026-03") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            } else {
                null
            },
        )

        ScrollableTabRow(selectedTabIndex = state.category.ordinal) {
            BrowserCategory.entries.forEach { category ->
                Tab(
                    selected = state.category == category,
                    onClick = { viewModel.selectCategory(category) },
                    text = { Text(category.label) },
                )
            }
        }

        when (state.category) {
            BrowserCategory.ALL -> Unit
            BrowserCategory.TAG -> FilterRow {
                state.availableTags.forEach { tag ->
                    FilterChip(
                        selected = state.selectedTag == tag,
                        onClick = { viewModel.toggleTag(tag) },
                        label = { Text(tag) },
                    )
                }
            }
            BrowserCategory.TIME -> FilterRow {
                BrowserTimeBucket.entries.forEach { bucket ->
                    FilterChip(
                        selected = state.selectedTimeBucket == bucket,
                        onClick = { viewModel.toggleTimeBucket(bucket) },
                        label = { Text(bucket.label) },
                    )
                }
            }
            BrowserCategory.MOOD -> FilterRow {
                state.availableMoods.forEach { mood ->
                    FilterChip(
                        selected = state.selectedMood == mood,
                        onClick = { viewModel.toggleMood(mood) },
                        label = { Text(mood) },
                    )
                }
            }
        }

        if (state.items.isEmpty() && !state.loading) {
            BrowserHintCard("没有符合条件的文章。")
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = state.items,
                key = { it.meta.id },
                contentType = { "entry" },
            ) { item ->
                BrowserEntryCard(
                    item = item,
                    onOpenEntry = onOpenEntry,
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }
}

@Composable
private fun BrowserHintCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun BrowserEntryCard(
    item: EntryBrowserItem,
    onOpenEntry: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                item.meta.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "更新于 " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.meta.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(item.meta.format.label) })
                item.meta.tags.take(3).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
            Text(
                text = item.previewText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenEntry(item.meta.id) }) {
                    Text("查看")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            }
        }
    }
}
