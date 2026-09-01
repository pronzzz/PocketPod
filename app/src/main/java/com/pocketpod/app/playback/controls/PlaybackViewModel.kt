package com.pocketpod.app.playback.controls

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.pocketpod.app.core.database.Track

class PlaybackViewModel : ViewModel() {

    private fun trackToMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist ?: "Unknown Artist")
            .setAlbumTitle(track.album)
            .build()
            
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(metadata)
            .build()
    }

    fun playTrack(mediaController: MediaController?, track: Track) {
        if (mediaController == null) return
        
        val mediaItem = trackToMediaItem(track)
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()
    }

    fun playQueue(mediaController: MediaController?, tracks: List<Track>, startIndex: Int = 0) {
        if (mediaController == null || tracks.isEmpty()) return
        
        val mediaItems = tracks.map { trackToMediaItem(it) }
        mediaController.setMediaItems(mediaItems, startIndex, 0L)
        mediaController.prepare()
        mediaController.play()
    }
    
    fun pause(mediaController: MediaController?) {
        mediaController?.pause()
    }
    
    fun resume(mediaController: MediaController?) {
        mediaController?.play()
    }
    
    fun skipToNext(mediaController: MediaController?) {
        mediaController?.seekToNextMediaItem()
    }
    
    fun skipToPrevious(mediaController: MediaController?) {
        mediaController?.seekToPreviousMediaItem()
    }
    
    fun seekTo(mediaController: MediaController?, positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }
}
