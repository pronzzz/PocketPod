package io.github.pronzzz.wheelhouse

import io.github.pronzzz.wheelhouse.data.LibraryIndex
import io.github.pronzzz.wheelhouse.data.db.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryIndexTest {

    private fun t(id: Long, title: String, artist: String, album: String, albumId: Long, no: Int = 0, disc: Int = 0, albumArtist: String? = null, genre: String? = null) =
        Track(id, title, artist, album, albumArtist, albumId, genre, 2000, 180_000, no, disc, null, id, id, 1000)

    private val tracks = listOf(
        t(1, "Come Together", "The Beatles", "Abbey Road", 10, no = 1, genre = "Rock"),
        t(2, "Something", "The Beatles", "Abbey Road", 10, no = 2, genre = "Rock"),
        t(3, "Alright", "Kendrick Lamar", "To Pimp a Butterfly", 20, no = 7),
        t(4, "Track A", "Artist One", "Mixtape", 30, no = 2),
        t(5, "Track B", "Artist Two", "Mixtape", 30, no = 1),
        t(6, "Disc Two Opener", "Artist One", "Mixtape", 30, no = 1, disc = 2),
    )

    @Test fun songsAreSortedIgnoringArticles() {
        val idx = LibraryIndex.build(tracks)
        assertEquals(listOf("Alright", "Come Together", "Disc Two Opener", "Something", "Track A", "Track B"), idx.songs.map { it.title })
    }

    @Test fun albumsGroupByAlbumIdAndOrderByDiscThenTrack() {
        val idx = LibraryIndex.build(tracks)
        val mixtape = idx.album(30)!!
        assertEquals(listOf("Track B", "Track A", "Disc Two Opener"), mixtape.tracks.map { it.title })
        assertEquals("Various Artists", mixtape.artist)
        assertEquals("The Beatles", idx.album(10)!!.artist)
    }

    @Test fun artistsSortIgnoringTheAndListTheirAlbums() {
        val idx = LibraryIndex.build(tracks)
        assertEquals(listOf("Artist One", "Artist Two", "The Beatles", "Kendrick Lamar"), idx.artists.map { it.name })
        assertEquals(listOf("Abbey Road"), idx.artist("The Beatles")!!.albums.map { it.title })
    }

    @Test fun genresSkipUntaggedSongs() {
        val idx = LibraryIndex.build(tracks)
        assertEquals(listOf("Rock"), idx.genres.map { it.name })
        assertEquals(2, idx.genres[0].tracks.size)
    }
}
