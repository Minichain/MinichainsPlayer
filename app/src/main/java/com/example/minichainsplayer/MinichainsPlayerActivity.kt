package com.example.minichainsplayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    lateinit var currentSongTimeBarSeekBar: SeekBar
    lateinit var currentSongIntegerTextView: TextView
    lateinit var fillPlayListImageButton: ImageButton
    lateinit var appVersionNumberTextView: TextView

    private var playing = false
    private var currentSongTime: Int = 0
    private var currentSongPath = ""
    private var currentSongName = ""
    private var currentSongInteger = 0
    private var currentSongLength: Long = 0
    private var listOfSongsSize = 0
    private var shuffle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 1) //Check the requestCode later
        } else {
            init()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.l("onRequestPermissionsResult, requestCode: $requestCode, permissions: $permissions, grantResults: $grantResults")
        when (requestCode) {
            1 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {
                    closeApp()
                }
            } else -> {

            }
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
        Log.l("Init Activity!")

        setContentView(R.layout.activity_main)
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
        currentSongTimeBarSeekBar = this.findViewById(R.id.current_song_time_bar)
        currentSongIntegerTextView = this.findViewById(R.id.current_song_integer)
        fillPlayListImageButton = this.findViewById(R.id.fill_play_list)
        appVersionNumberTextView = this.findViewById(R.id.app_version_number)

        try {
            val pInfo = this.packageManager.getPackageInfo(packageName, 0)
            val version = "v" + pInfo.versionName
            appVersionNumberTextView.text = version
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        initUpdateViewsThread()

        registerMinichainsPlayerActivityBroadcastReceiver()

        playButton.setOnClickListener {
            if (currentSongName != null && currentSongName != "") {
                if (!playing) {
                    Toast.makeText(this, "Playing Song", Toast.LENGTH_SHORT).show()
                    sendBroadcastToService(BroadcastMessage.START_PLAYING)
                } else {
                    Toast.makeText(this, "Pausing Song", Toast.LENGTH_SHORT).show()
                    sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
                }
            }
        }

        previousButton.setOnClickListener {
            Toast.makeText(this, "Playing previous song", Toast.LENGTH_SHORT).show()
            sendBroadcastToService(BroadcastMessage.PREVIOUS_SONG)
        }

        nextButton.setOnClickListener {
            Toast.makeText(this, "Playing next song", Toast.LENGTH_SHORT).show()
            sendBroadcastToService(BroadcastMessage.NEXT_SONG)
        }

        shuffleButton.setOnClickListener {
            if (shuffle) {
                Toast.makeText(this, "Shuffle disabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Shuffle enabled", Toast.LENGTH_SHORT).show()
            }
            sendBroadcastToService(BroadcastMessage.SHUFFLE)
        }

        fillPlayListImageButton.setOnClickListener {
            Toast.makeText(this, "Filling playlist with songs...", Toast.LENGTH_LONG).show()
            sendBroadcastToService(BroadcastMessage.FILL_PLAYLIST)
        }

        currentSongTimeBarSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var bundle = Bundle()
                currentSongTime = ((currentSongTimeBarSeekBar.progress.toDouble() / 100.0) * currentSongLength.toDouble()).toInt()
                bundle.putInt("currentSongTime", currentSongTime)
                sendBroadcastToService(BroadcastMessage.SET_CURRENT_SONG_TIME, bundle)
            }
        })
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
        if (currentSongTexView.text != currentSongName) {
            currentSongTexView.text = currentSongName
        }

        currentSongCurrentTimeTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(currentSongTime.toLong())
        currentSongLengthTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(currentSongLength)
        if (currentSongLength > 0) {
            currentSongTimeBarSeekBar.progress = ((currentSongTime.toFloat()  / currentSongLength.toFloat()) * 100f).toInt()
        }

        if (playing) {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_pause_white_48)
        } else {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
        }

        if (shuffle) {
            shuffleButton.alpha = 1f
        } else {
            shuffleButton.alpha = 0.5f
        }

        currentSongIntegerTextView.text = String().plus(currentSongInteger.plus(1).toString()).plus("/").plus(listOfSongsSize.toString())
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
                            currentSongPath = extras.getString("currentSongPath").toString()
                            currentSongInteger = extras.getInt("currentSongInteger")
                            currentSongName = extras.getString("currentSongName").toString()
                            currentSongLength = extras.getLong("currentSongLength")
                            listOfSongsSize = extras.getInt("listOfSongsSize")
                            shuffle = extras.getBoolean("shuffle")
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