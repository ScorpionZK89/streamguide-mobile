package com.example.streamguidemobile.playback

import java.net.HttpURLConnection
import java.net.URL

internal data class CastStreamProbeResult(
    val candidate: String,
    val responseCode: Int? = null,
    val contentType: String? = null,
    val bodyKind: String = "none",
    val hasCorsOrigin: Boolean = false,
    val redirected: Boolean = false,
    val finalScheme: String? = null,
    val errorType: String? = null
) {
    fun safeLogLine(): String =
        "cast_probe candidate=$candidate code=${responseCode ?: "none"} " +
            "contentType=${contentType ?: "none"} body=$bodyKind cors=$hasCorsOrigin " +
            "redirected=$redirected scheme=${finalScheme ?: "none"} error=${errorType ?: "none"}"
}

/** Reads only a small prefix and never returns or logs the credential-bearing URL. */
internal fun probeCastStream(candidate: String, url: String): CastStreamProbeResult {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            useCaches = false
            setRequestProperty("Range", "bytes=0-2047")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 CrKey/1.56"
            )
            setRequestProperty("Accept", "application/vnd.apple.mpegurl,application/x-mpegURL,video/mp2t,*/*")
        }
        val code = connection.responseCode
        val input = if (code >= 400) connection.errorStream else connection.inputStream
        val bytes = input?.use { stream ->
            val buffer = ByteArray(2_048)
            val read = stream.read(buffer)
            if (read > 0) buffer.copyOf(read) else byteArrayOf()
        } ?: byteArrayOf()
        CastStreamProbeResult(
            candidate = candidate,
            responseCode = code,
            contentType = connection.contentType?.substringBefore(';')?.trim()?.take(80),
            bodyKind = classifyCastProbeBody(bytes),
            hasCorsOrigin = !connection.getHeaderField("Access-Control-Allow-Origin").isNullOrBlank(),
            redirected = connection.url.toString() != url,
            finalScheme = connection.url.protocol
        )
    } catch (error: Exception) {
        CastStreamProbeResult(candidate = candidate, errorType = error.javaClass.simpleName)
    } finally {
        connection?.disconnect()
    }
}

internal fun classifyCastProbeBody(bytes: ByteArray): String {
    if (bytes.isEmpty()) return "empty"
    val prefix = bytes.take(16).toByteArray().toString(Charsets.US_ASCII).trimStart()
    if (prefix.startsWith("#EXTM3U")) return "hls"
    if (bytes[0] == 0x47.toByte() && (bytes.size <= 188 || bytes[188] == 0x47.toByte())) return "mpeg_ts"
    return "other"
}
