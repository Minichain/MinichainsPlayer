package com.example.minichainsplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    override fun onStart() {
        Log.l("SettingsActivityLog:: onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.l("SettingsActivityLog:: onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.l("SettingsActivityLog:: onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.l("SettingsActivityLog:: onStop")
        super.onStop()
    }

    private fun init() {
        Log.l("Init SettingsActivity!")
        setContentView(R.layout.settings_activity)
    }
}