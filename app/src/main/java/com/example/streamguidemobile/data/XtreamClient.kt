package com.example.streamguidemobile.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class XtreamClient(private val reader: TextSourceReader) {
    suspend fun loadLiveChannels(credentials: XtreamCredentials): List<ParsedChannel> {
        val categories = loadCategories(credentials)
        val streamsUrl = XtreamSourceCodec.apiUrl(credentials, "get_live_streams")
        val streams = readJsonArray(streamsUrl, "Xtream Codes gaf geen live-zenderlijst terug.")
        val channels = mutableListOf<ParsedChannel>()

        for (index in 0 until streams.length()) {
            val item = streams.optJSONObject(index) ?: continue
            val streamId = item.optString("stream_id").cleanOrNull() ?: continue
            val name = item.optString("name").cleanOrNull() ?: "Kanaal ${channels.size + 1}"
            val extension = item.optString("container_extension", "ts").cleanOrNull() ?: "ts"
            val categoryId = item.optString("category_id").cleanOrNull()
            channels += ParsedChannel(
                name = name,
                tvgId = item.optString("epg_channel_id").cleanOrNull() ?: streamId,
                tvgName = name,
                logoUrl = item.optString("stream_icon").cleanOrNull(),
                groupTitle = categoryId?.let { categories[it] } ?: "Live TV",
                streamUrl = XtreamSourceCodec.liveStreamUrl(credentials, streamId, extension)
            )
        }
        return channels
    }

    suspend fun loadVodMovies(credentials: XtreamCredentials): List<ParsedMovie> {
        val categories = loadVodCategories(credentials)
        val streams = readJsonArray(
            XtreamSourceCodec.apiUrl(credentials, "get_vod_streams"),
            "Xtream Codes gaf geen filmbibliotheek terug."
        )
        return buildList {
            for (index in 0 until streams.length()) {
                val item = streams.optJSONObject(index) ?: continue
                val streamId = item.optString("stream_id").cleanOrNull() ?: continue
                val title = item.optString("name").cleanOrNull() ?: continue
                val extension = item.optString("container_extension", "mp4").cleanOrNull() ?: "mp4"
                val categoryId = item.optString("category_id").cleanOrNull()
                add(
                    ParsedMovie(
                        providerId = streamId,
                        title = title,
                        streamUrl = XtreamSourceCodec.movieStreamUrl(credentials, streamId, extension),
                        categoryId = categoryId,
                        categoryName = categoryId?.let(categories::get) ?: "Films",
                        posterUrl = item.optString("stream_icon").cleanOrNull(),
                        backdropUrl = item.firstImage("backdrop_path"),
                        year = item.optionalInt("year"),
                        durationMinutes = item.durationMinutes(),
                        genre = item.optString("genre").cleanOrNull(),
                        ageRating = item.optString("rating_mpaa").cleanOrNull()
                            ?: item.optString("mpaa_rating").cleanOrNull(),
                        description = item.optString("plot").cleanOrNull(),
                        rating = item.optionalDouble("rating_5based") ?: item.optionalDouble("rating"),
                        director = item.optString("director").cleanOrNull(),
                        cast = item.optString("cast").cleanOrNull(),
                        trailerUrl = item.optString("youtube_trailer").cleanOrNull(),
                        addedAt = item.optString("added").toLongOrNull()?.let { if (it > 1_000_000_000_000L) it else it * 1_000L },
                        containerExtension = extension
                    )
                )
            }
        }
    }

    suspend fun loadVodDetails(credentials: XtreamCredentials, providerId: String): ParsedMovieDetails {
        val body = readJsonObject(
            XtreamSourceCodec.apiUrl(credentials, "get_vod_info", mapOf("vod_id" to providerId)),
            "Filmgegevens konden niet worden geladen."
        )
        val info = body.optJSONObject("info") ?: JSONObject()
        val movieData = body.optJSONObject("movie_data") ?: JSONObject()
        return ParsedMovieDetails(
            originalTitle = info.optString("o_name").cleanOrNull(),
            posterUrl = info.optString("movie_image").cleanOrNull()
                ?: movieData.optString("stream_icon").cleanOrNull(),
            backdropUrl = info.firstImage("backdrop_path"),
            year = info.optionalInt("releasedate") ?: info.optionalInt("year"),
            durationMinutes = info.durationMinutes(),
            genre = info.optString("genre").cleanOrNull(),
            ageRating = info.optString("rating_mpaa").cleanOrNull()
                ?: info.optString("mpaa_rating").cleanOrNull(),
            description = info.optString("plot").cleanOrNull(),
            rating = info.optionalDouble("rating_5based") ?: info.optionalDouble("rating"),
            director = info.optString("director").cleanOrNull(),
            cast = info.optString("cast").cleanOrNull(),
            trailerUrl = info.optString("youtube_trailer").cleanOrNull()
        )
    }

    suspend fun loadSeries(credentials: XtreamCredentials): List<ParsedSeries> {
        val categories = loadSeriesCategories(credentials)
        val items = readJsonArray(
            XtreamSourceCodec.apiUrl(credentials, "get_series"),
            "Xtream Codes gaf geen seriesbibliotheek terug."
        )
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val providerId = item.optString("series_id").cleanOrNull() ?: continue
                val title = item.optString("name").cleanOrNull() ?: continue
                val categoryId = item.optString("category_id").cleanOrNull()
                add(
                    ParsedSeries(
                        providerId = providerId,
                        title = title,
                        categoryId = categoryId,
                        categoryName = categoryId?.let(categories::get) ?: "Series",
                        posterUrl = item.optString("cover").cleanOrNull() ?: item.optString("stream_icon").cleanOrNull(),
                        backdropUrl = item.firstImage("backdrop_path"),
                        year = item.optionalInt("releaseDate") ?: item.optionalInt("year"),
                        genre = item.optString("genre").cleanOrNull(),
                        ageRating = item.optString("rating_mpaa").cleanOrNull() ?: item.optString("mpaa_rating").cleanOrNull(),
                        description = item.optString("plot").cleanOrNull(),
                        rating = item.optionalDouble("rating_5based") ?: item.optionalDouble("rating"),
                        director = item.optString("director").cleanOrNull(),
                        cast = item.optString("cast").cleanOrNull(),
                        trailerUrl = item.optString("youtube_trailer").cleanOrNull(),
                        addedAt = item.epochMillis("last_modified") ?: item.epochMillis("added"),
                        updatedAt = item.epochMillis("last_modified")
                    )
                )
            }
        }
    }

    suspend fun loadSeriesDetails(credentials: XtreamCredentials, providerId: String): ParsedSeriesDetails {
        val body = readJsonObject(
            XtreamSourceCodec.apiUrl(credentials, "get_series_info", mapOf("series_id" to providerId)),
            "Seriegegevens konden niet worden geladen."
        )
        val info = body.optJSONObject("info") ?: JSONObject()
        val seasonNames = buildMap<Int, String> {
            val seasons = body.optJSONArray("seasons") ?: JSONArray()
            for (index in 0 until seasons.length()) {
                val season = seasons.optJSONObject(index) ?: continue
                val number = season.intValue("season_number") ?: season.intValue("id") ?: continue
                season.optString("name").cleanOrNull()?.let { put(number, it) }
            }
        }
        val parsedEpisodes = mutableListOf<ParsedEpisode>()
        fun appendEpisodes(items: JSONArray, groupSeason: Int?, groupIndex: Int) {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val episodeInfo = item.optJSONObject("info") ?: JSONObject()
                val episodeNumber = item.intValue("episode_num") ?: item.intValue("episode")
                    ?: episodeInfo.intValue("episode_num") ?: parseEpisodeNumber(item.optString("title"))
                val seasonNumber = item.intValue("season") ?: episodeInfo.intValue("season")
                    ?: groupSeason ?: parseSeasonNumber(item.optString("title")) ?: UNKNOWN_SEASON
                val rawId = item.optString("id").cleanOrNull() ?: item.optString("episode_id").cleanOrNull()
                val title = item.optString("title").cleanOrNull()
                    ?: episodeInfo.optString("title").cleanOrNull()
                    ?: episodeNumber?.let { "Aflevering $it" }
                    ?: "Aflevering"
                val extension = item.optString("container_extension").cleanOrNull()
                val stableId = rawId ?: "missing:$seasonNumber:$groupIndex:$index:$title"
                parsedEpisodes += ParsedEpisode(
                    providerId = stableId,
                    seasonNumber = seasonNumber,
                    seasonName = seasonNames[seasonNumber] ?: seasonLabel(seasonNumber),
                    episodeNumber = episodeNumber,
                    providerOrder = parsedEpisodes.size,
                    title = title,
                    streamUrl = rawId?.let { XtreamSourceCodec.seriesStreamUrl(credentials, it, extension ?: "mp4") },
                    imageUrl = episodeInfo.optString("movie_image").cleanOrNull()
                        ?: episodeInfo.optString("cover_big").cleanOrNull()
                        ?: episodeInfo.optString("cover").cleanOrNull(),
                    description = episodeInfo.optString("plot").cleanOrNull(),
                    durationMinutes = episodeInfo.durationMinutes(),
                    containerExtension = extension,
                    addedAt = episodeInfo.epochMillis("releasedate") ?: item.epochMillis("added")
                )
            }
        }
        when (val episodeGroups = body.opt("episodes")) {
            is JSONArray -> appendEpisodes(episodeGroups, null, 0)
            is JSONObject -> {
            val keys = buildList { val iterator = episodeGroups.keys(); while (iterator.hasNext()) add(iterator.next()) }
            keys.forEachIndexed { groupIndex, key ->
                val items = episodeGroups.optJSONArray(key) ?: return@forEachIndexed
                val groupSeason = key.toIntOrNull()
                appendEpisodes(items, groupSeason, groupIndex)
            }
            }
        }
        return ParsedSeriesDetails(
            originalTitle = info.optString("original_name").cleanOrNull() ?: info.optString("o_name").cleanOrNull(),
            posterUrl = info.optString("cover").cleanOrNull(),
            backdropUrl = info.firstImage("backdrop_path"),
            year = info.optionalInt("releaseDate") ?: info.optionalInt("year"),
            genre = info.optString("genre").cleanOrNull(),
            ageRating = info.optString("rating_mpaa").cleanOrNull() ?: info.optString("mpaa_rating").cleanOrNull(),
            description = info.optString("plot").cleanOrNull(),
            rating = info.optionalDouble("rating_5based") ?: info.optionalDouble("rating"),
            director = info.optString("director").cleanOrNull(),
            cast = info.optString("cast").cleanOrNull(),
            trailerUrl = info.optString("youtube_trailer").cleanOrNull(),
            updatedAt = info.epochMillis("last_modified"),
            episodes = parsedEpisodes
        )
    }

    private suspend fun loadCategories(credentials: XtreamCredentials): Map<String, String> = runCatching {
        val categoriesUrl = XtreamSourceCodec.apiUrl(credentials, "get_live_categories")
        val categories = readJsonArray(categoriesUrl, "")
        buildMap {
            for (index in 0 until categories.length()) {
                val item = categories.optJSONObject(index) ?: continue
                val id = item.optString("category_id").cleanOrNull() ?: continue
                val name = item.optString("category_name").cleanOrNull() ?: continue
                put(id, name)
            }
        }
    }.getOrDefault(emptyMap())

    private suspend fun loadVodCategories(credentials: XtreamCredentials): Map<String, String> = runCatching {
        val categories = readJsonArray(XtreamSourceCodec.apiUrl(credentials, "get_vod_categories"), "")
        buildMap {
            for (index in 0 until categories.length()) {
                val item = categories.optJSONObject(index) ?: continue
                val id = item.optString("category_id").cleanOrNull() ?: continue
                val name = item.optString("category_name").cleanOrNull() ?: continue
                put(id, name)
            }
        }
    }.getOrDefault(emptyMap())

    private suspend fun loadSeriesCategories(credentials: XtreamCredentials): Map<String, String> = runCatching {
        val categories = readJsonArray(XtreamSourceCodec.apiUrl(credentials, "get_series_categories"), "")
        buildMap {
            for (index in 0 until categories.length()) {
                val item = categories.optJSONObject(index) ?: continue
                val id = item.optString("category_id").cleanOrNull() ?: continue
                val name = item.optString("category_name").cleanOrNull() ?: continue
                put(id, name)
            }
        }
    }.getOrDefault(emptyMap())

    private suspend fun readJsonArray(url: String, fallbackMessage: String): JSONArray {
        val text = reader.readWithReader(url) { it.readText() }.trim()
        if (text.startsWith("[")) return JSONArray(text)
        if (text.startsWith("{")) {
            val body = JSONObject(text)
            val userInfo = body.optJSONObject("user_info")
            val status = userInfo?.optString("status").orEmpty()
            if (status.isNotBlank() && !status.equals("Active", ignoreCase = true)) {
                throw IOException("Xtream-account is niet actief: $status")
            }
            val message = body.optString("message").cleanOrNull()
            throw IOException(message ?: fallbackMessage.ifBlank { "Xtream Codes gaf geen zenderlijst terug." })
        }
        throw IOException(fallbackMessage.ifBlank { "Xtream Codes gaf geen zenderlijst terug." })
    }

    private suspend fun readJsonObject(url: String, fallbackMessage: String): JSONObject {
        val text = reader.readWithReader(url) { it.readText() }.trim()
        if (!text.startsWith("{")) throw IOException(fallbackMessage)
        val body = JSONObject(text)
        val userInfo = body.optJSONObject("user_info")
        val status = userInfo?.optString("status").orEmpty()
        if (status.isNotBlank() && !status.equals("Active", ignoreCase = true)) {
            throw IOException("Xtream-account is niet actief: $status")
        }
        return body
    }

    private fun JSONObject.firstImage(key: String): String? {
        val array = optJSONArray(key)
        if (array != null && array.length() > 0) return array.optString(0).cleanOrNull()
        return optString(key).cleanOrNull()
    }

    private fun JSONObject.optionalInt(key: String): Int? {
        val value = optString(key).cleanOrNull() ?: return null
        return Regex("\\d{4}").find(value)?.value?.toIntOrNull() ?: value.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun JSONObject.optionalDouble(key: String): Double? =
        optString(key).cleanOrNull()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it >= 0.0 }

    private fun JSONObject.durationMinutes(): Int? {
        optString("duration_secs").toLongOrNull()?.takeIf { it > 0L }?.let { return (it / 60L).toInt().coerceAtLeast(1) }
        optString("episode_run_time").toIntOrNull()?.takeIf { it > 0 }?.let { return it }
        val duration = optString("duration").cleanOrNull() ?: return null
        val parts = duration.split(':').mapNotNull(String::toIntOrNull)
        return when (parts.size) {
            3 -> parts[0] * 60 + parts[1] + if (parts[2] >= 30) 1 else 0
            2 -> parts[0] * 60 + parts[1]
            else -> duration.toIntOrNull()?.takeIf { it > 0 }
        }
    }

    private fun JSONObject.intValue(key: String): Int? = optString(key).cleanOrNull()?.toDoubleOrNull()?.toInt()
    private fun JSONObject.epochMillis(key: String): Long? = optString(key).cleanOrNull()?.toLongOrNull()?.let {
        if (it > 1_000_000_000_000L) it else if (it > 0L) it * 1_000L else null
    }

    private fun parseSeasonNumber(value: String?): Int? = Regex("(?i)(?:season|seizoen|s)\\s*0*(\\d{1,3})").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    private fun parseEpisodeNumber(value: String?): Int? = Regex("(?i)(?:episode|aflevering|e)\\s*0*(\\d{1,4})").find(value.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
    private fun seasonLabel(number: Int): String = when (number) { 0 -> "Specials"; UNKNOWN_SEASON -> "Afleveringen"; else -> "Seizoen $number" }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
}

