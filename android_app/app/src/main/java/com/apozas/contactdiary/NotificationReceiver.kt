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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(NotificationHandler.INTENT_EXTRA_NOTIFICATION)) handleNotification(context)
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) setNotification(context)
    }

    private fun setNotification(context: Context) {
        val notificationHandler = NotificationHandler()
        notificationHandler.scheduleNotification(context)
    }

    private fun handleNotification(context: Context) {
        val notificationHandler = NotificationHandler()
        notificationHandler.showNotification(context)
    }
}