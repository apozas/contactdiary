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
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_addevent_inside.*
import java.text.DateFormat
import java.util.*

class NewEventActivity : AppCompatActivity() {

    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addevent)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupUI(findViewById(R.id.neweventlayout))

        val cal = Calendar.getInstance()

        // Set current values
        eventdate_input.setText(DateFormat.getDateInstance().format(cal.time))

        // Listen to new values
        val eventdateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
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

        eventtime_input.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.duration_dialog, null)
            val durationText = dialogView.findViewById<EditText>(R.id.duration)
            builder.setView(dialogView)
            builder.setTitle(getString(R.string.event_duration_title))
            durationText.hint = getString(R.string.duration_instructions)
            builder.setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                val durationText = durationStringToText(durationText.text.toString())
                if (durationText.isEmpty()) {
                    Toast.makeText(this,
                        R.string.incorrect_alarm_time,
                        Toast.LENGTH_LONG).show()
                } else { eventtime_input.setText(durationText) }
            })
            builder.setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            val dialog = builder.create()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show()
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
            val eventName = eventname_input.text.toString()
            if (eventName.isEmpty()) {
                eventname_input.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val eventPlace = eventplace_input.text.toString()
            if (eventPlace.isEmpty()) {
                eventplace_input.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val durationText = eventtime_input.text.toString()
            var contactDuration = 0
            if (durationText.isEmpty()) {
                eventtime_input.error = getString(R.string.compulsory_field)
                errorCount++
            } else {
                val durationParts = durationText.split('h')
                contactDuration = durationParts[0].toInt() * 60 + durationParts[1].dropLast(1).toInt()
            }

//          Handle time field
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

//          Create a new map of values, where column names are the keys
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Event")
                    put(feedEntry.NAME_COLUMN, eventName)
                    put(feedEntry.PLACE_COLUMN, eventPlace)
                    put(feedEntry.TIMESTAMP_COLUMN, cal.timeInMillis)
                    put(feedEntry.DURATION_COLUMN, contactDuration)
                    put(feedEntry.PHONE_COLUMN, eventphone_input.text.toString())
                    put(feedEntry.COMPANIONS_COLUMN, eventpeople_input.text.toString())
                    put(feedEntry.ENCOUNTER_COLUMN, eventIndoorOutdoorChoice)
                    put(feedEntry.CLOSECONTACT_COLUMN, eventCloseContactChoice)
                    put(feedEntry.NOTES_COLUMN, eventnotes_input.text.toString())
                }

//              Insert the new row, returning the primary key value of the new row
                db?.insert(feedEntry.TABLE_NAME, null, values)

                Toast.makeText(
                    applicationContext,
                    applicationContext.resources.getString(R.string.event_saved),
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }
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

    private fun durationStringToText(durationString: String): String {
        var durationText = ""
        val durationSplit = durationString.split(":")
        if (durationSplit.size == 2) {
            if (durationSplit[1].toInt() < 60) {
                durationText = durationSplit[0].toInt().toString() + "h" +
                        durationSplit[1].toInt().toString() + "m"
            }
        } else if (durationSplit.size == 1) {
            val hours = durationString.toInt() / 60
            val minutes = durationString.toInt() % 60
            durationText = hours.toString() + "h" + minutes.toString() + "m"
        }
        return durationText
    }
}