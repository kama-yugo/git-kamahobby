package com.kama.galaxyquickpanel.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kama.galaxyquickpanel.util.Permissions
import com.kama.galaxyquickpanel.util.Prefs

/** Restarts the panel on boot if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs(context).serviceEnabled && Permissions.canDrawOverlays(context)) {
            // Starting a specialUse FGS from boot can be rejected on some OEM
            // builds; swallow it so the receiver never crashes.
            runCatching { QuickPanelService.start(context) }
        }
    }
}
