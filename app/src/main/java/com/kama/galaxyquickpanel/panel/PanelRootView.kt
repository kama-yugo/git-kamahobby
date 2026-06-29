package com.kama.galaxyquickpanel.panel

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * Root container for the expanded panel window. Because the panel window is
 * focusable (so deep-link taps work), it receives the hardware/gesture BACK
 * key — we intercept it to collapse the panel instead of leaking it.
 */
class PanelRootView(context: Context) : FrameLayout(context) {

    var onBackPressed: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onBackPressed?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
