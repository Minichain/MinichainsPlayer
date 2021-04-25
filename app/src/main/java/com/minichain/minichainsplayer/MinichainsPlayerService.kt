package com.minichain.minichainsplayer

import android.annotation.SuppressLint
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
import android.media.audiofx.Visualizer
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
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_SONG
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME
import java.io.File
import java.util.Collections.shuffle

class MinichainsPlayerService : Service() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerServiceBroadcastReceiver

    private var mediaPlayer: MediaPlayer? = null
    private var updateActivityVariables01 = false
    private var updateActivityVariables02 = false

    private var currentSong = CurrentSong()

    private var listOfSongsSize: Int = 0
    private var shuffle = false

    private var listOfSongsSorted: ArrayList<SongFile>? = null
    private var listOfSongsShuffled: ArrayList<SongFile>? = null

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
        mediaSession.release()
    }

    private fun init() {
        DataBase.dataBaseHelper = FeedReaderDbHelper(this)

        if (DataBase.getMusicPath().isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && getStorageDirectory().exists()) {
                DataBase.setMusicPath(this, getStorageDirectory().path)
            } else if (getExternalStorageDirectory().exists()) {
                DataBase.setMusicPath(this, getExternalStorageDirectory().path)
            }
        }
        Log.l("Music Path: " + DataBase.getMusicPath())

        mediaPlayer = MediaPlayer()
        initVisualizer()

        listOfSongsSorted = ArrayList()
        listOfSongsShuffled = ArrayList()
        loadSongListFromDataBase()
        if (getListOfSongs() != null && getListOfSongs()?.isNotEmpty()!!) {
            val lastSongPlayed = DataBase.getLastSongPlayed()
            if (lastSongPlayed != null) updateCurrentSongInteger(DataBase.getLastSongPlayed())
        }
        updateCurrentSongInfo()

        minichainsPlayerBroadcastReceiver = MinichainsPlayerServiceBroadcastReceiver()
        registerMinichainsPlayerServiceBroadcastReceiver()

        initUpdateActivityThread()

        initMediaSessions()

        createMinichainsPlayerServiceNotification()
    }

    /**
     * MediaSession handles the inputs from the headphones. The user can pause/resume the
     * current playing song, and play the next/previous song.
     **/
    private lateinit var mediaSession: MediaSessionCompat

    private fun initMediaSessions() {
        mediaSession = MediaSessionCompat(applicationContext, MinichainsPlayerService::class.java.simpleName)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setMediaButtonReceiver(null)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        ).build())

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                Log.l("MediaSession:: onPlay")
                if (mediaPlayer != null && !mediaPlayer?.isPlaying!!) {
                    play()
                } else {
                    mediaPlayer?.pause()
                }
            }

            override fun onSkipToNext() {
                Log.l("MediaSession:: onSkipToNext")
                super.onSkipToNext()
                next()
            }

            override fun onSkipToPrevious() {
                Log.l("MediaSession:: onSkipToPrevious")
                super.onSkipToPrevious()
                previous()
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
        bundle01.putBoolean("playing", currentSong.playing)
        bundle01.putString("currentSongPath", currentSong.currentSongPath)
        bundle01.putInt("currentSongInteger", currentSong.currentSongInteger)
        if (getListOfSongs() != null && currentSong.currentSongInteger >= 0 && currentSong.currentSongInteger < getListOfSongs()?.size!!) {
            setCurrentSongName(getListOfSongs()?.get(currentSong.currentSongInteger)?.songName!!)
            bundle01.putString("currentSongName", currentSong.currentSongName)
            setCurrentSongLength(getListOfSongs()?.get(currentSong.currentSongInteger)?.length!!)
            bundle01.putInt("currentSongLength", currentSong.currentSongLength)
            setListOfSongsSize(getListOfSongs()?.size!!)
            bundle01.putInt("listOfSongsSize", listOfSongsSize)
        }
        bundle01.putBoolean("shuffle", shuffle)

        if (updateActivityVariables01) {
            sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_01, bundle01)
            updateActivityVariables01 = false
        }

        var bundle02 = Bundle()
        if (mediaPlayer != null) setCurrentSongTime(mediaPlayer?.currentPosition!!)
        bundle02.putInt("currentSongTime", currentSong.currentSongTime)
        if (updateActivityVariables02) {
            sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_02, bundle02)
            updateActivityVariables02 = false
        }
    }

    private fun getListOfSongs(): ArrayList<SongFile>? {
        return if (shuffle) listOfSongsShuffled
        else listOfSongsSorted
    }

    private fun setPlaying(newPlaying: Boolean) {
        if (currentSong.playing != newPlaying) {
            currentSong.playing = newPlaying
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongPath(newCurrentSongPath: String) {
        if (currentSong.currentSongPath != newCurrentSongPath) {
            currentSong.currentSongPath = newCurrentSongPath
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongInteger(newCurrentSongInteger: Int) {
        if (currentSong.currentSongInteger != newCurrentSongInteger) {
            currentSong.currentSongInteger = newCurrentSongInteger
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongName(newCurrentSongName: String) {
        if (currentSong.currentSongName != newCurrentSongName) {
            currentSong.currentSongName = newCurrentSongName
            updateActivityVariables01 = true
        }
    }

    private fun setCurrentSongLength(newCurrentSongLength: Int) {
        if (currentSong.currentSongLength != newCurrentSongLength) {
            currentSong.currentSongLength = newCurrentSongLength
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
            if (shuffle) shuffle(listOfSongsShuffled!!)
            //Changing the listOfSongs we use. We need to find the position of the current song on that list
            updateCurrentSongInteger(currentSong.currentSongName)
            updateCurrentSongInfo()
            updateActivityVariables01 = true
        }
    }

    private fun updateCurrentSongInteger(songName: String) {
        var songInteger = 0
        for (i in 0 until getListOfSongs()!!.size step 1) {
            if (getListOfSongs()!![i].songName == songName) break
            songInteger++
        }
        currentSong.currentSongInteger = songInteger
    }

    private fun setCurrentSongTime(newCurrentSongTime: Int) {
        if (newCurrentSongTime in 0 until currentSong.currentSongLength) {
            currentSong.currentSongTime = newCurrentSongTime
            updateActivityVariables02 = true
        }
    }

    private fun play(songPath: String = currentSong.currentSongPath, songTime: Int = currentSong.currentSongTime) {
        Log.l("Play $songPath")
        if (getListOfSongs().isNullOrEmpty()) return
        setCurrentSongPath(songPath)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(songPath)
            mediaPlayer?.prepare()
            mediaPlayer?.setOnPreparedListener {
                mediaPlayer?.seekTo(songTime)
                mediaPlayer?.start()
                DataBase.setLastSongPlayed(currentSong.currentSongName)
                Toast.makeText(this, getString(R.string.playing_song, currentSong.currentSongName), Toast.LENGTH_SHORT).show()
                mediaPlayer?.setOnCompletionListener {
                    if (currentSong.currentSongTime > 0) {
                        next()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Error. Song $songPath could not be played.")
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
        if (getListOfSongs().isNullOrEmpty()) return
        if (next) {
            setCurrentSongInteger((currentSong.currentSongInteger + 1) % getListOfSongs()?.size!!)
        } else {
            setCurrentSongInteger(currentSong.currentSongInteger - 1)
            if (currentSong.currentSongInteger < 0) {
                setCurrentSongInteger(getListOfSongs()?.size!! - 1)
            }
        }
        mediaPlayer?.pause()
        if (getListOfSongs() != null && !getListOfSongs()?.isEmpty()!!) {
            setCurrentSongTime(0)
            updateCurrentSongInfo()
            play()
        }
    }

    private fun previous() {
        next(false)
    }

    private fun updateCurrentSongInfo() {
        if (getListOfSongs() != null && getListOfSongs()?.isNotEmpty()!!
            && currentSong.currentSongInteger >= 0
            && currentSong.currentSongInteger < getListOfSongs()?.size!!) {
            setCurrentSongName(getListOfSongs()?.get(currentSong.currentSongInteger)?.songName.toString())
            setCurrentSongPath(String().plus(getListOfSongs()?.get(currentSong.currentSongInteger)?.path.toString())
                .plus("/")
                .plus(getListOfSongs()?.get(currentSong.currentSongInteger)?.songName)
                .plus(".")
                .plus(getListOfSongs()?.get(currentSong.currentSongInteger)?.format))

            if (getListOfSongs()?.get(currentSong.currentSongInteger)?.length!!.toInt() <= 0) {
                try {
                    val metaRetriever = MediaMetadataRetriever()
                    metaRetriever.setDataSource(currentSong.currentSongPath)
                    val durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    var duration: Int = -1
                    if (durationString != null) {
                        duration = durationString.toInt()
                    }
                    getListOfSongs()?.get(currentSong.currentSongInteger)?.length = duration
                } catch (e: Exception) {
                    Log.e("Error. Current song could not be updated.")
                }
            }
        }
    }

    private lateinit var visualizer: Visualizer

    private fun initVisualizer() {
        if (mediaPlayer != null) {
            visualizer = Visualizer(mediaPlayer!!.audioSessionId)
            visualizer.captureSize = Visualizer.getCaptureSizeRange()[0]

            visualizer.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {

                }

                override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    val bundle = Bundle()
                    var spectrum = FloatArray(10) {
                        i -> fft!![i].toFloat()
                    }

                    spectrum = Utils.normalize(spectrum)
                    spectrum = Utils.smooth(spectrum)

                    bundle.putFloatArray("spectrum", spectrum)
                    sendBroadcastToActivity(BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_03, bundle)
                }
            }, Visualizer.getMaxCaptureRate(), false, true)

            visualizer.enabled = true;
        }
    }

    private fun fillPlayList() {
        Toast.makeText(this, getString(R.string.filling_play_list), Toast.LENGTH_SHORT).show()
        val thread: Thread = object : Thread() {
            override fun run() {
                val currentTimeMillis = System.currentTimeMillis()
                fillDataBase(DataBase.getMusicPaths())
                loadSongListFromDataBase()
                updateCurrentSongInfo()
                Log.l("listOfSongs loaded. Time elapsed: " + (System.currentTimeMillis() - currentTimeMillis) + " ms")
                Log.l("listOfSongs size: " + getListOfSongs()?.size)
            }
        }
        thread.start()
    }

    private fun clearPlayList() {
        Toast.makeText(this, getString(R.string.clearing_play_list), Toast.LENGTH_SHORT).show()
        mediaPlayer?.pause()
        listOfSongsSorted?.clear()
        listOfSongsShuffled?.clear()
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
                Log.e("Folder does not exist! rootPath: $rootPath")
                return
            }
            val files: Array<File>? = rootFolder.listFiles() //here you will get NPE if directory doesn't contains any file. Handle it like this.
            if (!files.isNullOrEmpty()) {
                for (file in files) {
                    if (file.isDirectory) {
                        fillDataBase(file.path)
                    } else if (hasValidExtension(file.name)) {
//                        Log.l("Song added to play list: " + file.name)
                        val fileName = file.name.substring(0, file.name.lastIndexOf("."))
                        val fileFormat = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length)
                        val songFile = SongFile(rootPath, fileName, fileFormat, -1)
                        Log.l("Song added to play list. rootPath: " + rootPath
                                + ", fileName: " + fileName
                                + ", fileFormat: " + fileFormat)
//                        Log.l("fileList size: " + getListOfSongs()?.size)
                        DataBase.insertOrUpdateSongInDataBase(rootPath, fileName, fileFormat)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(String().plus("Error loading play list: ").plus(e))
            return
        }
    }

    private fun hasValidExtension(songName: String): Boolean {
        return songName.endsWith(".mp3") || songName.endsWith(".ogg") || songName.endsWith(".wav")
    }

    private fun loadSongListFromDataBase() {
        try {
            val dataBase = DataBase.dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery("SELECT * FROM ${SONG_LIST_TABLE_NAME} ORDER BY ${COLUMN_SONG} COLLATE NOCASE ASC", null)
            val listOfSongs: ArrayList<SongFile>?
            listOfSongs = null
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast) {
                    val path = cursor.getString(cursor.getColumnIndex("path"))
                    val songName = cursor.getString(cursor.getColumnIndex("song"))
                    val format = cursor.getString(cursor.getColumnIndex("format"))
                    Log.l("loadSongListFromDataBase: path: $path")
                    Log.l("loadSongListFromDataBase: songName: $songName")
                    Log.l("loadSongListFromDataBase: format: $format")
                    val songFile = SongFile(path, songName, format, -1)
                    listOfSongs?.add(songFile)
                    listOfSongsSorted?.add(songFile)
                    listOfSongsShuffled?.add(songFile)
                    cursor.moveToNext()
                }
            }
            cursor.close()
            shuffle(listOfSongsShuffled!!)
        } catch (e: Exception) {

        }
    }

    private fun getSongInteger(songName: String): Int {
        for (i in 0 until getListOfSongs()?.size!! step 1) {
            if (getListOfSongs()!![i].songName == songName) {
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
                        setCurrentSongInteger(getSongInteger(currentSong.currentSongName))
                        setCurrentSongPath(getListOfSongs()?.get(currentSong.currentSongInteger)?.path!!.toString()
                            .plus("/")
                            .plus(currentSong.currentSongName)
                            .plus(".")
                            .plus(getListOfSongs()?.get(currentSong.currentSongInteger)?.format!!.toString()))
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
                            mediaPlayer?.seekTo(currentSong.currentSongTime)
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
//                        Log.l("MinichainsPlayerServiceLog:: Unregistered broadcast received. $broadcast")
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

    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationName: CharSequence
    private lateinit var notificationTitle: CharSequence
    private var notificationPlaying = false
    private var notificationManager: NotificationManager? = null
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private val serviceNotificationStringId = "MINICHAINS_PLAYER_SERVICE_NOTIFICATION"
    private val serviceNotificationId = 1

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

        notificationTitle = currentSong.currentSongName
        notificationPlaying = currentSong.playing
        notification = NotificationCompat.Builder(this, serviceNotificationStringId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(notificationTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))

        updateNotificationActions()

        notificationManagerCompat?.notify(serviceNotificationId, notification.build())
        this.startForeground(serviceNotificationId, notification.build())
    }

    private fun updateNotification() {
        if (notificationTitle != currentSong.currentSongName || notificationPlaying != currentSong.playing) {
            if (notificationTitle != currentSong.currentSongName) {
                notificationTitle = currentSong.currentSongName
                notification.setContentTitle(notificationTitle)
            }
            if (notificationPlaying != currentSong.playing) {
                notificationPlaying = currentSong.playing
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
        if (currentSong.playing) {
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
