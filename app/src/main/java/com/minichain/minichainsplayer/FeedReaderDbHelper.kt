package com.minichain.minichainsplayer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.minichain.minichainsplayer.FeedReaderContract.LasSongPlayed.LAST_SONG_PLAYED_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.MusicPathsTable.MUSIC_PATHS_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.ParametersTable.PARAMETERS_TABLE_NAME
import com.minichain.minichainsplayer.FeedReaderContract.SongListTable.SONG_LIST_TABLE_NAME

object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object SongListTable : BaseColumns {
        const val SONG_LIST_TABLE_NAME = "songList"
        const val COLUMN_PATH = "path"
        const val COLUMN_SONG = "song"
        const val COLUMN_FORMAT = "format"
        const val COLUMN_LENGTH = "length"
    }

    object MusicPathsTable : BaseColumns {
        const val MUSIC_PATHS_TABLE_NAME = "musicPaths"
        const val COLUMN_MUSIC_PATH = "musicPath"
        const val COLUMN_MUSIC_PATH_VALUE = "value"
    }

    object ParametersTable : BaseColumns {
        const val PARAMETERS_TABLE_NAME = "parameters"
        const val COLUMN_PARAMETER = "parameter"
        const val COLUMN_PARAMETER_VALUE = "value"
    }

    object LasSongPlayed : BaseColumns {
        const val LAST_SONG_PLAYED_TABLE_NAME = "lastSongPlayed"
        const val COLUMN_LAST_SONG_NAME = "lastSongName"
    }
}

val SQL_CREATE_SONG_LIST_ENTRIES = "CREATE TABLE ${SONG_LIST_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.SongListTable.COLUMN_PATH} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_SONG} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_FORMAT} TEXT," +
        "${FeedReaderContract.SongListTable.COLUMN_LENGTH} TEXT)"

val SQL_CREATE_MUSIC_PATHS_ENTRIES = "CREATE TABLE ${MUSIC_PATHS_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.MusicPathsTable.COLUMN_MUSIC_PATH} TEXT," +
        "${FeedReaderContract.MusicPathsTable.COLUMN_MUSIC_PATH_VALUE} TEXT)"

val SQL_CREATE_PARAMETERS_ENTRIES = "CREATE TABLE ${PARAMETERS_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.ParametersTable.COLUMN_PARAMETER} TEXT," +
        "${FeedReaderContract.ParametersTable.COLUMN_PARAMETER_VALUE} TEXT)"

val SQL_CREATE_LAST_SONG_ENTRIES = "CREATE TABLE ${LAST_SONG_PLAYED_TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.LasSongPlayed.COLUMN_LAST_SONG_NAME} TEXT)"

val SQL_DELETE_SONG_LIST_ENTRIES = "DROP TABLE IF EXISTS ${SONG_LIST_TABLE_NAME}"

val SQL_DELETE_MUSIC_PATHS_ENTRIES = "DROP TABLE IF EXISTS ${MUSIC_PATHS_TABLE_NAME}"

val SQL_DELETE_PARAMETERS_ENTRIES = "DROP TABLE IF EXISTS ${PARAMETERS_TABLE_NAME}"

val SQL_DELETE_LAST_SONG_ENTRIES = "DROP TABLE IF EXISTS ${LAST_SONG_PLAYED_TABLE_NAME}"

class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_SONG_LIST_ENTRIES)
        db.execSQL(SQL_CREATE_MUSIC_PATHS_ENTRIES)
        db.execSQL(SQL_CREATE_PARAMETERS_ENTRIES)
        db.execSQL(SQL_CREATE_LAST_SONG_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_SONG_LIST_ENTRIES)
        db.execSQL(SQL_DELETE_MUSIC_PATHS_ENTRIES)
        db.execSQL(SQL_DELETE_PARAMETERS_ENTRIES)
        db.execSQL(SQL_DELETE_LAST_SONG_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 4
        const val DATABASE_NAME = "MinichainsPlayer.db"
    }
}