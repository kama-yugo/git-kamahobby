package com.kama.galaxyquickpanel.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Central place to query the runtime / special permissions the panel needs. */
object Permissions {

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun canWriteSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Everything required before the service can be usefully started. */
    fun allGranted(context: Context): Boolean =
        canDrawOverlays(context) &&
            canWriteSettings(context) &&
            hasNotificationPermission(context)
}
