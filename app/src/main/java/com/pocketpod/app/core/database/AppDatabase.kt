package com.pocketpod.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Track::class, 
        Album::class, 
        Artist::class,
        Playlist::class,
        PlaylistTrack::class
    ], 
    version = 2, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
