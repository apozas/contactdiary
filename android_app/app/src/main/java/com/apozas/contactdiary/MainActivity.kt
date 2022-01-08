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

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.apozas.contactdiary.databinding.ActivityMainBinding
import com.apozas.contactdiary.databinding.ActivityMainInsideBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    private var isFabOpen = false
    private var onlyRisky = false
    private var onlyRecent = true
    private var numDays = 15
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry
    private val dbHelper = FeedReaderDbHelper(this)
    private lateinit var binding: ActivityMainBinding
    private lateinit var elements: ActivityMainInsideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        super.onCreate(savedInstanceState)
        when (preferences.getString("theme", "System")) {
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        elements = binding.activityMainInside
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        val notificationHandler = NotificationHandler()
        notificationHandler.scheduleNotification(this)

        onlyRecent = preferences.getBoolean("logNdays", true)
        numDays = preferences.getString("number_of_days", "15")!!.toInt()
        if (onlyRecent) {
            restrictLastDays(numDays)
        }
        onlyRisky = preferences.getBoolean("closecontactonly", false)
        viewData(onlyRisky)

        registerForContextMenu(findViewById(R.id.diarytable))

//      Edit entry on click
        elements.diarytable.setOnItemClickListener { _, _, position, _ ->
            val idx = elements.diarytable.adapter.getItemId(position)
            val entry = elements.diarytable.adapter.getItem(position) as Cursor

            when (entry.getString(entry.getColumnIndex(ContactDatabase.ContactDatabase.FeedEntry.TYPE_COLUMN))) {
                "Contact" -> {
                    val intent = Intent(this@MainActivity, EditContactActivity::class.java)
                    intent.putExtra("entry", idx.toString())
                    startActivity(intent)
                }
                "Event" -> {
                    val intent = Intent(this@MainActivity, EditEventActivity::class.java)
                    intent.putExtra("entry", idx.toString())
                    startActivity(intent)
                }
                else -> {
                    Toast.makeText(this, "Something very wrong has happened", Toast.LENGTH_LONG).show()
                }
            }
        }

//      Show message on empty list
        elements.diarytable.emptyView = findViewById(R.id.emptyList)

//      FAB operation
        elements.fab.setOnClickListener {
            animateFAB()
        }
    }

    override fun onResume() {
        super.onResume()
        if (onlyRecent) { restrictLastDays(numDays) }
        viewData(onlyRisky)
    }

    override fun onBackPressed() {
        if (isFabOpen) {
            collapseFAB()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.popup_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info: AdapterView.AdapterContextMenuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        return when (item.itemId) {
            R.id.popup_select -> {
//              Launch multiChoiceMode
                elements.diarytable.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
                val itemList: MutableList<Long> = ArrayList()
                elements.diarytable.setMultiChoiceModeListener(
                    object : AbsListView.MultiChoiceModeListener {
                        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                            actionMode.menuInflater.inflate(R.menu.context_menu, menu)
                            return true
                        }

                        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                            return false
                        }

                        override fun onActionItemClicked(
                            actionMode: ActionMode,
                            menuItem: MenuItem
                        ): Boolean {
                            when (menuItem.itemId) {
                                R.id.context_delete -> {
                                    itemList.forEach { deleteEntry(it) }
                                    Toast.makeText(
                                        applicationContext,
                                        getString(
                                            if (itemList.size > 1) R.string.entries_deleted
                                            else R.string.entry_deleted
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    itemList.clear()
                                    actionMode.finish()
                                    if (onlyRecent) {
                                        restrictLastDays(numDays)
                                    }
                                    viewData(onlyRisky)
                                    return true
                                }
                                R.id.context_duplicate -> {
                                    itemList.forEach { duplicateEntry(it) }
                                    Toast.makeText(
                                        applicationContext,
                                        getString(
                                            if (itemList.size > 1) R.string.entries_duplicated
                                            else R.string.entry_duplicated
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    itemList.clear()
                                    actionMode.finish()
                                    if (onlyRecent) {
                                        restrictLastDays(numDays)
                                    }
                                    viewData(onlyRisky)
                                    return true
                                }
                                else -> {
                                    return false
                                }
                            }
                        }
                        override fun onDestroyActionMode(actionMode: ActionMode) {
                            itemList.clear()
                            elements.diarytable.choiceMode = ListView.CHOICE_MODE_SINGLE
                        }

                        override fun onItemCheckedStateChanged(
                            actionMode: ActionMode,
                            i: Int,
                            position: Long,
                            checked: Boolean
                        ) {
                            if (checked) {
                                itemList.add(position)
                                actionMode.title =
                                    itemList.size.toString() + getString(R.string.entries_selected)
                            } else {
                                itemList.remove(position)
                                actionMode.title =
                                    itemList.size.toString() + getString(R.string.entries_selected)
                            }
                        }
                    })
                elements.diarytable.setItemChecked(info.position, true)
                true
            }
            R.id.popup_duplicate -> {
                duplicateEntry(info.id)
                if (onlyRecent) { restrictLastDays(numDays) }
                viewData(onlyRisky)
                true
            }
            R.id.popup_delete -> {
                deleteEntry(info.id)
                Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                if (onlyRecent) { restrictLastDays(numDays) }
                viewData(onlyRisky)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

//  FAB animations
    private fun expandFAB() {
        val fabOpen = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_open)
        val fabTextOpen = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.fab_open
        )
        val rotateForward = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.rotate_forward
        )

        elements.fab.startAnimation(rotateForward)
        elements.fab1.startAnimation(fabOpen)
        elements.fabText1.startAnimation(fabTextOpen)
        elements.fab2.startAnimation(fabOpen)
        elements.fabText2.startAnimation(fabTextOpen)
        elements.fab1.isClickable = true
        elements.fab2.isClickable = true
        isFabOpen = true
    }

    private fun collapseFAB() {
        val fabClose: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.fab_close)
        val fabTextClose: Animation = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.fab_close
        )
        val rotateBackward: Animation = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.rotate_backward
        )

        elements.fab.startAnimation(rotateBackward)
        elements.fab1.startAnimation(fabClose)
        elements.fabText1.startAnimation(fabTextClose)
        elements.fab2.startAnimation(fabClose)
        elements.fabText2.startAnimation(fabTextClose)
        elements.fab1.isClickable = false
        elements.fab2.isClickable = false
        isFabOpen = false
    }

    private fun animateFAB() {
        if (isFabOpen) {
            collapseFAB()
        } else {
            expandFAB()
        }
    }

