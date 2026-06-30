package com.kama.galaxyquickpanel

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kama.galaxyquickpanel.databinding.ActivityMainBinding
import com.kama.galaxyquickpanel.service.QuickPanelService
import com.kama.galaxyquickpanel.util.Permissions
import com.kama.galaxyquickpanel.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { updateUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.btnOverlay.setOnClickListener { requestOverlay() }
        binding.btnWrite.setOnClickListener { requestWriteSettings() }
        binding.btnNotif.setOnClickListener { requestNotifications() }
        binding.btnToggleService.setOnClickListener { toggleService() }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    // region Permission requests ------------------------------------------
    private fun requestOverlay() {
        if (Permissions.canDrawOverlays(this)) return
        // Android 13+ blocks this toggle for sideloaded apps ("restricted
        // settings" / Enhanced Confirmation Mode). The settings screen opens
        // fine but the switch is greyed out until the user allows restricted
        // settings from the app-info overflow menu — so explain that first.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restricted_title)
                .setMessage(R.string.restricted_message)
                .setPositiveButton(R.string.restricted_open_appinfo) { _, _ ->
                    // App-info is where "Allow restricted settings" lives.
                    startFirstResolvable(appDetailsIntent())
                }
                .setNeutralButton(R.string.restricted_open_overlay) { _, _ ->
                    openOverlaySettings()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            openOverlaySettings()
        }
    }

    private fun openOverlaySettings() {
        // OEM ROMs vary: some don't resolve the package-scoped intent, some only
        // open the global list. Try the most specific screen first, then fall
        // back, and finally guide the user to do it by hand.
        val opened = startFirstResolvable(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ),
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
            appDetailsIntent()
        )
        if (!opened) {
            Toast.makeText(this, R.string.overlay_manual_hint, Toast.LENGTH_LONG).show()
        }
    }

    private fun requestWriteSettings() {
        if (Permissions.canWriteSettings(this)) return
        val opened = startFirstResolvable(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            ),
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
            appDetailsIntent()
        )
        if (!opened) {
            Toast.makeText(this, R.string.write_manual_hint, Toast.LENGTH_LONG).show()
        }
    }

    private fun appDetailsIntent() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName")
    )

    /** Launches the first intent that an Activity can actually handle. */
    private fun startFirstResolvable(vararg intents: Intent): Boolean {
        for (intent in intents) {
            try {
                startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // try the next fallback
            }
        }
        return false
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }
    // endregion

    private fun toggleService() {
        if (prefs.serviceEnabled) {
            QuickPanelService.stop(this)
            prefs.serviceEnabled = false
        } else {
            if (!Permissions.allGranted(this)) {
                Toast.makeText(this, R.string.need_permissions, Toast.LENGTH_SHORT).show()
                return
            }
            QuickPanelService.start(this)
            prefs.serviceEnabled = true
        }
        updateUi()
    }

    private fun updateUi() {
        markGranted(binding.btnOverlay, Permissions.canDrawOverlays(this))
        markGranted(binding.btnWrite, Permissions.canWriteSettings(this))
        markGranted(binding.btnNotif, Permissions.hasNotificationPermission(this))

        val running = prefs.serviceEnabled
        binding.btnToggleService.setText(
            if (running) R.string.disable_panel else R.string.enable_panel
        )
        binding.statusText.setText(
            if (running) R.string.status_running else R.string.status_stopped
        )
    }

    private fun markGranted(button: Button, granted: Boolean) {
        button.isEnabled = !granted
        button.setText(if (granted) R.string.perm_granted else R.string.perm_grant)
        button.alpha = if (granted) 0.5f else 1f
    }
}
