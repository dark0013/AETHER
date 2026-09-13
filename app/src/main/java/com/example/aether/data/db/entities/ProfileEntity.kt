package com.example.aether.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["songId"])]
)
data class ProfileEntity(
    @PrimaryKey val songId: Long,
    val status: AnalysisStatus = AnalysisStatus.PENDING,
    val schemaVersion: Int = 1,
    val analyzedAtMs: Long = 0,
    val sampleRateUsed: Int = 22050,
    val hopMs: Int = 93,
    
    // Rhythmic features
    val bpm: Double? = null,
    val bpmConfidence: Double? = null,
    val beatGridOffsetMs: Int? = null,
    
    // Global energy/spectral features
    val loudnessApprox: Double? = null,
    val energyMean: Double? = null,
    val energyStd: Double? = null,
    val centroidMean: Double? = null,
    val centroidNorm: Double? = null,
    val fluxMean: Double? = null,
    val silenceRatio: Double? = null,
    
    // Structure and Similarity
    val sectionBoundsMs: String? = null, // Stored as comma-separated ints
    val embedding: String? = null,       // Stored as comma-separated floats (16 dims)
    
    val error: String? = null
)

enum class AnalysisStatus {
    PENDING, READY, ERROR
}
