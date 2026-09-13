package com.example.aether.ui.presence

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.example.aether.ui.RealtimeAudioFeatures

@Composable
fun PresenceVisualizer(
    features: RealtimeAudioFeatures,
    isPlaying: Boolean,
    isRitual: Boolean = false,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslPresenceVisualizer(features, isPlaying, isRitual, progress, modifier)
    } else {
        FallbackPresenceVisualizer(features, isPlaying, isRitual, modifier)
    }
}

private const val PRESENCE_SHADER_CODE = """
    uniform float2 uSize;
    uniform float uTime;
    uniform float uEnergy;
    uniform float uCentroid;
    uniform float uFlux;
    uniform float uOnset;
    uniform float uRitual;
    uniform float uProgress;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uSize;
        float dist = distance(uv, float2(0.5, 0.5));

        float3 presenceTint = float3(0.5, 0.4, 0.9);
        float3 ritualTint = float3(0.78, 0.38, 0.22);
        float3 tint = mix(presenceTint, ritualTint, uRitual);
        float dim = 1.0 - uRitual * 0.22;
        float drive = clamp(uEnergy + uFlux, 0.0, 1.0);
        float3 baseColor = tint * (0.55 + uEnergy * 0.55) * dim;

        float wave = sin(dist * 15.0 - uTime * (0.4 + uFlux * 4.0) - uProgress * 6.28) * 0.1 * drive;
        float pulse = uOnset * 0.15 * (1.0 - uRitual * 0.4);

        float mask = smoothstep(0.45 + pulse + wave, 0.2 + wave, dist);
        float3 finalColor = baseColor * mask;

        return half4(finalColor, 1.0);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslPresenceVisualizer(
    features: RealtimeAudioFeatures,
    isPlaying: Boolean,
    isRitual: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(PRESENCE_SHADER_CODE) }
    var time by remember { mutableFloatStateOf(0f) }
    var smoothedOnset by remember { mutableFloatStateOf(0f) }
    val pendingOnset = remember { booleanArrayOf(false) }

    LaunchedEffect(isPlaying, features.isOnset) {
        if (isPlaying && features.isOnset) pendingOnset[0] = true
        if (!isPlaying) {
            pendingOnset[0] = false
            smoothedOnset = 0f
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val startNs = withFrameNanos { it }
        val baseTime = time
        var onsetStartNs = 0L
        while (true) {
            val now = withFrameNanos { it }
            time = baseTime + ((now - startNs) / 1_000_000_000f)
            if (pendingOnset[0]) {
                pendingOnset[0] = false
                onsetStartNs = now
                smoothedOnset = 1f
            } else if (onsetStartNs > 0L) {
                val elapsedMs = (now - onsetStartNs) / 1_000_000.0
                smoothedOnset = (1.0 - elapsedMs / 180.0).toFloat().coerceIn(0f, 1f)
                if (smoothedOnset <= 0f) onsetStartNs = 0L
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTime", time)
        shader.setFloatUniform("uEnergy", if (isPlaying) features.energy else features.energy * 0.35f)
        shader.setFloatUniform("uCentroid", features.centroid)
        shader.setFloatUniform("uFlux", if (isPlaying) features.flux else 0f)
        shader.setFloatUniform("uOnset", if (isPlaying) smoothedOnset else 0f)
        shader.setFloatUniform("uRitual", if (isRitual) 1f else 0f)
        shader.setFloatUniform("uProgress", progress.coerceIn(0f, 1f))
        drawRect(brush = ShaderBrush(shader))
    }
}

@Composable
private fun FallbackPresenceVisualizer(
    features: RealtimeAudioFeatures,
    isPlaying: Boolean,
    isRitual: Boolean,
    modifier: Modifier = Modifier
) {
    val targetEnergy = if (isPlaying) features.energy else features.energy * 0.35f
    val animatedEnergy by animateFloatAsState(
        targetValue = targetEnergy,
        animationSpec = tween(if (isPlaying) 100 else 400),
        label = "energy"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.minDimension / 2) * (0.6f + animatedEnergy * 0.5f)

        val core = if (isRitual) Color(0xFFC45C38) else Color(0xFFC4B5FD)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    core.copy(alpha = (if (isRitual) 0.38f else 0.5f) + animatedEnergy * 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = radius.coerceAtLeast(1f) * if (isRitual) 0.85f else 1f
            )
        )
    }
}