//  New button actions
    fun addContact(v: View) {
        startActivity(Intent(this@MainActivity, NewContactActivity::class.java))
    }

    fun addEvent(v: View) {
        startActivity(Intent(this@MainActivity, NewEventActivity::class.java))
    }

    fun openSettings(v: View) {
        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
    }

//  Database operation
    private fun viewData(onlyRisky: Boolean) {
        val cursor = dbHelper.viewData(onlyRisky)
        val adapter = DataCursorAdapter(this, cursor)

        elements.diarytable.adapter = adapter
    }

    private fun restrictLastDays(numDays: Int) {
        val db = dbHelper.writableDatabase
//      Create Calendar set to numDays days ago
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -numDays)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.HOUR, 0)
        val daysAgo = cal.timeInMillis.toString()

//      Define 'where' part of query.
        val selection = "DELETE FROM ${ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME} " +
                "WHERE ${ContactDatabase.ContactDatabase.FeedEntry.TIME_BEGIN_COLUMN} <= " + daysAgo
//      Issue SQL statement.
        db.execSQL(selection)
    }

    private fun deleteEntry(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(feedEntry.TABLE_NAME, "_id LIKE ?", arrayOf(id.toString()))
    }

    private fun duplicateEntry(id: Long) {
        val db = dbHelper.writableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${feedEntry.TABLE_NAME}" +
                    " WHERE _id=" + id, null
        )
        cursor.moveToFirst()

        val beginTimestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_BEGIN_COLUMN))
        val initCal = Calendar.getInstance()
        val currentDay = initCal.get(Calendar.DAY_OF_YEAR)
        val currentYear = initCal.get(Calendar.YEAR)
        initCal.timeInMillis = beginTimestamp
        initCal.set(Calendar.DAY_OF_YEAR, currentDay)
        initCal.set(Calendar.YEAR, currentYear)

        val endTimestamp = cursor.getLong(cursor.getColumnIndex(feedEntry.TIME_END_COLUMN))
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endTimestamp
        endCal.set(Calendar.DAY_OF_YEAR, currentDay)
        endCal.set(Calendar.YEAR, currentYear)

        val values = ContentValues().apply {
            put(
                feedEntry.TYPE_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.TYPE_COLUMN))
            )
            put(
                feedEntry.NAME_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.NAME_COLUMN))
            )
            put(
                feedEntry.PLACE_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.PLACE_COLUMN))
            )
            put(feedEntry.TIME_BEGIN_COLUMN, initCal.timeInMillis)
            put(feedEntry.TIME_END_COLUMN, endCal.timeInMillis)
            put(
                feedEntry.PHONE_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.PHONE_COLUMN))
            )
            put(
                feedEntry.RELATIVE_COLUMN,
                cursor.getInt(cursor.getColumnIndex(feedEntry.RELATIVE_COLUMN))
            )
            put(
                feedEntry.COMPANIONS_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.COMPANIONS_COLUMN))
            )
            put(
                feedEntry.CLOSECONTACT_COLUMN,
                cursor.getInt(cursor.getColumnIndex(feedEntry.CLOSECONTACT_COLUMN))
            )
            put(
                feedEntry.ENCOUNTER_COLUMN,
                cursor.getInt(cursor.getColumnIndex(feedEntry.ENCOUNTER_COLUMN))
            )
            put(
                feedEntry.NOTES_COLUMN,
                cursor.getString(cursor.getColumnIndex(feedEntry.NOTES_COLUMN))
            )
            put(
                feedEntry.MASK_COLUMN,
                cursor.getInt(cursor.getColumnIndex(feedEntry.MASK_COLUMN))
            )
            put(
                feedEntry.VENTILATION_COLUMN,
                cursor.getInt(cursor.getColumnIndex(feedEntry.VENTILATION_COLUMN))
            )
        }

//      Insert the new row
        db?.insert(feedEntry.TABLE_NAME, null, values)
        cursor.close()
    }
}
