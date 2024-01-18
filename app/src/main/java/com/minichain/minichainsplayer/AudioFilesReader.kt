package com.minichain.minichainsplayer

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.media3.common.MediaItem

object AudioFilesReader {

  fun getAudioFilesInFolder(context: Context, folderPath: String): List<SongData> {
    val audioFiles = mutableListOf<SongData>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.DISPLAY_NAME,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.SIZE
    )
    val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
    context.contentResolver.query(
      uri,
      projection,
      null,
      null,
      sortOrder
    )?.use { cursor ->
      val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
      val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
      val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
      while (cursor.moveToNext()) {
        val fileId = cursor.getLong(idColumn)
        val fileName = cursor.getString(nameColumn)
        val fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileId)
        val metadataRetriever = MediaMetadataRetriever().apply { setDataSource(context, fileUri) }
        audioFiles.add(
          SongData(
            uri = fileUri,
            fileName = fileName,
            length = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong(),
            mediaItem = MediaItem.fromUri(fileUri)
          )
        )
        println("AdriLog: file name: $fileName")
      }
      cursor.close()
    }
    return audioFiles
  }
}