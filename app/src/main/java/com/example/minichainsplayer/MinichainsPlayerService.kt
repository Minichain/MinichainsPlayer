package com.example.minichainsplayer

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


class MinichainsPlayerService : Service() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerServiceBroadcastReceiver
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val serviceNotificationStringId = "MINICHAINS_PLAYER_SERVICE_NOTIFICATION"
    private val serviceNotificationId = 1

    override fun onCreate() {
        super.onCreate()
        Log.l("MinichainsPlayerServiceLog:: onCreate service")
        init()
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.l("MinichainsPlayerServiceLog:: onBind service")
        return null
    }

    override fun onDestroy() {
        unregisterReceiver(minichainsPlayerBroadcastReceiver)
        removeMinichainsPlayerServiceNotification()
        Log.l("MinichainsPlayerServiceLog:: onDestroy service")
    }

    private fun init() {
        minichainsPlayerBroadcastReceiver = MinichainsPlayerServiceBroadcastReceiver()
        registerMinichainsPlayerServiceBroadcastReceiver()
        createMinichainsPlayerServiceNotification()
    }

    inner class MinichainsPlayerServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.l("MinichainsPlayerServiceLog:: Broadcast received " + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.START_STOP_PLAYING.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_STOP_PLAYING")
                    } else if (broadcast == BroadcastMessage.PREVIOUS_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: PREVIOUS_SONG")
                    } else if (broadcast == BroadcastMessage.NEXT_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: NEXT_SONG")
                    } else {
                        Log.l("MinichainsPlayerServiceLog:: Unknown broadcast received")
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun registerMinichainsPlayerServiceBroadcastReceiver() {
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }
            registerReceiver(minichainsPlayerBroadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun createMinichainsPlayerServiceNotification() {
        //Service notification
        val notificationName: CharSequence = resources.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(serviceNotificationStringId, notificationName, importance).apply {
                description = "descriptionText"
            }
            channel.setShowBadge(false)
            //Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            this.notificationManager = notificationManager
        }

        /** Open Main Activity **/
//        //Notification intent to open the activity when pressing the notification
        val intent = Intent(this, MinichainsPlayerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        /** PREVIOUS **/
        val previousIntent = Intent()
        previousIntent.action = BroadcastMessage.PREVIOUS_SONG.toString()
        val previousPendingIntent = PendingIntent.getBroadcast(this, 0, previousIntent, 0)

        /** PLAY/PAUSE **/
        val playStopIntent = Intent()
        playStopIntent.action = BroadcastMessage.START_STOP_PLAYING.toString()
        val playStopPendingIntent = PendingIntent.getBroadcast(this, 0, playStopIntent, 0)

        /** PREVIOUS **/
        val nextIntent = Intent()
        nextIntent.action = BroadcastMessage.NEXT_SONG.toString()
        val nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0)

        val notification = NotificationCompat.Builder(this, serviceNotificationStringId)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(notificationName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .addAction(R.drawable.baseline_skip_previous_white_18, "Previous Song", previousPendingIntent)
            .addAction(R.drawable.baseline_play_arrow_white_18, "Play", playStopPendingIntent)
            .addAction(R.drawable.baseline_skip_next_white_18, "Next Song", nextPendingIntent)
            .build()

        notificationManagerCompat?.notify(serviceNotificationId, notification)
        this.startForeground(1, notification)
    }

    private fun removeMinichainsPlayerServiceNotification() {
        if (notificationManagerCompat != null) {
            notificationManagerCompat!!.cancel(serviceNotificationId)
        }
    }
}