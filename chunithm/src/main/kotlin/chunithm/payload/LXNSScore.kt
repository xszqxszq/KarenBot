package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSScore(
    val id: Int,
    @SerialName("level_index")
    val levelIndex: Int,
    val score: Int,
    val rating: Double ?= null,
    @SerialName("over_power")
    val overpower: Double? = null,
    val clear: String,
    @SerialName("full_combo")
    val fullCombo: String ?= null,
    @SerialName("full_chain")
    val fullChain: String ?= null,
    val rank: String ?= null,
    @SerialName("play_time")
    val playTime: String ?= null,
    @SerialName("upload_time")
    val uploadTime: String ?= null,
    @SerialName("last_played_time")
    val lastPlayedTime: String ?= null,
)