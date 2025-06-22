package services.update

/**
 * Platform-specific update checker for desktop applications.
 * Only implements actual functionality on desktop platforms.
 */
expect class UpdateChecker() {
    /**
     * Checks for available app updates from GitHub releases.
     * @return UpdateInfo if a newer version is available, null otherwise
     */
    suspend fun checkForUpdates(): UpdateInfo?
    
    /**
     * Gets the cached update information if available
     */
    fun getCachedUpdateInfo(): UpdateInfo?
    
    /**
     * Clears the cached update information
     */
    fun clearCache()
}

/**
 * Version comparison utility
 */
object VersionComparator {
    /**
     * Compares two semantic version strings (e.g., "1.2.3" vs "1.2.4")
     * @param current Current version
     * @param latest Latest version from GitHub
     * @return true if latest is newer than current, false otherwise
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = parseVersion(current)
        val latestParts = parseVersion(latest)
        
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val latestPart = latestParts.getOrNull(i) ?: 0
            
            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        
        return false
    }
    
    private fun parseVersion(version: String): List<Int> {
        return version.removePrefix("v")
            .split(".")
            .mapNotNull { part ->
                // Extract only the numeric part (handles things like "1.0.0-beta")
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }
    }
}