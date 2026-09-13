package com.example.aether.analysis

import androidx.compose.ui.geometry.Offset
import com.example.aether.ui.presence.PresenceGestureMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardeningTest {

    @Test
    fun rmsOfConstantSignalIsAbsoluteValue() {
        val rms = DspUtils.calculateRms(FloatArray(8) { 1f })
        assertEquals(1f, rms, 0.0001f)
    }

    @Test
    fun quantizeUsesTrackPercentilesForContrast() {
        val data = List(100) { it.toFloat() }
        val quantized = DspUtils.quantizeToUInt8(data)
        assertEquals(100, quantized.size)
        assertTrue((quantized[2].toInt() and 0xFF) < 20)
        assertTrue((quantized[97].toInt() and 0xFF) > 230)
    }

    @Test
    fun onsetCooldownSuppressesImmediateRepeats() {
        assertFalse(
            DspUtils.isOnset(
                flux = 1f,
                medianFlux = 0.1f,
                thresholdK = 1.8f,
                framesSinceLastOnset = 0,
                cooldownFrames = 2
            )
        )
        assertTrue(
            DspUtils.isOnset(
                flux = 1f,
                medianFlux = 0.1f,
                thresholdK = 1.8f,
                framesSinceLastOnset = 2,
                cooldownFrames = 2
            )
        )
    }

    @Test
    fun transitFallsBackWhenTapesAreMissing() {
        val plan = TransitEngine.planCrossfade(
            currentDurationMs = 180_000L,
            currentTape = null,
            nextTape = null,
            currentProfile = null,
            nextProfile = null
        )
        assertEquals(179_300L, plan.endPointMs)
        assertEquals(0L, plan.startPointMs)
        assertEquals(700L, plan.fadeDurationMs)
    }

    @Test
    fun transitSnapsEndToBeatWhenConfidenceHigh() {
        val current = com.example.aether.data.db.entities.ProfileEntity(
            songId = 1,
            bpm = 120.0,
            bpmConfidence = 0.8,
            beatGridOffsetMs = 0
        )
        val next = current.copy(songId = 2)
        val plan = TransitEngine.planCrossfade(
            currentDurationMs = 180_200L,
            currentTape = null,
            nextTape = null,
            currentProfile = current,
            nextProfile = next
        )
        assertEquals(179_500L, plan.endPointMs)
    }

    @Test
    fun pinchUsesCumulativeScaleNotPerEventZoom() {
        assertFalse(PresenceGestureMath.isPinchScale(0.98f))
        assertTrue(PresenceGestureMath.isPinchScale(0.80f))
        assertTrue(PresenceGestureMath.isPinchScale(1.20f))
    }

    @Test
    fun flickIsHorizontalAndPastDistance() {
        assertTrue(PresenceGestureMath.isHorizontalFlick(Offset(-200f, 10f), 64f))
        assertTrue(PresenceGestureMath.isHorizontalFlick(Offset(200f, -20f), 64f))
        assertFalse(PresenceGestureMath.isHorizontalFlick(Offset(40f, 5f), 64f))
        assertTrue(PresenceGestureMath.isVerticalDominant(Offset(10f, 80f)))
        assertTrue(PresenceGestureMath.isFlickToNext(Offset(-200f, 10f)))
        assertFalse(PresenceGestureMath.isFlickToNext(Offset(200f, 10f)))
    }
}
