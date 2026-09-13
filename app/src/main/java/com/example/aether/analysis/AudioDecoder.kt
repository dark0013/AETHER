package com.example.aether.analysis

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.aether.util.AetherLog
import java.io.FileNotFoundException
import kotlin.math.min

class AudioDecoder(private val context: Context) {

    companion object {
        private const val TAG = "AudioDecoder"
        const val TARGET_SAMPLE_RATE = 22050
        const val TARGET_CHANNELS = 1
    }

    /**
     * Streams PCM as float blocks. Never loads the whole file into memory.
     */
    suspend fun decode(uri: Uri, onSamplesExtracted: (FloatArray) -> Unit) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: FileNotFoundException) {
            extractor.release()
            throw AnalysisException("archivo ilegible", unrecoverable = true, cause = e)
        } catch (e: SecurityException) {
            extractor.release()
            throw AnalysisException("archivo ilegible", unrecoverable = true, cause = e)
        } catch (e: Exception) {
            extractor.release()
            throw AnalysisException("archivo ilegible", unrecoverable = true, cause = e)
        }

        val trackIndex = selectAudioTrack(extractor)
        if (trackIndex < 0) {
            extractor.release()
            throw AnalysisException("sin pista de audio", unrecoverable = true)
        }

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            extractor.release()
            throw AnalysisException("códec no soportado", unrecoverable = true, cause = e)
        }

        try {
            codec.configure(format, null, null, 0)
            codec.start()
        } catch (e: Exception) {
            codec.release()
            extractor.release()
            throw AnalysisException("códec no soportado", unrecoverable = true, cause = e)
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var inputEndOfStream = false
        var outputEndOfStream = false

        extractor.selectTrack(trackIndex)

        val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        try {
            while (!outputEndOfStream) {
                if (!inputEndOfStream) {
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        val sampleSize = inputBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEndOfStream = true
                        } else {
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputEndOfStream = true
                    }

                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val pcmData = ShortArray(bufferInfo.size / 2)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.asShortBuffer().get(pcmData)

                        val processedSamples = processAudio(pcmData, sourceSampleRate, sourceChannels)
                        if (processedSamples.isNotEmpty()) {
                            onSamplesExtracted(processedSamples)
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }
            }
        } catch (e: AnalysisException) {
            throw e
        } catch (e: Exception) {
            AetherLog.e(TAG, "Decode failed for $uri", e)
            throw AnalysisException("archivo ilegible", unrecoverable = true, cause = e)
        } finally {
            try {
                codec.stop()
            } catch (_: Exception) {
            }
            codec.release()
            extractor.release()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    private fun processAudio(pcmData: ShortArray, sampleRate: Int, channels: Int): FloatArray {
        val channelCount = channels.coerceAtLeast(1)
        val monoSamples = if (channelCount > 1) {
            val frames = pcmData.size / channelCount
            FloatArray(frames) { i ->
                var sum = 0f
                val base = i * channelCount
                val end = min(base + channelCount, pcmData.size)
                for (c in base until end) {
                    sum += pcmData[c]
                }
                (sum / channelCount) / 32768f
            }
        } else {
            FloatArray(pcmData.size) { i -> pcmData[i] / 32768f }
        }

        val ratio = sampleRate.toFloat() / TARGET_SAMPLE_RATE
        if (ratio > 1.1f) {
            val newSize = (monoSamples.size / ratio).toInt()
            if (newSize <= 0) return FloatArray(0)
            return FloatArray(newSize) { i ->
                val sourceIndex = (i * ratio).toInt()
                if (sourceIndex < monoSamples.size) monoSamples[sourceIndex] else 0f
            }
        }

        return monoSamples
    }
}
