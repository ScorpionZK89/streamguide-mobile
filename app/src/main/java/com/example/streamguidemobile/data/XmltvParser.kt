package com.example.streamguidemobile.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class XmltvParser {
    fun parse(reader: Reader): List<ParsedProgram> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(reader)
        }
        val programs = mutableListOf<ParsedProgram>()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                parseProgramme(parser)?.let(programs::add)
            }
            event = parser.next()
        }
        return programs
    }

    private fun parseProgramme(parser: XmlPullParser): ParsedProgram? {
        val channel = parser.getAttributeValue(null, "channel").cleanOrNull()
        val startTime = parser.getAttributeValue(null, "start")?.let(::parseXmltvTime)
        val endTime = parser.getAttributeValue(null, "stop")?.let(::parseXmltvTime)
        var title: String? = null
        var description: String? = null
        var category: String? = null
        var iconUrl: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "programme")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = parser.nextText().cleanOrNull() ?: title
                    "desc" -> description = parser.nextText().cleanOrNull() ?: description
                    "category" -> category = parser.nextText().cleanOrNull() ?: category
                    "icon" -> iconUrl = parser.getAttributeValue(null, "src").cleanOrNull() ?: iconUrl
                }
            }
            event = parser.next()
        }

        if (channel == null || startTime == null || endTime == null || endTime <= startTime) return null
        return ParsedProgram(
            channelTvgId = channel,
            title = title ?: "Programma",
            description = description,
            startTime = startTime,
            endTime = endTime,
            category = category,
            iconUrl = iconUrl
        )
    }

    private fun parseXmltvTime(value: String): Long? {
        val match = xmltvTimeRegex.matchEntire(value.trim()) ?: return null
        val datePart = match.groupValues[1]
        val offsetPart = match.groupValues.getOrNull(2).orEmpty().takeIf { it.isNotBlank() }
        val local = LocalDateTime.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val instant = if (offsetPart != null) {
            val offset = ZoneOffset.of(offsetPart.take(3) + ":" + offsetPart.takeLast(2))
            OffsetDateTime.of(local, offset).toInstant()
        } else {
            local.toInstant(ZoneOffset.UTC)
        }
        return instant.toEpochMilli()
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private companion object {
        val xmltvTimeRegex = Regex("""^(\d{14})(?:\s*([+-]\d{4}))?.*$""")
    }
}

data class ParsedProgram(
    val channelTvgId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String?,
    val iconUrl: String?
)