data class ParsedMovie(
    val providerId: String?,
    val title: String,
    val streamUrl: String,
    val categoryId: String?,
    val categoryName: String,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val durationMinutes: Int? = null,
    val genre: String? = null,
    val ageRating: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val director: String? = null,
    val cast: String? = null,
    val trailerUrl: String? = null,
    val addedAt: Long? = null,
    val containerExtension: String? = null
)

data class ParsedMovieDetails(
    val originalTitle: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val year: Int?,
    val durationMinutes: Int?,
    val genre: String?,
    val ageRating: String?,
    val description: String?,
    val rating: Double?,
    val director: String?,
    val cast: String?,
    val trailerUrl: String?
)

data class ParsedSeries(
    val providerId: String,
    val title: String,
    val categoryId: String?,
    val categoryName: String,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val ageRating: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val director: String? = null,
    val cast: String? = null,
    val trailerUrl: String? = null,
    val addedAt: Long? = null,
    val updatedAt: Long? = null
)

data class ParsedEpisode(
    val providerId: String,
    val seasonNumber: Int,
    val seasonName: String,
    val episodeNumber: Int?,
    val providerOrder: Int,
    val title: String,
    val streamUrl: String?,
    val imageUrl: String?,
    val description: String?,
    val durationMinutes: Int?,
    val containerExtension: String?,
    val addedAt: Long?
)

