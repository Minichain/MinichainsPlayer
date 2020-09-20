package com.example.minichainsplayer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.minichainsplayer.FeedReaderContract.SettingsTable.SETTINGS_TABLE_NAME
import com.example.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME

object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object SongListTable : BaseColumns {
        const val SONG_LIST_TABLE_NAME = "songList"
        const val COLUMN_PATH = "path"
        const val COLUMN_SONG = "song"
        const val COLUMN_FORMAT = "format"
        const val COLUMN_LENGTH = "length"
    }

    object SettingsTable : BaseColumns {
        const val SETTINGS_TABLE_NAME = "settings"
        const val COLUMN_SETTING = "setting"
        const val COLUMN_SETTING_VALUE = "value"
    }
}

val SQL_CREATE_SONG_LIST_ENTRIES = "CREATE TABLE ${SONG_LIST_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.SongListTable.COLUMN_PATH} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_SONG} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_FORMAT} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_LENGTH} TEXT)"

val SQL_CREATE_SETTINGS_ENTRIES = "CREATE TABLE ${SETTINGS_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.SettingsTable.COLUMN_SETTING} TEXT," +
        "${FeedReaderContract.SettingsTable.COLUMN_SETTING_VALUE} TEXT)"

val SQL_DELETE_SONG_LIST_ENTRIES = "DROP TABLE IF EXISTS ${SONG_LIST_TABLE_NAME}"

val SQL_DELETE_SETTINGS_ENTRIES = "DROP TABLE IF EXISTS ${SETTINGS_TABLE_NAME}"

class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_SONG_LIST_ENTRIES)
        db.execSQL(SQL_CREATE_SETTINGS_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_SONG_LIST_ENTRIES)
        db.execSQL(SQL_DELETE_SETTINGS_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "MinichainsPlayer.db"
    }
}