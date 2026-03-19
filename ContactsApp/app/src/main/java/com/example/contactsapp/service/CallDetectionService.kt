package com.example.contactsapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.contactsapp.MainActivity
import com.example.contactsapp.R
import com.example.contactsapp.overlay.OverlayController

/**
 * Foreground service — keeps the process alive so the BroadcastReceiver
 * (CallReceiver) always has a live process to call OverlayController from.
 */
class CallDetectionService : Service() {

    private val CHANNEL_ID = "contacts_app_call_monitor"
    private val NOTIF_ID   = 1

    override fun onCreate() {
        super.onCreate()
        OverlayController.init(applicationContext)
        startForeground(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Call Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows contact info for incoming/outgoing calls" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Contacts App")
            .setContentText("Tap to open")
            .setSmallIcon(R.drawable.ic_call_incoming)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY // restart if killed

    override fun onDestroy() {
        super.onDestroy()
        OverlayController.hide()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
