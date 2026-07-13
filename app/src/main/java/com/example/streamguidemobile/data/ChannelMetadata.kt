package com.example.streamguidemobile.data

import java.util.ArrayDeque

internal class ExistingChannelMatcher(channels: List<ChannelEntity>) {
    private val usedIds = mutableSetOf<Long>()
    private val byTvgId = channels.bucketBy { it.tvgId.normalizedKey() }
    private val byStreamUrl = channels.bucketBy { it.streamUrl.trim().takeIf(String::isNotEmpty) }
    private val byNameAndGroup = channels.bucketBy { "${it.name.normalizedKey()}|${it.groupTitle.normalizedKey()}" }

    fun match(channel: ParsedChannel): ChannelEntity? {
        val tvgId = channel.tvgId.normalizedKey()
        val streamUrl = channel.streamUrl.trim().takeIf(String::isNotEmpty)
        val nameAndGroup = "${channel.name.normalizedKey()}|${channel.groupTitle.normalizedKey()}"
        return take(tvgId?.let(byTvgId::get))
            ?: take(streamUrl?.let(byStreamUrl::get))
            ?: take(byNameAndGroup[nameAndGroup])
    }

    private fun take(candidates: ArrayDeque<ChannelEntity>?): ChannelEntity? {
        while (candidates?.isNotEmpty() == true) {
            val candidate = candidates.removeFirst()
            if (usedIds.add(candidate.id)) return candidate
        }
        return null
    }
}

internal fun ParsedChannel.toChannelEntity(
    playlistId: Long,
    sortOrder: Int,
    previous: ChannelEntity?
): ChannelEntity = ChannelEntity(
    id = previous?.id ?: 0,
    playlistId = playlistId,
    name = name,
    tvgId = tvgId,
    tvgName = tvgName,
    logoUrl = logoUrl,
    groupTitle = groupTitle,
    streamUrl = streamUrl,
    isFavorite = previous?.isFavorite ?: false,
    lastWatchedAt = previous?.lastWatchedAt,
    sortOrder = sortOrder
)

private fun List<ChannelEntity>.bucketBy(keySelector: (ChannelEntity) -> String?): Map<String, ArrayDeque<ChannelEntity>> {
    val buckets = mutableMapOf<String, ArrayDeque<ChannelEntity>>()
    forEach { channel ->
        keySelector(channel)?.let { key -> buckets.getOrPut(key, ::ArrayDeque).addLast(channel) }
    }
    return buckets
}

private fun String?.normalizedKey(): String? = this?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
