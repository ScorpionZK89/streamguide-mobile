package com.example.streamguidemobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    private val parser = M3uParser()

    @Test
    fun parsesAttributesAndStreamUrl() {
        val input = """
            #EXTM3U
            #EXTINF:-1 tvg-id="demo.news" tvg-name="Demo News" tvg-logo="https://example.com/news.png" group-title="News",Demo News
            https://example.com/stream1.m3u8
            #EXTINF:-1 tvg-id="demo.sports" group-title="Sports",Demo Sports
            https://example.com/stream2.m3u8
        """.trimIndent()

        val channels = parser.parse(input)

        assertEquals(2, channels.size)
        assertEquals("Demo News", channels[0].name)
        assertEquals("demo.news", channels[0].tvgId)
        assertEquals("News", channels[0].groupTitle)
        assertEquals("https://example.com/stream1.m3u8", channels[0].streamUrl)
    }

    @Test
    fun fallsBackToDefaultGroup() {
        val input = """
            #EXTM3U
            #EXTINF:-1,Demo Kids
            https://example.com/kids.m3u8
        """.trimIndent()

        val channels = parser.parse(input)

        assertEquals(1, channels.size)
        assertEquals("Demo Kids", channels[0].name)
        assertEquals(M3uParser.DEFAULT_GROUP, channels[0].groupTitle)
        assertNull(channels[0].tvgId)
    }
}
