package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class ZetarakuResponse(
    val songs: List<ZetarakuItem>
)
