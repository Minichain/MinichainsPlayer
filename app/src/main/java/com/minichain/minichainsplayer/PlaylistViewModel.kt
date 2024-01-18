package com.minichain.minichainsplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlaylistViewModel : ViewModel() {

  private val _playlist = MutableStateFlow<List<SongData>>(listOf())
  val playlist = _playlist.asStateFlow()

  init {
    viewModelScope.launch {
      listenToPlaylistUpdates()
    }
  }

  private fun CoroutineScope.listenToPlaylistUpdates() {
    App.dataCommunicationBridge.playlist.onEach {
      _playlist.emit(it)
    }.launchIn(this)
  }
}