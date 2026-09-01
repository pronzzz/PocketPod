package com.pocketpod.app.artwork.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ArtworkLoader(private val context: Context) {

    // Calculate memory cache size based on available memory (J2 Pro has limited memory)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of the available memory for this memory cache.
    private val cacheSize = maxMemory / 8

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // The cache size will be measured in kilobytes rather than number of items.
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadArtwork(filePath: String?, artworkId: String, sizeLimit: Int = 500): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath == null) return@withContext null

        // 1. Check Memory Cache
        memoryCache.get(artworkId)?.let { return@withContext it }

        // 2. Check Embedded ID3 Artwork
        var bitmap = extractEmbeddedArtwork(filePath, sizeLimit)
        
        // 3. Check Local Folder Artwork (cover.jpg, folder.jpg)
        if (bitmap == null) {
            bitmap = extractLocalFolderArtwork(filePath, sizeLimit)
        }

        // 4. Update Memory Cache if found
        bitmap?.let { 
            memoryCache.put(artworkId, it) 
        }

        return@withContext bitmap
    }

    private fun extractEmbeddedArtwork(filePath: String, sizeLimit: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val byteData = retriever.embeddedPicture
            if (byteData != null) {
                decodeSampledBitmapFromByteArray(byteData, sizeLimit, sizeLimit)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    private fun extractLocalFolderArtwork(filePath: String, sizeLimit: Int): Bitmap? {
        val folder = File(filePath).parentFile ?: return null
        val commonNames = listOf("cover.jpg", "cover.jpeg", "folder.jpg", "folder.png", "album.jpg", "front.jpg")
        
        for (name in commonNames) {
            val imageFile = File(folder, name)
            if (imageFile.exists()) {
                return decodeSampledBitmapFromFile(imageFile.absolutePath, sizeLimit, sizeLimit)
            }
        }
        return null
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(data, 0, data.size, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
