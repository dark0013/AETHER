package com.example.aether.ui.skin

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.aether.ui.RealtimeAudioFeatures
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkinVisualizer(
    features: RealtimeAudioFeatures,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val skin = LocalSkin.current
    val infinite = rememberInfiniteTransition(label = "viz")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 2400 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val energy = if (isPlaying) features.energy else features.energy * 0.25f
    val flux = if (isPlaying) features.flux else 0f
    val onset = if (isPlaying && features.isOnset) 1f else 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSkinBackdrop(skin, energy)
        when (skin.visualizer) {
            VisualizerStyle.BARS, VisualizerStyle.PARTICLES ->
                drawBars(skin, energy, flux, onset, phase)
            VisualizerStyle.WAVE ->
                drawWave(skin, energy, flux, phase)
            VisualizerStyle.CIRCLE ->
                drawCircleBars(skin, energy, flux, phase)
            VisualizerStyle.CLASSIC_WINAMP ->
                drawClassicWinamp(skin, energy, flux, phase)
            VisualizerStyle.NONE ->
                drawMinimalLine(skin, energy)
        }
        if (skin.showScanlines) drawScanlines()
        if (skin.showWinampChrome) drawWinampChrome(skin)
    }
}

private fun DrawScope.drawSkinBackdrop(skin: Skin, energy: Float) {
    val bg = skin.background
    when (bg.kind) {
        SkinBackgroundKind.SOLID -> drawRect(bg.top)
        SkinBackgroundKind.VERTICAL_GRADIENT,
        SkinBackgroundKind.IMAGE_ASSET ->
            drawRect(brush = Brush.verticalGradient(listOf(bg.top, bg.bottom)))
    }
    if (skin.glassmorphism) drawGlassHaze(skin)
    if (skin.showStars) drawStars()
    if (skin.showScanlines) drawCyberGrid(skin)
    if (skin.showMountains) drawMountains(skin)
    if (skin.showWinampChrome) drawWinampBezel(skin)
    if (skin.showHeroRing) drawHeroRing(skin, energy)
}

private fun DrawScope.drawGlassHaze(skin: Skin) {
    drawCircle(
        color = Color.White.copy(alpha = 0.22f),
        radius = size.minDimension * 0.42f,
        center = Offset(size.width * 0.5f, size.height * 0.34f)
    )
    drawCircle(
        color = skin.colors.secondary.copy(alpha = 0.12f),
        radius = size.minDimension * 0.28f,
        center = Offset(size.width * 0.72f, size.height * 0.18f)
    )
    drawRect(Color.White.copy(alpha = 0.08f))
}

private fun DrawScope.drawStars() {
    val w = size.width
    val h = size.height * 0.62f
    var seed = 1337
    repeat(56) {
        seed = seed * 1103515245 + 12345
        val x = ((seed ushr 16) and 0x7FFF) / 32767f * w
        seed = seed * 1103515245 + 12345
        val y = ((seed ushr 16) and 0x7FFF) / 32767f * h
        val a = 0.18f + ((seed ushr 8) and 0xFF) / 255f * 0.55f
        val r = 1.1f + (seed and 3).toFloat()
        drawCircle(Color.White.copy(alpha = a), radius = r, center = Offset(x, y))
    }
}

