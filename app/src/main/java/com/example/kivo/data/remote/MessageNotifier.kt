package com.example.kivo.data.remote

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
import com.example.kivo.data.models.Message

object MessageNotifier {
    private const val CHANNEL_ID = "kivo_chat_messages"

    fun show(context: Context, message: Message) {
        val conversationId = message.conversationId ?: return
        ensureChannel(context)
        val senderName = message.senderName.ifBlank { "Alguien" }
        val body = if (message.type == "image") "📷 Foto" else message.text.ifBlank { "Mensaje" }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kivo_logo)
            .setContentTitle(senderName)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openConversation(context, conversationId, message.senderId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(conversationId.hashCode(), notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensajes de Kivo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificaciones de nuevos mensajes en el chat" }
            manager.createNotificationChannel(channel)
        }
    }

    private fun openConversation(context: Context, conversationId: String, otherUserId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("conversationId", conversationId)
            .putExtra("otherUserId", otherUserId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}