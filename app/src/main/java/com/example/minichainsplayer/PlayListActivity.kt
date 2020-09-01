package com.example.minichainsplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.size

class PlayListActivity : AppCompatActivity() {
    private lateinit var playListBroadcastReceiver: PlayListActivityBroadcastReceiver
    private lateinit var playListView: ListView
    private var currentSongInteger = -1

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
        try {
            unregisterReceiver(playListBroadcastReceiver)
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun init() {
        setContentView(R.layout.play_list_activity)

        registerPlayListActivityBroadcastReceiver()

        playListView = this.findViewById(R.id.play_list_view)

        val arrayListOfSongs = DataBase.getListOfSongs()
        var arrayAdapter = ArrayAdapter(this, R.layout.play_list_layout, arrayListOfSongs)
        playListView.adapter = arrayAdapter

        playListView.setOnItemClickListener { adapterView, view, i, l ->
            var bundle = Bundle()
            bundle.putString("currentSongName", arrayListOfSongs[i])
            bundle.putInt("currentSongInteger", i)
            updateCurrentSongInteger(i)
            sendBroadcastToService(BroadcastMessage.START_PLAYING_SONG, bundle)
        }

        playListView.post(Runnable {
            playListView.setSelectionFromTop(currentSongInteger, playListView.height / 2)
            updateCurrentSongInteger(intent.getIntExtra("CURRENT_SONG_INTEGER", -1))
        })
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
                            updateCurrentSongInteger(extras.getInt("currentSongInteger"))
                        }
                    } else if (broadcast == BroadcastMessage.UPDATE_ACTIVITY_VARIABLES_02.toString()) {
                        if (extras != null) {
                        }
                    } else {
                    }
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateCurrentSongInteger(newInteger: Int) {
        if (currentSongInteger != newInteger) {
            currentSongInteger = newInteger
            for (i in 0 until (playListView.size - 1) step 1) {
                playListView[i].background = getDrawable(R.color.colorPrimaryDark)
            }
            getViewByPosition(currentSongInteger, playListView)?.background = getDrawable(R.color.grey)
        }
    }

    private fun getViewByPosition(pos: Int, listView: ListView): View? {
        val firstListItemPosition = listView.firstVisiblePosition
        val lastListItemPosition = firstListItemPosition + listView.childCount - 1
        return if (pos < firstListItemPosition || pos > lastListItemPosition) {
            listView.adapter.getView(pos, null, listView)
        } else {
            val childIndex = pos - firstListItemPosition
            listView.getChildAt(childIndex)
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