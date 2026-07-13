package com.example.streamguidemobile.data

import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader

class M3uParser {
    fun parse(input: String): List<ParsedChannel> = StringReader(input).use { parse(it) }
    fun parse(reader: Reader): List<ParsedChannel> = parseSequence(reader).toList()

    fun parseSequence(reader: Reader): Sequence<ParsedChannel> = sequence {
        val buffered = reader as? BufferedReader ?: reader.buffered()
        var pending: PendingExtInf? = null
        var count = 0

        for (rawLine in buffered.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> pending = parseExtInf(line)
                line.startsWith("#") -> Unit
                pending != null -> {
                    val info = pending
                    val tvgName = info.attributes["tvg-name"].cleanOrNull()
                    val displayName = info.displayName.cleanOrNull()
                    val groupTitle = info.attributes["group-title"].cleanOrNull() ?: DEFAULT_GROUP
                    yield(
                        ParsedChannel(
                            name = tvgName ?: displayName ?: "Kanaal ${count + 1}",
                            tvgId = info.attributes["tvg-id"].cleanOrNull(),
                            tvgName = tvgName,
                            logoUrl = info.attributes["tvg-logo"].cleanOrNull(),
                            groupTitle = groupTitle,
                            streamUrl = line,
                            contentType = detectContentType(info.attributes, groupTitle),
                            description = info.attributes["description"].cleanOrNull()
                                ?: info.attributes["tvg-description"].cleanOrNull(),
                            year = info.attributes["year"]?.toIntOrNull(),
                            genre = info.attributes["genre"].cleanOrNull()
                        )
                    )
                    count += 1
                    pending = null
                }
            }
        }
    }

    private fun parseExtInf(line: String): PendingExtInf {
        val commaIndex = findCommaOutsideQuotes(line)
        val metadata = if (commaIndex >= 0) line.substring(0, commaIndex) else line
        val displayName = if (commaIndex >= 0) line.substring(commaIndex + 1).trim() else null
        val attributes = attributeRegex.findAll(metadata).associate { match ->
            match.groupValues[1].lowercase() to match.groupValues[2].trim()
        }
        return PendingExtInf(attributes, displayName)
    }

    private fun findCommaOutsideQuotes(value: String): Int {
        var quoted = false
        value.forEachIndexed { index, char ->
            when (char) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return index
            }
        }
        return -1
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun detectContentType(attributes: Map<String, String>, groupTitle: String): ParsedContentType {
        val declaredType = sequenceOf("tvg-type", "content-type", "type")
            .mapNotNull(attributes::get)
            .joinToString(" ")
            .lowercase()
        if (declaredType.contains("movie") || declaredType.contains("vod") || declaredType.contains("film")) {
            return ParsedContentType.Movie
        }
        val normalizedGroup = groupTitle.trim().lowercase()
        val explicitMovieGroup = normalizedGroup in setOf("movie", "movies", "film", "films", "vod") ||
            VOD_GROUP_PATTERN.containsMatchIn(normalizedGroup)
        return if (explicitMovieGroup) ParsedContentType.Movie else ParsedContentType.Live
    }

    private data class PendingExtInf(val attributes: Map<String, String>, val displayName: String?)

    companion object {
        const val DEFAULT_GROUP = "Overig"
        private val attributeRegex = Regex("""([A-Za-z0-9_-]+)\s*=\s*"([^"]*)"""")
        private val VOD_GROUP_PATTERN = Regex("(^|[|:_ -])vod([|:_ -]|$)")
    }
}

enum class ParsedContentType { Live, Movie }

data class ParsedChannel(
    val name: String,
    val tvgId: String?,
    val tvgName: String?,
    val logoUrl: String?,
    val groupTitle: String,
    val streamUrl: String,
    val contentType: ParsedContentType = ParsedContentType.Live,
    val description: String? = null,
    val year: Int? = null,
    val genre: String? = null
)
