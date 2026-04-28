package com.localdiary.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.UserPsychologyProfile
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PsychologyProfileUiState(
    val loading: Boolean = true,
    val profile: UserPsychologyProfile = UserPsychologyProfile(),
    val error: String? = null,
)

class PsychologyProfileViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    var uiState by mutableStateOf(PsychologyProfileUiState())
        private set

    init {
        viewModelScope.launch {
            repository.observeUserProfile().collectLatest { profile ->
                uiState = uiState.copy(loading = false, profile = profile)
            }
        }
    }

    fun updateList(field: ProfileField, rawValue: String) {
        val values = rawValue.lines().map { it.trim() }.filter { it.isNotBlank() }
        uiState = uiState.copy(
            profile = when (field) {
                ProfileField.TRIGGERS -> uiState.profile.copy(triggers = values)
                ProfileField.COGNITIVE -> uiState.profile.copy(cognitivePatterns = values)
                ProfileField.NEEDS -> uiState.profile.copy(needs = values)
                ProfileField.RELATIONSHIP -> uiState.profile.copy(relationshipPatterns = values)
                ProfileField.DEFENSE -> uiState.profile.copy(defensePatterns = values)
                ProfileField.BODY -> uiState.profile.copy(bodyStressSignals = values)
                ProfileField.STRENGTHS -> uiState.profile.copy(strengths = values)
                ProfileField.RISK -> uiState.profile.copy(riskNotes = values)
            },
        )
    }

    fun clearField(field: ProfileField) {
        updateList(field, "")
    }

    fun save() {
        viewModelScope.launch {
            runCatching { repository.saveUserProfile(uiState.profile) }
                .onSuccess { uiMessageManager.show("用户画像已保存。") }
                .onFailure { error -> uiState = uiState.copy(error = error.message ?: "保存用户画像失败。") }
        }
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            PsychologyProfileViewModel(repository, uiMessageManager)
        }
    }
}

enum class ProfileField {
    TRIGGERS,
    COGNITIVE,
    NEEDS,
    RELATIONSHIP,
    DEFENSE,
    BODY,
    STRENGTHS,
    RISK,
}
