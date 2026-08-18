package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSTrophyList(
    val trophies: List<LXNSTrophyInfo>
)