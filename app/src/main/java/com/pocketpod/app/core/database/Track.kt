package com.pocketpod.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "tracks",
    indices = [
        Index("title"),
        Index("artist"),
        Index("album"),
        Index("genre"),
        Index("albumArtist")
    ]
)
data class Track(
    @PrimaryKey val id: String,
    val uri: String,
    val path: String?,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val year: Int?,
    val duration: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val composer: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val format: String?,
    val fileSize: Long,
    val dateModified: Long,
    val artworkId: String?,
    val fingerprint: String?,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long? = null
)
