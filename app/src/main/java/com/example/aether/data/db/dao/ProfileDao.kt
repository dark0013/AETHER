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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)
}
