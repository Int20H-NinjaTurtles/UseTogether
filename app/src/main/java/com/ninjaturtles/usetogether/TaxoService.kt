package com.ninjaturtles.usetogether

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat


class TaxoService : Service() {

    companion object {
        const val FINDING_ACTION = "action.find"
        const val APPROVE_ACTION = "action.approve"
        const val ON_WAY_ACTION = "action.onway"
        const val ARRIVED_ACTION = "action.arrived"
    }

    private val CHANNEL_ID = "ForegroundServiceChannel"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val data = intent.action
        val text = when (data) {
            FINDING_ACTION -> "Searching for a driver"
            APPROVE_ACTION -> "You driver is found"
            ON_WAY_ACTION -> "Driver is on the way"
            ARRIVED_ACTION -> "Driver has arrived"
            else -> {
                stopForeground(true)
                stopSelf()
                ""
            }
        }
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Taxo")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(android.R.drawable.arrow_up_float)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

}