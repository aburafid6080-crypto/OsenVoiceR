package com.osenturboplay.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Uses Android's Priority DND filter: alarms, media and system sounds remain allowed,
 * while calls and message/other notification interruptions are suppressed.
 * The user's previous DND policy and filter are restored when this app turns DND off.
 */
object GameDndController {
    private const val PREFS = "osen_game_dnd_state"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_FILTER = "previous_filter"
    private const val KEY_CATEGORIES = "previous_categories"
    private const val KEY_CALL_SENDERS = "previous_call_senders"
    private const val KEY_MESSAGE_SENDERS = "previous_message_senders"
    private const val KEY_SUPPRESSED = "previous_suppressed_effects"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun toggle(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return "Do Not Disturb requires Android 6.0 or newer."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
            return "Allow OSEN TURBOPLAY DND access, then return and tap again."
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return try {
            if (prefs.getBoolean(KEY_ENABLED, false)) {
                val previousFilter = prefs.getInt(KEY_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
                val previousCategories = prefs.getInt(KEY_CATEGORIES, 0)
                val previousCallSenders = prefs.getInt(KEY_CALL_SENDERS, 0)
                val previousMessageSenders = prefs.getInt(KEY_MESSAGE_SENDERS, 0)
                val previousSuppressed = prefs.getInt(KEY_SUPPRESSED, 0)
                manager.setNotificationPolicy(
                    buildPolicy(previousCategories, previousCallSenders, previousMessageSenders, previousSuppressed)
                )
                manager.setInterruptionFilter(previousFilter)
                prefs.edit().clear().apply()
                "DO NOT DISTURB OFF • previous phone settings restored."
            } else {
                val oldPolicy = manager.notificationPolicy
                val oldFilter = manager.currentInterruptionFilter
                prefs.edit()
                    .putInt(KEY_FILTER, oldFilter)
                    .putInt(KEY_CATEGORIES, oldPolicy.priorityCategories)
                    .putInt(KEY_CALL_SENDERS, oldPolicy.priorityCallSenders)
                    .putInt(KEY_MESSAGE_SENDERS, oldPolicy.priorityMessageSenders)
                    .putInt(
                        KEY_SUPPRESSED,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) oldPolicy.suppressedVisualEffects else 0
                    )
                    .apply()

                val allowed = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM
                val suppressVisuals = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST or
                        NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE
                } else 0

                manager.setNotificationPolicy(buildPolicy(allowed, 0, 0, suppressVisuals))
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                prefs.edit().putBoolean(KEY_ENABLED, true).apply()
                "DO NOT DISTURB ON • calls and message alerts silenced; alarms and media remain."
            }
        } catch (_: SecurityException) {
            prefs.edit().clear().apply()
            "Android denied DND access. Check OSEN TURBOPLAY's DND permission."
        } catch (_: Exception) {
            "Could not change DND on this phone. Your current settings were not intentionally reset."
        }
    }

    @Suppress("DEPRECATION")
    private fun buildPolicy(categories: Int, callSenders: Int, messageSenders: Int, suppressed: Int): NotificationManager.Policy {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationManager.Policy(categories, callSenders, messageSenders, suppressed)
        } else {
            NotificationManager.Policy(categories, callSenders, messageSenders)
        }
    }
}
