package data

import platform.Foundation.NSUserDefaults

actual class AppModePreferences actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getAppMode(): AppMode {
        val name = defaults.stringForKey("app_mode") ?: return AppMode.ONLINE
        return try { AppMode.valueOf(name) } catch (_: Exception) { AppMode.ONLINE }
    }

    actual fun setAppMode(mode: AppMode) {
        defaults.setObject(mode.name, forKey = "app_mode")
    }

    actual fun isSeedDataDownloaded(): Boolean {
        return defaults.boolForKey("seed_data_downloaded")
    }

    actual fun setSeedDataDownloaded(downloaded: Boolean) {
        defaults.setBool(downloaded, forKey = "seed_data_downloaded")
    }
}
