package com.example.aether.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.aether.model.Song
import kotlin.math.abs

internal object MusicImport {
    private const val PREFS = "aether_imports"
    private const val KEY_TREES = "trees"
    private const val KEY_FILES = "files"
    private const val ID_BASE = 1_000_000_000_000L

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "alac"
    )

    fun persistUri(context: Context, uri: Uri, isTree: Boolean) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (isTree) KEY_TREES else KEY_FILES
        val next = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        next.add(uri.toString())
        prefs.edit().putStringSet(key, next).apply()
    }

    fun persistedTrees(context: Context): List<Uri> = readUris(context, KEY_TREES)

    fun persistedFiles(context: Context): List<Uri> = readUris(context, KEY_FILES)

    fun collectFromTree(context: Context, treeUri: Uri): List<Uri> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val found = mutableListOf<Uri>()
        walk(root, found)
        return found
    }

    fun readSong(context: Context, uri: Uri): Song? {
        val displayName = DocumentFile.fromSingleUri(context, uri)?.name
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Audio"
        if (!looksLikeAudio(displayName, context.contentResolver.getType(uri))) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: displayName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Artista desconocido"
            val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            Song(
                id = importedId(uri),
                contentUri = uri,
                title = title,
                artist = artist,
                duration = duration,
                displayName = displayName,
                albumArtUri = null,
                size = size,
                dateModified = System.currentTimeMillis(),
                source = Song.SOURCE_IMPORT
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun importedId(uri: Uri): Long {
        val hash = abs(uri.toString().hashCode().toLong())
        return ID_BASE + hash
    }

    private fun walk(dir: DocumentFile, out: MutableList<Uri>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                walk(child, out)
            } else {
                val name = child.name ?: continue
                val mime = child.type
                if (looksLikeAudio(name, mime)) {
                    out.add(child.uri)
                }
            }
        }
    }

    private fun looksLikeAudio(name: String, mime: String?): Boolean {
        if (mime?.startsWith("audio/") == true) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in audioExtensions
    }

    private fun readUris(context: Context, key: String): List<Uri> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(key, emptySet()).orEmpty().mapNotNull {
            runCatching { Uri.parse(it) }.getOrNull()
        }
    }
}
