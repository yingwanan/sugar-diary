package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.domain.emotion.EmotionCenterFilter
import com.localdiary.app.model.EmotionCenterItem
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EmotionCenterUiState(
    val loading: Boolean = true,
    val query: String = "",
    val isSearchExpanded: Boolean = false,
    val items: List<EmotionCenterItem> = emptyList(),
    val workingEntryId: String? = null,
    val error: String? = null,
)

class EmotionCenterViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    private var latestItems: List<EmotionCenterItem> = emptyList()

    var uiState by mutableStateOf(EmotionCenterUiState())
        private set

    init {
        viewModelScope.launch {
            repository.observeEmotionCenterItems().collectLatest { items ->
                latestItems = items
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

    fun analyzeEntry(entryId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(workingEntryId = entryId, error = null)
            runCatching { repository.analyzeEntry(entryId) }
                .onSuccess {
                    uiMessageManager.show("情绪分析已更新。")
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message ?: "情绪分析失败。")
                }
            uiState = uiState.copy(workingEntryId = null)
        }
    }

    private fun rebuildState() {
        uiState = uiState.copy(
            loading = false,
            items = EmotionCenterFilter.filter(latestItems, uiState.query),
        )
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            EmotionCenterViewModel(repository, uiMessageManager)
        }
    }
}
