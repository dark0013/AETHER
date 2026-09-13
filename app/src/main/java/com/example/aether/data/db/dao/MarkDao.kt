package com.example.aether.data.db.dao

import androidx.room.*
import com.example.aether.data.db.entities.MarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks WHERE songId = :songId ORDER BY positionMs ASC")
    fun observeMarksForSong(songId: Long): Flow<List<MarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(mark: MarkEntity)

    @Delete
    suspend fun deleteMark(mark: MarkEntity)
}
