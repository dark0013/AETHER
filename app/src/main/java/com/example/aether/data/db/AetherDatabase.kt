package com.example.aether.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.aether.data.db.dao.DensityTapeDao
import com.example.aether.data.db.dao.MarkDao
import com.example.aether.data.db.dao.ProfileDao
import com.example.aether.data.db.dao.SessionDao
import com.example.aether.data.db.dao.SongDao
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.MarkEntity
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.data.db.entities.SessionEntity
import com.example.aether.data.db.entities.SongEntity

@Database(
    entities = [SongEntity::class, ProfileEntity::class, DensityTapeEntity::class, MarkEntity::class, SessionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun profileDao(): ProfileDao
    abstract fun densityTapeDao(): DensityTapeDao
    abstract fun markDao(): MarkDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AetherDatabase? = null

        fun getDatabase(context: Context): AetherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AetherDatabase::class.java,
                    "aether_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
