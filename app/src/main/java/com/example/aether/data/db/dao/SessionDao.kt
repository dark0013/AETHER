package com.example.aether.data.db.dao

import androidx.room.*
import com.example.aether.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun observeAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE endTimeMs IS NULL ORDER BY startTimeMs DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endTimeMs IS NULL ORDER BY startTimeMs DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?
}
