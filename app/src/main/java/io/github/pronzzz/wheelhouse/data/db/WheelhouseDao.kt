package io.github.pronzzz.wheelhouse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WheelhouseDao {

    // Tracks
    @Query("SELECT * FROM tracks")
    suspend fun allTracks(): List<Track>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<Track>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksChunk(ids: List<Long>)

    @Transaction
    suspend fun deleteTracks(ids: List<Long>) {
        // SQLite caps bound variables at 999 on older Android versions.
        ids.chunked(500).forEach { deleteTracksChunk(it) }
    }

    // Stats
    @Query("SELECT * FROM track_stats")
    suspend fun allStats(): List<TrackStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: TrackStats)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    suspend fun playlists(): List<Playlist>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistRow(id: Long)

    @Query("SELECT * FROM playlist_items ORDER BY playlistId, position")
    suspend fun allPlaylistItems(): List<PlaylistItem>

    @Query("DELETE FROM playlist_items WHERE playlistId = :id")
    suspend fun clearPlaylist(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItem>)

    @Transaction
    suspend fun setPlaylistTracks(id: Long, trackIds: List<Long>) {
        clearPlaylist(id)
        insertPlaylistItems(trackIds.mapIndexed { i, t -> PlaylistItem(id, i, t) })
    }

    @Transaction
    suspend fun deletePlaylist(id: Long) {
        clearPlaylist(id)
        deletePlaylistRow(id)
    }
}
