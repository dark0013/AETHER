package com.example.aether.ui.skin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Seven built-in skins. Hex values come from docs/mockups/aether-skins.png
 * and docs/mockups/implementar-skins.md — do not invent palettes.
 */
object BuiltinSkins {

    val Default = Skin(
        id = SkinId.DEFAULT,
        name = "AETHER Default",
        tagline = "Moderna y elegante (Material 3)",
        colors = SkinColors(
            primary = Color(0xFFC4B5FD),
            secondary = Color(0xFFA78BFA),
            background = Color(0xFF121212),
            surface = Color(0xFF121212),
            onBackground = Color(0xFFF5F3FF),
            onSurface = Color(0xFFF5F3FF),
            accent = Color(0xFFE0D4FF),
            visualizerStart = Color(0xFFC4B5FD),
            visualizerEnd = Color(0xFF7C5CFF),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = Color(0xFFB0A8C8)
        ),
        fontName = "Space Grotesk",
        fontFamily = fontFamilyFor("Space Grotesk"),
        letterSpacing = letterSpacingFor("Space Grotesk"),
        buttonRadius = 18.dp,
        cardRadius = 16.dp,
        iconStyle = IconStyle.ROUNDED,
        background = SkinBackground(
            SkinBackgroundKind.VERTICAL_GRADIENT,
            Color(0xFF1A0B3A),
            Color(0xFF07040F)
        ),
        screenTransition = ScreenTransition.FADE,
        playButton = PlayButtonAnim.SCALE,
        visualizer = VisualizerStyle.BARS,
        barCount = 36,
        isDark = true,
        showStars = true,
        showMountains = true,
        ringWidth = 10.dp
    )

    val Neon = Skin(
        id = SkinId.NEON,
        name = "AETHER Neon",
        tagline = "Futurista y vibrante",
        colors = SkinColors(
            primary = Color(0xFFB85FFF),
            secondary = Color(0xFFFF80AA),
            background = Color(0xFF090014),
            surface = Color(0xFF140022),
            onBackground = Color(0xFFFFF0FF),
            onSurface = Color(0xFFFFF0FF),
            accent = Color(0xFFFF4FD8),
            visualizerStart = Color(0xFFFF4FD8),
            visualizerEnd = Color(0xFFB85FFF),
            surfaceVariant = Color(0xFF220033),
            onSurfaceVariant = Color(0xFFE0B0FF)
        ),
        fontName = "Space Grotesk",
        fontFamily = fontFamilyFor("Space Grotesk"),
        letterSpacing = 1.4.sp,
        buttonRadius = 18.dp,
        cardRadius = 24.dp,
        iconStyle = IconStyle.ROUNDED,
        background = SkinBackground(
            SkinBackgroundKind.VERTICAL_GRADIENT,
            Color(0xFF2A0050),
            Color(0xFF080010)
        ),
        screenTransition = ScreenTransition.FADE,
        playButton = PlayButtonAnim.PULSE,
        visualizer = VisualizerStyle.BARS,
        barCount = 40,
        isDark = true,
        showStars = true,
        showMountains = true,
        visualizerGlow = true,
        ringWidth = 12.dp
    )

    val Xp = Skin(
        id = SkinId.XP,
        name = "AETHER XP",
        tagline = "Clásica y nostálgica",
        colors = SkinColors(
            primary = Color(0xFF245EDC),
            secondary = Color(0xFF3D8BFF),
            background = Color(0xFF5BA3E8),
            surface = Color(0xFFE8F3FC),
            onBackground = Color(0xFF123056),
            onSurface = Color(0xFF123056),
            accent = Color(0xFFFFD24A),
            visualizerStart = Color(0xFF3D7EFF),
            visualizerEnd = Color(0xFF1E4FBF),
            surfaceVariant = Color(0xFFD2E6F8),
            onSurfaceVariant = Color(0xFF3A5A80)
        ),
        fontName = "Roboto",
        fontFamily = fontFamilyFor("Roboto"),
        letterSpacing = letterSpacingFor("Roboto"),
        buttonRadius = 12.dp,
        cardRadius = 12.dp,
        iconStyle = IconStyle.ROUNDED,
        background = SkinBackground(
            SkinBackgroundKind.VERTICAL_GRADIENT,
            Color(0xFF5BA3E8),
            Color(0xFFB8D4F0)
        ),
        screenTransition = ScreenTransition.SLIDE,
        playButton = PlayButtonAnim.SCALE,
        visualizer = VisualizerStyle.BARS,
        barCount = 28,
        isDark = false,
        showMountains = true,
        ringWidth = 10.dp
    )

