package com.example.aether.data.db.dao

import androidx.room.*
import com.example.aether.data.db.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: Long)
}
