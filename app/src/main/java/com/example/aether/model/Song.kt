package com.example.aether.model

import android.net.Uri

data class Song(
    val id: Long,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val duration: Long,
    val displayName: String,
    val albumArtUri: Uri? = null,
    val size: Long,
    val dateModified: Long
)
