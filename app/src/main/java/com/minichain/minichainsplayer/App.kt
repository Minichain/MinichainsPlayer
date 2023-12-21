package com.minichain.minichainsplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class App : Application() {

  companion object {
    const val notificationChannelID = "minichains_player_notification_channel"
    val dataCommunicationBridge = DataCommunicationBridge()
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    val notificationChannel = NotificationChannel(
      notificationChannelID,
      "Notification channel",
      NotificationManager.IMPORTANCE_HIGH
    )
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(notificationChannel)
  }
}