package com.localdiary.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.EntryMeta
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimelineViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    val entries: StateFlow<List<EntryMeta>> = repository.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createEntry(
        title: String,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createEntry(title = title, format = com.localdiary.app.model.EntryFormat.MARKDOWN)
            }.onSuccess { entryId ->
                onCreated(entryId)
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "创建文章失败。")
            }
        }
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            TimelineViewModel(repository, uiMessageManager)
        }
    }
}
