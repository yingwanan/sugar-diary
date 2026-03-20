package com.localdiary.app.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localdiary.app.data.DiaryRepository
import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.EmotionPromptTemplate
import com.localdiary.app.model.StorageMode
import com.localdiary.app.model.StylePreset
import com.localdiary.app.ui.UiMessageManager
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val requestTimeoutSeconds: String = "90",
    val supportsVision: Boolean = false,
    val imageModelEnabled: Boolean = false,
    val imageBaseUrl: String = "",
    val imageApiKey: String = "",
    val imageModel: String = "",
    val emotionPromptTemplate: String = AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE,
    val isEmotionPromptExpanded: Boolean = false,
    val storageMode: StorageMode = StorageMode.APP_PRIVATE,
    val systemFolderUri: String? = null,
    val styles: List<StylePreset> = emptyList(),
    val newStyleName: String = "",
    val newStylePrompt: String = "",
    val busy: Boolean = false,
)

class SettingsViewModel(
    private val repository: DiaryRepository,
    private val uiMessageManager: UiMessageManager,
) : ViewModel() {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val config = repository.getAiConfig()
            val storageSettings = repository.getStorageSettings()
            uiState = uiState.copy(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                model = config.model,
                requestTimeoutSeconds = config.requestTimeoutSeconds.toString(),
                supportsVision = config.supportsVision,
                imageModelEnabled = config.imageModelEnabled,
                imageBaseUrl = config.imageBaseUrl,
                imageApiKey = config.imageApiKey,
                imageModel = config.imageModel,
                emotionPromptTemplate = config.emotionPromptTemplate,
                storageMode = storageSettings.mode,
                systemFolderUri = storageSettings.treeUri,
                styles = repository.listStyles(),
            )
        }
    }

    fun updateBaseUrl(value: String) {
        uiState = uiState.copy(baseUrl = value)
    }

    fun updateApiKey(value: String) {
        uiState = uiState.copy(apiKey = value)
    }

    fun updateModel(value: String) {
        uiState = uiState.copy(model = value)
    }

    fun updateRequestTimeoutSeconds(value: String) {
        uiState = uiState.copy(requestTimeoutSeconds = value.filter { it.isDigit() }.take(3))
    }

    fun updateSupportsVision(value: Boolean) {
        uiState = uiState.copy(supportsVision = value)
    }

    fun updateImageModelEnabled(value: Boolean) {
        uiState = uiState.copy(imageModelEnabled = value)
    }

    fun updateImageBaseUrl(value: String) {
        uiState = uiState.copy(imageBaseUrl = value)
    }

    fun updateImageApiKey(value: String) {
        uiState = uiState.copy(imageApiKey = value)
    }

    fun updateImageModel(value: String) {
        uiState = uiState.copy(imageModel = value)
    }

    fun updateEmotionPromptTemplate(value: String) {
        uiState = uiState.copy(emotionPromptTemplate = value)
    }

    fun toggleEmotionPromptExpanded() {
        uiState = uiState.copy(isEmotionPromptExpanded = !uiState.isEmotionPromptExpanded)
    }

    fun updateNewStyleName(value: String) {
        uiState = uiState.copy(newStyleName = value)
    }

    fun updateNewStylePrompt(value: String) {
        uiState = uiState.copy(newStylePrompt = value)
    }

    fun useAppPrivateStorage() {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveStorageSettings(StorageMode.APP_PRIVATE, uiState.systemFolderUri)
            }.onSuccess {
                uiState = uiState.copy(storageMode = StorageMode.APP_PRIVATE)
                uiMessageManager.show("已切换到应用私有目录。")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "切换保存目录失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun useSystemFolder(treeUri: String) {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveSystemFolder(treeUri)
            }.onSuccess {
                uiState = uiState.copy(
                    storageMode = StorageMode.SYSTEM_FOLDER,
                    systemFolderUri = treeUri,
                )
                uiMessageManager.show("系统文件夹已授权，后续新文章会直接保存到该目录。")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "系统文件夹授权失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun saveAiConfig() {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveAiConfig(buildAiConfig())
            }.onSuccess {
                uiMessageManager.show("AI 配置已保存。")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "AI 配置保存失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveAiConfig(buildAiConfig())
                repository.testAiConnection()
            }.onSuccess { message ->
                uiMessageManager.show("连接成功：$message")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "连接测试失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun testImageConnection() {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveAiConfig(buildAiConfig())
                repository.testImageConnection()
            }.onSuccess { message ->
                uiMessageManager.show("图片模型连接成功：$message")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "图片模型连接测试失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun saveCustomStyle() {
        viewModelScope.launch {
            if (uiState.newStyleName.isBlank() || uiState.newStylePrompt.isBlank()) {
                uiMessageManager.show("请填写文风名称和提示词。")
                return@launch
            }
            uiState = uiState.copy(busy = true)
            runCatching {
                repository.saveCustomStyle(uiState.newStyleName, uiState.newStylePrompt)
                repository.listStyles()
            }.onSuccess { styles ->
                uiState = uiState.copy(
                    styles = styles,
                    newStyleName = "",
                    newStylePrompt = "",
                )
                uiMessageManager.show("自定义文风已保存。")
            }.onFailure { error ->
                uiMessageManager.show(error.message ?: "自定义文风保存失败。")
            }
            uiState = uiState.copy(busy = false)
        }
    }

    fun exportBundle(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching { repository.exportBundle(resolver, uri) }
                .onSuccess { uiMessageManager.show("完整迁移包已导出。") }
                .onFailure { error -> uiMessageManager.show(error.message ?: "完整迁移包导出失败。") }
            uiState = uiState.copy(busy = false)
        }
    }

    fun importBundle(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching { repository.importBundle(resolver, uri) }
                .onSuccess {
                    refresh()
                    uiMessageManager.show("完整迁移包已导入。")
                }
                .onFailure { error -> uiMessageManager.show(error.message ?: "完整迁移包导入失败。") }
            uiState = uiState.copy(busy = false)
        }
    }

    fun exportRaw(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching { repository.exportRaw(resolver, uri) }
                .onSuccess { uiMessageManager.show("原始文章文件已导出。") }
                .onFailure { error -> uiMessageManager.show(error.message ?: "原始文章导出失败。") }
            uiState = uiState.copy(busy = false)
        }
    }

    fun importRaw(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(busy = true)
            runCatching { repository.importRaw(resolver, uri) }
                .onSuccess { uiMessageManager.show("原始文章文件已导入。") }
                .onFailure { error -> uiMessageManager.show(error.message ?: "原始文章导入失败。") }
            uiState = uiState.copy(busy = false)
        }
    }

    companion object {
        fun factory(repository: DiaryRepository, uiMessageManager: UiMessageManager): ViewModelProvider.Factory = singleFactory {
            SettingsViewModel(repository, uiMessageManager)
        }
    }

    private fun buildAiConfig(): AiEndpointConfig {
        EmotionPromptTemplate.validate(uiState.emotionPromptTemplate)?.let { error(it) }
        return AiEndpointConfig(
            baseUrl = uiState.baseUrl.trim(),
            apiKey = uiState.apiKey.trim(),
            model = uiState.model.trim(),
            requestTimeoutSeconds = uiState.requestTimeoutSeconds.toIntOrNull()
                ?.takeIf { it in 15..300 }
                ?: error("请求超时时间必须是 15 到 300 之间的整数秒。"),
            supportsVision = uiState.supportsVision,
            imageModelEnabled = uiState.imageModelEnabled,
            imageBaseUrl = uiState.imageBaseUrl.trim(),
            imageApiKey = uiState.imageApiKey.trim(),
            imageModel = uiState.imageModel.trim(),
            emotionPromptTemplate = uiState.emotionPromptTemplate.trim(),
        )
    }
}
