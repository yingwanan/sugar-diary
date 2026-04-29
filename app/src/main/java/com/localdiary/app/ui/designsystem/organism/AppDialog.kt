package com.localdiary.app.ui.designsystem.organism

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = { onDismissRequest?.invoke() ?: onDismiss?.invoke() },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = if (dismissText != null) {
            {
                TextButton(onClick = { onDismiss?.invoke() }) {
                    Text(dismissText, style = MaterialTheme.typography.labelLarge)
                }
            }
        } else null,
        modifier = modifier,
    )
}
