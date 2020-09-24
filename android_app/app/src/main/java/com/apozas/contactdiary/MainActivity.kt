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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.ActionMode
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private var isFabOpen = false
    private var onlyRisky = false
    private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry
    private val dbHelper = FeedReaderDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val notificationHandler = NotificationHandler()
        notificationHandler.scheduleNotification(this)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        onlyRisky = preferences.getBoolean("closecontactonly", false)

        restrict15LastDays()
        viewData(onlyRisky)

        registerForContextMenu(findViewById(R.id.diarytable))

//      Edit entry on click
        diarytable.setOnItemClickListener { _, _, position, _ ->
            val idx = diarytable.adapter.getItemId(position)
            val entry = diarytable.adapter.getItem(position) as Cursor

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

//      Select to delete on long press
        val itemList: MutableList<Long> = ArrayList()
        diarytable.setMultiChoiceModeListener(object : MultiChoiceModeListener {
            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                actionMode.menuInflater.inflate(R.menu.context_menu, menu)
                return true
            }

            override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.context_delete -> {
                        val db = dbHelper.writableDatabase
                        for (item: Long in itemList) {
                            db.delete(
                                ContactDatabase.ContactDatabase.FeedEntry.TABLE_NAME,
                                "_id LIKE ?",
                                arrayOf(item.toString())
                            )
                        }
                        Toast.makeText(
                            applicationContext,
                            getString(if (itemList.size > 1) R.string.entries_deleted
                            else R.string.entry_deleted
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        itemList.clear()
                        actionMode.finish()
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
                false
            }

            override fun onItemCheckedStateChanged(
                actionMode: ActionMode,
                i: Int,
                position: Long,
                checked: Boolean
            ) {
                if (checked) {
                    itemList.add(position)
                    actionMode.title = itemList.size.toString() + getString(R.string.entries_selected)
                } else {
                    itemList.remove(position)
                    actionMode.title = itemList.size.toString() + getString(R.string.entries_selected)
                }
            }
        })

//      Show message on empty list
        diarytable.emptyView = findViewById(R.id.emptyList)

//      FAB operation
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
            R.id.popup_delete -> {
                deleteEntry(info.id)
                Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
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

        fab.startAnimation(rotateForward)
        fab1.startAnimation(fabOpen)
        fabText1.startAnimation(fabTextOpen)
        fab2.startAnimation(fabOpen)
        fabText2.startAnimation(fabTextOpen)
        fab1.isClickable = true
        fab2.isClickable = true
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

        fab.startAnimation(rotateBackward)
        fab1.startAnimation(fabClose)
        fabText1.startAnimation(fabTextClose)
        fab2.startAnimation(fabClose)
        fabText2.startAnimation(fabTextClose)
        fab1.isClickable = false
        fab2.isClickable = false
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
    fun addContact(v: View): Unit {
        startActivity(Intent(this@MainActivity, NewContactActivity::class.java))
    }

    fun addEvent(v: View): Unit {
        startActivity(Intent(this@MainActivity, NewEventActivity::class.java))
    }

    fun openSettings(v: View): Unit {
        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
    }

//  Database operation
    private fun viewData(onlyRisky: Boolean) {
        val cursor = dbHelper.viewData(onlyRisky)

        val adapter = DataCursorAdapter(this, cursor)

        diarytable.adapter = adapter
    }

    private fun restrict15LastDays() {
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

    private fun deleteEntry(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(feedEntry.TABLE_NAME, "_id LIKE ?", arrayOf(id.toString()))
    }
}
