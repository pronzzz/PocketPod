package com.pocketpod.app.library.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpod.app.core.database.LibraryDao
import com.pocketpod.app.core.database.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class LibraryViewModel(private val dao: LibraryDao) : ViewModel() {

    val allTracks: StateFlow<List<Track>> = dao.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allAlbums: StateFlow<List<String>> = dao.getAllAlbumNames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allArtists: StateFlow<List<String>> = dao.getAllArtistNames()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val recentlyPlayed: StateFlow<List<Track>> = dao.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<Track>> = dao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    
    // ExperimentalOptIn for flatMapLatest if using older coroutines, but usually fine in modern apps
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val searchResults: StateFlow<List<Track>> = _searchQuery
        .flatMapLatest { query -> 
            if (query.isBlank()) dao.getAllTracks() else dao.searchTracks(query) 
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun search(query: String) {
        _searchQuery.value = query
    }
    
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            dao.insertTrack(track.copy(favorite = !track.favorite))
        }
    }
}
