package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.domain.browser.EntryBrowserFilter
import com.localdiary.app.model.BrowserCategory
import com.localdiary.app.model.BrowserTimeBucket
import com.localdiary.app.model.EntryBrowserItem
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class BrowserUiState(
    val loading: Boolean = true,
    val query: String = "",
    val isSearchExpanded: Boolean = false,
    val category: BrowserCategory = BrowserCategory.ALL,
    val selectedTag: String? = null,
    val selectedMood: String? = null,
    val selectedTimeBucket: BrowserTimeBucket? = null,
    val availableTags: List<String> = emptyList(),
    val availableMoods: List<String> = emptyList(),
    val items: List<EntryBrowserItem> = emptyList(),
)

class BrowserViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    private var allItems: List<EntryBrowserItem> = emptyList()

    var uiState by mutableStateOf(BrowserUiState())
        private set

    init {
        viewModelScope.launch {
            repository.observeBrowserItems().collectLatest { items ->
                allItems = items
                rebuildState()
            }
        }
    }

    fun updateQuery(value: String) {
        uiState = uiState.copy(query = value)
        rebuildState()
    }

    fun toggleSearch() {
        val expanded = !uiState.isSearchExpanded
        uiState = uiState.copy(
            isSearchExpanded = expanded,
            query = if (expanded) uiState.query else "",
        )
        rebuildState()
    }

    fun selectCategory(category: BrowserCategory) {
        uiState = when (category) {
            BrowserCategory.ALL -> uiState.copy(
                category = category,
                selectedTag = null,
                selectedMood = null,
                selectedTimeBucket = null,
            )

            BrowserCategory.TAG -> uiState.copy(
                category = category,
                selectedMood = null,
                selectedTimeBucket = null,
            )

            BrowserCategory.TIME -> uiState.copy(
                category = category,
                selectedTag = null,
                selectedMood = null,
            )

            BrowserCategory.MOOD -> uiState.copy(
                category = category,
                selectedTag = null,
                selectedTimeBucket = null,
            )
        }
        rebuildState()
    }

    fun toggleTag(tag: String) {
        uiState = uiState.copy(
            selectedTag = if (uiState.selectedTag == tag) null else tag,
            selectedMood = null,
            selectedTimeBucket = null,
            category = BrowserCategory.TAG,
        )
        rebuildState()
    }

    fun toggleMood(mood: String) {
        uiState = uiState.copy(
            selectedMood = if (uiState.selectedMood == mood) null else mood,
            selectedTag = null,
            selectedTimeBucket = null,
            category = BrowserCategory.MOOD,
        )
        rebuildState()
    }

    fun toggleTimeBucket(bucket: BrowserTimeBucket) {
        uiState = uiState.copy(
            selectedTimeBucket = if (uiState.selectedTimeBucket == bucket) null else bucket,
            selectedTag = null,
            selectedMood = null,
            category = BrowserCategory.TIME,
        )
        rebuildState()
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            runCatching {
                repository.deleteEntry(entryId)
            }.onSuccess {
                uiMessageManager.show("文章已删除。")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "删除文章失败。")
            }
        }
    }

    private fun rebuildState() {
        val filtered = EntryBrowserFilter.filter(
            items = allItems,
            query = uiState.query,
            selectedTag = uiState.selectedTag,
            selectedMood = uiState.selectedMood,
            selectedTimeBucket = uiState.selectedTimeBucket,
        )
        uiState = uiState.copy(
            loading = false,
            items = filtered,
            availableTags = allItems.flatMap { it.meta.tags }.distinct().sorted(),
            availableMoods = allItems.flatMap { it.latestEmotion?.labels.orEmpty() }.distinct().sorted(),
        )
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            BrowserViewModel(repository, uiMessageManager)
        }
    }
}
