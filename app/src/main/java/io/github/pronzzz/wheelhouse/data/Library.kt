package io.github.pronzzz.wheelhouse.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import io.github.pronzzz.wheelhouse.data.db.Playlist
import io.github.pronzzz.wheelhouse.data.db.Track
import io.github.pronzzz.wheelhouse.data.db.TrackStats
import io.github.pronzzz.wheelhouse.data.db.WheelhouseDao
import io.github.pronzzz.wheelhouse.util.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** An immutable snapshot of the whole music library, sorted and grouped for browsing. */
class LibraryIndex(
    val songs: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val genres: List<Genre>,
    val byId: Map<Long, Track>,
) {
    fun album(albumId: Long): Album? = albums.firstOrNull { it.albumId == albumId }
    fun artist(name: String): Artist? = artists.firstOrNull { it.name == name }

    companion object {
        val EMPTY = LibraryIndex(emptyList(), emptyList(), emptyList(), emptyList(), emptyMap())

        fun build(tracks: List<Track>): LibraryIndex {
            val songs = tracks.sortedBy { Text.sortKey(it.title) }
            val albumOrder = compareBy<Track>({ it.discNumber }, { it.trackNumber }, { Text.sortKey(it.title) })
            val albums = tracks.groupBy { it.albumId }.map { (albumId, list) ->
                val sorted = list.sortedWith(albumOrder)
                val artists = sorted.map { it.artist }.distinct()
                val artist = sorted.firstNotNullOfOrNull { it.albumArtist }
                    ?: if (artists.size == 1) artists[0] else "Various Artists"
                Album(albumId, sorted[0].album, artist, sorted.maxOf { it.year }, sorted, Text.sortKey(sorted[0].album))
            }.sortedBy { it.sortKey }
            val albumsById = albums.associateBy { it.albumId }
            val artists = tracks.groupBy { it.artist }.map { (name, list) ->
                val artistAlbums = list.map { it.albumId }.distinct().mapNotNull { albumsById[it] }
                    .sortedWith(compareBy({ it.year }, { it.sortKey }))
                Artist(name, artistAlbums, list.sortedBy { Text.sortKey(it.title) }, Text.sortKey(name))
            }.sortedBy { it.sortKey }
            val genres = tracks.filter { it.genre != null }.groupBy { it.genre!! }
                .map { (name, list) -> Genre(name, list.sortedBy { Text.sortKey(it.title) }) }
                .sortedBy { Text.sortKey(it.name) }
            return LibraryIndex(songs, albums, artists, genres, tracks.associateBy { it.id })
        }
    }
}

