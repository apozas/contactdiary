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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.apozas.contactdiary.databinding.ActivityAddeventInsideBinding
import com.apozas.contactdiary.databinding.ActivityEditeventBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {

    private val dbHelper = FeedReaderDbHelper(this)
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry
    private lateinit var binding: ActivityEditeventBinding
    private lateinit var elements: ActivityAddeventInsideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditeventBinding.inflate(layoutInflater)
        elements = binding.activityEditeventInside
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        setupUI(findViewById(R.id.editeventlayout))

//      Show top-right shortcuts
        elements.eventTopDuplicateBtn.visibility = View.VISIBLE
        elements.eventTopDeleteBtn.visibility = View.VISIBLE

//      Get info from MainActivity
        val info = intent.extras?.getString("entry")
        val db = dbHelper.writableDatabase

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        elements.eventnameInput.setText(cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.NAME_COLUMN)))
        elements.eventplaceInput.setText(cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.PLACE_COLUMN)))

        val timeFormat = SimpleDateFormat("H:mm")
        val initCal = Calendar.getInstance()
        initCal.timeInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIME_BEGIN_COLUMN))

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIME_END_COLUMN))

        elements.eventdateInput.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(initCal.time))

        if (!((initCal.get(Calendar.HOUR) == 0)
                    and (initCal.get(Calendar.MINUTE) == 0)
                    and (initCal.get(Calendar.SECOND) == 0)
                    and (initCal.get(Calendar.MILLISECOND) == 0))) {
            elements.eventinittimeInput.setText(timeFormat.format(initCal.time))
        }

        elements.endeventdateInput.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(endCal.time))

        if (!((endCal.get(Calendar.HOUR) == 0)
                    and (endCal.get(Calendar.MINUTE) == 0)
                    and (endCal.get(Calendar.SECOND) == 0)
                    and (endCal.get(Calendar.MILLISECOND) == 0))) {
            elements.eventendtimeInput.setText(timeFormat.format(endCal.time))
        }

        if (cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.COMPANIONS_COLUMN)) != ""){
            elements.eventpeopleInput.setText(cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.COMPANIONS_COLUMN)))
        }
        if (cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.PHONE_COLUMN)) != ""){
            elements.eventphoneInput.setText(cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.PHONE_COLUMN)))
        }

        val encounter = cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.ENCOUNTER_COLUMN))
        if (encounter > 0) {
            val encounterBtn = elements.eventIndoorOutdoor.getChildAt(encounter) as RadioButton
            encounterBtn.isChecked = true
        }

        val preventionMeasures = ArrayList<String>()
        val closeContact = cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.CLOSECONTACT_COLUMN))
        val mask = cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.MASK_COLUMN))
        val ventilation = cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.VENTILATION_COLUMN))
        if (closeContact == 0) {
            preventionMeasures.add(getString(R.string.mitigation_distance_value))
        }
        when (mask) {
            1 -> preventionMeasures.add(getString(R.string.mitigation_mask_other_value))
            2 -> preventionMeasures.add(getString(R.string.mitigation_mask_me_value))
            3 -> {
                preventionMeasures.add(getString(R.string.mitigation_mask_me_value))
                preventionMeasures.add(getString(R.string.mitigation_mask_other_value))
            }
        }
        if (ventilation == 1) {
            preventionMeasures.add(getString(R.string.mitigation_ventilation_value))
        }

        if (closeContact + mask + ventilation == -3) {
            elements.eventMitigation.text = getString(R.string.click_to_select)
        } else {
            if (preventionMeasures.isNotEmpty()) {
                elements.eventMitigation.text = preventionMeasures.sorted().joinToString(", ")
            }
        }

        elements.eventnotesInput.setText(cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.NOTES_COLUMN)))

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

            elements.eventdateInput.setText(DateFormat.getDateInstance().format(initCal.time))
            elements.endeventdateInput.setText(DateFormat.getDateInstance().format(initCal.time))

        }

        elements.eventdateInput.setOnClickListener {
            DatePickerDialog(
                this@EditEventActivity, dateSetListener,
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val endeventdateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            endCal.set(Calendar.YEAR, year)
            endCal.set(Calendar.MONTH, monthOfYear)
            endCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            elements.endeventdateInput.setText(DateFormat.getDateInstance().format(endCal.time))

        }

        elements.endeventdateInput.setOnClickListener {
            val pickDialog = DatePickerDialog(
                this@EditEventActivity, endeventdateSetListener,
                initCal.get(Calendar.YEAR),
                initCal.get(Calendar.MONTH),
                initCal.get(Calendar.DAY_OF_MONTH)
            )
            pickDialog.datePicker.minDate = initCal.time.time
            pickDialog.show()
        }

        val initTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            initCal.set(Calendar.HOUR_OF_DAY, hour)
            initCal.set(Calendar.MINUTE, minute)
            initCal.set(Calendar.MILLISECOND, 1)    // To distinguish 0:00 from empty when loading

            elements.eventinittimeInput.setText(timeFormat.format(initCal.time))
            if (elements.eventendtimeInput.text.isEmpty() or (endCal.timeInMillis < initCal.timeInMillis)) {
                endCal.set(Calendar.HOUR_OF_DAY, initCal.get(Calendar.HOUR_OF_DAY))
                endCal.set(Calendar.MINUTE, initCal.get(Calendar.MINUTE))
                endCal.add(Calendar.MINUTE, 30)
                elements.eventendtimeInput.setText(timeFormat.format(endCal.time))
            }
        }

        val is24Hour = android.text.format.DateFormat.is24HourFormat(applicationContext)

        elements.eventinittimeInput.setOnClickListener {
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

//            if (endCal.timeInMillis < initCal.timeInMillis) {
//                Toast.makeText(
//                    this, R.string.incorrect_alarm_time, Toast.LENGTH_LONG
//                ).show()
//            } else {
                elements.eventendtimeInput.setText(timeFormat.format(endCal.time))
//            }
        }

        elements.eventendtimeInput.setOnClickListener {
            TimePickerDialog(
                this@EditEventActivity, endTimeSetListener,
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
                is24Hour
            ).show()
        }

        elements.eventMitigation.setOnClickListener {
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

            builder.setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                var measuresTaken = getString(R.string.none)
                if (preventionMeasures.isNotEmpty()) {
                    measuresTaken = preventionMeasures.sorted().joinToString(", ")
                }
                elements.eventMitigation.text = measuresTaken
            }
            builder.setNegativeButton(getString(android.R.string.cancel)) { _, _ -> }
            builder.create().show()
        }

        elements.okButtonAddEvent.setOnClickListener {
//          Process RadioButtons
            val contactIndoorOutdoorId = elements.eventIndoorOutdoor.checkedRadioButtonId
            var contactIndoorOutdoorChoice = -1
            if (contactIndoorOutdoorId != -1) {
                val btn: View = elements.eventIndoorOutdoor.findViewById(contactIndoorOutdoorId)
                contactIndoorOutdoorChoice = elements.eventIndoorOutdoor.indexOfChild(btn)
            }

            val maskMe = preventionMeasures.contains(getString(R.string.mitigation_mask_me_value)).compareTo(false)
            val maskOther = preventionMeasures.contains(getString(R.string.mitigation_mask_other_value)).compareTo(false)

//          Compulsory text field
            var errorCount = 0
            val eventName = elements.eventnameInput.text.toString()
            if (eventName.isEmpty()) {
                elements.eventnameInput.error = getString(R.string.compulsory_field)
                errorCount++
            }
            
//          Create updated row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Event")
                    put(feedEntry.NAME_COLUMN, eventName)
                    put(feedEntry.PLACE_COLUMN, elements.eventplaceInput.text.toString())
                    put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
                    put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, elements.eventphoneInput.text.toString())
                    put(feedEntry.COMPANIONS_COLUMN, elements.eventpeopleInput.text.toString())
                    put(
                        feedEntry.CLOSECONTACT_COLUMN,
                        if (elements.eventMitigation.text != getString(R.string.click_to_select)) {
                            (!preventionMeasures.contains(getString(R.string.mitigation_distance_value))).compareTo(
                                false
                            )
                        } else { -1 }
                    )
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                    put(feedEntry.NOTES_COLUMN, elements.eventnotesInput.text.toString())
                    put(
                        feedEntry.MASK_COLUMN,
                        if (elements.eventMitigation.text != getString(R.string.click_to_select)) {
                            2 * maskMe + maskOther
                        } else { -1 }
                    )
                    put(
                        feedEntry.VENTILATION_COLUMN,
                        if (elements.eventMitigation.text != getString(R.string.click_to_select)) {
                            (preventionMeasures.contains(getString(R.string.mitigation_ventilation_value))).compareTo(
                                false
                            )
                        } else { -1 }
                    )
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

        val beginTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIME_BEGIN_COLUMN))
        val initCal = Calendar.getInstance()
        val currentDay = initCal.get(Calendar.DAY_OF_YEAR)
        val currentYear = initCal.get(Calendar.YEAR)
        initCal.timeInMillis = beginTimestamp
        initCal.set(Calendar.DAY_OF_YEAR, currentDay)
        initCal.set(Calendar.YEAR, currentYear)

        val endTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(feedEntry.TIME_END_COLUMN))
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endTimestamp
        endCal.set(Calendar.DAY_OF_YEAR, currentDay)
        endCal.set(Calendar.YEAR, currentYear)

        val values = ContentValues().apply {
            put(feedEntry.TYPE_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.TYPE_COLUMN)))
            put(feedEntry.NAME_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.NAME_COLUMN)))
            put(feedEntry.PLACE_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.PLACE_COLUMN)))
            put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
            put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
            put(feedEntry.PHONE_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.PHONE_COLUMN)))
            put(feedEntry.RELATIVE_COLUMN, cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.RELATIVE_COLUMN)))
            put(feedEntry.COMPANIONS_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.COMPANIONS_COLUMN)))
            put(feedEntry.CLOSECONTACT_COLUMN, cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.CLOSECONTACT_COLUMN)))
            put(feedEntry.ENCOUNTER_COLUMN, cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.ENCOUNTER_COLUMN)))
            put(feedEntry.NOTES_COLUMN, cursor.getString(cursor.getColumnIndexOrThrow(feedEntry.NOTES_COLUMN)))
            put(feedEntry.MASK_COLUMN, cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.MASK_COLUMN)))
            put(feedEntry.VENTILATION_COLUMN, cursor.getInt(cursor.getColumnIndexOrThrow(feedEntry.VENTILATION_COLUMN)))
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
}
