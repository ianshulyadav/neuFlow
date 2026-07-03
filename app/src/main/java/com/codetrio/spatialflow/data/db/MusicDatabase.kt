package com.codetrio.spatialflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codetrio.spatialflow.data.cache.CachedColorScheme
import com.codetrio.spatialflow.data.cache.ColorSchemeDao

@Database(entities = [PlaylistEntity::class, PlaylistSongEntity::class, HistoryEventEntity::class, CachedColorScheme::class], version = 4, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun colorSchemeDao(): ColorSchemeDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "spatialflow_music_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlist_songs ADD COLUMN lufs REAL DEFAULT NULL")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `color_schemes` (`uri` TEXT NOT NULL, `paletteStyle` TEXT NOT NULL, `lightSchemeJson` TEXT NOT NULL, `darkSchemeJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
            }
        }
    }
}
