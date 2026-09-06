package com.example.aether.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.aether.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun getLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = StringBuilder().apply {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            // Omitir audios muy cortos o basura (como EVT_IO_*) con duración 0
            append(" AND ${MediaStore.Audio.Media.DURATION} > 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} >= 30000")
            // Omitir tonos, alarmas y notificaciones explícitamente
            append(" AND ${MediaStore.Audio.Media.IS_RINGTONE} == 0")
            append(" AND ${MediaStore.Audio.Media.IS_ALARM} == 0")
            append(" AND ${MediaStore.Audio.Media.IS_NOTIFICATION} == 0")
        }.toString()

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: ""
                
                // Filtros adicionales por ruta para excluir carpetas específicas de apps
                if (isExcludedPath(path)) continue

                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn) ?: ""
                
                // Lógica de "AETHER Look": usar nombre de archivo si el título está vacío
                val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } 
                    ?: displayName.substringBeforeLast(".")
                
                val artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } 
                    ?: "Artista desconocido"
                
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                songs.add(Song(id, contentUri, title, artist, duration, displayName, albumArtUri))
            }
        }
        songs
    }

    private fun isExcludedPath(path: String): Boolean {
        val excludedFolders = listOf(
            "WhatsApp",
            "Telegram",
            "Notifications",
            "Ringtones",
            "Alarms",
            "CallRecordings",
            "VoiceRecorder",
            "System"
        )
        return excludedFolders.any { path.contains(it, ignoreCase = true) }
    }
}
