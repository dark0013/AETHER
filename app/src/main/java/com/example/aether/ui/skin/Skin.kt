package com.example.aether.ui.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SkinBackgroundKind { SOLID, VERTICAL_GRADIENT, IMAGE_ASSET }

enum class ScreenTransition { FADE, SLIDE }

enum class PlayButtonAnim { PULSE, SCALE, NONE }

enum class IconStyle { MATERIAL, ROUNDED, SHARP }

data class SkinColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onBackground: Color,
    val onSurface: Color,
    val accent: Color,
    val visualizerStart: Color,
    val visualizerEnd: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color
)

data class SkinBackground(
    val kind: SkinBackgroundKind,
    val top: Color,
    val bottom: Color,
    val imageAsset: String? = null
)

data class Skin(
    val id: SkinId,
    val name: String,
    val tagline: String,
    val colors: SkinColors,
    val fontName: String,
    val fontFamily: FontFamily,
    val letterSpacing: TextUnit,
    val buttonRadius: Dp,
    val cardRadius: Dp,
    val iconStyle: IconStyle,
    val background: SkinBackground,
    val screenTransition: ScreenTransition,
    val playButton: PlayButtonAnim,
    val visualizer: VisualizerStyle,
    val barCount: Int,
    val isDark: Boolean,
    val glassmorphism: Boolean = false,
    val showHeroRing: Boolean = true,
    val showWinampChrome: Boolean = false,
    val showScanlines: Boolean = false,
    val showMountains: Boolean = true,
    val showStars: Boolean = false,
    val visualizerGlow: Boolean = false,
    val ringWidth: Dp = 10.dp
) {
    companion object {
        val Fallback: Skin get() = BuiltinSkins.Default
    }
}

internal fun fontFamilyFor(name: String): FontFamily = when (name) {
    "Roboto Mono" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

internal fun letterSpacingFor(name: String): TextUnit = when (name) {
    "Orbitron" -> 1.8.sp
    "Space Grotesk" -> 0.8.sp
    "Montserrat" -> 0.4.sp
    "Inter" -> 0.sp
    "Roboto" -> 0.1.sp
    "Roboto Mono" -> 0.sp
    else -> 0.4.sp
}
