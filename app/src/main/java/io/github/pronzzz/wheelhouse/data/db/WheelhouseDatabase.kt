package io.github.pronzzz.wheelhouse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Track::class, TrackStats::class, Playlist::class, PlaylistItem::class],
    version = 1,
    exportSchema = false,
)
abstract class WheelhouseDatabase : RoomDatabase() {
    abstract fun dao(): WheelhouseDao

    companion object {
        fun create(context: Context): WheelhouseDatabase =
            Room.databaseBuilder(context, WheelhouseDatabase::class.java, "wheelhouse.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
