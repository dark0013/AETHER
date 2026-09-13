package com.example.aether.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.aether.data.db.entities.PlaylistEntity
import com.example.aether.data.db.entities.PlaylistSummary
import com.example.aether.data.db.entities.PlaylistTrackEntity
import com.example.aether.data.db.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.name, p.createdAtMs, p.updatedAtMs, COUNT(t.id) AS trackCount
        FROM playlists p
        LEFT JOIN playlist_tracks t ON t.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.updatedAtMs DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTracks(playlistId: Long): List<PlaylistTrackEntity>

    @Query(
        """
        SELECT s.* FROM songs s
        INNER JOIN playlist_tracks t ON t.songId = s.id
        WHERE t.playlistId = :playlistId
        ORDER BY t.position ASC
        """
    )
    suspend fun getSongsForPlaylist(playlistId: Long): List<SongEntity>

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<PlaylistTrackEntity>)

    @Transaction
    suspend fun replaceTracks(playlistId: Long, songIds: List<Long>) {
        clearTracks(playlistId)
        if (songIds.isEmpty()) return
        insertTracks(
            songIds.mapIndexed { index, songId ->
                PlaylistTrackEntity(playlistId = playlistId, songId = songId, position = index)
            }
        )
    }
}
