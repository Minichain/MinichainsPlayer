package com.example.minichainsplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File


class MainActivity : AppCompatActivity() {
    lateinit var playButton: ImageButton
    lateinit var previousButton: ImageButton
    lateinit var nextButton: ImageButton
    lateinit var currentSongTexView: TextView
    lateinit var currentSongLengthTexView: TextView
    lateinit var currentSongCurrentTimeTexView: TextView

    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongTime = 0
    private var musicLocation = ""
    private var currentSongInteger = 0

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
        currentSongTexView = this.findViewById(R.id.current_song_name)
        currentSongLengthTexView = this.findViewById(R.id.current_song_length)
        currentSongCurrentTimeTexView = this.findViewById(R.id.current_song_current_time)

        musicLocation = "/sdcard/Music/"
        listOfSongs = getPlayList(musicLocation)

        updateCurrentSongInfo()

        initUpdateCurrentSongInfoThread()

        playButton.setOnClickListener {
            if (!isPlaying) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Start playing", Toast.LENGTH_SHORT).show()
                    playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, currentSongTime)
                } else {
                    Toast.makeText(this, "Cannot be played", Toast.LENGTH_SHORT).show()
                }
            } else {
                pauseCurrentSong()
            }
        }

        previousButton.setOnClickListener {
            Toast.makeText(this, "Previous song", Toast.LENGTH_SHORT).show()
            currentSongInteger = (currentSongInteger + 1) % listOfSongs?.size!!
            mediaPlayer?.pause()
            playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, 0)
        }

        nextButton.setOnClickListener {
            Toast.makeText(this, "Next song", Toast.LENGTH_SHORT).show()
            currentSongInteger--
            if (currentSongInteger < 0) {
                currentSongInteger = listOfSongs?.size!! - 1
            }
            mediaPlayer?.pause()
            playSong(listOfSongs?.get(currentSongInteger)?.path + listOfSongs?.get(currentSongInteger)?.songName, 0)
        }
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
        currentSongTexView.text = listOfSongs?.get(currentSongInteger)?.songName
        currentSongLengthTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(listOfSongs?.get(currentSongInteger)?.length)
        currentSongCurrentTimeTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(mediaPlayer?.currentPosition?.toLong())
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

    private fun pauseCurrentSong() {
        Toast.makeText(this, "Pause song", Toast.LENGTH_SHORT).show()
        if (isPlaying) {
            isPlaying = false
            mediaPlayer?.pause()
            currentSongTime = mediaPlayer?.currentPosition!!
        }
        playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
    }

    private fun getPlayList(rootPath: String): ArrayList<SongFile>? {
        val fileList: ArrayList<SongFile> = ArrayList()
        return try {
            val rootFolder = File(rootPath)
            val files: Array<File> = rootFolder.listFiles() //here you will get NPE if directory doesn't contains  any file,handle it like this.
            for (file in files) {
                if (file.isDirectory) {
                    if (getPlayList(file.absolutePath) != null) {
                        fileList.addAll(getPlayList(file.absolutePath)!!)
                    } else {
                        break
                    }
                } else if (file.name.endsWith(".mp3")) {
                    val metaRetriever = MediaMetadataRetriever()
                    metaRetriever.setDataSource(rootPath + file.name)
                    val durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    var duration: Long = -1
                    if (durationString != null) {
                        duration = durationString.toLong()
                    }

                    val songFile = SongFile(rootPath, file.name, duration)
                    fileList.add(songFile)
                }
            }
            fileList
        } catch (e: Exception) {
            null
        }
    }
}