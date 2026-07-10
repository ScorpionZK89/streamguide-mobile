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

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
}

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

    fun apiUrl(credentials: XtreamCredentials, action: String): String =
        "${credentials.serverUrl}/player_api.php?username=${encodeQuery(credentials.username)}" +
            "&password=${encodeQuery(credentials.password)}&action=${encodeQuery(action)}"

    fun liveStreamUrl(credentials: XtreamCredentials, streamId: String, extension: String): String =
        "${credentials.serverUrl}/live/${encodePath(credentials.username)}/${encodePath(credentials.password)}/" +
            "${encodePath(streamId)}.${extension.cleanOrNull() ?: "ts"}"


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
