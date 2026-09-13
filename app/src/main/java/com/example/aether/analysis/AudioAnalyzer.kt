package com.example.aether.analysis

import android.content.Context
import android.net.Uri
import com.example.aether.data.db.entities.AnalysisStatus
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.util.AetherLog
import java.util.LinkedList
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class AudioAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val FFT_SIZE = 2048
        private const val HOP_SIZE = 2048
        private const val ONSET_THRESHOLD_K = 1.8f
        private const val ONSET_COOLDOWN_MS = 120.0
    }

    data class AnalysisResult(
        val profile: ProfileEntity,
        val energyTape: ByteArray,
        val centroidTape: ByteArray,
        val fluxTape: ByteArray,
        val onsetFlags: ByteArray
    )

    suspend fun analyze(songId: Long, uri: Uri): AnalysisResult? {
        val decoder = AudioDecoder(context)

        val energyList = mutableListOf<Float>()
        val centroidList = mutableListOf<Float>()
        val fluxList = mutableListOf<Float>()
        val onsets = mutableListOf<Boolean>()

        var prevMagnitudes: FloatArray? = null
        val accumulator = FloatArray(FFT_SIZE)
        var accSize = 0
        var frameIndex = 0
        var lastOnsetFrame = -1000

        val hopMs = (HOP_SIZE.toDouble() / AudioDecoder.TARGET_SAMPLE_RATE) * 1000.0
        val cooldownFrames = ceil(ONSET_COOLDOWN_MS / hopMs).toInt().coerceAtLeast(1)

        val fluxWindow = LinkedList<Float>()
        val windowSize = 16

        decoder.decode(uri) { samples ->
            var offset = 0
            while (offset < samples.size) {
                val toCopy = min(FFT_SIZE - accSize, samples.size - offset)
                System.arraycopy(samples, offset, accumulator, accSize, toCopy)
                accSize += toCopy
                offset += toCopy

                if (accSize < FFT_SIZE) break

                val frame = accumulator.copyOf()
                accSize = 0

                val rms = DspUtils.calculateRms(frame)
                energyList.add(rms)

                DspUtils.applyHannWindow(frame)
                val magnitudes = DspUtils.fftMagnitudes(frame)

                val centroid = DspUtils.calculateCentroid(magnitudes, AudioDecoder.TARGET_SAMPLE_RATE)
                centroidList.add(centroid)

                val flux = DspUtils.calculateFlux(magnitudes, prevMagnitudes)
                fluxList.add(flux)
                prevMagnitudes = magnitudes

                fluxWindow.add(flux)
                if (fluxWindow.size > windowSize) fluxWindow.removeFirst()

                val medianFlux = if (fluxWindow.isNotEmpty()) {
                    val sorted = fluxWindow.sorted()
                    sorted[sorted.size / 2]
                } else 0f

                val isOnset = DspUtils.isOnset(
                    flux = flux,
                    medianFlux = medianFlux,
                    thresholdK = ONSET_THRESHOLD_K,
                    framesSinceLastOnset = frameIndex - lastOnsetFrame,
                    cooldownFrames = cooldownFrames
                )
                if (isOnset) lastOnsetFrame = frameIndex
                onsets.add(isOnset)
                frameIndex++
            }
        }

        if (energyList.isEmpty()) {
            AetherLog.e(TAG, "No frames decoded for $songId")
            return null
        }

        val bpmResult = RhythmAnalysis.estimateBpm(onsets, hopMs)
        val beatOffset = RhythmAnalysis.beatGridOffsetMs(onsets, hopMs, bpmResult.first)
        val energyMean = energyList.average().toFloat()
        val energyStd = calculateStdDev(energyList, energyMean)
        val centroidMean = centroidList.average().toFloat()
        val fluxMean = fluxList.average().toFloat()
        val sectionBounds = RhythmAnalysis.detectSections(energyList, hopMs)
        val loudness = RhythmAnalysis.loudnessDbfs(energyMean)

        val silenceRatio = energyList.count { it < 0.01f }.toFloat() / energyList.size
        val onsetDensity = onsets.count { it }.toFloat() /
            (energyList.size * (HOP_SIZE.toFloat() / AudioDecoder.TARGET_SAMPLE_RATE))
        val brightRatio = centroidList.count { it > 2500f }.toFloat() / centroidList.size

        val embedding = FloatArray(16) { 0f }
        embedding[0] = (bpmResult.first / 200.0).toFloat().coerceIn(0f, 1f)
        embedding[1] = bpmResult.second.toFloat()
        embedding[2] = energyMean.coerceIn(0f, 1f)
        embedding[3] = (energyStd * 2f).coerceIn(0f, 1f)
        embedding[4] = (centroidMean / 5000f).coerceIn(0f, 1f)
        embedding[5] = (fluxMean * 5f).coerceIn(0f, 1f)
        embedding[6] = silenceRatio.coerceIn(0f, 1f)
        embedding[7] = RhythmAnalysis.loudnessNorm(loudness)

        val chunk15 = (energyList.size * 0.15).toInt().coerceAtLeast(1)
        val chunk50 = (energyList.size * 0.50).toInt().coerceAtLeast(1)
        val chunk85 = (energyList.size * 0.85).toInt().coerceAtLeast(1)

        embedding[8] = energyList.subList(0, chunk15).average().toFloat().coerceIn(0f, 1f)
        embedding[9] = energyList.subList(chunk15, chunk50).average().toFloat().coerceIn(0f, 1f)
        embedding[10] = energyList.subList(chunk50, chunk85).average().toFloat().coerceIn(0f, 1f)
        embedding[11] = energyList.subList(chunk85, energyList.size).average().toFloat().coerceIn(0f, 1f)

        embedding[12] = (onsetDensity / 10f).coerceIn(0f, 1f)
        embedding[13] = brightRatio.coerceIn(0f, 1f)
        embedding[14] = (sectionBounds.size / 8f).coerceIn(0f, 1f)
        embedding[15] = 0f

        return AnalysisResult(
            profile = ProfileEntity(
                songId = songId,
                status = AnalysisStatus.READY,
                schemaVersion = RhythmAnalysis.SCHEMA_VERSION,
                analyzedAtMs = System.currentTimeMillis(),
                hopMs = hopMs.toInt(),
                bpm = bpmResult.first,
                bpmConfidence = bpmResult.second,
                beatGridOffsetMs = beatOffset,
                loudnessApprox = loudness,
                energyMean = energyMean.toDouble(),
                energyStd = energyStd.toDouble(),
                centroidMean = centroidMean.toDouble(),
                fluxMean = fluxMean.toDouble(),
                centroidNorm = (centroidMean / 5000.0).coerceIn(0.0, 1.0),
                silenceRatio = silenceRatio.toDouble(),
                sectionBoundsMs = sectionBounds.joinToString(","),
                embedding = embedding.joinToString(",")
            ),
            energyTape = DspUtils.quantizeToUInt8(energyList),
            centroidTape = DspUtils.quantizeToUInt8(centroidList),
            fluxTape = DspUtils.quantizeToUInt8(fluxList),
            onsetFlags = onsets.map { if (it) 1.toByte() else 0.toByte() }.toByteArray()
        )
    }

    private fun calculateStdDev(data: List<Float>, mean: Float): Float {
        if (data.isEmpty()) return 0f
        return sqrt(data.map { (it - mean).pow(2) }.average().toFloat())
    }
}
