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

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.Cursor.*
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.apozas.contactdiary.ContactDatabase.Companion.SQL_CREATE_ENTRIES
import com.apozas.contactdiary.ContactDatabase.Companion.SQL_DELETE_ENTRIES
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportFragmentManager.beginTransaction().replace(R.id.container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val EXPORT_DB = 1
        private val IMPORT_DB = 2
        private val feedEntry = ContactDatabase.ContactDatabase.FeedEntry

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val preferences = preferenceManager.sharedPreferences
            val prefsedit = preferences.edit()

            val oldTime = preferences.getString("reminder_time", "21:00").toString()
            val reminderTime = findPreference<EditTextPreference>("reminder_time")
            val reminderToggle =
                findPreference<SwitchPreference>("reminder_toggle") as SwitchPreference

            reminderTime?.setOnPreferenceChangeListener { _, newValue ->
                var isTimeGood = true
                val newTime = newValue as String
                if (newTime.split(":").size == 2) {
                    val timeparts = newValue.split(":")
                    if ((timeparts[0].toInt() > 23) || (timeparts[1].toInt() > 59)) {
                        Toast.makeText(
                            context,
                            getString(R.string.incorrect_alarm_time),
                            Toast.LENGTH_LONG
                        ).show()
                        isTimeGood = false
                    }
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.incorrect_alarm_time),
                        Toast.LENGTH_LONG
                    ).show()
                    isTimeGood = false
                }
                if ((newValue.toString() != oldTime) && isTimeGood) {
                    prefsedit.putString("reminder_time", newValue)
                    prefsedit.apply()
                    Toast.makeText(context, getString(R.string.alarm_modified), Toast.LENGTH_SHORT)
                        .show()
                    updateNotificationPreferences(reminderToggle.isEnabled)
                    true
                } else {
                    prefsedit.putString("reminder_time", oldTime)
                    prefsedit.apply()
                    false
                }
            }
            reminderToggle.setOnPreferenceChangeListener { _, newValue ->
                updateNotificationPreferences(newValue as Boolean)
                true
            }

            val prefTheme = findPreference<ListPreference>("theme")
            prefTheme!!.setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }

            val export = findPreference<Preference>("export")
            export!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                createFile()
                true
            }

            val import = findPreference<Preference>("import")
            import!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                readFile()
                true
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
            if ((requestCode == EXPORT_DB) && (resultCode == Activity.RESULT_OK)) {
                // The result data contains a URI for the document or directory that
                // the user selected.
                resultData?.data?.also { uri ->
                    exportDB(requireActivity().applicationContext, uri)
                }
            }
            if ((requestCode == IMPORT_DB) && (resultCode == Activity.RESULT_OK)) {
                resultData?.data?.also { uri ->
                    importDB(requireActivity().applicationContext, uri)
                }
            }
        }

        private fun updateNotificationPreferences(on: Boolean) {
            val receiver = ComponentName(
                requireActivity().applicationContext, NotificationReceiver::class.java
            )
            val pm = requireActivity().applicationContext.packageManager
            val notificationHandler = NotificationHandler()
            if (on) {
                notificationHandler.scheduleNotification(requireActivity().applicationContext)
                pm.setComponentEnabledSetting(
                    receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                notificationHandler.disableNotification(requireActivity().applicationContext)
                pm.setComponentEnabledSetting(
                    receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        private fun exportDB(context: Context, uri: Uri) {
            val dbHelper = FeedReaderDbHelper(context)
            val dateFormatter = SimpleDateFormat("yyyy-LL-dd-HH:mm")
            try {
                val csvWriter = context.contentResolver.openOutputStream(uri)
                val db: SQLiteDatabase = dbHelper.readableDatabase
                val cursor: Cursor = db.rawQuery(
                    "SELECT * FROM ${feedEntry.TABLE_NAME}",
                    null
                )
                val columnNames =
                    cursor.columnNames.drop(1).toMutableList()    // We don't care of the _id column
                columnNames[columnNames.indexOf("CloseContact")] = "DistanceKept"
                columnNames[columnNames.indexOf("Masks")] = "MaskMe"
                columnNames.add(columnNames.size-1,"MaskOthers")
                csvWriter!!.write(
                    columnNames.joinToString(separator = "\t", postfix = "\n").toByteArray()
                )
                while (cursor.moveToNext()) {
                    val columns = cursor.columnCount
                    val arrStr = mutableListOf<String>()
                    for (i in 1 until columns + 1) {    // We don't care of the _id column
                        when (columnNames[i - 1]) {
                            "BeginTime" -> arrStr.add(dateFormatter.format(cursor.getLong(i)))
                            "EndTime" -> arrStr.add(dateFormatter.format(cursor.getLong(i)))
                            "Relative" -> arrStr.add(
                                when (cursor.getInt(i)) {
                                    -1 -> ""
                                    0 -> ""
                                    1 -> "Yes"
                                    3 -> "No"
                                    else -> cursor.getInt(i).toString()
                                }
                            )
                            "EncounterType" -> arrStr.add(
                                when (cursor.getInt(i)) {
                                    -1 -> ""
                                    1 -> "Indoors"
                                    3 -> "Outdoors"
                                    else -> cursor.getInt(i).toString()
                                }
                            )
                            "DistanceKept" -> arrStr.add(
                                when (cursor.getInt(i)) {
                                    -1 -> ""
                                    0 -> "Yes"
                                    1 -> "No"
                                    else -> cursor.getInt(i).toString()
                                }
                            )
                            "MaskMe" -> when (cursor.getInt(i)) {
                                    -1 -> {
                                        arrStr.add("")
                                        arrStr.add("")
                                    }
                                    0 -> {
                                        arrStr.add("No")
                                        arrStr.add("No")
                                    }
                                    1 -> {
                                        arrStr.add("No")
                                        arrStr.add("Yes")
                                    }
                                    2 -> {
                                        arrStr.add("Yes")
                                        arrStr.add("No")
                                    }
                                    3 -> {
                                        arrStr.add("Yes")
                                        arrStr.add("Yes")
                                    }
                                    else -> cursor.getInt(i).toString()
                                }
                            "MaskOthers" -> {}
                            "Ventilation" -> arrStr.add(
                                when (cursor.getInt(i - 1)) {    // The -1 is because MaskOthers
                                    -1 -> ""
                                    0 -> "No"
                                    1 -> "Yes"
                                    else -> cursor.getInt(i - 1).toString()
                                }
                            )
                            else -> when (cursor.getType(i)) {
                                FIELD_TYPE_STRING -> arrStr.add(cursor.getString(i))
                                FIELD_TYPE_INTEGER -> arrStr.add(cursor.getLong(i).toString())
                                FIELD_TYPE_NULL -> arrStr.add("")
                            }
                        }
                    }
                    csvWriter.write(
                        arrStr.joinToString(separator = "\t", postfix = "\n").toByteArray()
                    )
                }
                csvWriter.close()
                cursor.close()
                Toast.makeText(context, getString(R.string.export_success), Toast.LENGTH_LONG).show()
            } catch (sqlEx: Exception) {
                Log.e("Export", sqlEx.message, sqlEx)
            }
        }

        private fun importDB(context: Context, uri: Uri) {
            val dbHelper = FeedReaderDbHelper(context)
            val db = dbHelper.writableDatabase
            val csvReader = BufferedReader(
                InputStreamReader(
                    context.contentResolver.openInputStream(
                        uri
                    )
                )
            )

            val columnNames = csvReader.readLine().split("\t")
            if (columnNames != listOf(
                    feedEntry.TYPE_COLUMN, feedEntry.NAME_COLUMN, feedEntry.PLACE_COLUMN,
                    feedEntry.TIME_BEGIN_COLUMN, feedEntry.TIME_END_COLUMN, feedEntry.PHONE_COLUMN,
                    feedEntry.RELATIVE_COLUMN, feedEntry.COMPANIONS_COLUMN,
                    feedEntry.ENCOUNTER_COLUMN, "DistanceKept", feedEntry.NOTES_COLUMN, "MaskMe",
                    "MaskOthers", feedEntry.VENTILATION_COLUMN
                )
            ) {
                Toast.makeText(
                    requireActivity().applicationContext,
                    getString(R.string.import_fail),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd-HH:mm")
                try {
                    db.execSQL(SQL_DELETE_ENTRIES)
                    db.execSQL(SQL_CREATE_ENTRIES)
                    var nextLine = csvReader.readLine()
                    while (nextLine != null) {
                        val nextLineList = nextLine.split("\t")
                        val type = nextLineList[0]
                        val name = nextLineList[1]
                        val place = nextLineList[2]
                        val beginTime = nextLineList[3]
                        val endTime = nextLineList[4]
                        val phone = nextLineList[5]
                        val relative = nextLineList[6]
                        val companions = nextLineList[7]
                        val encounterType = nextLineList[8]
                        val distanceKept = nextLineList[9]
                        val notes = nextLineList[10]
                        val maskMe = when (nextLineList[11]) {
                            "Yes" -> 1
                            "No" -> 0
                            else -> 0
                        }
                        val maskOther = when (nextLineList[12]) {
                            "Yes" -> 1
                            "No" -> 0
                            else -> 0
                        }
                        val ventilation = nextLineList[13]

                        val values = ContentValues().apply {
                            put(feedEntry.TYPE_COLUMN, type)
                            put(feedEntry.NAME_COLUMN, name)
                            put(feedEntry.PLACE_COLUMN, place)
                            put(feedEntry.TIME_BEGIN_COLUMN, dateFormatter.parse(beginTime).time)
                            put(feedEntry.TIME_END_COLUMN, dateFormatter.parse(endTime).time)
                            put(feedEntry.PHONE_COLUMN, phone)
                            put(feedEntry.RELATIVE_COLUMN, when (relative) {
                                "Yes" -> 1
                                "No" -> 3
                                else -> -1
                            })
                            put(feedEntry.CLOSECONTACT_COLUMN, when (distanceKept) {
                                "Yes" -> 0
                                "No" -> 1
                                else -> -1
                            })
                            put(feedEntry.ENCOUNTER_COLUMN, when (encounterType) {
                                "Indoors" -> 1
                                "Outdoors" -> 3
                                else -> -1
                            })
                            put(feedEntry.COMPANIONS_COLUMN, companions)
                            put(feedEntry.NOTES_COLUMN, notes)
                            put(feedEntry.MASK_COLUMN, 2*maskMe + maskOther)
                            put(feedEntry.VENTILATION_COLUMN, when (ventilation) {
                                "Yes" -> 1
                                "No" -> 0
                                else -> -1
                            })
                        }
                        db?.insert(feedEntry.TABLE_NAME, null, values)
                        nextLine = csvReader.readLine()
                    }
                    Toast.makeText(context, getString(R.string.import_success), Toast.LENGTH_LONG).show()
                    db.close()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, getString(R.string.import_notfound), Toast.LENGTH_LONG)
                        .show()
                }
            }
            csvReader.close()
        }

        private fun createFile() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, "ContactDiary.csv")
                }
                startActivityForResult(intent, EXPORT_DB)
            } else {
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("I am afraid that the Export option is not yet available for " +
                        "Android versions below KitKat (4.4). I appreciate your patience while " +
                        "this is being developed. Feel free to drop me an email at any point.")
                builder.setPositiveButton(android.R.string.ok) { _, _ -> }
                builder.create().show()
            }
        }

        private fun readFile() {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "file/*"
            }
            val mimeTypes = arrayOf(
                "text/csv", "text/comma-separated-values", "text/tab-separated-values"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                intent.putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    mimeTypes
                )
            } else {
                intent.type = mimeTypes.joinToString(separator = "|")
            }
            try {
                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.import_select)),
                    IMPORT_DB
                )
            } catch (ex: ActivityNotFoundException) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(
                    requireActivity().applicationContext,
                    "Please install a File Manager",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
