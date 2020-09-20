package com.example.minichainsplayer

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getStorageDirectory
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_SONG
import com.example.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class MinichainsPlayerService : Service() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerServiceBroadcastReceiver
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationName: CharSequence
    private lateinit var notificationTitle: CharSequence
    private var notificationPlaying = false
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val serviceNotificationStringId = "MINICHAINS_PLAYER_SERVICE_NOTIFICATION"
    private val serviceNotificationId = 1

    private var mediaPlayer: MediaPlayer? = null
    private var updateActivityVariables01 = false
    private var updateActivityVariables02 = false
    private var playing = false
    private var currentSongTime = 0
    private var currentSongPath = ""
    private var currentSongInteger = 0
    private var currentSongName = ""
    private var currentSongLength: Long = 0
    private var listOfSongsSize = 0
    private var shuffle = false

    private var listOfSongs: ArrayList<SongFile>? = null

    private lateinit var mediaSession: MediaSessionCompat
    private var timesPressingMediaButton = 0

    private lateinit var updateActivityInfoThread: Thread

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
        Log.l("MinichainsPlayerServiceLog:: onDestroy $this")
        updateActivityInfoThread.interrupt()

        if (mediaPlayer != null) stopAndRelease()

        unregisterReceiver(minichainsPlayerBroadcastReceiver)
        removeMinichainsPlayerServiceNotification()
    }

    private fun init() {
        DataBase.dataBaseHelper = FeedReaderDbHelper(this)

        if (DataBase.getMusicPath().isEmpty()) {
            if (getStorageDirectory().exists()) {
                DataBase.setMusicPath(this, getStorageDirectory().path)
            } else if (getExternalStorageDirectory().exists()) {
                DataBase.setMusicPath(this, getExternalStorageDirectory().path)
            }
        }
        Log.l("Music Path: " + DataBase.getMusicPath())

        mediaPlayer = MediaPlayer()

        listOfSongs = ArrayList()
        loadSongListFromDataBase()
        updateCurrentSongInfo()

        minichainsPlayerBroadcastReceiver = MinichainsPlayerServiceBroadcastReceiver()
        registerMinichainsPlayerServiceBroadcastReceiver()

        initUpdateActivityThread()

        initMediaSessions()

        createMinichainsPlayerServiceNotification()
    }

    var mediaSessionTimer = Timer()

    /**
     * MediaSession handles the inputs from the headphones. The user can pause/resume the
     * current playing song, and play the next/previous song.
     **/
    private fun initMediaSessions() {
        mediaSession = MediaSessionCompat(applicationContext, MinichainsPlayerService::class.java.simpleName)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.setMediaButtonReceiver(null)
        var mStateBuilder = PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY)
        mediaSession.setPlaybackState(mStateBuilder.build())
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            //callback code is here.
            override fun onPlay() {
                mediaSessionTimer.cancel()
                mediaSessionTimer = Timer()
                mediaSessionTimer.schedule(object : TimerTask() {
                    override fun run() {
                        Thread(Runnable {
                            Log.l("timesPressingMediaButton: $timesPressingMediaButton")
                            if (timesPressingMediaButton == 1) {
                                if (mediaPlayer != null && !mediaPlayer?.isPlaying!!) {
                                    play()
                                } else {
                                    mediaPlayer?.pause()
                                }
                            } else if (timesPressingMediaButton == 2) {
                                next()
                            } else if (timesPressingMediaButton >= 3) {
                                previous()
                            }

                            timesPressingMediaButton = 0
                        }).start()
                    }
                }, 450)

                Log.l("timesPressingMediaButton++")
                timesPressingMediaButton++
            }
        })
        mediaSession.isActive = true
    }

    private fun initUpdateActivityThread() {
        val sleepTime = 250
        updateActivityInfoThread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(sleepTime.toLong())
                        updateCurrentSongInfo()
                        updateActivityVariables()
                        updateNotification()
                    }
                } catch (e: InterruptedException) {
                }
            }
        }
        updateActivityInfoThread.start()
    }

    private fun updateActivityVariables() {
        var bundle01 = Bundle()
        if (mediaPlayer != null) setPlaying(mediaPlayer?.isPlaying!!)
        bundle01.putBoolean("playing", playing)
        bundle01.putString("currentSongPath", currentSongPath)
        bundle01.putInt("currentSongInteger", currentSongInteger)
        if (listOfSongs != null && currentSongInteger >= 0 && currentSongInteger < listOfSongs?.size!!) {
            setCurrentSongName(listOfSongs?.get(currentSongInteger)?.songName!!)
            bundle01.putString("currentSongName", currentSongName)
            setCurrentSongLength(listOfSongs?.get(currentSongInteger)?.length!!)
            bundle01.putLong("currentSongLength", currentSongLength)
            setListOfSongsSize(listOfSongs?.size!!)
            bundle01.putInt("listOfSongsSize", listOfSongsSize)
        }
        bundle01.putBoolean("shuffle", shuffle)

        if (updateActivityVariables01) {
            sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_01, bundle01)
            updateActivityVariables01 = false
        }

        var bundle02 = Bundle()
        if (mediaPlayer != null) setCurrentSongTime(mediaPlayer?.currentPosition!!)
        bundle02.putInt("currentSongTime", currentSongTime)
        if (updateActivityVariables02) {
            sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_02, bundle02)
            updateActivityVariables02 = false
        }
    }

    private fun setPlaying(newPlaying: Boolean) {
        if (playing != newPlaying) {
            playing = newPlaying
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongPath(newCurrentSongPath: String) {
        if (!currentSongPath.equals(newCurrentSongPath)) {
            currentSongPath = newCurrentSongPath
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongInteger(newCurrentSongInteger: Int) {
        if (currentSongInteger != newCurrentSongInteger) {
            currentSongInteger = newCurrentSongInteger
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongName(newCurrentSongName: String) {
        if (currentSongName != newCurrentSongName) {
            currentSongName = newCurrentSongName
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongLength(newCurrentSongLength: Long) {
        if (currentSongLength != newCurrentSongLength) {
            currentSongLength = newCurrentSongLength
            updateActivityVariables01 = true
        }
    }

    private fun setListOfSongsSize(newListOfSongsSize: Int) {
        if (listOfSongsSize != newListOfSongsSize) {
            listOfSongsSize = newListOfSongsSize
            updateActivityVariables01 = true
        }
    }

    private fun setShuffle(newShuffle: Boolean) {
        if (shuffle != newShuffle) {
            shuffle = newShuffle
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongTime(newCurrentSongTime: Int) {
        if (newCurrentSongTime in 0 until currentSongLength && currentSongTime != newCurrentSongTime) {
            currentSongTime = newCurrentSongTime
            updateActivityVariables02 = true
        }
    }

    private fun play(songPath: String = currentSongPath, songTime: Int = currentSongTime) {
        Log.l("Play $songPath")
        if (listOfSongs.isNullOrEmpty()) return

        setCurrentSongPath(songPath)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(songPath)
            mediaPlayer?.prepare()
            mediaPlayer?.setOnPreparedListener {
                mediaPlayer?.seekTo(songTime)
                mediaPlayer?.start()
                Toast.makeText(this, getString(R.string.playing_song, currentSongName), Toast.LENGTH_SHORT).show()
                mediaPlayer?.setOnCompletionListener {
                    if (currentSongTime > 0) {
                        next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Song $songPath could not be played.")
            stopAndRelease()
            play()
        }
    }

    private fun pause() {
        mediaPlayer?.pause()
        Toast.makeText(this, getString(R.string.pausing), Toast.LENGTH_SHORT).show()
    }

    private fun pauseAndRelease() {
        mediaPlayer?.pause()
        mediaPlayer?.release()
    }

    private fun stopAndRelease() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun next(next: Boolean = true) {
        if (listOfSongs.isNullOrEmpty()) return
        if (shuffle) {
            setCurrentSongInteger((Math.random() * (listOfSongs?.size!! - 1)).toInt())
        } else {
            if (next) {
                setCurrentSongInteger((currentSongInteger + 1) % listOfSongs?.size!!)
            } else {
                setCurrentSongInteger(currentSongInteger - 1)
                if (currentSongInteger < 0) {
                    setCurrentSongInteger(listOfSongs?.size!! - 1)
                }
            }
        }
        mediaPlayer?.pause()
        if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
            setCurrentSongTime(0)
            updateCurrentSongInfo()
            play()
        }
    }

    private fun previous() {
        next(false)
    }

    private fun updateCurrentSongInfo() {
        if (listOfSongs != null && listOfSongs?.isNotEmpty()!! && currentSongInteger >= 0 && currentSongInteger < listOfSongs?.size!!) {
            setCurrentSongName(listOfSongs?.get(currentSongInteger)?.songName.toString())
            setCurrentSongPath(String().plus(listOfSongs?.get(currentSongInteger)?.path.toString())
                .plus("/")
                .plus(listOfSongs?.get(currentSongInteger)?.songName)
                .plus(".")
                .plus(listOfSongs?.get(currentSongInteger)?.format))

            if (listOfSongs?.get(currentSongInteger)?.length!!.toInt() <= 0) {
                try {
                    val metaRetriever = MediaMetadataRetriever()
                    metaRetriever.setDataSource(currentSongPath)
                    val durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    var duration: Long = -1
                    if (durationString != null) {
                        duration = durationString.toLong()
                    }
                    listOfSongs?.get(currentSongInteger)?.length = duration
                } catch (e: Exception) {
                    Log.e(e.toString())
                }
            }
        }
    }

    private fun fillPlayList() {
        Toast.makeText(this, getString(R.string.filling_play_list), Toast.LENGTH_LONG).show()
        val thread: Thread = object : Thread() {
            override fun run() {
                val currentTimeMillis = System.currentTimeMillis()
                fillDataBase(DataBase.getMusicPaths())
                loadSongListFromDataBase()
                updateCurrentSongInfo()
                Log.l("listOfSongs loaded. Time elapsed: " + (System.currentTimeMillis() - currentTimeMillis) + " ms")
                Log.l("listOfSongs size: " + listOfSongs?.size)
            }
        }
        thread.start()
    }

    private fun clearPlayList() {
        Toast.makeText(this, getString(R.string.clearing_play_list), Toast.LENGTH_LONG).show()
        mediaPlayer?.pause()
        listOfSongs?.clear()
        setCurrentSongName("")
        setCurrentSongPath("")
        setCurrentSongInteger(0)
        setCurrentSongTime(0)
        DataBase.clearSongListTable()
    }

    private fun fillDataBase(rootPaths: ArrayList<String>) {
        Log.l("Filling database with songs. rootPaths size: ${rootPaths.size}")
        for (i in 0 until rootPaths?.size!! step 1) {
            fillDataBase(rootPaths[i])
        }
    }

    private fun fillDataBase(rootPath: String) {
        Log.l("Filling database with songs from: '$rootPath'")
        try {
            val rootFolder = File(rootPath)
            if (!rootFolder.exists()) {
                return
            }
            val files: Array<File> = rootFolder.listFiles()!! //here you will get NPE if directory doesn't contains any file. Handle it like this.
            for (file in files) {
                if (file.isDirectory) {
                    fillDataBase(file.path)
                } else if (file.name.endsWith(".mp3")) {
//                    Log.l("Song added to play list: " + file.name)
                    val fileName = file.name.substring(0, file.name.lastIndexOf("."))
                    val fileFormat = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length)
                    val songFile = SongFile(rootPath, fileName, fileFormat, -1)
                    Log.l("Song added to play list. rootPath: " + rootPath
                            + ", fileName: " + fileName
                            + ", fileFormat: " + fileFormat)
//                    Log.l("fileList size: " + listOfSongs?.size)
                    DataBase.insertOrUpdateSongInDataBase(rootPath, fileName, fileFormat)
                }
            }

        } catch (e: Exception) {
            Log.e(String().plus("Error loading play list: ").plus(e))
            return
        }
    }

    private fun loadSongListFromDataBase() {
        try {
            val dataBase = DataBase.dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery("SELECT * FROM $SONG_LIST_TABLE_NAME ORDER BY $COLUMN_SONG ASC", null)
            listOfSongs?.clear()
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val path = cursor.getString(cursor.getColumnIndex("path"))
                    val songName = cursor.getString(cursor.getColumnIndex("song")).replace("_", "'")
                    val format = cursor.getString(cursor.getColumnIndex("format"))
                    Log.l("loadSongListFromDataBase: path: $path")
                    Log.l("loadSongListFromDataBase: songName: $songName")
                    Log.l("loadSongListFromDataBase: format: $format")
                    val songFile = SongFile(path, songName, format, -1)
                    listOfSongs?.add(songFile)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        } catch (e: Exception) {

        }
    }

    private fun getSongInteger(songName: String): Int {
        for (i in 0 until listOfSongs?.size!! step 1) {
            if (listOfSongs!![i].songName == songName) {
                return i
            }
        }
        return -1
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

    /**
     * BROADCAST RECEIVER
     **/

    inner class MinichainsPlayerServiceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.l("MinichainsPlayerServiceLog:: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.START_PLAYING.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_PLAYING")
                        play()
                    } else if (broadcast == BroadcastMessage.START_PLAYING_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_PLAYING_SONG")
                        mediaPlayer?.pause()
                        setCurrentSongName(extras?.getString("currentSongName").toString())
                        setCurrentSongInteger(getSongInteger(currentSongName))
                        setCurrentSongPath(listOfSongs?.get(currentSongInteger)?.path!!.toString()
                            .plus("/")
                            .plus(currentSongName)
                            .plus(".")
                            .plus(listOfSongs?.get(currentSongInteger)?.format!!.toString()))
                        setCurrentSongTime(0)
                        play()
                    } else if (broadcast == BroadcastMessage.STOP_PLAYING.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: STOP_PLAYING")
                        pause()
                    } else if (broadcast == BroadcastMessage.START_STOP_PLAYING_NOTIFICATION.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: START_STOP_PLAYING_NOTIFICATION")
                        if (mediaPlayer != null && !mediaPlayer?.isPlaying!!) {
                            play()
                        } else {
                            pause()
                        }
                    } else if (broadcast == BroadcastMessage.PREVIOUS_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: PREVIOUS_SONG")
                        previous()
                    } else if (broadcast == BroadcastMessage.NEXT_SONG.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: NEXT_SONG")
                        next()
                    } else if (broadcast == BroadcastMessage.SHUFFLE.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: SHUFFLE")
                        setShuffle(!shuffle)
                    } else if (broadcast == BroadcastMessage.SET_CURRENT_SONG_TIME.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: SET_CURRENT_SONG_TIME")
                        if (extras != null) {
                            setCurrentSongTime(extras.getInt("currentSongTime"))
                            mediaPlayer?.seekTo(currentSongTime)
                        }
                    } else if (broadcast == Intent.ACTION_MEDIA_BUTTON) {
                        Log.l("MinichainsPlayerServiceLog:: ACTION_MEDIA_BUTTON")
                    } else if (broadcast == Intent.ACTION_HEADSET_PLUG) {
                        Log.l("MinichainsPlayerServiceLog:: ACTION_HEADSET_PLUG")
                        pause()
                    } else if (broadcast == BroadcastMessage.FILL_PLAYLIST.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: FILL_PLAYLIST")
                        fillPlayList()
                    } else if (broadcast == BroadcastMessage.CLEAR_PLAYLIST.toString()) {
                        Log.l("MinichainsPlayerServiceLog:: CLEAR_PLAYLIST")
                        clearPlayList()
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
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG)

            registerReceiver(minichainsPlayerBroadcastReceiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * SERVICE NOTIFICATION
     **/

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
            flags = Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, intent.flags)

        notificationTitle = currentSongName
        notificationPlaying = playing
        notification = NotificationCompat.Builder(this, serviceNotificationStringId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(notificationTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(mediaSession.sessionToken))

        updateNotificationActions()

        notificationManagerCompat?.notify(serviceNotificationId, notification.build())
        this.startForeground(1, notification.build())
    }

    private fun updateNotification() {
        if (notificationTitle != currentSongName || notificationPlaying != playing) {
            if (notificationTitle != currentSongName) {
                notificationTitle = currentSongName
                notification.setContentTitle(notificationTitle)
            }
            if (notificationPlaying != playing) {
                notificationPlaying = playing
                updateNotificationActions()
            }
            notificationManagerCompat?.notify(serviceNotificationId, notification.build())
            this.startForeground(1, notification.build())
        }
    }

    @SuppressLint("RestrictedApi")
    private fun updateNotificationActions() {
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

        var listOfActions = ArrayList<NotificationCompat.Action>()
        listOfActions.add(NotificationCompat.Action(R.drawable.baseline_skip_previous_white_36, "Previous", previousPendingIntent))
        if (playing) {
            listOfActions.add(NotificationCompat.Action(R.drawable.baseline_pause_white_36, "Pause", playStopPendingIntent))
        } else {
            listOfActions.add(NotificationCompat.Action(R.drawable.baseline_play_arrow_white_36, "Play", playStopPendingIntent))
        }
        listOfActions.add(NotificationCompat.Action(R.drawable.baseline_skip_next_white_36, "Next", nextPendingIntent))

        notification.mActions.clear()
        for (i in 0 until listOfActions.size step 1) {
            notification.addAction(listOfActions[i])
        }
    }

    private fun removeMinichainsPlayerServiceNotification() {
        if (notificationManagerCompat != null) {
            notificationManagerCompat!!.cancel(serviceNotificationId)
        }
    }
}