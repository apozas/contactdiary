package com.apozas.contactdiary

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_addcontact.*
import kotlinx.android.synthetic.main.activity_addevent.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class NewContactActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addcontact)
        setSupportActionBar(findViewById(R.id.toolbar))

        var cal = Calendar.getInstance()

        // Set current values
        date_input.setText(DateFormat.getDateInstance().format(cal.time))
        time_input.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))

        // Listen to new values
        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            date_input.setText(DateFormat.getDateInstance().format(cal.time))

        }

        date_input.setOnClickListener {
            DatePickerDialog(this@NewContactActivity, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)

            time_input.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time))
        }

        time_input.setOnClickListener {
            TimePickerDialog(this@NewContactActivity, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true).show()
        }

//      Database operation
        val dbHelper = FeedReaderDbHelper(this)

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

//          Process timestamp
            var timeInput = time_input.getText().toString()
            if (timeInput == "") {
                timeInput = "0:00"
            }
            val datetime = SimpleDateFormat("dd MMMM yyyy HH:mm").parse(
                date_input.getText().toString() + " " + timeInput) as Date
            cal.setTime(datetime)

//          Create a new map of values, where column names are the keys
            val values = ContentValues().apply {
                put(ContactDatabase.ContactDatabase.FeedEntry.TYPE_COLUMN, "Contact")
                put(ContactDatabase.ContactDatabase.FeedEntry.NAME_COLUMN, name_input.getText().toString())
                put(ContactDatabase.ContactDatabase.FeedEntry.PLACE_COLUMN, place_input.getText().toString())
                put(ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN, cal.timeInMillis)
                put(ContactDatabase.ContactDatabase.FeedEntry.PHONE_COLUMN, phone_input.getText().toString())
                put(ContactDatabase.ContactDatabase.FeedEntry.RELATIVE_COLUMN, relativeChoice)
                put(ContactDatabase.ContactDatabase.FeedEntry.CLOSECONTACT_COLUMN, contactCloseContactChoice)
                put(ContactDatabase.ContactDatabase.FeedEntry.ENCOUNTER_COLUMN, contactIndoorOutdoorChoice)
            }

//          Insert the new row, returning the primary key value of the new row
            db?.insert(ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME, null, values)

            Toast.makeText(
                applicationContext,
                applicationContext.getResources().getString(R.string.contact_saved),
                Toast.LENGTH_LONG).show()

            finish()
 //            TODO("Perform checks of required information")
        }
    }
}
