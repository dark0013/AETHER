package com.example.aether.analysis

import kotlin.math.*

object DspUtils {

    /**
     * Aplica la ventana de Hann a un array de samples.
     */
    fun applyHannWindow(samples: FloatArray) {
        val size = samples.size
        for (i in 0 until size) {
            val hann = 0.5 * (1 - cos(2.0 * PI * i / (size - 1)))
            samples[i] *= hann.toFloat()
        }
    }

    /**
     * Calcula el Root Mean Square (RMS) de un bloque de audio.
     */
    fun calculateRms(samples: FloatArray): Float {
        var sum = 0f
        for (s in samples) sum += s * s
        return sqrt(sum / samples.size)
    }

    /**
     * FFT (Fast Fourier Transform) Radix-2.
     * N debe ser potencia de 2.
     * Devuelve la magnitud del espectro (N/2 bins).
     */
    fun fftMagnitudes(real: FloatArray): FloatArray {
        val n = real.size
        val imag = FloatArray(n) { 0f }
        
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val temp = real[i]
                real[i] = real[j]
                real[j] = temp
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Butterfly computation
        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wRealStep = cos(angle).toFloat()
            val wImagStep = sin(angle).toFloat()
            
            for (i in 0 until n step length) {
                var wReal = 1.0f
                var wImag = 0.0f
                val half = length / 2
                for (k in 0 until half) {
                    val uIdx = i + k
                    val vIdx = i + k + half
                    val vReal = real[vIdx] * wReal - imag[vIdx] * wImag
                    val vImag = real[vIdx] * wImag + imag[vIdx] * wReal
                    
                    real[vIdx] = real[uIdx] - vReal
                    imag[vIdx] = imag[uIdx] - vImag
                    real[uIdx] += vReal
                    imag[uIdx] += vImag
                    
                    val nextWReal = wReal * wRealStep - wImag * wImagStep
                    wImag = wReal * wImagStep + wImag * wRealStep
                    wReal = nextWReal
                }
            }
            length *= 2
        }

        // Magnitudes (solo la primera mitad del espectro)
        val magnitudes = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return magnitudes
    }

    /**
     * Calcula el Spectral Centroid (Brillo).
     */
    fun calculateCentroid(magnitudes: FloatArray, sampleRate: Int): Float {
        var weightedSum = 0f
        var totalSum = 0f
        val binSize = sampleRate.toFloat() / (magnitudes.size * 2)
        
        for (i in magnitudes.indices) {
            val frequency = i * binSize
            weightedSum += frequency * magnitudes[i]
            totalSum += magnitudes[i]
        }
        
        return if (totalSum > 0) weightedSum / totalSum else 0f
    }

    /**
     * Calcula el Spectral Flux (Cambio de energía por banda).
     */
    fun calculateFlux(magnitudes: FloatArray, prevMagnitudes: FloatArray?): Float {
        if (prevMagnitudes == null) return 0f
        var flux = 0f
        for (i in magnitudes.indices) {
            val diff = magnitudes[i] - prevMagnitudes[i]
            if (diff > 0) flux += diff
        }
        return flux
    }

    /**
     * Onset if flux exceeds the local median, with a cooldown in frames (~120 ms).
     */
    fun isOnset(
        flux: Float,
        medianFlux: Float,
        thresholdK: Float,
        framesSinceLastOnset: Int,
        cooldownFrames: Int,
        minFlux: Float = 0.01f
    ): Boolean {
        if (framesSinceLastOnset < cooldownFrames) return false
        return flux > medianFlux * thresholdK && flux > minFlux
    }

    /**
     * Quantize to uint8 using the track's own 2nd–98th percentiles for contrast.
     */
    fun quantizeToUInt8(data: List<Float>): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        val sorted = data.sorted()
        val p2 = percentile(sorted, 0.02f)
        val p98 = percentile(sorted, 0.98f)
        val range = (p98 - p2).let { if (it < 1e-6f) 1f else it }
        return ByteArray(data.size) { i ->
            (((data[i] - p2) / range).coerceIn(0f, 1f) * 255f).toInt().toByte()
        }
    }

    fun percentile(sorted: List<Float>, p: Float): Float {
        if (sorted.isEmpty()) return 0f
        val index = (p * (sorted.size - 1)).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }
}
