package com.minichain.minichainsplayer

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyPlayer(
  private val context: Context,
  private val scope: CoroutineScope
) {

  private lateinit var player: ExoPlayer

  init {
    scope.launch {
      player = ExoPlayer.Builder(context).build()
      AudioFilesReader.getAudioFilesInFolder(context, "%/Music/%").let { songList ->
        updatePlaylist(songList)
      }
      player.prepare()
      updateDataFrequently()
    }
  }

  fun playStop() {
    if (player.isPlaying) {
      player.pause()
    } else {
      player.play()
      println("AdriLog: player playing!")
    }
  }

  fun previousSong() {
    player.seekToPreviousMediaItem()
  }

  fun nextSong() {
    player.seekToNextMediaItem()
  }

  private suspend fun updatePlaylist(songList: List<SongData>) {
    App.dataCommunicationBridge.playlist.emit(songList)
    songList.forEach { song ->
      println("AdriLog: mediaItem metadata: ${song.mediaItem.mediaMetadata}")
      player.addMediaItem(song.mediaItem)
    }
    println("AdriLog: items: ${player.mediaItemCount}")
  }

  private fun CoroutineScope.updateDataFrequently() {
    launch {
      while (true) {
        delay(500)
        App.dataCommunicationBridge.playlist.value.find { it.mediaItem == player.currentMediaItem }.let {
          App.dataCommunicationBridge.currentSong.emit(it)
        }
        App.dataCommunicationBridge.playerState.emit(if (player.isPlaying) PlayerState.Playing else PlayerState.Stopped)
      }
    }
  }
}