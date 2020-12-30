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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_editevent_inside.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {

    private val dbHelper = FeedReaderDbHelper(this)
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editevent)
        setSupportActionBar(findViewById(R.id.toolbar))

        setupUI(findViewById(R.id.editeventlayout))

//      Get info from MainActivity
        val info = intent.extras?.getString("entry")
        val db = dbHelper.writableDatabase

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        eventname_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
        eventplace_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))

        val timeFormat = SimpleDateFormat("H:mm")
        val initCal = Calendar.getInstance()
        initCal.timeInMillis = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_BEGIN_COLUMN))

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_END_COLUMN))

        eventdate_edit.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(initCal.time))

        if (!((initCal.get(Calendar.HOUR) == 0)
                    and (initCal.get(Calendar.MINUTE) == 0)
                    and (initCal.get(Calendar.SECOND) == 0)
                    and (initCal.get(Calendar.MILLISECOND) == 0))) {
            eventinittime_edit.setText(timeFormat.format(initCal.time))
        }

        if (!((endCal.get(Calendar.HOUR) == 0)
                    and (endCal.get(Calendar.MINUTE) == 0)
                    and (endCal.get(Calendar.SECOND) == 0)
                    and (endCal.get(Calendar.MILLISECOND) == 0))) {
            eventendtime_edit.setText(timeFormat.format(endCal.time))
        }

        if (cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN)) != ""){
            eventpeople_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN)))
        }
        if (cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)) != ""){
            eventphone_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)))
        }

        var encounter = cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN))
        if (encounter > 0) {
            val encounterBtn = event_indoor_outdoor.getChildAt(encounter) as RadioButton
            encounterBtn.isChecked = true
        }

        var closeContact = cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN))
        if (closeContact > 0) {
            val closeContactBtn = eventclosecontact.getChildAt(closeContact) as RadioButton
            closeContactBtn.isChecked = true
        }

        eventnotes_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NOTES_COLUMN)))

//      Close the cursor after reading it
        cursor.close()

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            initCal.set(Calendar.YEAR, year)
            initCal.set(Calendar.MONTH, monthOfYear)
            initCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            endCal.set(Calendar.YEAR, year)
            endCal.set(Calendar.MONTH, monthOfYear)
            endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            eventdate_edit.setText(DateFormat.getDateInstance().format(initCal.time))

        }

        eventdate_edit.setOnClickListener {
            DatePickerDialog(
                this@EditEventActivity, dateSetListener,
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val initTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            initCal.set(Calendar.HOUR_OF_DAY, hour)
            initCal.set(Calendar.MINUTE, minute)
            initCal.set(Calendar.MILLISECOND, 1)    // To distinguish 0:00 from empty when loading

            eventinittime_edit.setText(timeFormat.format(initCal.time))
            if (eventendtime_edit.text.isEmpty() or (endCal.timeInMillis < initCal.timeInMillis)) {
                endCal.timeInMillis = initCal.timeInMillis
                endCal.add(Calendar.MINUTE, 30)
                eventendtime_edit.setText(timeFormat.format(endCal.time))
            }
        }

        val is24Hour = android.text.format.DateFormat.is24HourFormat(applicationContext)

        eventinittime_edit.setOnClickListener {
            TimePickerDialog(
                this@EditEventActivity, initTimeSetListener,
                initCal.get(Calendar.HOUR_OF_DAY),
                initCal.get(Calendar.MINUTE),
                is24Hour
            ).show()
        }

        val endTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            endCal.set(Calendar.HOUR_OF_DAY, hour)
            endCal.set(Calendar.MINUTE, minute)
            endCal.set(Calendar.MILLISECOND, 1)    // To distinguish 0:00 from empty when loading

            if (endCal.timeInMillis < initCal.timeInMillis) {
                Toast.makeText(
                    this, R.string.incorrect_alarm_time, Toast.LENGTH_LONG
                ).show()
            } else {
                eventendtime_edit.setText(timeFormat.format(endCal.time))
            }
        }

        eventendtime_edit.setOnClickListener {
            TimePickerDialog(
                this@EditEventActivity, endTimeSetListener,
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                is24Hour
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

//          Compulsory text field
            var errorCount = 0
            val eventName = eventname_edit.text.toString()
            if (eventName.isEmpty()) {
                eventname_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            
//          Create updated row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Event")
                    put(feedEntry.NAME_COLUMN, eventName)
                    put(feedEntry.PLACE_COLUMN, eventplace_edit.text.toString())
                    put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
                    put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, eventphone_edit.text.toString())
                    put(feedEntry.COMPANIONS_COLUMN, eventpeople_edit.text.toString())
                    put(feedEntry.CLOSECONTACT_COLUMN, contactCloseContactChoice)
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                    put(feedEntry.NOTES_COLUMN, eventnotes_edit.text.toString())
                }

//              Update the database
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

    fun duplicateEvent(view: View) {
        val db = dbHelper.writableDatabase
        val info = intent.extras?.getString("entry")
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        val beginTimestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_BEGIN_COLUMN))
        val initCal = Calendar.getInstance()
        initCal.timeInMillis = beginTimestamp
        initCal.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))

        val endTimestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_END_COLUMN))
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endTimestamp
        endCal.set(Calendar.DAY_OF_YEAR, initCal.get(Calendar.DAY_OF_YEAR))

        val values = ContentValues().apply {
            put(feedEntry.TYPE_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.TYPE_COLUMN)))
            put(feedEntry.NAME_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
            put(feedEntry.PLACE_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))
            put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
            put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
            put(feedEntry.PHONE_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)))
            put(feedEntry.RELATIVE_COLUMN, cursor.getInt(cursor.getColumnIndex(feedEntry.RELATIVE_COLUMN)))
            put(feedEntry.COMPANIONS_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN)))
            put(feedEntry.CLOSECONTACT_COLUMN, cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN)))
            put(feedEntry.ENCOUNTER_COLUMN, cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN)))
            put(feedEntry.NOTES_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.NOTES_COLUMN)))
        }

        db?.insert(feedEntry.TABLE_NAME, null, values)
        cursor.close()

        Toast.makeText(
            applicationContext,
            applicationContext.resources.getString(R.string.entry_duplicated),
            Toast.LENGTH_SHORT
        ).show()
        cursor.close()
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

    fun openPopup(view: View) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView: View = inflater.inflate(R.layout.popup_window, null)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // Taps outside the popup also dismiss it

        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.showAsDropDown(help, 0, 10)

//      Dismiss the popup window when touched
        popupView.setOnTouchListener { _, _ ->
            popupWindow.dismiss()
            true
        }
    }
}
