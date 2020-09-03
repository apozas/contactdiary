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

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import java.util.*


class NotificationHandler {
    fun scheduleNotification(context: Context) {
        disableNotification(context)
        setRepeatingNotification(context)
    }

    private fun setRepeatingNotification(context: Context) {
        val alarmPendingIntent = getPendingNotificationIntent(context)
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val reminderTime = preferences.getString("reminder_time", "21:00").toString().split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, reminderTime[0].toInt())
        cal.set(Calendar.MINUTE, reminderTime[1].toInt())
        cal.set(Calendar.SECOND, 0)
        if (cal.time < Date()) cal.add(Calendar.DAY_OF_MONTH, 1)
        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            alarmPendingIntent
        )
    }

    private fun getPendingNotificationIntent(context: Context): PendingIntent {
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        alarmIntent.putExtra(INTENT_EXTRA_NOTIFICATION, true)
        return PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun disableNotification(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingNotificationIntent(context)
        alarmMgr.cancel(pendingIntent)
    }

    fun showNotification(context: Context) {
        val notifyIntent = Intent(context, MainActivity::class.java)
        val notifyPendingIntent =
            PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val mBuilder = NotificationCompat.Builder(context, "contactDiary_notify")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "contactDiary_notify",
                "Contact Diary notification channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = mBuilder.setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setAutoCancel(true)
            .setContentIntent(notifyPendingIntent)
            .build()
        val mNotifyMgr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotifyMgr.notify(ALARM_NOTIFICATION_ID, notification)
    }

    companion object {
        const val INTENT_EXTRA_NOTIFICATION = "notificationIntent"
        private const val ALARM_NOTIFICATION_ID = 0x01
    }
}