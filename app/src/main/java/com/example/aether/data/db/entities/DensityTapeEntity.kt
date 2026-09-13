package com.example.aether.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "density_tapes",
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
data class DensityTapeEntity(
    @PrimaryKey val songId: Long,
    val frameCount: Int,
    val hopMs: Int,
    val energyTape: ByteArray,
    val centroidTape: ByteArray,
    val fluxTape: ByteArray,
    val onsetFlags: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DensityTapeEntity
        if (songId != other.songId) return false
        return true
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }
}
