package com.localdiary.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localdiary.app.domain.psychology.PsychologyAgentCatalog

@Composable
fun PsychologyAgentSelector(
    selectedAgentId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedAgentId == null,
            onClick = { onSelect(null) },
            label = { Text("全部 Agent") },
        )
        PsychologyAgentCatalog.defaultAgents.forEach { agent ->
            FilterChip(
                selected = selectedAgentId == agent.id,
                onClick = { onSelect(agent.id) },
                label = { Text(agent.displayName.removeSuffix(" Agent")) },
            )
        }
    }
}
