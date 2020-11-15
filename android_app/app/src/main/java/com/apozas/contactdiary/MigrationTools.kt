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
    val cal: Calendar = Calendar.getInstance()
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    fun migrateTo3 (dataBase: SQLiteDatabase) {
        val query = "Select * from " + feedEntry.TABLE_NAME
        val cursor = dataBase.rawQuery(query, null)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex("_id"))
                val time = cursor.getLong(cursor.getColumnIndex(feedEntry.TIMESTAMP_COLUMN))
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
}