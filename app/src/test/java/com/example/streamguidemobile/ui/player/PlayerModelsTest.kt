package com.example.streamguidemobile.ui.player

import androidx.media3.common.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerModelsTest {
    @Test
    fun `formats short and long playback positions`() {
        assertEquals("00:00", formatPlaybackTime(-1L))
        assertEquals("02:05", formatPlaybackTime(125_000L))
        assertEquals("1:02:03", formatPlaybackTime(3_723_000L))
    }

    @Test
    fun `classifies common playback failures`() {
        assertEquals("Netwerkfout", classifyPlayerFailure("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED").title)
        assertEquals("Decoderfout", classifyPlayerFailure("ERROR_CODE_DECODING_FAILED").title)
        assertEquals("Stream niet beschikbaar", classifyPlayerFailure("ERROR_CODE_PARSING_MANIFEST_MALFORMED").title)
        assertEquals("Afspeelfout", classifyPlayerFailure("ERROR_CODE_UNSPECIFIED").title)
    }

    @Test
    fun `video track label reflects actual format metadata`() {
        val format = Format.Builder()
            .setHeight(1080)
            .setAverageBitrate(5_200_000)
            .setFrameRate(50f)
            .build()

        val label = videoTrackLabel(format)

        assertTrue(label.contains("1080p"))
        assertTrue(label.contains("5.2 Mbps"))
        assertTrue(label.contains("50 fps"))
    }

    @Test
    fun `resume position is only kept for unfinished non-live media`() {
        assertEquals(null, normalizedResumePosition(120_000L, 600_000L, isLive = true))
        assertEquals(0L, normalizedResumePosition(5_000L, 600_000L, isLive = false))
        assertEquals(0L, normalizedResumePosition(585_000L, 600_000L, isLive = false))
        assertEquals(120_000L, normalizedResumePosition(120_000L, 600_000L, isLive = false))
    }

    @Test
    fun `technical film information uses selected track metadata`() {
        val video = Format.Builder()
            .setSampleMimeType("video/hevc")
            .setCodecs("hvc1.2.4.L153")
            .setAverageBitrate(12_500_000)
            .setFrameRate(23.976f)
            .build()
        val audio = Format.Builder()
            .setSampleMimeType("audio/mp4a-latm")
            .setCodecs("mp4a.40.2")
            .setChannelCount(6)
            .setSampleRate(48_000)
            .setAverageBitrate(384_000)
            .build()

        val info = buildPlayerTechnicalInfo("mkv", video, audio, 3840, 2160).associate { it.label to it.value }

        assertEquals("3840 x 2160", info["Resolutie"])
        assertEquals("4K", info["Kwaliteit"])
        assertEquals("MKV", info["Container"])
        assertTrue(info.getValue("Videocodec").contains("H.265 / HEVC"))
        assertEquals("5.1", info["Kanalen"])
        assertEquals("48.0 kHz", info["Samplefrequentie"])
    }
}
