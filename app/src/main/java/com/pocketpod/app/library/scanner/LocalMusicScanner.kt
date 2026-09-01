package com.pocketpod.app.library.scanner

import android.content.Context
import android.provider.MediaStore
import com.pocketpod.app.core.database.Track
import com.pocketpod.app.core.database.LibraryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class ScanProgress(
    val status: String,
    val itemsProcessed: Int,
    val totalItems: Int,
    val percent: Int
)

class LocalMusicScanner(
    private val context: Context,
    private val libraryDao: LibraryDao
) {

    fun scanLibrary(): Flow<ScanProgress> = flow {
        emit(ScanProgress("Initializing scan...", 0, 0, 0))
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )
        
        if (cursor == null) {
            emit(ScanProgress("Error: Could not access MediaStore", 0, 0, 0))
            return@flow
        }
        
        val totalItems = cursor.count
        if (totalItems == 0) {
            emit(ScanProgress("No music found", 0, 0, 100))
            cursor.close()
            return@flow
        }
        
        emit(ScanProgress("Finding your music...", 0, totalItems, 0))
        
        // Fetch existing tracks for incremental scanning
        val existingTracks = libraryDao.getAllTracksSnapshot().associateBy { it.id }
        val processedIds = mutableSetOf<String>()
        val tracksToInsert = mutableListOf<Track>()
        
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumArtistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
        val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        
        var processed = 0
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn).toString()
            val uri = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id"
            val path = cursor.getString(dataColumn)
            val title = cursor.getString(titleColumn) ?: "Unknown Title"
            val artist = cursor.getString(artistColumn)
            val album = cursor.getString(albumColumn)
            val albumArtist = if (albumArtistColumn >= 0) cursor.getString(albumArtistColumn) else null
            val year = cursor.getInt(yearColumn).takeIf { it > 0 }
            val duration = cursor.getLong(durationColumn)
            val trackNum = cursor.getInt(trackColumn).takeIf { it > 0 }
            val size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(modifiedColumn)
            
            processedIds.add(id)
            
            val existing = existingTracks[id]
            val hasChanged = existing == null || existing.dateModified != dateModified || existing.fileSize != size
            
            if (hasChanged) {
                val track = Track(
                    id = id,
                    uri = uri,
                    path = path,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = null, // Requires additional query or extraction
                    year = year,
                    duration = duration,
                    trackNumber = trackNum,
                    discNumber = null,
                    composer = null,
                    bitrate = null,
                    sampleRate = null,
                    format = path?.substringAfterLast('.'),
                    fileSize = size,
                    dateModified = dateModified,
                    artworkId = id,
                    fingerprint = null
                )
                tracksToInsert.add(track)
            }
            
            processed++
            
            // Batch inserts & progress updates
            if (processed % 50 == 0 || processed == totalItems) {
                if (tracksToInsert.isNotEmpty()) {
                    libraryDao.insertTracks(tracksToInsert)
                    tracksToInsert.clear()
                }
                val percent = (processed * 100f / totalItems).toInt()
                emit(ScanProgress("Scanning...", processed, totalItems, percent))
            }
        }
        
        cursor.close()
        
        // Remove tracks that were deleted from device
        val deletedIds = existingTracks.keys - processedIds
        if (deletedIds.isNotEmpty()) {
            libraryDao.deleteTracksById(deletedIds.toList())
        }
        
        emit(ScanProgress("Scan complete", totalItems, totalItems, 100))
        
    }.flowOn(Dispatchers.IO)
}
