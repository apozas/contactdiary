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
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_addevent_inside.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class NewEventActivity : AppCompatActivity() {

    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addevent)
        setSupportActionBar(findViewById(R.id.toolbar))
        setupUI(findViewById(R.id.neweventlayout))

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

        val timeFormat = SimpleDateFormat("H:mm")

//      Set current values
        eventdate_input.setText(DateFormat.getDateInstance().format(initCal.time))

//      If coming from Open With menu, set place and time if appropriate
        if ((intent.type != null) and (intent.action.equals(Intent.ACTION_SEND))) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            if (preferences.getBoolean("remindWarning", true)) {
                val checkInReminder = androidx.appcompat.app.AlertDialog.Builder(this)
                checkInReminder.setTitle(getString(R.string.checkin_title))
                    .setMessage(getString(R.string.checkin_reminder))
                    .setCancelable(true)
                    .setPositiveButton(getString(android.R.string.ok)) {_, _ -> }
                    .setNegativeButton(getString(R.string.do_not_show_again)) { _, _ ->
                    val editor = preferences.edit()
                    editor.putBoolean("remindWarning", false)
                    editor.apply()
                }
                    .create()
                    .show()
            }
            val data = getPlace(intent.getStringExtra(Intent.EXTRA_TEXT) as String)
            if (data.any {it.isNotEmpty()}) {
                eventname_input.setText(data[0])
                eventplace_input.setText(data[1])
                eventnotes_input.setText(data[2])

                val initCal = Calendar.getInstance()
                endCal.timeInMillis = initCal.timeInMillis + 60 * 60 * 1000
                eventinittime_input.setText(timeFormat.format(initCal.time))
                eventendtime_input.setText(timeFormat.format(endCal.time))
            }
        }

//      Listen to new values
        val eventdateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            initCal.set(Calendar.YEAR, year)
            initCal.set(Calendar.MONTH, monthOfYear)
            initCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            endCal.set(Calendar.YEAR, year)
            endCal.set(Calendar.MONTH, monthOfYear)
            endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            eventdate_input.setText(DateFormat.getDateInstance().format(initCal.time))

        }

        eventdate_input.setOnClickListener {
            DatePickerDialog(
                this@NewEventActivity, eventdateSetListener,
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val initTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            initCal.set(Calendar.HOUR_OF_DAY, hour)
            initCal.set(Calendar.MINUTE, minute)
            initCal.set(Calendar.MILLISECOND, 1)    // To distinguish 0:00 from empty when loading

            eventinittime_input.setText(timeFormat.format(initCal.time))
            if (eventendtime_input.text.isEmpty() or (endCal.timeInMillis < initCal.timeInMillis)) {
                endCal.timeInMillis = initCal.timeInMillis
                endCal.add(Calendar.MINUTE, 30)
                eventendtime_input.setText(timeFormat.format(endCal.time))
            }
        }

        val is24Hour = android.text.format.DateFormat.is24HourFormat(applicationContext)

        eventinittime_input.setOnClickListener {
            TimePickerDialog(
                this@NewEventActivity, initTimeSetListener,
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
                eventendtime_input.setText(timeFormat.format(endCal.time))
            }
        }

        eventendtime_input.setOnClickListener {
            TimePickerDialog(
                this@NewEventActivity, endTimeSetListener,
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                is24Hour
            ).show()
        }

        val preventionMeasures = ArrayList<String>()
        event_mitigation.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val checkedItems = BooleanArray(4) { i -> preventionMeasures.contains(
                resources.getStringArray(R.array.mitigation_values)[i]
            )}
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

            builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                var measuresTaken = getString(R.string.none)
                if (preventionMeasures.isNotEmpty()) {
                    measuresTaken = preventionMeasures.sorted().joinToString(", ")
                }
                event_mitigation.text = measuresTaken
            }
            builder.setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
            builder.create().show()
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
            }

            val maskMe = preventionMeasures.contains(getString(R.string.mitigation_mask_me_value)).compareTo(false)
            val maskOther = preventionMeasures.contains(getString(R.string.mitigation_mask_other_value)).compareTo(false)

