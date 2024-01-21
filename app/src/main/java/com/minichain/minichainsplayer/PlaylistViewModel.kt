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

  private val _songPlaying = MutableStateFlow<SongData?>(null)
  val songPlaying = _songPlaying.asStateFlow()

  init {
    viewModelScope.launch {
      listenToPlaylistUpdates()
      listenToCurrentSongUpdates()
    }
  }

  private fun CoroutineScope.listenToPlaylistUpdates() {
    App.dataCommunicationBridge.playlist.onEach {
      _playlist.emit(it)
    }.launchIn(this)
  }

  private fun CoroutineScope.listenToCurrentSongUpdates() {
    App.dataCommunicationBridge.currentSong.onEach {
      _songPlaying.emit(it)
    }.launchIn(this)
  }
}