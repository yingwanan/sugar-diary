package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.EntryDocument
import com.localdiary.app.model.PsychologyChatMessage
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PsychologyChatUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val input: String = "",
    val document: EntryDocument? = null,
    val messages: List<PsychologyChatMessage> = emptyList(),
    val error: String? = null,
)

class PsychologyChatViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
    private val entryId: String,
) : ViewModel() {
    var uiState by mutableStateOf(PsychologyChatUiState())
        private set

    init {
        viewModelScope.launch {
            runCatching { repository.loadDocument(entryId) }
                .onSuccess { document ->
                    uiState = uiState.copy(loading = false, document = document)
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        loading = false,
                        error = error.message ?: "加载心理 Agent 对话失败。",
                    )
                }
        }
        viewModelScope.launch {
            repository.observePsychologyChat(entryId).collectLatest { messages ->
                uiState = uiState.copy(messages = messages)
            }
        }
    }

    fun updateInput(value: String) {
        uiState = uiState.copy(input = value)
    }

    fun send() {
        val message = uiState.input.trim()
        if (message.isBlank()) return
        viewModelScope.launch {
            uiState = uiState.copy(sending = true, input = "", error = null)
            runCatching {
                repository.sendPsychologyChatMessage(entryId, message)
            }.onSuccess {
                uiState = uiState.copy(sending = false)
            }.onFailure { error ->
                uiState = uiState.copy(
                    sending = false,
                    input = message,
                    error = error.message ?: "心理 Agent 回复失败。",
                )
                uiMessageManager.show(error.message ?: "心理 Agent 回复失败。")
            }
        }
    }

    companion object {
        fun factory(
            repository: DiaryRepository,
            uiMessageManager: UiMessageManager,
            entryId: String,
        ): ViewModelProvider.Factory = singleFactory {
            PsychologyChatViewModel(repository, uiMessageManager, entryId)
        }
    }
}
