package com.instamaker.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** رنگ‌ها: بژِ سنگی، کرم، قهوه‌ای برنزی و طلایی؛ الهام‌گرفته از تصویر نمونه */
object Palette {
    val Bg = Color(0xFFEFE9E1)
    val BgTop = Color(0xFFF5F0E9)
    val BgBottom = Color(0xFFE4DACD)
    val Cream = Color(0xFFFAF8F5)
    val Sand = Color(0xFFF0E9DF)
    val Gold = Color(0xFFB8832A)
    val GoldLight = Color(0xFFD7A64E)
    val Bronze = Color(0xFF6A4B33)
    val BronzeLight = Color(0xFF8B6444)
    val Ink = Color(0xFF24201C)
    val Muted = Color(0xFF8C8175)
    val Line = Color(0xFFDDD3C6)
}

val AppFont = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
)

private val InstaScheme = lightColorScheme(
    primary = Palette.Gold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E3C4),
    onPrimaryContainer = Palette.Bronze,
    secondary = Palette.Bronze,
    onSecondary = Color.White,
    background = Palette.Bg,
    onBackground = Palette.Ink,
    surface = Palette.Cream,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Sand,
    onSurfaceVariant = Palette.Muted,
    outline = Palette.Line,
    outlineVariant = Palette.Line
)

private fun appTypography(): Typography {
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = AppFont),
        displayMedium = d.displayMedium.copy(fontFamily = AppFont),
        displaySmall = d.displaySmall.copy(fontFamily = AppFont),
        headlineLarge = d.headlineLarge.copy(fontFamily = AppFont),
        headlineMedium = d.headlineMedium.copy(fontFamily = AppFont),
        headlineSmall = d.headlineSmall.copy(fontFamily = AppFont),
        titleLarge = d.titleLarge.copy(fontFamily = AppFont),
        titleMedium = d.titleMedium.copy(fontFamily = AppFont),
        titleSmall = d.titleSmall.copy(fontFamily = AppFont),
        bodyLarge = d.bodyLarge.copy(fontFamily = AppFont),
        bodyMedium = d.bodyMedium.copy(fontFamily = AppFont),
        bodySmall = d.bodySmall.copy(fontFamily = AppFont),
        labelLarge = d.labelLarge.copy(fontFamily = AppFont),
        labelMedium = d.labelMedium.copy(fontFamily = AppFont),
        labelSmall = d.labelSmall.copy(fontFamily = AppFont)
    )
}

@Composable
fun InstaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = InstaScheme, typography = appTypography(), content = content)
}
