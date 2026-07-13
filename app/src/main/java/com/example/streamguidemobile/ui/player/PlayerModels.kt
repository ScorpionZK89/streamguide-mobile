@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.example.streamguidemobile.ui.player

import androidx.media3.common.Format
import java.util.Locale
import kotlin.math.roundToInt

internal enum class PlayerSheet {
    Audio,
    Subtitles,
    Quality,
    AspectRatio,
    SleepTimer,
    ProgramInfo,
    More,
    TechnicalInfo
}

internal data class PlayerTechnicalInfoRow(val label: String, val value: String)

internal data class PlayerFailure(
    val title: String,
    val message: String
)

internal fun classifyPlayerFailure(errorCodeName: String): PlayerFailure {
    val code = errorCodeName.uppercase(Locale.ROOT)
    return when {
        "DECOD" in code -> PlayerFailure(
            title = "Decoderfout",
            message = "Dit toestel kan deze video- of audiocodec niet correct verwerken."
        )
        "IO_" in code || "NETWORK" in code || "TIMEOUT" in code -> PlayerFailure(
            title = "Netwerkfout",
            message = "De verbinding met de stream is verbroken of reageert niet op tijd."
        )
        "PARSING" in code || "CONTENT" in code || "MANIFEST" in code -> PlayerFailure(
            title = "Stream niet beschikbaar",
            message = "De streamindeling of afspeellijst kon niet worden gelezen."
        )
        else -> PlayerFailure(
            title = "Afspeelfout",
            message = "De stream kon niet worden afgespeeld. Probeer het opnieuw of kies een andere zender."
        )
    }
}

internal fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

internal fun normalizedResumePosition(
    positionMs: Long,
    durationMs: Long,
    isLive: Boolean
): Long? = when {
    isLive || durationMs <= 0L -> null
    positionMs < MIN_RESUME_POSITION_MS -> 0L
    durationMs - positionMs <= COMPLETION_MARGIN_MS -> 0L
    else -> positionMs
}

internal fun videoTrackLabel(format: Format): String {
    val resolution = when {
        format.height >= 2000 -> "4K"
        format.height >= 1400 -> "1440p"
        format.height >= 1000 -> "1080p"
        format.height >= 700 -> "720p"
        format.height > 0 -> "${format.height}p"
        else -> "Videotrack"
    }
    val bitrate = sequenceOf(format.averageBitrate, format.peakBitrate, format.bitrate)
        .firstOrNull { it > 0 }
        ?.let { "%.1f Mbps".format(Locale.ROOT, it / 1_000_000f) }
    val frameRate = format.frameRate.takeIf { it > 0f }?.let { "${it.roundToInt()} fps" }
    return listOfNotNull(resolution, bitrate, frameRate).joinToString(" - ")
}

internal fun audioTrackLabel(format: Format): String {
    val language = format.label?.takeIf { it.isNotBlank() }
        ?: format.language?.let(::displayLanguage)
        ?: "Audiotrack"
    val channels = when (format.channelCount) {
        1 -> "mono"
        2 -> "stereo"
        6 -> "5.1"
        8 -> "7.1"
        in 3..Int.MAX_VALUE -> "${format.channelCount} kanalen"
        else -> null
    }
    val codec = format.codecs?.takeIf { it.isNotBlank() }
        ?: format.sampleMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
    return listOfNotNull(language, channels, codec).joinToString(" - ")
}

internal fun subtitleTrackLabel(format: Format): String {
    val language = format.label?.takeIf { it.isNotBlank() }
        ?: format.language?.let(::displayLanguage)
        ?: "Ondertiteling"
    val type = format.sampleMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
    return listOfNotNull(language, type).joinToString(" - ")
}

