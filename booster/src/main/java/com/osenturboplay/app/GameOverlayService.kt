package com.osenturboplay.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display

enum class HealthLevel { GOOD, WARN, BAD }

enum class DeviceTier(val label: String) {
    LOW("LOW-END"), MID("MID-RANGE"), HIGH("HIGH-END")
}

data class HealthSnapshot(
    val ramFreeMb: Long,
    val ramFreePercent: Int,
    val ramLevel: HealthLevel,
    val thermalText: String,
    val thermalLevel: HealthLevel,
    val batterySaverOn: Boolean,
    val refreshHz: Int,
    val tier: DeviceTier,
    val tips: List<String>
) {
    val worst: HealthLevel
        get() = if (ramLevel == HealthLevel.BAD || thermalLevel == HealthLevel.BAD) HealthLevel.BAD
        else if (ramLevel == HealthLevel.WARN || thermalLevel == HealthLevel.WARN) HealthLevel.WARN
        else HealthLevel.GOOD
}

/**
 * Reads real device state and gives honest, practical advice for smoother gameplay.
 * It does NOT change game files, memory or FPS. Android controls performance.
 */
object SmoothPlayAdvisor {

    fun detectTier(context: Context): DeviceTier {
        val mem = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mem)
        val gb = mem.totalMem / (1024.0 * 1024.0 * 1024.0)
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            gb < 3.5 || cores <= 4 -> DeviceTier.LOW
            gb < 6.5 || cores <= 6 -> DeviceTier.MID
            else -> DeviceTier.HIGH
        }
    }

    fun snapshot(context: Context): HealthSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val total = mem.totalMem.coerceAtLeast(1L)
        val freeMb = mem.availMem / (1024L * 1024L)
        val freePct = ((mem.availMem * 100L) / total).toInt().coerceIn(0, 100)
        val ramLevel = when {
            mem.lowMemory || freePct < 10 -> HealthLevel.BAD
            freePct < 20 -> HealthLevel.WARN
            else -> HealthLevel.GOOD
        }

        var thermalText = "NOT SUPPORTED"
        var thermalLevel = HealthLevel.GOOD
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> { thermalText = "COOL" }
                PowerManager.THERMAL_STATUS_LIGHT -> { thermalText = "WARM" }
                PowerManager.THERMAL_STATUS_MODERATE -> { thermalText = "HOT"; thermalLevel = HealthLevel.WARN }
                else -> { thermalText = "OVERHEATING"; thermalLevel = HealthLevel.BAD }
            }
        }

        val saver = pm.isPowerSaveMode
        val tier = detectTier(context)
        val hz = try {
            (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate?.toInt() ?: 0
        } catch (_: Exception) { 0 }

        val tips = buildTips(ramLevel, thermalLevel, saver, tier)
        return HealthSnapshot(freeMb, freePct, ramLevel, thermalText, thermalLevel, saver, hz, tier, tips)
    }

    private fun buildTips(
        ram: HealthLevel, thermal: HealthLevel, saver: Boolean, tier: DeviceTier
    ): List<String> {
        val tips = mutableListOf<String>()
        if (thermal == HealthLevel.BAD) tips += "Phone is overheating. Take the case off, stop charging, and rest 5 minutes."
        else if (thermal == HealthLevel.WARN) tips += "Phone is getting hot. Avoid charging while playing."
        if (ram == HealthLevel.BAD) tips += "RAM is very low. Close recent apps manually before playing."
        else if (ram == HealthLevel.WARN) tips += "RAM is getting low. Close unused apps from Recents."
        if (saver) tips += "Battery Saver is ON and can reduce game performance. Turn it off."
        when (tier) {
            DeviceTier.LOW -> tips += "In Free Fire: use Smooth graphics, shadows OFF, and the lowest stable FPS option."
            DeviceTier.MID -> tips += "In Free Fire: Standard graphics with a stable FPS option works best."
            DeviceTier.HIGH -> tips += "Your phone can handle higher graphics. Lower them only if it gets hot."
        }
        tips += "Keep 2 GB+ storage free and turn on Do Not Disturb to avoid popups."
        return tips
    }

    fun summaryLine(s: HealthSnapshot): String =
        "RAM ${s.ramFreeMb} MB (${s.ramFreePercent}%)  •  TEMP ${s.thermalText}" +
            (if (s.batterySaverOn) "  •  SAVER ON" else "")

    fun colorFor(level: HealthLevel): Int = when (level) {
        HealthLevel.GOOD -> Color.rgb(120, 255, 160)
        HealthLevel.WARN -> Color.rgb(255, 205, 75)
        HealthLevel.BAD -> Color.rgb(255, 105, 105)
    }

    // ---- Settings shortcuts (all optional screens, safe if missing) ----

    fun openBatterySaver(c: Context) = open(c, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    fun openStorage(c: Context) = open(c, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    fun openDeveloperOptions(c: Context) = open(c, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    fun openDisplaySettings(c: Context) = open(c, Intent(Settings.ACTION_DISPLAY_SETTINGS))

    private fun open(context: Context, intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) { }
        }
    }
}
