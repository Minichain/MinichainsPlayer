package com.example.minichainsplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var foldersDialog: MaterialDialog

    private lateinit var musicPathEditText: EditText
    private lateinit var openFileChooserInternalStorageImageButton: ImageButton
    private lateinit var openFileChooserExternalStorageImageButton: ImageButton
    private lateinit var openFileChooserExternalStorageRelativeLayout: RelativeLayout
    private lateinit var addMusicPathImageButton: ImageButton
    private lateinit var fillPlayListButton: Button
    private lateinit var clearPlayListButton: Button

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

        musicPathEditText = this.findViewById(R.id.music_path_edit_text)
        openFileChooserInternalStorageImageButton = this.findViewById(R.id.open_file_chooser_internal_storage)
        openFileChooserExternalStorageImageButton = this.findViewById(R.id.open_file_chooser_external_storage)
        openFileChooserExternalStorageRelativeLayout = this.findViewById(R.id.open_file_chooser_external_storage_relative_layout)
        addMusicPathImageButton = this.findViewById(R.id.add_music_path)
        fillPlayListButton = this.findViewById(R.id.fill_play_list_button)
        clearPlayListButton = this.findViewById(R.id.clear_play_list_button)

        fillPlayListButton.setOnClickListener {
            sendBroadcastToService(BroadcastMessage.FILL_PLAYLIST)
        }

        clearPlayListButton.setOnClickListener {
            sendBroadcastToService(BroadcastMessage.CLEAR_PLAYLIST)
        }

        musicPathEditText.setText(DataBase.getMusicPath())
        musicPathEditText.addTextChangedListener {
            DataBase.setMusicPath(musicPathEditText.text.toString())
        }


        openFileChooserInternalStorageImageButton.setOnClickListener {
            showFoldersDialog(getExternalFilesDirs(null)[0].toString())
        }

        if (getExternalFilesDirs(null).size <= 1) {
            openFileChooserExternalStorageRelativeLayout.visibility = View.GONE
        } else {
            openFileChooserExternalStorageImageButton.setOnClickListener {
                showFoldersDialog(getExternalFilesDirs(null)[1].toString())
            }
        }

        addMusicPathImageButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.music_path_added), Toast.LENGTH_LONG).show()
            DataBase.setMusicPath(musicPathEditText.text.toString())
        }
    }

    private fun showFoldersDialog(directory: String) {
        val initialDirectory = File(directory)
        MaterialDialog(this).show {
            folderChooser(context, initialDirectory = initialDirectory) {
                    dialog, folder ->
                Log.l("Folder Selected: " + folder)
                musicPathEditText.setText(folder.toString())
            }
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
}