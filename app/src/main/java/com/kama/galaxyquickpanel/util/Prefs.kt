package com.kama.galaxyquickpanel.util

import android.content.Context

/** Small wrapper around SharedPreferences for the few flags we persist. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var serviceEnabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val NAME = "galaxy_quick_panel"
        private const val KEY_ENABLED = "service_enabled"
    }
}
