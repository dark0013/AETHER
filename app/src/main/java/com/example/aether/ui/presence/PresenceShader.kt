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
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslPresenceVisualizer(features, isPlaying, modifier)
    } else {
        FallbackPresenceVisualizer(features, isPlaying, modifier)
    }
}

private const val PRESENCE_SHADER_CODE = """
    uniform float2 uSize;
    uniform float uTime;
    uniform float uEnergy;
    uniform float uCentroid;
    uniform float uFlux;
    uniform float uOnset;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uSize;
        float dist = distance(uv, float2(0.5, 0.5));

        float3 baseColor = float3(0.5, 0.4, 0.9) * (0.8 + uEnergy * 0.4);

        float wave = sin(dist * 15.0 - uTime * (1.5 + uFlux * 4.0)) * 0.1;
        float pulse = uOnset * 0.15;

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
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(PRESENCE_SHADER_CODE) }
    var time by remember { mutableFloatStateOf(0f) }
    var smoothedOnset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying, features.isOnset) {
        if (isPlaying && features.isOnset) smoothedOnset = 1f
        if (!isPlaying) smoothedOnset = 0f
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val startNs = withFrameNanos { it }
        val baseTime = time
        while (true) {
            val now = withFrameNanos { it }
            time = baseTime + ((now - startNs) / 1_000_000_000f)
            if (smoothedOnset > 0f) {
                smoothedOnset = (smoothedOnset - 0.08f).coerceAtLeast(0f)
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
        drawRect(brush = ShaderBrush(shader))
    }
}

@Composable
private fun FallbackPresenceVisualizer(
    features: RealtimeAudioFeatures,
    isPlaying: Boolean,
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

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFC4B5FD).copy(alpha = 0.5f + animatedEnergy * 0.3f),
                    Color.Transparent
                ),
                center = center,
                radius = radius.coerceAtLeast(1f)
            )
        )
    }
}
