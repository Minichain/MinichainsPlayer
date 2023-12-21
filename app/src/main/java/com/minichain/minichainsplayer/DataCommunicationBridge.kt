package com.minichain.minichainsplayer

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class DataCommunicationBridge {
  val events = MutableSharedFlow<Event>()
  val currentSong = MutableStateFlow<SongData?>(null)
  val playlist = MutableStateFlow<List<SongData>>(emptyList())
  val playerState = MutableStateFlow(PlayerState.Stopped)
}