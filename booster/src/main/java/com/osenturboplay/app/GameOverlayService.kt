package com.osenturboplay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
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

        val gold = Color.rgb(255, 190, 55)
        val brightGold = Color.rgb(255, 220, 115)
        val muted = Color.rgb(174, 164, 143)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 8, 13), Color.rgb(24, 19, 13), Color.rgb(8, 9, 14))
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(2), gold)
            }
            elevation = dp(20).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val identity = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        identity.addView(label("♛  OSEN TURBOPLAY", 16f, brightGold, true))
        identity.addView(label("DRAGON CORE  •  GAMING PERFORMANCE", 8f, muted, true), spacedParams(dp(3)))
        val live = label("● READY", 8f, Color.rgb(120, 255, 160), true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(7), dp(9), dp(7))
            background = rounded(Color.rgb(24, 39, 23), Color.rgb(113, 185, 70), 16f)
        }
        header.addView(identity, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(live)

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(112, 78, 27))
        }

        val fpsCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(38, 29, 13), Color.rgb(18, 17, 17))
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), gold)
            }
        }
        val fpsInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fpsInfo.addView(label("FPS TARGET", 8f, muted, true))
        fpsInfo.addView(label("60 FPS", 28f, brightGold, true), spacedParams(dp(1)))
        fpsInfo.addView(label("LIVE FPS • NOT MEASURED", 7f, Color.rgb(185, 177, 159), true), spacedParams(dp(1)))
        fpsCard.addView(fpsInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        fpsCard.addView(label(readDisplayRate(), 9f, brightGold, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = rounded(Color.rgb(25, 22, 16), gold, 12f)
        })

        val resourcesTitle = label("DEVICE RESOURCES", 8f, gold, true)
        val resourceRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        resourceRow.addView(goldInfoCard("RAM", readFreeRam(), "AVAILABLE"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        resourceRow.addView(goldInfoCard("STORAGE", readFreeStorage(), "FREE SPACE"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })

        val hardwareTitle = label("HARDWARE IDENTIFICATION", 8f, gold, true)
        val hardware = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(10), dp(11), dp(10))
            background = rounded(Color.rgb(17, 16, 17), Color.rgb(112, 78, 27), 14f)
        }
        hardware.addView(label("CPU  •  ${readCpuName().take(32)}", 9f, Color.WHITE, true))
        hardware.addView(label("${Runtime.getRuntime().availableProcessors()} LOGICAL CORES", 7f, muted, true), spacedParams(dp(3)))
        hardware.addView(label("GPU  •  ${readGpuRenderer().take(38)}", 8f, brightGold, true), spacedParams(dp(7)))

        val status = label("SYSTEM READY  •  DND STATUS CHECK", 8f, Color.rgb(255, 204, 112), true)
        panelStatus = status
        val dnd = action(dndLabel()) { toggleDnd() }.apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(80, 53, 13), Color.rgb(36, 28, 15))
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), brightGold)
            }
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        dndButton = dnd
        val minimize = action("⌄   MINIMIZE PANEL") { minimizePanel() }.apply {
            background = rounded(Color.rgb(20, 19, 19), Color.rgb(105, 78, 35), 12f)
            textSize = 9f
            setTextColor(brightGold)
        }

        panel.addView(header)
        panel.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(11); bottomMargin = dp(11) })
        panel.addView(fpsCard)
        panel.addView(resourcesTitle, spacedParams(dp(13)))
        panel.addView(resourceRow, spacedParams(dp(7)))
        panel.addView(hardwareTitle, spacedParams(dp(13)))
        panel.addView(hardware, spacedParams(dp(7)))
        panel.addView(status, spacedParams(dp(11)))
        panel.addView(dnd, spacedParams(dp(6)))
        panel.addView(minimize, spacedParams(dp(7)))

        val width = min(dp(316), resources.displayMetrics.widthPixels - dp(24))
        params = windowParams(width.coerceAtLeast(dp(220)), WindowManager.LayoutParams.WRAP_CONTENT)
        overlayView = panel
        windowManager.addView(panel, params)
        makeDraggable(identity)
    }

    private fun goldInfoCard(title: String, value: String, footer: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(30, 25, 17), Color.rgb(13, 14, 17))
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(126, 88, 30))
            }
            addView(label(title, 8f, Color.rgb(195, 164, 101), true))
            addView(label(value, 16f, Color.WHITE, true), spacedParams(dp(5)))
            addView(label(footer, 7f, Color.rgb(255, 195, 74), true), spacedParams(dp(2)))
        }
    }

    private fun readFreeRam(): String {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return "${info.availMem / (1024L * 1024L)} MB"
    }

    private fun readFreeStorage(): String {
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        return "${stat.availableBytes / (1024L * 1024L)} MB"
    }

    private fun readCpuName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: Build.HARDWARE
        } else Build.HARDWARE
    }

    private fun readDisplayRate(): String {
        val hz = try { windowManager.defaultDisplay.refreshRate.toInt() } catch (_: Exception) { 0 }
        return if (hz > 0) "DISPLAY\\n${hz} Hz" else "DISPLAY\\n—"
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

    private fun readHardwareInfo(): String {
        val chip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }
        val cores = Runtime.getRuntime().availableProcessors()
        val gpu = readGpuRenderer()
        return "CPU / CHIP  •  ${chip.take(28)}  •  ${cores} CORES\\nGPU  •  ${gpu.take(34)}"
    }

    private fun readGpuRenderer(): String {
        var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
        var context: android.opengl.EGLContext = EGL14.EGL_NO_CONTEXT
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return "NOT AVAILABLE"
            val versions = IntArray(2)
            if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) return "NOT AVAILABLE"
            val configAttrs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, 4,
                EGL14.EGL_SURFACE_TYPE, 1,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttrs, 0, configs, 0, 1, count, 0) || count[0] == 0) {
                return "NOT AVAILABLE"
            }
            val contextAttrs = intArrayOf(0x3098, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
            if (context == EGL14.EGL_NO_CONTEXT) return "NOT AVAILABLE"
            val surfaceAttrs = intArrayOf(0x3057, 1, 0x3056, 1, EGL14.EGL_NONE)
            surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttrs, 0)
            if (surface == EGL14.EGL_NO_SURFACE) return "NOT AVAILABLE"
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return "NOT AVAILABLE"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
            val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
            return (renderer ?: vendor ?: "NOT AVAILABLE").replace(Regex("\\s+"), " ").trim()
        } catch (_: Exception) {
            return "NOT AVAILABLE"
        } finally {
            try { EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT) } catch (_: Exception) { }
            try { if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface) } catch (_: Exception) { }
            try { if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context) } catch (_: Exception) { }
            try { if (display != EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(display) } catch (_: Exception) { }
        }
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
