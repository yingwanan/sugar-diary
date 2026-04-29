package com.localdiary.app.ui.designsystem.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.localdiary.app.ui.designsystem.atom.AppText
import com.localdiary.app.ui.designsystem.token.DiarySpacing

@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "出错了",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .padding(DiarySpacing.space6)
            .semantics {
                stateDescription = "$title: $message"
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(DiarySpacing.space2))
        AppText(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(DiarySpacing.space5))
            Button(onClick = onRetry) {
                AppText(text = "重试", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
