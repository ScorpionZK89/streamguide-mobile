package com.example.streamguidemobile.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

class TextSourceReader(private val context: Context) {
    suspend fun <T> readWithReader(source: String, block: suspend (BufferedReader) -> T): T = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        when (uri.scheme?.lowercase()) {
            "http", "https" -> readRemote(source.trim(), block)
            "content", "file" -> readLocal(uri, block)
            null, "" -> readPath(source, block)
            else -> throw IOException("Deze bron wordt niet ondersteund: ${uri.scheme}")
        }
    }

    private suspend fun <T> readRemote(source: String, block: suspend (BufferedReader) -> T): T {
        var lastHttpCode: Int? = null
        var lastError: IOException? = null

        for (profile in requestProfiles) {
            val connection = openConnection(source, profile)
            try {
                val code = connection.responseCode
                if (code in 200..299) {
                    val reader = responseBody(connection).bufferedReader(Charsets.UTF_8)
                    return try { block(reader) } finally { reader.close() }
                }
                lastHttpCode = code
                if (code !in retryableHttpCodes) break
            } catch (error: IOException) {
                lastError = error
            } finally {
                connection.disconnect()
            }
        }

        val code = lastHttpCode
        if (code != null) throw IOException(messageForHttpError(code))
        throw lastError ?: IOException("Bron niet bereikbaar.")
    }

    private fun openConnection(source: String, profile: RequestProfile): HttpURLConnection {
        val url = URL(source)
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 45_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            useCaches = false
            setRequestProperty("User-Agent", profile.userAgent)
            setRequestProperty("Accept", profile.accept)
            setRequestProperty("Accept-Language", "nl-NL,nl;q=0.9,en-US;q=0.8,en;q=0.7")
            setRequestProperty("Accept-Encoding", "gzip, deflate, identity")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("Connection", "close")
            if (profile.sendReferer) setRequestProperty("Referer", "${url.protocol}://${url.host}/")
        }
    }

    private fun responseBody(connection: HttpURLConnection): InputStream {
        val stream = connection.inputStream
        val encoding = connection.contentEncoding?.lowercase().orEmpty()
        return when {
            "gzip" in encoding -> GZIPInputStream(stream)
            "deflate" in encoding -> InflaterInputStream(stream)
            else -> stream
        }
    }

    private suspend fun <T> readLocal(uri: Uri, block: suspend (BufferedReader) -> T): T {
        val reader = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?: throw IOException("Lokaal bestand kan niet worden geopend.")
        return try { block(reader) } finally { reader.close() }
    }

    private suspend fun <T> readPath(path: String, block: suspend (BufferedReader) -> T): T {
        val file = File(path)
        if (!file.exists()) throw IOException("Bestand bestaat niet.")
        val reader = file.inputStream().bufferedReader(Charsets.UTF_8)
        return try { block(reader) } finally { reader.close() }
    }

    private fun messageForHttpError(code: Int): String = when (code) {
        401, 403 -> "De server weigert deze gegevens. Controleer je gebruikersnaam, wachtwoord of abonnement."
        404 -> "Deze serverlink bestaat niet. Controleer de server URL."
        444 -> "De server weigert de app-aanvraag. Gebruik Xtream Codes of vraag je aanbieder om app-toegang."
        in 500..599 -> "De server heeft nu een storing. Probeer het later opnieuw. HTTP $code"
        else -> "Bron niet bereikbaar. HTTP $code"
    }

    private data class RequestProfile(
        val userAgent: String,
        val accept: String,
        val sendReferer: Boolean = false
    )

    private companion object {
        val retryableHttpCodes = setOf(400, 403, 406, 418, 429, 444)
        val requestProfiles = listOf(
            RequestProfile(
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                accept = "text/html,application/xhtml+xml,application/xml;q=0.9,text/plain;q=0.8,*/*;q=0.7"
            ),
            RequestProfile(
                userAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36",
                accept = "application/json,text/plain,*/*"
            ),
            RequestProfile(
                userAgent = "VLC/3.0.20 LibVLC/3.0.20",
                accept = "application/x-mpegURL,application/vnd.apple.mpegurl,application/json,text/plain,*/*",
                sendReferer = true
            )
        )
    }
}