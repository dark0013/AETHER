package com.example.aether.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Lavender = Color(0xFFC4B5FD)
private val Background = Color(0xFF121212)

private val DarkColorScheme = darkColorScheme(
    primary = Lavender,
    secondary = Lavender,
    tertiary = Lavender,
    background = Background,
    surface = Background,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color.Gray
)

@Composable
fun AETHERTheme(
    darkTheme: Boolean = true, // Forzado a dark según el requerimiento "AETHER Look"
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