internal fun buildPlayerTechnicalInfo(
    containerExtension: String?,
    videoFormat: Format?,
    audioFormat: Format?,
    measuredWidth: Int = 0,
    measuredHeight: Int = 0
): List<PlayerTechnicalInfoRow> {
    val width = measuredWidth.takeIf { it > 0 } ?: videoFormat?.width?.takeIf { it > 0 }
    val height = measuredHeight.takeIf { it > 0 } ?: videoFormat?.height?.takeIf { it > 0 }
    val resolution = if (width != null && height != null) "$width x $height" else UNKNOWN
    val quality = height?.let(::qualityFromHeight) ?: UNKNOWN
    val container = containerExtension?.trim()?.removePrefix(".")?.takeIf(String::isNotEmpty)?.uppercase(Locale.ROOT)
        ?: videoFormat?.containerMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
        ?: audioFormat?.containerMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
        ?: UNKNOWN
    val videoBitrate = videoFormat?.preferredBitrate()?.let(::formatBitrate) ?: UNKNOWN
    val audioBitrate = audioFormat?.preferredBitrate()?.let(::formatBitrate) ?: UNKNOWN
    val frameRate = videoFormat?.frameRate?.takeIf { it > 0f }?.let { "%.2f fps".format(Locale.ROOT, it) } ?: UNKNOWN
    val sampleRate = audioFormat?.sampleRate?.takeIf { it > 0 }?.let { "%.1f kHz".format(Locale.ROOT, it / 1000f) } ?: UNKNOWN
    val language = audioFormat?.label?.takeIf(String::isNotBlank)
        ?: audioFormat?.language?.let(::displayLanguage)
        ?: UNKNOWN

    return listOf(
        PlayerTechnicalInfoRow("Resolutie", resolution),
        PlayerTechnicalInfoRow("Kwaliteit", quality),
        PlayerTechnicalInfoRow("Container", container),
        PlayerTechnicalInfoRow("Videocodec", videoFormat.codecName(video = true)),
        PlayerTechnicalInfoRow("Video MIME", videoFormat?.sampleMimeType ?: UNKNOWN),
        PlayerTechnicalInfoRow("Framerate", frameRate),
        PlayerTechnicalInfoRow("Videobitrate", videoBitrate),
        PlayerTechnicalInfoRow("Audiotrack", language),
        PlayerTechnicalInfoRow("Audiocodec", audioFormat.codecName(video = false)),
        PlayerTechnicalInfoRow("Audio MIME", audioFormat?.sampleMimeType ?: UNKNOWN),
        PlayerTechnicalInfoRow("Kanalen", channelLayout(audioFormat?.channelCount ?: Format.NO_VALUE)),
        PlayerTechnicalInfoRow("Samplefrequentie", sampleRate),
        PlayerTechnicalInfoRow("Audiobitrate", audioBitrate)
    )
}

private fun Format?.codecName(video: Boolean): String {
    if (this == null) return UNKNOWN
    val mime = sampleMimeType.orEmpty().lowercase(Locale.ROOT)
    val codec = codecs?.takeIf(String::isNotBlank)
    val friendly = when {
        "avc" in mime || codec.orEmpty().startsWith("avc1", true) -> "H.264 / AVC"
        "hevc" in mime || "h265" in mime || codec.orEmpty().startsWith("hev1", true) || codec.orEmpty().startsWith("hvc1", true) -> "H.265 / HEVC"
        "av01" in mime || codec.orEmpty().startsWith("av01", true) -> "AV1"
        "vp9" in mime || codec.orEmpty().startsWith("vp09", true) -> "VP9"
        "aac" in mime || codec.orEmpty().startsWith("mp4a", true) -> "AAC"
        "eac3" in mime || "e-ac3" in mime || codec.orEmpty().contains("ec-3", true) -> "E-AC-3"
        "ac3" in mime || codec.orEmpty().contains("ac-3", true) -> "AC-3"
        "dts" in mime -> "DTS"
        "opus" in mime -> "Opus"
        "flac" in mime -> "FLAC"
        "mpeg" in mime && !video -> "MP3"
        else -> sampleMimeType?.substringAfter('/')?.uppercase(Locale.ROOT)
    }
    return listOfNotNull(friendly, codec?.takeUnless { it.equals(friendly, ignoreCase = true) })
        .joinToString(" - ").ifBlank { UNKNOWN }
}

private fun Format.preferredBitrate(): Int? = sequenceOf(averageBitrate, peakBitrate, bitrate).firstOrNull { it > 0 }

private fun formatBitrate(bitrate: Int): String = if (bitrate >= 1_000_000) {
    "%.2f Mbps".format(Locale.ROOT, bitrate / 1_000_000f)
} else {
    "%.0f kbps".format(Locale.ROOT, bitrate / 1000f)
}

private fun qualityFromHeight(height: Int): String = when {
    height >= 2000 -> "4K"
    height >= 1000 -> "FHD"
    height >= 700 -> "HD"
    else -> "SD"
}

private fun channelLayout(channels: Int): String = when (channels) {
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    in 3..Int.MAX_VALUE -> "$channels kanalen"
    else -> UNKNOWN
}

private fun displayLanguage(tag: String): String = runCatching {
    Locale.forLanguageTag(tag).getDisplayLanguage(Locale.forLanguageTag("nl-NL"))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("nl-NL")) else it.toString() }
}.getOrDefault(tag.uppercase(Locale.ROOT))

private const val MIN_RESUME_POSITION_MS = 10_000L
private const val COMPLETION_MARGIN_MS = 30_000L
private const val UNKNOWN = "Onbekend"
