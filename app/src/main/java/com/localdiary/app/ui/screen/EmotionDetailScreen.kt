package com.localdiary.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.localdiary.app.model.EmotionAnalysis
import com.localdiary.app.model.PsychologyAgentProcessEvent
import com.localdiary.app.domain.psychology.PsychologyAgentEventDisplay
import com.localdiary.app.ui.components.MarkdownText
import com.localdiary.app.ui.components.PsychologyAgentSelector
import com.localdiary.app.ui.viewmodel.EmotionDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionDetailScreen(
    viewModel: EmotionDetailViewModel,
    onNavigateBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    onOpenPsychologyChat: (String) -> Unit,
) {
    val state = viewModel.uiState
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun scrollToAgent(agentId: String) {
        val eventIndex = PsychologyAgentEventDisplay.visibleEvents(state.runtimeEvents)
            .indexOfFirst { it.agentId == agentId }
        if (eventIndex >= 0) {
            coroutineScope.launch { listState.animateScrollToItem(3 + eventIndex) }
        }
    }

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
            title = { Text(state.document?.meta?.title ?: "心理分析") },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
            },
            actions = {
                TextButton(onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } }) {
                    Text("回顶")
                }
            },
        )

        when {
            state.loading -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("正在加载心理分析...")
                }
            }

            state.error != null -> {
                Card(modifier = Modifier.padding(16.dp)) {
                    Text(state.error, modifier = Modifier.padding(16.dp))
                }
            }

            state.document != null -> {
                val document = state.document
                val visibleEvents = PsychologyAgentEventDisplay.visibleEvents(state.runtimeEvents)
                val showSynthesisStatus = PsychologyAgentEventDisplay.shouldShowSynthesisStatus(
                    events = state.runtimeEvents,
                    working = state.working,
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item("emotion-detail-header") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(document.meta.title, style = MaterialTheme.typography.headlineSmall)
                                Text("更新于 ${formatEmotionTimestamp(document.meta.updatedAt)}")
                                state.latestAnalysis?.let { analysis ->
                                    Text(
                                        "最近分析于 ${formatEmotionTimestamp(analysis.createdAt)}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text("重新分析范围", style = MaterialTheme.typography.titleSmall)
                                PsychologyAgentSelector(
                                    selectedAgentId = state.selectedAgentId,
                                    onSelect = viewModel::selectAgent,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = viewModel::analyzeEntry,
                                        enabled = !state.working,
                                    ) {
                                        Text(if (state.working) "分析中..." else "开始分析")
                                    }
                                    TextButton(onClick = { onOpenEntry(document.meta.id) }) {
                                        Text("查看文章")
                                    }
                                    TextButton(onClick = { onEditEntry(document.meta.id) }) {
                                        Text("编辑")
                                    }
                                    TextButton(onClick = { onOpenPsychologyChat(document.meta.id) }) {
                                        Text("与心理 Agent 对话")
                                    }
                                }
                            }
                        }
                    }

                    if (visibleEvents.isNotEmpty() || showSynthesisStatus) {
                        item("agent-process-title") {
                            Text(if (state.working) "Agent 运行中" else "Agent 过程摘要", style = MaterialTheme.typography.titleMedium)
                        }
                        if (visibleEvents.isNotEmpty()) {
                            item("agent-process-nav") {
                                AgentProcessNavigator(
                                    events = visibleEvents,
                                    onSelectAgent = ::scrollToAgent,
                                )
                            }
                        }
                        items(visibleEvents, key = { it.id }) { event ->
                            AgentProcessEventCard(event)
                        }
                        if (showSynthesisStatus) {
                            item("synthesis-status") {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "综合 Agent 正在整合各 Agent 观点，稍后会在最新分析中展示可读结果。",
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    item("emotion-detail-latest-title") {
                        Text("最新分析", style = MaterialTheme.typography.titleMedium)
                    }

                    item("emotion-detail-latest-card") {
                        state.latestAnalysis?.let { analysis ->
                            EmotionAnalysisCard(
                                analysis = analysis,
                                title = "当前解读",
                            )
                        } ?: Card(modifier = Modifier.fillMaxWidth()) {
                        Text("这篇文章还没有心理分析记录。", modifier = Modifier.padding(16.dp))
                        }
                    }

                    if (state.history.isNotEmpty()) {
                        item("emotion-detail-history-title") {
                            Text("历史分析", style = MaterialTheme.typography.titleMedium)
                        }
                        items(state.history.drop(1), key = { it.id }) { analysis ->
                            EmotionAnalysisCard(
                                analysis = analysis,
                                title = formatEmotionTimestamp(analysis.createdAt),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionAnalysisCard(
    analysis: EmotionAnalysis,
    title: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("心情: ${analysis.labels.joinToString()}")
            MarkdownText(analysis.summary)
            AnalysisSection("触发点", analysis.triggers)
            AnalysisSection("认知模式", analysis.cognitivePatterns)
            AnalysisSection("深层需求", analysis.needs)
            AnalysisSection("关系线索", analysis.relationshipSignals)
            AnalysisSection("防御/应对", analysis.defenseMechanisms)
            AnalysisSection("资源优势", analysis.strengths)
            AnalysisSection("身体压力", analysis.bodyStressSignals)
            AnalysisSection("风险注意", analysis.riskNotes)
            analysis.suggestions.forEach { suggestion ->
                MarkdownText("• $suggestion")
            }
            if (analysis.safetyFlag) {
                Text("检测到高风险内容，请优先联系现实中的可信支持或专业帮助。")
            }
            Text(
                "分析于 ${formatEmotionTimestamp(analysis.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AgentProcessNavigator(
    events: List<PsychologyAgentProcessEvent>,
    onSelectAgent: (String) -> Unit,
) {
    val agents = events
        .filter { it.agentId != "runtime_result" }
        .distinctBy { it.agentId }
    if (agents.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        agents.forEach { event ->
            FilterChip(
                selected = false,
                onClick = { onSelectAgent(event.agentId) },
                label = { Text(event.agentName.removeSuffix(" Agent")) },
            )
        }
    }
}

@Composable
private fun AgentProcessEventCard(event: PsychologyAgentProcessEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("${event.agentName} · ${event.title}", style = MaterialTheme.typography.titleSmall)
            MarkdownText(event.contentMarkdown)
        }
    }
}

@Composable
private fun AnalysisSection(title: String, values: List<String>) {
    if (values.isNotEmpty()) {
        Text("$title: ${values.joinToString("；")}")
    }
}

private fun formatEmotionTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
