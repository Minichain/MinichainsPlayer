package com.minichain.minichainsplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

  private val _mediaState = MutableStateFlow(PlayerState.Stopped)
  val mediaState = _mediaState.asStateFlow()

  private val _songProgress = MutableStateFlow(0f)
  val songProgress = _songProgress.asStateFlow()

  private val _songCurrentTimestamp = MutableStateFlow(0L)
  val songCurrentTimestamp = _songCurrentTimestamp.asStateFlow()

  private val _songLength = MutableStateFlow(0L)
  val songLength = _songLength.asStateFlow()

  private val _currentSongName = MutableStateFlow("")
  val currentSongName = _currentSongName.asStateFlow()

  private val _playlistCounter = MutableStateFlow("")
  val playlistCounter = _playlistCounter.asStateFlow()

  init {
    viewModelScope.launch {
      listenToPlayerStateUpdates()
      listenToCurrentSongUpdates()
      listenToPlaylistCounterUpdates()

      combine(
        App.dataCommunicationBridge.currentSong,
        App.dataCommunicationBridge.currentSongPosition
      ) { currentSong, currentSongPosition ->
        currentSong?.let {
          currentSong.length?.let { currentSongLength ->
            _songCurrentTimestamp.emit(currentSongPosition)
            _songLength.emit(currentSongLength)
            _songProgress.emit(currentSongPosition.toFloat() / currentSongLength.toFloat())
          }
        }
      }.launchIn(this)
    }
  }

  private fun CoroutineScope.listenToPlayerStateUpdates() {
    App.dataCommunicationBridge.playerState.onEach { playerState ->
      _mediaState.emit(playerState)
    }.launchIn(this)
  }

  private fun CoroutineScope.listenToCurrentSongUpdates() {
    App.dataCommunicationBridge.currentSong.onEach { songData ->
      songData?.let {
        _currentSongName.emit(songData.fileName)
      } ?: run {
        _currentSongName.emit("No song")
      }
    }.launchIn(this)
  }

  private fun CoroutineScope.listenToPlaylistCounterUpdates() {
    combine(
      App.dataCommunicationBridge.playlist,
      App.dataCommunicationBridge.currentSong
    ) { playlist, currentSong ->
      currentSong?.let {
        _playlistCounter.emit("${playlist.indexOf(currentSong) + 1}/${playlist.size}")
      }
    }.launchIn(this)
  }

  fun startStopSong() {
    viewModelScope.launch {
      App.dataCommunicationBridge.events.emit(PlayStopSong)
    }
  }

  fun nextSong() {
    viewModelScope.launch {
      App.dataCommunicationBridge.events.emit(NextSong)
    }
  }

  fun previousSong() {
    viewModelScope.launch {
      App.dataCommunicationBridge.events.emit(PreviousSong)
    }
  }

  fun jumpToCurrentSongProgress(progress: Float) {
    //TODO
//    viewModelScope.launch {
//      _songProgress.emit(progress)
//    }
  }
}