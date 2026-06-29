package com.kama.galaxyquickpanel.tiles

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * A single quick-settings tile.
 *
 * @param isOn        current highlight state, evaluated each time the panel opens
 * @param iconFor     optional dynamic icon (e.g. sound mode), defaults to [iconRes]
 * @param onClick     performs the action; returns true if the panel should close
 *                    afterwards (deep-link tiles), false to stay open (toggles)
 */
data class Tile(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val isOn: () -> Boolean,
    val iconFor: (() -> Int)? = null,
    val onClick: () -> Boolean
)
