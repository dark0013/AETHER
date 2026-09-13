package com.example.aether.analysis

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * BPM, beat grid and sections per AETHER_SPEC §4.2 — no Android types, unit-testable.
 *
 * Confidence is `1 - secondPeak/peak` so the spec threshold 0.35 is meaningful
 * (`peak/secondPeak` clamped to 0..1 would always be 1 when peak ≥ second).
 */
object RhythmAnalysis {
    const val SCHEMA_VERSION = 2
    const val MIN_BPM = 60.0
    const val MAX_BPM = 180.0
    const val MIN_CONFIDENCE = 0.35
    const val ALIGN_CONFIDENCE = 0.45
    const val ALIGN_RATIO_MIN = 0.92
    const val ALIGN_RATIO_MAX = 1.08
    const val ALIGN_SNAP_MS = 120L

    fun estimateBpm(onsets: List<Boolean>, hopMs: Double): Pair<Double, Double> {
        if (onsets.size < 32) return 0.0 to 0.0
        val n = onsets.size
        val minLag = (60_000.0 / MAX_BPM / hopMs).toInt().coerceAtLeast(1)
        val maxLag = (60_000.0 / MIN_BPM / hopMs).toInt().coerceAtMost(n / 2)
        if (maxLag <= minLag) return 0.0 to 0.0

        val x = FloatArray(n) { i -> if (onsets[i]) 1f else 0f }
        val scores = FloatArray(maxLag + 1)
        for (lag in minLag..maxLag) {
            var acc = 0f
            val last = n - lag
            for (i in 0 until last) acc += x[i] * x[i + lag]
            scores[lag] = acc
        }
        var bestLag = minLag
        var best = -1f
        for (lag in minLag..maxLag) {
            if (scores[lag] > best) {
                best = scores[lag]
                bestLag = lag
            }
        }
        if (best <= 0f) return 0.0 to 0.0
        val doubleLag = bestLag * 2
        if (doubleLag in minLag..maxLag && scores[doubleLag] >= best * 0.9f) {
            // Octave: keep the faster tempo (smaller lag) already selected.
        }
        val halfLag = bestLag / 2
        if (halfLag >= minLag && scores[halfLag] >= best * 0.95f) {
            bestLag = halfLag
            best = scores[halfLag]
        }
        var second = -1f
        for (lag in minLag..maxLag) {
            if (lag == bestLag || isOctaveLag(lag, bestLag)) continue
            if (scores[lag] > second) second = scores[lag]
        }
        val confidence = if (second <= 0f) 1.0 else (1.0 - second / best).coerceIn(0.0, 1.0)
        if (confidence < MIN_CONFIDENCE) return 0.0 to confidence
        val bpm = 60_000.0 / (bestLag * hopMs)
        return bpm.coerceIn(MIN_BPM, MAX_BPM) to confidence
    }

    fun beatGridOffsetMs(onsets: List<Boolean>, hopMs: Double, bpm: Double): Int {
        if (bpm <= 0.0) return 0
        val period = 60_000.0 / bpm
        val times = onsets.indices.mapNotNull { i -> if (onsets[i]) i * hopMs else null }
        if (times.isEmpty()) return 0
        val steps = (period / hopMs).roundToInt().coerceIn(8, 64)
        var bestOff = 0
        var bestScore = -1.0
        for (s in 0 until steps) {
            val off = s * period / steps
            var score = 0.0
            for (t in times) {
                val k = ((t - off) / period).roundToInt()
                val beat = off + k * period
                val err = abs(t - beat)
                if (err < hopMs) score += 1.0 - err / hopMs
            }
            if (score > bestScore) {
                bestScore = score
                bestOff = off.roundToInt()
            }
        }
        return bestOff
    }

    fun nearestBeatMs(timeMs: Long, bpm: Double, offsetMs: Int): Long {
        if (bpm <= 0.0) return timeMs
        val period = 60_000.0 / bpm
        val k = ((timeMs - offsetMs) / period).roundToInt()
        return (offsetMs + k * period).roundToInt().toLong().coerceAtLeast(0L)
    }

    fun loudnessDbfs(energyMean: Float): Double {
        val rms = energyMean.coerceAtLeast(1e-8f)
        return 20.0 * ln(rms.toDouble()) / ln(10.0)
    }

    fun loudnessNorm(dbfs: Double): Float =
        ((dbfs + 60.0) / 60.0).toFloat().coerceIn(0f, 1f)

    fun detectSections(energy: List<Float>, hopMs: Double): List<Int> {
        val framesPerSec = (1000.0 / hopMs).roundToInt().coerceAtLeast(1)
        if (energy.size < framesPerSec * 24) return emptyList()

        val oneHz = ArrayList<Float>(energy.size / framesPerSec + 1)
        var i = 0
        while (i < energy.size) {
            val end = (i + framesPerSec).coerceAtMost(energy.size)
            var sum = 0.0
            for (j in i until end) sum += energy[j]
            oneHz.add((sum / (end - i)).toFloat())
            i = end
        }

        val win = 8
        if (oneHz.size < win * 2 + 1) return emptyList()
        val distances = FloatArray(oneHz.size)
        val slice = ArrayList<Float>(oneHz.size)
        for (t in win until oneHz.size - win) {
            var d = 0.0
            for (k in 0 until win) {
                d += abs(oneHz[t - win + k] - oneHz[t + k])
            }
            val mean = (d / win).toFloat()
            distances[t] = mean
            slice.add(mean)
        }
        if (slice.isEmpty()) return emptyList()
        val sorted = slice.sorted()
        val p80 = DspUtils.percentile(sorted, 0.80f)
        val minGapSec = 12
        val bounds = mutableListOf<Int>()
        var lastCut = -minGapSec
        for (t in win until oneHz.size - win) {
            if (distances[t] > p80 && t - lastCut >= minGapSec) {
                bounds.add(t * 1000)
                lastCut = t
                if (bounds.size >= 8) break
            }
        }
        return bounds
    }

    private fun isOctaveLag(a: Int, b: Int): Boolean {
        if (a <= 0 || b <= 0) return false
        val ratio = a.toDouble() / b
        return abs(ratio - 2.0) < 0.18 || abs(ratio - 0.5) < 0.12
    }
}
