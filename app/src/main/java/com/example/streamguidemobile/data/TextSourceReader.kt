package com.example.streamguidemobile.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TextSourceReader(private val context: Context) {
    suspend fun <T> readWithReader(source: String, block: suspend (BufferedReader) -> T): T = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        when (uri.scheme?.lowercase()) {
            "http", "https" -> readRemote(source, block)
            "content", "file" -> readLocal(uri, block)
            null, "" -> readPath(source, block)
            else -> throw IOException("Deze bron wordt niet ondersteund: ${uri.scheme}")
        }
    }

    private suspend fun <T> readRemote(source: String, block: suspend (BufferedReader) -> T): T {
        val connection = (URL(source).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "StreamGuideMobile/0.1")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Bron niet bereikbaar. HTTP $code")
            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            try { block(reader) } finally { reader.close() }
        } finally {
            connection.disconnect()
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
}
