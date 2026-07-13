package com.example.streamguidemobile.update

import android.content.Context
import com.example.streamguidemobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val versionName: String,
    val title: String,
    val notes: String,
    val downloadUrl: String,
    val assetName: String,
    val assetSizeBytes: Long,
    val pageUrl: String
)

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data class Checking(val manual: Boolean) : AppUpdateState
    data object UpToDate : AppUpdateState
    data class Available(val release: AppRelease) : AppUpdateState
    data class Downloading(val release: AppRelease, val progress: Int) : AppUpdateState
    data class Downloaded(val release: AppRelease, val file: File) : AppUpdateState
    data class Error(val message: String) : AppUpdateState
}

class AppUpdateRepository(private val context: Context) {
    suspend fun latestRelease(): AppRelease = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "StreamGuide-Mobile/${BuildConfig.VERSION_NAME}")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw AppUpdateException("Er is nog geen openbare GitHub-release beschikbaar.")
            }
            if (responseCode !in 200..299) {
                throw AppUpdateException("GitHub kon niet worden gecontroleerd (HTTP $responseCode).")
            }
            parseRelease(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(
        release: AppRelease,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updateDirectory = File(context.getExternalFilesDir(null) ?: context.filesDir, UPDATE_DIRECTORY)
        if (!updateDirectory.exists() && !updateDirectory.mkdirs()) {
            throw AppUpdateException("De update-map kon niet worden gemaakt.")
        }

        val safeName = release.assetName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeIf { it.endsWith(".apk", ignoreCase = true) }
            ?: "StreamGuide-Mobile-${release.versionName}.apk"
        val destination = File(updateDirectory, safeName)
        if (destination.isFile && (release.assetSizeBytes <= 0L || destination.length() == release.assetSizeBytes)) {
            onProgress(100)
            return@withContext destination
        }

        val partial = File(updateDirectory, "$safeName.part")
        partial.delete()
        val connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = DOWNLOAD_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.android.package-archive")
            setRequestProperty("User-Agent", "StreamGuide-Mobile/${BuildConfig.VERSION_NAME}")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw AppUpdateException("De APK kon niet worden gedownload (HTTP $responseCode).")
            }
            val expectedBytes = connection.contentLengthLong.takeIf { it > 0L } ?: release.assetSizeBytes
            connection.inputStream.use { input ->
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var previousProgress = -1
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        val progress = if (expectedBytes > 0L) {
                            ((downloaded * 100L) / expectedBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        if (progress != previousProgress) {
                            previousProgress = progress
                            onProgress(progress)
                        }
                    }
                }
            }
            if (partial.length() <= 0L) throw AppUpdateException("De gedownloade APK is leeg.")
            if (expectedBytes > 0L && partial.length() != expectedBytes) {
                throw AppUpdateException("De download is niet compleet. Probeer het opnieuw.")
            }
            destination.delete()
            if (!partial.renameTo(destination)) {
                partial.copyTo(destination, overwrite = true)
                partial.delete()
            }
            onProgress(100)
            destination
        } catch (error: Exception) {
            partial.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(json: String): AppRelease {
        val root = JSONObject(json)
        val tag = root.optString("tag_name").trim()
        val version = tag.removePrefix("v").removePrefix("V").trim()
        if (version.isBlank()) throw AppUpdateException("De GitHub-release heeft geen geldig versienummer.")

        val assets = root.optJSONArray("assets")
            ?: throw AppUpdateException("De GitHub-release bevat geen APK.")
        val candidates = buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true) && !name.contains("unsigned", ignoreCase = true)) {
                    add(asset)
                }
            }
        }
        val asset = candidates.sortedByDescending { candidate ->
            candidate.optString("name").contains("release", ignoreCase = true)
        }.firstOrNull() ?: throw AppUpdateException("De GitHub-release bevat geen installeerbare APK.")

        val downloadUrl = asset.optString("browser_download_url")
        if (!downloadUrl.startsWith(EXPECTED_RELEASE_DOWNLOAD_PREFIX)) {
            throw AppUpdateException("De downloadlocatie van de update is niet vertrouwd.")
        }
        return AppRelease(
            versionName = version,
            title = root.optString("name").trim().ifBlank { "StreamGuide $version" },
            notes = root.optString("body").trim().take(MAX_RELEASE_NOTES_LENGTH),
            downloadUrl = downloadUrl,
            assetName = asset.optString("name"),
            assetSizeBytes = asset.optLong("size"),
            pageUrl = root.optString("html_url")
        )
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/ScorpionZK89/streamguide-mobile/releases/latest"
        const val EXPECTED_RELEASE_DOWNLOAD_PREFIX = "https://github.com/ScorpionZK89/streamguide-mobile/releases/download/"
        const val UPDATE_DIRECTORY = "updates"
        const val NETWORK_TIMEOUT_MS = 15_000
        const val DOWNLOAD_TIMEOUT_MS = 60_000
        const val MAX_RELEASE_NOTES_LENGTH = 1_500
    }
}

class AppUpdateException(message: String) : Exception(message)

internal fun isNewerVersion(candidate: String, current: String): Boolean {
    val candidateVersion = ComparableVersion.parse(candidate) ?: return false
    val currentVersion = ComparableVersion.parse(current) ?: return false
    return candidateVersion > currentVersion
}

private data class ComparableVersion(
    val numbers: List<Int>,
    val prerelease: String?
) : Comparable<ComparableVersion> {
    override fun compareTo(other: ComparableVersion): Int {
        val count = maxOf(numbers.size, other.numbers.size)
        repeat(count) { index ->
            val comparison = (numbers.getOrElse(index) { 0 }).compareTo(other.numbers.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return when {
            prerelease == null && other.prerelease != null -> 1
            prerelease != null && other.prerelease == null -> -1
            else -> prerelease.orEmpty().compareTo(other.prerelease.orEmpty(), ignoreCase = true)
        }
    }

    companion object {
        fun parse(value: String): ComparableVersion? {
            val clean = value.trim().removePrefix("v").removePrefix("V").substringBefore('+')
            if (clean.isBlank()) return null
            val stable = clean.substringBefore('-')
            val numbers = stable.split('.').map { component -> component.toIntOrNull() ?: return null }
            return ComparableVersion(numbers, clean.substringAfter('-', "").takeIf(String::isNotBlank))
        }
    }
}
