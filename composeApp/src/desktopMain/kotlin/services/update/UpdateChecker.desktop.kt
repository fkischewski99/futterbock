package services.update

import co.touchlab.kermit.Logger
import com.andreasgift.kmpweatherapp.BuildKonfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import kotlin.time.Duration.Companion.hours

/**
 * Desktop implementation of UpdateChecker that checks GitHub releases
 */
actual class UpdateChecker {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val mutex = Mutex()
    private val cacheFile = File(System.getProperty("user.home"), ".futterbock-update-cache.json")
    private val githubApiUrl = "https://api.github.com/repos/fkischewski99/futterbock/releases"

    // Cache for 24 hours
    private val cacheValidityDuration = 24.hours

    private var cachedUpdateInfo: UpdateInfo? = null
    private var lastCheckTime: Instant? = null

    init {
        loadCacheFromDisk()
    }

    actual suspend fun checkForUpdates(): UpdateInfo? {
        return mutex.withLock {
            try {
                // Check if we have a valid cache
                val now = Instant.now()
                if (lastCheckTime != null &&
                    java.time.Duration.between(lastCheckTime, now)
                        .toMillis() < cacheValidityDuration.inWholeMilliseconds &&
                    cachedUpdateInfo != null
                ) {
                    Logger.d("UpdateChecker: Using cached update info")
                    return@withLock cachedUpdateInfo
                }

                Logger.d("UpdateChecker: Checking for updates from GitHub...")
                val updateInfo = fetchLatestRelease()

                // Update cache
                cachedUpdateInfo = updateInfo
                lastCheckTime = now
                saveCacheToDisk()

                updateInfo
            } catch (e: Exception) {
                Logger.e("UpdateChecker: Error checking for updates", e)
                // Return cached info if available, null otherwise
                cachedUpdateInfo
            }
        }
    }

    actual fun getCachedUpdateInfo(): UpdateInfo? {
        return cachedUpdateInfo
    }

    actual fun clearCache() {
        cachedUpdateInfo = null
        lastCheckTime = null
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    private suspend fun fetchLatestRelease(): UpdateInfo? {
        try {
            val response = httpClient.get(githubApiUrl) {
                headers {
                    append(HttpHeaders.Accept, "application/vnd.github.v3+json")
                    append(HttpHeaders.UserAgent, "Futterbock-App-UpdateChecker")
                }
            }

            if (!response.status.isSuccess()) {
                Logger.w("UpdateChecker: GitHub API request failed with status: ${response.status}")
                return null
            }

            val releases: List<GitHubRelease> = response.body()
            Logger.d("UpdateChecker: Fetched ${releases.size} releases from GitHub")

            // Find the latest non-draft, non-prerelease version
            val latestRelease = releases.firstOrNull { !it.draft && !it.prerelease }

            if (latestRelease == null) {
                Logger.d("UpdateChecker: No stable releases found")
                return null
            }

            val currentVersion = getCurrentVersion()
            val latestVersion = latestRelease.tag_name

            Logger.d("UpdateChecker: Current version: $currentVersion, Latest version: $latestVersion")

            return if (VersionComparator.isNewerVersion(currentVersion, latestVersion)) {
                Logger.i("UpdateChecker: New version available: $latestVersion")
                UpdateInfo(
                    version = latestVersion,
                    downloadUrl = getDownloadUrl(latestRelease),
                    releaseUrl = latestRelease.html_url,
                    releaseNotes = latestRelease.body?.take(500), // Limit to 500 chars
                    publishedAt = latestRelease.published_at
                )
            } else {
                Logger.d("UpdateChecker: App is up to date")
                null
            }

        } catch (e: Exception) {
            Logger.e("UpdateChecker: Error fetching releases from GitHub", e)
            throw e
        }
    }

    private fun getCurrentVersion(): String {
        return try {
            // Use version from BuildKonfig (embedded at compile time)
            BuildKonfig.APP_VERSION
        } catch (e: Exception) {
            Logger.w(
                "UpdateChecker: Could not determine current version from BuildKonfig, using default",
                e
            )
            "1.0.0"
        }
    }

    private fun getDownloadUrl(release: GitHubRelease): String {
        // Based on the GitHub pipeline, determine the correct asset name for the current platform
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

        val assetName = when {
            osName.contains("mac") -> {
                // Determine if ARM or Intel Mac
                if (osArch.contains("aarch64") || osArch.contains("arm")) {
                    "futterbock-arm.dmg"
                } else {
                    "futterbock-x86.dmg"
                }
            }

            osName.contains("windows") -> "futterbock.msi"
            else -> null // No Linux builds in pipeline yet
        }

        // Look for the specific asset
        val asset = assetName?.let { name ->
            release.assets.find { it.name == name }
        }

        // Return the asset download URL if found, otherwise the release page
        return asset?.browser_download_url ?: release.html_url
    }

    private fun loadCacheFromDisk() {
        try {
            if (cacheFile.exists()) {
                val cacheContent = cacheFile.readText()
                val cacheData = Json.decodeFromString<CacheData>(cacheContent)

                // Check if cache is still valid
                val cacheAge = java.time.Duration.between(
                    Instant.parse(cacheData.timestamp),
                    Instant.now()
                )

                if (cacheAge.toMillis() < cacheValidityDuration.inWholeMilliseconds) {
                    cachedUpdateInfo = cacheData.updateInfo
                    lastCheckTime = Instant.parse(cacheData.timestamp)
                    Logger.d("UpdateChecker: Loaded valid cache from disk")
                } else {
                    Logger.d("UpdateChecker: Cache expired, will check for updates")
                }
            }
        } catch (e: Exception) {
            Logger.w("UpdateChecker: Could not load cache from disk", e)
        }
    }

    private fun saveCacheToDisk() {
        try {
            val cacheData = CacheData(
                updateInfo = cachedUpdateInfo,
                timestamp = (lastCheckTime ?: Instant.now()).toString()
            )

            val cacheContent = Json.encodeToString(CacheData.serializer(), cacheData)
            cacheFile.writeText(cacheContent)
            Logger.d("UpdateChecker: Saved cache to disk")
        } catch (e: Exception) {
            Logger.w("UpdateChecker: Could not save cache to disk", e)
        }
    }

    @kotlinx.serialization.Serializable
    private data class CacheData(
        val updateInfo: UpdateInfo?,
        val timestamp: String
    )
}