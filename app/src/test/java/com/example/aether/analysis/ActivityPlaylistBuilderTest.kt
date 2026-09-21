package com.example.aether.analysis

import com.example.aether.data.db.entities.AnalysisStatus
import com.example.aether.data.db.entities.ProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPlaylistBuilderTest {

    @Test
    fun highDriveClassifiesAsEntrenar() {
        val kind = ActivityPlaylistBuilder.classify(trainProfile(1, bpm = 148.0))
        assertEquals(ActivityPlaylistBuilder.ActivityKind.ENTRENAR, kind)
    }

    @Test
    fun lowDriveClassifiesAsRelajar() {
        val kind = ActivityPlaylistBuilder.classify(relaxProfile(1, bpm = 72.0))
        assertEquals(ActivityPlaylistBuilder.ActivityKind.RELAJAR, kind)
    }

    @Test
    fun midDriveClassifiesAsTrabajar() {
        val kind = ActivityPlaylistBuilder.classify(workProfile(1, bpm = 104.0))
        assertEquals(ActivityPlaylistBuilder.ActivityKind.TRABAJAR, kind)
    }

    @Test
    fun unreliableBpmStillClassifiesByDrive() {
        val train = ActivityPlaylistBuilder.classify(trainProfile(1, bpm = 0.0, confidence = 0.2))
        val relax = ActivityPlaylistBuilder.classify(relaxProfile(2, bpm = 0.0, confidence = 0.1))
        assertEquals(ActivityPlaylistBuilder.ActivityKind.ENTRENAR, train)
        assertEquals(ActivityPlaylistBuilder.ActivityKind.RELAJAR, relax)
    }

    @Test
    fun fastQuietTrackMatchesNothing() {
        val profile = relaxProfile(1, bpm = 150.0)
        assertNull(ActivityPlaylistBuilder.classify(profile))
    }

    @Test
    fun onlyRockLibraryDoesNotInventRelajar() {
        val tracks = (1L..8L).map { id ->
            ActivityPlaylistBuilder.AnalyzedTrack(id, trainProfile(id, bpm = 140.0 + id))
        }
        val built = ActivityPlaylistBuilder.build(tracks)
        assertEquals(setOf(ActivityPlaylistBuilder.ActivityKind.ENTRENAR), built.keys)
        assertEquals(8, built.getValue(ActivityPlaylistBuilder.ActivityKind.ENTRENAR).size)
    }

    @Test
    fun mixedLibraryFillsEachMatchingActivity() {
        val tracks = (1L..4L).map { ActivityPlaylistBuilder.AnalyzedTrack(it, trainProfile(it)) } +
            (5L..8L).map { ActivityPlaylistBuilder.AnalyzedTrack(it, workProfile(it)) } +
            (9L..12L).map { ActivityPlaylistBuilder.AnalyzedTrack(it, relaxProfile(it)) }
        val built = ActivityPlaylistBuilder.build(tracks)
        assertEquals(
            setOf(
                ActivityPlaylistBuilder.ActivityKind.ENTRENAR,
                ActivityPlaylistBuilder.ActivityKind.TRABAJAR,
                ActivityPlaylistBuilder.ActivityKind.RELAJAR
            ),
            built.keys
        )
        assertEquals(4, built.getValue(ActivityPlaylistBuilder.ActivityKind.ENTRENAR).size)
        assertEquals(4, built.getValue(ActivityPlaylistBuilder.ActivityKind.TRABAJAR).size)
        assertEquals(4, built.getValue(ActivityPlaylistBuilder.ActivityKind.RELAJAR).size)
    }

    @Test
    fun fewerThanMinTracksSkipsThatActivity() {
        val tracks = listOf(
            ActivityPlaylistBuilder.AnalyzedTrack(1, trainProfile(1)),
            ActivityPlaylistBuilder.AnalyzedTrack(2, trainProfile(2)),
            ActivityPlaylistBuilder.AnalyzedTrack(3, relaxProfile(3)),
            ActivityPlaylistBuilder.AnalyzedTrack(4, relaxProfile(4)),
            ActivityPlaylistBuilder.AnalyzedTrack(5, relaxProfile(5)),
            ActivityPlaylistBuilder.AnalyzedTrack(6, workProfile(6))
        )
        val built = ActivityPlaylistBuilder.build(tracks)
        assertEquals(setOf(ActivityPlaylistBuilder.ActivityKind.RELAJAR), built.keys)
        assertEquals(3, built.getValue(ActivityPlaylistBuilder.ActivityKind.RELAJAR).size)
    }

    @Test
    fun emptyOrUnclassifiedYieldsNothing() {
        assertTrue(ActivityPlaylistBuilder.build(emptyList()).isEmpty())
        val weird = ActivityPlaylistBuilder.build(
            listOf(ActivityPlaylistBuilder.AnalyzedTrack(1, relaxProfile(1, bpm = 160.0)))
        )
        assertTrue(weird.isEmpty())
    }

    @Test
    fun entrenarRanksHigherDriveFirst() {
        val milder = trainProfile(1, bpm = 128.0, loudness = 0.76f, onset = 0.33f)
        val harder = trainProfile(2, bpm = 168.0, loudness = 0.88f, onset = 0.45f)
        val built = ActivityPlaylistBuilder.build(
            listOf(
                ActivityPlaylistBuilder.AnalyzedTrack(1, milder),
                ActivityPlaylistBuilder.AnalyzedTrack(2, harder),
                ActivityPlaylistBuilder.AnalyzedTrack(3, trainProfile(3))
            )
        )
        val ids = built.getValue(ActivityPlaylistBuilder.ActivityKind.ENTRENAR)
        assertEquals(2L, ids.first())
    }

    private fun trainProfile(
        id: Long,
        bpm: Double = 142.0,
        confidence: Double = 0.8,
        loudness: Float = 0.78f,
        onset: Float = 0.36f
    ) = profile(
        id = id,
        bpm = bpm,
        confidence = confidence,
        energyMean = 0.18,
        silence = 0.02,
        fluxMean = 0.10,
        onsetEmb = onset,
        loudnessEmb = loudness
    )

    private fun workProfile(
        id: Long,
        bpm: Double = 104.0,
        confidence: Double = 0.8
    ) = profile(
        id = id,
        bpm = bpm,
        confidence = confidence,
        energyMean = 0.08,
        silence = 0.06,
        fluxMean = 0.055,
        onsetEmb = 0.18f,
        loudnessEmb = 0.55f
    )

    private fun relaxProfile(
        id: Long,
        bpm: Double = 72.0,
        confidence: Double = 0.8
    ) = profile(
        id = id,
        bpm = bpm,
        confidence = confidence,
        energyMean = 0.02,
        silence = 0.18,
        fluxMean = 0.03,
        onsetEmb = 0.08f,
        loudnessEmb = 0.36f
    )

    private fun profile(
        id: Long,
        bpm: Double,
        confidence: Double,
        energyMean: Double,
        silence: Double,
        fluxMean: Double,
        onsetEmb: Float,
        loudnessEmb: Float
    ): ProfileEntity {
        val emb = FloatArray(16)
        emb[0] = (bpm / 200.0).toFloat().coerceIn(0f, 1f)
        emb[1] = confidence.toFloat()
        emb[2] = energyMean.toFloat().coerceIn(0f, 1f)
        emb[5] = (fluxMean * 5.0).toFloat().coerceIn(0f, 1f)
        emb[6] = silence.toFloat()
        emb[7] = loudnessEmb
        emb[12] = onsetEmb
        return ProfileEntity(
            songId = id,
            status = AnalysisStatus.READY,
            bpm = bpm,
            bpmConfidence = confidence,
            energyMean = energyMean,
            fluxMean = fluxMean,
            silenceRatio = silence,
            loudnessApprox = RhythmAnalysis.loudnessDbfs(energyMean.toFloat()),
            embedding = emb.joinToString(",")
        )
    }
}
