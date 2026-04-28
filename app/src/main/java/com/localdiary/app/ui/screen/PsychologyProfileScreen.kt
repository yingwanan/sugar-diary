package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.localdiary.app.model.UserPsychologyProfile
import com.localdiary.app.ui.components.MarkdownText
import com.localdiary.app.ui.viewmodel.ProfileField
import com.localdiary.app.ui.viewmodel.PsychologyProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychologyProfileScreen(
    viewModel: PsychologyProfileViewModel,
    onNavigateBack: () -> Unit,
) {
    val state = viewModel.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        TopAppBar(
            title = { Text("用户画像") },
            navigationIcon = { TextButton(onClick = onNavigateBack) { Text("返回") } },
            actions = { TextButton(onClick = viewModel::save) { Text("保存") } },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("profile-help") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    MarkdownText(
                        "用户画像只保存在本机，用于让后续心理分析更贴近你。你可以随时修改或清空任何条目。",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            state.error?.let { error ->
                item("profile-error") { Card { Text(error, modifier = Modifier.padding(16.dp)) } }
            }
            profileFields(state.profile).forEach { item ->
                item(item.field.name) {
                    ProfileFieldEditor(
                        label = item.label,
                        values = item.values,
                        onValueChange = { viewModel.updateList(item.field, it) },
                        onClear = { viewModel.clearField(item.field) },
                    )
                }
            }
            item("profile-save") {
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text("保存用户画像")
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldEditor(
    label: String,
    values: List<String>,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("清空") }
            }
            OutlinedTextField(
                value = values.joinToString("\n"),
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("每行一条") },
            )
        }
    }
}

private data class ProfileFieldItem(
    val field: ProfileField,
    val label: String,
    val values: List<String>,
)

private fun profileFields(profile: UserPsychologyProfile): List<ProfileFieldItem> = listOf(
    ProfileFieldItem(ProfileField.TRIGGERS, "长期触发点", profile.triggers),
    ProfileFieldItem(ProfileField.COGNITIVE, "认知倾向", profile.cognitivePatterns),
    ProfileFieldItem(ProfileField.NEEDS, "核心需求", profile.needs),
    ProfileFieldItem(ProfileField.RELATIONSHIP, "关系模式", profile.relationshipPatterns),
    ProfileFieldItem(ProfileField.DEFENSE, "防御/应对", profile.defensePatterns),
    ProfileFieldItem(ProfileField.BODY, "身体压力", profile.bodyStressSignals),
    ProfileFieldItem(ProfileField.STRENGTHS, "资源优势", profile.strengths),
    ProfileFieldItem(ProfileField.RISK, "风险注意", profile.riskNotes),
)
