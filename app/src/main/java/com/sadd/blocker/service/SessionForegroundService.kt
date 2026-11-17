package com.sadd.blocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * Foreground Service سشن فعال — فقط هنگام سشن فعال اجرا می‌شود (فاز ۵ کامل می‌شود).
 * اعلان، باقی‌مانده سشن را نشان می‌دهد. بدون سشن فعال، سرویس متوقف است.
 */
class SessionForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "blocker_session"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("سد — سشن فعال است")
            .setContentText("بلاک در طول سشن فعال است")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        // TODO (فاز ۵): تیک هر ثانیه + توقف خودکار در پایان سشن.
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "سشن بلاک",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
