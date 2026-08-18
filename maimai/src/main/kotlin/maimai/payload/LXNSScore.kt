package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSScore(
    val id: Int,
    @SerialName("level_index")
    val levelIndex: Int,
    val achievements: Float,
    val fc: String ?= null,
    val fs: String ?= null,
    @SerialName("dx_score")
    val dxScore: Int,
    val type: String,
    @SerialName("play_time")
    val playTime: String ?= null,
    @SerialName("upload_time")
    val uploadTime: String ?= null,
    @SerialName("last_played_time")
    val lastPlayedTime: String ?= null,
)