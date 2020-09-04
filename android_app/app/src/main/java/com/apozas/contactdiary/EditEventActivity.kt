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
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_editevent.*
import java.text.DateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {

    private val dbHelper = FeedReaderDbHelper(this)
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editevent)
        setSupportActionBar(findViewById(R.id.toolbar))

        setupUI(findViewById(R.id.editeventlayout))

        // Get info from MainActivity
        val info = intent.extras?.getString("entry")

        val db = dbHelper.writableDatabase

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        eventname_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
        eventplace_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))

        val timestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.DATETIME_COLUMN))
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        eventdate_edit.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(cal.time))
        eventtime_edit.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))
        if (cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN)) != ""){
            eventpeople_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN)))
        }
        if (cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)) != ""){
            eventphone_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)))
        }

        val encounterBtn = cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN))
        val closeContactBtn = cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN))

        if (encounterBtn == 0) {
            event_indoors.isChecked = true
        } else if (encounterBtn == 1) {
            event_outdoors.isChecked = true
        }

        when (closeContactBtn) {
            0 -> {
                closeevent_yes.isChecked = true
            }
            1 -> {
                closeevent_no.isChecked = true
            }
            2 -> {
                closeevent_maybe.isChecked = true
            }
        }

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
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

        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
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
            val eventName = eventname_edit.text.toString()
            if (eventName.isEmpty()) {
                eventname_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val eventPlace = eventplace_edit.text.toString()
            if (eventPlace.isEmpty()) {
                eventplace_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
//          Create new row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Event")
                    put(feedEntry.NAME_COLUMN, eventName)
                    put(feedEntry.PLACE_COLUMN, eventPlace)
                    put(feedEntry.DATETIME_COLUMN, cal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, eventphone_edit.text.toString())
                    put(feedEntry.COMPANIONS_COLUMN, eventpeople_edit.text.toString())
                    put(feedEntry.CLOSECONTACT_COLUMN, contactCloseContactChoice)
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                }

//          Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(info.toString())
                db.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)

                Toast.makeText(
                    applicationContext,
                    applicationContext.resources.getString(R.string.event_saved),
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
    }

    fun deleteEvent(view: View) {
        val db = dbHelper.writableDatabase
        val info = intent.extras?.getString("entry")
        db.delete(ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME,
            "_id LIKE ?",
            arrayOf(info)
        )

        Toast.makeText(
            applicationContext,
            applicationContext.resources.getString(R.string.entry_deleted),
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun setupUI(view: View) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { v, _ ->
                v.clearFocus()
                hideSoftKeyboard()
                false
            }
        }

        //If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun hideSoftKeyboard() {
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
