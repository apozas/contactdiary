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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_editevent.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {


    val dbHelper = FeedReaderDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editevent)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get info from MainActivity
        val info = getIntent().getExtras()?.getString("entry")
//        Toast.makeText(this, "Index is " + info, Toast.LENGTH_LONG).show()

        val db = dbHelper.writableDatabase

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        eventname_edit.setText(cursor.getString(cursor.getColumnIndex("Name")))
        eventplace_edit.setText(cursor.getString(cursor.getColumnIndex("Place")))

        val timestamp = cursor.getLong(cursor.getColumnIndex("Timestamp"))
        var cal = Calendar.getInstance()
        cal.setTimeInMillis(timestamp)

        eventdate_edit.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(cal.time))
        eventtime_edit.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))
        if (cursor.getString(cursor.getColumnIndex("Companions")) != ""){
            eventpeople_edit.setText(cursor.getString(cursor.getColumnIndex("Companions")))
        }
        if (cursor.getString(cursor.getColumnIndex("Phone")) != ""){
            eventphone_edit.setText(cursor.getString(cursor.getColumnIndex("Phone")))
        }

        var encounterBtn = cursor.getInt(cursor.getColumnIndex("EncounterType"))
        var closeContactBtn = cursor.getInt(cursor.getColumnIndex("CloseContact"))

        if (encounterBtn == 0) {
            event_indoors.setChecked(true)
        } else if (encounterBtn == 1) {
            event_outdoors.setChecked(true)
        }

        if (closeContactBtn == 0) {
            closeevent_yes.setChecked(true)
        } else if (closeContactBtn == 1) {
            closeevent_no.setChecked(true)
        } else if (closeContactBtn == 2) {
            closeevent_maybe.setChecked(true)
        }

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            eventdate_edit.setText(DateFormat.getDateInstance().format(cal.time))

        }

        eventdate_edit.setOnClickListener {
            DatePickerDialog(
                this@EditEventActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)

            eventtime_edit.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))

        }

        eventtime_edit.setOnClickListener {
            TimePickerDialog(
                this@EditEventActivity, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        okButton_AddEvent.setOnClickListener {
//          Gets the data repository in write mode
            val db = dbHelper.writableDatabase

//          Process RadioButtons
            val contactIndoorOutdoorId = event_indoor_outdoor.checkedRadioButtonId
            var contactIndoorOutdoorChoice = -1
            if (contactIndoorOutdoorId != -1) {
                val btn: View = event_indoor_outdoor.findViewById(contactIndoorOutdoorId)
                contactIndoorOutdoorChoice = event_indoor_outdoor.indexOfChild(btn)
            }

            val contactCloseContactId = eventclosecontact.checkedRadioButtonId
            var contactCloseContactChoice = -1
            if (contactCloseContactId != -1) {
                val btn: View = eventclosecontact.findViewById(contactCloseContactId)
                contactCloseContactChoice = eventclosecontact.indexOfChild(btn)
            }

//          Compulsory text fields
            var errorCount = 0
            val eventName = eventname_edit.getText().toString()
            if (eventName.length == 0) {
                eventname_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val eventPlace = eventplace_edit.getText().toString()
            if (eventPlace.length == 0) {
                eventplace_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
//          Create new row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(ContactDatabase.ContactDatabase.FeedEntry.TYPE_COLUMN, "Event")
                    put(ContactDatabase.ContactDatabase.FeedEntry.NAME_COLUMN, eventName)
                    put(ContactDatabase.ContactDatabase.FeedEntry.PLACE_COLUMN, eventPlace)
                    put(ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN, cal.timeInMillis)
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.PHONE_COLUMN,
                        eventphone_edit.getText().toString()
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.COMPANIONS_COLUMN,
                        eventpeople_edit.getText().toString()
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.CLOSECONTACT_COLUMN,
                        contactCloseContactChoice
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.ENCOUNTER_COLUMN,
                        contactIndoorOutdoorChoice
                    )
                }

//          Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(info.toString())
                val count = db.update(
                    ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs
                )

                Toast.makeText(
                    applicationContext,
                    applicationContext.getResources().getString(R.string.contact_saved),
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    fun deleteEvent(view: View) {
        val db = dbHelper.writableDatabase
        val info = getIntent().getExtras()?.getString("entry")
        db.delete(ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME,
            "_id LIKE ?",
            arrayOf(info)
        )

        Toast.makeText(
            applicationContext,
            applicationContext.getResources().getString(R.string.contact_deleted),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}
