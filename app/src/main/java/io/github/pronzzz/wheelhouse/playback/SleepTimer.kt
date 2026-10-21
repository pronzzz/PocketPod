package io.github.pronzzz.wheelhouse.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Pauses playback after a delay, or at the end of the current song. */
class SleepTimer(private val pause: () -> Unit) {

    private val handler = Handler(Looper.getMainLooper())
    private var endsAt = 0L
    var endOfSong = false; private set

    val active get() = endOfSong || endsAt > SystemClock.elapsedRealtime()
    val remainingMs get() = (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)

    private val fire = Runnable {
        endsAt = 0
        pause()
    }

    fun start(minutes: Int) {
        cancel()
        endsAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        handler.postDelayed(fire, minutes * 60_000L)
    }

    fun startEndOfSong() {
        cancel()
        endOfSong = true
    }

    /** Called by the player when a song finishes naturally. Returns true if playback should stop. */
    fun onSongEnded(): Boolean {
        if (!endOfSong) return false
        endOfSong = false
        return true
    }

    fun cancel() {
        handler.removeCallbacks(fire)
        endsAt = 0
        endOfSong = false
    }

    fun label(): String = when {
        endOfSong -> "End of Song"
        endsAt > SystemClock.elapsedRealtime() -> {
            val mins = (remainingMs + 59_999) / 60_000
            "$mins min left"
        }
        else -> "Off"
    }
}
