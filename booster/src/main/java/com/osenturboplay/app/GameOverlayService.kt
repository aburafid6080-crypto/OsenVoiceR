package com.osenturboplay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/**
 * Lightweight optional in-game shortcut panel.
 * It does not alter game memory, graphics, network traffic, or FPS settings.
 */
class GameOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private var expanded = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("OSEN TURBOPLAY")
            .setContentText("Floating game panel is active")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    private fun showBubble() {
        if (overlayView != null) return
        val bubble = TextView(this).apply {
            text = "OSEN\n🐉"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(15, 7, 7), Color.rgb(255, 74, 36), 28f)
            elevation = dp(8).toFloat()
            setOnClickListener { showPanel() }
        }
        params = windowParams(dp(58), dp(58))
        overlayView = bubble
        windowManager.addView(bubble, params)
        makeDraggable(bubble)
    }

    private fun showPanel() {
        val old = overlayView ?: return
        try { windowManager.removeView(old) } catch (_: Exception) { }
        expanded = true

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(15, 7, 7), Color.rgb(255, 74, 36), 18f)
            elevation = dp(10).toFloat()
        }
        val heading = label("🐉 OSEN TURBOPLAY", 15f, Color.rgb(255, 112, 55), true)
        val subheading = label("DRAGON FIRE EDITION", 10f, Color.rgb(255, 74, 36), true)
        val info = label("DRAGON FIRE MODE\nA bold red-and-black look with lightweight controls. Android manages game performance.", 12f, Color.WHITE, false)
        val minimize = action("LOW-LAG MODE") { minimizePanel() }
        val close = action("CLOSE PANEL") { minimizePanel() }
        panel.addView(heading)
        panel.addView(subheading, spacedParams(dp(4)))
        panel.addView(info, spacedParams(dp(12)))
        panel.addView(minimize, spacedParams(dp(10)))
        panel.addView(close, spacedParams(dp(6)))

        val width = min(dp(250), resources.displayMetrics.widthPixels - dp(24))
        params = windowParams(width.coerceAtLeast(dp(190)), WindowManager.LayoutParams.WRAP_CONTENT)
        overlayView = panel
        windowManager.addView(panel, params)
        makeDraggable(heading)
    }

    private fun minimizePanel() {
        val current = overlayView
        if (current != null) {
            try { windowManager.removeView(current) } catch (_: Exception) { }
        }
        overlayView = null
        expanded = false
        showBubble()
    }

    private fun action(text: String, click: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 11f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = rounded(Color.rgb(42, 12, 10), Color.rgb(255, 74, 36), 10f)
        setOnClickListener { click() }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun spacedParams(top: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = top }

    private fun rounded(fill: Int, stroke: Int, radius: Float) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun windowParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width,
        height,
        overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        x = dp(6)
    }

    private fun makeDraggable(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { touchedView, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                        moved = true
                    }
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) touchedView.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Game panel", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        overlayView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "osen_game_panel"
        private const val NOTIFICATION_ID = 2107
    }
}
