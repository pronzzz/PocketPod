package com.example.puffwheel.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LibraryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var libraryDao: LibraryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        libraryDao = db.libraryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeTrackAndReadInList() = runBlocking {
        val track = Track(
            id = "1",
            contentUri = "content://media/external/audio/media/1",
            sourceType = 0,
            displayName = "song.mp3",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumArtist = "Test Album Artist",
            genre = null,
            duration = 3000L,
            trackNumber = 1,
            discNumber = 1,
            dateAdded = 1000L,
            dateModified = 1000L,
            size = 5000L,
            artworkKey = null,
            isAvailable = true
        )
        libraryDao.insertTracks(listOf(track))
        
        val tracks = libraryDao.getAllTracks().first()
        assertEquals(1, tracks.size)
        assertEquals("Test Song", tracks[0].title)
    }
}
