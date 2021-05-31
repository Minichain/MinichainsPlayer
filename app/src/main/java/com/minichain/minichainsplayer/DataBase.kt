package com.minichain.minichainsplayer

import android.content.ContentValues
import android.content.Context
import android.widget.Toast
import com.minichain.minichainsplayer.FeedReaderContract.LasSongPlayed.COLUMN_LAST_SONG_NAME
import com.minichain.minichainsplayer.FeedReaderContract.LasSongPlayed.LAST_SONG_PLAYED_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.MusicPathsTable.COLUMN_MUSIC_PATH
import com.minichain.minichainsplayer.FeedReaderContract.MusicPathsTable.COLUMN_MUSIC_PATH_VALUE
import com.minichain.minichainsplayer.FeedReaderContract.MusicPathsTable.MUSIC_PATHS_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.ParametersTable.COLUMN_PARAMETER
import com.minichain.minichainsplayer.FeedReaderContract.ParametersTable.COLUMN_PARAMETER_VALUE
import com.minichain.minichainsplayer.FeedReaderContract.ParametersTable.PARAMETERS_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_FORMAT
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_LENGTH
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_PATH
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.COLUMN_SONG
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME

class DataBase {
    companion object {
        lateinit var dataBaseHelper: FeedReaderDbHelper

        /** LIST OF SONGS **/

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
                        dataBase.insert(SONG_LIST_TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Song '$newFileName' inserted into the database.")
                    } else {
                        dataBase.update(SONG_LIST_TABLE_NAME, values,  "$COLUMN_SONG = '$newFileName'", null)
                        Log.l("DataBaseLog: Song '$newFileName' is already in the database. Updating it.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting song '$newFileName' into database.")
            }
        }

        private fun isSongInDataBase(songName: String): Boolean {
            Log.l("isSongInDataBase: songName: $songName")
            return isInDataBase(SONG_LIST_TABLE_NAME, COLUMN_SONG, COLUMN_SONG, "songName", songName)
        }

