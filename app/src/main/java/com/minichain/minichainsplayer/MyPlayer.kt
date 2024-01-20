package com.minichain.minichainsplayer

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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

  @UnstableApi
  private fun CoroutineScope.updateDataFrequently() {

    player.currentMediaItem?.let { updateCurrentMediaItem(it) }

    player.addAnalyticsListener(object : AnalyticsListener {

      override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {
        println("AdriLog: onMediaItemTransition, mediaItem: $mediaItem")
        mediaItem?.let { updateCurrentMediaItem(it) }
      }

      override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
        super.onIsPlayingChanged(eventTime, isPlaying)
        App.dataCommunicationBridge.playerState.tryEmit(if (isPlaying) PlayerState.Playing else PlayerState.Stopped)
      }
    })

    launch {
      while (true) {
        delay(1000)
        App.dataCommunicationBridge.currentSongPosition.emit(player.currentPosition)
      }
    }
  }

  private fun updateCurrentMediaItem(mediaItem: MediaItem) {
    App.dataCommunicationBridge.playlist.value.find { it.mediaItem == mediaItem }.let {
      App.dataCommunicationBridge.currentSong.tryEmit(it)
    }
  }
}