class Library(
    private val context: Context,
    private val dao: WheelhouseDao,
    private val scope: CoroutineScope,
) {
    private val scanner = MediaScanner(context)
    private val scanMutex = Mutex()

    private val _index = MutableStateFlow(LibraryIndex.EMPTY)
    val index: StateFlow<LibraryIndex> = _index

    /** Bumped whenever songs, stats or playlists change, so screens can refresh. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning

    private val stats = HashMap<Long, TrackStats>()
    var playlists: List<Playlist> = emptyList(); private set
    private var playlistTracks: Map<Long, List<Long>> = emptyMap()

    private var observerJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    private var observing = false

    /** Loads the cached library from the database, then refreshes it from MediaStore. */
    fun start(hasPermission: Boolean) {
        scope.launch {
            if (!_loaded.value) {
                val cached = withContext(Dispatchers.IO) {
                    val tracks = dao.allTracks()
                    stats.putAll(dao.allStats().associateBy { it.trackId })
                    loadPlaylistsLocked()
                    LibraryIndex.build(tracks)
                }
                _index.value = cached
                _loaded.value = true
                bump()
            }
            if (hasPermission) {
                rescan(deep = false)
                observeMediaStore()
            }
        }
    }

    suspend fun awaitLoaded() { loaded.first { it } }

    fun rescan(deep: Boolean) {
        scope.launch {
            scanMutex.withLock {
                _scanning.value = true
                try {
                    val fresh = withContext(Dispatchers.IO) {
                        if (deep) withTimeoutOrNull(90_000) { runCatching { scanner.indexMusicFolders() } }
                        val tracks = try { scanner.query() } catch (e: SecurityException) { return@withContext null }
                        val old = _index.value.byId
                        val changed = tracks.filter { t -> old[t.id] != t }
                        val removed = old.keys - tracks.map { it.id }.toSet()
                        if (changed.isNotEmpty()) dao.upsertTracks(changed)
                        if (removed.isNotEmpty()) dao.deleteTracks(removed.toList())
                        if (changed.isEmpty() && removed.isEmpty()) null else LibraryIndex.build(tracks)
                    }
                    if (fresh != null) {
                        _index.value = fresh
                        bump()
                    }
                } finally {
                    _scanning.value = false
                }
            }
        }
    }

    private fun observeMediaStore() {
        if (observing) return
        observing = true
        val rescanSoon = Runnable { rescan(deep = false) }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true,
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    handler.removeCallbacks(rescanSoon)
                    handler.postDelayed(rescanSoon, 4_000)
                }
            },
        )
    }

    private fun bump() { _version.value = _version.value + 1 }

    // ---- Stats -------------------------------------------------------------------------------

    fun stats(id: Long): TrackStats = stats[id] ?: TrackStats(id)
    fun isLoved(id: Long) = stats[id]?.loved == true

    fun recordPlay(id: Long) {
        val s = stats(id).let { it.copy(playCount = it.playCount + 1, lastPlayed = System.currentTimeMillis()) }
        stats[id] = s
        scope.launch(Dispatchers.IO) { dao.upsertStats(s) }
        bump()
    }

    fun toggleLoved(id: Long) {
        val s = stats(id).let { it.copy(loved = !it.loved) }
        stats[id] = s
        scope.launch(Dispatchers.IO) { dao.upsertStats(s) }
        bump()
    }

    fun loved(): List<Track> = _index.value.songs.filter { stats[it.id]?.loved == true }

    fun mostPlayed(limit: Int = 50): List<Track> {
        val byId = _index.value.byId
        return stats.values.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
            .mapNotNull { byId[it.trackId] }.take(limit)
    }

    fun recentlyPlayed(limit: Int = 50): List<Track> {
        val byId = _index.value.byId
        return stats.values.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }
            .mapNotNull { byId[it.trackId] }.take(limit)
    }

    fun recentlyAdded(limit: Int = 100): List<Track> =
        _index.value.songs.sortedByDescending { it.dateAdded }.take(limit)

    // ---- Playlists ---------------------------------------------------------------------------

    private suspend fun loadPlaylistsLocked() {
        playlists = dao.playlists()
        playlistTracks = dao.allPlaylistItems().groupBy({ it.playlistId }, { it.trackId })
    }

    fun playlistTracks(id: Long): List<Track> {
        val byId = _index.value.byId
        return playlistTracks[id].orEmpty().mapNotNull { byId[it] }
    }

    fun createPlaylist(name: String, firstTrack: Long? = null, done: (Long) -> Unit = {}) {
        scope.launch {
            val id = withContext(Dispatchers.IO) {
                val id = dao.insertPlaylist(Playlist(name = name, createdAt = System.currentTimeMillis()))
                if (firstTrack != null) dao.setPlaylistTracks(id, listOf(firstTrack))
                loadPlaylistsLocked()
                id
            }
            bump()
            done(id)
        }
    }

    fun addToPlaylist(id: Long, trackIds: List<Long>) = editPlaylist(id) { it + trackIds }

    fun removeFromPlaylist(id: Long, position: Int) = editPlaylist(id) { list ->
        list.filterIndexed { i, _ -> i != position }
    }

    private fun editPlaylist(id: Long, edit: (List<Long>) -> List<Long>) {
        val updated = edit(playlistTracks[id].orEmpty())
        playlistTracks = playlistTracks + (id to updated)
        bump()
        scope.launch(Dispatchers.IO) { dao.setPlaylistTracks(id, updated) }
    }

    fun renamePlaylist(id: Long, name: String) {
        playlists = playlists.map { if (it.id == id) it.copy(name = name) else it }.sortedBy { it.name.lowercase() }
        bump()
        scope.launch(Dispatchers.IO) { dao.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        playlists = playlists.filterNot { it.id == id }
        playlistTracks = playlistTracks - id
        bump()
        scope.launch(Dispatchers.IO) { dao.deletePlaylist(id) }
    }

    fun nextPlaylistName(): String {
        val names = playlists.map { it.name }.toSet()
        var n = 1
        while ("Playlist $n" in names) n++
        return "Playlist $n"
    }
}
