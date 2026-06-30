package com.kama.galaxyquickpanel.panel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.kama.galaxyquickpanel.R
import com.kama.galaxyquickpanel.system.SystemController
import com.kama.galaxyquickpanel.tiles.Tile
import com.kama.galaxyquickpanel.tiles.TileFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Owns the two overlay windows that make up the quick panel:
 *
 *  1. a thin, always-present trigger strip pinned to the top edge — swiping
 *     down on it expands the panel (One Shade style);
 *  2. the expandable panel itself (scrim + One UI styled content) which is
 *     only attached while open.
 */
class QuickPanelController(private val context: Context) {

    private val windowManager = context.getSystemService<WindowManager>()!!
    private val inflater = LayoutInflater.from(context)
    private val sys = SystemController(context)
    private val tiles: List<Tile> = TileFactory.build(sys)

    private var triggerView: View? = null
    private var panelRoot: PanelRootView? = null
    private var panelContent: LinearLayout? = null
    private var isOpen = false

    private val tileViews = HashMap<String, View>()

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.getDefault())

    // region Lifecycle ----------------------------------------------------
    fun attachTrigger() {
        if (triggerView != null) return
        val view = View(context)
        val triggerHeight = context.resources.getDimensionPixelSize(R.dimen.trigger_height)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            triggerHeight,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            // The very top edge is owned by the system status bar (which sits
            // above app overlays), so a strip at y=0 never sees the swipe.
            // Drop it just below the status bar where it can actually receive
            // touches.
            y = statusBarHeight()
        }
        view.setOnTouchListener(PullDownListener { open() })
        windowManager.addView(view, params)
        triggerView = view
    }

    fun detach() {
        close()
        triggerView?.let { runCatching { windowManager.removeView(it) } }
        triggerView = null
    }
    // endregion

    // region Open / close -------------------------------------------------
    fun open() {
        if (isOpen) return
        isOpen = true

        val root = PanelRootView(context).apply {
            onBackPressed = { close() }
            setOnClickListener { close() } // tap on the scrim dismisses
        }
        val content = inflater.inflate(R.layout.view_quick_panel, root, false) as LinearLayout
        root.addView(content)

        bindHeader(content)
        bindTiles(content.findViewById(R.id.tileGrid))
        bindBrightness(content.findViewById(R.id.brightnessSeek))
        bindVolume(content.findViewById(R.id.volumeSeek))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            dimAmount = 0.45f
        }

        windowManager.addView(root, params)
        panelRoot = root
        panelContent = content

        // Slide the content down from above the screen.
        content.post {
            val h = content.height.toFloat()
            content.translationY = -h
            content.animate()
                .translationY(0f)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator(1.4f))
                .start()
        }
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        val root = panelRoot
        val content = panelContent
        if (root == null || content == null) {
            removePanel()
            return
        }
        content.animate()
            .translationY(-content.height.toFloat())
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { removePanel() }
            .start()
    }

    private fun removePanel() {
        panelRoot?.let { runCatching { windowManager.removeView(it) } }
        panelRoot = null
        panelContent = null
        tileViews.clear()
    }
    // endregion

    // region Binding ------------------------------------------------------
    private fun bindHeader(content: View) {
        val now = Date()
        content.findViewById<TextView>(R.id.clockText).text = timeFormat.format(now)
        content.findViewById<TextView>(R.id.dateText).text = dateFormat.format(now)
        content.findViewById<View>(R.id.settingsButton).setOnClickListener {
            sys.openAppSettings()
            close()
        }
    }

    private fun bindTiles(grid: GridLayout) {
        grid.removeAllViews()
        tileViews.clear()
        val columns = grid.columnCount
        tiles.forEach { tile ->
            val tileView = inflater.inflate(R.layout.view_tile, grid, false)
            tileView.findViewById<TextView>(R.id.tileLabel)
                .setText(tile.labelRes)
            tileView.setOnClickListener {
                val shouldClose = tile.onClick()
                if (shouldClose) close() else refreshTile(tile)
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(tileView, lp)
            tileViews[tile.key] = tileView
            refreshTile(tile)
        }
        // Make sure the grid keeps four columns even with eight tiles.
        grid.columnCount = columns
    }

    private fun refreshTile(tile: Tile) {
        val view = tileViews[tile.key] ?: return
        val icon = view.findViewById<ImageView>(R.id.tileIcon)
        val on = tile.isOn()
        icon.setImageResource(tile.iconFor?.invoke() ?: tile.iconRes)
        icon.setBackgroundResource(if (on) R.drawable.bg_tile_on else R.drawable.bg_tile_off)
        val tint = if (on) R.color.tile_icon_on else R.color.tile_icon_off
        icon.setColorFilter(ContextCompat.getColor(context, tint))
    }

    private fun bindBrightness(seek: SeekBar) {
        seek.max = 255
        seek.progress = sys.getBrightness()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) sys.setBrightness(value)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun bindVolume(seek: SeekBar) {
        seek.max = sys.getMaxMediaVolume()
        seek.progress = sys.getMediaVolume()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) sys.setMediaVolume(value)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }
    // endregion

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun statusBarHeight(): Int {
        val res = context.resources
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) res.getDimensionPixelSize(id)
        else (24 * res.displayMetrics.density).toInt()
    }

    /** Detects a downward swipe on the trigger strip and opens the panel. */
    private class PullDownListener(private val onPull: () -> Unit) : View.OnTouchListener {
        private var startY = 0f
        private val threshold = 36f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.rawY - startY > threshold) {
                        onPull()
                        return true
                    }
                }
            }
            return false
        }
    }
}
