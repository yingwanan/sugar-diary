package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.MoodReport
import com.localdiary.app.model.ReportPeriod
import com.localdiary.app.model.label
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EmotionReportsUiState(
    val loading: Boolean = true,
    val reports: List<MoodReport> = emptyList(),
    val generatingPeriod: ReportPeriod? = null,
    val deletingReportId: String? = null,
    val error: String? = null,
)

class EmotionReportsViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    var uiState by mutableStateOf(EmotionReportsUiState())
        private set

    init {
        viewModelScope.launch {
            repository.deleteLegacyDailyReports()
            repository.observeReports().collectLatest { reports ->
                uiState = uiState.copy(
                    loading = false,
                    reports = reports,
                )
            }
        }
    }

    fun generateReport(period: ReportPeriod) {
        viewModelScope.launch {
            uiState = uiState.copy(generatingPeriod = period, error = null)
            runCatching { repository.generateReport(period) }
                .onSuccess {
                    uiMessageManager.show("${period.label}已生成。")
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message ?: "生成报告失败。")
                }
            uiState = uiState.copy(generatingPeriod = null)
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(deletingReportId = reportId, error = null)
            runCatching { repository.deleteReport(reportId) }
                .onSuccess {
                    uiMessageManager.show("报告已删除。")
                }
                .onFailure { error ->
                    uiState = uiState.copy(error = error.message ?: "删除报告失败。")
                }
            uiState = uiState.copy(deletingReportId = null)
        }
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            EmotionReportsViewModel(repository, uiMessageManager)
        }
    }
}
