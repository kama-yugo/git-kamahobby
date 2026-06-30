package com.kama.galaxyquickpanel.system

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Wraps the real Android system calls behind each tile / slider.
 *
 * On modern Android many radios (Wi-Fi, Bluetooth, airplane, location) can no
 * longer be toggled silently by a third-party app, so for those we read the
 * current state for the tile highlight and deep-link into the relevant system
 * settings screen on tap — exactly like One Shade and similar apps do.
 */
class SystemController(private val context: Context) {

    private val audio = context.getSystemService<AudioManager>()!!
    private val cameraManager = context.getSystemService<CameraManager>()

    // region Flashlight ---------------------------------------------------
    private var torchOn = false
    private val torchCameraId: String? by lazy {
        runCatching {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun isFlashlightOn(): Boolean = torchOn

    fun toggleFlashlight() {
        val id = torchCameraId ?: return
        torchOn = !torchOn
        runCatching { cameraManager?.setTorchMode(id, torchOn) }
            .onFailure { torchOn = !torchOn }
    }
    // endregion

    // region Brightness ---------------------------------------------------
    /** 0..255 */
    fun getBrightness(): Int = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(128)

    fun setBrightness(value: Int) {
        if (!Settings.System.canWrite(context)) return
        // Switch off auto-brightness so the manual value takes effect.
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            value.coerceIn(1, 255)
        )
    }
    // endregion

    // region Media volume -------------------------------------------------
    fun getMaxMediaVolume(): Int = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    fun getMediaVolume(): Int = audio.getStreamVolume(AudioManager.STREAM_MUSIC)

    fun setMediaVolume(value: Int) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    }
    // endregion

    // region Auto-rotate --------------------------------------------------
    fun isAutoRotateOn(): Boolean = runCatching {
        Settings.System.getInt(
            context.contentResolver, Settings.System.ACCELEROMETER_ROTATION
        ) == 1
    }.getOrDefault(false)

    fun toggleAutoRotate() {
        if (!Settings.System.canWrite(context)) return
        val next = if (isAutoRotateOn()) 0 else 1
        Settings.System.putInt(
            context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, next
        )
    }
    // endregion

    // region Sound mode (normal / vibrate / silent) -----------------------
    fun getRingerMode(): Int = audio.ringerMode

    /** Cycles normal -> vibrate -> silent -> normal. Falls back gracefully
     *  when silent isn't allowed without DND access. */
    fun cycleRingerMode() {
        val next = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        runCatching { audio.ringerMode = next }
    }
    // endregion

    // region Read-only states for deep-link tiles -------------------------
    @Suppress("DEPRECATION")
    fun isWifiOn(): Boolean =
        context.getSystemService<WifiManager>()?.isWifiEnabled == true

    fun isBluetoothOn(): Boolean =
        context.getSystemService<BluetoothManager>()?.adapter?.isEnabled == true

    fun isAirplaneModeOn(): Boolean =
        Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) == 1

    fun isLocationOn(): Boolean =
        context.getSystemService<LocationManager>()?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLocationEnabled
            else it.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } == true
    // endregion

    // region Deep links into system settings ------------------------------
    fun openWifiSettings() = startPanelOrSettings(
        Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS
    )

    fun openBluetoothSettings() = startSettings(Settings.ACTION_BLUETOOTH_SETTINGS)

    fun openAirplaneSettings() = startSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)

    fun openLocationSettings() = startSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    fun openAppSettings() = startSettings(Settings.ACTION_SETTINGS)

    private fun startSettings(action: String) {
        runCatching {
            context.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Settings.Panel intents (API 29+) open a slim inline panel; fall back to
     *  the full settings screen if unavailable. */
    private fun startPanelOrSettings(panelAction: String, fallback: String) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) panelAction else fallback
        startSettings(action)
    }
    // endregion
}
