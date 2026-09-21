package com.example.aether.analysis

import com.example.aether.data.db.entities.ProfileEntity

/**
 * Arma listas de actividad (Entrenar / Trabajar / Relajar) solo con pistas
 * cuyo perfil acústico encaja de verdad. No rellena un mood con “lo más cercano”:
 * si no hay relajante, Relajar no sale.
 */
object ActivityPlaylistBuilder {

    const val MIN_TRACKS = 3

    const val DRIVE_RELAX_MAX = 0.40
    const val DRIVE_TRAIN_MIN = 0.58
    const val BPM_TRAIN_MIN = 115.0
    const val BPM_RELAX_MAX = 100.0
    const val BPM_WORK_MIN = 78.0
    const val BPM_WORK_MAX = 132.0

    enum class ActivityKind(val id: String, val displayName: String) {
        ENTRENAR("entrenar", "Entrenar"),
        TRABAJAR("trabajar", "Trabajar"),
        RELAJAR("relajar", "Relajar")
    }

    data class AnalyzedTrack(
        val songId: Long,
        val profile: ProfileEntity
    )

    data class Features(
        val bpm: Double,
        val drive: Double,
        val loudness: Double,
        val onset: Double,
        val flux: Double,
        val silence: Double
    )

    fun featuresOf(profile: ProfileEntity): Features {
        val emb = parseEmbedding(profile.embedding)
        val bpmRaw = profile.bpm ?: 0.0
        val confidence = profile.bpmConfidence ?: 0.0
        val bpm = if (bpmRaw > 0.0 && confidence >= RhythmAnalysis.MIN_CONFIDENCE) bpmRaw else 0.0

        val loudness = emb?.getOrNull(7)?.toDouble()
            ?: RhythmAnalysis.loudnessNorm(
                profile.loudnessApprox
                    ?: RhythmAnalysis.loudnessDbfs((profile.energyMean ?: 0.05).toFloat())
            ).toDouble()
        val silence = emb?.getOrNull(6)?.toDouble() ?: (profile.silenceRatio ?: 0.0)
        val flux = emb?.getOrNull(5)?.toDouble()
            ?: ((profile.fluxMean ?: 0.0) * 5.0).coerceIn(0.0, 1.0)
        val onset = emb?.getOrNull(12)?.toDouble() ?: 0.2

        val tempo = if (bpm > 0.0) {
            (bpm / 200.0).coerceIn(0.0, 1.0)
        } else {
            (0.4 + 0.9 * onset.coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0)
        }
        val drive = (
            0.28 * tempo +
                0.28 * loudness.coerceIn(0.0, 1.0) +
                0.22 * onset.coerceIn(0.0, 1.0) +
                0.14 * flux.coerceIn(0.0, 1.0) +
                0.08 * (1.0 - silence.coerceIn(0.0, 1.0))
            ).coerceIn(0.0, 1.0)

        return Features(
            bpm = bpm,
            drive = drive,
            loudness = loudness,
            onset = onset,
            flux = flux,
            silence = silence
        )
    }

    fun classify(profile: ProfileEntity): ActivityKind? = classify(featuresOf(profile))

    fun classify(features: Features): ActivityKind? {
        val drive = features.drive
        val bpm = features.bpm
        return when {
            drive >= DRIVE_TRAIN_MIN && bpmAllows(bpm, BPM_TRAIN_MIN, Double.POSITIVE_INFINITY) ->
                ActivityKind.ENTRENAR
            drive < DRIVE_RELAX_MAX && bpmAllows(bpm, Double.NEGATIVE_INFINITY, BPM_RELAX_MAX) ->
                ActivityKind.RELAJAR
            drive >= DRIVE_RELAX_MAX &&
                drive < DRIVE_TRAIN_MIN &&
                bpmAllows(bpm, BPM_WORK_MIN, BPM_WORK_MAX) ->
                ActivityKind.TRABAJAR
            else -> null
        }
    }

    /**
     * Devuelve solo actividades con al menos [MIN_TRACKS] pistas, ordenadas
     * de más a menos representativas del mood.
     */
    fun build(tracks: List<AnalyzedTrack>): Map<ActivityKind, List<Long>> {
        val buckets = ActivityKind.entries.associateWith { mutableListOf<Pair<Long, Double>>() }
        for (track in tracks) {
            val features = featuresOf(track.profile)
            val kind = classify(features) ?: continue
            buckets.getValue(kind).add(track.songId to fitScore(features, kind))
        }
        return buildMap {
            for (kind in ActivityKind.entries) {
                val ranked = buckets.getValue(kind)
                    .sortedByDescending { it.second }
                    .map { it.first }
                if (ranked.size >= MIN_TRACKS) put(kind, ranked)
            }
        }
    }

    fun fitScore(features: Features, kind: ActivityKind): Double = when (kind) {
        ActivityKind.ENTRENAR -> features.drive
        ActivityKind.RELAJAR -> 1.0 - features.drive
        ActivityKind.TRABAJAR -> 1.0 - kotlin.math.abs(features.drive - 0.51)
    }

    private fun bpmAllows(bpm: Double, min: Double, max: Double): Boolean {
        if (bpm <= 0.0) return true
        return bpm >= min && bpm <= max
    }

    private fun parseEmbedding(raw: String?): List<Float>? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(',').mapNotNull { it.trim().toFloatOrNull() }
        return if (parts.size >= 16) parts else null
    }
}
