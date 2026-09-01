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

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY dateCreated ASC")
    fun getAllPlaylists(): kotlinx.coroutines.flow.Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, newName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.position ASC")
    fun getTracksForPlaylist(playlistId: String): kotlinx.coroutines.flow.Flow<List<Track>>
}
