package services.update

import kotlinx.serialization.Serializable

/**
 * Contains information about an available app update
 */
@Serializable
data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseUrl: String,
    val releaseNotes: String? = null,
    val publishedAt: String? = null
)

/**
 * GitHub Release API response model
 */
@Serializable
internal data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val html_url: String,
    val body: String? = null,
    val published_at: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

/**
 * GitHub Release Asset model
 */
@Serializable
internal data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val content_type: String
)