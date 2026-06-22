package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSTrophyRequired(
    val difficulties: List<Int> ?= null,
    val rank: String ?= null,
    @SerialName("full_combo")
    val fullCombo: String ?= null,
    @SerialName("full_chain")
    val fullChain: String ?= null,
    val songs: List<LXNSTrophyRequiredSong> ?= null
)
