package com.example.minichainsplayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.system.exitProcess

class MinichainsPlayerActivity : AppCompatActivity() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerActivityBroadcastReceiver

    lateinit var playButton: ImageButton
    lateinit var previousButton: ImageButton
    lateinit var nextButton: ImageButton
    lateinit var shuffleButton: ImageButton
    lateinit var currentSongTexView: TextView
    lateinit var currentSongLengthTexView: TextView
    lateinit var currentSongCurrentTimeTexView: TextView

    private var playing = false
    private var currentSongTime: Int = 0
    private var musicLocation = ""
    private var currentSongInteger = 0
    private var shuffle = false

    private var listOfSongs: ArrayList<SongFile>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        init()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 0) //Check the requestCode later
        }
    }

    override fun onStart() {
        Log.l("MinichainsPlayerActivityLog:: onStart")
        super.onStart()
        registerMinichainsPlayerActivityBroadcastReceiver()
    }

    override fun onResume() {
        Log.l("MinichainsPlayerActivityLog:: onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.l("MinichainsPlayerActivityLog:: onPause")
        super.onPause()
        try {
            unregisterReceiver(minichainsPlayerBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("MinichainsPlayerActivityLog:: error un-registering receiver $e")
        }
    }

    override fun onStop() {
        Log.l("MinichainsPlayerActivityLog:: onStop")
        super.onStop()
        try {
            unregisterReceiver(minichainsPlayerBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun init() {
        val serviceIntent = Intent(applicationContext, MinichainsPlayerService::class.java)

        //Start service:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                applicationContext.startForegroundService(serviceIntent)
                Log.l("startForegroundService")
            } catch (e: java.lang.Exception) {
                applicationContext.startService(serviceIntent)
                Log.l("startService")
            }
        } else {
            Log.l("startService")
            applicationContext.startService(serviceIntent)
        }

        /** INITIALIZE VIEWS **/
        playButton = this.findViewById(R.id.play_button)
        previousButton = this.findViewById(R.id.previous_button)
        nextButton = this.findViewById(R.id.next_button)
        shuffleButton = this.findViewById(R.id.shuffle_button)
        currentSongTexView = this.findViewById(R.id.current_song_name)
        currentSongTexView.isSelected = true
        currentSongLengthTexView = this.findViewById(R.id.current_song_length)
        currentSongCurrentTimeTexView = this.findViewById(R.id.current_song_current_time)

        musicLocation = "/sdcard/Music/"
//        musicLocation = String().plus("/storage/0C80-1910").plus("/Music/")
        Log.l("musicLocation: " + musicLocation)

        fillPlayList()

        initUpdateViewsThread()

        updateShuffleButtonAlpha()

        playButton.setOnClickListener {
            if (!playing) {
                sendBroadcastToService(BroadcastMessage.START_PLAYING)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show()
                    if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                        playCurrentSong()
                    }
                } else {
                    Toast.makeText(this, "Cannot be played", Toast.LENGTH_SHORT).show()
                }
            } else {
                pauseCurrentSong()
            }
        }

        previousButton.setOnClickListener {
            Toast.makeText(this, "Playing previous song", Toast.LENGTH_SHORT).show()
            if (shuffle) {
                currentSongInteger = (Math.random() * (listOfSongs?.size!! - 1)).toInt()
            } else {
                currentSongInteger = (currentSongInteger + 1) % listOfSongs?.size!!
            }
            sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
            if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                this.currentSongTime = 0
                playCurrentSong()
            }
        }

        nextButton.setOnClickListener {
            Toast.makeText(this, "Playing next song", Toast.LENGTH_SHORT).show()
            if (shuffle) {
                currentSongInteger = (Math.random() * (listOfSongs?.size!! - 1)).toInt()
            } else {
                currentSongInteger--
                if (currentSongInteger < 0) {
                    currentSongInteger = listOfSongs?.size!! - 1
                }
            }
            sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
            if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                this.currentSongTime = 0
                playCurrentSong()
            }
        }

        shuffleButton.setOnClickListener {
            shuffle = if (shuffle) {
                Toast.makeText(this, "Shuffle disabled", Toast.LENGTH_SHORT).show()
                false
            } else {
                Toast.makeText(this, "Shuffle enabled", Toast.LENGTH_SHORT).show()
                true
            }
            updateShuffleButtonAlpha()
        }
    }

    private fun updateShuffleButtonAlpha() {
        if (shuffle) {
            shuffleButton.alpha = 1f
        } else {
            shuffleButton.alpha = 0.5f
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

    private fun initUpdateViewsThread() {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(200)
                        runOnUiThread {
                            updateViews()
                        }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }
        thread.start()
    }

    private fun updateViews() {
        if (listOfSongs == null || listOfSongs?.isEmpty()!!) {
            return
        }

        if (currentSongTexView.text != listOfSongs?.get(currentSongInteger)?.songName) {
            currentSongTexView.text = listOfSongs?.get(currentSongInteger)?.songName
        }

        currentSongLengthTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(listOfSongs?.get(currentSongInteger)?.length)
        currentSongCurrentTimeTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(currentSongTime.toLong())

        updateSongDuration()

        if (currentSongTime >= listOfSongs?.get(currentSongInteger)?.length!!) {
            //Song has ended. Playing next song...
            nextButton.performClick()
        }

        if (playing) {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_pause_white_48)
        } else {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
        }
    }

    private fun playCurrentSong() {
        var bundle = Bundle()
        bundle.putString("songPath", listOfSongs?.get(currentSongInteger)?.path
                + listOfSongs?.get(currentSongInteger)?.songName
                + "."
                + listOfSongs?.get(currentSongInteger)?.format)
        bundle.putInt("currentSongTime", currentSongTime)
        sendBroadcastToService(BroadcastMessage.START_PLAYING, bundle)
        playing = true
        updateViews()
    }

    private fun updateSongDuration() {
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

    private fun pauseCurrentSong() {
        Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show()
        if (playing) {
            playing = false
            sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dropdown_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val intent: Intent
        return when (id) {
            R.id.exit_app_option -> {
                closeApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun closeApp() {
        onDestroy()
        Process.killProcess(Process.myPid())
        exitProcess(-1)
    }

    override fun onDestroy() {
        super.onDestroy()
        //If there is a Service running...
        val serviceIntent = Intent(applicationContext, MinichainsPlayerService::class.java)
        applicationContext.stopService(serviceIntent)
    }

    private fun sendBroadcastToService(broadcastMessage: BroadcastMessage) {
        sendBroadcastToService(broadcastMessage, null)
    }

    private fun sendBroadcastToService(broadcastMessage: BroadcastMessage, bundle: Bundle?) {
        Log.l("MinichainsPlayerActivityLog:: sending broadcast $broadcastMessage")
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

    inner class MinichainsPlayerActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.l("MinichainsPlayerActivityLog:: Broadcast received. Context: " + context + ", intent:" + intent.action)
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.START_PLAYING.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: START_PLAYING")
                    } else if (broadcast == BroadcastMessage.STOP_PLAYING.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: STOP_PLAYING")
                    } else if (broadcast == BroadcastMessage.START_STOP_PLAYING_NOTIFICATION.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: START_STOP_PLAYING_NOTIFICATION")
                    } else if (broadcast == BroadcastMessage.PREVIOUS_SONG.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: PREVIOUS_SONG")
                    } else if (broadcast == BroadcastMessage.NEXT_SONG.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: NEXT_SONG")
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY.toString()) {
//                        Log.l("MinichainsPlayerActivityLog:: UPDATE_ACTIVITY")
                        if (extras != null) {
                            currentSongTime = extras.getInt("currentSongTime")
                            playing = extras.getBoolean("playing")
                        }
                    } else {
                        Log.l("MinichainsPlayerActivityLog:: Unknown broadcast received")
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun registerMinichainsPlayerActivityBroadcastReceiver() {
        minichainsPlayerBroadcastReceiver = MinichainsPlayerActivityBroadcastReceiver()
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }
            registerReceiver(minichainsPlayerBroadcastReceiver, intentFilter)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }
}