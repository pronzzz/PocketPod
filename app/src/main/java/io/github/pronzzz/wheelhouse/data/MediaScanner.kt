package io.github.pronzzz.wheelhouse.data

import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.pronzzz.wheelhouse.data.db.Track
import io.github.pronzzz.wheelhouse.util.Text
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/** Reads the song list from Android's MediaStore. */
class MediaScanner(private val context: Context) {

    fun query(): List<Track> {
        val cr = context.contentResolver
        val cols = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
        )
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        if (modern) {
            cols += "album_artist"
            cols += "genre"
            cols += "disc_number"
        }
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val genres = if (modern) emptyMap() else legacyGenres()
        val out = ArrayList<Track>()
        cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols.toTypedArray(), selection, null, null)?.use { c ->
            val iId = c.idx(MediaStore.Audio.Media._ID)
            val iTitle = c.idx(MediaStore.Audio.Media.TITLE)
            val iArtist = c.idx(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.idx(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.idx(MediaStore.Audio.Media.ALBUM_ID)
            val iYear = c.idx(MediaStore.Audio.Media.YEAR)
            val iDur = c.idx(MediaStore.Audio.Media.DURATION)
            val iTrack = c.idx(MediaStore.Audio.Media.TRACK)
            val iData = c.idx(MediaStore.Audio.Media.DATA)
            val iAdded = c.idx(MediaStore.Audio.Media.DATE_ADDED)
            val iMod = c.idx(MediaStore.Audio.Media.DATE_MODIFIED)
            val iSize = c.idx(MediaStore.Audio.Media.SIZE)
            val iAlbumArtist = c.getColumnIndex("album_artist")
            val iGenre = c.getColumnIndex("genre")
            val iDisc = c.getColumnIndex("disc_number")
            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val duration = c.longOrZero(iDur)
                if (duration in 1 until 3_000) continue // skip notification blips
                val rawTrack = c.intOrZero(iTrack)
                val path = c.stringOrNull(iData)
                val fileName = path?.substringAfterLast('/')?.substringBeforeLast('.')
                val disc = if (iDisc >= 0) c.stringOrNull(iDisc)?.substringBefore('/')?.toIntOrNull() ?: 0 else rawTrack / 1000
                out += Track(
                    id = id,
                    title = Text.clean(c.stringOrNull(iTitle), fileName ?: "Unknown Song"),
                    artist = Text.clean(c.stringOrNull(iArtist), "Unknown Artist"),
                    album = Text.clean(c.stringOrNull(iAlbum), "Unknown Album"),
                    albumArtist = if (iAlbumArtist >= 0) c.stringOrNull(iAlbumArtist)?.takeIf { it.isNotBlank() && it != "<unknown>" } else null,
                    albumId = c.longOrZero(iAlbumId),
                    genre = (if (iGenre >= 0) c.stringOrNull(iGenre) else genres[id])?.trim()?.takeIf { it.isNotEmpty() },
                    year = c.intOrZero(iYear),
                    durationMs = duration,
                    trackNumber = rawTrack % 1000,
                    discNumber = disc,
                    path = path,
                    dateAdded = c.longOrZero(iAdded),
                    dateModified = c.longOrZero(iMod),
                    size = c.longOrZero(iSize),
                )
            }
        }
        return out
    }

    /** Before Android 11 the genre lives in a separate table. */
    private fun legacyGenres(): Map<Long, String> {
        val map = HashMap<Long, String>()
        try {
            @Suppress("DEPRECATION")
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, null,
            )?.use { g ->
                while (g.moveToNext()) {
                    val gid = g.getLong(0)
                    val name = g.getString(1) ?: continue
                    context.contentResolver.query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", gid),
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null,
                    )?.use { m -> while (m.moveToNext()) map[m.getLong(0)] = name }
                }
            }
        } catch (_: Exception) {
            // Some OEM builds lack the genres table; genres are optional.
        }
        return map
    }

    /**
     * Asks Android to index audio files in the usual music folders. This catches files
     * that were copied with adb or a file manager and never got picked up by the system.
     */
    suspend fun indexMusicFolders() {
        val roots = listOf(Environment.DIRECTORY_MUSIC, Environment.DIRECTORY_DOWNLOADS, "Audio")
            .map { File(Environment.getExternalStorageDirectory(), it) }
            .filter { it.isDirectory }
        val exts = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "mka", "amr")
        val files = ArrayList<String>()
        fun walk(dir: File, depth: Int) {
            if (depth > 8 || files.size > 20_000) return
            val children = try { dir.listFiles() } catch (_: SecurityException) { null } ?: return
            for (f in children) {
                if (f.isDirectory) { if (!f.name.startsWith(".")) walk(f, depth + 1) }
                else if (f.extension.lowercase() in exts) files += f.absolutePath
            }
        }
        roots.forEach { walk(it, 0) }
        if (files.isEmpty()) return
        suspendCancellableCoroutine { cont ->
            var remaining = files.size
            MediaScannerConnection.scanFile(context, files.toTypedArray(), null) { _, _ ->
                synchronized(this) {
                    remaining--
                    if (remaining == 0 && cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private fun Cursor.idx(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.stringOrNull(i: Int): String? = if (i < 0 || isNull(i)) null else getString(i)
    private fun Cursor.longOrZero(i: Int): Long = if (i < 0 || isNull(i)) 0L else getLong(i)
    private fun Cursor.intOrZero(i: Int): Int = if (i < 0 || isNull(i)) 0 else getInt(i)
}
