package com.example.aether.analysis

import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.model.Song
import kotlin.math.sqrt

object SimilarityEngine {

    private val weights = floatArrayOf(
        1.2f, // 0: bpm
        1.0f, // 1: bpmConfidence (spec uses it as weight multiplier for bpm)
        1.0f, // 2: energyMean
        0.6f, // 3: energyStd
        1.0f, // 4: centroidNorm
        0.7f, // 5: fluxMean
        0.4f, // 6: silenceRatio
        1.0f, // 7: energyMean (loudness approx)
        0.5f, // 8: chunk 15%
        0.5f, // 9: chunk 15-50%
        0.5f, // 10: chunk 50-85%
        0.5f, // 11: chunk last 15%
        0.8f, // 12: onsetDensity
        0.3f, // 13: brightRatio
        0.3f, // 14: sections count
        0.0f  // 15: reserved
    )

    /**
     * Calcula la distancia musical entre dos perfiles.
     * Cuanto menor sea la distancia, más similares son las canciones.
     */
    fun calculateDistance(p1: ProfileEntity, p2: ProfileEntity): Double {
        val e1 = p1.embedding?.split(",")?.map { it.toFloat() } ?: return Double.MAX_VALUE
        val e2 = p2.embedding?.split(",")?.map { it.toFloat() } ?: return Double.MAX_VALUE

        if (e1.size < 16 || e2.size < 16) return Double.MAX_VALUE

        var sum = 0.0
        for (i in weights.indices) {
            val diff = e1[i] - e2[i]
            var w = weights[i]
            
            // Regla spec: si alguno tiene bpm=0, peso de bpm es 0
            if (i == 0 && (p1.bpm == 0.0 || p2.bpm == 0.0)) {
                w = 0f
            }

            sum += w * (diff * diff)
        }

        return sqrt(sum)
    }

    /**
     * Sugiere la mejor canción candidata basándose en similitud acústica.
     */
    fun suggestNext(
        currentSong: Song,
        currentProfile: ProfileEntity,
        candidates: List<Pair<Song, ProfileEntity>>,
        historyIds: Set<Long>
    ): Song? {
        if (candidates.isEmpty()) return null

        val filtered = candidates.filter { (song, _) ->
            song.id != currentSong.id && song.id !in historyIds
        }

        if (filtered.isEmpty()) return null

        // Calcular distancias y ordenar
        val scored = filtered.map { (song, profile) ->
            song to calculateDistance(currentProfile, profile)
        }.sortedBy { it.second }

        val best = scored.first()
        
        // Regla de Artista del spec: 
        // Excluir mismo artista si hay alternativa viable (distancia <= 1.15 * mejor)
        if (best.first.artist == currentSong.artist) {
            val otherArtist = scored.find { it.first.artist != currentSong.artist }
            if (otherArtist != null && otherArtist.second <= best.second * 1.15) {
                return otherArtist.first
            }
        }

        return best.first
    }
}
