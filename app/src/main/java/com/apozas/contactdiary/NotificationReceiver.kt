package com.apozas.contactdiary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(NotificationHandler.INTENT_EXTRA_NOTIFICATION)) handleNotification(context)
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) setNotification(context)
    }

    private fun setNotification(context: Context) {
        val alarmHandler = NotificationHandler()
        alarmHandler.scheduleAlarms(context)
    }

    private fun handleNotification(context: Context) {
        val alarmHandler = NotificationHandler()
        alarmHandler.showAlarmNotification(context)
    }
}