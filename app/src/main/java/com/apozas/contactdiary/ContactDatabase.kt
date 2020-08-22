package com.apozas.contactdiary

import android.provider.BaseColumns

class ContactDatabase {
    object ContactDatabase {
        // Table contents are grouped together in an anonymous object.
        object FeedEntry : BaseColumns {
            const val TABLE_NAME = "ContactDB"
            const val TYPE_COLUMN = "Type"
            const val NAME_COLUMN = "Name"
            const val PLACE_COLUMN = "Place"
            const val DATETIME_COLUMN = "Timestamp"
            const val PHONE_COLUMN = "Phone"
            const val RELATIVE_COLUMN = "Relative"
            const val COMPANIONS_COLUMN = "Companions"
            const val CLOSECONTACT_COLUMN = "CloseContact"
            const val ENCOUNTER_COLUMN = "EncounterType"
        }
    }

    companion object {
        const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${ContactDatabase.FeedEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                    "${ContactDatabase.FeedEntry.TYPE_COLUMN} TEXT," +
                    "${ContactDatabase.FeedEntry.NAME_COLUMN} TEXT," +
                    "${ContactDatabase.FeedEntry.PLACE_COLUMN} TEXT," +
                    "${ContactDatabase.FeedEntry.DATETIME_COLUMN} INT," +
                    "${ContactDatabase.FeedEntry.PHONE_COLUMN} TEXT," +
                    "${ContactDatabase.FeedEntry.RELATIVE_COLUMN} TINYINT," +
                    "${ContactDatabase.FeedEntry.COMPANIONS_COLUMN} TEXT," +
                    "${ContactDatabase.FeedEntry.ENCOUNTER_COLUMN} TINYINT," +
                    "${ContactDatabase.FeedEntry.CLOSECONTACT_COLUMN} TINYINT)"
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${ContactDatabase.FeedEntry.TABLE_NAME}"
    }

}