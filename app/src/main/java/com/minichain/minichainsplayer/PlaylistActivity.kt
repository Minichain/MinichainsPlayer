package com.minichain.minichainsplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class PlaylistActivity : ComponentActivity() {

  private val viewModel: PlaylistViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      PlaylistActivityContent()
    }
  }

  @Preview
  @Composable
  private fun PlaylistActivityContent() {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val currentSong by viewModel.songPlaying.collectAsStateWithLifecycle()
    LazyColumn(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
    ) {
      items(playlist.size) { index ->
        PlaylistItem(
          songData = playlist[index],
          playingThisSong = playlist[index].uri == currentSong?.uri
        )
      }
    }
  }

  @Composable
  private fun PlaylistItem(
    songData: SongData,
    playingThisSong: Boolean
  ) {
    val backgroundColor = if (playingThisSong) Color.Green else Color.White
    Column(
      modifier = Modifier.padding(18.dp).background(backgroundColor)
    ) {
      Text(
        fontSize = 28.sp,
        text = songData.fileName
      )
      Text(
        fontSize = 18.sp,
        text = songData.length?.millisecondsToHoursMinutesAndSeconds() ?: "00:00"
      )
    }
  }
}