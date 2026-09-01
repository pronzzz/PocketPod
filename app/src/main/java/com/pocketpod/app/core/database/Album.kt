package com.pocketpod.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey
    val albumKey: String, // E.g. composite of album name and artist
    val title: String,
    val albumArtist: String?,
    val artworkKey: String?,
    val year: Int?,
    val trackCount: Int = 0
)
