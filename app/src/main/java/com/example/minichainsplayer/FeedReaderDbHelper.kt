package com.example.minichainsplayer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object FeedEntry : BaseColumns {
        const val TABLE_NAME = "songList"
        const val COLUMN_PATH = "path"
        const val COLUMN_SONG = "song"
        const val COLUMN_FORMAT = "format"
        const val COLUMN_LENGTH = "length"
    }
}

private val SQL_CREATE_ENTRIES = "CREATE TABLE ${FeedReaderContract.FeedEntry.TABLE_NAME} (" +
        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
        "${FeedReaderContract.FeedEntry.COLUMN_PATH} TEXT," +
        "${FeedReaderContract.FeedEntry.COLUMN_SONG} TEXT," +
        "${FeedReaderContract.FeedEntry.COLUMN_FORMAT} TEXT," +
        "${FeedReaderContract.FeedEntry.COLUMN_LENGTH} TEXT,"

private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedReaderContract.FeedEntry.TABLE_NAME}"

class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "MinichainsPlayer.db"
    }
}