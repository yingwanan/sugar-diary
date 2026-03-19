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

data class ViewerUiState(
    val loading: Boolean = true,
    val document: EntryDocument? = null,
    val latestAnalysis: EmotionAnalysis? = null,
    val error: String? = null,
)

class ViewerViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
    private val entryId: String,
) : ViewModel() {
    var uiState by mutableStateOf(ViewerUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                ViewerUiState(
                    loading = false,
                    document = repository.loadDocument(entryId),
                    latestAnalysis = repository.latestAnalysis(entryId),
                )
            }.onSuccess { state ->
                uiState = state
            }.onFailure { error ->
                uiState = ViewerUiState(loading = false, error = error.message)
            }
        }
    }

    fun deleteEntry(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.deleteEntry(entryId)
            }.onSuccess {
                uiMessageManager.show("文章已删除。")
                onDeleted()
            }.onFailure { error ->
                uiState = uiState.copy(error = error.message ?: "删除文章失败。")
            }
        }
    }

    companion object {
        fun factory(
            repository: DiaryRepository,
            uiMessageManager: UiMessageManager,
            entryId: String,
        ): ViewModelProvider.Factory = singleFactory {
            ViewerViewModel(repository, uiMessageManager, entryId)
        }
    }
}
