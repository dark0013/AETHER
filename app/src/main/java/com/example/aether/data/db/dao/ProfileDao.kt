package com.example.aether.data.db.dao

import androidx.room.*
import com.example.aether.data.db.entities.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE songId = :songId")
    suspend fun getProfileForSong(songId: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE songId = :songId")
    fun observeProfileForSong(songId: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE status = 'READY'")
    suspend fun getAllReadyProfiles(): List<ProfileEntity>

    @Query("SELECT songId FROM profiles WHERE schemaVersion < :version")
    suspend fun getOutdatedSongIds(version: Int): List<Long>

    @Query("SELECT songId, status FROM profiles")
    fun observeStatuses(): Flow<List<ProfileStatusRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)
}

data class ProfileStatusRow(
    val songId: Long,
    val status: String
)
