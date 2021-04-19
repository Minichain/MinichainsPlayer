package com.minichain.minichainsplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener

class PlayListActivity : AppCompatActivity() {
    private lateinit var playListBroadcastReceiver: PlayListActivityBroadcastReceiver

    private lateinit var playListActivity: LinearLayout
    private lateinit var playListView: ListView
    private lateinit var arrayAdapter: CustomArrayAdapter<String?>
    private lateinit var playListTextFilter: EditText
    private lateinit var playListPlayerRelativeLayout: RelativeLayout
    private lateinit var playButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var currentSongTexView: TextView
    private lateinit var currentSongTimeBarSeekBar: SeekBar
    private lateinit var playListEmptyTextView: TextView

    private lateinit var currentSong: CurrentSong

    private var listOfSongsSize: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    override fun onStart() {
        Log.l("PlayListActivityLog:: onStart")
        super.onStart()
        registerPlayListActivityBroadcastReceiver()
    }

    override fun onResume() {
        Log.l("PlayListActivityLog:: onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.l("PlayListActivityLog:: onPause")
        super.onPause()
        try {
            unregisterReceiver(playListBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("PlayListActivityLog:: error un-registering receiver $e")
        }
    }

    override fun onStop() {
        Log.l("PlayListActivityLog:: onStop")
        super.onStop()
    }

    private fun init() {
        setContentView(R.layout.play_list_activity)

        registerPlayListActivityBroadcastReceiver()

        playListActivity = this.findViewById(R.id.play_list_activity)

        playListTextFilter = this.findViewById(R.id.play_list_text_filter)

        playListView = this.findViewById(R.id.play_list_view)
        playListEmptyTextView = this.findViewById(R.id.play_list_empty_text_view)

        playListPlayerRelativeLayout = this.findViewById(R.id.play_list_player_relative_layout)
        playButton = this.findViewById(R.id.play_button_play_list)
        previousButton = this.findViewById(R.id.previous_button_play_list)
        nextButton = this.findViewById(R.id.next_button_play_list)
        currentSongTexView = this.findViewById(R.id.current_song_name_play_list)
        currentSongTexView.isSelected = true
        currentSongTimeBarSeekBar = this.findViewById(R.id.current_song_time_bar_play_list)

        currentSong = CurrentSong(0,
            intent.getIntExtra("CURRENT_SONG_LENGTH", 0),
            intent.getStringExtra("CURRENT_SONG_NAME").toString(),
            "",
            intent.getIntExtra("CURRENT_SONG_TIME", 0),
            intent.getBooleanExtra("PLAYING", false))

        val arrayListOfSongs = DataBase.getListOfSongs()
        if (arrayListOfSongs.isNullOrEmpty() || arrayListOfSongs[0] == null) {
            playListEmptyTextView.visibility = View.VISIBLE
            playListView.visibility = View.GONE
        } else {
            playListEmptyTextView.visibility = View.GONE
            arrayAdapter = CustomArrayAdapter(this, R.layout.play_list_layout, arrayListOfSongs)
            playListView.adapter = arrayAdapter

            playListView.setOnItemClickListener { _, _, i, _ ->
                val bundle = Bundle()
                val currentSongName = playListView.adapter.getItem(i).toString()
                bundle.putString("currentSongName", currentSongName)
                sendBroadcastToService(BroadcastMessage.START_PLAYING_SONG, bundle)
            }

            playListView.post(Runnable {
                updateCurrentSongInteger(currentSong.currentSongName, arrayListOfSongs.indexOf(currentSong.currentSongName))
                playListView.setSelectionFromTop(currentSong.currentSongInteger, playListView.height / 2)
            })

            playListView.setOnScrollChangeListener { _, _, _, _, _ ->
                updateListView()
            }

            playListTextFilter.addTextChangedListener {
                arrayAdapter.filter.filter(playListTextFilter.text.toString())
                playListView.adapter = arrayAdapter
                updateListView()
            }

            playButton.setOnClickListener {
                if (currentSong.currentSongName != "") {
                    if (!currentSong.playing) {
                        sendBroadcastToService(BroadcastMessage.START_PLAYING)
                        playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
                    } else {
                        sendBroadcastToService(BroadcastMessage.STOP_PLAYING)
                        playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_pause_white_48)
                    }
                }
            }

            previousButton.setOnClickListener {
                sendBroadcastToService(BroadcastMessage.PREVIOUS_SONG)
            }

            nextButton.setOnClickListener {
                sendBroadcastToService(BroadcastMessage.NEXT_SONG)
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

            playListActivity.addOnLayoutChangeListener { view, i, i2, i3, i4, i5, i6, i7, i8 ->
//            Log.l("OnLayoutChangeListener. bottom: " + i4 + ", oldBottom: " + i8)
                if (i4 < i8) {
                    playListPlayerRelativeLayout.layoutParams = LinearLayout.LayoutParams(
                        RelativeLayout.LayoutParams.FILL_PARENT, 0, 0f
                    )
                } else if (i4 > i8) {
                    playListPlayerRelativeLayout.layoutParams = LinearLayout.LayoutParams(
                        RelativeLayout.LayoutParams.FILL_PARENT, 0, 0.25f
                    )
                } else {
                    //Size did not change
                }
            }

            updateViews()
        }
    }

    private fun updateViews() {
        if (currentSongTexView.text != currentSong.currentSongName) {
            currentSongTexView.text = currentSong.currentSongName
        }

        if (currentSong.currentSongLength > 0) {
            currentSongTimeBarSeekBar.progress = ((currentSong.currentSongTime.toFloat()  / currentSong.currentSongLength.toFloat()) * 100f).toInt()
        }

        if (currentSong.playing) {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_pause_white_48)
        } else {
            playButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_play_arrow_white_48)
        }
    }

