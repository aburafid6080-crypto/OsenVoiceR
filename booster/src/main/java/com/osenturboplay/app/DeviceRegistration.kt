package com.osenturboplay.app

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Registers this installation with the OSEN owner dashboard.
 * Uses an app-generated random ID; does not read hardware serials, contacts,
 * location, RAM contents, or personal files.
 */
internal object DeviceRegistration {
    private const val PREFS = "osen_device_registration"
    private const val ID_KEY = "installation_id"
    private const val API_URL = "https://osen-voice-r.vercel.app/api/devices"

    fun register(context: Context) {
        val appContext = context.applicationContext
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                var id = prefs.getString(ID_KEY, null)
                if (id.isNullOrBlank()) {
                    id = UUID.randomUUID().toString()
                    prefs.edit().putString(ID_KEY, id).apply()
                }
                val version = try {
                    @Suppress("DEPRECATION")
                    appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "Unknown"
                } catch (_: Exception) { "Unknown" }

                val payload = JSONObject()
                    .put("device_id", id)
                    .put("package_name", appContext.packageName)
                    .put("device_model", (Build.MANUFACTURER + " " + Build.MODEL).take(80))
                    .put("android_version", Build.VERSION.RELEASE ?: "Unknown")
                    .put("app_version", version)

                connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode in 200..299) {
                    prefs.edit().putLong("last_success_ms", System.currentTimeMillis()).apply()
                }
            } catch (_: Exception) {
                // Registration is best-effort; the app remains fully usable offline.
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
