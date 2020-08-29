package com.apozas.contactdiary

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(ContactDatabase.SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(ContactDatabase.SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "ContactDiary.db"


    }

    fun viewData(onlyRisky: Boolean): Cursor {
        val db = this.readableDatabase
        val query: String

        if (onlyRisky) {
            query = "Select * from " + ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME +
                    " WHERE " + ContactDatabase.ContactDatabase.FeedEntry.CLOSECONTACT_COLUMN + ">=1" +
                    " ORDER BY " + ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN + " DESC"
        } else {
            query = "Select * from " + ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME +
                    " ORDER BY " + ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN + " DESC"
        }

        val cursor = db.rawQuery(query, null)

        return cursor
    }

}
