package services.update

/**
 * Android implementation of UpdateChecker - no functionality as updates are handled by Play Store
 */
actual class UpdateChecker {
    actual suspend fun checkForUpdates(): UpdateInfo? = null
    actual fun getCachedUpdateInfo(): UpdateInfo? = null
    actual fun clearCache() = Unit
}