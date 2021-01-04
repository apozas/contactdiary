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

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_addcontact_inside.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NewContactActivity : AppCompatActivity() {

    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addcontact)
        setSupportActionBar(findViewById(R.id.toolbar))

        setupUI(findViewById(R.id.newcontactlayout))

        val initCal = Calendar.getInstance()
        initCal.set(Calendar.HOUR_OF_DAY, 0)
        initCal.set(Calendar.MINUTE, 0)
        initCal.set(Calendar.SECOND, 0)
        initCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance()
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        // Set current values
        date_input.setText(DateFormat.getDateInstance().format(initCal.time))

        val timeFormat = SimpleDateFormat("H:mm")

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            initCal.set(Calendar.YEAR, year)
            initCal.set(Calendar.MONTH, monthOfYear)
            initCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            endCal.set(Calendar.YEAR, year)
            endCal.set(Calendar.MONTH, monthOfYear)
            endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            date_input.setText(DateFormat.getDateInstance().format(initCal.time))
        }

        date_input.setOnClickListener {
            DatePickerDialog(
                this@NewContactActivity, dateSetListener,
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val initTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            initCal.set(Calendar.HOUR_OF_DAY, hour)
            initCal.set(Calendar.MINUTE, minute)
            initCal.set(Calendar.MILLISECOND, 1)    // To distinguish 0:00 from empty when loading

            inittime_input.setText(timeFormat.format(initCal.time))
            if (endtime_input.text.isEmpty() or (endCal.timeInMillis < initCal.timeInMillis)) {
                endCal.timeInMillis = initCal.timeInMillis
                endCal.add(Calendar.MINUTE, 30)
                endtime_input.setText(timeFormat.format(endCal.time))
            }
        }

        val is24Hour = is24HourFormat(applicationContext)

        inittime_input.setOnClickListener {
            TimePickerDialog(
                this@NewContactActivity, initTimeSetListener,
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
                endtime_input.setText(timeFormat.format(endCal.time))
            }
        }

        endtime_input.setOnClickListener {
            TimePickerDialog(
                this@NewContactActivity, endTimeSetListener,
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                is24Hour
            ).show()
        }

        val preventionMeasures = ArrayList<String>()
        mitigation.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val checkedItems = BooleanArray(4) {i -> preventionMeasures.contains(resources.getStringArray(R.array.mitigation_values)[i])}
            builder.setTitle(getString(R.string.mitigation_title))
            builder.setMultiChoiceItems(R.array.mitigation_entries, checkedItems
            ) { _, which, isChecked ->
                val measures = this.resources.getStringArray(R.array.mitigation_values)
                if (isChecked) {
                    preventionMeasures.add(measures[which])
                } else if (preventionMeasures.contains(measures[which])) {
                    preventionMeasures.remove(measures[which])
                }
            }

            builder.setPositiveButton("OK") { _, _ ->
                var measuresTaken = getString(R.string.none)
                if (preventionMeasures.isNotEmpty()) {
                    measuresTaken = preventionMeasures.sorted().joinToString(", ")
                }
                mitigation.text = measuresTaken
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.create().show()
        }

//      Database operation
        val dbHelper = FeedReaderDbHelper(this)

        okButton_AddContact.setOnClickListener {
//          Gets the data repository in write mode
            val db = dbHelper.writableDatabase
            var errorCount = 0

//          Process RadioButtons
            val relativeId = known_group.checkedRadioButtonId
            var relativeChoice = -1
            if (relativeId != -1) {
                val btn: View = known_group.findViewById(relativeId)
                relativeChoice = known_group.indexOfChild(btn)
            }

            val contactIndoorOutdoorId = contact_indoor_outdoor.checkedRadioButtonId
            var contactIndoorOutdoorChoice = -1
            if (contactIndoorOutdoorId != -1) {
                val btn: View = contact_indoor_outdoor.findViewById(contactIndoorOutdoorId)
                contactIndoorOutdoorChoice = contact_indoor_outdoor.indexOfChild(btn)
            }

            val maskMe = preventionMeasures.contains(getString(R.string.mitigation_mask_me_value)).compareTo(false)
            val maskOther = preventionMeasures.contains(getString(R.string.mitigation_mask_other_value)).compareTo(false)

//          Compulsory text field
            val contactName = name_input.text.toString()
            if (contactName.isEmpty()) {
                name_input.error = getString(R.string.compulsory_field)
                errorCount++
            }

//          Create a new map of values, where column names are the keys
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Contact")
                    put(feedEntry.NAME_COLUMN, contactName)
                    put(feedEntry.PLACE_COLUMN, place_input.text.toString())
                    put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
                    put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, phone_input.text.toString())
                    put(feedEntry.RELATIVE_COLUMN, relativeChoice)
                    put(feedEntry.CLOSECONTACT_COLUMN,
                        (!preventionMeasures.contains(getString(R.string.mitigation_distance_value))).compareTo(false))
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                    put(feedEntry.NOTES_COLUMN, notes_input.text.toString())
                    put(feedEntry.MASK_COLUMN, 2*maskMe + maskOther)
                    put(feedEntry.VENTILATION_COLUMN,
                        (preventionMeasures.contains(getString(R.string.mitigation_ventilation_value))).compareTo(false))
                }

//              Insert the new row, returning the primary key value of the new row
                db?.insert(feedEntry.TABLE_NAME, null, values)

                Toast.makeText(
                    applicationContext,
                    applicationContext.resources.getString(R.string.contact_saved),
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
