package com.example.aether.ui.presence

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal object PresenceGestureMath {
    fun isPinchScale(scale: Float): Boolean = scale < 0.88f || scale > 1.15f

    fun isVerticalDominant(pan: Offset): Boolean =
        abs(pan.y) > abs(pan.x) * 1.15f

    fun isHorizontalFlick(pan: Offset, minDistancePx: Float): Boolean =
        abs(pan.x) >= minDistancePx && abs(pan.x) > abs(pan.y)

    /** Right-to-left swipe (finger moves left), like turning a page forward. */
    fun isFlickToNext(pan: Offset): Boolean = pan.x < 0f
}

private sealed class GestureStart {
    data object Released : GestureStart()
    data class Pinched(val distance: Float) : GestureStart()
    data class Moved(val pan: Offset, val last: Offset) : GestureStart()
}

/**
 * Single pointer handler for Presence. Multiple [pointerInput] detectors on the
 * same node steal events from each other (transform pan ate flicks and volume;
 * per-event zoom never reached 0.8).
 */
fun Modifier.presenceGestures(
    isRitualMode: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onFlickNext: () -> Unit,
    onFlickPrevious: () -> Unit,
    onPinchOpenSession: () -> Unit,
    onSwipeFromLeftEdge: () -> Unit
): Modifier = pointerInput(isRitualMode) {
    val touchSlop = viewConfiguration.touchSlop
    val flickDistance = 64.dp.toPx()
    val edgeWidth = 28.dp.toPx()
    val longPressMs = 420L
    val doubleTapMs = viewConfiguration.doubleTapTimeoutMillis

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        val startPos = down.position

        val start = withTimeoutOrNull(longPressMs) {
            awaitGestureStart(down, touchSlop)
        }

        if (start == null) {
            onLongPress()
            waitForUpOrCancellation()
            return@awaitEachGesture
        }

        when (start) {
            GestureStart.Released -> {
                val secondDown = withTimeoutOrNull(doubleTapMs) {
                    awaitFirstDown(requireUnconsumed = false)
                }
                if (secondDown != null) {
                    onDoubleTap()
                    waitForUpOrCancellation()
                } else {
                    onTap()
                }
            }
            is GestureStart.Pinched -> {
                consumePinch(start.distance, onPinchOpenSession)
            }
            is GestureStart.Moved -> {
                followDrag(
                    startPos = startPos,
                    initialPan = start.pan,
                    lastPos = start.last,
                    isRitualMode = isRitualMode,
                    flickDistance = flickDistance,
                    edgeWidth = edgeWidth,
                    onVolumeDelta = onVolumeDelta,
                    onFlickNext = onFlickNext,
                    onFlickPrevious = onFlickPrevious,
                    onSwipeFromLeftEdge = onSwipeFromLeftEdge
                )
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitGestureStart(
    down: PointerInputChange,
    touchSlop: Float
): GestureStart {
    var pan = Offset.Zero
    var last = down.position
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) return GestureStart.Released
        if (pressed.size >= 2) {
            val dist = (pressed[0].position - pressed[1].position).getDistance()
            pressed.forEach { if (it.positionChanged()) it.consume() }
            return GestureStart.Pinched(dist.coerceAtLeast(1f))
        }
        val change = pressed[0]
        val pos = change.position
        pan += pos - last
        last = pos
        if (pan.getDistance() > touchSlop) {
            return GestureStart.Moved(pan, last)
        }
    }
}

private suspend fun AwaitPointerEventScope.consumePinch(
    startDistance: Float,
    onPinchOpenSession: () -> Unit
) {
    var origin = startDistance.coerceAtLeast(1f)
    var fired = false
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) break
        if (pressed.size >= 2) {
            val dist = (pressed[0].position - pressed[1].position).getDistance()
            val scale = dist / origin
            if (!fired && PresenceGestureMath.isPinchScale(scale)) {
                fired = true
                onPinchOpenSession()
            }
        }
        pressed.forEach { if (it.positionChanged()) it.consume() }
    }
}

private suspend fun AwaitPointerEventScope.followDrag(
    startPos: Offset,
    initialPan: Offset,
    lastPos: Offset,
    isRitualMode: Boolean,
    flickDistance: Float,
    edgeWidth: Float,
    onVolumeDelta: (Float) -> Unit,
    onFlickNext: () -> Unit,
    onFlickPrevious: () -> Unit,
    onSwipeFromLeftEdge: () -> Unit
) {
    var pan = initialPan
    var last = lastPos
    val isVolume = PresenceGestureMath.isVerticalDominant(pan)
    val height = size.height.toFloat().coerceAtLeast(1f)

    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) break
        val change = pressed[0]
        val delta = change.position - last
        last = change.position
        pan += delta
        if (isVolume) {
            onVolumeDelta(-delta.y / height)
        }
        if (change.positionChanged()) change.consume()
    }

    if (isVolume) return
    val fromLeftEdge = startPos.x < edgeWidth && pan.x > flickDistance * 0.5f
    when {
        fromLeftEdge -> onSwipeFromLeftEdge()
        PresenceGestureMath.isHorizontalFlick(pan, flickDistance) && !isRitualMode -> {
            if (PresenceGestureMath.isFlickToNext(pan)) onFlickNext() else onFlickPrevious()
        }
    }
}
