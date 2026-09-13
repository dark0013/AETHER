package com.example.aether.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.aether.model.Song
import android.net.Uri

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val displayName: String,
    val albumArtUri: String?,
    val size: Long,
    val dateModified: Long
)

fun SongEntity.toDomain(): Song = Song(
    id = id,
    contentUri = Uri.parse(contentUri),
    title = title,
    artist = artist,
    duration = duration,
    displayName = displayName,
    albumArtUri = albumArtUri?.let { Uri.parse(it) },
    size = size,
    dateModified = dateModified
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    contentUri = contentUri.toString(),
    title = title,
    artist = artist,
    duration = duration,
    displayName = displayName,
    albumArtUri = albumArtUri?.toString(),
    size = size,
    dateModified = dateModified
)
