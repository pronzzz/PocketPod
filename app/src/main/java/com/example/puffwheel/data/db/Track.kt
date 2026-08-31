package com.example.puffwheel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey
    val id: String, // Stable ID e.g. MediaStore ID or SAF URI string
    val contentUri: String,
    val sourceType: Int, // e.g. 0 for MediaStore, 1 for SAF
    val displayName: String?,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val genre: String?,
    val duration: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val artworkKey: String?,
    val isAvailable: Boolean = true
)
