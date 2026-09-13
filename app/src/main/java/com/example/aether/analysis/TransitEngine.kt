package com.example.aether.analysis

import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.ProfileEntity
import kotlin.math.abs

object TransitEngine {

    private const val SEARCH_WINDOW_END_MS = 2200L
    private const val SEARCH_WINDOW_START_MS = 1800L
    private const val FADE_MIN_MS = 400L
    private const val FADE_MAX_MS = 1800L
    private const val ONSET_MIN_GAP_MS = 80L

    data class TransitionPlan(
        val endPointMs: Long,
        val startPointMs: Long,
        val fadeDurationMs: Long
    )

    /**
     * Planifica el cruce entre dos canciones buscando onsets óptimos.
     */
    fun planCrossfade(
        currentDurationMs: Long,
        currentTape: DensityTapeEntity?,
        nextTape: DensityTapeEntity?,
        currentProfile: ProfileEntity?,
        nextProfile: ProfileEntity?
    ): TransitionPlan {
        // 1. Encontrar tEnd (punto de salida de la canción actual)
        // Buscamos el último onset en la ventana final
        val tEnd = findOptimalPoint(
            tape = currentTape,
            windowStartMs = currentDurationMs - SEARCH_WINDOW_END_MS,
            windowEndMs = currentDurationMs - ONSET_MIN_GAP_MS,
            preferLast = true
        ) ?: (currentDurationMs - 700L) // Fallback si no hay onsets

        // 2. Encontrar tStart (punto de entrada de la siguiente canción)
        // Buscamos el primer onset en la ventana inicial
        val tStart = findOptimalPoint(
            tape = nextTape,
            windowStartMs = ONSET_MIN_GAP_MS,
            windowEndMs = SEARCH_WINDOW_START_MS,
            preferLast = false
        ) ?: 0L

        var alignedEnd = tEnd
        val confA = currentProfile?.bpmConfidence ?: 0.0
        val confB = nextProfile?.bpmConfidence ?: 0.0
        val bpmA = currentProfile?.bpm ?: 0.0
        val bpmB = nextProfile?.bpm ?: 0.0
        if (confA >= RhythmAnalysis.ALIGN_CONFIDENCE &&
            confB >= RhythmAnalysis.ALIGN_CONFIDENCE &&
            bpmA > 0.0 && bpmB > 0.0
        ) {
            val ratio = bpmA / bpmB
            if (ratio in RhythmAnalysis.ALIGN_RATIO_MIN..RhythmAnalysis.ALIGN_RATIO_MAX) {
                val snapped = RhythmAnalysis.nearestBeatMs(
                    tEnd,
                    bpmA,
                    currentProfile?.beatGridOffsetMs ?: 0
                )
                if (abs(snapped - tEnd) <= RhythmAnalysis.ALIGN_SNAP_MS) {
                    alignedEnd = snapped.coerceIn(
                        currentDurationMs - SEARCH_WINDOW_END_MS,
                        currentDurationMs - ONSET_MIN_GAP_MS
                    )
                }
            }
        }

        val fadeDuration = (currentDurationMs - alignedEnd).coerceIn(FADE_MIN_MS, FADE_MAX_MS)

        return TransitionPlan(
            endPointMs = alignedEnd,
            startPointMs = tStart,
            fadeDurationMs = fadeDuration
        )
    }

    private fun findOptimalPoint(
        tape: DensityTapeEntity?,
        windowStartMs: Long,
        windowEndMs: Long,
        preferLast: Boolean
    ): Long? {
        if (tape == null || tape.onsetFlags.isEmpty()) return null

        val hopMs = tape.hopMs.toDouble()
        val startFrame = (windowStartMs / hopMs).toInt().coerceAtLeast(0)
        val endFrame = (windowEndMs / hopMs).toInt().coerceAtMost(tape.frameCount - 1)

        if (startFrame >= endFrame) return null

        val onsetFrames = mutableListOf<Int>()
        for (i in startFrame..endFrame) {
            if (tape.onsetFlags[i] == 1.toByte()) {
                onsetFrames.add(i)
            }
        }

        if (onsetFrames.isEmpty()) return null

        val selectedFrame = if (preferLast) onsetFrames.last() else onsetFrames.first()
        return (selectedFrame * hopMs).toLong()
    }
}
