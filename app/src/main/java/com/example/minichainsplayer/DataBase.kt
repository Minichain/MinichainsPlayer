package com.example.minichainsplayer

import android.content.ContentValues
import com.example.minichainsplayer.FeedReaderContract.FeedEntry.COLUMN_FORMAT
import com.example.minichainsplayer.FeedReaderContract.FeedEntry.COLUMN_LENGTH
import com.example.minichainsplayer.FeedReaderContract.FeedEntry.COLUMN_PATH
import com.example.minichainsplayer.FeedReaderContract.FeedEntry.COLUMN_SONG
import com.example.minichainsplayer.FeedReaderContract.FeedEntry.TABLE_NAME

class DataBase {
    companion object {
        lateinit var dataBaseHelper: FeedReaderDbHelper

        fun insertOrUpdateSongInDataBase(rootPath: String, fileName: String, fileFormat: String) {
            var newFileName = fileName
            if (fileName.contains("'")) {
                newFileName = fileName.replace("'", "_")
            }
            try {
                val dataBase = dataBaseHelper.writableDatabase

                if (dataBase != null) {
                    val values = ContentValues().apply {
                        put(COLUMN_PATH, rootPath)
                        put(COLUMN_SONG, newFileName)
                        put(COLUMN_FORMAT, fileFormat)
                        put(COLUMN_LENGTH, -1)
                    }

                    if (!isSongInDataBase(newFileName)) {
                        dataBase?.insert(TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Song '$newFileName' inserted into the database.")
                    } else {
                        dataBase?.update(TABLE_NAME, values, COLUMN_SONG + " = '" + newFileName + "'", null)
                        Log.l("DataBaseLog: Song '$newFileName' is already in the database. Updating it.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting song '$newFileName' into database.")
            }
        }

        fun isSongInDataBase(songName: String): Boolean {
            Log.l("isSongInDataBase: songName: " + songName)
            try {
                val dataBase = dataBaseHelper.writableDatabase
                val cursor = dataBase.rawQuery( "SELECT COUNT(${COLUMN_SONG}) " +
                        "FROM ${TABLE_NAME} WHERE ${COLUMN_SONG} = '$songName'", null);
                cursor.moveToFirst()
                if (cursor.getInt(0) != 0) {
                    cursor.close()
                    return true
                }
            } catch (e: Exception) {
                return false
            }
            return false
        }

        fun getListOfSongs(): Array<String?> {
            val arrayListOfSongs = arrayOfNulls<String>(getNumberOfSongs())
            val dataBase = dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery("SELECT * FROM ${TABLE_NAME} ORDER BY ${COLUMN_SONG} ASC", null)
            if (cursor.moveToFirst()) {
                var i = 0;
                while (!cursor.isAfterLast) {
                    val songName = cursor.getString(cursor.getColumnIndex("song")).replace("_", "'")
                    arrayListOfSongs[i] = songName
                    cursor.moveToNext()
                    i++;
                }
            }
            cursor.close()
            return arrayListOfSongs;
        }

        fun getNumberOfSongs(): Int {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                val cursor = dataBase.rawQuery("SELECT COUNT(*) FROM ${TABLE_NAME}", null)
                cursor.moveToFirst()
                val count = cursor.getInt(0);
                cursor.close()
                return count
            } catch (e: Exception) {
                return -1
            }
        }
    }
}