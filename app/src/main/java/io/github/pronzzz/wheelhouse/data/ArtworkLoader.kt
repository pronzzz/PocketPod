package io.github.pronzzz.wheelhouse.data

import android.app.ActivityManager
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import io.github.pronzzz.wheelhouse.data.db.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/**
 * Loads album artwork without ever blocking the UI. Callers ask for a bitmap; if it isn't
 * cached yet they get null, and every registered listener is pinged once it arrives.
 */
class ArtworkLoader(private val context: Context) {

    enum class Quality(val px: Int) { THUMB(112), LARGE(480), LITE_LARGE(300) }

    private val cache: LruCache<String, Bitmap>
    private val missing = HashSet<String>()
    private val pending = HashSet<String>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Executors.newFixedThreadPool(2).asCoroutineDispatcher())
    private var notifyQueued = false

    init {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // A slice of the app's heap budget, in KB: ~8 MB on a 64 MB-class phone, max 32 MB.
        val kb = (am.memoryClass * 1024 / 8).coerceIn(4 * 1024, 32 * 1024)
        cache = object : LruCache<String, Bitmap>(kb) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
        }
    }

    fun addListener(l: () -> Unit) = listeners.add(l)
    fun removeListener(l: () -> Unit) = listeners.remove(l)

    /** Returns the cached artwork for an album, or null while it loads (or if there is none). */
    fun album(albumId: Long, sample: Track?, quality: Quality): Bitmap? {
        val key = "$albumId@${quality.px}"
        cache.get(key)?.let { return it }
        synchronized(this) {
            if (key in missing || key in pending) return null
            pending += key
        }
        scope.launch {
            val bmp = runCatching { load(albumId, sample, quality.px) }.getOrNull()
            synchronized(this@ArtworkLoader) {
                pending -= key
                if (bmp == null) missing += key
            }
            if (bmp != null) {
                cache.put(key, bmp)
                scheduleNotify()
            }
        }
        return null
    }

    fun hasNoArt(albumId: Long, quality: Quality) = synchronized(this) { "$albumId@${quality.px}" in missing }

    fun clear() {
        cache.evictAll()
        synchronized(this) { missing.clear() }
    }

    private fun scheduleNotify() {
        main.post {
            if (notifyQueued) return@post
            notifyQueued = true
            main.postDelayed({
                notifyQueued = false
                listeners.forEach { it() }
            }, 60)
        }
    }

    private fun load(albumId: Long, sample: Track?, px: Int): Bitmap? {
        val raw = fromMediaStore(albumId, px)
            ?: sample?.let { fromEmbedded(it, px) }
            ?: sample?.path?.let { fromFolder(it, px) }
            ?: return null
        return squareCrop(raw, px)
    }

    private fun fromMediaStore(albumId: Long, px: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
            context.contentResolver.loadThumbnail(uri, Size(px, px), null)
        } else {
            context.contentResolver.openFileDescriptor(albumArtUri(albumId), "r")?.use { pfd ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, opts)
                opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, px)
                opts.inJustDecodeBounds = false
                BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, opts)
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun fromEmbedded(track: Track, px: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, track.contentUri())
            r.embeddedPicture?.let { decodeBytes(it, px) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    private fun fromFolder(path: String, px: Int): Bitmap? {
        val dir = File(path).parentFile ?: return null
        val names = listOf("cover.jpg", "folder.jpg", "cover.png", "folder.png", "album.jpg", "front.jpg", "AlbumArt.jpg")
        for (n in names) {
            val f = File(dir, n)
            if (f.canRead()) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, opts)
                opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, px)
                opts.inJustDecodeBounds = false
                BitmapFactory.decodeFile(f.path, opts)?.let { return it }
            }
        }
        return null
    }

    private fun decodeBytes(data: ByteArray, px: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, opts)
        opts.inSampleSize = sampleSize(opts.outWidth, opts.outHeight, px)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }

    private fun sampleSize(w: Int, h: Int, px: Int): Int {
        var s = 1
        while (w / (s * 2) >= px && h / (s * 2) >= px) s *= 2
        return s
    }

    private fun squareCrop(src: Bitmap, px: Int): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val cropped = if (x == 0 && y == 0) src else Bitmap.createBitmap(src, x, y, side, side)
        return if (side > px) Bitmap.createScaledBitmap(cropped, px, px, true) else cropped
    }
}