        fun getListOfSongs(): Array<String?> {
            val numOfSongs = getNumberOfSongs()
            if (numOfSongs <= 0) {
                return arrayOfNulls(0)
            }
            val arrayListOfSongs = arrayOfNulls<String>(numOfSongs)
            val dataBase = dataBaseHelper.readableDatabase
            val cursor = dataBase.rawQuery("SELECT * FROM $SONG_LIST_TABLE_NAME ORDER BY $COLUMN_SONG COLLATE NOCASE ASC", null)
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

        private fun getNumberOfSongs(): Int {
            val dataBase = dataBaseHelper.readableDatabase
            try {
                val cursor = dataBase.rawQuery("SELECT COUNT(*) FROM $SONG_LIST_TABLE_NAME", null)
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

        /** PARAMETERS **/

        fun setParameter(parameter: String, value: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                if (dataBase != null) {
                    val values = ContentValues().apply {
                        put(COLUMN_PARAMETER, parameter)
                        put(COLUMN_PARAMETER_VALUE, value)
                    }

                    if (!isInDataBase(PARAMETERS_TABLE_NAME, COLUMN_PARAMETER, parameter)) {
                        dataBase.insert(PARAMETERS_TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Parameter: '$parameter', with Value: '$value' inserted into the database.")
                    } else {
                        dataBase.update(PARAMETERS_TABLE_NAME, values, "$COLUMN_PARAMETER = '$parameter'", null)
                        Log.l("DataBaseLog: Parameter: '$parameter', with Value: '$value' updated into the database.")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting Parameter: '$parameter', with Value: '$value' into database.")
            }
        }

        fun getParameter(parameter: String): String {
            var value = ""
            val dataBase = dataBaseHelper.readableDatabase
            val cursor = dataBase.rawQuery(
                "SELECT $COLUMN_PARAMETER_VALUE FROM $PARAMETERS_TABLE_NAME " +
                        "WHERE $COLUMN_PARAMETER = '$parameter'", null)
            Log.l("AdriHell count " + cursor.count)
            if (cursor.moveToFirst()) {
                Log.l("AdriHell")
                value = cursor.getString(0)
            }
            cursor.close()
            return value
        }

        /** MUSIC PATHS **/

        fun setMusicPath(context: Context, musicPath: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                if (dataBase != null) {
                    val values = ContentValues().apply {
                        put(COLUMN_MUSIC_PATH, "musicPath")
                        put(COLUMN_MUSIC_PATH_VALUE, musicPath)
                    }

                    if (!isInDataBase(MUSIC_PATHS_TABLE_NAME, COLUMN_MUSIC_PATH, COLUMN_MUSIC_PATH_VALUE,  "musicPath", musicPath)) {
                        dataBase.insert(MUSIC_PATHS_TABLE_NAME, null, values)
                        Log.l("DataBaseLog: Music path with Value: '$musicPath' inserted into the database.")
                        Toast.makeText(context, context.getString(R.string.music_path_added), Toast.LENGTH_SHORT).show()
                    } else {
                        dataBase.update(MUSIC_PATHS_TABLE_NAME, values, "$COLUMN_MUSIC_PATH_VALUE = '$musicPath'", null)
                        Log.l("DataBaseLog: Music path with Value: '$musicPath' updated into the database.")
                        Toast.makeText(context, context.getString(R.string.music_path_already_stored), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting music path with Value: '$musicPath' into database.")
            }
        }

        fun deleteMusicPath(musicPath: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                dataBase.delete(MUSIC_PATHS_TABLE_NAME, "$COLUMN_MUSIC_PATH_VALUE=?", arrayOf(musicPath))
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
            val dataBase = dataBaseHelper.readableDatabase
            val cursor = dataBase.rawQuery(
                "SELECT $COLUMN_MUSIC_PATH_VALUE FROM $MUSIC_PATHS_TABLE_NAME " +
                    "WHERE $COLUMN_MUSIC_PATH = 'musicPath'", null)
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

        /** LAST SONG PLAYED **/

        private fun isLastSongPlayedTableEmpty(): Boolean {
            val dataBase = dataBaseHelper.writableDatabase
            val cursor = dataBase.rawQuery("SELECT count(*) FROM $LAST_SONG_PLAYED_TABLE_NAME", null)
            cursor.moveToFirst()
            val count = cursor.getInt(0)
            cursor.close()
            return count == 0
        }

        fun setLastSongPlayed(lastSongPlayed: String) {
            val dataBase = dataBaseHelper.writableDatabase
            try {
                if (dataBase != null) {
                    val values = ContentValues().apply {
                        put(COLUMN_LAST_SONG_NAME, lastSongPlayed)
                    }

                    if (isLastSongPlayedTableEmpty()) {
                        dataBase?.insert(LAST_SONG_PLAYED_TABLE_NAME, null, values)
                    } else {
                        dataBase?.update(LAST_SONG_PLAYED_TABLE_NAME, values,  "$COLUMN_LAST_SONG_NAME IS NOT NULL", null)
                    }
                    Log.l("DataBaseLog: Last song played '$lastSongPlayed' inserted into the database.")
                }
            } catch (e: Exception) {
                Log.e("DataBaseLog: Error inserting last song played '$lastSongPlayed' into database.")
            }
        }

        fun getLastSongPlayed(): String? {
            try {
                val dataBase = dataBaseHelper.writableDatabase
                val cursor = dataBase.rawQuery("SELECT $COLUMN_LAST_SONG_NAME FROM $LAST_SONG_PLAYED_TABLE_NAME", null)
                cursor.moveToLast()
                val lastSongName = cursor.getString(0)
                cursor.close()
                return lastSongName
            } catch (e: Exception) {
                return null
            }
        }

        /** ALL TABLES **/

        private fun isInDataBase(tableName: String, columnKey: String, key: String): Boolean {
            return try {
                val dataBase = dataBaseHelper.writableDatabase
                val cursor = dataBase.rawQuery( "SELECT COUNT(${columnKey}) " +
                        "FROM $tableName WHERE $columnKey = '$key'", null)
                cursor.moveToFirst()
            } catch (e: Exception) {
                false
            }
        }

        private fun isInDataBase(tableName: String, columnKey: String, columnValue: String, key: String, value: String): Boolean {
            try {
                val dataBase = dataBaseHelper.writableDatabase
                val cursor = dataBase.rawQuery( "SELECT COUNT(${columnKey}) " +
                        "FROM $tableName WHERE $columnKey = '$key' AND $columnValue = '$value'", null)
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
    }
}