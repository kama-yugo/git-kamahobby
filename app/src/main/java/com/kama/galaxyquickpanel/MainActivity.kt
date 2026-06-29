package com.kama.galaxyquickpanel

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun requestWriteSettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
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
