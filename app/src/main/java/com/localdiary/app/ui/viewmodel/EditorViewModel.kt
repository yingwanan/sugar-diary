package com.localdiary.app.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.EditorDocumentBlock
import com.localdiary.app.model.EditorDocumentParser
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EntryFormat
import com.localdiary.app.model.PolishCandidate
import com.localdiary.app.model.ReviewResult
import com.localdiary.app.model.StylePreset
import com.localdiary.app.model.VersionSnapshot
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.launch

data class EditorUiState(
    val title: String = "",
    val tagsInput: String = "",
    val content: String = "",
    val editorBlocks: List<EditorDocumentBlock> = listOf(EditorDocumentParser.newTextBlock()),
    val activeTextBlockId: String? = null,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val format: EntryFormat = EntryFormat.MARKDOWN,
    val reviewTargetFormat: EntryFormat = EntryFormat.HTML,
    val isMetaCollapsed: Boolean = false,
    val loading: Boolean = true,
    val working: Boolean = false,
    val latestAnalysis: EmotionAnalysis? = null,
    val styles: List<StylePreset> = emptyList(),
    val versions: List<VersionSnapshot> = emptyList(),
    val reviewResult: ReviewResult? = null,
    val polishCandidate: PolishCandidate? = null,
    val dirty: Boolean = false,
    val infoMessage: String? = null,
    val error: String? = null,
)

class EditorViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
    private val entryId: String,
) : ViewModel() {
    var uiState by mutableStateOf(EditorUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                val document = repository.loadDocument(entryId)
                val blocks = EditorDocumentParser.parse(document.content)
                val activeTextBlock = blocks.filterIsInstance<EditorDocumentBlock.Text>().lastOrNull()
                uiState = uiState.copy(
                    title = document.meta.title,
                    tagsInput = document.meta.tags.joinToString(),
                    content = document.content,
                    editorBlocks = blocks,
                    activeTextBlockId = activeTextBlock?.id,
                    selectionStart = activeTextBlock?.text?.length ?: 0,
                    selectionEnd = activeTextBlock?.text?.length ?: 0,
                    format = document.meta.format,
                    reviewTargetFormat = alternateFormat(document.meta.format),
                    styles = repository.listStyles(),
                    versions = repository.listVersions(entryId),
                    latestAnalysis = repository.latestAnalysis(entryId),
                    loading = false,
                    dirty = false,
                    infoMessage = null,
                    error = null,
                )
            }.onFailure { error ->
                uiState = uiState.copy(loading = false, error = error.message)
            }
        }
    }

    fun updateTitle(value: String) {
        uiState = uiState.copy(title = value, dirty = true, infoMessage = null)
    }

    fun updateTags(value: String) {
        uiState = uiState.copy(tagsInput = value, dirty = true, infoMessage = null)
    }

    fun updateReviewTarget(format: EntryFormat) {
        uiState = uiState.copy(reviewTargetFormat = format, error = null, infoMessage = null)
    }

    fun expandMeta() {
        uiState = uiState.copy(isMetaCollapsed = false)
    }

    fun updateTextBlock(
        blockId: String,
        value: String,
        selection: TextRange,
    ) {
        val blocks = uiState.editorBlocks.map { block ->
            if (block is EditorDocumentBlock.Text && block.id == blockId) {
                block.copy(text = value)
            } else {
                block
            }
        }
        uiState = uiState.copy(
            editorBlocks = blocks,
            activeTextBlockId = blockId,
            selectionStart = selection.start.coerceIn(0, value.length),
            selectionEnd = selection.end.coerceIn(0, value.length),
            isMetaCollapsed = true,
            dirty = true,
            infoMessage = null,
        )
    }

    fun focusTextBlock(
        blockId: String,
        selection: TextRange? = null,
        collapseMeta: Boolean = false,
    ) {
        val block = uiState.editorBlocks.firstOrNull { it is EditorDocumentBlock.Text && it.id == blockId } as? EditorDocumentBlock.Text
            ?: return
        uiState = uiState.copy(
            activeTextBlockId = blockId,
            selectionStart = selection?.start?.coerceIn(0, block.text.length) ?: block.text.length,
            selectionEnd = selection?.end?.coerceIn(0, block.text.length) ?: block.text.length,
            isMetaCollapsed = uiState.isMetaCollapsed || collapseMeta,
        )
    }

    fun save() {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.saveEntry(
                    entryId = entryId,
                    title = uiState.title,
                    content = EditorDocumentParser.buildContent(uiState.editorBlocks),
                    tags = uiState.tagsInput.split(",").mapNotNull { tag ->
                        tag.trim().takeIf { it.isNotBlank() }
                    },
                )
            }.onSuccess {
                uiState = uiState.copy(
                    content = EditorDocumentParser.buildContent(uiState.editorBlocks),
                    dirty = false,
                    infoMessage = "文章已保存。",
                )
            }.onFailure { error ->
                uiState = uiState.copy(error = error.message)
            }
            uiState = uiState.copy(working = false)
        }
    }

    fun insertImage(uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null, infoMessage = null)
            runCatching {
                repository.createEmbeddedImageSnippet(uri, uiState.format)
            }.onSuccess { result ->
                val updatedBlocks = insertImageIntoBlocks(
                    blocks = uiState.editorBlocks,
                    imageBlock = EditorDocumentParser.createImageBlock(
                        rawSnippet = result.rawSnippet,
                        dataUrl = result.dataUrl,
                        mimeType = result.mimeType,
                        byteSize = result.byteSize,
                    ),
                )
                val nextTextBlock = updatedBlocks.blocks.getOrNull(updatedBlocks.nextTextIndex) as? EditorDocumentBlock.Text
                uiState = uiState.copy(
                    editorBlocks = updatedBlocks.blocks,
                    activeTextBlockId = nextTextBlock?.id,
                    selectionStart = 0,
                    selectionEnd = 0,
                    isMetaCollapsed = true,
                    dirty = true,
                    infoMessage = "图片已插入当前光标位置。",
                )
            }.onFailure { error ->
                uiState = uiState.copy(error = error.message ?: "插入图片失败。")
            }
            uiState = uiState.copy(working = false)
        }
    }

    fun removeImageBlock(blockId: String) {
        val imageIndex = uiState.editorBlocks.indexOfFirst { it.id == blockId && it is EditorDocumentBlock.Image }
        if (imageIndex < 0) return
        val updatedBlocks = uiState.editorBlocks.toMutableList().apply { removeAt(imageIndex) }
            .mergeAdjacentTextBlocks()
            .ensureTrailingTextBlock()
        val focusIndex = updatedBlocks
            .subList(0, minOf(imageIndex, updatedBlocks.size - 1) + 1)
            .indexOfLast { it is EditorDocumentBlock.Text }
            .takeIf { it >= 0 }
            ?: updatedBlocks.indexOfLast { it is EditorDocumentBlock.Text }
        val focusBlock = updatedBlocks.getOrNull(focusIndex) as? EditorDocumentBlock.Text
        uiState = uiState.copy(
            editorBlocks = updatedBlocks,
            activeTextBlockId = focusBlock?.id,
            selectionStart = focusBlock?.text?.length ?: 0,
            selectionEnd = focusBlock?.text?.length ?: 0,
            dirty = true,
            infoMessage = "图片已删除。",
            error = null,
        )
    }

    fun deleteEntry(onDeleted: () -> Unit) {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.deleteEntry(entryId)
            }.onSuccess {
                uiMessageManager.show("文章已删除。")
                onDeleted()
            }.onFailure { error ->
                uiState = uiState.copy(error = error.message ?: "删除文章失败。")
            }
            uiState = uiState.copy(working = false)
        }
    }

    fun review() {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.reviewContent(
                    content = buildEditorContent(),
                    targetFormat = uiState.reviewTargetFormat,
                )
            }
                .onSuccess { result ->
                    uiState = uiState.copy(reviewResult = result, polishCandidate = null)
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message)
                }
            uiState = uiState.copy(working = false)
        }
    }

    fun polish(preset: StylePreset) {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.polishContent(
                    content = buildEditorContent(),
                    format = uiState.format,
                    preset = preset,
                )
            }
                .onSuccess { candidate ->
                    uiState = uiState.copy(polishCandidate = candidate, reviewResult = null)
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message)
                }
            uiState = uiState.copy(working = false)
        }
    }

    fun applyReviewCandidate() {
        val candidate = uiState.reviewResult ?: return
        applyCandidate(candidate.candidateContent, "format_conversion", candidate.format)
    }

    fun applyPolishCandidate() {
        val candidate = uiState.polishCandidate ?: return
        applyCandidate(candidate.content, "ai_polish", uiState.format)
    }

    private fun applyCandidate(
        content: String,
        source: String,
        targetFormat: EntryFormat,
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.applyCandidate(entryId, content, source, targetFormat)
                val document = repository.loadDocument(entryId)
                val blocks = EditorDocumentParser.parse(document.content)
                val focusText = blocks.filterIsInstance<EditorDocumentBlock.Text>().lastOrNull()
                uiState = uiState.copy(
                    content = document.content,
                    editorBlocks = blocks,
                    activeTextBlockId = focusText?.id,
                    selectionStart = focusText?.text?.length ?: 0,
                    selectionEnd = focusText?.text?.length ?: 0,
                    format = document.meta.format,
                    reviewTargetFormat = alternateFormat(document.meta.format),
                    isMetaCollapsed = true,
                    versions = repository.listVersions(entryId),
                    reviewResult = null,
                    polishCandidate = null,
                    dirty = false,
                    infoMessage = "已应用候选稿并保存。",
                )
            }.onFailure { error ->
                uiState = uiState.copy(error = error.message)
            }
            uiState = uiState.copy(working = false)
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(error = null, infoMessage = null)
    }

    fun analyzePsychology() {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.analyzeContent(
                    entryId = entryId,
                    content = buildEditorContent(),
                    format = uiState.format,
                )
            }
                .onSuccess { analysis ->
                    uiState = uiState.copy(
                        latestAnalysis = analysis,
                        infoMessage = "情绪分析已更新。",
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message)
                }
            uiState = uiState.copy(working = false)
        }
    }

    companion object {
        fun factory(
            repository: DiaryRepository,
            uiMessageManager: UiMessageManager,
            entryId: String,
        ): ViewModelProvider.Factory = singleFactory {
            EditorViewModel(repository, uiMessageManager, entryId)
        }
    }

    private fun buildEditorContent(): String = EditorDocumentParser.buildContent(uiState.editorBlocks)

    private fun insertImageIntoBlocks(
        blocks: List<EditorDocumentBlock>,
        imageBlock: EditorDocumentBlock.Image,
    ): InsertResult {
        val currentTextIndex = blocks.indexOfFirst { it.id == uiState.activeTextBlockId && it is EditorDocumentBlock.Text }
        val targetIndex = currentTextIndex.takeIf { it >= 0 }
            ?: blocks.indexOfLast { it is EditorDocumentBlock.Text }.takeIf { it >= 0 }
            ?: blocks.size

        if (targetIndex == blocks.size) {
            val appended = blocks.toMutableList()
            val trailing = EditorDocumentParser.newTextBlock()
            appended += imageBlock
            appended += trailing
            return InsertResult(appended.ensureTrailingTextBlock(), appended.indexOf(trailing))
        }

        val currentTextBlock = blocks[targetIndex] as EditorDocumentBlock.Text
        val start = uiState.selectionStart.coerceIn(0, currentTextBlock.text.length)
        val end = uiState.selectionEnd.coerceIn(0, currentTextBlock.text.length)
        val leftText = currentTextBlock.text.substring(0, start)
        val rightText = currentTextBlock.text.substring(end)
        val replacement = mutableListOf<EditorDocumentBlock>()
        if (leftText.isNotEmpty()) {
            replacement += currentTextBlock.copy(text = leftText)
        }
        replacement += imageBlock
        val trailingTextBlock = EditorDocumentParser.newTextBlock(rightText)
        replacement += trailingTextBlock

        val updated = blocks.toMutableList().apply {
            removeAt(targetIndex)
            addAll(targetIndex, replacement)
        }.mergeAdjacentTextBlocks().ensureTrailingTextBlock()
        val nextIndex = updated.indexOfFirst { it.id == trailingTextBlock.id }
            .takeIf { it >= 0 }
            ?: updated.indexOfLast { it is EditorDocumentBlock.Text }
        return InsertResult(updated, nextIndex)
    }

    private data class InsertResult(
        val blocks: List<EditorDocumentBlock>,
        val nextTextIndex: Int,
    )
}

private fun alternateFormat(format: EntryFormat): EntryFormat = when (format) {
    EntryFormat.MARKDOWN -> EntryFormat.HTML
    EntryFormat.HTML -> EntryFormat.MARKDOWN
}

private fun MutableList<EditorDocumentBlock>.mergeAdjacentTextBlocks(): MutableList<EditorDocumentBlock> {
    var index = 0
    while (index < size - 1) {
        val current = this[index]
        val next = this[index + 1]
        if (current is EditorDocumentBlock.Text && next is EditorDocumentBlock.Text) {
            this[index] = current.copy(text = current.text + next.text)
            removeAt(index + 1)
        } else {
            index++
        }
    }
    return this
}

private fun MutableList<EditorDocumentBlock>.ensureTrailingTextBlock(): MutableList<EditorDocumentBlock> {
    if (isEmpty()) {
        add(EditorDocumentParser.newTextBlock())
    } else if (last() !is EditorDocumentBlock.Text) {
        add(EditorDocumentParser.newTextBlock())
    }
    return this
}
