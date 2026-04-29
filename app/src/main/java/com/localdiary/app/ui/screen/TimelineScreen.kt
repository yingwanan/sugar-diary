package com.localdiary.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localdiary.app.model.EntryMeta
import com.localdiary.app.ui.designsystem.molecule.AppChip
import com.localdiary.app.ui.designsystem.molecule.AppEmptyState
import com.localdiary.app.ui.designsystem.molecule.AppListItem
import com.localdiary.app.ui.designsystem.organism.AppFAB
import com.localdiary.app.ui.designsystem.template.AppScreenScaffold
import com.localdiary.app.ui.designsystem.token.DiarySpacing
import com.localdiary.app.ui.viewmodel.TimelineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onOpenEntry: (String) -> Unit,
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val createEntry = {
        viewModel.createEntry(title = "", onCreated = onOpenEntry)
    }

    AppScreenScaffold(
        floatingActionButton = {
            AppFAB(
                onClick = createEntry,
                icon = Icons.Filled.Edit,
                contentDescription = "新建日记",
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(it)
                .padding(horizontal = DiarySpacing.space4),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.space4),
        ) {
            Column(
                modifier = Modifier.padding(top = DiarySpacing.space5),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.space1),
            ) {
                Text(
                    text = "时间轴",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "共 ${entries.size} 篇日记",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AppEmptyState(
                        icon = Icons.Filled.Book,
                        title = "还没有日记",
                        description = "点击下方按钮开始记录",
                        actionLabel = "写日记",
                        onAction = createEntry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
                    modifier = Modifier.weight(1f),
                ) {
                    items(entries, key = { it.id }, contentType = { "timeline-entry" }) { entry ->
                        TimelineEntryListItem(
                            entry = entry,
                            onClick = { onOpenEntry(entry.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEntryListItem(
    entry: EntryMeta,
    onClick: () -> Unit,
) {
    AppListItem(
        headline = entry.title,
        supporting = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.updatedAt)),
        leading = {
            EntryInitial(title = entry.title)
        },
        trailing = {
            AppChip(label = entry.format.label)
        },
        onClick = onClick,
    )
    if (entry.tags.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(start = 72.dp, end = DiarySpacing.space4, bottom = DiarySpacing.space2),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space2),
        ) {
            entry.tags.take(3).forEach { tag ->
                AppChip(label = tag)
            }
        }
    }
}

@Composable
private fun EntryInitial(title: String) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title.firstOrNull()?.toString() ?: "日",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
