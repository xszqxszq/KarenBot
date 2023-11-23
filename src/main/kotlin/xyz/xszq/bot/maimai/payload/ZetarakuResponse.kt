package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class ZetarakuResponse(
    val songs: List<ZetarakuItem>
)
