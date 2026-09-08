package com.example.kivo.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kivo.MainActivity
import com.example.kivo.R

object KivoNotificationManager {

    private const val GROUP_KEY = "kivo_group_"
    private const val GROUP_SUMMARY_SUFFIX = "_summary"
    private const val REQUEST_CODE_OPEN = 1000
    private const val REQUEST_CODE_CLEAR = 2000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationCategory.MUSIC to ("Música de Kivo" to "Novedades musicales, canciones y favoritos"),
            NotificationCategory.GAMES to ("Juegos de Kivo" to "Retos, récords y recompensas"),
            NotificationCategory.SOCIAL to ("Social de Kivo" to "Mensajes, seguidores e interacciones"),
            NotificationCategory.SYSTEM to ("Kivo general" to "Avisos de la aplicación"),
            NotificationCategory.DOWNLOADS to ("Descargas de Kivo" to "Progreso de descargas y contenido disponible")
        )
        val defaultChannels = mapOf(
            NotificationCategory.MUSIC to NotificationManager.IMPORTANCE_DEFAULT,
            NotificationCategory.GAMES to NotificationManager.IMPORTANCE_HIGH,
            NotificationCategory.SOCIAL to NotificationManager.IMPORTANCE_HIGH,
            NotificationCategory.SYSTEM to NotificationManager.IMPORTANCE_DEFAULT,
            NotificationCategory.DOWNLOADS to NotificationManager.IMPORTANCE_LOW
        )
        for ((category, meta) in channels) {
            val channel = NotificationChannel(
                category.channelId,
                meta.first,
                defaultChannels.getValue(category)
            ).apply {
                description = meta.second
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, notification: KivoNotification) {
        ensureChannels(context)
        val notificationId = notificationId(notification.id)
        val groupKey = GROUP_KEY + notification.category.name

        val builder = NotificationCompat.Builder(context, notification.category.channelId)
            .setSmallIcon(R.drawable.ic_stat_kivo)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setContentIntent(openPendingIntent(context, notification))
            .setAutoCancel(true)
            .setGroup(groupKey)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(notification.timestamp)
            .clearActions()
            .setPriority(priorityOf(notification.priority))

        if (!NotificationSettingsStore.values.value.sound) builder.setSound(null)
        if (!NotificationSettingsStore.values.value.vibration) builder.setVibrate(null)

        val settings = NotificationSettingsStore.values.value
        if (!settings.lockscreenContent) {
            val publicVersion = NotificationCompat.Builder(context, notification.category.channelId)
                .setSmallIcon(R.drawable.ic_stat_kivo)
                .setContentTitle("Kivo")
                .setContentText(notification.category.label)
                .setPriority(priorityOf(notification.priority))
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            builder.setPublicVersion(publicVersion)
        }

        when (notification.destination) {
            NotificationDestination.CHAT, NotificationDestination.PLAYER -> {
                builder.addAction(
                    R.drawable.ic_stat_kivo,
                    "Ver",
                    openPendingIntent(context, notification)
                )
            }
            else -> Unit
        }

        builder.addAction(
            0,
            if (notification.isRead) "Eliminar" else "Marcar leída",
            notifyActionPendingIntent(context, notification, markRead = !notification.isRead)
        )

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            showGroupSummary(context, notification)
        }
    }

    fun cancel(context: Context, notificationIdKey: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(notificationId(notificationIdKey))
        }
    }

    private fun showGroupSummary(context: Context, notification: KivoNotification) {
        val groupKey = GROUP_KEY + notification.category.name
        val summary = NotificationCompat.Builder(context, notification.category.channelId)
            .setSmallIcon(R.drawable.ic_stat_kivo)
            .setContentTitle("Kivo")
            .setContentText("Noticias de ${notification.category.label}")
            .setStyle(NotificationCompat.InboxStyle())
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(openGroupIntent(context, notification.category))
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                groupKey.hashCode(),
                summary
            )
        }
    }

    private fun notificationId(key: String): Int = key.hashCode()

    private fun priorityOf(priority: NotificationPriority): Int = when (priority) {
        NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
    }

    private fun openPendingIntent(context: Context, notification: KivoNotification): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("kivo_destination", notification.destination.name)
            .putExtra("kivo_destination_id", notification.destinationId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN + notificationId(notification.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openGroupIntent(context: Context, category: NotificationCategory): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("kivo_destination", NotificationDestination.NOTIFICATIONS.name)
            .putExtra("kivo_category", category.name)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN + GROUP_KEY.hashCode() + category.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifyActionPendingIntent(
        context: Context,
        notification: KivoNotification,
        markRead: Boolean
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .putExtra("kivo_action", if (markRead) "mark_read" else "delete")
            .putExtra("kivo_notification_id", notification.id)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CLEAR + notificationId(notification.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}