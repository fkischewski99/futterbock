package services.update

/**
 * iOS implementation of UpdateChecker - no functionality as updates are handled by App Store
 */
actual class UpdateChecker {
    actual suspend fun checkForUpdates(): UpdateInfo? = null
    actual fun getCachedUpdateInfo(): UpdateInfo? = null
    actual fun clearCache() = Unit
}