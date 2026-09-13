package com.example.aether.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marks",
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
data class MarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val positionMs: Long,
    val createdAtMs: Long = System.currentTimeMillis(),
    val label: String? = null
)
