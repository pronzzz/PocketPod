package com.pocketpod.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String?,
    val releaseYear: Int?,
    val artworkId: String?
)