data class ParsedSeriesDetails(
    val originalTitle: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val year: Int?,
    val genre: String?,
    val ageRating: String?,
    val description: String?,
    val rating: Double?,
    val director: String?,
    val cast: String?,
    val trailerUrl: String?,
    val updatedAt: Long?,
    val episodes: List<ParsedEpisode>
)

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

object XtreamSourceCodec {
    private const val PREFIX = "xtream://source?"

    fun encode(serverUrl: String, username: String, password: String): String {
        val credentials = XtreamCredentials(
            serverUrl = normalizeServerUrl(serverUrl),
            username = username.trim(),
            password = password.trim()
        )
        return PREFIX + listOf(
            "server" to credentials.serverUrl,
            "username" to credentials.username,
            "password" to credentials.password
        ).joinToString("&") { (key, value) -> "$key=${encodeQuery(value)}" }
    }

    fun decode(source: String): XtreamCredentials? {
        if (!source.startsWith(PREFIX)) return null
        val values = source.removePrefix(PREFIX)
            .split('&')
            .mapNotNull { part ->
                val pieces = part.split('=', limit = 2)
                if (pieces.size == 2) pieces[0] to decodeQuery(pieces[1]) else null
            }
            .toMap()

        val server = values["server"]?.cleanOrNull() ?: return null
        val username = values["username"]?.cleanOrNull() ?: return null
        val password = values["password"]?.cleanOrNull() ?: return null
        return XtreamCredentials(server, username, password)
    }

