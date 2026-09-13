package com.example.aether.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.aether.data.db.dao.DensityTapeDao
import com.example.aether.data.db.dao.MarkDao
import com.example.aether.data.db.dao.PlaylistDao
import com.example.aether.data.db.dao.ProfileDao
import com.example.aether.data.db.dao.SessionDao
import com.example.aether.data.db.dao.SongDao
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.MarkEntity
import com.example.aether.data.db.entities.PlaylistEntity
import com.example.aether.data.db.entities.PlaylistTrackEntity
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.data.db.entities.SessionEntity
import com.example.aether.data.db.entities.SongEntity

@Database(
    entities = [
        SongEntity::class,
        ProfileEntity::class,
        DensityTapeEntity::class,
        MarkEntity::class,
        SessionEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AetherDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun profileDao(): ProfileDao
    abstract fun densityTapeDao(): DensityTapeDao
    abstract fun markDao(): MarkDao
    abstract fun sessionDao(): SessionDao
    abstract fun playlistDao(): PlaylistDao

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
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAtMs INTEGER NOT NULL,
                        updatedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_tracks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_playlistId ON playlist_tracks(playlistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_songId ON playlist_tracks(songId)")
            }
        }
    }
}
