package com.example.minichainsplayer

import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import java.io.File

class SettingsActivity : AppCompatActivity() {
    lateinit var foldersDialog: MaterialDialog

    lateinit var musicPathEditText: EditText
    lateinit var openFileDialogButton: ImageButton

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
        openFileDialogButton = this.findViewById(R.id.open_file_dialog_button)

        musicPathEditText.setText(DataBase.getMusicPath())

        openFileDialogButton.setOnClickListener {
            showFoldersDialog()
        }
    }

    private fun showFoldersDialog() {
        foldersDialog = MaterialDialog(this)
        val initialFolder = File("/sdcard", "")

        foldersDialog.show {
            folderChooser(context, initialDirectory = initialFolder) {
                    dialog, folder ->
                Log.l("Folder Selected: " + folder)
                musicPathEditText.setText(folder.toString())
                DataBase.setMusicPath(folder.toString())
            }
        }
    }
}