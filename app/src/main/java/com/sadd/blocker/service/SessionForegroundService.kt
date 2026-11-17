package com.sadd.blocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.sadd.blocker.MainActivity
import com.sadd.blocker.R

/**
 * Foreground Service سشن فعال — فقط هنگام سشن فعال اجرا می‌شود (فاز ۵ کامل می‌شود).
 * اعلان، باقی‌مانده سشن را نشان می‌دهد. بدون سشن فعال، سرویس متوقف است.
 *
 * ایمنی (H-7): START_NOT_STICKY است تا سیستم بعد از kill، سرویس را بی‌دلیل
 * زنده نکند؛ با اکشن ACTION_STOP، سرویس خودش را متوقف می‌کند.
 * لمس اعلان، اپ را باز می‌کند (L-8).
 */
class SessionForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "blocker_session"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.sadd.blocker.action.STOP_SESSION"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("سد — سشن فعال است")
            .setContentText("بلاک در طول سشن فعال است")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        // TODO (فاز ۵): تیک هر ثانیه + توقف خودکار در پایان سشن.
        return START_NOT_STICKY
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
