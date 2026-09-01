package com.pocketpod.app.library.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ScannerViewModel(private val scanner: LocalMusicScanner) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanProgress?>(null)
    val scanState: StateFlow<ScanProgress?> = _scanState.asStateFlow()

    fun startMediaStoreScan() {
        viewModelScope.launch {
            scanner.scanLibrary()
                .catch { e ->
                    _scanState.value = ScanProgress("Error: ${e.message}", 0, 0, 0)
                }
                .collect { progress ->
                    _scanState.value = progress
                }
        }
    }
}
