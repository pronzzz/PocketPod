package io.github.pronzzz.wheelhouse.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A song, mirrored from Android's MediaStore. [id] is the MediaStore audio id. */
@Entity(tableName = "tracks", indices = [Index("albumId")])
data class Track(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val albumId: Long,
    val genre: String?,
    val year: Int,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val path: String?,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
)

/** Listening history kept separately so a rescan never wipes it. */
@Entity(tableName = "track_stats")
data class TrackStats(
    @PrimaryKey val trackId: Long,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val loved: Boolean = false,
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "position"],
    indices = [Index("playlistId")],
)
data class PlaylistItem(
    val playlistId: Long,
    val position: Int,
    val trackId: Long,
)
