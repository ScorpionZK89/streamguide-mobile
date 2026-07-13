package com.example.streamguidemobile.ui.preview

import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.GuideChannelState
import com.example.streamguidemobile.StreamGuideState
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import java.time.LocalDate
import java.time.ZoneId

internal fun liveGuidePreviewState(): StreamGuideState {
    val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val now = dayStart + 20L * 60L * 60L * 1000L + 18L * 60L * 1000L
    val channels = listOf(
        "Nova News 4K" to "Nieuws",
        "Arena Sports FHD" to "Sport",
        "Horizon Cinema HD" to "Films",
        "Junior Planet" to "Kinderen",
        "Terra Docs" to "Documentaires",
        "Pulse Music" to "Muziek",
        "City Live" to "Nieuws",
        "Adventure One" to "Documentaires"
    ).mapIndexed { index, (name, group) ->
        ChannelEntity(
            id = (index + 1).toLong(),
            playlistId = 1,
            name = name,
            tvgId = "preview-${index + 1}",
            tvgName = name,
            logoUrl = null,
            groupTitle = group,
            streamUrl = "https://example.com/${index + 1}",
            isFavorite = index == 0 || index == 2,
            lastWatchedAt = if (index == 1) now - 60_000 else null,
            sortOrder = index
        )
    }
    val programsByChannel = channels.associateWith { channel ->
        val titles = when (channel.groupTitle) {
            "Nieuws" -> listOf("Wereldnieuws", "Avondreportage", "Nieuws van morgen", "Nachtbulletin")
            "Sport" -> listOf("Voorbeschouwing", "Live kampioenschap", "Analyse", "Sportjournaal")
            "Films" -> listOf("City of Glass", "Night Crossing", "Behind the Story", "Late Premiere")
            "Kinderen" -> listOf("Studio Junior", "Ruimtereis", "Avonturenclub", "Slaapverhalen")
            "Muziek" -> listOf("Chart Live", "Acoustic Sessions", "Night Beats", "Classic Stage")
            else -> listOf("Wilde werelden", "Oceanen", "Techniek van morgen", "Grenzeloos")
        }
        titles.mapIndexed { slot, title ->
            val start = dayStart + (19L + slot) * 60L * 60L * 1000L
            ProgramEntity(
                id = channel.id * 100 + slot,
                playlistId = 1,
                channelTvgId = channel.tvgId.orEmpty(),
                title = title,
                description = "Een zorgvuldig geselecteerd programma uit het persoonlijke live aanbod, met achtergrondinformatie en actuele details.",
                startTime = start,
                endTime = start + 60L * 60L * 1000L,
                category = channel.groupTitle,
                iconUrl = null
            )
        }
    }
    val rows = channels.map { channel ->
        val programs = programsByChannel.getValue(channel)
        val current = programs.firstOrNull { it.startTime <= now && it.endTime > now }
        val next = programs.firstOrNull { it.startTime > now }
        ChannelRowState(
            channel = channel,
            currentProgram = current,
            nextProgram = next,
            progress = current?.let { ((now - it.startTime).toFloat() / (it.endTime - it.startTime)).coerceIn(0f, 1f) } ?: 0f
        )
    }
    return StreamGuideState(
        channelRows = rows,
        channels = channels,
        groups = channels.map { it.groupTitle }.distinct(),
        allGroups = channels.map { it.groupTitle }.distinct(),
        guideRows = channels.map { GuideChannelState(it, programsByChannel.getValue(it)) },
        homeRows = rows,
        guideDayStart = dayStart,
        nowMillis = now
    )
}
