package com.example.aether.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long? = null,
    val mode: String, // "presence" | "ritual"
    val trackIds: String = "" // Comma-separated track IDs
)