//          Compulsory text field
            val eventName = eventname_input.text.toString()
            if (eventName.isEmpty()) {
                eventname_input.error = getString(R.string.compulsory_field)
                errorCount++
            }

//          Create a new map of values, where column names are the keys
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Event")
                    put(feedEntry.NAME_COLUMN, eventName)
                    put(feedEntry.PLACE_COLUMN, eventplace_input.text.toString())
                    put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
                    put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, eventphone_input.text.toString())
                    put(feedEntry.COMPANIONS_COLUMN, eventpeople_input.text.toString())
                    put(feedEntry.ENCOUNTER_COLUMN, eventIndoorOutdoorChoice)
                    put(
                        feedEntry.CLOSECONTACT_COLUMN,
                        if (event_mitigation.text != getString(R.string.click_to_select)) {
                            (!preventionMeasures.contains(getString(R.string.mitigation_distance_value))).compareTo(
                                false
                            )
                        } else { -1 }
                    )
                    put(feedEntry.NOTES_COLUMN, eventnotes_input.text.toString())
                    put(
                        feedEntry.MASK_COLUMN,
                        if (event_mitigation.text != getString(R.string.click_to_select)) {
                            2 * maskMe + maskOther
                        } else { -1 }
                    )
                    put(
                        feedEntry.VENTILATION_COLUMN,
                        if (event_mitigation.text != getString(R.string.click_to_select)) {
                            (preventionMeasures.contains(getString(R.string.mitigation_ventilation_value))).compareTo(
                                false
                            )
                        } else { -1 }
                    )
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
//      Set up touch listener for non-text box views to hide keyboard.
        if (!((view is EditText) or (view is FloatingActionButton))) {
            view.setOnTouchListener { v, _ ->
                v.clearFocus()
                hideSoftKeyboard()
                false
            }
        }

//      If a layout container, iterate over children and seed recursion.
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

    private fun getPlace(qrCode: String): List<String> {
        var name = ""
        var place = ""
        var notes = ""
        var postalCode = ""
        when {
            qrCode.take(12) == "UKC19TRACING" -> {
                val data = qrCode.split(":").last().split(".")[1]
                val decoded = String(Base64.decode(data, Base64.DEFAULT), charset("UTF-8"))
                val parts = decoded.drop(1).dropLast(1).split(",")
                run loop@{
                    parts.forEach {
                        when {
                            (it.split(":")[0] == "\"opn\"") -> {
                                name = it.split(":")[1].drop(1).dropLast(1)
                            }
                            (it.split(":")[0] == "\"pc\"") -> {
                                postalCode = "PC " + it.split(":")[1].drop(1).dropLast(1)
                            }
                            (it.split(":")[0] == "\"adr\"") -> {
                                place = it.split(":")[1].drop(1).dropLast(1)
                                place = place.split("\\n").joinToString(", ")
                            }
                        }
                    }
                }
                if (place == "") { place = postalCode }
                notes = getString(R.string.shared_from) + " UKC19TRACING"
            }
            qrCode.take(13) == "NZCOVIDTRACER" -> {
                val data = qrCode.split(":").last()
                val decoded = String(Base64.decode(data, Base64.DEFAULT), charset("UTF-8"))
                val parts = decoded.drop(1).dropLast(1).split(",")
                run loop@{
                    parts.forEach {
                        when {
                            (it.split(":")[0] == "\"opn\"") -> {
                                name = it.split(":")[1].drop(1).dropLast(1)
                            }
                            (it.split(":")[0] == "\"adr\"") -> {
                                place = it.split(":")[1].drop(1).dropLast(1)
                                place = place.split("\\n").joinToString(", ")
                            }
                        }
                    }
                }
                notes = getString(R.string.shared_from) + " NZCOVIDTRACER"
            }
            else -> {
                Toast.makeText(this, getString(R.string.qr_error), Toast.LENGTH_LONG).show()
            }
        }
        return listOf(name, place, notes)
    }
}