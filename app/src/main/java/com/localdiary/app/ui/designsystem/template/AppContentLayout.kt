package com.localdiary.app.ui.designsystem.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.localdiary.app.ui.designsystem.molecule.AppEmptyState
import com.localdiary.app.ui.designsystem.molecule.AppErrorState
import com.localdiary.app.ui.designsystem.molecule.AppLoadingState
import com.localdiary.app.ui.designsystem.token.DiarySpacing

sealed class ContentState<out T> {
    data object Loading : ContentState<Nothing>()
    data class Error(val message: String) : ContentState<Nothing>()
    data class Empty(val message: String) : ContentState<Nothing>()
    data class Success<T>(val data: T) : ContentState<T>()
}

@Composable
fun <T> AppContentLayout(
    state: ContentState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    emptyTitle: String = "没有内容",
    emptyDescription: String = "",
    content: @Composable (T) -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is ContentState.Loading -> {
                AppLoadingState(modifier = Modifier.fillMaxSize())
            }
            is ContentState.Error -> {
                AppErrorState(
                    message = state.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is ContentState.Empty -> {
                if (emptyIcon != null) {
                    AppEmptyState(
                        icon = emptyIcon,
                        title = emptyTitle,
                        description = emptyDescription.ifBlank { state.message },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            is ContentState.Success -> {
                content(state.data)
            }
        }
    }
}
