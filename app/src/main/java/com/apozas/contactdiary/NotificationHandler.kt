package com.apozas.contactdiary

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
    fun scheduleAlarms(context: Context) {
        disableAllAlarms(context)
        enableAlarms(context)
    }

    private fun enableAlarms(context: Context) {
        enableAlarm(context)
    }

    private fun enableAlarm(context: Context) {
        setRepeatingAlarm(context)
    }

    private fun setRepeatingAlarm(context: Context) {
        val alarmPendingIntent = getPendingAlarmIntent(context)
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val reminderTime = preferences.getString("reminder_time", "21:00").toString().split(":")
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, reminderTime[0].toInt())
        cal.set(Calendar.MINUTE, reminderTime[1].toInt())
        cal.set(Calendar.SECOND, 0)
        if (cal.time.compareTo(Date()) < 0) cal.add(Calendar.DAY_OF_MONTH, 1)
        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            alarmPendingIntent
        )
    }

    private fun getWeekdaysPendingAlarmIntent(context: Context): List<PendingIntent> {
        val dayOfWeeks = intArrayOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )
        val pendingIntents: MutableList<PendingIntent> = LinkedList()
        for (dayOfWeek in dayOfWeeks) pendingIntents.add(getPendingAlarmIntent(context))
        return pendingIntents
    }

    private fun getPendingAlarmIntent(context: Context): PendingIntent {
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        alarmIntent.putExtra(INTENT_EXTRA_NOTIFICATION, true)
        return PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun disableAllAlarms(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntents = getWeekdaysPendingAlarmIntent(context)
        for (pendingIntent in pendingIntents) alarmMgr.cancel(pendingIntent)
    }

    fun showAlarmNotification(context: Context) {
        val notifyIntent = Intent(context, MainActivity::class.java)
        val notifyPendingIntent =
            PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val mBuilder = NotificationCompat.Builder(context, "covidDiary_notify")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "covidDiary_notify",
                "COVID Diary notification channel",
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