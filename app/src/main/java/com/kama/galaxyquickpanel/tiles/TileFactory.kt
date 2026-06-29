package com.kama.galaxyquickpanel.tiles

import android.media.AudioManager
import com.kama.galaxyquickpanel.R
import com.kama.galaxyquickpanel.system.SystemController

/** Builds the default Galaxy-style tile set backed by [SystemController]. */
object TileFactory {

    fun build(sys: SystemController): List<Tile> = listOf(
        Tile(
            key = "wifi",
            labelRes = R.string.tile_wifi,
            iconRes = R.drawable.ic_wifi,
            isOn = sys::isWifiOn,
            onClick = { sys.openWifiSettings(); true }
        ),
        Tile(
            key = "bluetooth",
            labelRes = R.string.tile_bluetooth,
            iconRes = R.drawable.ic_bluetooth,
            isOn = sys::isBluetoothOn,
            onClick = { sys.openBluetoothSettings(); true }
        ),
        Tile(
            key = "flashlight",
            labelRes = R.string.tile_flashlight,
            iconRes = R.drawable.ic_flashlight,
            isOn = sys::isFlashlightOn,
            onClick = { sys.toggleFlashlight(); false }
        ),
        Tile(
            key = "rotate",
            labelRes = R.string.tile_rotate,
            iconRes = R.drawable.ic_rotate,
            isOn = sys::isAutoRotateOn,
            onClick = { sys.toggleAutoRotate(); false }
        ),
        Tile(
            key = "sound",
            labelRes = R.string.tile_sound,
            iconRes = R.drawable.ic_sound,
            isOn = { sys.getRingerMode() == AudioManager.RINGER_MODE_NORMAL },
            iconFor = {
                when (sys.getRingerMode()) {
                    AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_silent
                    AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_vibrate
                    else -> R.drawable.ic_sound
                }
            },
            onClick = { sys.cycleRingerMode(); false }
        ),
        Tile(
            key = "airplane",
            labelRes = R.string.tile_airplane,
            iconRes = R.drawable.ic_airplane,
            isOn = sys::isAirplaneModeOn,
            onClick = { sys.openAirplaneSettings(); true }
        ),
        Tile(
            key = "location",
            labelRes = R.string.tile_location,
            iconRes = R.drawable.ic_location,
            isOn = sys::isLocationOn,
            onClick = { sys.openLocationSettings(); true }
        ),
        Tile(
            key = "settings",
            labelRes = R.string.settings,
            iconRes = R.drawable.ic_settings,
            isOn = { false },
            onClick = { sys.openAppSettings(); true }
        )
    )
}
