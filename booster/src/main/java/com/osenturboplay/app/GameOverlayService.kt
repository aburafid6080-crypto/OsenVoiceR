package com.osenturboplay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.os.Build
import android.os.IBinder
import android.os.StatFs
import android.app.ActivityManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private var panelStatus: TextView? = null
    private var dndButton: TextView? = null

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
            text = "OC\nOSEN"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(8, 13, 25), Color.rgb(0, 220, 210), 28f)
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
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(Color.rgb(8, 13, 25), Color.rgb(0, 220, 210), 24f)
            elevation = dp(14).toFloat()
        }
        val eyebrow = label("OSEN  //  TURBOPLAY", 10f, Color.rgb(0, 225, 215), true)
        val heading = label("GAME CONTROL", 20f, Color.WHITE, true)
        val subheading = label("CORE PANEL  •  LOW DISTRACTION", 9f, Color.rgb(151, 169, 196), true)
        val stats = label(readQuickStats(), 10f, Color.rgb(177, 196, 218), false)
        val fpsInfo = label(readFpsInfo(), 10f, Color.rgb(0, 225, 215), true)
        val status = label("SYSTEM READY  •  DND STATUS CHECK", 9f, Color.rgb(255, 190, 90), true)
        panelStatus = status
        val dnd = action(dndLabel()) { toggleDnd() }.apply {
            background = rounded(Color.rgb(12, 40, 49), Color.rgb(0, 225, 215), 16f)
            setTextColor(Color.rgb(105, 255, 239))
            textSize = 13f
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        dndButton = dnd
        val minimize = action("MINIMIZE  •  KEEP GAME RUNNING") { minimizePanel() }.apply {
            background = rounded(Color.rgb(23, 29, 45), Color.rgb(93, 115, 150), 14f)
            textSize = 10f
        }

        panel.addView(eyebrow)
        panel.addView(heading, spacedParams(dp(4)))
        panel.addView(subheading, spacedParams(dp(2)))
        panel.addView(stats, spacedParams(dp(16)))
        panel.addView(fpsInfo, spacedParams(dp(7)))
        panel.addView(status, spacedParams(dp(12)))
        panel.addView(dnd, spacedParams(dp(12)))
        panel.addView(minimize, spacedParams(dp(8)))

        val width = min(dp(270), resources.displayMetrics.widthPixels - dp(28))
        params = windowParams(width.coerceAtLeast(dp(200)), WindowManager.LayoutParams.WRAP_CONTENT)
        overlayView = panel
        windowManager.addView(panel, params)
        makeDraggable(eyebrow)
    }

    private fun dndLabel(): String {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        ) "DO NOT DISTURB  •  ON" else "DO NOT DISTURB  •  OFF"
    }

    private fun readFpsInfo(): String {
        // 60 FPS is the selected target mode, not a measurement of Free Fire's live FPS.
        val hz = try { windowManager.defaultDisplay.refreshRate.toInt() } catch (_: Exception) { 0 }
        val display = if (hz > 0) "DISPLAY  •  " + hz + " Hz" else "DISPLAY RATE  •  UNKNOWN"
        return "FPS TARGET  •  60     LIVE FPS  •  NOT MEASURED\n" + display
    }

    private fun readQuickStats(): String {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val freeRamMb = memory.availMem / (1024L * 1024L)
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        val freeStorageMb = (stat.availableBytes / (1024L * 1024L))
        return "FREE RAM  ${freeRamMb} MB     FREE STORAGE  ${freeStorageMb} MB"
    }

    private fun toggleDnd() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            showPanelStatus("DND needs Android 6.0 or newer.")
            return
        }
        if (!manager.isNotificationPolicyAccessGranted) {
            showPanelStatus("Allow OSEN TURBOPLAY in the DND access screen, then return and tap again.")
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
                showPanelStatus("Open Android Settings > Special app access > Do Not Disturb access.")
            }
            return
        }
        try {
            val turningOff = manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
            manager.setInterruptionFilter(
                if (turningOff) NotificationManager.INTERRUPTION_FILTER_ALL
                else NotificationManager.INTERRUPTION_FILTER_NONE
            )
            val result = if (turningOff) "DO NOT DISTURB OFF • interruptions restored."
                else "DO NOT DISTURB ON • interruptions silenced."
            showPanelStatus(result)
            dndButton?.text = dndLabel()
        } catch (_: SecurityException) {
            showPanelStatus("Android denied DND access. Check OSEN permission.")
        }
    }

    private fun showPanelStatus(message: String) {
        panelStatus?.text = message
    }

    private fun minimizePanel() {
        val current = overlayView
        if (current != null) {
            try { windowManager.removeView(current) } catch (_: Exception) { }
        }
        overlayView = null
        panelStatus = null
        dndButton = null
        expanded = false
        showBubble()
    }

    private fun action(text: String, click: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 11f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(10), dp(12), dp(10), dp(12))
        background = rounded(Color.rgb(20, 27, 43), Color.rgb(0, 205, 205), 12f)
        setOnClickListener {
            animate().scaleX(0.97f).scaleY(0.97f).setDuration(65).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(90).start()
            }.start()
            Toast.makeText(context, text.toString(), Toast.LENGTH_SHORT).show()
            click()
        }
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
        panelStatus = null
        dndButton = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "osen_game_panel"
        private const val NOTIFICATION_ID = 2107
    }
}
