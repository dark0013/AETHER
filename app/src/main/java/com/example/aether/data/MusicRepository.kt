package com.example.aether.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.aether.analysis.AnalysisScheduler
import com.example.aether.data.db.AetherDatabase
import com.example.aether.data.db.entities.AnalysisStatus
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.MarkEntity
import com.example.aether.data.db.entities.PlaylistEntity
import com.example.aether.data.db.entities.PlaylistSummary
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.data.db.entities.SessionEntity
import com.example.aether.data.db.entities.toDomain
import com.example.aether.data.db.entities.toEntity
import com.example.aether.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val db = AetherDatabase.getDatabase(context)
    private val songDao = db.songDao()
    private val profileDao = db.profileDao()
    private val tapeDao = db.densityTapeDao()
    private val markDao = db.markDao()
    private val sessionDao = db.sessionDao()
    private val playlistDao = db.playlistDao()
    private val analysisScheduler = AnalysisScheduler.getInstance(context)

    fun getPlaylistsFlow(): Flow<List<PlaylistSummary>> = playlistDao.observePlaylists()

    suspend fun getPlaylist(id: Long): PlaylistEntity? = withContext(Dispatchers.IO) {
        playlistDao.getPlaylist(id)
    }

    suspend fun getPlaylistSongs(playlistId: Long): List<Song> = withContext(Dispatchers.IO) {
        playlistDao.getSongsForPlaylist(playlistId).map { it.toDomain() }
    }

    suspend fun savePlaylist(id: Long?, name: String, songIds: List<Long>): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim().ifBlank { "Lista sin nombre" }
        val now = System.currentTimeMillis()
        val playlistId = if (id == null) {
            playlistDao.insertPlaylist(PlaylistEntity(name = trimmed, createdAtMs = now, updatedAtMs = now))
        } else {
            val existing = playlistDao.getPlaylist(id)
            if (existing != null) {
                playlistDao.updatePlaylist(existing.copy(name = trimmed, updatedAtMs = now))
                id
            } else {
                playlistDao.insertPlaylist(PlaylistEntity(name = trimmed, createdAtMs = now, updatedAtMs = now))
            }
        }
        playlistDao.replaceTracks(playlistId, songIds)
        playlistId
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
    }

    fun getSongsFlow(): Flow<List<Song>> = songDao.getAllSongs().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getProfileFlow(songId: Long): Flow<ProfileEntity?> = profileDao.observeProfileForSong(songId)
    
    fun getTapeFlow(songId: Long): Flow<DensityTapeEntity?> = tapeDao.observeTapeForSong(songId)

    suspend fun getTapesForSongs(songIds: List<Long>): List<DensityTapeEntity> = withContext(Dispatchers.IO) {
        tapeDao.getTapesForSongs(songIds)
    }

    fun getMarksFlow(songId: Long): Flow<List<MarkEntity>> = markDao.observeMarksForSong(songId)

    suspend fun addMark(songId: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        markDao.insertMark(MarkEntity(songId = songId, positionMs = positionMs))
    }

    suspend fun startSession(mode: String): Long = withContext(Dispatchers.IO) {
        sessionDao.insertSession(SessionEntity(mode = mode))
    }

    suspend fun appendTrackToSession(sessionId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext
        val updatedTracks = if (session.trackIds.isBlank()) {
            songId.toString()
        } else {
            "${session.trackIds},$songId"
        }
        sessionDao.updateSession(session.copy(trackIds = updatedTracks))
    }

    suspend fun endSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext
        sessionDao.updateSession(session.copy(endTimeMs = System.currentTimeMillis()))
    }

    fun getActiveSessionFlow(): Flow<SessionEntity?> = sessionDao.observeActiveSession()

    suspend fun getActiveSession(): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getActiveSession()
    }

    suspend fun getReadyProfilesWithSongs(): List<Pair<Song, ProfileEntity>> = withContext(Dispatchers.IO) {
        val readyProfiles = profileDao.getAllReadyProfiles()
        val songsById = songDao.getAllSongsList().associateBy { it.id }

        readyProfiles.mapNotNull { profile ->
            val song = songsById[profile.songId]?.toDomain()
            if (song != null) song to profile else null
        }
    }

    suspend fun syncWithMediaStore() = withContext(Dispatchers.IO) {
        val mediaStoreSongs = mutableListOf<Song>()
        
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val selection = StringBuilder().apply {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} > 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} >= 30000")
            append(" AND ${MediaStore.Audio.Media.IS_RINGTONE} == 0")
            append(" AND ${MediaStore.Audio.Media.IS_ALARM} == 0")
            append(" AND ${MediaStore.Audio.Media.IS_NOTIFICATION} == 0")
        }.toString()

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: ""
                if (isExcludedPath(path)) continue

                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn) ?: ""
                val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } 
                    ?: displayName.substringBeforeLast(".")
                val artist = cursor.getString(artistColumn)?.takeIf { it.isNotBlank() && it != "<unknown>" } 
                    ?: "Artista desconocido"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                mediaStoreSongs.add(Song(id, contentUri, title, artist, duration, displayName, albumArtUri, size, dateModified))
            }
        }

        val existingSongs = songDao.getAllSongsList()
        val existingById = existingSongs.associateBy { it.id }
        val mediaStoreSongIds = mediaStoreSongs.map { it.id }.toSet()

        val songsToDelete = existingSongs.filter { entity ->
            entity.source != Song.SOURCE_IMPORT && entity.id !in mediaStoreSongIds
        }
        if (songsToDelete.isNotEmpty()) {
            songDao.deleteSongs(songsToDelete)
        }

        mediaStoreSongs.forEach { song ->
            val existing = existingById[song.id]
            val needsUpsert = existing == null || existing.size != song.size || existing.dateModified != song.dateModified
            
            if (needsUpsert) {
                songDao.insertSongs(listOf(song.toEntity()))
                
                // Si cambió o es nueva, invalidamos/creamos perfil PENDING
                profileDao.upsertProfile(ProfileEntity(
                    songId = song.id,
                    status = AnalysisStatus.PENDING
                ))
                
                // Programamos análisis en background
                analysisScheduler.scheduleBackgroundAnalysis(song.id)
            }
        }
    }

    suspend fun getLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        syncWithMediaStore()
        rescanImported()
        songDao.getAllSongsList().map { it.toDomain() }
    }

    suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        MusicImport.persistUri(context, treeUri, isTree = true)
        upsertImported(MusicImport.collectFromTree(context, treeUri).mapNotNull { MusicImport.readSong(context, it) })
    }

    suspend fun importFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        uris.forEach { MusicImport.persistUri(context, it, isTree = false) }
        upsertImported(uris.mapNotNull { MusicImport.readSong(context, it) })
    }

    suspend fun rescanImported(): Int = withContext(Dispatchers.IO) {
        val fromTrees = MusicImport.persistedTrees(context).flatMap { tree ->
            MusicImport.collectFromTree(context, tree)
        }
        val fromFiles = MusicImport.persistedFiles(context)
        upsertImported((fromTrees + fromFiles).distinct().mapNotNull { MusicImport.readSong(context, it) })
    }

    private suspend fun upsertImported(songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        val existingByUri = songDao.getAllSongsList().associateBy { it.contentUri }
        var added = 0
        songs.forEach { song ->
            val existing = existingByUri[song.contentUri.toString()]
            val needsUpsert = existing == null || existing.size != song.size
            if (needsUpsert) {
                songDao.insertSongs(listOf(song.toEntity()))
                profileDao.upsertProfile(
                    ProfileEntity(songId = song.id, status = AnalysisStatus.PENDING)
                )
                analysisScheduler.scheduleBackgroundAnalysis(song.id)
                added++
            }
        }
        return added
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
