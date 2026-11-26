package com.dswork.list.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 应用自己的配色，深浅两套。 */
data class ListPalette(
    val background: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val selectedCard: Color,
    val onSelected: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
)

val LightPalette = ListPalette(
    background = Color(0xFFF3F1F8),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFE4DFEC),
    textPrimary = Color(0xFF1F1B24),
    textSecondary = Color(0xFF7B7488),
    selectedCard = Color(0xFF2E2620),
    onSelected = Color(0xFFF7F3EC),
    accent = Color(0xFF3F8F4F),
    accentSoft = Color(0xFFE4F2E6),
    danger = Color(0xFFC2504A),
)

val DarkPalette = ListPalette(
    background = Color(0xFF121016),
    card = Color(0xFF1D1924),
    border = Color(0xFF2E2938),
    textPrimary = Color(0xFFEFEBF5),
    textSecondary = Color(0xFF9C94AA),
    selectedCard = Color(0xFFEDE7F3),
    onSelected = Color(0xFF1B1620),
    accent = Color(0xFF6BC47E),
    accentSoft = Color(0xFF23331F),
    danger = Color(0xFFE27C74),
)

val LocalListPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ListTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color(0xFF10240F),
            secondary = palette.accent,
            secondaryContainer = palette.accentSoft,
            onSecondaryContainer = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            secondary = palette.accent,
            secondaryContainer = palette.accentSoft,
            onSecondaryContainer = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalListPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
