package com.example.puffwheel.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Track::class, Album::class, Artist::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
