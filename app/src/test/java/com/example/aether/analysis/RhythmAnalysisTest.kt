package com.example.aether.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RhythmAnalysisTest {

    private val hopMs = 100.0

    private fun pulseTrain(bpm: Double, seconds: Int): List<Boolean> {
        val frames = (seconds * 1000.0 / hopMs).toInt()
        val period = 60_000.0 / bpm
        return List(frames) { i ->
            val t = i * hopMs
            val k = kotlin.math.round(t / period)
            abs(t - k * period) < hopMs * 0.45
        }
    }

    @Test
    fun autocorrelationFinds120Bpm() {
        val (bpm, confidence) = RhythmAnalysis.estimateBpm(pulseTrain(120.0, 30), hopMs)
        assertTrue("confidence $confidence", confidence >= 0.35)
        assertEquals(120.0, bpm, 6.0)
    }

    @Test
    fun autocorrelationFinds100Bpm() {
        val (bpm, confidence) = RhythmAnalysis.estimateBpm(pulseTrain(100.0, 30), hopMs)
        assertTrue("confidence $confidence", confidence >= 0.35)
        assertEquals(100.0, bpm, 6.0)
    }

    @Test
    fun sparseOnsetsYieldZeroBpm() {
        val onsets = MutableList(200) { false }.also {
            it[12] = true
            it[77] = true
            it[141] = true
        }
        val (bpm, _) = RhythmAnalysis.estimateBpm(onsets, hopMs)
        assertEquals(0.0, bpm, 0.0)
    }

    @Test
    fun beatGridOffsetAlignsWithFirstPulse() {
        val onsets = pulseTrain(120.0, 20)
        val offset = RhythmAnalysis.beatGridOffsetMs(onsets, hopMs, 120.0)
        val first = onsets.indexOfFirst { it } * hopMs
        val nearest = RhythmAnalysis.nearestBeatMs(first.toLong(), 120.0, offset)
        assertTrue(abs(nearest - first.toLong()) < hopMs * 2)
    }

    @Test
    fun nearestBeatSnapsWithin120Ms() {
        val snapped = RhythmAnalysis.nearestBeatMs(10_050L, 120.0, 0)
        assertEquals(10_000L, snapped)
    }

    @Test
    fun loudnessOfUnitRmsIsZeroDbfs() {
        assertEquals(0.0, RhythmAnalysis.loudnessDbfs(1f), 0.01)
        assertEquals(1f, RhythmAnalysis.loudnessNorm(0.0), 0.01f)
        assertEquals(0f, RhythmAnalysis.loudnessNorm(-60.0), 0.01f)
    }

    @Test
    fun sectionsRespectMaxEightAndMinGap() {
        val hop = 93.0
        val energy = MutableList(800) { 0.1f }
        for (sec in listOf(20, 40, 60, 80)) {
            val start = (sec * 1000 / hop).toInt()
            for (i in start until (start + 20).coerceAtMost(energy.size)) {
                energy[i] = 0.9f
            }
        }
        val bounds = RhythmAnalysis.detectSections(energy, hop)
        assertTrue(bounds.size <= 8)
        if (bounds.size >= 2) {
            for (i in 1 until bounds.size) {
                assertTrue(bounds[i] - bounds[i - 1] >= 12_000)
            }
        }
    }
}