private fun DrawScope.drawCyberGrid(skin: Skin) {
    val color = skin.colors.primary.copy(alpha = 0.14f)
    val accent = skin.colors.secondary.copy(alpha = 0.08f)
    val horizon = size.height * 0.58f
    var x = 0f
    val step = 26.dp.toPx()
    while (x < size.width) {
        drawLine(color, Offset(x, horizon), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        x += step
    }
    var y = horizon
    var row = 0
    while (y < size.height) {
        drawLine(
            if (row % 2 == 0) color else accent,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        y += step * (0.55f + (y - horizon) / size.height)
        row++
    }
}

private fun DrawScope.drawMountains(skin: Skin) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.72f)
        quadraticBezierTo(w * 0.18f, h * 0.58f, w * 0.32f, h * 0.68f)
        quadraticBezierTo(w * 0.48f, h * 0.78f, w * 0.62f, h * 0.64f)
        quadraticBezierTo(w * 0.78f, h * 0.52f, w, h * 0.66f)
        lineTo(w, h)
        close()
    }
    val fill = if (skin.isDark) {
        Color.Black.copy(alpha = 0.62f)
    } else {
        Color(0xFF3A6A9A).copy(alpha = 0.28f)
    }
    drawPath(path, fill)
    if (skin.isDark) {
        val water = Path().apply {
            moveTo(0f, h * 0.82f)
            quadraticBezierTo(w * 0.5f, h * 0.78f, w, h * 0.84f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(water, skin.colors.primary.copy(alpha = 0.08f))
    }
}

private fun DrawScope.drawWinampBezel(skin: Skin) {
    val left = size.width * 0.07f
    val top = size.height * 0.11f
    val w = size.width * 0.86f
    val h = size.height * 0.62f
    val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    drawRoundRect(
        color = Color(0xFF3A3A3A),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = radius
    )
    drawRoundRect(
        color = Color(0xFF121212),
        topLeft = Offset(left + 5.dp.toPx(), top + 5.dp.toPx()),
        size = Size(w - 10.dp.toPx(), h * 0.72f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFF0A0A0A),
        topLeft = Offset(left + 10.dp.toPx(), top + h * 0.58f),
        size = Size(w - 20.dp.toPx(), h * 0.22f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
}

private fun DrawScope.drawHeroRing(skin: Skin, energy: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.36f
    val r = size.minDimension * (0.22f + energy * 0.04f)
    val glowAlpha = if (skin.visualizerGlow) 0.55f else if (skin.glassmorphism) 0.22f else 0.34f
    val glow = Brush.radialGradient(
        colors = listOf(
            skin.colors.secondary.copy(alpha = glowAlpha),
            skin.colors.primary.copy(alpha = 0.0f)
        ),
        center = Offset(cx, cy),
        radius = r * 1.8f
    )
    drawCircle(brush = glow, radius = r * 1.65f, center = Offset(cx, cy))
    val ringColors = listOf(
        skin.colors.secondary,
        skin.colors.visualizerStart,
        skin.colors.primary,
        skin.colors.visualizerEnd,
        skin.colors.secondary
    )
    drawCircle(
        brush = Brush.sweepGradient(ringColors, center = Offset(cx, cy)),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = skin.ringWidth.toPx())
    )
    if (!skin.id.let { it == SkinId.MINIMAL }) {
        val helmetFill = if (skin.isDark) Color.Black.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.45f)
        drawCircle(color = helmetFill, radius = r * 0.78f, center = Offset(cx, cy))
    }
    drawAstronaut(skin, Offset(cx, cy), r * 0.72f)
}

private fun DrawScope.drawAstronaut(skin: Skin, center: Offset, radius: Float) {
    val helmet = if (skin.isDark) Color(0xFF1A1228) else Color(0xFFEEF4FA)
    val visor = Brush.verticalGradient(
        if (skin.isDark) listOf(Color(0xFF2A1848), Color(0xFF0A0614))
        else listOf(Color(0xFF5A88B8), Color(0xFF1E3A5A)),
        startY = center.y - radius,
        endY = center.y + radius
    )
    drawCircle(color = helmet, radius = radius, center = center)
    drawCircle(
        brush = visor,
        radius = radius * 0.62f,
        center = Offset(center.x, center.y - radius * 0.04f)
    )
    drawCircle(
        color = skin.colors.primary.copy(alpha = 0.4f),
        radius = radius * 0.18f,
        center = Offset(center.x - radius * 0.18f, center.y - radius * 0.16f)
    )
}

private fun DrawScope.drawBars(
    skin: Skin,
    energy: Float,
    flux: Float,
    onset: Float,
    phase: Float
) {
    val count = skin.barCount.coerceAtLeast(8)
    val w = size.width * 0.72f
    val left = (size.width - w) / 2f
    val baseY = size.height * 0.70f
    val maxH = size.height * 0.16f
    val gap = 3.dp.toPx()
    val barW = ((w - gap * (count - 1)) / count).coerceAtLeast(2f)
    for (i in 0 until count) {
        val n = i / count.toFloat()
        val wobble = (0.35f + 0.65f * absSin(phase + n * 6.2f))
        val h = maxH * (0.12f + energy * wobble + flux * 0.25f + onset * 0.12f)
        val x = left + i * (barW + gap)
        val start = lerp(skin.colors.visualizerStart, skin.colors.visualizerEnd, n)
        val end = lerp(skin.colors.visualizerEnd, skin.colors.visualizerStart, n)
        if (skin.visualizerGlow) {
            drawRect(
                color = start.copy(alpha = 0.28f),
                topLeft = Offset(x - 2.dp.toPx(), baseY - h - 4.dp.toPx()),
                size = Size(barW + 4.dp.toPx(), h + 8.dp.toPx())
            )
        }
        drawRect(
            brush = Brush.verticalGradient(listOf(start, end)),
            topLeft = Offset(x, baseY - h),
            size = Size(barW, h)
        )
        if (skin.isDark) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(start.copy(alpha = 0.22f), Color.Transparent)
                ),
                topLeft = Offset(x, baseY),
                size = Size(barW, h * 0.45f)
            )
        }
    }
}

