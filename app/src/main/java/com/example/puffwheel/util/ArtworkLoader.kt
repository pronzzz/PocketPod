package com.example.puffwheel.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkLoader {

    suspend fun loadArtwork(context: Context, albumId: Long, width: Int = 200, height: Int = 200): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumId
                    )
                    context.contentResolver.loadThumbnail(contentUri, Size(width, height), null)
                } else {
                    // Fallback for older APIs if necessary, though we target minSdk 30 (Android 11)
                    // so the above branch will always execute.
                    null
                }
            } catch (e: Exception) {
                // FileNotFoundException is thrown if the album has no artwork
                null
            }
        }
    }
}
