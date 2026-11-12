package com.osenturboplay.app

import android.app.Notification
import android.widget.ScrollView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.SeekBar
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Handler
import android.os.Looper
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
import android.widget.EditText
import android.text.InputType
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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
    private var ramValueView: TextView? = null
    private var storageValueView: TextView? = null
    private var popupView: View? = null
    private val popupHandler = Handler(Looper.getMainLooper())
    private val popupDismiss = Runnable { popupView?.let { view -> try { windowManager.removeView(view) } catch (_: Exception) { }; popupView = null } }
    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private var freeFireWasForeground = false
    private var outsideGameSince = 0L
    private val autoCloseCheck = object : Runnable {
        override fun run() {
            if (!hasUsageAccess()) {
                autoCloseHandler.postDelayed(this, 2000)
                return
            }
            val now = System.currentTimeMillis()
            val events = (getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)
                .queryEvents(now - 20000L, now)
            val event = UsageEvents.Event()
            var latestForegroundPackage: String? = null
            var latestForegroundTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val foreground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                } else {
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }
                if (foreground && event.timeStamp >= latestForegroundTime) {
                    latestForegroundTime = event.timeStamp
                    latestForegroundPackage = event.packageName
                }
            }

            val isFreeFire = latestForegroundPackage == FREE_FIRE_PACKAGE ||
                latestForegroundPackage == FREE_FIRE_MAX_PACKAGE
            val isTemporarySystemUi = latestForegroundPackage in setOf(
                packageName,
                "com.android.systemui",
                "com.android.permissioncontroller",
                "com.google.android.permissioncontroller"
            )

            if (isFreeFire) {
                freeFireWasForeground = true
                outsideGameSince = 0L
            } else if (freeFireWasForeground && latestForegroundPackage != null && !isTemporarySystemUi) {
                // Usage Events can briefly report the launcher/system UI while an overlay is tapped.
                // Only minimize after another app remains foreground for 10 seconds.
                if (outsideGameSince == 0L) outsideGameSince = now
                if (expanded && getPanelPrefs().getBoolean("auto_hide", true) && now - outsideGameSince >= 10000L) {
                    freeFireWasForeground = false
                    outsideGameSince = 0L
                    minimizePanel("OSEN")
                }
            } else if (isTemporarySystemUi) {
                outsideGameSince = 0L
            }
            autoCloseHandler.postDelayed(this, 2000)
        }
    }

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
        autoCloseHandler.postDelayed(autoCloseCheck, 1500)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    private fun showBubble(labelText: String = "OSEN") {
        if (overlayView != null) return
        val isBoost = labelText == "BOOST"
        val bubble = TextView(this).apply {
            text = labelText
            textSize = if (isBoost) 10f else 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(
                if (isBoost) Color.rgb(10, 35, 58) else Color.rgb(4, 24, 40),
                if (isBoost) Color.rgb(255, 196, 45) else Color.rgb(70, 205, 255),
                28f
            )
            elevation = dp(8).toFloat()
            setOnClickListener {
                animate().scaleX(0.78f).scaleY(0.78f).alpha(0.72f).setDuration(75)
                    .withEndAction {
                        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(110)
                            .withEndAction { showPanel() }.start()
                    }.start()
            }
        }
        params = windowParams(if (isBoost) dp(76) else dp(58), if (isBoost) dp(42) else dp(58))
        overlayView = bubble
        windowManager.addView(bubble, params)
        makeDraggable(bubble)
    }

    private fun showPanel() {
        val old = overlayView ?: return
        val savedX = params.x
        val savedY = params.y
        try { windowManager.removeView(old) } catch (_: Exception) { }
        expanded = true

        val gold = getPanelPrefs().getInt("theme_accent_color", Color.rgb(45, 190, 255))
        val brightGold = getPanelPrefs().getInt("theme_accent_light", Color.rgb(155, 230, 255))
        val muted = Color.rgb(130, 190, 215)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(3, 15, 28), Color.rgb(5, 28, 48), Color.rgb(2, 13, 24))
            ).apply {
                cornerRadius = dp(17).toFloat()
                setStroke(dp(2), gold)
            }
            elevation = dp(20).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val identity = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        identity.addView(label("✦  OSEN TURBOPLAY", 13f, brightGold, true))
        identity.addView(label("PRO PANEL  •  DRAG HERE TO MOVE", 6f, muted, true), spacedParams(dp(1)))
        val live = label("● READY", 8f, Color.rgb(120, 255, 160), true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = rounded(Color.rgb(24, 39, 23), Color.rgb(113, 185, 70), 16f)
        }
        header.addView(identity, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val profile = getPerformanceProfile()
        val premiumToggle = label(profile.displayName + "  ▾", 9f, Color.rgb(255, 218, 105), true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(8), dp(9), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(125, 82, 8), Color.rgb(65, 39, 3))
            ).apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(255, 218, 105))
            }
        }
        header.addView(premiumToggle)
        header.addView(live)

        val premiumOptions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(8, 39, 62), Color.rgb(3, 19, 34))
            ).apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.rgb(255, 205, 75))
            }
        }
        PerformanceProfile.values().forEach { mode ->
            val option = label(
                (if (mode == profile) "✓  " else "◇  ") + mode.displayName + "  •  " + mode.description,
                8f,
                if (mode == profile) Color.rgb(255, 218, 105) else Color.WHITE,
                true
            ).apply {
                setPadding(dp(10), dp(9), dp(10), dp(9))
                isClickable = true
                setOnClickListener {
                    if (mode == PerformanceProfile.PREMIUM) {
                        requestPremiumUnlock(premiumToggle, premiumOptions)
                    } else {
                        selectPerformanceProfile(mode, premiumToggle, premiumOptions)
                    }
                }
            }
            premiumOptions.addView(option)
        }
        premiumToggle.setOnClickListener {
            premiumOptions.visibility = if (premiumOptions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(35, 160, 225))
        }

        val fpsCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(8, 48, 75), Color.rgb(3, 22, 38))
            ).apply {
                cornerRadius = dp(13).toFloat()
                setStroke(dp(1), gold)
            }
        }
        val fpsInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fpsInfo.addView(label("YOUR PHONE", 7f, muted, true))
        fpsInfo.addView(label(readPhoneName(), 15f, brightGold, true), spacedParams(dp(1)))
        fpsInfo.addView(label("DEVICE MODEL", 6f, Color.rgb(130, 190, 215), true), spacedParams(dp(1)))
        fpsCard.addView(fpsInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        fpsCard.addView(label(readDisplayRate(), 9f, brightGold, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = rounded(Color.rgb(5, 34, 54), gold, 12f)
        })

        val resourcesTitle = label("DEVICE STATUS", 6f, gold, true)
        val resourceRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        resourceRow.addView(goldInfoCard("RAM", readFreeRam(), "AVAILABLE"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        resourceRow.addView(goldInfoCard("STORAGE", readFreeStorage(), "FREE SPACE"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })

        val hardwareTitle = label("DEVICE HARDWARE", 6f, gold, true)
        val hardware = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(Color.rgb(4, 27, 45), Color.rgb(35, 160, 225), 14f)
        }
        hardware.addView(label("CPU  •  ${readCpuName().take(26)}", 8f, Color.WHITE, true))
        hardware.addView(label("${Runtime.getRuntime().availableProcessors()} CORES  •  GPU  •  ${readGpuRenderer().take(22)}", 7f, muted, true), spacedParams(dp(2)))
        

        val status = label("SYSTEM READY  •  DND STATUS CHECK", 7f, Color.rgb(120, 220, 255), true)
        panelStatus = status
        val dnd = action(dndLabel(), dndLabel()) { toggleDnd() }.apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(35, 165, 225), Color.rgb(5, 45, 73))
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), brightGold)
            }
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        dndButton = dnd
        updateDndButtonVisual(false)
        val bottomActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val back = action("‹  BACK", "BACK").apply {
            background = rounded(Color.rgb(5, 39, 63), Color.rgb(45, 190, 255), 13f)
            textSize = 11f
            setTextColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener { animateButtonPress(this) { minimizePanel() } }
        }
        val boost = action("⚡  BOOST", "BOOST").apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(255, 225, 115), Color.rgb(245, 176, 35), Color.rgb(190, 112, 12))
            ).apply {
                cornerRadius = dp(13).toFloat()
                setStroke(dp(2), Color.rgb(255, 239, 175))
            }
            elevation = dp(8).toFloat()
            textSize = 11f
            setTextColor(Color.rgb(35, 25, 5))
            isClickable = true
            isFocusable = true
            setOnClickListener { animateButtonPress(this) { showActionPopup("BOOST") } }
        }
        bottomActions.addView(back, LinearLayout.LayoutParams(0, dp(39), 1f).apply { marginEnd = dp(5) })
        bottomActions.addView(boost, LinearLayout.LayoutParams(0, dp(39), 1f).apply { marginStart = dp(5) })

        panel.addView(header)
        panel.addView(premiumOptions, spacedParams(dp(4)))
        panel.addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { topMargin = dp(5); bottomMargin = dp(5) })
        panel.addView(fpsCard)
        panel.addView(resourcesTitle, spacedParams(dp(6)))
        panel.addView(resourceRow, spacedParams(dp(4)))
        panel.addView(hardwareTitle, spacedParams(dp(6)))
        panel.addView(hardware, spacedParams(dp(4)))

        val quickSettings = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val quickTiles = mutableListOf<TextView>()
        var gamingGlowOn = getPanelPrefs().getBoolean("gaming_glow_on", false)
        fun updateGamingGlow() {
            quickTiles.firstOrNull()?.let { gamingTile ->
                gamingTile.background = GradientDrawable().apply {
                    setColor(if (gamingGlowOn) Color.rgb(8, 76, 112) else Color.rgb(5, 39, 63))
                    cornerRadius = dp(11).toFloat()
                    setStroke(dp(if (gamingGlowOn) 2 else 1), if (gamingGlowOn) Color.rgb(125, 235, 255) else Color.rgb(45, 150, 195))
                }
                gamingTile.elevation = dp(if (gamingGlowOn) 9 else 1).toFloat()
                gamingTile.setTextColor(if (gamingGlowOn) Color.rgb(190, 245, 255) else Color.WHITE)
                gamingTile.alpha = 1f
            }
        }
        listOf("🎮 GAMING", "⚙ PANEL", "✦ THEME").forEachIndexed { index, title ->
            val tile = action(title, if (index == 0) "OSEN GAME BOOST" else title) {
                when (index) {
                    0 -> {
                        gamingGlowOn = !gamingGlowOn
                        getPanelPrefs().edit().putBoolean("gaming_glow_on", gamingGlowOn).apply()
                        updateGamingGlow()
                        showPanelStatus(if (gamingGlowOn) "GAMING LIGHT ON" else "GAMING LIGHT OFF")
                    }
                    1 -> {
                        showPanelSettings()
                        showPanelStatus("PANEL SETTINGS OPENED")
                    }
                    else -> {
                        showThemeSettings()
                    }
                }
            }.apply {
                textSize = 8f
                setPadding(dp(3), dp(9), dp(3), dp(9))
                background = rounded(Color.rgb(5, 39, 63), Color.rgb(45, 190, 255), 11f)
            }
            quickTiles.add(tile)
            quickSettings.addView(tile, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                if (index > 0) marginStart = dp(4)
            })
        }
        updateGamingGlow()
        panel.addView(quickSettings, spacedParams(dp(5)))
        panel.addView(status, spacedParams(dp(5)))
        panel.addView(dnd, spacedParams(dp(4)))
        val passwordSettingsButton = action("🔐  PREMIUM DEVICE ID", "DEVICE ID") {
            showPremiumPasswordSettings()
        }.apply {
            textSize = 8f
            setPadding(dp(8), dp(9), dp(8), dp(9))
            background = rounded(Color.rgb(8, 39, 63), Color.rgb(255, 205, 75), 12f)
        }
        panel.addView(passwordSettingsButton, spacedParams(dp(4)))
        panel.addView(bottomActions, spacedParams(dp(4)))

        val prefs = getPanelPrefs()
        val width = min(dp(prefs.getInt("panel_size", 286)), resources.displayMetrics.widthPixels - dp(20)).coerceAtLeast(dp(210))
        params = windowParams(width, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            x = if (prefs.getString("panel_position", "LEFT") == "RIGHT") {
                (resources.displayMetrics.widthPixels - width - dp(6)).coerceAtLeast(0)
            } else savedX
            y = savedY
        }
        panel.alpha = prefs.getInt("panel_transparency", 100).coerceIn(60, 100) / 100f
        overlayView = panel
        windowManager.addView(panel, params)
        makeDraggable(header)
    }


    private fun showThemeSettings() {
        val prefs = getPanelPrefs()
        val names = arrayOf("Sky Blue", "Purple", "Emerald", "Rose", "Amber")
        val accents = intArrayOf(
            Color.rgb(45, 190, 255),
            Color.rgb(177, 105, 255),
            Color.rgb(55, 210, 145),
            Color.rgb(255, 105, 135),
            Color.rgb(235, 170, 55)
        )
        val lightAccents = intArrayOf(
            Color.rgb(155, 230, 255),
            Color.rgb(220, 180, 255),
            Color.rgb(165, 255, 215),
            Color.rgb(255, 190, 205),
            Color.rgb(255, 225, 155)
        )
        val current = prefs.getInt("theme_accent_color", accents[0])
        val selected = accents.indexOf(current).coerceAtLeast(0)
        val dialog = AlertDialog.Builder(this)
            .setTitle("✦  CUSTOMIZE COLORS")
            .setSingleChoiceItems(names, selected) { dialog, which ->
                prefs.edit()
                    .putInt("theme_accent_color", accents[which])
                    .putInt("theme_accent_light", lightAccents[which])
                    .apply()
                dialog.dismiss()
                // Rebuild the expanded panel so the selected accent is visible immediately.
                val x = params.x
                val y = params.y
                minimizePanel()
                params.x = x
                params.y = y
                showPanel()
                showPanelStatus("THEME COLOR SAVED • ${names[which].uppercase()}")
            }
            .setNegativeButton("CLOSE", null)
            .create()
        // Set the overlay window type before showing; setting it afterwards can
        // cause Android to reject/recreate the dialog and collapse the floating panel.
        dialog.window?.setType(overlayWindowType())
        dialog.show()
    }

    private fun getPanelPrefs() = getSharedPreferences("osen_panel_settings", MODE_PRIVATE)

    private fun showPanelSettings() {
        val prefs = getPanelPrefs()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(4))
        }
        val scroll = ScrollView(this).apply { addView(content) }

        fun section(text: String) {
            content.addView(TextView(this).apply {
                this.text = text
                setTextColor(Color.rgb(65, 185, 245))
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(10), 0, dp(4))
            })
        }
        fun seekSetting(title: String, key: String, minValue: Int, maxValue: Int, defaultValue: Int, suffix: String): SeekBar {
            val value = prefs.getInt(key, defaultValue).coerceIn(minValue, maxValue)
            val labelView = TextView(this).apply {
                text = "$title: $value$suffix"
                setTextColor(Color.WHITE)
                textSize = 13f
            }
            content.addView(labelView)
            val seek = SeekBar(this).apply {
                max = maxValue - minValue
                progress = value - minValue
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        labelView.text = "$title: ${progress + minValue}$suffix"
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
            }
            content.addView(seek)
            return seek
        }

        section("PANEL SIZE")
        val sizeSeek = seekSetting("Width", "panel_size", 240, 320, 286, " dp")
        section("PANEL POSITION")
        val positionGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val left = RadioButton(this).apply { text = "Left"; setTextColor(Color.WHITE); id = View.generateViewId() }
        val right = RadioButton(this).apply { text = "Right"; setTextColor(Color.WHITE); id = View.generateViewId() }
        positionGroup.addView(left)
        positionGroup.addView(right)
        if (prefs.getString("panel_position", "LEFT") == "RIGHT") right.isChecked = true else left.isChecked = true
        content.addView(positionGroup)
        section("PANEL TRANSPARENCY")
        val transparencySeek = seekSetting("Opacity", "panel_transparency", 60, 100, 100, "%")
        val autoHide = Switch(this).apply {
            text = "Auto Hide after leaving Free Fire"
            setTextColor(Color.WHITE)
            textSize = 13f
            isChecked = prefs.getBoolean("auto_hide", true)
        }
        content.addView(autoHide)
        val dialog = AlertDialog.Builder(this)
            .setTitle("⚙ PANEL SETTINGS")
            .setView(scroll)
            .create()

        fun applyPanelSettings(widthDp: Int, position: String, opacity: Int, hide: Boolean) {
            prefs.edit()
                .putInt("panel_size", widthDp)
                .putString("panel_position", position)
                .putInt("panel_transparency", opacity)
                .putBoolean("auto_hide", hide)
                .apply()
            overlayView?.let { current ->
                val newWidth = min(dp(widthDp), resources.displayMetrics.widthPixels - dp(20)).coerceAtLeast(dp(210))
                params.width = newWidth
                params.x = if (position == "RIGHT") {
                    (resources.displayMetrics.widthPixels - newWidth - dp(6)).coerceAtLeast(0)
                } else dp(6)
                current.alpha = opacity / 100f
                try { windowManager.updateViewLayout(current, params) } catch (_: Exception) { }
            }
        }

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        }
        fun actionButton(label: String, fill: Int, stroke: Int, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(4), dp(13), dp(4), dp(13))
                background = rounded(fill, stroke, 22f)
                isClickable = true
                setOnClickListener { onClick() }
            }
        }
        val resetButton = actionButton("RESET", Color.rgb(42, 48, 61), Color.rgb(65, 185, 245)) {
            sizeSeek.progress = 286 - 240
            left.isChecked = true
            transparencySeek.progress = 100 - 60
            autoHide.isChecked = true
            applyPanelSettings(286, "LEFT", 100, true)
            showPanelStatus("PANEL SETTINGS RESET TO DEFAULT")
            showPremiumPopup("PANEL SETTINGS RESET")
        }
        val cancelButton = actionButton("CANCEL", Color.rgb(42, 48, 61), Color.rgb(105, 125, 150)) {
            dialog.dismiss()
        }
        val saveButton = actionButton("SAVE", Color.rgb(38, 92, 220), Color.rgb(65, 185, 245)) {
            val selectedPosition = if (right.isChecked) "RIGHT" else "LEFT"
            applyPanelSettings(sizeSeek.progress + 240, selectedPosition, transparencySeek.progress + 60, autoHide.isChecked)
            showPanelStatus("PANEL SETTINGS SAVED")
            dialog.dismiss()
        }
        listOf(resetButton, cancelButton, saveButton).forEachIndexed { index, button ->
            actionRow.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                if (index > 0) marginStart = dp(7)
            })
        }
        content.addView(actionRow)
        dialog.window?.setType(overlayWindowType())
        dialog.show()
    }

    private fun goldInfoCard(title: String, value: String, footer: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(8), dp(9), dp(8))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(10, 48, 75), Color.rgb(5, 28, 45))
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(50, 175, 235))
            }
            addView(label(title, 8f, Color.rgb(115, 215, 255), true))
            val valueView = label(value, 16f, Color.WHITE, true)
            if (title == "RAM") ramValueView = valueView
            if (title == "STORAGE") storageValueView = valueView
            addView(valueView, spacedParams(dp(5)))
            addView(label(footer, 7f, Color.rgb(105, 220, 255), true), spacedParams(dp(2)))
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        return appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        ) == android.app.AppOpsManager.MODE_ALLOWED
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

    private fun refreshBoostInfo() {
        // Refreshes displayed device information only; Android remains responsible for game scheduling.
        ramValueView?.text = readFreeRam()
        storageValueView?.text = readFreeStorage()
        showPanelStatus("BOOST CHECK COMPLETE • DEVICE STATS REFRESHED")
    }

    private fun readCpuName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: Build.HARDWARE
        } else Build.HARDWARE
    }

    private fun readPhoneName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val phone = if (manufacturer.isNotBlank() && !model.startsWith(manufacturer, ignoreCase = true)) {
            "$manufacturer $model"
        } else model.ifBlank { manufacturer }
        return phone.ifBlank { "Android Phone" }.take(30)
    }

    private fun readDisplayRate(): String {
        val hz = try { windowManager.defaultDisplay.refreshRate.toInt() } catch (_: Exception) { 0 }
        return if (hz > 0) "DISPLAY\\n${hz} Hz" else "DISPLAY\\n—"
    }

    private fun isDndEnabled(): Boolean = GameDndController.isEnabled(this)

    private fun dndLabel(): String {
        return if (isDndEnabled()) "◉  DO NOT DISTURB  •  ON"
        else "◉  DO NOT DISTURB  •  OFF"
    }

    private fun updateDndButtonVisual(animateChange: Boolean) {
        val button = dndButton ?: return
        val enabled = isDndEnabled()
        val border = if (enabled) Color.rgb(145, 235, 255) else Color.rgb(75, 190, 245)
        val gradient = if (enabled) {
            intArrayOf(Color.rgb(0, 190, 220), Color.rgb(12, 115, 180), Color.rgb(3, 44, 70))
        } else {
            intArrayOf(Color.rgb(8, 68, 105), Color.rgb(4, 42, 68), Color.rgb(3, 27, 45))
        }
        val applyVisual = {
            button.text = dndLabel()
            button.background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, gradient
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(if (enabled) 2 else 1), border)
            }
            button.elevation = dp(if (enabled) 10 else 4).toFloat()
            button.alpha = 1f
            button.scaleX = 1f
            button.scaleY = 1f
        }
        if (!animateChange) {
            applyVisual()
            return
        }
        button.animate().cancel()
        button.animate().scaleX(1.045f).scaleY(1.045f).alpha(0.72f).setDuration(110)
            .withEndAction {
                applyVisual()
                button.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170).start()
            }.start()
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
        showPanelStatus(GameDndController.toggle(this))
        updateDndButtonVisual(true)
    }

    private fun showPanelStatus(message: String) {
        panelStatus?.text = message
    }

    private fun showActionPopup(buttonName: String) {
        if (buttonName == "BOOST") {
            refreshBoostInfo()
            showPremiumPopup("BOOST • DEVICE STATUS UPDATED")
        } else {
            showPremiumPopup("$buttonName • READY")
            showPanelStatus("$buttonName • READY")
        }
    }

    // Branded, non-interactive popup shown above the overlay panel.
    private fun showPremiumPopup(message: String) {
        popupHandler.removeCallbacks(popupDismiss)
        popupView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        val popup = TextView(this).apply {
            text = "✦  OSEN TURBOPLAY  ✦\n$message"
            textSize = 10f
            gravity = Gravity.CENTER
            maxWidth = dp(260)
            minWidth = dp(190)
            setLineSpacing(dp(2).toFloat(), 1.05f)
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(145, 232, 255), Color.rgb(38, 165, 235), Color.rgb(5, 88, 160))
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), Color.rgb(205, 247, 255))
            }
            elevation = dp(12).toFloat()
        }
        val popupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(58)
        }
        try {
            windowManager.addView(popup, popupParams)
            popupView = popup
            popup.alpha = 0f
            popup.translationY = -dp(8).toFloat()
            popup.animate().alpha(1f).translationY(0f).setDuration(170).start()
            popupHandler.postDelayed(popupDismiss, 1850)
        } catch (_: Exception) {
            popupView = null
        }
    }

    private fun minimizePanel(bubbleLabel: String = "OSEN") {
        val current = overlayView
        val savedX = params.x
        val savedY = params.y
        if (current != null) {
            try { windowManager.removeView(current) } catch (_: Exception) { }
        }
        overlayView = null
        panelStatus = null
        dndButton = null
        ramValueView = null
        storageValueView = null
        expanded = false
        showBubble(bubbleLabel)
        params.x = savedX
        params.y = savedY
        overlayView?.let { bubble ->
            try { windowManager.updateViewLayout(bubble, params) } catch (_: Exception) { }
            bubble.alpha = 0f
            bubble.scaleX = 0.72f
            bubble.scaleY = 0.72f
            bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
            if (bubbleLabel == "BACK" && bubble is TextView) {
                bubble.postDelayed({
                    if (overlayView === bubble) {
                        bubble.animate().alpha(0.65f).setDuration(100).withEndAction {
                            bubble.text = "OSEN"
                            bubble.animate().alpha(1f).setDuration(140).start()
                        }.start()
                    }
                }, 650)
            }
        }
    }

    private enum class PerformanceProfile(
        val displayName: String,
        val description: String
    ) {
        PREMIUM("♛ PREMIUM", "Full panel animations"),
        STANDARD("STANDARD", "Normal, smooth animations"),
        BALANCED("BALANCED", "Reduced animations for lighter panel use")
    }

    private fun getPerformanceProfile(): PerformanceProfile {
        val prefs = getPanelPrefs()
        val saved = prefs.getString("performance_profile", null)
        if (saved != null) {
            val stored = try { PerformanceProfile.valueOf(saved) } catch (_: IllegalArgumentException) {
                PerformanceProfile.STANDARD
            }
            // A previously selected Premium profile remains locked after the service restarts.
            return if (stored == PerformanceProfile.PREMIUM && !premiumUnlockedThisSession) {
                PerformanceProfile.STANDARD
            } else stored
        }
        // Choose a conservative first-run default using Android's reported device capacity.
        val memory = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        val cores = Runtime.getRuntime().availableProcessors()
        val detected = when {
            memory.totalMem < 6L * 1024L * 1024L * 1024L || cores <= 6 -> PerformanceProfile.BALANCED
            else -> PerformanceProfile.STANDARD
        }
        prefs.edit().putString("performance_profile", detected.name).apply()
        return detected
    }

    // Premium password is stored only as a salted PBKDF2 hash in private app storage.
    // Premium access is verified by the owner's server-side settings and device approval.
    // Unlock lasts only for this floating-panel session.
    private var premiumUnlockedThisSession = false

    private fun getPremiumDeviceId(): String {
        val identityPrefs = getSharedPreferences("osen_device_identity", MODE_PRIVATE)
        val existing = identityPrefs.getString("device_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = java.util.UUID.randomUUID().toString()
        identityPrefs.edit().putString("device_id", created).apply()
        return created
    }

    private fun requestPremiumUnlock(toggle: TextView, options: LinearLayout) {
        if (premiumUnlockedThisSession) {
            selectPerformanceProfile(PerformanceProfile.PREMIUM, toggle, options)
            return
        }
        val deviceId = getPremiumDeviceId()
        val passwordInput = EditText(this).apply {
            hint = "Admin-provided Premium password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        addPasswordInput(content, passwordInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle("🔒  PREMIUM ACCESS")
            .setMessage("Enter the Premium password. This device must also be approved by the app owner.")
            .setView(content)
            .setPositiveButton("VERIFY & UNLOCK", null)
            .setNeutralButton("COPY DEVICE ID", null)
            .setNegativeButton("CANCEL", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OSEN Device ID", deviceId))
                Toast.makeText(this, "Device ID copied.", Toast.LENGTH_SHORT).show()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val entered = passwordInput.text?.toString().orEmpty()
                if (entered.length < 8 || entered.length > 128) {
                    passwordInput.error = "Enter the valid Premium password"
                    return@setOnClickListener
                }
                val verifyButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                verifyButton.isEnabled = false
                verifyButton.text = "CHECKING…"
                Thread {
                    var resultCode = "NETWORK_ERROR"
                    var unlocked = false
                    var connection: java.net.HttpURLConnection? = null
                    try {
                        connection = (java.net.URL("$PREMIUM_API_URL/api/premium/verify").openConnection()
                            as java.net.HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 10000
                            readTimeout = 10000
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        }
                        val payload = org.json.JSONObject().put("device_id", deviceId).put("password", entered)
                        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                        val responseStream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                        val responseText = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        val response = org.json.JSONObject(responseText)
                        unlocked = response.optBoolean("ok", false)
                        resultCode = response.optString("code", if (unlocked) "OK" else "DENIED")
                    } catch (_: Exception) {
                        resultCode = "NETWORK_ERROR"
                    } finally {
                        connection?.disconnect()
                    }
                    popupHandler.post {
                        if (unlocked) {
                            premiumUnlockedThisSession = true
                            dialog.dismiss()
                            selectPerformanceProfile(PerformanceProfile.PREMIUM, toggle, options)
                            showPanelStatus("PREMIUM UNLOCKED • OWNER APPROVED")
                        } else {
                            verifyButton.isEnabled = true
                            verifyButton.text = "VERIFY & UNLOCK"
                            val message = when (resultCode) {
                                "NOT_APPROVED" -> "This device is not approved yet. Send this Device ID to the owner:\n$deviceId"
                                "PREMIUM_DISABLED" -> "Premium is currently disabled by the owner."
                                "WRONG_PASSWORD" -> "Incorrect Premium password."
                                "PASSWORD_NOT_SET" -> "The owner has not set the Premium password yet."
                                "INVALID_PASSWORD" -> "Enter a valid Premium password."
                                else -> "Could not verify Premium. Check your internet connection and try again."
                            }
                            dialog.setMessage(message)
                        }
                    }
                }.start()
            }
        }
        dialog.window?.setType(overlayWindowType())
        dialog.show()
    }

    private fun addPasswordInput(container: LinearLayout, input: EditText) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f))
        val visibility = TextView(this).apply {
            text = "SHOW"
            textSize = 10f
            setTextColor(Color.rgb(255, 205, 75))
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                val start = input.selectionStart.coerceAtLeast(0)
                val visible = input.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0
                input.inputType = InputType.TYPE_CLASS_TEXT or (if (visible)
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
                    else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
                input.setSelection(start.coerceAtMost(input.text?.length ?: 0))
                text = if (visible) "SHOW" else "HIDE"
            }
        }
        row.addView(visibility, LinearLayout.LayoutParams(dp(58), dp(48)))
        container.addView(row)
    }

    private fun showPremiumPasswordSettings() {
        val deviceId = getPremiumDeviceId()
        val dialog = AlertDialog.Builder(this)
            .setTitle("OSEN TURBOPLAY • DEVICE ID")
            .setMessage("Share this ID with the app owner to request Premium approval:\n\n$deviceId\n\nOnly the owner can approve this device and change the Premium password.")
            .setPositiveButton("COPY DEVICE ID") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OSEN Device ID", deviceId))
                Toast.makeText(this, "Device ID copied.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CLOSE", null)
            .create()
        dialog.window?.setType(overlayWindowType())
        dialog.show()
    }

    private fun selectPerformanceProfile(
        mode: PerformanceProfile,
        toggle: TextView,
        options: LinearLayout
    ) {
        getPanelPrefs().edit().putString("performance_profile", mode.name).apply()
        toggle.text = mode.displayName + "  ▾"
        options.visibility = View.GONE
        options.removeAllViews()
        PerformanceProfile.values().forEach { item ->
            val selected = item == mode
            options.addView(label(
                (if (selected) "✓  " else "◇  ") + item.displayName + "  •  " + item.description,
                8f,
                if (selected) Color.rgb(255, 218, 105) else Color.WHITE,
                true
            ).apply {
                setPadding(dp(10), dp(9), dp(10), dp(9))
                setOnClickListener {
                    if (item == PerformanceProfile.PREMIUM) {
                        requestPremiumUnlock(toggle, options)
                    } else {
                        selectPerformanceProfile(item, toggle, options)
                    }
                }
            })
        }
        showPanelStatus("PROFILE ACTIVE: ${mode.displayName} • UI ANIMATIONS ONLY")
    }

    private fun profileAnimationScale(): Float {
        return when (getPerformanceProfile()) {
            PerformanceProfile.PREMIUM -> 1.0f
            PerformanceProfile.STANDARD -> 0.8f
            PerformanceProfile.BALANCED -> 0.5f
        }
    }

    private fun animateButtonPress(view: View, onFinished: () -> Unit) {
        val scale = profileAnimationScale()
        view.animate().cancel()
        if (scale <= 0f) {
            onFinished()
            return
        }
        view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.82f).setDuration((75 * scale).toLong())
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration((105 * scale).toLong())
                    .withEndAction { onFinished() }.start()
            }.start()
    }

    private fun action(text: String, popupText: String = text, onClick: (() -> Unit)? = null) = TextView(this).apply {
        this.text = text
        textSize = 11f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(dp(10), dp(12), dp(10), dp(12))
        background = rounded(Color.rgb(20, 27, 43), Color.rgb(0, 205, 205), 12f)
        setOnClickListener {
            val animationScale = profileAnimationScale()
            if (animationScale > 0f) {
                animate().scaleX(0.97f).scaleY(0.97f).setDuration((65 * animationScale).toLong()).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration((90 * animationScale).toLong()).start()
                }.start()
            }
            showPremiumPopup(popupText)
            onClick?.invoke()
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
        ramValueView = null
        storageValueView = null
        autoCloseHandler.removeCallbacks(autoCloseCheck)
        popupHandler.removeCallbacks(popupDismiss)
        popupView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        popupView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        autoCloseHandler.removeCallbacks(autoCloseCheck)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "osen_game_panel"
        private const val NOTIFICATION_ID = 2107
        private const val PREMIUM_API_URL = "https://osen-voice-r.vercel.app"
        private const val FREE_FIRE_PACKAGE = "com.dts.freefireth"
        private const val FREE_FIRE_MAX_PACKAGE = "com.dts.freefiremax"
    }
}
