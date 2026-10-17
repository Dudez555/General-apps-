package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
  private const val CHANNEL_MESSAGES = "wifi_messages_channel"
  private const val CHANNEL_CALLS = "wifi_calls_channel"
  private const val PREFS_NAME = "notification_prefs"

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val msgChannel = NotificationChannel(
        CHANNEL_MESSAGES,
        "Incoming Messages",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts for incoming Wi-Fi messages and group chats"
        enableVibration(true)
      }

      val callChannel = NotificationChannel(
        CHANNEL_CALLS,
        "Incoming Calls",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts for video calls and screen sharing invitations"
        enableVibration(true)
      }

      notificationManager.createNotificationChannel(msgChannel)
      notificationManager.createNotificationChannel(callChannel)
    }
  }

  fun areNotificationsEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("notifications_enabled", true)
  }

  fun setNotificationsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean("notifications_enabled", enabled)
      .apply()
  }

  fun areCallAlertsEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean("call_alerts_enabled", true)
  }

  fun setCallAlertsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean("call_alerts_enabled", enabled)
      .apply()
  }

  fun showIncomingMessageNotification(
    context: Context,
    senderName: String,
    messageSnippet: String,
    notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
  ) {
    if (!areNotificationsEnabled(context)) return

    try {
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(senderName)
        .setContentText(messageSnippet)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .addAction(android.R.drawable.ic_menu_send, "Open Chat", pendingIntent)

      with(NotificationManagerCompat.from(context)) {
        notify(notificationId, builder.build())
      }
    } catch (_: SecurityException) {
      // Handled if runtime POST_NOTIFICATIONS is not granted
    }
  }
}
