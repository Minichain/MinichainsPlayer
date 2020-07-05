package com.example.minichainsplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class MinichainsPlayerService : Service() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerServiceBroadcastReceiver
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationName: CharSequence
    private lateinit var notificationTitle: CharSequence
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val serviceNotificationStringId = "MINICHAINS_PLAYER_SERVICE_NOTIFICATION"
    private val serviceNotificationId = 1

    private var musicLocation = ""
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongName = ""
    private var currentSongPath = ""
    private var currentSongTime = 0
    private var playing: Boolean = false
    private var currentSongInteger = 0
    private var shuffle = false

    private var listOfSongs: ArrayList<SongFile>? = null
    private var listOfSongsPlayed: ArrayList<Int>? = null

    private lateinit var mediaSession: MediaSessionCompat
    private var timesPressingMediaButton = 0

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
//        musicLocation = "/sdcard/Music/"
        musicLocation = String().plus("/storage/0C80-1910").plus("/Music/")
        Log.l("musicLocation: " + musicLocation)

        fillPlayList()
        updateCurrentSongInfo()

        minichainsPlayerBroadcastReceiver = MinichainsPlayerServiceBroadcastReceiver()
        registerMinichainsPlayerServiceBroadcastReceiver()
        createMinichainsPlayerServiceNotification()
        initUpdateActivityThread()

        initMediaSessions()
    }

    private fun initMediaSessions() {
        mediaSession = MediaSessionCompat(applicationContext, MinichainsPlayerService::class.java.simpleName)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.setMediaButtonReceiver(null)
        var mStateBuilder = PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY)
        mediaSession.setPlaybackState(mStateBuilder.build())
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            //callback code is here.
            override fun onPlay() {
                if (timesPressingMediaButton == 0) {
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            Thread(Runnable {
                                Log.l("timesPressingMediaButton: $timesPressingMediaButton")
                                if (timesPressingMediaButton == 1) {
                                    if (!playing) {
                                        play(currentSongPath, currentSongTime)
                                    } else {
                                        stopPlaying()
                                    }
                                } else if (timesPressingMediaButton == 2) {
                                    next()
                                } else if (timesPressingMediaButton >= 3) {
                                    previous()
                                }

                                timesPressingMediaButton = 0
                            }).start()
                        }
                    }, 750)
                }

                Log.l("timesPressingMediaButton++")
                timesPressingMediaButton++
            }
        })
        mediaSession.isActive = true
    }

    private fun initUpdateActivityThread() {
        val sleepTime = 100
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(sleepTime.toLong())
                        updateCurrentSongInfo()
                        updateActivityVariables()
                        updateNotificationTitle()
                        if (listOfSongs != null && currentSongInteger < listOfSongs?.size!!) {
                            var currentSongLength = listOfSongs?.get(currentSongInteger)?.length
                            if (currentSongLength != null) {
                                if (currentSongLength > 0 && currentSongTime >= (currentSongLength!! - sleepTime.toLong())) {
                                    //Song has ended. Playing next song...
                                    next()
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }
        thread.start()
    }

    private fun updateActivityVariables() {
        var bundle = Bundle()
        if (mediaPlayer != null) {
            currentSongTime = mediaPlayer?.currentPosition!!
        }
        bundle.putInt("currentSongTime", currentSongTime)
        bundle.putBoolean("playing", playing)
        bundle.putString("currentSongPath", currentSongPath)
        bundle.putInt("currentSongInteger", currentSongInteger)
        if (listOfSongs != null && currentSongInteger < listOfSongs?.size!!) {
            bundle.putString("currentSongName", listOfSongs?.get(currentSongInteger)?.songName)
            bundle.putLong("currentSongLength", listOfSongs?.get(currentSongInteger)?.length!!)
        }
        bundle.putBoolean("shuffle", shuffle)
        sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY, bundle)
    }

    private fun play(songPath: String, currentSongTime: Int) {
        if (!playing) {
            currentSongPath = songPath
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(songPath)
            mediaPlayer?.prepare()
            mediaPlayer?.seekTo(currentSongTime)
            mediaPlayer?.start()
            playing = true
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.pause()
        playing = false
    }

    private fun previous() {
        if (shuffle) {
            currentSongInteger = (Math.random() * (listOfSongs?.size!! - 1)).toInt()
        } else {
            currentSongInteger = (currentSongInteger + 1) % listOfSongs?.size!!
        }
        stopPlaying()
        if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
            this.currentSongTime = 0
            play(listOfSongs?.get(currentSongInteger)?.path
                    + listOfSongs?.get(currentSongInteger)?.songName
                    + "."
                    + listOfSongs?.get(currentSongInteger)?.format,
                currentSongTime)
        }
    }

    private fun next() {
        if (shuffle) {
            currentSongInteger = (Math.random() * (listOfSongs?.size!! - 1)).toInt()
        } else {
            currentSongInteger--
            if (currentSongInteger < 0) {
                currentSongInteger = listOfSongs?.size!! - 1
            }
        }
        stopPlaying()
        if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
            this.currentSongTime = 0
            play(listOfSongs?.get(currentSongInteger)?.path
                    + listOfSongs?.get(currentSongInteger)?.songName
                    + "."
                    + listOfSongs?.get(currentSongInteger)?.format,
                currentSongTime)
        }
    }

    private fun updateCurrentSongInfo() {
        if (listOfSongs != null && listOfSongs?.isNotEmpty()!!) {
            currentSongName = listOfSongs?.get(currentSongInteger)?.songName.toString()
            currentSongPath = String().plus(listOfSongs?.get(currentSongInteger)?.path.toString())
                .plus(listOfSongs?.get(currentSongInteger)?.songName)
                .plus(".")
                .plus(listOfSongs?.get(currentSongInteger)?.format)

            if (listOfSongs?.get(currentSongInteger)?.length!!.toInt() <= 0) {
                val metaRetriever = MediaMetadataRetriever()
                metaRetriever.setDataSource(listOfSongs?.get(currentSongInteger)?.path
                        + listOfSongs?.get(currentSongInteger)?.songName
                        + "."
                        + listOfSongs?.get(currentSongInteger)?.format)
                val durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                var duration: Long = -1
                if (durationString != null) {
                    duration = durationString.toLong()
                }
                listOfSongs?.get(currentSongInteger)?.length = duration
            }
        }
    }

    private fun fillPlayList() {
        val thread: Thread = object : Thread() {
            override fun run() {
                val currentTimeMillis = System.currentTimeMillis()
                fillPlayList(musicLocation)
                Log.l("listOfSongs loaded. Time elapsed: " + (System.currentTimeMillis() - currentTimeMillis) + " ms")
                Log.l("listOfSongs size: " + listOfSongs?.size)
            }
        }
        thread.start()
    }

    private fun fillPlayList(rootPath: String) {
        listOfSongs = ArrayList()
        try {
            val rootFolder = File(rootPath)
            if (!rootFolder.exists()) {
                return
            }
            val files: Array<File> = rootFolder.listFiles() //here you will get NPE if directory doesn't contains any file. Handle it like this.
            for (file in files) {
                if (file.isDirectory) {
                    fillPlayList(file.path)
                } else if (file.name.endsWith(".mp3")) {
//                    Log.l("Song added to play list: " + file.name)
                    val fileName = file.name.substring(0, file.name.lastIndexOf("."))
                    val fileFormat = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length)
                    val songFile = SongFile(rootPath, fileName, fileFormat, -1)
                    listOfSongs?.add(songFile)
//                    Log.l("fileList size: " + listOfSongs?.size)
                }
            }
        } catch (e: Exception) {
            Log.e(String().plus("Error loading play list: ").plus(e))
            return
        }
    }

    private fun sendBroadcastToActivity(broadcastMessage: BroadcastMessage) {
        sendBroadcastToActivity(broadcastMessage, null)
    }

    private fun sendBroadcastToActivity(broadcastMessage: BroadcastMessage, bundle: Bundle?) {
//        Log.l("MinichainsPlayerServiceLog:: sending broadcast $broadcastMessage")
        try {
            val broadCastIntent = Intent()
            broadCastIntent.action = broadcastMessage.toString()
            if (bundle != null) {
                broadCastIntent.putExtras(bundle)
            }
            sendBroadcast(broadCastIntent)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    inner class MinichainsPlayerServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.l("MinichainsPlayerServiceLog:: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.START_PLAYING.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_PLAYING")
                        play(currentSongPath, currentSongTime)
                    } else if (broadcast == BroadcastMessage.STOP_PLAYING.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: STOP_PLAYING")
                        stopPlaying()
                    } else if (broadcast == BroadcastMessage.START_STOP_PLAYING_NOTIFICATION.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_STOP_PLAYING_NOTIFICATION")
                        if (!playing) {
                            play(currentSongPath, currentSongTime)
                        } else {
                            stopPlaying()
                        }
                    } else if (broadcast == BroadcastMessage.PREVIOUS_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: PREVIOUS_SONG")
                        previous()
                    } else if (broadcast == BroadcastMessage.NEXT_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: NEXT_SONG")
                        next()
                    } else if (broadcast == BroadcastMessage.SHUFFLE.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: NEXT_SONG")
                        shuffle = !shuffle
                    } else if (broadcast == Intent.ACTION_MEDIA_BUTTON) {
                        Log.l("MinichainsPlayerServiceLog:: ACTION_MEDIA_BUTTON")
                    } else if (broadcast == Intent.ACTION_HEADSET_PLUG) {
                        Log.l("MinichainsPlayerServiceLog:: ACTION_HEADSET_PLUG")
                    } else {
//                        Log.l("MinichainsPlayerServiceLog:: Unknown broadcast received")
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

//            intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON)
//            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)

            registerReceiver(minichainsPlayerBroadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun createMinichainsPlayerServiceNotification() {
        //Service notification
        notificationName = resources.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
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

        /** PAUSE **/
        val playStopIntent = Intent()
        playStopIntent.action = BroadcastMessage.START_STOP_PLAYING_NOTIFICATION.toString()
        var playStopPendingIntent = PendingIntent.getBroadcast(this, 0, playStopIntent, 0)

        /** PREVIOUS **/
        val nextIntent = Intent()
        nextIntent.action = BroadcastMessage.NEXT_SONG.toString()
        val nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0)

        notificationTitle = currentSongName
        notification = NotificationCompat.Builder(this, serviceNotificationStringId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(notificationTitle)
//            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .addAction(R.drawable.baseline_skip_previous_white_18, "Previous", previousPendingIntent)
            .addAction(R.drawable.baseline_play_arrow_white_18, "Play/Stop", playStopPendingIntent)
            .addAction(R.drawable.baseline_skip_next_white_18, "Next", nextPendingIntent)

        notificationManagerCompat?.notify(serviceNotificationId, notification.build())
        this.startForeground(1, notification.build())
    }

    private fun updateNotificationTitle() {
        if (notificationTitle != currentSongName) {
            notificationTitle = currentSongName
            notification.setContentTitle(notificationTitle)
            notificationManagerCompat?.notify(serviceNotificationId, notification.build())
            this.startForeground(1, notification.build())
        }
    }

    private fun removeMinichainsPlayerServiceNotification() {
        if (notificationManagerCompat != null) {
            notificationManagerCompat!!.cancel(serviceNotificationId)
        }
    }
}