package com.example.aether.data.db.dao

import androidx.room.*
import com.example.aether.data.db.entities.DensityTapeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DensityTapeDao {
    @Query("SELECT * FROM density_tapes WHERE songId = :songId")
    suspend fun getTapeForSong(songId: Long): DensityTapeEntity?

    @Query("SELECT * FROM density_tapes WHERE songId = :songId")
    fun observeTapeForSong(songId: Long): Flow<DensityTapeEntity?>

    @Query("SELECT * FROM density_tapes WHERE songId IN (:songIds)")
    suspend fun getTapesForSongs(songIds: List<Long>): List<DensityTapeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTape(tape: DensityTapeEntity)
}
