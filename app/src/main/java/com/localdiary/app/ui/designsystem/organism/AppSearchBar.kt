package com.localdiary.app.ui.designsystem.organism

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localdiary.app.ui.designsystem.molecule.AppIconButton

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (expanded) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { androidx.compose.material3.Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onToggleExpand) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    } else {
        AppIconButton(
            onClick = onToggleExpand,
            imageVector = Icons.Filled.Search,
            contentDescription = "搜索",
            modifier = modifier,
        )
    }
}
