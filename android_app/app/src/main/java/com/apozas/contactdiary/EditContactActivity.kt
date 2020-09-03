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
import kotlinx.android.synthetic.main.activity_addcontact.*
import kotlinx.android.synthetic.main.activity_editcontact.*
import kotlinx.android.synthetic.main.activity_editcontact.contact_indoor_outdoor
import kotlinx.android.synthetic.main.activity_editcontact.distance_group
import kotlinx.android.synthetic.main.activity_editcontact.known_group
import kotlinx.android.synthetic.main.activity_editcontact.known_no
import kotlinx.android.synthetic.main.activity_editcontact.known_yes
import kotlinx.android.synthetic.main.activity_editcontact.okButton_AddContact
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class EditContactActivity : AppCompatActivity() {


    val dbHelper = FeedReaderDbHelper(this)
    val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editcontact)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Get info from MainActivity
        val db = dbHelper.writableDatabase
        val info = getIntent().getExtras()?.getString("entry")

        val cursor: Cursor = db.rawQuery("SELECT * FROM ${feedEntry.TABLE_NAME}" +
                " WHERE _id=" + info, null)
        cursor.moveToFirst()

        name_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN)))
        place_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN)))

        val timestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.DATETIME_COLUMN))
        var cal = Calendar.getInstance()
        cal.setTimeInMillis(timestamp)

        date_edit.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(cal.time))
        time_edit.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))

        if (cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)) != ""){
            phone_edit.setText(cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN)))
        }

        var relativeBtn = cursor.getInt(cursor.getColumnIndex(feedEntry.RELATIVE_COLUMN))
        var encounterBtn = cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN))
        var closeContactBtn = cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN))

        if (relativeBtn == 0) {
            known_yes.setChecked(true)
        } else if (relativeBtn == 1) {
            known_no.setChecked(true)
        }

        if (encounterBtn == 0) {
            indoors.setChecked(true)
        } else if (encounterBtn == 1) {
            outdoors.setChecked(true)
        }

        if (closeContactBtn == 0) {
            closecontact.setChecked(true)
        } else if (closeContactBtn == 1) {
            noclosecontact.setChecked(true)
        } else if (closeContactBtn == 2) {
            unknowncontact.setChecked(true)
        }

        // Listen to new values

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            date_edit.setText(DateFormat.getDateInstance().format(cal.time))

        }

        date_edit.setOnClickListener {
            DatePickerDialog(this@EditContactActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)

            time_edit.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))

        }

        time_edit.setOnClickListener {
            TimePickerDialog(this@EditContactActivity, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true).show()
        }

        okButton_AddContact.setOnClickListener {
//          Gets the data repository in write mode
            val db = dbHelper.writableDatabase

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

//          Compulsory text fields
            var errorCount = 0
            val contactName = name_edit.getText().toString()
            if (contactName.length == 0) {
                name_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }
            val contactPlace = place_edit.getText().toString()
            if (contactPlace.length == 0) {
                place_edit.error = getString(R.string.compulsory_field)
                errorCount++
            }

//          Create new row
            if (errorCount == 0) {
                val values = ContentValues().apply {
                    put(feedEntry.TYPE_COLUMN, "Contact")
                    put(feedEntry.NAME_COLUMN, contactName)
                    put(feedEntry.PLACE_COLUMN, contactPlace)
                    put(feedEntry.DATETIME_COLUMN, cal.timeInMillis)
                    put(feedEntry.PHONE_COLUMN, phone_edit.getText().toString())
                    put(feedEntry.RELATIVE_COLUMN, relativeChoice)
                    put(feedEntry.CLOSECONTACT_COLUMN, contactCloseContactChoice)
                    put(feedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
                }

//              Update the database
                val selection = "_id LIKE ?"
                val selectionArgs = arrayOf(info.toString())
                db.update(feedEntry.TABLE_NAME, values, selection, selectionArgs)

                Toast.makeText(
                    applicationContext,
                    applicationContext.getResources().getString(R.string.contact_saved),
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    fun deleteContact(view: View) {
        val db = dbHelper.writableDatabase
        val info = getIntent().getExtras()?.getString("entry")
        db.delete(feedEntry.TABLE_NAME, "_id LIKE ?", arrayOf(info))

        Toast.makeText(
            applicationContext,
            applicationContext.getResources().getString(R.string.contact_deleted),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

}
