package com.assistant.ai.android

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log

class SystemCommandExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            50 // fallback default
        }
    }

    fun isCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun setVolume(percentage: Int): Boolean {
        if (audioManager == null) return false
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = ((percentage.coerceIn(0, 100) / 100f) * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun getVolume(): Int {
        if (audioManager == null) return 0
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol == 0) return 0
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((currentVol.toFloat() / maxVol) * 100).toInt()
    }

    fun setBrightness(percentage: Int): Boolean {
        return try {
            if (Settings.System.canWrite(context)) {
                val brightnessValue = ((percentage.coerceIn(0, 100) / 100f) * 255).toInt()
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessValue
                )
                true
            } else {
                openSettings("brightness")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
            false
        }
    }

    fun getBrightness(): Int {
        return try {
            val brightnessValue = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            ((brightnessValue / 255f) * 100).toInt()
        } catch (e: Exception) {
            50
        }
    }

    fun openSettings(section: String? = null): Boolean {
        val action = when (section?.lowercase()?.trim()) {
            "wifi", "wi-fi", "network" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: $section", e)
            false
        }
    }

    fun openApp(appNameOrPackage: String): Boolean {
        val pm = context.packageManager
        val targetQuery = appNameOrPackage.lowercase().trim()

        // 1. Try launching by exact package name
        val intentByPkg = pm.getLaunchIntentForPackage(appNameOrPackage)
        if (intentByPkg != null) {
            intentByPkg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intentByPkg)
            return true
        }

        // 2. Search installed applications by label
        val installedApps = pm.getInstalledApplications(0)
        for (appInfo in installedApps) {
            val appLabel = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (appLabel == targetQuery || appLabel.contains(targetQuery)) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
        }

        Log.w(TAG, "Application not found: $appNameOrPackage")
        return false
    }

    fun playMedia(): Boolean {
        return sendMediaButtonIntent(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    fun pauseMedia(): Boolean {
        return sendMediaButtonIntent(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    private fun sendMediaButtonIntent(keyCode: Int): Boolean {
        if (audioManager == null) return false
        val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
        return true
    }

    companion object {
        private const val TAG = "SystemCommandExecutor"
    }
}
