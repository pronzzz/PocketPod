package com.pocketpod.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    // Songs
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun getAllTracks(): Flow<List<Track>>
    
    // Albums (Derived from tracks for now to save complexity, or query distinct albums)
    @Query("SELECT DISTINCT album FROM tracks WHERE album IS NOT NULL ORDER BY album COLLATE NOCASE ASC")
    fun getAllAlbumNames(): Flow<List<String>>

    // Artists
    @Query("SELECT DISTINCT artist FROM tracks WHERE artist IS NOT NULL ORDER BY artist COLLATE NOCASE ASC")
    fun getAllArtistNames(): Flow<List<String>>

    // Genres
    @Query("SELECT DISTINCT genre FROM tracks WHERE genre IS NOT NULL ORDER BY genre COLLATE NOCASE ASC")
    fun getAllGenreNames(): Flow<List<String>>

    // Search
    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun searchTracks(query: String): Flow<List<Track>>

    // Recently Played
    @Query("SELECT * FROM tracks WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<Track>>

    // Favorites
    @Query("SELECT * FROM tracks WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavorites(): Flow<List<Track>>

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
