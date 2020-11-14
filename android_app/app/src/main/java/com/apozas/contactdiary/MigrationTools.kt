package com.apozas.contactdiary

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
                val time = cursor.getLong(cursor.getColumnIndex(feedEntry.DATETIME_COLUMN))
                cal.timeInMillis = time
                cal.set(Calendar.HOUR, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val values = ContentValues().apply {
                    put(feedEntry.DATETIME_COLUMN, cal.timeInMillis)
                }
//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(id.toString())
                dataBase.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)
            }
        }
    }
}