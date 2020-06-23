package com.example.minichainsplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Process
import android.util.Log
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

class MainActivity : AppCompatActivity() {
    lateinit var playButton: ImageButton
    lateinit var previousButton: ImageButton
    lateinit var nextButton: ImageButton
    lateinit var shuffleButton: ImageButton
    lateinit var currentSongTexView: TextView
    lateinit var currentSongLengthTexView: TextView
    lateinit var currentSongCurrentTimeTexView: TextView

    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongTime = 0
    private var musicLocation = ""
    private var currentSongInteger = 0
    private var shuffle = false

    private var listOfSongs: ArrayList<SongFile>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 0) //Check the requestCode later
        }

        /** INITIALIZE VIEWS **/
        playButton = this.findViewById(R.id.play_button)
        previousButton = this.findViewById(R.id.previous_button)
        nextButton = this.findViewById(R.id.next_button)
        shuffleButton = this.findViewById(R.id.shuffle_button)
        currentSongTexView = this.findViewById(R.id.current_song_name)
        currentSongLengthTexView = this.findViewById(R.id.current_song_length)
        currentSongCurrentTimeTexView = this.findViewById(R.id.current_song_current_time)

        musicLocation = "/sdcard/Music/"
//        musicLocation = String().plus("/storage/0C80-1910").plus("/Music/")
        Log.d("MinichainsPlayer:: ", "musicLocation: " + musicLocation)

        fillPlayList()

        initUpdateCurrentSongInfoThread()

        updateShuffleButtonAlpha()

        playButton.setOnClickListener {
            if (!isPlaying) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show()
                    if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                        playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, currentSongTime)
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
            mediaPlayer?.pause()
            if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, 0)
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
            mediaPlayer?.pause()
            if (listOfSongs != null && !listOfSongs?.isEmpty()!!) {
                playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, 0)
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
                Log.d("MinichainsPlayer:: ", "listOfSongs loaded. Time elapsed: " + (System.currentTimeMillis() - currentTimeMillis) + " ms")
                Log.d("MinichainsPlayer:: ", "listOfSongs size: " + listOfSongs?.size)
            }
        }
        thread.start()
    }

    private fun initUpdateCurrentSongInfoThread() {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(250)
                        runOnUiThread {
                            updateCurrentSongInfo()
                        }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }

        thread.start()
    }

    private fun updateCurrentSongInfo() {
        if (listOfSongs == null || listOfSongs?.isEmpty()!!) {
            return
        }
        currentSongTexView.text = listOfSongs?.get(currentSongInteger)?.songName
        currentSongLengthTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(listOfSongs?.get(currentSongInteger)?.length)
        currentSongCurrentTimeTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(mediaPlayer?.currentPosition?.toLong())
        updateSongDuration()
    }

    private fun playSong(songPath: String?, currentSongTime: Int) {
        this.currentSongTime = currentSongTime
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(songPath)
        mediaPlayer?.prepare()
        mediaPlayer?.seekTo(currentSongTime)
        mediaPlayer?.start()
        isPlaying = true
        updateCurrentSongInfo()
        playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_pause_white_48)
    }

    private fun updateSongDuration() {
        if (listOfSongs?.get(currentSongInteger)?.length!!.toInt() <= 0) {
            val metaRetriever = MediaMetadataRetriever()
            metaRetriever.setDataSource(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName)
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
        if (isPlaying) {
            isPlaying = false
            mediaPlayer?.pause()
            currentSongTime = mediaPlayer?.currentPosition!!
        }
        playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
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
                    Log.d("MinichainsPlayer:: ", "Song added to play list: " + file.name)
                    val songFile = SongFile(rootPath, file.name, -1)
                    listOfSongs?.add(songFile)
                    Log.d("MinichainsPlayer:: ", "fileList size: " + listOfSongs?.size)
                }
            }
        } catch (e: Exception) {
            Log.e("MinichainsPlayer:: ", "Error loading play list: " + e)
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
//        val serviceIntent = Intent(applicationContext, MinichainsPlayerService::class.java)
//        applicationContext.stopService(serviceIntent)
    }
}