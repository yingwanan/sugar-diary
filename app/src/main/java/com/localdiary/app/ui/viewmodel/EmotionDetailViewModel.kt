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
import com.localdiary.app.model.PsychologyAgentProcessEvent
import com.localdiary.app.model.PsychologyAgentRuntimeUpdate
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.launch

data class EmotionDetailUiState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val document: EntryDocument? = null,
    val latestAnalysis: EmotionAnalysis? = null,
    val history: List<EmotionAnalysis> = emptyList(),
    val selectedAgentId: String? = null,
    val runtimeEvents: List<PsychologyAgentProcessEvent> = emptyList(),
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
                        error = error.message ?: "加载心理分析失败。",
                    )
                }
        }
    }

    fun selectAgent(agentId: String?) {
        uiState = uiState.copy(selectedAgentId = agentId)
    }

    fun analyzeEntry() {
        viewModelScope.launch {
            uiState = uiState.copy(working = true, error = null, runtimeEvents = emptyList())
            runCatching {
                repository.runPsychologyAnalysis(entryId, uiState.selectedAgentId).collect { update ->
                    when (update) {
                        is PsychologyAgentRuntimeUpdate.Event -> {
                            val event = update.event
                            val others = uiState.runtimeEvents.filterNot { existing ->
                                existing.agentId == event.agentId &&
                                    existing.phase == event.phase &&
                                    (existing.isPartial || event.isPartial)
                            }
                            uiState = uiState.copy(runtimeEvents = (others + event).sortedBy { it.sequence })
                        }
                        is PsychologyAgentRuntimeUpdate.Completed -> {
                            val state = loadState()
                            uiState = state.copy(
                                working = true,
                                selectedAgentId = uiState.selectedAgentId,
                                runtimeEvents = uiState.runtimeEvents,
                            )
                        }
                    }
                }
                loadState()
            }.onSuccess { state ->
                uiState = state.copy(
                    working = false,
                    selectedAgentId = uiState.selectedAgentId,
                    runtimeEvents = repository.latestAgentEvents(entryId),
                )
                uiMessageManager.show("心理分析已更新。")
            }.onFailure { error ->
                uiState = uiState.copy(
                    working = false,
                    error = error.message ?: "心理分析失败。",
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
            selectedAgentId = uiState.selectedAgentId,
            runtimeEvents = repository.latestAgentEvents(entryId),
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
