package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪查分器的单曲游玩成绩
 *
 * @property score 达成率，1000000 对应 100%
 * @property clear 通关状态
 * @property fullCombo 连击状态码
 * @property fullChain FULL CHAIN 状态码
 * @property rank 评级字符串
 * @property playTime 游玩时间点
 * @property uploadTime 上传时间点
 * @property lastPlayedTime 最近游玩时间点
 */
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