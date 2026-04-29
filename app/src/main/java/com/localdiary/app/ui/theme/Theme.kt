package com.localdiary.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.localdiary.app.ui.designsystem.token.DiaryColors
import com.localdiary.app.ui.designsystem.token.DiaryColorsDark
import com.localdiary.app.ui.designsystem.token.DiaryTypography

private val LightColors = lightColorScheme(
    primary = DiaryColors.Primary,
    onPrimary = DiaryColors.OnPrimary,
    primaryContainer = DiaryColors.PrimaryContainer,
    onPrimaryContainer = DiaryColors.OnPrimaryContainer,
    secondary = DiaryColors.Secondary,
    onSecondary = DiaryColors.OnSecondary,
    secondaryContainer = DiaryColors.SecondaryContainer,
    onSecondaryContainer = DiaryColors.OnSecondaryContainer,
    tertiary = DiaryColors.Tertiary,
    onTertiary = DiaryColors.OnTertiary,
    tertiaryContainer = DiaryColors.TertiaryContainer,
    onTertiaryContainer = DiaryColors.OnTertiaryContainer,
    background = DiaryColors.Background,
    onBackground = DiaryColors.OnBackground,
    surface = DiaryColors.Surface,
    onSurface = DiaryColors.OnSurface,
    surfaceVariant = DiaryColors.SurfaceVariant,
    onSurfaceVariant = DiaryColors.OnSurfaceVariant,
    surfaceContainer = DiaryColors.SurfaceContainer,
    surfaceContainerHigh = DiaryColors.SurfaceContainerHigh,
    surfaceContainerHighest = DiaryColors.SurfaceContainerHighest,
    error = DiaryColors.Error,
    onError = DiaryColors.OnError,
    errorContainer = DiaryColors.ErrorContainer,
    onErrorContainer = DiaryColors.OnErrorContainer,
    outline = DiaryColors.Outline,
    outlineVariant = DiaryColors.OutlineVariant,
    inverseSurface = DiaryColors.InverseSurface,
    inverseOnSurface = DiaryColors.InverseOnSurface,
    inversePrimary = DiaryColors.InversePrimary,
    scrim = DiaryColors.Scrim,
)

private val DarkColors = darkColorScheme(
    primary = DiaryColorsDark.Primary,
    onPrimary = DiaryColorsDark.OnPrimary,
    primaryContainer = DiaryColorsDark.PrimaryContainer,
    onPrimaryContainer = DiaryColorsDark.OnPrimaryContainer,
    secondary = DiaryColorsDark.Secondary,
    onSecondary = DiaryColorsDark.OnSecondary,
    secondaryContainer = DiaryColorsDark.SecondaryContainer,
    onSecondaryContainer = DiaryColorsDark.OnSecondaryContainer,
    tertiary = DiaryColorsDark.Tertiary,
    onTertiary = DiaryColorsDark.OnTertiary,
    tertiaryContainer = DiaryColorsDark.TertiaryContainer,
    onTertiaryContainer = DiaryColorsDark.OnTertiaryContainer,
    background = DiaryColorsDark.Background,
    onBackground = DiaryColorsDark.OnBackground,
    surface = DiaryColorsDark.Surface,
    onSurface = DiaryColorsDark.OnSurface,
    surfaceVariant = DiaryColorsDark.SurfaceVariant,
    onSurfaceVariant = DiaryColorsDark.OnSurfaceVariant,
    surfaceContainer = DiaryColorsDark.SurfaceContainer,
    surfaceContainerHigh = DiaryColorsDark.SurfaceContainerHigh,
    surfaceContainerHighest = DiaryColorsDark.SurfaceContainerHighest,
    error = DiaryColorsDark.Error,
    onError = DiaryColorsDark.OnError,
    errorContainer = DiaryColorsDark.ErrorContainer,
    onErrorContainer = DiaryColorsDark.OnErrorContainer,
    outline = DiaryColorsDark.Outline,
    outlineVariant = DiaryColorsDark.OutlineVariant,
    inverseSurface = DiaryColorsDark.InverseSurface,
    inverseOnSurface = DiaryColorsDark.InverseOnSurface,
    inversePrimary = DiaryColorsDark.InversePrimary,
    scrim = DiaryColorsDark.Scrim,
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
