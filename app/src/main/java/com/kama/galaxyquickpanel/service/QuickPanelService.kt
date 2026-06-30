package com.kama.galaxyquickpanel.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.kama.galaxyquickpanel.MainActivity
import com.kama.galaxyquickpanel.R
import com.kama.galaxyquickpanel.panel.QuickPanelController
import com.kama.galaxyquickpanel.util.Permissions
import com.kama.galaxyquickpanel.util.Prefs

/**
 * Foreground service that keeps the trigger overlay alive so the panel can be
 * pulled down at any time, even over other apps.
 */
class QuickPanelService : Service() {

    private var controller: QuickPanelController? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        // Bail out cleanly if the overlay permission was revoked while we slept.
        if (!Permissions.canDrawOverlays(this)) {
            Prefs(this).serviceEnabled = false
            stopSelf()
            return
        }

        controller = QuickPanelController(this).also { it.attachTrigger() }
        Prefs(this).serviceEnabled = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Tapping the notification (or its action) opens the panel directly —
        // a reliable trigger that doesn't depend on the status-bar swipe.
        if (intent?.action == ACTION_OPEN_PANEL) {
            controller?.open()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        controller?.detach()
        controller = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        createChannel()
        val openPanel = PendingIntent.getService(
            this, 1,
            Intent(this, QuickPanelService::class.java).setAction(ACTION_OPEN_PANEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_panel)
            .setContentIntent(openPanel) // tap the notification to open the panel
            .addAction(R.drawable.ic_panel, getString(R.string.notif_open_panel), openPanel)
            .addAction(R.drawable.ic_settings, getString(R.string.notif_open_settings), openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "quick_panel_service"
        private const val NOTIF_ID = 1001
        const val ACTION_OPEN_PANEL = "com.kama.galaxyquickpanel.OPEN_PANEL"

        fun start(context: Context) {
            val intent = Intent(context, QuickPanelService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, QuickPanelService::class.java))
        }
    }
}
