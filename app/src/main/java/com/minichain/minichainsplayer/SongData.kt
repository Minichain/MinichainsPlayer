package com.minichain.minichainsplayer

import android.net.Uri
import androidx.media3.common.MediaItem

data class SongData(
  val uri: Uri,
  val fileName: String,
  val mediaItem: MediaItem
)