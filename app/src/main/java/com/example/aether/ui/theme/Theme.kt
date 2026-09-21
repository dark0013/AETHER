package com.example.aether.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.aether.ui.skin.Skin
import com.example.aether.ui.skin.Skin.Companion.Fallback

@Composable
fun AETHERTheme(
    skin: Skin = Fallback,
    content: @Composable () -> Unit
) {
    val c = skin.colors
    val colorScheme = if (skin.isDark) {
        darkColorScheme(
            primary = c.primary,
            secondary = c.secondary,
            tertiary = c.accent,
            background = c.background,
            surface = c.surface,
            onPrimary = if (c.primary.luminance() > 0.5f) Color.Black else Color.White,
            onSecondary = if (c.secondary.luminance() > 0.5f) Color.Black else Color.White,
            onTertiary = if (c.accent.luminance() > 0.5f) Color.Black else Color.White,
            onBackground = c.onBackground,
            onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.onSurfaceVariant,
            outline = c.onSurfaceVariant.copy(alpha = 0.45f)
        )
    } else {
        lightColorScheme(
            primary = c.primary,
            secondary = c.secondary,
            tertiary = c.accent,
            background = c.background,
            surface = c.surface,
            onPrimary = if (c.primary.luminance() > 0.5f) Color.Black else Color.White,
            onSecondary = if (c.secondary.luminance() > 0.5f) Color.Black else Color.White,
            onTertiary = if (c.accent.luminance() > 0.5f) Color.Black else Color.White,
            onBackground = c.onBackground,
            onSurface = c.onSurface,
            surfaceVariant = c.surfaceVariant,
            onSurfaceVariant = c.onSurfaceVariant,
            outline = c.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }

    val shapes = Shapes(
        extraSmall = RoundedCornerShape((skin.buttonRadius.value / 2).dp),
        small = RoundedCornerShape(skin.buttonRadius),
        medium = RoundedCornerShape(skin.cardRadius),
        large = RoundedCornerShape(skin.cardRadius),
        extraLarge = RoundedCornerShape((skin.cardRadius.value + 8).dp)
    )

    val base = MaterialTheme.typography
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing),
        headlineLarge = base.headlineLarge.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing),
        bodyLarge = base.bodyLarge.copy(fontFamily = skin.fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = skin.fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = skin.fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing),
        labelMedium = base.labelMedium.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing),
        labelSmall = base.labelSmall.copy(fontFamily = skin.fontFamily, letterSpacing = skin.letterSpacing)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
