package data

expect class AppModePreferences() {
    fun getAppMode(): AppMode
    fun setAppMode(mode: AppMode)
    fun isSeedDataDownloaded(): Boolean
    fun setSeedDataDownloaded(downloaded: Boolean)
}
