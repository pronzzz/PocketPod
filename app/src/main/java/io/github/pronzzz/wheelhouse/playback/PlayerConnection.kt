package io.github.pronzzz.wheelhouse.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.pronzzz.wheelhouse.data.Library
import io.github.pronzzz.wheelhouse.data.Prefs
import io.github.pronzzz.wheelhouse.data.albumArtUri
import io.github.pronzzz.wheelhouse.data.contentUri
import io.github.pronzzz.wheelhouse.data.db.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val trackId: Long = -1,
    val index: Int = 0,
    val count: Int = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
) {
    val hasQueue get() = count > 0
}

/** The single app-wide handle on the playback service. */
class PlayerConnection(
    private val context: Context,
    private val library: Library,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {
    private var controller: MediaController? = null
    private var connecting = false
    private var lastCountedId = -1L

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    val sleepTimer = SleepTimer { controller?.pause() }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
            if (player.isPlaying) {
                val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: -1
                if (id != -1L && id != lastCountedId) {
                    lastCountedId = id
                    library.recordPlay(id)
                }
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && !player.isPlaying) saveQueue()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                lastCountedId = -1 // a repeat or the next song counts as a new play
                if (sleepTimer.onSongEnded()) {
                    controller?.pause()
                    controller?.seekTo(0)
                }
            }
            saveQueue()
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            connecting = false
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            c.shuffleModeEnabled = prefs.shuffle
            c.repeatMode = repeatToPlayer(prefs.repeatMode)
            publish()
            scope.launch {
                library.awaitLoaded()
                restoreQueue()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun publish() {
        val c = controller ?: return
        _state.value = PlayerState(
            connected = true,
            isPlaying = c.isPlaying,
            playWhenReady = c.playWhenReady,
            trackId = c.currentMediaItem?.mediaId?.toLongOrNull() ?: -1,
            index = c.currentMediaItemIndex,
            count = c.mediaItemCount,
            durationMs = c.duration.takeIf { it > 0 } ?: currentTrack()?.durationMs ?: 0,
            shuffle = c.shuffleModeEnabled,
            repeatMode = prefs.repeatMode,
        )
    }

    // ---- Queue persistence -------------------------------------------------------------------

    fun saveQueue() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        val ids = (0 until c.mediaItemCount).joinToString(",") { c.getMediaItemAt(it).mediaId }
        prefs.queueIds = ids
        prefs.queueIndex = c.currentMediaItemIndex
        prefs.queuePosition = c.currentPosition
    }

    private fun restoreQueue() {
        val c = controller ?: return
        if (c.mediaItemCount > 0) return
        val byId = library.index.value.byId
        val saved = prefs.queueIds.split(',').mapNotNull { it.toLongOrNull() }
        val tracks = saved.mapNotNull { byId[it] }
        if (tracks.isEmpty()) return
        val savedCurrent = saved.getOrNull(prefs.queueIndex)
        val index = tracks.indexOfFirst { it.id == savedCurrent }.coerceAtLeast(0)
        val pos = if (index >= 0 && tracks[index].id == savedCurrent) prefs.queuePosition else 0
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, pos)
        c.prepare()
        publish()
    }

    // ---- Controls ----------------------------------------------------------------------------

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean? = null) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        shuffle?.let { setShuffle(it) }
        val start = if (shuffle == true) tracks.indices.random() else startIndex.coerceIn(0, tracks.lastIndex)
        c.setMediaItems(tracks.map { it.toMediaItem() }, start, 0)
        c.prepare()
        c.play()
    }

    fun shuffleAll() {
        val songs = library.index.value.songs
        if (songs.isNotEmpty()) playTracks(songs, shuffle = true)
    }

    fun playNext(track: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return playTracks(listOf(track))
        val at = nextPlayIndex(c)
        c.addMediaItem(at, track.toMediaItem())
    }

    fun addToQueue(track: Track) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return playTracks(listOf(track))
        c.addMediaItem(track.toMediaItem())
    }

    /** Index where an item will play right after the current one, honouring shuffle order. */
    private fun nextPlayIndex(c: MediaController): Int = c.currentMediaItemIndex + 1

    fun togglePlay() {
        val c = controller ?: return
        when {
            c.mediaItemCount == 0 -> shuffleAll()
            c.isPlaying -> c.pause()
            else -> {
                if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
                c.play()
            }
        }
    }

    fun pause() = controller?.pause()
    fun next() { controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() } }
    fun previous() = controller?.seekToPrevious()

    fun position(): Long = controller?.currentPosition ?: 0
    fun duration(): Long = _state.value.durationMs

    fun seekTo(ms: Long) = controller?.seekTo(ms.coerceIn(0, duration().coerceAtLeast(0)))
    fun seekBy(ms: Long) = seekTo(position() + ms)

    fun setShuffle(on: Boolean) {
        prefs.shuffle = on
        controller?.shuffleModeEnabled = on
        publish()
    }

    fun setRepeat(mode: Int) {
        prefs.repeatMode = mode
        controller?.repeatMode = repeatToPlayer(mode)
        publish()
    }

    private fun repeatToPlayer(mode: Int) = when (mode) {
        1 -> Player.REPEAT_MODE_ONE
        2 -> Player.REPEAT_MODE_ALL
        else -> Player.REPEAT_MODE_OFF
    }

    fun currentTrack(): Track? = library.index.value.byId[_state.value.trackId]
        ?: controller?.currentMediaItem?.mediaId?.toLongOrNull()?.let { library.index.value.byId[it] }

    /** Songs that will play after the current one, in actual play order (shuffle aware). */
    fun upNext(limit: Int = 200): List<Pair<Int, Track>> {
        val c = controller ?: return emptyList()
        val tl = c.currentTimeline
        if (tl.isEmpty) return emptyList()
        val byId = library.index.value.byId
        val out = ArrayList<Pair<Int, Track>>()
        var i = tl.getNextWindowIndex(c.currentMediaItemIndex, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
        while (i != C_INDEX_UNSET && out.size < limit) {
            val id = c.getMediaItemAt(i).mediaId.toLongOrNull()
            byId[id]?.let { out += i to it }
            i = tl.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
        }
        return out
    }

    fun jumpTo(queueIndex: Int) {
        controller?.run {
            seekToDefaultPosition(queueIndex)
            play()
        }
    }

    private companion object {
        val C_INDEX_UNSET = androidx.media3.common.C.INDEX_UNSET
    }
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(contentUri())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setAlbumArtist(albumArtist)
            .setTrackNumber(trackNumber.takeIf { it > 0 })
            .setArtworkUri(albumArtUri(albumId))
            .build(),
    )
    .build()
