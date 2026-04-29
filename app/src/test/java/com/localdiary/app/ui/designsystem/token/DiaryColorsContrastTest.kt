package com.localdiary.app.ui.designsystem.token

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryColorsContrastTest {
    @Test
    fun `foreground colors meet accessible contrast on their containers`() {
        val pairs = listOf(
            "light secondary" to (DiaryColors.Secondary to DiaryColors.OnSecondary),
            "light tertiary" to (DiaryColors.Tertiary to DiaryColors.OnTertiary),
            "light success on surface" to (DiaryColors.Success to DiaryColors.OnSurface),
            "dark primary" to (DiaryColorsDark.Primary to DiaryColorsDark.OnPrimary),
            "dark secondary" to (DiaryColorsDark.Secondary to DiaryColorsDark.OnSecondary),
            "dark tertiary" to (DiaryColorsDark.Tertiary to DiaryColorsDark.OnTertiary),
        )

        pairs.forEach { (name, colors) ->
            val ratio = contrastRatio(colors.first, colors.second)
            assertTrue("$name contrast was $ratio", ratio >= 4.5)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val lighter = maxOf(first.relativeLuminance(), second.relativeLuminance())
        val darker = minOf(first.relativeLuminance(), second.relativeLuminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun Color.relativeLuminance(): Double {
        fun channel(value: Float): Double {
            val normalized = value.toDouble()
            return if (normalized <= 0.04045) {
                normalized / 12.92
            } else {
                Math.pow((normalized + 0.055) / 1.055, 2.4)
            }
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }
}
