package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.localdiary.app.model.PsychologyChatMessage
import com.localdiary.app.model.PsychologyChatRole
import com.localdiary.app.ui.viewmodel.PsychologyChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsychologyChatScreen(
    viewModel: PsychologyChatViewModel,
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
            title = { Text(state.document?.meta?.title ?: "心理 Agent") },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Text(
                    "心理 Agent 基于本机日记上下文和历史分析提供自我理解支持，不能替代心理咨询、医学诊断或危机干预。",
                    modifier = Modifier.padding(16.dp),
                )
            }
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(16.dp))
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.messages.isEmpty() && !state.loading) {
                    item("empty-chat") {
                        Text("你可以围绕这篇日记追问：我为什么会这样反应？下一步可以怎么照顾自己？")
                    }
                }
                items(state.messages, key = { it.id }) { message ->
                    PsychologyChatBubble(message)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f),
                    label = { Text("和心理 Agent 对话") },
                    minLines = 1,
                    maxLines = 4,
                )
                Button(
                    onClick = viewModel::send,
                    enabled = !state.sending && state.input.isNotBlank(),
                ) {
                    Text(if (state.sending) "发送中" else "发送")
                }
            }
        }
    }
}

@Composable
private fun PsychologyChatBubble(message: PsychologyChatMessage) {
    val isUser = message.role == PsychologyChatRole.USER
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (isUser) "我" else "心理 Agent", style = MaterialTheme.typography.labelMedium)
            Text(message.content)
        }
    }
}
