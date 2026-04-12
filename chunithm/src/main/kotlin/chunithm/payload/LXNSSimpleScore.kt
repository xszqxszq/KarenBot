package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSSimpleScore(
    val id: Int,
    @SerialName("level_index")
    val levelIndex: Int,
    val clear: String,
    @SerialName("full_combo")
    val fullCombo: String ?= null,
    @SerialName("full_chain")
    val fullChain: String ?= null,
    val rank: String ?= null,
)
