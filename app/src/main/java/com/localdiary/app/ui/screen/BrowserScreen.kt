package com.localdiary.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.BrowserCategory
import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.ui.designsystem.molecule.AppChip
import com.localdiary.app.ui.designsystem.molecule.AppEmptyState
import com.localdiary.app.ui.designsystem.molecule.AppIconButton
import com.localdiary.app.ui.designsystem.molecule.AppListItem
import com.localdiary.app.ui.designsystem.organism.AppDialog
import com.localdiary.app.ui.designsystem.organism.AppSearchBar
import com.localdiary.app.ui.designsystem.template.AppScreenScaffold
import com.localdiary.app.ui.designsystem.token.DiarySpacing
import com.localdiary.app.ui.viewmodel.BrowserViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenEntry: (String) -> Unit,
) {
    val state = viewModel.uiState
    val pendingDeleteItem = state.pendingDeleteItemId?.let { id ->
        state.items.firstOrNull { it.meta.id == id }
    }

    if (state.pendingDeleteItemId != null) {
        AppDialog(
            title = "删除这篇文章？",
            text = pendingDeleteItem?.let {
                "《${it.meta.title}》的正文、版本快照和心理分析记录都会被删除。"
            } ?: "这篇文章的正文、版本快照和心理分析记录都会被删除。",
            confirmText = "彻底删除",
            onConfirm = viewModel::confirmPendingDelete,
            dismissText = "取消",
            onDismiss = viewModel::dismissDelete,
            onDismissRequest = viewModel::dismissDelete,
        )
    }

    AppScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(it)
                .padding(horizontal = DiarySpacing.space4),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.space4),
        ) {
            BrowserHeader(
                resultCount = state.items.size,
                tagCount = state.availableTags.size,
                moodCount = state.availableMoods.size,
                query = state.query,
                onQueryChange = viewModel::updateQuery,
                expanded = state.isSearchExpanded,
                onToggleExpand = viewModel::toggleSearch,
            )

            ScrollableTabRow(
                selectedTabIndex = state.category.ordinal,
                edgePadding = 0.dp,
            ) {
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
                BrowserCategory.TAG -> FilterFlowRow {
                    state.availableTags.forEach { tag ->
                        AppChip(
                            label = tag,
                            selected = state.selectedTag == tag,
                            onClick = { viewModel.toggleTag(tag) },
                        )
                    }
                }
                BrowserCategory.TIME -> FilterFlowRow {
                    BrowserTimeBucket.entries.forEach { bucket ->
                        AppChip(
                            label = bucket.label,
                            selected = state.selectedTimeBucket == bucket,
                            onClick = { viewModel.toggleTimeBucket(bucket) },
                        )
                    }
                }
                BrowserCategory.MOOD -> FilterFlowRow {
                    state.availableMoods.forEach { mood ->
                        AppChip(
                            label = mood,
                            selected = state.selectedMood == mood,
                            onClick = { viewModel.toggleMood(mood) },
                        )
                    }
                }
            }

            if (state.items.isEmpty() && !state.loading) {
                AppEmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "没有符合条件的文章",
                    description = "换个关键词或清除筛选条件再试试",
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
                ) {
                    items(
                        items = state.items,
                        key = { it.meta.id },
                        contentType = { "entry" },
                    ) { item ->
                        BrowserEntryListItem(
                            item = item,
                            onOpenEntry = onOpenEntry,
                            onDelete = { viewModel.requestDelete(item.meta.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserHeader(
    resultCount: Int,
    tagCount: Int,
    moodCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DiarySpacing.space5),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "浏览",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "结果 $resultCount · 标签 $tagCount · 心情 $moodCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "搜索标题或日期",
            expanded = expanded,
            onToggleExpand = onToggleExpand,
            modifier = if (expanded) Modifier.weight(2f) else Modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterFlowRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
    ) {
        content()
    }
}

@Composable
private fun BrowserEntryListItem(
    item: EntryBrowserItem,
    onOpenEntry: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AppListItem(
            headline = item.meta.title,
            supporting = "更新于 " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.meta.updatedAt)),
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space1)) {
                    AppChip(label = item.meta.format.label)
                    AppIconButton(
                        onClick = onDelete,
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除《${item.meta.title}》",
                    )
                }
            },
            onClick = { onOpenEntry(item.meta.id) },
        )
        if (item.meta.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = DiarySpacing.space4),
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
            ) {
                item.meta.tags.take(3).forEach { tag ->
                    AppChip(label = tag)
                }
            }
        }
        Text(
            text = item.previewText,
            modifier = Modifier.padding(
                start = DiarySpacing.space4,
                end = DiarySpacing.space4,
                bottom = DiarySpacing.space4,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
