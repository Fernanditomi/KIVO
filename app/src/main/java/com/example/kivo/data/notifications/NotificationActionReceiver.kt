package com.example.kivo.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("kivo_action") ?: return
        val notificationId = intent.getStringExtra("kivo_notification_id") ?: return
        when (action) {
            "mark_read" -> NotificationStore.markRead(notificationId)
            "delete" -> NotificationStore.delete(notificationId)
        }
        KivoNotificationManager.cancel(context, notificationId)
    }
}