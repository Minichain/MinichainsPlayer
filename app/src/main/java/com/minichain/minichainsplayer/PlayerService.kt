package com.minichain.minichainsplayer

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.minichain.minichainsplayer.App.Companion.notificationChannelID
import java.lang.Exception
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PlayerService : Service() {

  private val scope = CoroutineScope(Dispatchers.Main + Job())
  private lateinit var player: MyPlayer

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startService()
    return super.onStartCommand(intent, flags, startId)
  }

  private fun startService() {
    try {
      val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
      } else {
        0
      }
      ServiceCompat.startForeground(this, 100, buildNotification(), serviceType)
    } catch (e: Exception) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
        // App not in a valid state to start foreground service
        // (e.g. started from bg)
      }
    }

    scope.launch {
      player = MyPlayer(this@PlayerService, this)
      App.dataCommunicationBridge.events.onEach { event ->
        when (event) {
          is PlayStopSong -> player.playStop()
          is PreviousSong -> player.previousSong()
          is NextSong -> player.nextSong()
          else -> {}
        }
      }.launchIn(this)
    }
  }

  private fun buildNotification() = NotificationCompat.Builder(this, notificationChannelID)
    .setContentTitle("Player ongoing")
    .build()

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}