    fun apiUrl(credentials: XtreamCredentials, action: String, parameters: Map<String, String> = emptyMap()): String =
        "${credentials.serverUrl}/player_api.php?username=${encodeQuery(credentials.username)}" +
            "&password=${encodeQuery(credentials.password)}&action=${encodeQuery(action)}" +
            parameters.entries.joinToString("") { (key, value) -> "&${encodeQuery(key)}=${encodeQuery(value)}" }

    fun liveStreamUrl(credentials: XtreamCredentials, streamId: String, extension: String): String =
        "${credentials.serverUrl}/live/${encodePath(credentials.username)}/${encodePath(credentials.password)}/" +
            "${encodePath(streamId)}.${extension.cleanOrNull() ?: "ts"}"

    fun movieStreamUrl(credentials: XtreamCredentials, streamId: String, extension: String): String =
        "${credentials.serverUrl}/movie/${encodePath(credentials.username)}/${encodePath(credentials.password)}/" +
            "${encodePath(streamId)}.${extension.cleanOrNull() ?: "mp4"}"

    fun seriesStreamUrl(credentials: XtreamCredentials, episodeId: String, extension: String): String =
        "${credentials.serverUrl}/series/${encodePath(credentials.username)}/${encodePath(credentials.password)}/" +
            "${encodePath(episodeId)}.${extension.cleanOrNull() ?: "mp4"}"


    fun xmltvUrl(credentials: XtreamCredentials): String =
        "${credentials.serverUrl}/xmltv.php?username=${encodeQuery(credentials.username)}" +
            "&password=${encodeQuery(credentials.password)}"

    private fun normalizeServerUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "Vul een Xtream server URL in." }
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun decodeQuery(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    private fun encodePath(value: String): String = encodeQuery(value).replace("+", "%20")
    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

private const val UNKNOWN_SEASON = -1
