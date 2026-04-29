package com.localdiary.app.ui.designsystem.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.localdiary.app.ui.designsystem.atom.AppText
import com.localdiary.app.ui.designsystem.token.DiarySpacing

enum class StatusType {
    Info, Success, Warning, Error
}

@Composable
fun AppStatusBanner(
    title: String,
    message: String,
    type: StatusType,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val containerColor = when (type) {
        StatusType.Info -> MaterialTheme.colorScheme.secondaryContainer
        StatusType.Success -> MaterialTheme.colorScheme.primaryContainer
        StatusType.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        StatusType.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (type) {
        StatusType.Info -> MaterialTheme.colorScheme.onSecondaryContainer
        StatusType.Success -> MaterialTheme.colorScheme.onPrimaryContainer
        StatusType.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        StatusType.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        shape = RoundedCornerShape(DiarySpacing.space3),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(DiarySpacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.space3),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
                AppText(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "关闭提示",
                        tint = contentColor,
                    )
                }
            }
        }
    }
}
