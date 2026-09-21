package com.example.aether.ui.skin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue

val LocalSkin = compositionLocalOf { Skin.Fallback }

@Composable
fun rememberPlayButtonScale(isPlaying: Boolean): Float {
    val skin = LocalSkin.current
    val infinite = rememberInfiniteTransition(label = "play_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    return when {
        !isPlaying -> 1f
        skin.playButton == PlayButtonAnim.PULSE -> pulse
        skin.playButton == PlayButtonAnim.SCALE -> 1.05f
        else -> 1f
    }
}
