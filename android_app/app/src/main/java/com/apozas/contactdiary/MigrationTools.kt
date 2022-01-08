package com.apozas.contactdiary

/*
    This file is part of Contact Diary.
    Contact Diary is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    Contact Diary is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with Contact Diary. If not, see <http://www.gnu.org/licenses/>.
    Copyright 2020 by Alex Pozas-Kerstjens (apozas)
*/

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.util.*

class MigrationTools {
    private val cal: Calendar = Calendar.getInstance()
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    fun migrateTo3(dataBase: SQLiteDatabase) {
        val query = "Select * from " + feedEntry.TABLE_NAME
        val cursor = dataBase.rawQuery(query, null)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val time = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIMESTAMP_COLUMN))
                cal.timeInMillis = time
                cal.set(Calendar.HOUR, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val values = ContentValues().apply {
                    put(feedEntry.TIMESTAMP_COLUMN, cal.timeInMillis)
                    put(feedEntry.DURATION_COLUMN, 60)
                }
//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(id.toString())
                dataBase.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)
            }
        }
    }

    fun migrateTo4(dataBase: SQLiteDatabase) {
        val query = "Select * from tmp_table"
        val cursor = dataBase.rawQuery(query, null)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val time = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIMESTAMP_COLUMN))
                val duration = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.DURATION_COLUMN))
                val hours = duration / 60
                val minutes = duration % 60
                cal.timeInMillis = time
                cal.add(Calendar.HOUR_OF_DAY, hours.toInt())
                cal.add(Calendar.MINUTE, minutes.toInt())

                val values = ContentValues().apply {
                    put(feedEntry.TIME_END_COLUMN, cal.timeInMillis)
                }
//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(id.toString())
                dataBase.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)
            }
        }
    }

    fun migrateTo5(dataBase: SQLiteDatabase) {
        val query = "Select * from " + feedEntry.TABLE_NAME
        val cursor = dataBase.rawQuery(query, null)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                val closeContact = cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.CLOSECONTACT_COLUMN))
                val values = ContentValues().apply {
                    put(feedEntry.CLOSECONTACT_COLUMN,
                        when (closeContact) {
                            1 -> 0
                            3 -> 1
                            else -> -1
                        }
                    )
                }
//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(id.toString())
                dataBase.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)
            }
        }
    }
}