package com.example.minichainsplayer

import android.app.ActionBar
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener


class SettingsActivity : AppCompatActivity() {
    private lateinit var musicPathEditText: EditText
    private lateinit var openFileChooserImageButton: ImageButton
    private lateinit var addMusicPathImageButton: ImageButton
    private lateinit var fillPlayListButton: Button
    private lateinit var clearPlayListButton: Button
    private lateinit var musicPathsParentLinearLayout: LinearLayout

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
        openFileChooserImageButton = this.findViewById(R.id.open_file_chooser_internal_storage)
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

        openFileChooserImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, 42)
        }

        addMusicPathImageButton.setOnClickListener {
            DataBase.setMusicPath(this, musicPathEditText.text.toString())
            updateMusicPaths()
        }

        musicPathsParentLinearLayout = this.findViewById(R.id.music_paths_parent_linear_layout)

        updateMusicPaths()
    }

    private fun updateMusicPaths() {
        musicPathsParentLinearLayout.removeAllViews()
        var musicPaths = DataBase.getMusicPaths()
        for (i in 0 until musicPaths?.size!! step 1) {
            musicPathsParentLinearLayout.addView(createPathLinearLayout(musicPaths[i]))
        }
    }

    private fun createPathLinearLayout(text: String): LinearLayout {
        var newLinearLayout = LinearLayout(this)
        newLinearLayout.orientation = LinearLayout.HORIZONTAL

        /** IMAGE BUTTON **/
        var imageButton = ImageButton(this)
        var params = ViewGroup.LayoutParams(Utils.dpToPx(36f), Utils.dpToPx(36f))
        imageButton.layoutParams = params
        imageButton.foregroundGravity = Gravity.CENTER
        imageButton.background = ContextCompat.getDrawable(this, R.drawable.baseline_delete_white_36)

        imageButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.music_path_removed), Toast.LENGTH_SHORT).show()
            DataBase.deleteMusicPath(text)
            updateMusicPaths()
        }

        /** TEXT VIEW **/
        var textView = TextView(this)
        params = ViewGroup.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, Utils.dpToPx(36f))
        textView.layoutParams = params
        textView.setTextColor(Color.parseColor("#A8A8A8"))
        textView.text = text
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        textView.maxLines = 1

        newLinearLayout.addView(imageButton)
        newLinearLayout.addView(textView)

        return newLinearLayout
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (data?.data != null) {
                val uri: Uri? = data.data
                val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                if (docUri != null) {
                    val path = MyFileUtil.getPath(this, docUri)
                    if (path != null) {
                        musicPathEditText.setText(path)
                    }
                }
            }
        }
    }
}