    private fun sendBroadcastToService(broadcastMessage: BroadcastMessage) {
        sendBroadcastToService(broadcastMessage, null)
    }

    private fun sendBroadcastToService(broadcastMessage: BroadcastMessage, bundle: Bundle?) {
        Log.l("SettingsActivityLog:: sending broadcast $broadcastMessage")
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

    inner class PlayListActivityBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val broadcast = intent.action
                val extras = intent.extras
                if (broadcast != null) {
                    if (broadcast == BroadcastMessage.START_PLAYING.toString()) {
                    } else if (broadcast == BroadcastMessage.STOP_PLAYING.toString()) {
                    } else if (broadcast == BroadcastMessage.START_STOP_PLAYING_NOTIFICATION.toString()) {
                    } else if (broadcast == BroadcastMessage.PREVIOUS_SONG.toString()) {
                    } else if (broadcast == BroadcastMessage.NEXT_SONG.toString()) {
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_01.toString()) {
                        if (extras != null) {
                            currentSong.playing = extras.getBoolean("playing")
                            updateCurrentSongInteger(extras.getString("currentSongName").toString(), extras.getInt("currentSongInteger"))
                            currentSong.currentSongName = extras.getString("currentSongName").toString()
                            currentSong.currentSongLength = extras.getInt("currentSongLength")
                            listOfSongsSize = extras.getInt("listOfSongsSize")
                        }
                        updateViews()
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_02.toString()) {
                        if (extras != null) {
                            currentSong.currentSongTime = extras.getInt("currentSongTime")
                        }
                        updateViews()
                    } else {
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateCurrentSongInteger(currentSongName: String, currentSongInteger: Int) {
        currentSong.currentSongName = currentSongName
        currentSong.currentSongInteger = currentSongInteger
        updateListView()
    }

    private fun updateListView() {
        for (i in 0 until playListView.size step 1) {
            if (playListView.adapter.getItem(playListView.firstVisiblePosition + i) == currentSong.currentSongName) {
                playListView[i].background = getDrawable(R.color.color_02)
            } else {
                playListView[i].background = getDrawable(R.color.color_04)
            }
        }
    }

    private fun registerPlayListActivityBroadcastReceiver() {
        playListBroadcastReceiver = PlayListActivityBroadcastReceiver()
        try {
            val intentFilter = IntentFilter()
            for (i in BroadcastMessage.values().indices) {
                intentFilter.addAction(BroadcastMessage.values()[i].toString())
            }
            registerReceiver(playListBroadcastReceiver, intentFilter)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }
}