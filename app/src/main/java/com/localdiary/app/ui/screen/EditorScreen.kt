package com.localdiary.app.ui.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.EditorDocumentBlock
import com.localdiary.app.model.EditorDocumentParser
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.PolishCandidate
import com.localdiary.app.model.ReviewResult
import com.localdiary.app.model.StylePreset
import com.localdiary.app.ui.components.ArticlePreview
import com.localdiary.app.ui.components.EmbeddedImagePreview
import com.localdiary.app.ui.viewmodel.EditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onOpenEmotionCenter: () -> Unit,
    onOpenEmotionDetail: () -> Unit,
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val resolver = context.contentResolver
    var tabIndex by remember { mutableStateOf(0) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var pendingStyle by remember { mutableStateOf<StylePreset?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val previewContent = remember(state.editorBlocks) {
        EditorDocumentParser.buildContent(state.editorBlocks)
    }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.insertImage(uri)
        }
    }

    BackHandler(enabled = state.dirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃未保存内容？") },
            text = { Text("你有尚未保存的修改，返回后这些修改会丢失。") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onNavigateBack()
                }) {
                    Text("放弃并返回")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("继续编辑")
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这篇文章？") },
            text = { Text("正文、版本快照和情绪分析记录都会一并删除，且无法恢复。") },
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

    if (confirmAction != null || pendingStyle != null) {
        AlertDialog(
            onDismissRequest = {
                confirmAction = null
                pendingStyle = null
            },
            title = { Text("确认发送到 LLM") },
            text = { Text("这会把当前文章内容发送到你配置的 OpenAI 兼容接口。") },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        pendingStyle != null -> viewModel.polish(pendingStyle!!)
                        confirmAction == "review" -> viewModel.review()
                        confirmAction == "analysis" -> viewModel.analyzePsychology()
                    }
                    confirmAction = null
                    pendingStyle = null
                }) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmAction = null
                    pendingStyle = null
                }) {
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
            )
            .imePadding(),
    ) {
        TopAppBar(
            title = { Text(if (state.title.isBlank()) "编辑文章" else state.title) },
            navigationIcon = {
                TextButton(
                    onClick = {
                        if (state.dirty) showDiscardDialog = true else onNavigateBack()
                    },
                ) {
                    Text("返回")
                }
            },
            actions = {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text("删除")
                }
                TextButton(onClick = viewModel::save) {
                    Text("保存")
                }
            },
        )

        ScrollableTabRow(selectedTabIndex = tabIndex) {
            listOf("编辑", "预览", "AI", "情绪").forEachIndexed { index, label ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(label) })
            }
        }

        when (tabIndex) {
            0 -> EditorTab(
                state = state,
                onTitleChange = viewModel::updateTitle,
                onTagsChange = viewModel::updateTags,
                onExpandMeta = viewModel::expandMeta,
                onInsertImage = { imagePickerLauncher.launch(arrayOf("image/*")) },
                onFocusTextBlock = { blockId, selection ->
                    viewModel.focusTextBlock(blockId, selection, collapseMeta = true)
                },
                onTextChange = viewModel::updateTextBlock,
                onDeleteImage = viewModel::removeImageBlock,
            )

            1 -> ArticlePreview(content = previewContent, format = state.format, modifier = Modifier.fillMaxSize())

            2 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EntryStatusBanner(
                    title = "AI 工具",
                    detail = "文风润色只改表达，不改格式。格式转换只改成你指定的 Markdown 或 HTML，不改原文措辞。",
                    isError = false,
                )
                state.error?.let { EntryStatusBanner(title = "AI 请求失败", detail = it, isError = true) }
                Text("格式转换", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EntryFormat.entries.forEach { target ->
                        FilterChip(
                            selected = state.reviewTargetFormat == target,
                            onClick = { viewModel.updateReviewTarget(target) },
                            label = { Text(target.label) },
                        )
                    }
                }
                Text(
                    if (state.reviewTargetFormat == state.format) {
                        "当前文章已经是 ${state.format.label}，请选择另一种目标格式。"
                    } else {
                        "当前将转换为 ${state.reviewTargetFormat.label}。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { confirmAction = "review" },
                        enabled = state.reviewTargetFormat != state.format,
                    ) {
                        Text("生成格式转换稿")
                    }
                }
                Text("文风润色", style = MaterialTheme.typography.titleMedium)
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item("ai-review-result") {
                            state.reviewResult?.let { result ->
                                ReviewResultCard(
                                    result = result,
                                    onApply = viewModel::applyReviewCandidate,
                                )
                            }
                        }
                        item("ai-polish-result") {
                            state.polishCandidate?.let { candidate ->
                                PolishCandidateCard(
                                    candidate = candidate,
                                    onApply = viewModel::applyPolishCandidate,
                                )
                            }
                        }
                        item("style-title") {
                            Text("可用文风", style = MaterialTheme.typography.titleMedium)
                        }
                        items(state.styles, key = { it.id }) { preset ->
                            StylePresetCard(
                                preset = preset,
                                onApply = { pendingStyle = preset },
                            )
                        }
                    }
                }
            }

            3 -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("emotion-banner") {
                    EntryStatusBanner(
                        title = "当前文章情绪",
                        detail = "这里保留当前文章的重新分析与摘要查看；完整情绪详情已经拆分为独立页面。",
                        isError = false,
                    )
                }
                if (state.error != null) {
                    item("emotion-error") {
                        EntryStatusBanner(title = "分析失败", detail = state.error, isError = true)
                    }
                }
                item("emotion-actions") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { confirmAction = "analysis" }) {
                            Text("重新分析当前文章")
                        }
                        TextButton(onClick = onOpenEmotionDetail) {
                            Text("查看完整分析")
                        }
                    }
                }
                item("emotion-latest") {
                    state.latestAnalysis?.let { analysis ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("最近一次分析", style = MaterialTheme.typography.titleMedium)
                                Text("情绪标签: ${analysis.labels.joinToString()}")
                                Text("强度: ${analysis.intensity}/100")
                                Text(analysis.summary)
                                analysis.suggestions.forEach { suggestion ->
                                    Text("• $suggestion")
                                }
                                if (analysis.safetyFlag) {
                                    Text("检测到高风险内容，请优先联系现实中的可信支持或专业帮助。")
                                }
                            }
                        }
                    } ?: Card(modifier = Modifier.fillMaxWidth()) {
                        Text("这篇文章还没有情绪分析记录。", modifier = Modifier.padding(16.dp))
                    }
                }
                item("emotion-center-link") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("更多分析", style = MaterialTheme.typography.titleMedium)
                            Text("日报、周报、月报与全部文章分析都在情绪中心；单篇完整记录可从独立分析页查看。")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onOpenEmotionCenter) {
                                    Text("前往情绪中心")
                                }
                                TextButton(onClick = onOpenEmotionDetail) {
                                    Text("打开分析详情")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorTab(
    state: com.localdiary.app.ui.viewmodel.EditorUiState,
    onTitleChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onExpandMeta: () -> Unit,
    onInsertImage: () -> Unit,
    onFocusTextBlock: (String, TextRange?) -> Unit,
    onTextChange: (String, String, TextRange) -> Unit,
    onDeleteImage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.infoMessage?.let {
            EntryStatusBanner(
                title = "操作提示",
                detail = it,
                isError = false,
            )
        }
        state.error?.let {
            EntryStatusBanner(
                title = "操作失败",
                detail = it,
                isError = true,
            )
        }
        if (state.isMetaCollapsed) {
            MetaSummaryCard(
                title = state.title.ifBlank { "未命名文章" },
                tagsInput = state.tagsInput,
                onExpand = onExpandMeta,
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onTitleChange,
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.tagsInput,
                        onValueChange = onTagsChange,
                        label = { Text("标签，逗号分隔") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("正文", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isMetaCollapsed) {
                            TextButton(onClick = onExpandMeta) {
                                Text("标题与标签")
                            }
                        }
                        Button(onClick = onInsertImage) {
                            Text("插入图片")
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.editorBlocks, key = { it.id }, contentType = { block ->
                        when (block) {
                            is EditorDocumentBlock.Text -> "text"
                            is EditorDocumentBlock.Image -> "image"
                        }
                    }) { block ->
                        when (block) {
                            is EditorDocumentBlock.Text -> {
                                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                                val scope = rememberCoroutineScope()
                                val selection = if (state.activeTextBlockId == block.id) {
                                    TextRange(
                                        state.selectionStart.coerceIn(0, block.text.length),
                                        state.selectionEnd.coerceIn(0, block.text.length),
                                    )
                                } else {
                                    TextRange(block.text.length)
                                }
                                OutlinedTextField(
                                    value = TextFieldValue(text = block.text, selection = selection),
                                    onValueChange = { value ->
                                        onFocusTextBlock(block.id, value.selection)
                                        onTextChange(block.id, value.text, value.selection)
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                onFocusTextBlock(block.id, selection)
                                                scope.launch {
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        },
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    placeholder = {
                                        Text("在这里继续写作，或把图片插入到当前光标位置。")
                                    },
                                )
                            }

                            is EditorDocumentBlock.Image -> {
                                EmbeddedImagePreview(
                                    dataUrl = block.dataUrl,
                                    subtitle = "${block.mimeType} · ${formatBytes(block.byteSize)}",
                                    footer = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { onDeleteImage(block.id) }) {
                                                Text("删除图片")
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaSummaryCard(
    title: String,
    tagsInput: String,
    onExpand: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (tagsInput.isNotBlank()) {
                    Text(
                        "标签: $tagsInput",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onExpand) {
                Text("展开")
            }
        }
    }
}

@Composable
private fun StylePresetCard(
    preset: StylePreset,
    onApply: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(preset.name, style = MaterialTheme.typography.titleMedium)
            Text(preset.prompt)
            TextButton(onClick = onApply) {
                Text("发送到 LLM 润色")
            }
        }
    }
}

@Composable
private fun ReviewResultCard(
    result: ReviewResult,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("格式转换候选稿", style = MaterialTheme.typography.titleMedium)
            Text("目标格式: ${result.format.label}")
            result.suggestedTitle?.takeIf { it.isNotBlank() }?.let {
                Text("建议标题: $it")
            }
            result.issues.forEach { issue ->
                Text("• $issue")
            }
            TextButton(onClick = onApply) {
                Text("应用转换稿")
            }
        }
    }
}

@Composable
private fun PolishCandidateCard(
    candidate: PolishCandidate,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("润色候选稿", style = MaterialTheme.typography.titleMedium)
            Text("文风: ${candidate.styleName}")
            Text(candidate.rationale)
            TextButton(onClick = onApply) {
                Text("应用润色稿")
            }
        }
    }
}

@Composable
private fun EntryStatusBanner(
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

private fun formatBytes(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format("%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
