package io.github.pronzzz.wheelhouse.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import io.github.pronzzz.wheelhouse.data.db.Track

data class Album(
    val albumId: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
    val sortKey: String,
)

data class Artist(
    val name: String,
    val albums: List<Album>,
    val tracks: List<Track>,
    val sortKey: String,
)

data class Genre(val name: String, val tracks: List<Track>)

fun Track.contentUri(): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
fun albumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)
