package data

import android.content.Context
import org.futterbock.app.MainActivity

actual class AppModePreferences actual constructor() {
    private val prefs by lazy {
        MainActivity.appContext.getSharedPreferences("futterbock_prefs", Context.MODE_PRIVATE)
    }

    actual fun getAppMode(): AppMode {
        val name = prefs.getString("app_mode", null) ?: return AppMode.OFFLINE_FIRST
        return try { AppMode.valueOf(name) } catch (_: Exception) { AppMode.OFFLINE_FIRST }
    }

    actual fun setAppMode(mode: AppMode) {
        prefs.edit().putString("app_mode", mode.name).apply()
    }

    actual fun isSeedDataDownloaded(): Boolean {
        return prefs.getBoolean("seed_data_downloaded", false)
    }

    actual fun setSeedDataDownloaded(downloaded: Boolean) {
        prefs.edit().putBoolean("seed_data_downloaded", downloaded).apply()
    }
}
