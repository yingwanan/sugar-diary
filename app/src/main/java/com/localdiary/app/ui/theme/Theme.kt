package com.localdiary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = InkBlue,
    secondary = Coral,
    tertiary = Sage,
    background = Paper,
    surface = Paper,
    surfaceVariant = Sand,
    secondaryContainer = Sand,
)

private val DarkColors = darkColorScheme(
    primary = InkBlueDark,
    secondary = CoralDark,
    tertiary = SageDark,
    background = PaperDark,
    surface = Slate,
    surfaceVariant = InkBlue,
    secondaryContainer = InkBlue,
)

@Composable
fun DiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiaryTypography,
        content = content,
    )
}
