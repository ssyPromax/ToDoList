package com.dswork.list.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 配色来自 dswork-suite 设计令牌（`suite/design/tokens.json`），
 * 由 gen.py 生成到 DsTokens.kt。本文件只负责"令牌 -> Material3 主题"的映射，
 * 不再硬编码色值；改颜色请改 tokens.json 后重新生成。
 */
typealias ListPalette = DsPalette

val LightPalette = DsLightPalette
val DarkPalette = DsDarkPalette

val LocalListPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ListTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
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
            onPrimary = palette.onAccent,
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