    val Winamp = Skin(
        id = SkinId.WINAMP,
        name = "AETHER Winamp",
        tagline = "Retro y clásica",
        colors = SkinColors(
            primary = Color(0xFF39FF14),
            secondary = Color(0xFF7CFF00),
            background = Color(0xFF1A1A1A),
            surface = Color(0xFF2A2A2A),
            onBackground = Color(0xFFE8E8E8),
            onSurface = Color(0xFFE8E8E8),
            accent = Color(0xFF7CFF00),
            visualizerStart = Color(0xFF39FF14),
            visualizerEnd = Color(0xFF7CFF00),
            surfaceVariant = Color(0xFF333333),
            onSurfaceVariant = Color(0xFFAAAAAA)
        ),
        fontName = "Roboto Mono",
        fontFamily = fontFamilyFor("Roboto Mono"),
        letterSpacing = letterSpacingFor("Roboto Mono"),
        buttonRadius = 2.dp,
        cardRadius = 2.dp,
        iconStyle = IconStyle.SHARP,
        background = SkinBackground(
            SkinBackgroundKind.SOLID,
            Color(0xFF1A1A1A),
            Color(0xFF111111)
        ),
        screenTransition = ScreenTransition.FADE,
        playButton = PlayButtonAnim.NONE,
        visualizer = VisualizerStyle.CLASSIC_WINAMP,
        barCount = 48,
        isDark = true,
        showWinampChrome = true,
        showMountains = false,
        showStars = false,
        ringWidth = 8.dp
    )

    val Glass = Skin(
        id = SkinId.GLASS,
        name = "AETHER Glass",
        tagline = "Transparente y minimalista",
        colors = SkinColors(
            primary = Color(0xFF6B9BC3),
            secondary = Color(0xFF8BB8D8),
            background = Color(0xFFE8F1F8),
            surface = Color(0xFFF7FBFF),
            onBackground = Color(0xFF2A3A4A),
            onSurface = Color(0xFF2A3A4A),
            accent = Color(0xFF8BB8D8),
            visualizerStart = Color(0xFF8BB8D8),
            visualizerEnd = Color(0xFF6B9BC3),
            surfaceVariant = Color(0xFFD5E6F2),
            onSurfaceVariant = Color(0xFF5A7080)
        ),
        fontName = "Inter",
        fontFamily = fontFamilyFor("Inter"),
        letterSpacing = 0.2.sp,
        buttonRadius = 22.dp,
        cardRadius = 28.dp,
        iconStyle = IconStyle.ROUNDED,
        background = SkinBackground(
            SkinBackgroundKind.VERTICAL_GRADIENT,
            Color(0xFFC5DDF0),
            Color(0xFFE8F1F8)
        ),
        screenTransition = ScreenTransition.FADE,
        playButton = PlayButtonAnim.SCALE,
        visualizer = VisualizerStyle.WAVE,
        barCount = 24,
        isDark = false,
        glassmorphism = true,
        showMountains = true,
        ringWidth = 7.dp
    )

    val Minimal = Skin(
        id = SkinId.MINIMAL,
        name = "AETHER Minimal",
        tagline = "Simple y enfocada",
        colors = SkinColors(
            primary = Color(0xFF9A9A9A),
            secondary = Color(0xFF6A6A6A),
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            onBackground = Color(0xFFE6E6E6),
            onSurface = Color(0xFFE6E6E6),
            accent = Color(0xFF666666),
            visualizerStart = Color(0xFF666666),
            visualizerEnd = Color(0xFF444444),
            surfaceVariant = Color(0xFF111111),
            onSurfaceVariant = Color(0xFF888888)
        ),
        fontName = "Inter",
        fontFamily = fontFamilyFor("Inter"),
        letterSpacing = letterSpacingFor("Inter"),
        buttonRadius = 0.dp,
        cardRadius = 0.dp,
        iconStyle = IconStyle.SHARP,
        background = SkinBackground(
            SkinBackgroundKind.SOLID,
            Color(0xFF000000),
            Color(0xFF000000)
        ),
        screenTransition = ScreenTransition.FADE,
        playButton = PlayButtonAnim.NONE,
        visualizer = VisualizerStyle.NONE,
        barCount = 1,
        isDark = true,
        showMountains = false,
        showStars = false,
        ringWidth = 3.dp
    )

    val Cyberpunk = Skin(
        id = SkinId.CYBERPUNK,
        name = "AETHER Cyberpunk",
        tagline = "Oscura y poderosa",
        colors = SkinColors(
            primary = Color(0xFF00F0FF),
            secondary = Color(0xFFFF2BD6),
            background = Color(0xFF050008),
            surface = Color(0xFF120018),
            onBackground = Color(0xFFE6FFFF),
            onSurface = Color(0xFFE6FFFF),
            accent = Color(0xFFFF2BD6),
            visualizerStart = Color(0xFFFF2BD6),
            visualizerEnd = Color(0xFF00F0FF),
            surfaceVariant = Color(0xFF1A0028),
            onSurfaceVariant = Color(0xFF88E0E8)
        ),
        fontName = "Orbitron",
        fontFamily = fontFamilyFor("Orbitron"),
        letterSpacing = letterSpacingFor("Orbitron"),
        buttonRadius = 4.dp,
        cardRadius = 6.dp,
        iconStyle = IconStyle.SHARP,
        background = SkinBackground(
            SkinBackgroundKind.VERTICAL_GRADIENT,
            Color(0xFF1A0030),
            Color(0xFF050008)
        ),
        screenTransition = ScreenTransition.SLIDE,
        playButton = PlayButtonAnim.PULSE,
        visualizer = VisualizerStyle.BARS,
        barCount = 42,
        isDark = true,
        showScanlines = true,
        showStars = true,
        showMountains = true,
        visualizerGlow = true,
        ringWidth = 8.dp
    )

    val all: List<Skin> = listOf(Default, Neon, Xp, Winamp, Glass, Minimal, Cyberpunk)
}
