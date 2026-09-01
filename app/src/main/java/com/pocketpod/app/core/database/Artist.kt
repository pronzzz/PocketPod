package com.pocketpod.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey
    val artistKey: String,
    val name: String,
    val sortName: String?,
    val albumCount: Int = 0,
    val trackCount: Int = 0
)
