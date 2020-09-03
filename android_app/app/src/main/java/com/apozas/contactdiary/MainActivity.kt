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

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    var isFabOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val notificationHandler = NotificationHandler()
        notificationHandler.scheduleNotification(this)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val onlyRisky = preferences.getBoolean("closecontactonly", false)

        restrict15LastDays()
        viewData(onlyRisky)

        diarytable.setOnItemClickListener { adapterView, view, position, id ->
            val idx = diarytable.adapter.getItemId(position)
            val entry = diarytable.adapter.getItem(position) as Cursor

            val contactType = entry.getString(entry.getColumnIndex(ContactDatabase.ContactDatabase.FeedEntry.TYPE_COLUMN))

            if (contactType == "Contact") {
                val intent = Intent(this@MainActivity, EditContactActivity::class.java)
                intent.putExtra("entry", idx.toString())
                startActivity(intent)
            } else if (contactType == "Event") {
                val intent = Intent(this@MainActivity, EditEventActivity::class.java)
                intent.putExtra("entry", idx.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Something very wrong has happened", Toast.LENGTH_LONG).show()
            }
        }

        fab.setOnClickListener {
            animateFAB()
        }
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val onlyRisky = preferences.getBoolean("closecontactonly", false)
        viewData(onlyRisky)
    }

    override fun onBackPressed() {
        if (isFabOpen) {
            collapseFAB()
        } else {
            super.onBackPressed()
        }
    }

//  FAB animations
    fun expandFAB() {
        val fab_open = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        val rotate_forward = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_forward)

        fab.startAnimation(rotate_forward)
//      fab1.animate().translationY(0.toFloat());
//      fab2.animate().translationY(0.toFloat());
        fab1.startAnimation(fab_open)
        fabText1.startAnimation(fab_open)
        fab2.startAnimation(fab_open)
        fabText2.startAnimation(fab_open)
        fab1.isClickable = true
        fab2.isClickable = true
        isFabOpen = true
    }

    fun collapseFAB() {
        val fab_close: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)
        val rotate_backward: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_backward)

        fab.startAnimation(rotate_backward)
        fab1.startAnimation(fab_close)
        fabText1.startAnimation(fab_close)
        fab2.startAnimation(fab_close)
        fabText2.startAnimation(fab_close)
//      fab1.animate().translationY(-resources.getDimension(R.dimen.standard_55));
//      fab2.animate().translationY(-resources.getDimension(R.dimen.standard_105));
        fab1.isClickable = false
        fab2.isClickable = false
        isFabOpen = false
    }

    fun animateFAB() {
        if (isFabOpen) {
            collapseFAB()
        } else {
            expandFAB()
        }
    }

//  New button actions
    fun addContact(v: View): Unit {
        startActivity(Intent(this@MainActivity, NewContactActivity::class.java))
    }

    fun addEvent(v: View): Unit {
        startActivity(Intent(this@MainActivity, NewEventActivity::class.java))
    }

    fun openSettings(v: View): Unit {
        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
    }

//    Database operation
    val dbHelper = FeedReaderDbHelper(this)

    fun viewData(onlyRisky: Boolean) {
        val cursor = dbHelper.viewData(onlyRisky)

        val adapter = DataCursorAdapter(this, cursor)

        diarytable.adapter = adapter
    }

    fun restrict15LastDays() {
        val db = dbHelper.writableDatabase
//      Create Calendar set to 15 days ago
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR, 0)
        val fifteenDaysAgo = cal.timeInMillis

        // Define 'where' part of query.
        val selection = "DELETE FROM ${ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME} " +
                "WHERE ${ContactDatabase.ContactDatabase.FeedEntry.DATETIME_COLUMN} <= " + fifteenDaysAgo.toString()
        // Issue SQL statement.
        db.execSQL(selection)
    }
}
