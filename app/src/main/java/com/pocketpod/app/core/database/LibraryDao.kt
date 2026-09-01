package com.pocketpod.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun getAllTracks(): Flow<List<Track>>
    
    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSnapshot(): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Query("DELETE FROM tracks WHERE id IN (:trackIds)")
    suspend fun deleteTracksById(trackIds: List<String>)

    @Query("DELETE FROM tracks")
    suspend fun clearAllTracks()
}
