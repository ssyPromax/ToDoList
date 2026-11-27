// GENERATED FILE - DO NOT EDIT.
// 来源: suite/design/tokens.json
// 重新生成: python D:/.dswork/App/suite/design/gen.py
package com.dswork.list.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 各 app 共用的语义配色。字段名由 apps.json 决定，值来自 tokens.json。 */
data class DsPalette(
    val background: Color,
    val card: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val selectedCard: Color,
    val onSelected: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
)

data class DsText(val size: androidx.compose.ui.unit.TextUnit, val weight: FontWeight)

val DsLightPalette = DsPalette(
    background = Color(0xFFF3F1F8),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFE4DFEC),
    textPrimary = Color(0xFF1F1B24),
    textSecondary = Color(0xFF7B7488),
    selectedCard = Color(0xFF2E2620),
    onSelected = Color(0xFFF7F3EC),
    accent = Color(0xFF3F8F4F),
    accentSoft = Color(0xFFE4F2E6),
    onAccent = Color(0xFFFFFFFF),
    danger = Color(0xFFC2504A),
)

val DsDarkPalette = DsPalette(
    background = Color(0xFF121016),
    card = Color(0xFF1D1924),
    border = Color(0xFF2E2938),
    textPrimary = Color(0xFFEFEBF5),
    textSecondary = Color(0xFF9C94AA),
    selectedCard = Color(0xFFEDE7F3),
    onSelected = Color(0xFF1B1620),
    accent = Color(0xFF6BC47E),
    accentSoft = Color(0xFF23331F),
    onAccent = Color(0xFF10240F),
    danger = Color(0xFFE27C74),
)

object DsRadius {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 16.dp
    val xl = 18.dp
    val xxl = 22.dp
    val dialog = 28.dp
}

object DsSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object DsType {
    val caption = DsText(11.sp, FontWeight(400))
    val label = DsText(12.sp, FontWeight(500))
    val body = DsText(13.sp, FontWeight(400))
    val subtitle = DsText(15.sp, FontWeight(500))
    val title = DsText(19.sp, FontWeight(600))
    val headline = DsText(30.sp, FontWeight(600))
    val display = DsText(38.sp, FontWeight(700))
}
