package com.minichain.minichainsplayer

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
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.FILL_PARENT
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.system.exitProcess

class MinichainsPlayerActivity : AppCompatActivity() {
    private lateinit var minichainsPlayerBroadcastReceiver: MinichainsPlayerActivityBroadcastReceiver

    private var gLView: MyGLSurfaceView? = null
    private lateinit var playFloatingButton: FloatingActionButton
    private lateinit var previousFloatingButton: FloatingActionButton
    private lateinit var nextFloatingButton: FloatingActionButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var currentSongTexView: TextView
    private lateinit var currentSongLengthTexView: TextView
    private lateinit var currentSongCurrentTimeTexView: TextView
    private lateinit var currentSongTimeBarSeekBar: SeekBar
    private lateinit var numberOfSongsTextView: TextView
    private lateinit var showPlayListImageButton: ImageButton

    private var currentSong = CurrentSong()

    private var listOfSongsSize = 0
    private var shuffle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkAndRequestPermissions()) {
            init()
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
    }

    private val REQUEST_ID_MULTIPLE_PERMISSIONS: Int = 1

    private fun checkAndRequestPermissions(): Boolean {
        val readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (readExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (recordAudioPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), REQUEST_ID_MULTIPLE_PERMISSIONS)
            return false
        }
        return true
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
            }
        }
    }

    private fun init() {
        Log.l("Init MinichainsPlayerActivity!")

        setContentView(R.layout.minichains_player_activity)
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
        val myGLSurfaceViewLayout: LinearLayout = this.findViewById(R.id.my_gl_surface_view)
        gLView = MyGLSurfaceView(this)
        myGLSurfaceViewLayout.addView(gLView, ViewGroup.LayoutParams(FILL_PARENT, FILL_PARENT))
        playFloatingButton = this.findViewById(R.id.play_button)
        previousFloatingButton = this.findViewById(R.id.previous_button)
        nextFloatingButton = this.findViewById(R.id.next_button)
        shuffleButton = this.findViewById(R.id.shuffle_button)
        currentSongTexView = this.findViewById(R.id.current_song_name)
        currentSongTexView.isSelected = true
        currentSongLengthTexView = this.findViewById(R.id.current_song_length)
        currentSongCurrentTimeTexView = this.findViewById(R.id.current_song_current_time)
        currentSongTimeBarSeekBar = this.findViewById(R.id.current_song_time_bar)
        numberOfSongsTextView = this.findViewById(R.id.number_of_songs)
        showPlayListImageButton = this.findViewById(R.id.show_play_list)

        registerMinichainsPlayerActivityBroadcastReceiver()

        playFloatingButton.setOnClickListener {
            if (currentSong.currentSongName != "") {
                if (!currentSong.playing) {
                    sendBroadcastToService(BroadcastMessage.START_PLAYING)
                    playFloatingButton.setImageResource(R.drawable.baseline_play_arrow_white_48)
                } else {
                    sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
                    playFloatingButton.setImageResource(R.drawable.baseline_pause_white_48)
                }
            }
        }

        previousFloatingButton.setOnClickListener {
            sendBroadcastToService(BroadcastMessage.PREVIOUS_SONG)
        }

        nextFloatingButton.setOnClickListener {
            sendBroadcastToService(BroadcastMessage.NEXT_SONG)
        }

        shuffleButton.setOnClickListener {
            if (shuffle) {
                Toast.makeText(this, getString(R.string.shuffle_disabled), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.shuffle_enabled), Toast.LENGTH_SHORT).show()
            }
            sendBroadcastToService(BroadcastMessage.SHUFFLE)
        }

        showPlayListImageButton.setOnClickListener {
            this.onPause()
            intent = Intent(applicationContext, PlayListActivity::class.java)
            intent.putExtra("CURRENT_SONG_INTEGER", currentSong.currentSongInteger)
            intent.putExtra("PLAYING", currentSong.playing)
            intent.putExtra("CURRENT_SONG_NAME", currentSong.currentSongName)
            intent.putExtra("CURRENT_SONG_TIME", currentSong.currentSongTime)
            intent.putExtra("CURRENT_SONG_LENGTH", currentSong.currentSongLength)
            startActivity(intent)
        }

        currentSongTimeBarSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var bundle = Bundle()
                currentSong.currentSongTime = ((currentSongTimeBarSeekBar.progress.toDouble() / 100.0) * currentSong.currentSongLength.toDouble()).toInt()
                bundle.putInt("currentSongTime", currentSong.currentSongTime)
                sendBroadcastToService(BroadcastMessage.SET_CURRENT_SONG_TIME, bundle)
            }
        })

        Log.l("Log de Tony: gLView: " + gLView)
    }

    private fun updateViews() {
        if (currentSongTexView.text != currentSong.currentSongName) {
            if (currentSong.currentSongName.isNotEmpty() && currentSong.currentSongName != "null") {
                currentSongTexView.text = currentSong.currentSongName
            } else {
                currentSongTexView.text = "- - -"
            }
        }

        currentSongCurrentTimeTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(currentSong.currentSongTime)
        currentSongLengthTexView.text = Utils.millisecondsToHoursMinutesAndSeconds(currentSong.currentSongLength)
        if (currentSong.currentSongLength > 0) {
            currentSongTimeBarSeekBar.progress = ((currentSong.currentSongTime.toFloat()  / currentSong.currentSongLength.toFloat()) * 100f).toInt()
        }

        if (currentSong.playing) {
            playFloatingButton.setImageResource(R.drawable.baseline_pause_white_48)
        } else {
            playFloatingButton.setImageResource(R.drawable.baseline_play_arrow_white_48)
        }

        if (shuffle) {
            shuffleButton.alpha = 1f
        } else {
            shuffleButton.alpha = 0.5f
        }

        if (listOfSongsSize > 0) {
            numberOfSongsTextView.text = getString(R.string.number_of_songs, listOfSongsSize.toString())
        } else {
            numberOfSongsTextView.text = getString(R.string.number_of_songs, "0")
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
            R.id.settings_option -> {
                this.onPause()
                intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
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
        Log.l("onDestroy $this")
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
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_01.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: UPDATE_ACTIVITY_VARIABLES_01")
                        if (extras != null) {
                            currentSong.playing = extras.getBoolean("playing")
                            currentSong.currentSongPath = extras.getString("currentSongPath").toString()
                            currentSong.currentSongInteger = extras.getInt("currentSongInteger")
                            currentSong.currentSongName = extras.getString("currentSongName").toString()
                            currentSong.currentSongLength = extras.getInt("currentSongLength")
                            listOfSongsSize = extras.getInt("listOfSongsSize")
                            shuffle = extras.getBoolean("shuffle")
                        }
                        updateViews()
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_02.toString()) {
                        Log.l("MinichainsPlayerActivityLog:: UPDATE_ACTIVITY_VARIABLES_02")
                        if (extras != null) {
                            currentSong.currentSongTime = extras.getInt("currentSongTime")
                        }
                        updateViews()
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_03.toString()) {
                        if (extras != null) {
                            val spectrum: FloatArray = extras.getFloatArray("spectrum")!!
                            if (spectrum.isNotEmpty() && gLView != null) {
                                gLView!!.getRenderer().setSpectrum(spectrum)
                            }
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