package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.EntryDocument
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.launch

data class EmotionDetailUiState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val document: EntryDocument? = null,
    val latestAnalysis: EmotionAnalysis? = null,
    val history: List<EmotionAnalysis> = emptyList(),
    val error: String? = null,
)

class EmotionDetailViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
    private val entryId: String,
) : ViewModel() {
    var uiState by mutableStateOf(EmotionDetailUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val working = uiState.working
            runCatching { loadState() }
                .onSuccess { state ->
                    uiState = state.copy(working = working)
                }
                .onFailure { error ->
                    uiState = EmotionDetailUiState(
                        loading = false,
                        working = false,
                        error = error.message ?: "加载情绪分析失败。",
                    )
                }
        }
    }

    fun analyzeEntry() {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null)
            runCatching {
                repository.analyzeEntry(entryId)
                loadState()
            }.onSuccess { state ->
                uiState = state.copy(working = false)
                uiMessageManager.show("情绪分析已更新。")
            }.onFailure { error ->
                uiState = uiState.copy(
                    working = false,
                    error = error.message ?: "情绪分析失败。",
                )
            }
        }
    }

    private suspend fun loadState(): EmotionDetailUiState {
        val document = repository.loadDocument(entryId)
        val analyses = repository.listAnalyses(entryId)
        return EmotionDetailUiState(
            loading = false,
            document = document,
            latestAnalysis = analyses.firstOrNull(),
            history = analyses,
        )
    }

    companion object {
        fun factory(
            repository: DiaryRepository,
            uiMessageManager: UiMessageManager,
            entryId: String,
        ): ViewModelProvider.Factory = singleFactory {
            EmotionDetailViewModel(repository, uiMessageManager, entryId)
        }
    }
}
