package data

import java.util.prefs.Preferences

actual class AppModePreferences actual constructor() {
    private val prefs = Preferences.userNodeForPackage(AppModePreferences::class.java)

    actual fun getAppMode(): AppMode {
        val name = prefs.get("app_mode", null) ?: return AppMode.ONLINE
        return try { AppMode.valueOf(name) } catch (_: Exception) { AppMode.ONLINE }
    }

    actual fun setAppMode(mode: AppMode) {
        prefs.put("app_mode", mode.name)
    }

    actual fun isSeedDataDownloaded(): Boolean {
        return prefs.getBoolean("seed_data_downloaded", false)
    }

    actual fun setSeedDataDownloaded(downloaded: Boolean) {
        prefs.putBoolean("seed_data_downloaded", downloaded)
    }
}
