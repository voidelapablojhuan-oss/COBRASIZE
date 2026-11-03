package com.cobrasize.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class ActiveService : Service() {

    companion object {
        const val ACTION_STOP = "com.cobrasize.app.STOP"
        const val ACTION_STOPPED = "com.cobrasize.app.STOPPED"
        private const val CHANNEL_ID = "cobrasize_active"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "COBRASIZE ativo", NotificationManager.IMPORTANCE_LOW)
        )

        val stopPending = PendingIntent.getService(
            this, 0,
            Intent(this, ActiveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("COBRASIZE ativo")
            .setContentText("Toque para abrir. Use Desativar para encerrar.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(0, "Desativar", stopPending)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Tweaker.restore(this)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
