package com.localdiary.app.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.StorageMode
import com.localdiary.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val resolver = context.contentResolver

    val exportBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) viewModel.exportBundle(resolver, uri)
    }

    val importBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importBundle(resolver, uri)
    }

    val exportRawLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.exportRaw(resolver, uri)
    }

    val importRawLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.importRaw(resolver, uri)
    }

    val pickSystemFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.useSystemFolder(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        SettingsSectionCard(
            title = "主模型",
            description = "填写 OpenAI 兼容接口地址、模型名和密钥。",
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                label = { Text("接口地址") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("支持站点根地址、/v1 地址，或完整 /chat/completions 地址。")
            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::updateModel,
                label = { Text("主模型名称") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.requestTimeoutSeconds,
                onValueChange = viewModel::updateRequestTimeoutSeconds,
                label = { Text("请求超时秒数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("建议范围 15 到 300 秒。正文里有图片时，超时应适当放宽。")
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::updateApiKey,
                label = { Text("主模型 API Key") },
                modifier = Modifier.fillMaxWidth(),
            )
            ToggleRow(
                title = "主模型支持图片理解",
                subtitle = "仅作为高级路径使用。正文含图片时，优先先生成图片摘要，再交给主模型分析，可显著降低超时风险。",
                checked = state.supportsVision,
                onCheckedChange = viewModel::updateSupportsVision,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::saveAiConfig) {
                    Text("保存 AI 配置")
                }
                Button(onClick = viewModel::testAiConnection) {
                    Text("测试主模型")
                }
            }
        }

        SettingsSectionCard(
            title = "图片理解模型",
            description = "当主模型不支持视觉时，用它先解析图片内容，再交给主模型做情绪分析。",
        ) {
            ToggleRow(
                title = "启用独立图片模型",
                subtitle = "仅在情绪分析时用于图片理解。",
                checked = state.imageModelEnabled,
                onCheckedChange = viewModel::updateImageModelEnabled,
            )
            if (state.imageModelEnabled) {
                OutlinedTextField(
                    value = state.imageBaseUrl,
                    onValueChange = viewModel::updateImageBaseUrl,
                    label = { Text("图片模型接口地址") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.imageModel,
                    onValueChange = viewModel::updateImageModel,
                    label = { Text("图片模型名称") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.imageApiKey,
                    onValueChange = viewModel::updateImageApiKey,
                    label = { Text("图片模型 API Key") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = viewModel::testImageConnection) {
                    Text("测试图片模型")
                }
            }
        }

        SettingsSectionCard(
            title = "情绪分析提示词",
            description = "保留系统约束，同时允许你定制分析风格。",
            onHeaderClick = viewModel::toggleEmotionPromptExpanded,
            headerAction = {
                TextButton(onClick = viewModel::toggleEmotionPromptExpanded) {
                    Text(if (state.isEmotionPromptExpanded) "收起" else "展开")
                }
            },
        ) {
            if (state.isEmotionPromptExpanded) {
                Text("可用变量：{{entry_text}}  {{entry_format}}  {{image_context}}")
                OutlinedTextField(
                    value = state.emotionPromptTemplate,
                    onValueChange = viewModel::updateEmotionPromptTemplate,
                    label = { Text("情绪分析模板") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                )
                Text("模板必须保留 JSON 输出字段约束，否则无法保存。")
            }
        }

        SettingsSectionCard(
            title = "文章保存位置",
            description = "可保存在应用私有目录，或你授权的系统文件夹。",
        ) {
            Text(
                "当前模式: ${
                    if (state.storageMode == StorageMode.SYSTEM_FOLDER) "系统文件夹" else "应用私有目录"
                }",
            )
            if (state.storageMode == StorageMode.APP_PRIVATE) {
                Text("应用私有目录默认不显示在系统文件管理器中，可通过导出拿到文件。")
            } else {
                Text("当前系统目录: ${state.systemFolderUri ?: "未选择"}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::useAppPrivateStorage) {
                    Text("使用应用私有目录")
                }
                Button(onClick = { pickSystemFolderLauncher.launch(null) }) {
                    Text(if (state.systemFolderUri.isNullOrBlank()) "选择系统文件夹" else "更换系统文件夹")
                }
            }
        }

        SettingsSectionCard(
            title = "自定义文风",
            description = "内置文风保留不动，这里新增你自己的润色提示词。",
        ) {
            OutlinedTextField(
                value = state.newStyleName,
                onValueChange = viewModel::updateNewStyleName,
                label = { Text("文风名称") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.newStylePrompt,
                onValueChange = viewModel::updateNewStylePrompt,
                label = { Text("文风提示词") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Button(onClick = viewModel::saveCustomStyle) {
                Text("保存自定义文风")
            }
            state.styles.forEach { preset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(preset.name, style = MaterialTheme.typography.titleMedium)
                        Text(preset.prompt)
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "导入导出",
            description = "用于整库迁移，或直接迁移原始文章目录。",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportBundleLauncher.launch("diary_export.zip") }) {
                    Text("导出完整迁移包")
                }
                Button(onClick = { importBundleLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Text("导入完整迁移包")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportRawLauncher.launch(null) }) {
                    Text("导出原始文章文件夹")
                }
                Button(onClick = { importRawLauncher.launch(null) }) {
                    Text("导入原始文章文件夹")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    onHeaderClick: (() -> Unit)? = null,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onHeaderClick != null) {
                    TextButton(onClick = onHeaderClick) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
                headerAction?.invoke()
            }
            Text(description)
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
