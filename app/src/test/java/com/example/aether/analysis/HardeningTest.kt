package com.example.aether.analysis

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
}
