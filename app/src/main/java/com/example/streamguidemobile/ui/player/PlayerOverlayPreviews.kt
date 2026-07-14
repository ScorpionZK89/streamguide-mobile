package com.example.streamguidemobile.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.streamguidemobile.R
import com.example.streamguidemobile.ui.preview.liveGuidePreviewState
import com.example.streamguidemobile.ui.theme.StreamGuideTheme

private enum class PlayerPreviewMode {
    Live,
    Film,
    Audio,
    Subtitles,
    TechnicalInfo,
    UpNext
}

@Composable
private fun PremiumPlayerPreview(mode: PlayerPreviewMode) {
    val state = liveGuidePreviewState()
    val row = state.channelRows.first()
    val upcoming = state.guideRows.first().programs.filter { it.startTime > state.nowMillis }.take(3)
    val isMedia = mode == PlayerPreviewMode.Film || mode == PlayerPreviewMode.TechnicalInfo || mode == PlayerPreviewMode.UpNext

    StreamGuideTheme {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                painter = painterResource(
                    if (isMedia) R.drawable.streamguide_cinematic_hero else R.drawable.streamguide_hero_sports
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            PlayerOverlayScrims(compact = false)

            if (isMedia) {
                PremiumMediaTopBar(
                    title = "Voorbij de horizon",
                    subtitle = "2026 - 2 u 08 min - 12+",
                    quality = "4K",
                    onBack = {},
                    onMore = {},
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else {
                PremiumPlayerTopBar(
                    channel = row.channel,
                    program = row.currentProgram,
                    quality = "FHD",
                    showLogos = false,
                    isFavorite = true,
                    onBack = {},
                    onFavorite = {},
                    onMore = {},
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            PremiumCenterControls(
                isPlaying = true,
                seekable = isMedia,
                onSeekBack = {},
                onPlayPause = {},
                onSeekForward = {},
                seekBackIcon = Icons.Default.Replay10,
                playIcon = Icons.Default.PlayArrow,
                pauseIcon = Icons.Default.Pause,
                seekForwardIcon = Icons.Default.Forward10,
                onPreviousChannel = if (isMedia) null else ({}),
                onNextChannel = if (isMedia) null else ({}),
                modifier = Modifier.align(Alignment.Center)
            )

            if (mode != PlayerPreviewMode.UpNext) {
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.88f).padding(bottom = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isMedia) {
                        SeekableTimeline(
                            position = 3_180_000L,
                            duration = 7_680_000L,
                            bufferedPosition = 4_260_000L,
                            onSeek = {},
                            onInteraction = {}
                        )
                    } else {
                        LiveProgramTimeline(row.currentProgram, state.nowMillis)
                    }
                    PlayerActionBar(
                        actions = listOf(
                            PlayerAction("Info", Icons.Default.Info) {},
                            PlayerAction("Ondertitels", Icons.Default.ClosedCaption) {},
                            PlayerAction("Audio", Icons.Default.Audiotrack) {},
                            PlayerAction("Kwaliteit", Icons.Default.HighQuality) {},
                            PlayerAction("Beeld", Icons.Default.AspectRatio) {},
                            PlayerAction("Slaap", Icons.Default.Bedtime) {},
                            PlayerAction("Meer", Icons.Default.MoreHoriz) {}
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    if (!isMedia) {
                        LiveScheduleStrip(
                            programs = upcoming,
                            expanded = false,
                            onToggle = {},
                            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp).fillMaxWidth()
                        )
                    }
                }
            }

            when (mode) {
                PlayerPreviewMode.Audio -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
                    PlayerSelectionPanel(
                        title = "Audiotrack",
                        items = listOf(
                            PlayerSelectionItem("auto", "Automatisch", "Volgt de voorkeur van de stream", false) {},
                            PlayerSelectionItem("nl", "Nederlands", "Stereo - AAC", true) {},
                            PlayerSelectionItem("en", "Engels", "5.1 - AC-3", false) {}
                        ),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
                PlayerPreviewMode.Subtitles -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
                    PlayerSelectionPanel(
                        title = "Ondertitels",
                        items = listOf(
                            PlayerSelectionItem("off", "Uit", selected = false) {},
                            PlayerSelectionItem("nl", "Nederlands", "WebVTT", true) {},
                            PlayerSelectionItem("en", "English", "WebVTT", false) {}
                        ),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
                PlayerPreviewMode.TechnicalInfo -> {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
                    PlayerTechnicalInfoPanel(
                        title = "Voorbij de horizon",
                        rows = listOf(
                            PlayerTechnicalInfoRow("Resolutie", "3840 x 2160"),
                            PlayerTechnicalInfoRow("Kwaliteit", "4K"),
                            PlayerTechnicalInfoRow("Container", "MKV"),
                            PlayerTechnicalInfoRow("Videocodec", "H.265 / HEVC - hvc1.2.4.L153"),
                            PlayerTechnicalInfoRow("Framerate", "23.98 fps"),
                            PlayerTechnicalInfoRow("Videobitrate", "12.50 Mbps"),
                            PlayerTechnicalInfoRow("Audiocodec", "AAC - mp4a.40.2"),
                            PlayerTechnicalInfoRow("Kanalen", "5.1"),
                            PlayerTechnicalInfoRow("Samplefrequentie", "48.0 kHz")
                        ),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
                PlayerPreviewMode.UpNext -> MediaUpNextCard(
                    title = "De stille grens",
                    metadata = "Seizoen 1 - Aflevering 6",
                    countdownSeconds = 8,
                    artwork = painterResource(R.drawable.streamguide_poster_city),
                    onPlay = {},
                    onCancel = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.76f).padding(bottom = 14.dp)
                )
                else -> Unit
            }
        }
    }
}

@Preview(name = "1 Live TV - controls", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun LiveControlsPreview() = PremiumPlayerPreview(PlayerPreviewMode.Live)

@Preview(name = "2 Film - controls", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun FilmControlsPreview() = PremiumPlayerPreview(PlayerPreviewMode.Film)

@Preview(name = "3 Audio menu", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun AudioMenuPreview() = PremiumPlayerPreview(PlayerPreviewMode.Audio)

@Preview(name = "4 Subtitle menu", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun SubtitleMenuPreview() = PremiumPlayerPreview(PlayerPreviewMode.Subtitles)

@Preview(name = "5 Up Next", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun UpNextPreview() = PremiumPlayerPreview(PlayerPreviewMode.UpNext)

@Preview(name = "6 Film technical info", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun TechnicalInfoPreview() = PremiumPlayerPreview(PlayerPreviewMode.TechnicalInfo)

@Preview(name = "Tablet landscape", widthDp = 1100, heightDp = 720, showBackground = true)
@Composable
private fun TabletPlayerPreview() = PremiumPlayerPreview(PlayerPreviewMode.Live)
