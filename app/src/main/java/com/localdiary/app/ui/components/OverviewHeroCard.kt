package com.localdiary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OverviewHeroCard(
    title: String,
    subtitle: String,
    actions: @Composable RowScope.() -> Unit,
    stats: @Composable RowScope.() -> Unit,
    expandedContent: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = stats)
            expandedContent?.invoke()
        }
    }
}

@Composable
fun OverviewHeroChip(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
