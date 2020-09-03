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
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_addevent.*
import java.text.DateFormat
import java.util.*

class NewEventActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addevent)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupUI(findViewById(R.id.neweventlayout))

        var cal = Calendar.getInstance()

        // Set current values
        eventdate_input.setText(DateFormat.getDateInstance().format(cal.time))

        // Listen to new values
        val eventdateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            eventdate_input.setText(DateFormat.getDateInstance().format(cal.time))

        }

        eventdate_input.setOnClickListener {
            DatePickerDialog(
                this@NewEventActivity, eventdateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val eventtimeSetListener = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)

            eventtime_input.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))

        }

        eventtime_input.setOnClickListener {
            TimePickerDialog(
                this@NewEventActivity, eventtimeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

//      Database operation
        val dbHelper = FeedReaderDbHelper(this)

        okButton_AddEvent.setOnClickListener {
//          Gets the data repository in write mode
            val db = dbHelper.writableDatabase

            var errorCount = 0

//          Process RadioButtons
            val eventIndoorOutdoorId = event_indoor_outdoor.checkedRadioButtonId
            var eventIndoorOutdoorChoice = -1
            if (eventIndoorOutdoorId != -1) {
                val btn: View = event_indoor_outdoor.findViewById(eventIndoorOutdoorId)
                eventIndoorOutdoorChoice = event_indoor_outdoor.indexOfChild(btn)
            } else {
                event_encounter_question.error = getString(R.string.choose_option)
                errorCount++
            }

            val eventCloseContactId = eventclosecontact.checkedRadioButtonId
            var eventCloseContactChoice = -1
            if (eventCloseContactId != -1) {
                val btn: View = eventclosecontact.findViewById(eventCloseContactId)
                eventCloseContactChoice = eventclosecontact.indexOfChild(btn)
            } else {
                closecontact_question.error = getString(R.string.choose_option)
                errorCount++
            }

//          Compulsory text fields
            val eventName = eventname_input.getText().toString()
            if (eventName.length == 0) {
                eventname_input.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val eventPlace = eventplace_input.getText().toString()
            if (eventPlace.length == 0) {
                eventplace_input.error = getString(R.string.compulsory_field)
                errorCount++
            }

//          Handle time field
            if (eventtime_input.text.toString() == "") {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

//          Create a new map of values, where column names are the keys
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(ContactDatabase.ContactDatabase.FeedEntry.TYPE_COLUMN, "Event")
                    put(ContactDatabase.ContactDatabase.FeedEntry.NAME_COLUMN, eventName)
                    put(ContactDatabase.ContactDatabase.FeedEntry.PLACE_COLUMN, eventPlace)
                    put(ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN, cal.timeInMillis)
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.PHONE_COLUMN,
                        eventphone_input.getText().toString()
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.COMPANIONS_COLUMN,
                        eventpeople_input.getText().toString()
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.ENCOUNTER_COLUMN,
                        eventIndoorOutdoorChoice
                    )
                    put(
                        ContactDatabase.ContactDatabase.FeedEntry.CLOSECONTACT_COLUMN,
                        eventCloseContactChoice
                    )
                }

//              Insert the new row, returning the primary key value of the new row
                val newRowId =
                    db?.insert(ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME, null, values)

                Toast.makeText(
                    applicationContext,
                    applicationContext.getResources().getString(R.string.event_saved),
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    fun setupUI(view: View) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { v, event ->
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

    fun hideSoftKeyboard() {
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus?.getWindowToken(), 0)
    }
}