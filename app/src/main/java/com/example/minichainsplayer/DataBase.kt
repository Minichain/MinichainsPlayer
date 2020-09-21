package com.example.minichainsplayer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import com.example.minichainsplayer.FeedReaderContract.SettingsTable.COLUMN_SETTING
import com.example.minichainsplayer.FeedReaderContract.SettingsTable.COLUMN_SETTING_VALUE
import com.example.minichainsplayer.FeedReaderContract.SettingsTable.SETTINGS_TABLE_NAME
import com.example.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_FORMAT
import com.example.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_LENGTH
import com.example.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_PATH
import com.example.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_SONG
import com.example.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME

class DataBase {
    companion object {
        lateinit var dataBaseHelper: FeedReaderDbHelper

        fun insertOrUpdateSongInDataBase(rootPath: String, fileName: String, fileFormat: String) {
            var newFileName = fileName
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
                        dataBase?.insert(SONG_LIST_TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Song '$newFileName' inserted into the database.")
                    } else {
                        dataBase?.update(SONG_LIST_TABLE_NAME, values,  "$COLUMN_SONG = '$newFileName'", null)
                        Log.l("DataBaseLog: Song '$newFileName' is already in the database. Updating it.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting song '$newFileName' into database.")
            }
        }

        private fun isSongInDataBase(songName: String): Boolean {
            Log.l("isSongInDataBase: songName: $songName")
            return isInDataBase(SONG_LIST_TABLE_NAME, COLUMN_SONG, COLUMN_SONG, songName)
        }

        fun getListOfSongs(): Array<String?> {
            val numOfSongs = getNumberOfSongs()
            if (numOfSongs <= 0) {
                return arrayOfNulls(0)
            }
            val arrayListOfSongs = arrayOfNulls<String>(numOfSongs)
            val dataBase = dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery("SELECT * FROM ${SONG_LIST_TABLE_NAME} ORDER BY ${COLUMN_SONG} ASC", null)
            if (cursor.moveToFirst()) {
                var i = 0;
                while (!cursor.isAfterLast) {
                    val songName = cursor.getString(cursor.getColumnIndex("song"))
                    arrayListOfSongs[i] = songName
                    cursor.moveToNext()
                    i++;
                }
            }
            cursor.close()
            return arrayListOfSongs
        }

        fun getNumberOfSongs(): Int {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                val cursor = dataBase.rawQuery("SELECT COUNT(*) FROM ${SONG_LIST_TABLE_NAME}", null)
                cursor.moveToFirst()
                val count = cursor.getInt(0);
                cursor.close()
                return count
            } catch (e: Exception) {
                return -1
            }
        }

        fun clearSongListTable() {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                dataBase.execSQL(SQL_DELETE_SONG_LIST_ENTRIES)
                dataBase.execSQL(SQL_CREATE_SONG_LIST_ENTRIES)
            } catch (e: Exception) {

            }
        }

        fun setMusicPath(context: Context, musicPath: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                if (dataBase != null) {
                    val values = ContentValues().apply {
                        put(COLUMN_SETTING, "musicPath")
                        put(COLUMN_SETTING_VALUE, musicPath)
                    }

                    if (!isMusicPathInDataBase(musicPath)) {
                        dataBase?.insert(SETTINGS_TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Music Path '$musicPath' inserted into the database.")
                        Toast.makeText(context, context.getString(R.string.music_path_added), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.music_path_already_stored), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting music path '$musicPath' into database.")
            }
        }

        private fun isMusicPathInDataBase(musicPath: String): Boolean {
            Log.l("isMusicPathInDataBase: musicPath: $musicPath")
            return isInDataBase(SETTINGS_TABLE_NAME, COLUMN_SETTING, COLUMN_SETTING_VALUE, musicPath)
        }

        private fun isInDataBase(tableName: String, columnName: String, columnValue: String, value: String): Boolean {
            try {
                val dataBase = dataBaseHelper.writableDatabase
                val cursor = dataBase.rawQuery( "SELECT COUNT(${columnName}) " +
                        "FROM ${tableName} WHERE ${columnValue} = '$value'", null);
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

        fun deleteMusicPath(musicPath: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                dataBase.delete(SETTINGS_TABLE_NAME, "$COLUMN_SETTING_VALUE=?", arrayOf(musicPath))
                Log.l("DataBaseLog: Deleting music path '$musicPath'.")
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error deleting music path '$musicPath'.")
            }
        }

        fun getMusicPath(): String {
            return if (getMusicPaths().isNotEmpty()) {
                getMusicPaths()[0]
            } else {
                null.toString()
            }
        }

        fun getMusicPaths(): ArrayList<String> {
            var musicPath = ArrayList<String>()
            val dataBase = dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery(
                "SELECT $COLUMN_SETTING_VALUE FROM ${SETTINGS_TABLE_NAME} " +
                    "WHERE $COLUMN_SETTING = 'musicPath'", null)
            if (cursor.moveToFirst()) {
                musicPath.add(cursor.getString(0))
                var i = 0
                while (cursor.moveToNext()) {
                    i++
                    musicPath.add(cursor.getString(0))
                }
            }
            cursor.close()
            return musicPath
        }
    }
}