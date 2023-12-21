package com.minichain.minichainsplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class PlayerActivity : ComponentActivity() {

  private val viewModel: PlayerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
    }
    startForegroundService(Intent(applicationContext, PlayerService::class.java))
    setContent {
      MinichainsPlayerActivityContent()
    }
  }

  @Composable
  @Preview
  private fun MinichainsPlayerActivityContent() {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      PlaylistData()
      CurrentSongData()
      MediaButtons()
    }
  }

  @Composable
  private fun PlaylistData() {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      val playlistCounter by viewModel.playlistCounter.collectAsStateWithLifecycle()
      Text(text = playlistCounter)
      FloatingActionButton(
        onClick = { },
        modifier = Modifier.size(32.dp, 32.dp)
      ) {
        Icon(Icons.Filled.List, "Floating action button")
      }
    }
  }

  @Composable
  private fun CurrentSongData() {
    Column {
      CurrentSongName()
      CurrentSongTime()
    }
  }

  @Composable
  private fun CurrentSongName() {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      val currentSongName by viewModel.currentSongName.collectAsStateWithLifecycle()
      Text(text = currentSongName, style = MaterialTheme.typography.h6, modifier = Modifier.fillMaxWidth())
    }
  }

  @Composable
  private fun CurrentSongTime() {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      val songCurrentTimestamp by viewModel.songCurrentTimestamp.collectAsStateWithLifecycle()
      val songProgress by viewModel.songProgress.collectAsStateWithLifecycle()
      val songLength by viewModel.songLength.collectAsStateWithLifecycle()
      Text(text = songCurrentTimestamp.millisecondsToHoursMinutesAndSeconds())
      Row(Modifier.weight(1f)) {
        Slider(
          value = songProgress,
          onValueChange = {
            viewModel.jumpToCurrentSongProgress(it)
          }
        )
      }
      Text(text = songLength.millisecondsToHoursMinutesAndSeconds())
    }
  }

  @Composable
  private fun MediaButtons() {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      PreviousSongFloatingButton()
      StartStopSongFloatingButton()
      NextSongFloatingButton()
    }
  }

  @Composable
  private fun PreviousSongFloatingButton() {
    FloatingActionButton(
      onClick = { viewModel.previousSong() },
      modifier = Modifier.size(48.dp, 48.dp)
    ) {
      Icon(
        Icons.Filled.KeyboardArrowLeft,
        "Previous song"
      )
    }
  }

  @Composable
  private fun StartStopSongFloatingButton() {
    FloatingActionButton(
      onClick = { viewModel.startStopSong() },
      modifier = Modifier.size(80.dp, 80.dp)
    ) {
      val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
      Icon(
        when (mediaState) {
          PlayerState.Playing -> Icons.Filled.Warning
          PlayerState.Stopped -> Icons.Filled.PlayArrow
        },
        "Play/stop current song"
      )
    }
  }

  @Composable
  private fun NextSongFloatingButton() {
    FloatingActionButton(
      onClick = { viewModel.nextSong() },
      modifier = Modifier.size(48.dp, 48.dp)
    ) {
      Icon(
        Icons.Filled.KeyboardArrowRight,
        "Next song"
      )
    }
  }
}