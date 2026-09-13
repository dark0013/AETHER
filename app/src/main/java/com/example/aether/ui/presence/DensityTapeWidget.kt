package com.example.aether.ui.presence

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.MarkEntity
import kotlin.math.max

@Composable
fun DensityTapeWidget(
    tape: DensityTapeEntity?,
    marks: List<MarkEntity> = emptyList(),
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    snapToOnsets: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val seekProgress = (offset.x / size.width) * duration
                    onSeek(seekProgress.toLong().coerceIn(0, duration))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val seekProgress = (change.position.x / size.width) * duration
                    onSeek(seekProgress.toLong().coerceIn(0, duration))
                }
            }
    ) {
        if (tape != null && tape.energyTape.isNotEmpty()) {
            drawRealTape(tape, primaryColor)
        } else {
            drawPlaceholderTape(onSurfaceColor.copy(alpha = 0.3f))
        }

        // Marks
        marks.forEach { mark ->
            val markX = if (duration > 0) (mark.positionMs.toFloat() / duration) * size.width else 0f
            drawRect(
                color = Color.Yellow.copy(alpha = 0.8f),
                topLeft = Offset(markX - 1.dp.toPx(), 0f),
                size = Size(2.dp.toPx(), size.height)
            )
        }

        // Playhead
        val playheadX = if (duration > 0) (progress.toFloat() / duration) * size.width else 0f
        drawLine(
            color = Color.White,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun DrawScope.drawRealTape(
    tape: DensityTapeEntity,
    color: Color
) {
    val frameCount = tape.frameCount
    if (frameCount <= 0 || tape.energyTape.isEmpty()) return

    val energyData = tape.energyTape
    val centroidData = tape.centroidTape
    val onsetData = tape.onsetFlags
    val columns = max(1, size.width.toInt())
    val barWidth = size.width / columns

    for (col in 0 until columns) {
        val start = (col.toLong() * frameCount / columns).toInt()
        val end = (((col + 1).toLong() * frameCount / columns).toInt()).coerceAtMost(frameCount)
        if (start >= energyData.size) break

        var maxEnergy = 0
        var centroidSum = 0
        var count = 0
        var hasOnset = false
        val last = max(start + 1, end)
        for (i in start until last.coerceAtMost(energyData.size)) {
            maxEnergy = max(maxEnergy, energyData[i].toInt() and 0xFF)
            centroidSum += centroidData.getOrElse(i) { 0 }.toInt() and 0xFF
            if (onsetData.getOrElse(i) { 0 }.toInt() == 1) hasOnset = true
            count++
        }
        if (count == 0) continue

        val energy = maxEnergy / 255f
        val centroid = (centroidSum / count) / 255f
        val barHeight = energy * size.height
        val x = col * barWidth

        drawRect(
            color = color.copy(alpha = 0.4f + (centroid * 0.6f)),
            topLeft = Offset(x, (size.height - barHeight) / 2),
            size = Size(barWidth.coerceAtLeast(1f), barHeight.coerceAtLeast(1f))
        )

        if (hasOnset) {
            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(barWidth.coerceAtLeast(1f), 4.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawPlaceholderTape(
    color: Color
) {
    val barHeight = size.height * 0.45f
    drawRect(
        color = color,
        topLeft = Offset(0f, (size.height - barHeight) / 2),
        size = Size(size.width, barHeight)
    )
}