private fun DrawScope.drawWave(skin: Skin, energy: Float, flux: Float, phase: Float) {
    val path = Path()
    val midY = size.height * 0.70f
    val amp = size.height * 0.055f * (0.2f + energy * 1.4f + flux)
    val steps = 48
    for (i in 0..steps) {
        val x = size.width * i / steps
        val y = midY + sin(phase + i * 0.35f) * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path,
        skin.colors.visualizerStart,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
    val echo = Path()
    for (i in 0..steps) {
        val x = size.width * i / steps
        val y = midY + sin(phase + i * 0.35f + 0.6f) * amp * 0.45f
        if (i == 0) echo.moveTo(x, y) else echo.lineTo(x, y)
    }
    drawPath(
        echo,
        skin.colors.visualizerEnd.copy(alpha = 0.45f),
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCircleBars(skin: Skin, energy: Float, flux: Float, phase: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.36f
    val count = skin.barCount.coerceAtLeast(16)
    val inner = size.minDimension * 0.24f
    for (i in 0 until count) {
        val ang = (i / count.toFloat()) * PI.toFloat() * 2f + phase * 0.15f
        val len = inner * (0.15f + energy * (0.4f + 0.4f * absSin(phase + i)))
        val x1 = cx + cos(ang) * inner
        val y1 = cy + sin(ang) * inner
        val x2 = cx + cos(ang) * (inner + len)
        val y2 = cy + sin(ang) * (inner + len)
        drawLine(skin.colors.visualizerStart, Offset(x1, y1), Offset(x2, y2), strokeWidth = 3.dp.toPx())
    }
}

private fun DrawScope.drawClassicWinamp(skin: Skin, energy: Float, flux: Float, phase: Float) {
    val count = skin.barCount.coerceAtLeast(24)
    val w = size.width * 0.78f
    val left = (size.width - w) / 2f
    val baseY = size.height * 0.54f
    val maxH = size.height * 0.12f
    val barW = w / count
    for (i in 0 until count) {
        val n = i / count.toFloat()
        val h = maxH * (0.08f + energy * (0.4f + 0.6f * absSin(phase * 2f + n * 9f)) + flux * 0.3f)
        val segments = 10
        val segH = (h / segments).coerceAtLeast(2f)
        for (s in 0 until segments) {
            val y = baseY - (s + 1) * segH
            val t = s / segments.toFloat()
            val color = lerp(skin.colors.visualizerEnd, skin.colors.visualizerStart, t)
            drawRect(color, Offset(left + i * barW + 1f, y), Size(barW - 2f, segH - 1f))
        }
    }
}

private fun DrawScope.drawMinimalLine(skin: Skin, energy: Float) {
    val y = size.height * 0.70f
    val w = size.width * (0.2f + energy * 0.5f)
    drawLine(
        color = skin.colors.visualizerStart,
        start = Offset((size.width - w) / 2f, y),
        end = Offset((size.width + w) / 2f, y),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawScanlines() {
    val step = 4.dp.toPx()
    var y = 0f
    while (y < size.height) {
        drawRect(Color.Black.copy(alpha = 0.12f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
        y += step
    }
}

private fun DrawScope.drawWinampChrome(skin: Skin) {
    val top = size.height * 0.66f
    val cx = size.width / 2f
    val btnY = top + 10.dp.toPx()
    drawCircle(skin.colors.primary, 9.dp.toPx(), Offset(cx, btnY))
    drawCircle(Color(0xFF555555), 7.dp.toPx(), Offset(cx - 36.dp.toPx(), btnY))
    drawCircle(Color(0xFF555555), 7.dp.toPx(), Offset(cx + 36.dp.toPx(), btnY))
}

private fun absSin(v: Float): Float = kotlin.math.abs(sin(v))

private fun lerp(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u
    )
}
