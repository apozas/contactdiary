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
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_editcontact.*
import java.text.DateFormat
import java.util.*

class EditContactActivity : AppCompatActivity() {

    private val dbHelper = FeedReaderDbHelper(this)
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editcontact)
        setSupportActionBar(findViewById(R.id.toolbar))

        setupUI(findViewById(R.id.editcontactlayout))

//      Get info from MainActivity
        val db = dbHelper.writableDatabase
        val info = intent.extras?.getString("entry")

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        name_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
        place_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))

        val timestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIMESTAMP_COLUMN))
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        date_edit.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(cal.time))
        time_edit.setText(minutesToText(cursor.getInt(cursor.getColumnIndex(feedEntry.DURATION_COLUMN))))

        if (cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)) != ""){
            phone_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)))
        }

        var relative = cursor.getInt(cursor.getColumnIndex(feedEntry.RELATIVE_COLUMN))
        if (relative%2==0) {relative = 2*relative+1}
        else if (relative==-1) {relative = known_group.childCount-2}    // Migration from 1.0.4 fix
        val relativeBtn = known_group.getChildAt(relative) as RadioButton
        relativeBtn.isChecked = true

        var encounter = cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN))
        if (encounter%2==0) {encounter = 2*encounter+1}
        else if (encounter==-1) {encounter = contact_indoor_outdoor.childCount-2}    // Migration from 1.0.4 fix
        val encounterBtn = contact_indoor_outdoor.getChildAt(encounter) as RadioButton
        encounterBtn.isChecked = true

        var closeContact = cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN))
        if (closeContact%2==0) {closeContact = 2*closeContact+1}
        else if (closeContact==-1) {closeContact = distance_group.childCount-2}    // Migration from 1.0.4 fix
        val closeContactBtn = distance_group.getChildAt(closeContact) as RadioButton
        closeContactBtn.isChecked = true

        notes_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NOTES_COLUMN)))

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            date_edit.setText(DateFormat.getDateInstance().format(cal.time))

        }

        date_edit.setOnClickListener {
            DatePickerDialog(
                this@EditContactActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        time_edit.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.duration_dialog, null)
            val durationText = dialogView.findViewById<EditText>(R.id.duration)
            builder.setView(dialogView)
            builder.setTitle(getString(R.string.contact_duration_title))
            durationText.hint = getString(R.string.duration_instructions)
            builder.setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                val durationText = durationStringToText(durationText.text.toString())
                if (durationText.isEmpty()) {
                    Toast.makeText(this,
                        R.string.incorrect_alarm_time,
                        Toast.LENGTH_LONG).show()
                } else { time_edit.setText(durationText) }
            })
            builder.setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            val dialog = builder.create()
            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show()
        }

        okButton_AddContact.setOnClickListener {
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

            val contactCloseContactId = distance_group.checkedRadioButtonId
            var contactCloseContactChoice = -1
            if (contactCloseContactId != -1) {
                val btn: View = distance_group.findViewById(contactCloseContactId)
                contactCloseContactChoice = distance_group.indexOfChild(btn)
            }

//          Handle time field
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

//          Compulsory text fields
            var errorCount = 0
            val contactName = name_edit.text.toString()
            if (contactName.isEmpty()) {
                name_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val contactPlace = place_edit.text.toString()
            if (contactPlace.isEmpty()) {
                place_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val durationText = time_edit.text.toString()
            var contactDuration = 0
            if (durationText.isEmpty()) {
                time_edit.error = getString(R.string.compulsory_field)
                errorCount++
            } else {
                val durationParts = durationText.split('h')
                contactDuration = durationParts[0].toInt() * 60 + durationParts[1].dropLast(1).toInt()
            }

//          Create new row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Contact")
                    put(feedEntry.NAME_COLUMN, contactName)
                    put(feedEntry.PLACE_COLUMN, contactPlace)
                    put(feedEntry.TIMESTAMP_COLUMN, cal.timeInMillis)
                    put(feedEntry.DURATION_COLUMN, contactDuration)
                    put(feedEntry.PHONE_COLUMN, phone_edit.text.toString())
                    put(feedEntry.RELATIVE_COLUMN, relativeChoice)
                    put(feedEntry.CLOSECONTACT_COLUMN, contactCloseContactChoice)
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                    put(feedEntry.NOTES_COLUMN, notes_edit.text.toString())
                }

//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(info.toString())
                db.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)

                Toast.makeText(
                    applicationContext,
                    applicationContext.getResources().getString(R.string.contact_saved),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    fun deleteContact(view: View) {
        val db = dbHelper.writableDatabase
        val info = intent.extras?.getString("entry")
        db.delete(feedEntry.TABLE_NAME, "_id LIKE ?", arrayOf(info))

        Toast.makeText(
            applicationContext,
            applicationContext.resources.getString(R.string.entry_deleted),
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    fun duplicateContact(view: View) {
        val db = dbHelper.writableDatabase
        val info = intent.extras?.getString("entry")
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + info, null
        )
        cursor.moveToFirst()

        val timestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIMESTAMP_COLUMN))
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_YEAR, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))

        val values = ContentValues().apply {
            put(feedEntry.TYPE_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.TYPE_COLUMN)))
            put(feedEntry.NAME_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
            put(feedEntry.PLACE_COLUMN, cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))
            put(feedEntry.TIMESTAMP_COLUMN, cal.timeInMillis)
            put(feedEntry.DURATION_COLUMN, cursor.getInt(cursor.getColumnIndex(feedEntry.DURATION_COLUMN)))
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
        finish()
    }

    private fun setupUI(view: View) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) view.setOnTouchListener { v, _ ->
            v.clearFocus()
            hideSoftKeyboard()
            false
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

    private fun minutesToText(duration: Int): String {
        val hours = duration / 60
        val minutes = duration % 60
        return hours.toString() + "h" + minutes.toString() + "m"
    }
}
